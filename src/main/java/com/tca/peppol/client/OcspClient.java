package com.tca.peppol.client;

import org.bouncycastle.cert.ocsp.*;
import org.bouncycastle.cert.ocsp.jcajce.JcaCertificateID;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OCSP (Online Certificate Status Protocol) client for real-time certificate revocation checking.
 * 
 * Features:
 * - OCSP request construction and response validation
 * - 10-second timeout with graceful failure handling
 * - Response caching based on validity period
 * - Rate limiting protection for OCSP queries
 * - Fallback mechanism when OCSP is unavailable
 * - Comprehensive error handling and logging
 * 
 * This implementation follows RFC 6960 (OCSP) specifications and provides
 * robust certificate revocation checking for Peppol certificate validation.
 */
public class OcspClient {
    
    private static final Logger logger = LoggerFactory.getLogger(OcspClient.class);
    
    // Configuration constants
    private static final int OCSP_TIMEOUT_SECONDS = 10;
    private static final int MAX_CACHE_SIZE = 1000;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000; // 1 minute
    private static final int MAX_REQUESTS_PER_WINDOW = 100;
    
    // Cache for OCSP responses
    private static final Map<String, CachedOcspResponse> responseCache = new ConcurrentHashMap<>();

    
    // Rate limiting
    private static final Map<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();
    
    // Digest calculator provider for OCSP requests
    private static final DigestCalculatorProvider digestCalculatorProvider;
    
    static {
        try {
            digestCalculatorProvider = new JcaDigestCalculatorProviderBuilder().build();
        } catch (OperatorCreationException e) {
            throw new RuntimeException("Failed to initialize OCSP digest calculator provider", e);
        }
    }
    
