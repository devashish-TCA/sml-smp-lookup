package com.tca.peppol.service;

import com.tca.peppol.model.response.CertificateDetails;
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

/**
 * Simplified certificate processing service for Peppol lookup operations.
 * Focuses on essential certificate operations needed for SML/SMP workflows:
 * - DER to PEM conversion
 * - Basic certificate parsing
 * - Essential metadata extraction
 * - Simple Peppol compliance checking
 */
public class CertificateService {

    private static final Logger logger = LoggerFactory.getLogger(CertificateService.class);

    private final CertificateFactory certificateFactory;

    /**
     * Initialize certificate service
     */
    public CertificateService() {
        try {
            this.certificateFactory = CertificateFactory.getInstance("X.509");
            logger.debug("CertificateService initialized");
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
            X509Certificate certificate = parseDerCertificate(derData);

            StringWriter stringWriter = new StringWriter();
            try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
                pemWriter.writeObject(certificate);
            }

            String pemString = stringWriter.toString();
            logger.debug("Successfully converted DER to PEM format, length: {} bytes", pemString.length());

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

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(derData)) {
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(inputStream);
            logger.debug("Successfully parsed DER certificate: subject={}", certificate.getSubjectX500Principal().getName());
            return certificate;
        } catch (IOException e) {
            throw new CertificateException("Failed to parse DER certificate data", e);
        }
    }

    /**
     * Extract essential certificate details for Peppol operations
     *
     * @param certificate The certificate to analyze
     * @return CertificateDetails with essential metadata
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

            // Key and signature information
            details.setKeyAlgorithm(certificate.getPublicKey().getAlgorithm());
            details.setKeyLength(extractKeyLength(certificate));
            details.setSignatureAlgorithm(certificate.getSigAlgName());

            // Certificate fingerprints
            details.setSha1Fingerprint(calculateFingerprint(certificate, "SHA-1"));
            details.setSha256Fingerprint(calculateFingerprint(certificate, "SHA-256"));

            // Key usage information
            boolean[] keyUsage = certificate.getKeyUsage();
            if (keyUsage != null) {
                details.setKeyUsage(parseKeyUsage(keyUsage));
            }

            // Extended key usage
            List<String> extKeyUsage = certificate.getExtendedKeyUsage();
            if (extKeyUsage != null) {
                details.setExtendedKeyUsage(new ArrayList<>(extKeyUsage));
            }

            // Subject Alternative Names (important for Peppol)
            details.setSubjectAlternativeNames(extractSubjectAlternativeNames(certificate));

            // Basic Peppol compliance check
            details.setPeppolCompliant(isPeppolCertificate(certificate));

            logger.debug("Extracted certificate details for: {}", details.getSubject());

        } catch (Exception e) {
            logger.warn("Error extracting certificate details: {}", e.getMessage());
            // Return partial details rather than failing completely
        }

        return details;
    }

    /**
     * Extract key length from certificate public key
     */
    @Nullable
    private Integer extractKeyLength(@Nonnull X509Certificate certificate) {
        try {
            String algorithm = certificate.getPublicKey().getAlgorithm();

            if ("RSA".equals(algorithm)) {
                java.security.interfaces.RSAPublicKey rsaKey =
                        (java.security.interfaces.RSAPublicKey) certificate.getPublicKey();
                return rsaKey.getModulus().bitLength();
            } else if ("EC".equals(algorithm)) {
                java.security.interfaces.ECPublicKey ecKey =
                        (java.security.interfaces.ECPublicKey) certificate.getPublicKey();
                return ecKey.getParams().getCurve().getField().getFieldSize();
            } else if ("DSA".equals(algorithm)) {
                java.security.interfaces.DSAPublicKey dsaKey =
                        (java.security.interfaces.DSAPublicKey) certificate.getPublicKey();
                return dsaKey.getParams().getP().bitLength();
            }

        } catch (Exception e) {
            logger.debug("Could not extract key length for algorithm: {}",
                    certificate.getPublicKey().getAlgorithm());
        }

        return null;
    }

    /**
     * Calculate certificate fingerprint using specified algorithm
     */
    @Nonnull
    private String calculateFingerprint(@Nonnull X509Certificate certificate, @Nonnull String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(certificate.getEncoded());
            return Hex.toHexString(digest).toUpperCase();
        } catch (Exception e) {
            logger.warn("Failed to calculate {} fingerprint: {}", algorithm, e.getMessage());
            return "";
        }
    }

    /**
     * Parse key usage boolean array to human-readable list
     */
    @Nonnull
    private List<String> parseKeyUsage(@Nonnull boolean[] keyUsage) {
        List<String> usages = new ArrayList<>();

        String[] keyUsageNames = {
                "Digital Signature",    // 0
                "Non Repudiation",      // 1
                "Key Encipherment",     // 2
                "Data Encipherment",    // 3
                "Key Agreement",        // 4
                "Key Cert Sign",        // 5
                "CRL Sign",             // 6
                "Encipher Only",        // 7
                "Decipher Only"         // 8
        };

        for (int i = 0; i < keyUsage.length && i < keyUsageNames.length; i++) {
            if (keyUsage[i]) {
                usages.add(keyUsageNames[i]);
            }
        }

        return usages;
    }

    /**
     * Extract Subject Alternative Names from certificate
     */
    @Nonnull
    private List<String> extractSubjectAlternativeNames(@Nonnull X509Certificate certificate) {
        List<String> sanList = new ArrayList<>();

        try {
            Collection<List<?>> sanCollection = certificate.getSubjectAlternativeNames();
            if (sanCollection != null) {
                for (List<?> san : sanCollection) {
                    if (san.size() >= 2) {
                        // Format: [type, value]
                        Integer type = (Integer) san.get(0);
                        String value = san.get(1).toString();

                        // Add type prefix for clarity
                        String typePrefix = switch (type) {
                            case 1 -> "email:";
                            case 2 -> "dns:";
                            case 4 -> "dn:";
                            case 6 -> "uri:";
                            case 7 -> "ip:";
                            default -> "other(" + type + "):";
                        };

                        sanList.add(typePrefix + value);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error extracting Subject Alternative Names: {}", e.getMessage());
        }

        return sanList;
    }

    /**
     * Simple Peppol certificate compliance check
     * This is a basic check - in production you'd want more sophisticated validation
     */
    private boolean isPeppolCertificate(@Nonnull X509Certificate certificate) {
        try {
            String issuerDN = certificate.getIssuerX500Principal().getName();
            String subjectDN = certificate.getSubjectX500Principal().getName();

            // Check for known Peppol CA patterns in issuer
            boolean hasKnownIssuer = issuerDN.toLowerCase().contains("peppol") ||
                    issuerDN.toLowerCase().contains("openpeppol") ||
                    issuerDN.toLowerCase().contains("openpeppol test ca") ||
                    issuerDN.toLowerCase().contains("openpeppol production ca");

            // Check for Peppol participant identifier in subject
            boolean hasParticipantId = subjectDN.toLowerCase().contains("serialnumber=") &&
                    (subjectDN.contains("::") || subjectDN.contains("iso6523-actorid-upis"));

            // Check validity period (Peppol certificates are typically short-lived)
            long validityPeriodDays = (certificate.getNotAfter().getTime() -
                    certificate.getNotBefore().getTime()) / (24 * 60 * 60 * 1000);
            boolean reasonableValidityPeriod = validityPeriodDays <= (5 * 365); // Max 5 years

            boolean isPeppol = hasKnownIssuer && hasParticipantId && reasonableValidityPeriod;

            logger.debug("Peppol compliance check: issuer={}, participant={}, validity={}, overall={}",
                    hasKnownIssuer, hasParticipantId, reasonableValidityPeriod, isPeppol);

            return isPeppol;

        } catch (Exception e) {
            logger.warn("Error checking Peppol certificate compliance: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if certificate is currently valid (not expired and not not-yet-valid)
     */
    public boolean isCurrentlyValid(@Nonnull X509Certificate certificate) {
        try {
            certificate.checkValidity();
            return true;
        } catch (Exception e) {
            logger.debug("Certificate validity check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get days until certificate expiration (negative if already expired)
     */
    public long getDaysUntilExpiration(@Nonnull X509Certificate certificate) {
        long now = System.currentTimeMillis();
        long expiryTime = certificate.getNotAfter().getTime();
        return (expiryTime - now) / (24 * 60 * 60 * 1000);
    }

    /**
     * Calculate SHA-256 hash of byte array for internal use
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
}