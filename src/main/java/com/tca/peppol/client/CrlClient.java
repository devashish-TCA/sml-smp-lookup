package com.tca.peppol.client;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CRL (Certificate Revocation List) client for certificate revocation checking.
 * 
 * Features:
 * - CRL download from distribution points
 * - CRL signature validation before processing revocation data
 * - CRL caching with appropriate TTL based on nextUpdate field
 * - Revocation status checking against CRL data
 * - Comprehensive error handling and logging
 * 
 * This implementation follows RFC 5280 specifications and provides
 * robust certificate revocation checking for Peppol certificate validation.
 */
public class CrlClient {
    
    private static final Logger logger = LoggerFactory.getLogger(CrlClient.class);
    
    // Configuration constants
    private static final int CRL_DOWNLOAD_TIMEOUT_SECONDS = 30;
    private static final int MAX_CACHE_SIZE = 100;
    private static final long CACHE_CLEANUP_INTERVAL_MS = 300_000; // 5 minutes
    
    // Cache for CRL data
    private static final Map<String, CachedCrl> crlCache = new ConcurrentHashMap<>();
    private static volatile long lastCacheCleanup = System.currentTimeMillis();
    


    /**
     * Check the revocation status of a certificate using CRL.
     * 
     * @param certificate The certificate to check
     * @param issuerCertificate The issuer certificate for signature validation
     * @return CrlResult containing the revocation status and details
     */
    public CrlResult checkRevocationStatus(X509Certificate certificate, X509Certificate issuerCertificate) {
        if (certificate == null) {
            throw new IllegalArgumentException("Certificate cannot be null");
        }
        if (issuerCertificate == null) {
            throw new IllegalArgumentException("Issuer certificate cannot be null");
        }
        
        String correlationId = generateCorrelationId();
        logger.info("CRL revocation check initiated [correlationId={}, subject={}]", 
            correlationId, certificate.getSubjectX500Principal().getName());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Extract CRL distribution points
            List<String> crlUrls = extractCrlDistributionPoints(certificate);
            if (crlUrls.isEmpty()) {
                logger.warn("No CRL distribution points found in certificate [correlationId={}]", correlationId);
                return CrlResult.unavailable(null, "No CRL distribution points found in certificate");
            }
            
            // Try each CRL URL until we get a successful result
            CrlResult lastResult = null;
            for (String crlUrl : crlUrls) {
                logger.debug("Checking CRL [correlationId={}, url={}]", correlationId, crlUrl);
                
                try {
                    CrlResult result = checkCrlRevocation(certificate, issuerCertificate, crlUrl, correlationId);
                    
                    if (result.isSuccessful()) {
                        long responseTime = System.currentTimeMillis() - startTime;
                        logger.info("CRL revocation check completed successfully [correlationId={}, status={}, responseTime={}ms]", 
                            correlationId, result.getStatus(), responseTime);
                        return result.withResponseTime(responseTime);
                    }
                    
                    lastResult = result;
                    logger.debug("CRL check failed, trying next URL [correlationId={}, status={}]", 
                        correlationId, result.getStatus());
                    
                } catch (Exception e) {
                    logger.warn("CRL check failed for URL [correlationId={}, url={}]", correlationId, crlUrl, e);
                    lastResult = CrlResult.error(crlUrl, "CRL check failed: " + e.getMessage());
                }
            }
            
            // All CRL URLs failed
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("All CRL checks failed [correlationId={}, responseTime={}ms]", correlationId, responseTime);
            
            if (lastResult != null) {
                return lastResult.withResponseTime(responseTime);
            } else {
                return CrlResult.error(null, "All CRL distribution points failed").withResponseTime(responseTime);
            }
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("CRL revocation check failed [correlationId={}, responseTime={}ms]", 
                correlationId, responseTime, e);
            return CrlResult.error(null, "CRL check failed: " + e.getMessage()).withResponseTime(responseTime);
        }
    }
    
    /**
     * Check revocation status against a specific CRL URL.
     */
    private static CrlResult checkCrlRevocation(X509Certificate certificate, X509Certificate issuerCertificate, 
                                               String crlUrl, String correlationId) {
        try {
            // Check cache first
            CachedCrl cachedCrl = crlCache.get(crlUrl);
            X509CRL crl = null;
            
            if (cachedCrl != null && !cachedCrl.isExpired()) {
                logger.debug("CRL found in cache [correlationId={}, url={}]", correlationId, crlUrl);
                crl = cachedCrl.getCrl();
            } else {
                // Download CRL
                logger.debug("Downloading CRL [correlationId={}, url={}]", correlationId, crlUrl);
                crl = downloadCrl(crlUrl, correlationId);
                
                if (crl == null) {
                    return CrlResult.unavailable(crlUrl, "Failed to download CRL");
                }
                
                // Validate CRL signature
                if (!validateCrlSignature(crl, issuerCertificate, correlationId)) {
                    return CrlResult.error(crlUrl, "CRL signature validation failed");
                }
                
                // Cache the CRL
                cacheCrl(crlUrl, crl);
            }
            
            // Check revocation status
            return checkRevocationInCrl(certificate, crl, crlUrl, correlationId);
            
        } catch (Exception e) {
            logger.error("CRL revocation check failed [correlationId={}, url={}]", correlationId, crlUrl, e);
            return CrlResult.error(crlUrl, "CRL check failed: " + e.getMessage());
        }
    }
    
    /**
     * Download CRL from the specified URL.
     */
    private static X509CRL downloadCrl(String crlUrl, String correlationId) {
        try {
            logger.debug("Downloading CRL from URL [correlationId={}, url={}]", correlationId, crlUrl);
            
            // Use SecureHttpClient to download CRL
            Map<String, String> headers = Map.of(
                "Accept", "application/pkix-crl, application/x-pkcs7-crl"
            );
            
            HttpResponse response = SecureHttpClient.get(crlUrl, headers);
            
            if (!response.isSuccess()) {
                logger.warn("CRL download failed [correlationId={}, status={}]", 
                    correlationId, response.getStatusCode());
                return null;
            }
            
            byte[] crlBytes = response.getBodyBytes();
            if (crlBytes == null || crlBytes.length == 0) {
                logger.warn("CRL download returned empty content [correlationId={}]", correlationId);
                return null;
            }
            
            // Parse CRL
            try {
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                X509CRL crl = (X509CRL) certFactory.generateCRL(new ByteArrayInputStream(crlBytes));
                
                logger.debug("CRL downloaded successfully [correlationId={}, size={}, issuer={}]", 
                    correlationId, crlBytes.length, crl.getIssuerX500Principal().getName());
                
                return crl;
            } catch (CertificateException | CRLException e) {
                logger.warn("Failed to parse CRL data [correlationId={}, size={}]", 
                    correlationId, crlBytes.length, e);
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Failed to download CRL [correlationId={}, url={}]", correlationId, crlUrl, e);
            return null;
        }
    }
    
    /**
     * Validate CRL signature using the issuer certificate.
     */
    private static boolean validateCrlSignature(X509CRL crl, X509Certificate issuerCertificate, String correlationId) {
        try {
            logger.debug("Validating CRL signature [correlationId={}, issuer={}]", 
                correlationId, crl.getIssuerX500Principal().getName());
            
            // Verify that the CRL was signed by the issuer certificate
            PublicKey issuerPublicKey = issuerCertificate.getPublicKey();
            crl.verify(issuerPublicKey);
            
            logger.debug("CRL signature validation successful [correlationId={}]", correlationId);
            return true;
            
        } catch (CRLException | NoSuchAlgorithmException | InvalidKeyException | 
                 NoSuchProviderException | SignatureException e) {
            logger.error("CRL signature validation failed [correlationId={}]", correlationId, e);
            return false;
        }
    }
    
    /**
     * Check if the certificate is revoked according to the CRL.
     */
    private static CrlResult checkRevocationInCrl(X509Certificate certificate, X509CRL crl, 
                                                 String crlUrl, String correlationId) {
        try {
            BigInteger serialNumber = certificate.getSerialNumber();
            
            logger.debug("Checking certificate revocation in CRL [correlationId={}, serialNumber={}]", 
                correlationId, serialNumber.toString(16));
            
            // Check if certificate is revoked
            X509CRLEntry crlEntry = crl.getRevokedCertificate(serialNumber);
            
            Date thisUpdate = crl.getThisUpdate();
            Date nextUpdate = crl.getNextUpdate();
            
            Instant thisUpdateInstant = thisUpdate != null ? thisUpdate.toInstant() : Instant.now();
            Instant nextUpdateInstant = nextUpdate != null ? nextUpdate.toInstant() : null;
            
            if (crlEntry != null) {
                // Certificate is revoked
                Date revocationDate = crlEntry.getRevocationDate();
                Instant revocationTime = revocationDate != null ? revocationDate.toInstant() : Instant.now();
                
                // Extract revocation reason if available
                int revocationReason = 0; // Default to "unspecified"
                if (crlEntry.hasExtensions()) {
                    byte[] reasonExtension = crlEntry.getExtensionValue("2.5.29.21"); // CRL Reason Code
                    if (reasonExtension != null) {
                        try {
                            // Parse the reason code (simplified parsing)
                            ASN1InputStream asn1Stream = new ASN1InputStream(reasonExtension);
                            ASN1Primitive primitive = asn1Stream.readObject();
                            if (primitive instanceof ASN1OctetString) {
                                ASN1OctetString octetString = (ASN1OctetString) primitive;
                                byte[] reasonBytes = octetString.getOctets();
                                if (reasonBytes.length > 0) {
                                    revocationReason = reasonBytes[0] & 0xFF;
                                }
                            }
                            asn1Stream.close();
                        } catch (Exception e) {
                            logger.debug("Failed to parse revocation reason [correlationId={}]", correlationId, e);
                        }
                    }
                }
                
                logger.warn("Certificate is revoked according to CRL [correlationId={}, revocationTime={}, reason={}]", 
                    correlationId, revocationTime, revocationReason);
                
                return CrlResult.revoked(crlUrl, revocationTime, revocationReason, thisUpdateInstant, nextUpdateInstant);
            } else {
                // Certificate is not revoked
                logger.debug("Certificate is not revoked according to CRL [correlationId={}]", correlationId);
                return CrlResult.good(crlUrl, thisUpdateInstant, nextUpdateInstant);
            }
            
        } catch (Exception e) {
            logger.error("Failed to check revocation in CRL [correlationId={}]", correlationId, e);
            return CrlResult.error(crlUrl, "Failed to check revocation in CRL: " + e.getMessage());
        }
    }
    
    /**
     * Extract CRL distribution points from certificate.
     */
    private static List<String> extractCrlDistributionPoints(X509Certificate certificate) {
        List<String> crlUrls = new ArrayList<>();
        
        try {
            byte[] crlDistPointsExtension = certificate.getExtensionValue("2.5.29.31"); // CRL Distribution Points
            if (crlDistPointsExtension == null) {
                return crlUrls;
            }
            
            // Parse the extension
            ASN1InputStream asn1Stream = new ASN1InputStream(crlDistPointsExtension);
            ASN1Primitive primitive = asn1Stream.readObject();
            
            if (primitive instanceof ASN1OctetString) {
                ASN1OctetString octetString = (ASN1OctetString) primitive;
                ASN1InputStream innerStream = new ASN1InputStream(octetString.getOctets());
                ASN1Primitive innerPrimitive = innerStream.readObject();
                
                CRLDistPoint crlDistPoint = CRLDistPoint.getInstance(innerPrimitive);
                DistributionPoint[] distributionPoints = crlDistPoint.getDistributionPoints();
                
                for (DistributionPoint dp : distributionPoints) {
                    DistributionPointName dpName = dp.getDistributionPoint();
                    if (dpName != null && dpName.getType() == DistributionPointName.FULL_NAME) {
                        GeneralNames generalNames = (GeneralNames) dpName.getName();
                        GeneralName[] names = generalNames.getNames();
                        
                        for (GeneralName name : names) {
                            if (name.getTagNo() == GeneralName.uniformResourceIdentifier) {
                                DERIA5String uri = (DERIA5String) name.getName();
                                String crlUrl = uri.getString();
                                if (crlUrl.startsWith("http://") || crlUrl.startsWith("https://")) {
                                    crlUrls.add(crlUrl);
                                }
                            }
                        }
                    }
                }
                
                innerStream.close();
            }
            
            asn1Stream.close();
            
        } catch (Exception e) {
            logger.debug("Failed to extract CRL distribution points from certificate", e);
        }
        
        return crlUrls;
    }
    
    /**
     * Cache the CRL with appropriate TTL.
     */
    private static void cacheCrl(String crlUrl, X509CRL crl) {
        try {
            Date nextUpdate = crl.getNextUpdate();
            Instant expiry;
            
            if (nextUpdate != null) {
                // Cache until nextUpdate with a 5-minute buffer
                expiry = nextUpdate.toInstant().minusSeconds(300);
            } else {
                // Default cache time of 1 hour if no nextUpdate
                expiry = Instant.now().plusSeconds(3600);
            }
            
            crlCache.put(crlUrl, new CachedCrl(crl, expiry));
            
            logger.debug("CRL cached [url={}, expiry={}]", crlUrl, expiry);
            
            // Clean up cache if it gets too large
            if (crlCache.size() > MAX_CACHE_SIZE) {
                cleanupCache();
            }
            
            // Periodic cache cleanup
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCacheCleanup > CACHE_CLEANUP_INTERVAL_MS) {
                cleanupCache();
                lastCacheCleanup = currentTime;
            }
            
        } catch (Exception e) {
            logger.warn("Failed to cache CRL [url={}]", crlUrl, e);
        }
    }
    
    /**
     * Clean up expired entries from the CRL cache.
     */
    private static void cleanupCache() {
        Instant now = Instant.now();
        int initialSize = crlCache.size();
        
        crlCache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
        
        int finalSize = crlCache.size();
        logger.debug("CRL cache cleanup completed, removed {} entries, remaining: {}", 
            initialSize - finalSize, finalSize);
    }
    
    /**
     * Generate correlation ID for request tracking.
     */
    private static String generateCorrelationId() {
        return "crl-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString(Thread.currentThread().hashCode());
    }
    
    /**
     * Get CRL cache statistics for monitoring.
     */
    public static Map<String, Object> getCacheStats() {
        int totalEntries = crlCache.size();
        long expiredEntries = crlCache.values().stream()
            .mapToLong(entry -> entry.isExpired() ? 1 : 0)
            .sum();
        
        return Map.of(
            "totalEntries", totalEntries,
            "expiredEntries", (int) expiredEntries,
            "maxCacheSize", MAX_CACHE_SIZE
        );
    }
    
    /**
     * Clear the CRL cache.
     */
    public static void clearCache() {
        crlCache.clear();
        logger.info("CRL cache cleared");
    }
    
    /**
     * Cached CRL with expiry time.
     */
    private static class CachedCrl {
        private final X509CRL crl;
        private final Instant expiry;
        
        public CachedCrl(X509CRL crl, Instant expiry) {
            this.crl = crl;
            this.expiry = expiry;
        }
        
        public X509CRL getCrl() {
            return crl;
        }
        
        public boolean isExpired() {
            return isExpired(Instant.now());
        }
        
        public boolean isExpired(Instant now) {
            return expiry != null && now.isAfter(expiry);
        }
    }
}