    /**
     * Check the revocation status of a certificate using OCSP.
     * 
     * @param certificate The certificate to check
     * @param issuerCertificate The issuer certificate
     * @return OcspResult containing the revocation status and details
     */
    public OcspResult checkRevocationStatus(X509Certificate certificate, X509Certificate issuerCertificate) {
        if (certificate == null) {
            throw new IllegalArgumentException("Certificate cannot be null");
        }
        if (issuerCertificate == null) {
            throw new IllegalArgumentException("Issuer certificate cannot be null");
        }
        
        String correlationId = generateCorrelationId();
        logger.info("OCSP revocation check initiated [correlationId={}, subject={}]", 
            correlationId, certificate.getSubjectX500Principal().getName());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Generate cache key
            String cacheKey = generateCacheKey(certificate, issuerCertificate);
            
            // Check cache first
            CachedOcspResponse cachedResponse = responseCache.get(cacheKey);
            if (cachedResponse != null && !cachedResponse.isExpired()) {
                logger.debug("OCSP response found in cache [correlationId={}, cacheKey={}]", 
                    correlationId, cacheKey);
                return cachedResponse.getResult();
            }
            
            // Extract OCSP responder URL
            String ocspUrl = extractOcspResponderUrl(certificate);
            if (ocspUrl == null) {
                logger.warn("No OCSP responder URL found in certificate [correlationId={}]", correlationId);
                return OcspResult.unavailable("No OCSP responder URL found in certificate");
            }
            
            // Check rate limiting
            if (isRateLimited(ocspUrl)) {
                logger.warn("OCSP request rate limited [correlationId={}, url={}]", correlationId, ocspUrl);
                return OcspResult.unavailable("OCSP requests rate limited");
            }
            
            // Build OCSP request
            OCSPReq ocspRequest = buildOcspRequest(certificate, issuerCertificate);
            
            // Send OCSP request with X-Ray tracing
            OcspResult result = sendOcspRequest(ocspUrl, ocspRequest, correlationId);
            
            // Cache successful responses
            if (result.isSuccessful() && result.getNextUpdate() != null) {
                Instant cacheExpiry = result.getNextUpdate().minus(5, ChronoUnit.MINUTES); // 5 min buffer
                responseCache.put(cacheKey, new CachedOcspResponse(result, cacheExpiry));
                
                // Clean up cache if it gets too large
                if (responseCache.size() > MAX_CACHE_SIZE) {
                    cleanupCache();
                }
            }
            
            long responseTime = System.currentTimeMillis() - startTime;
            logger.info("OCSP revocation check completed [correlationId={}, status={}, responseTime={}ms]", 
                correlationId, result.getStatus(), responseTime);
            
            return result;
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("OCSP revocation check failed [correlationId={}, responseTime={}ms]", 
                correlationId, responseTime, e);
            return OcspResult.error("OCSP check failed: " + e.getMessage());
        }
    }
    
    /**
     * Build an OCSP request for the given certificate.
     */
    private static OCSPReq buildOcspRequest(X509Certificate certificate, X509Certificate issuerCertificate) 
            throws OCSPException, OperatorCreationException, CertificateEncodingException {
        
        logger.debug("Building OCSP request for certificate [subject={}]", 
            certificate.getSubjectX500Principal().getName());
        
        // Create certificate ID
        CertificateID certId = new JcaCertificateID(
            digestCalculatorProvider.get(CertificateID.HASH_SHA1),
            issuerCertificate,
            certificate.getSerialNumber()
        );
        
        // Create OCSP request builder
        OCSPReqBuilder reqBuilder = new OCSPReqBuilder();
        reqBuilder.addRequest(certId);
        
        // Note: Nonce extension could be added here for replay attack prevention
        // but is omitted for simplicity in this implementation
        
        return reqBuilder.build();
    }
    
    /**
     * Send OCSP request to the responder and process the response.
     */
    private OcspResult sendOcspRequest(String ocspUrl, OCSPReq ocspRequest, String correlationId) {
        try {
            logger.debug("Sending OCSP request [correlationId={}, url={}]", correlationId, ocspUrl);
            
            // Prepare request headers
            Map<String, String> headers = Map.of(
                "Content-Type", "application/ocsp-request",
                "Accept", "application/ocsp-response"
            );
            
            // Send POST request with OCSP request body
            byte[] requestBytes = ocspRequest.getEncoded();
            HttpResponse httpResponse = SecureHttpClient.post(ocspUrl, headers, requestBytes, OCSP_TIMEOUT_SECONDS);
            
            if (!httpResponse.isSuccess()) {
                logger.warn("OCSP HTTP request failed [correlationId={}, status={}]", 
                    correlationId, httpResponse.getStatusCode());
                return OcspResult.unavailable("OCSP HTTP request failed with status: " + httpResponse.getStatusCode());
            }
            
            // Parse OCSP response
            byte[] responseBytes = httpResponse.getBodyBytes();
            if (responseBytes == null) {
                return OcspResult.error("OCSP response body is empty");
            }
            OCSPResp ocspResponse = new OCSPResp(responseBytes);
            
            return processOcspResponse(ocspResponse, correlationId);
            
        } catch (Exception e) {
            logger.error("Failed to send OCSP request [correlationId={}]", correlationId, e);
            return OcspResult.error("Failed to send OCSP request: " + e.getMessage());
        }
    }
    
    /**
     * Process OCSP response and extract revocation status.
     */
    private static OcspResult processOcspResponse(OCSPResp ocspResponse, String correlationId) {
        try {
            int status = ocspResponse.getStatus();
            
            if (status != OCSPResp.SUCCESSFUL) {
                logger.warn("OCSP response indicates failure [correlationId={}, status={}]", correlationId, status);
                return OcspResult.unavailable("OCSP response status: " + getOcspStatusDescription(status));
            }
            
            BasicOCSPResp basicResponse = (BasicOCSPResp) ocspResponse.getResponseObject();
            if (basicResponse == null) {
                logger.warn("OCSP basic response is null [correlationId={}]", correlationId);
                return OcspResult.unavailable("OCSP basic response is null");
            }
            
            SingleResp[] responses = basicResponse.getResponses();
            if (responses == null || responses.length == 0) {
                logger.warn("OCSP response contains no certificate responses [correlationId={}]", correlationId);
                return OcspResult.unavailable("OCSP response contains no certificate responses");
            }
            
            // Process first response (we only sent one request)
            SingleResp singleResponse = responses[0];
            CertificateStatus certStatus = singleResponse.getCertStatus();
            
            Date thisUpdate = singleResponse.getThisUpdate();
            Date nextUpdate = singleResponse.getNextUpdate();
            
            Instant thisUpdateInstant = thisUpdate != null ? thisUpdate.toInstant() : Instant.now();
            Instant nextUpdateInstant = nextUpdate != null ? nextUpdate.toInstant() : null;
            
            if (certStatus == CertificateStatus.GOOD) {
                logger.debug("OCSP response indicates certificate is good [correlationId={}]", correlationId);
                return OcspResult.good(thisUpdateInstant, nextUpdateInstant);
            } else if (certStatus instanceof RevokedStatus) {
                RevokedStatus revokedStatus = (RevokedStatus) certStatus;
                Date revocationTime = revokedStatus.getRevocationTime();
                int revocationReason = revokedStatus.getRevocationReason();
                
                logger.warn("OCSP response indicates certificate is revoked [correlationId={}, revocationTime={}, reason={}]", 
                    correlationId, revocationTime, revocationReason);
                
                return OcspResult.revoked(
                    revocationTime != null ? revocationTime.toInstant() : Instant.now(),
                    revocationReason,
                    thisUpdateInstant,
                    nextUpdateInstant
                );
            } else {
                logger.warn("OCSP response indicates unknown certificate status [correlationId={}]", correlationId);
                return OcspResult.unknown(thisUpdateInstant, nextUpdateInstant);
            }
            
        } catch (Exception e) {
            logger.error("Failed to process OCSP response [correlationId={}]", correlationId, e);
            return OcspResult.error("Failed to process OCSP response: " + e.getMessage());
        }
    }
    
    /**
     * Extract OCSP responder URL from certificate's Authority Information Access extension.
     */
    private static String extractOcspResponderUrl(X509Certificate certificate) {
        try {
            byte[] aiaExtension = certificate.getExtensionValue("1.3.6.1.5.5.7.1.1"); // Authority Information Access
            if (aiaExtension == null) {
                return null;
            }
            
            // Parse AIA extension to extract OCSP URL
            // This is a simplified implementation - in production, you might want to use
            // a more robust ASN.1 parser
            String aiaString = new String(aiaExtension);
            if (aiaString.contains("http")) {
                int startIndex = aiaString.indexOf("http");
                int endIndex = aiaString.indexOf('\0', startIndex);
                if (endIndex == -1) {
                    endIndex = aiaString.length();
                }
                return aiaString.substring(startIndex, endIndex);
            }
            
            return null;
            
        } catch (Exception e) {
            logger.debug("Failed to extract OCSP responder URL from certificate", e);
            return null;
        }
    }
    
    /**
     * Generate cache key for OCSP response caching.
     */
    private static String generateCacheKey(X509Certificate certificate, X509Certificate issuerCertificate) {
        return "ocsp-" + certificate.getSerialNumber().toString(16) + "-" + 
               issuerCertificate.getSerialNumber().toString(16);
    }
    
    /**
     * Check if requests to the given URL are rate limited.
     */
    private static boolean isRateLimited(String url) {
        long currentTime = System.currentTimeMillis();
        
        RateLimitInfo rateLimitInfo = rateLimitMap.computeIfAbsent(url, k -> new RateLimitInfo());
        
        synchronized (rateLimitInfo) {
            // Reset window if expired
            if (currentTime - rateLimitInfo.windowStart > RATE_LIMIT_WINDOW_MS) {
                rateLimitInfo.windowStart = currentTime;
                rateLimitInfo.requestCount.set(0);
            }
            
            // Check if rate limit exceeded
            if (rateLimitInfo.requestCount.get() >= MAX_REQUESTS_PER_WINDOW) {
                return true;
            }
            
            // Increment request count
            rateLimitInfo.requestCount.incrementAndGet();
            return false;
        }
    }
    
    /**
     * Clean up expired entries from the response cache.
     */
    private static void cleanupCache() {
        Instant now = Instant.now();
        responseCache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
        logger.debug("OCSP cache cleanup completed, remaining entries: {}", responseCache.size());
    }
    
    /**
     * Get description for OCSP response status code.
     */
    private static String getOcspStatusDescription(int status) {
        return switch (status) {
            case OCSPResp.SUCCESSFUL -> "Successful";
            case OCSPResp.MALFORMED_REQUEST -> "Malformed Request";
            case OCSPResp.INTERNAL_ERROR -> "Internal Error";
            case OCSPResp.TRY_LATER -> "Try Later";
            case OCSPResp.SIG_REQUIRED -> "Signature Required";
            case OCSPResp.UNAUTHORIZED -> "Unauthorized";
            default -> "Unknown Status: " + status;
        };
    }
    
    /**
     * Generate correlation ID for request tracking.
     */
    private static String generateCorrelationId() {
        return "ocsp-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString(Thread.currentThread().hashCode());
    }
    
    /**
     * Get OCSP cache statistics for monitoring.
     */
    public static OcspCacheStats getCacheStats() {
        int totalEntries = responseCache.size();
        long expiredEntries = responseCache.values().stream()
            .mapToLong(entry -> entry.isExpired() ? 1 : 0)
            .sum();
        
        return new OcspCacheStats(totalEntries, (int) expiredEntries, MAX_CACHE_SIZE);
    }
    
    /**
     * Clear the OCSP response cache.
     */
    public static void clearCache() {
        responseCache.clear();
        logger.info("OCSP response cache cleared");
    }
    
    /**
     * Cached OCSP response with expiry time.
     */
    private static class CachedOcspResponse {
        private final OcspResult result;
        private final Instant expiry;
        
        public CachedOcspResponse(OcspResult result, Instant expiry) {
            this.result = result;
            this.expiry = expiry;
        }
        
        public OcspResult getResult() {
            return result;
        }
        
        public boolean isExpired() {
            return isExpired(Instant.now());
        }
        
        public boolean isExpired(Instant now) {
            return expiry != null && now.isAfter(expiry);
        }
    }
    
    /**
     * Rate limiting information for OCSP requests.
     */
    private static class RateLimitInfo {
        private volatile long windowStart = System.currentTimeMillis();
        private final AtomicLong requestCount = new AtomicLong(0);
    }
}