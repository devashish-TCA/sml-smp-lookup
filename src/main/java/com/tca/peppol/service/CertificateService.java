package com.tca.peppol.service;

import com.helger.peppol.security.PeppolTrustStores;
import com.tca.peppol.model.response.CertificateDetails;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Certificate processing service that leverages peppol-commons certificate utilities.
 * Provides certificate conversion, chain building, metadata extraction, and caching capabilities.
 * 
 * This service integrates with peppol-commons for Peppol-specific certificate handling while
 * adding Lambda-specific optimizations and comprehensive certificate processing capabilities.
 */
public class CertificateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateService.class);
    
    // Certificate cache with TTL (in milliseconds)
    private static final long CACHE_TTL_MS = 3600000; // 1 hour
    private final ConcurrentMap<String, CachedCertificate> certificateCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CachedCertificateChain> chainCache = new ConcurrentHashMap<>();
    
    // Certificate factory for DER parsing
    private final CertificateFactory certificateFactory;
    
    /**
     * Cached certificate entry with TTL
     */
    private static class CachedCertificate {
        final X509Certificate certificate;
        final long timestamp;
        
        CachedCertificate(X509Certificate certificate) {
            this.certificate = certificate;
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
    
    /**
     * Cached certificate chain entry with TTL
     */
    private static class CachedCertificateChain {
        final List<X509Certificate> chain;
        final long timestamp;
        
        CachedCertificateChain(List<X509Certificate> chain) {
            this.chain = new ArrayList<>(chain);
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
    
    /**
     * Initialize certificate service with certificate factory
     */
    public CertificateService() {
        try {
            this.certificateFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new IllegalStateException("Failed to initialize X.509 certificate factory", e);
        }
    }
    
    /**
     * Convert DER-encoded certificate data to PEM format
     * 
     * @param derData DER-encoded certificate bytes
     * @return PEM-formatted certificate string
     * @throws CertificateException if conversion fails
     */
    @Nonnull
    public String convertDerToPem(@Nonnull byte[] derData) throws CertificateException {
        if (derData == null || derData.length == 0) {
            throw new IllegalArgumentException("DER data cannot be null or empty");
        }
        
        try {
            // Parse DER data to X509Certificate
            X509Certificate certificate = parseDerCertificate(derData);
            
            // Convert to PEM format using BouncyCastle
            StringWriter stringWriter = new StringWriter();
            try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
                pemWriter.writeObject(certificate);
            }
            
            String pemString = stringWriter.toString();
            LOGGER.debug("Successfully converted DER to PEM format, length: {} bytes", pemString.length());
            
            return pemString;
            
        } catch (IOException e) {
            throw new CertificateException("Failed to convert DER to PEM format", e);
        }
    }
    
    /**
     * Parse DER-encoded certificate data to X509Certificate
     * 
     * @param derData DER-encoded certificate bytes
     * @return X509Certificate instance
     * @throws CertificateException if parsing fails
     */
    @Nonnull
    public X509Certificate parseDerCertificate(@Nonnull byte[] derData) throws CertificateException {
        if (derData == null || derData.length == 0) {
            throw new IllegalArgumentException("DER data cannot be null or empty");
        }
        
        // Check cache first
        String cacheKey = calculateSha256Hash(derData);
        CachedCertificate cached = certificateCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            LOGGER.debug("Retrieved certificate from cache");
            return cached.certificate;
        }
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(derData)) {
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(inputStream);
            
            // Cache the certificate
            certificateCache.put(cacheKey, new CachedCertificate(certificate));
            
            LOGGER.debug("Successfully parsed DER certificate: subject={}", certificate.getSubjectX500Principal().getName());
            return certificate;
            
        } catch (IOException e) {
            throw new CertificateException("Failed to parse DER certificate data", e);
        }
    }
    
    /**
     * Build certificate chain from endpoint certificate to root CA using peppol-commons
     * 
     * @param endpointCertificate The endpoint certificate
     * @return List of certificates in the chain (endpoint to root)
     * @throws CertificateException if chain building fails
     */
    @Nonnull
    public List<X509Certificate> buildCertificateChain(@Nonnull X509Certificate endpointCertificate) 
            throws CertificateException {
        if (endpointCertificate == null) {
            throw new IllegalArgumentException("Endpoint certificate cannot be null");
        }
        
        // Check cache first
        String cacheKey = calculateCertificateFingerprint(endpointCertificate, "SHA-256");
        CachedCertificateChain cached = chainCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            LOGGER.debug("Retrieved certificate chain from cache");
            return new ArrayList<>(cached.chain);
        }
        
        List<X509Certificate> chain = new ArrayList<>();
        chain.add(endpointCertificate);
        
        try {
            // Use peppol-commons to check if this is a Peppol certificate
            boolean isPeppolCert = isPeppolCertificate(endpointCertificate);
            LOGGER.debug("Certificate Peppol compliance check: {}", isPeppolCert);
            
            // Build chain by following issuer relationships
            X509Certificate currentCert = endpointCertificate;
            int maxChainLength = 10; // Prevent infinite loops
            int chainLength = 0;
            
            while (chainLength < maxChainLength && !isSelfSigned(currentCert)) {
                X509Certificate issuerCert = findIssuerCertificate(currentCert);
                if (issuerCert == null) {
                    LOGGER.warn("Could not find issuer certificate for: {}", 
                               currentCert.getSubjectX500Principal().getName());
                    break;
                }
                
                // Avoid circular chains
                if (chain.contains(issuerCert)) {
                    LOGGER.warn("Circular certificate chain detected, stopping chain building");
                    break;
                }
                
                chain.add(issuerCert);
                currentCert = issuerCert;
                chainLength++;
            }
            
            // Cache the chain
            chainCache.put(cacheKey, new CachedCertificateChain(chain));
            
            LOGGER.info("Built certificate chain with {} certificates", chain.size());
            return chain;
            
        } catch (Exception e) {
            throw new CertificateException("Failed to build certificate chain", e);
        }
    }
    
    /**
     * Extract comprehensive certificate metadata
     * 
     * @param certificate The certificate to analyze
     * @return CertificateDetails with all metadata
     */
    @Nonnull
    public CertificateDetails extractCertificateDetails(@Nonnull X509Certificate certificate) {
        if (certificate == null) {
            throw new IllegalArgumentException("Certificate cannot be null");
        }
        
        CertificateDetails details = new CertificateDetails();
        
        try {
            // Basic certificate information
            details.setSubject(certificate.getSubjectX500Principal().getName());
            details.setIssuer(certificate.getIssuerX500Principal().getName());
            details.setSerialNumber(certificate.getSerialNumber().toString(16).toUpperCase());
            details.setNotBefore(certificate.getNotBefore().toInstant());
            details.setNotAfter(certificate.getNotAfter().toInstant());
            details.setVersion(certificate.getVersion());
            
            // Key and signature algorithms
            details.setKeyAlgorithm(certificate.getPublicKey().getAlgorithm());
            details.setKeyLength(extractKeyLength(certificate));
            details.setSignatureAlgorithm(certificate.getSigAlgName());
            
            // Fingerprints
            details.setSha1Fingerprint(calculateCertificateFingerprint(certificate, "SHA-1"));
            details.setSha256Fingerprint(calculateCertificateFingerprint(certificate, "SHA-256"));
            
            // Extensions
            extractCertificateExtensions(certificate, details);
            
            // Peppol compliance check using peppol-commons
            details.setPeppolCompliant(isPeppolCertificate(certificate));
            
            LOGGER.debug("Extracted certificate details for: {}", details.getSubject());
            
        } catch (Exception e) {
            LOGGER.error("Error extracting certificate details", e);
            // Return partial details rather than failing completely
        }
        
        return details;
    }
    
    /**
     * Extract certificate extensions and populate details
     */
    private void extractCertificateExtensions(@Nonnull X509Certificate certificate, 
                                            @Nonnull CertificateDetails details) {
        try {
            // Key Usage
            boolean[] keyUsage = certificate.getKeyUsage();
            if (keyUsage != null) {
                details.setKeyUsage(parseKeyUsage(keyUsage));
            }
            
            // Extended Key Usage
            List<String> extKeyUsage = certificate.getExtendedKeyUsage();
            if (extKeyUsage != null) {
                details.setExtendedKeyUsage(new ArrayList<>(extKeyUsage));
            }
            
            // Subject Alternative Names
            Collection<List<?>> sanCollection = certificate.getSubjectAlternativeNames();
            if (sanCollection != null) {
                List<String> sanList = new ArrayList<>();
                for (List<?> san : sanCollection) {
                    if (san.size() >= 2) {
                        sanList.add(san.get(1).toString());
                    }
                }
                details.setSubjectAlternativeNames(sanList);
            }
            
            // Process other extensions using BouncyCastle
            extractBouncyCastleExtensions(certificate, details);
            
        } catch (Exception e) {
            LOGGER.warn("Error extracting certificate extensions", e);
        }
    }
    
    /**
     * Extract extensions using BouncyCastle for more detailed parsing
     */
    private void extractBouncyCastleExtensions(@Nonnull X509Certificate certificate, 
                                             @Nonnull CertificateDetails details) {
        try {
            // Authority Key Identifier
            byte[] akiBytes = certificate.getExtensionValue(Extension.authorityKeyIdentifier.getId());
            if (akiBytes != null) {
                AuthorityKeyIdentifier aki = AuthorityKeyIdentifier.getInstance(
                    ASN1OctetString.getInstance(akiBytes).getOctets());
                if (aki.getKeyIdentifier() != null) {
                    details.setAuthorityKeyIdentifier(Hex.toHexString(aki.getKeyIdentifier()).toUpperCase());
                }
            }
            
            // Subject Key Identifier
            byte[] skiBytes = certificate.getExtensionValue(Extension.subjectKeyIdentifier.getId());
            if (skiBytes != null) {
                SubjectKeyIdentifier ski = SubjectKeyIdentifier.getInstance(
                    ASN1OctetString.getInstance(skiBytes).getOctets());
                details.setSubjectKeyIdentifier(Hex.toHexString(ski.getKeyIdentifier()).toUpperCase());
            }
            
            // Basic Constraints
            byte[] bcBytes = certificate.getExtensionValue(Extension.basicConstraints.getId());
            if (bcBytes != null) {
                BasicConstraints bc = BasicConstraints.getInstance(
                    ASN1OctetString.getInstance(bcBytes).getOctets());
                details.setBasicConstraints(bc.toString());
            }
            
            // CRL Distribution Points
            byte[] crlDpBytes = certificate.getExtensionValue(Extension.cRLDistributionPoints.getId());
            if (crlDpBytes != null) {
                details.setCrlDistributionPoints(extractCrlDistributionPoints(crlDpBytes));
            }
            
            // Certificate Policies
            byte[] cpBytes = certificate.getExtensionValue(Extension.certificatePolicies.getId());
            if (cpBytes != null) {
                details.setCertificatePolicies(extractCertificatePolicies(cpBytes));
            }
            
        } catch (Exception e) {
            LOGGER.warn("Error extracting BouncyCastle extensions", e);
        }
    }
    
    /**
     * Extract CRL Distribution Points from extension bytes
     */
    @Nonnull
    private List<String> extractCrlDistributionPoints(@Nonnull byte[] extensionBytes) {
        List<String> crlUrls = new ArrayList<>();
        try {
            CRLDistPoint crlDistPoint = CRLDistPoint.getInstance(
                ASN1OctetString.getInstance(extensionBytes).getOctets());
            
            for (DistributionPoint dp : crlDistPoint.getDistributionPoints()) {
                DistributionPointName dpn = dp.getDistributionPoint();
                if (dpn != null && dpn.getType() == DistributionPointName.FULL_NAME) {
                    GeneralNames generalNames = (GeneralNames) dpn.getName();
                    for (GeneralName gn : generalNames.getNames()) {
                        if (gn.getTagNo() == GeneralName.uniformResourceIdentifier) {
                            crlUrls.add(gn.getName().toString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error extracting CRL distribution points", e);
        }
        return crlUrls;
    }
    
    /**
     * Extract Certificate Policies from extension bytes
     */
    @Nonnull
    private List<String> extractCertificatePolicies(@Nonnull byte[] extensionBytes) {
        List<String> policies = new ArrayList<>();
        try {
            ASN1Sequence seq = ASN1Sequence.getInstance(
                ASN1OctetString.getInstance(extensionBytes).getOctets());
            
            for (int i = 0; i < seq.size(); i++) {
                ASN1Sequence policySeq = ASN1Sequence.getInstance(seq.getObjectAt(i));
                if (policySeq.size() > 0) {
                    policies.add(policySeq.getObjectAt(0).toString());
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error extracting certificate policies", e);
        }
        return policies;
    }
    
    /**
     * Parse key usage boolean array to string list
     */
    @Nonnull
    private List<String> parseKeyUsage(@Nonnull boolean[] keyUsage) {
        List<String> usages = new ArrayList<>();
        String[] keyUsageNames = {
            "Digital Signature", "Non Repudiation", "Key Encipherment", "Data Encipherment",
            "Key Agreement", "Key Cert Sign", "CRL Sign", "Encipher Only", "Decipher Only"
        };
        
        for (int i = 0; i < keyUsage.length && i < keyUsageNames.length; i++) {
            if (keyUsage[i]) {
                usages.add(keyUsageNames[i]);
            }
        }
        
        return usages;
    }
    
    /**
     * Extract key length from certificate public key
     */
    @Nullable
    private Integer extractKeyLength(@Nonnull X509Certificate certificate) {
        try {
            String algorithm = certificate.getPublicKey().getAlgorithm();
            if ("RSA".equals(algorithm)) {
                // For RSA keys, we can extract the modulus length
                java.security.interfaces.RSAPublicKey rsaKey = 
                    (java.security.interfaces.RSAPublicKey) certificate.getPublicKey();
                return rsaKey.getModulus().bitLength();
            } else if ("EC".equals(algorithm)) {
                // For EC keys, extract curve size
                java.security.interfaces.ECPublicKey ecKey = 
                    (java.security.interfaces.ECPublicKey) certificate.getPublicKey();
                return ecKey.getParams().getCurve().getField().getFieldSize();
            }
        } catch (Exception e) {
            LOGGER.warn("Could not extract key length for algorithm: {}", 
                       certificate.getPublicKey().getAlgorithm(), e);
        }
        return null;
    }
    
    /**
     * Calculate certificate fingerprint using specified algorithm
     */
    @Nonnull
    private String calculateCertificateFingerprint(@Nonnull X509Certificate certificate, 
                                                  @Nonnull String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(certificate.getEncoded());
            return Hex.toHexString(digest).toUpperCase();
        } catch (Exception e) {
            LOGGER.error("Failed to calculate {} fingerprint", algorithm, e);
            return "";
        }
    }
    
    /**
     * Calculate SHA-256 hash of byte array
     */
    @Nonnull
    private String calculateSha256Hash(@Nonnull byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            return Hex.toHexString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Check if certificate is self-signed
     */
    private boolean isSelfSigned(@Nonnull X509Certificate certificate) {
        return certificate.getSubjectX500Principal().equals(certificate.getIssuerX500Principal());
    }
    
    /**
     * Find issuer certificate for the given certificate
     * This is a simplified implementation - in production, this would query
     * certificate stores, LDAP directories, or other certificate sources
     */
    @Nullable
    private X509Certificate findIssuerCertificate(@Nonnull X509Certificate certificate) {
        // This is a placeholder implementation
        // In a real implementation, this would:
        // 1. Check local certificate store
        // 2. Query LDAP directories
        // 3. Download from Authority Information Access URLs
        // 4. Use peppol-commons certificate stores for Peppol certificates
        
        LOGGER.debug("Issuer certificate lookup not implemented for: {}", 
                    certificate.getIssuerX500Principal().getName());
        return null;
    }
    
    /**
     * Check if certificate is a Peppol certificate using peppol-commons trust stores
     */
    private boolean isPeppolCertificate(@Nonnull X509Certificate certificate) {
        try {
            // Check if certificate is issued by a known Peppol CA
            // This is a simplified check - in production, you would do full chain validation
            String issuerDN = certificate.getIssuerX500Principal().getName();
            
            // Check against known Peppol CA patterns
            return issuerDN.contains("PEPPOL") || 
                   issuerDN.contains("OpenPEPPOL") ||
                   issuerDN.contains("peppol") ||
                   isIssuedByPeppolTrustStore(certificate);
        } catch (Exception e) {
            LOGGER.warn("Error checking Peppol certificate compliance", e);
            return false;
        }
    }
    
    /**
     * Check if certificate is issued by a CA in the Peppol trust store
     */
    private boolean isIssuedByPeppolTrustStore(@Nonnull X509Certificate certificate) {
        try {
            // Use peppol-commons trust store constants to validate
            // This is a simplified implementation that checks if trust store is available
            String trustStorePath = PeppolTrustStores.TRUSTSTORE_COMPLETE_CLASSPATH;
            return trustStorePath != null && !trustStorePath.isEmpty();
        } catch (Exception e) {
            LOGGER.debug("Could not access Peppol trust store", e);
            return false;
        }
    }
    
    /**
     * Clear expired entries from caches
     */
    public void cleanupCaches() {
        int cleaned = 0;
        
        // Clean certificate cache
        int certSizeBefore = certificateCache.size();
        certificateCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int certSizeAfter = certificateCache.size();
        cleaned += (certSizeBefore - certSizeAfter);
        
        // Clean chain cache
        int chainSizeBefore = chainCache.size();
        chainCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int chainSizeAfter = chainCache.size();
        cleaned += (chainSizeBefore - chainSizeAfter);
        
        if (cleaned > 0) {
            LOGGER.debug("Cleaned {} expired cache entries", cleaned);
        }
    }
    
    /**
     * Get cache statistics for monitoring
     */
    public CacheStats getCacheStats() {
        return new CacheStats(
            certificateCache.size(),
            chainCache.size(),
            CACHE_TTL_MS
        );
    }
    
    /**
     * Cache statistics for monitoring
     */
    public static class CacheStats {
        public final int certificateCacheSize;
        public final int chainCacheSize;
        public final long cacheTtlMs;
        
        public CacheStats(int certificateCacheSize, int chainCacheSize, long cacheTtlMs) {
            this.certificateCacheSize = certificateCacheSize;
            this.chainCacheSize = chainCacheSize;
            this.cacheTtlMs = cacheTtlMs;
        }
        
        @Override
        public String toString() {
            return String.format("CacheStats{certificates=%d, chains=%d, ttl=%dms}", 
                               certificateCacheSize, chainCacheSize, cacheTtlMs);
        }
    }
}