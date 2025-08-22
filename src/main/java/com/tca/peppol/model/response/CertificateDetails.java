package com.tca.peppol.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/**
 * Certificate details model containing comprehensive certificate metadata.
 * Provides information about certificate subject, issuer, validity dates,
 * key algorithm, fingerprints, and other certificate-specific details.
 */
public class CertificateDetails {

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("issuer")
    private String issuer;

    @JsonProperty("serialNumber")
    private String serialNumber;

    @JsonProperty("notBefore")
    private Instant notBefore;

    @JsonProperty("notAfter")
    private Instant notAfter;

    @JsonProperty("keyAlgorithm")
    private String keyAlgorithm;

    @JsonProperty("keyLength")
    private Integer keyLength;

    @JsonProperty("signatureAlgorithm")
    private String signatureAlgorithm;

    @JsonProperty("sha1Fingerprint")
    private String sha1Fingerprint;

    @JsonProperty("sha256Fingerprint")
    private String sha256Fingerprint;

    @JsonProperty("version")
    private Integer version;

    @JsonProperty("keyUsage")
    private List<String> keyUsage;

    @JsonProperty("extendedKeyUsage")
    private List<String> extendedKeyUsage;

    @JsonProperty("subjectAlternativeNames")
    private List<String> subjectAlternativeNames;

    @JsonProperty("certificatePolicies")
    private List<String> certificatePolicies;

    @JsonProperty("crlDistributionPoints")
    private List<String> crlDistributionPoints;

    @JsonProperty("ocspUrls")
    private List<String> ocspUrls;

    @JsonProperty("authorityKeyIdentifier")
    private String authorityKeyIdentifier;

    @JsonProperty("subjectKeyIdentifier")
    private String subjectKeyIdentifier;

    @JsonProperty("basicConstraints")
    private String basicConstraints;

    @JsonProperty("peppolCompliant")
    private boolean peppolCompliant;

    @JsonProperty("daysUntilExpiry")
    private Long daysUntilExpiry;

    // Default constructor
    public CertificateDetails() {}

    // Constructor with basic fields
    public CertificateDetails(String subject, String issuer, String serialNumber, 
                            Instant notBefore, Instant notAfter) {
        this.subject = subject;
        this.issuer = issuer;
        this.serialNumber = serialNumber;
        this.notBefore = notBefore;
        this.notAfter = notAfter;
        this.daysUntilExpiry = calculateDaysUntilExpiry(notAfter);
    }

    // Getters and setters
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Instant getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Instant notBefore) {
        this.notBefore = notBefore;
    }

    public Instant getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(Instant notAfter) {
        this.notAfter = notAfter;
        this.daysUntilExpiry = calculateDaysUntilExpiry(notAfter);
    }

    public String getKeyAlgorithm() {
        return keyAlgorithm;
    }

    public void setKeyAlgorithm(String keyAlgorithm) {
        this.keyAlgorithm = keyAlgorithm;
    }

    public Integer getKeyLength() {
        return keyLength;
    }

    public void setKeyLength(Integer keyLength) {
        this.keyLength = keyLength;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public String getSha1Fingerprint() {
        return sha1Fingerprint;
    }

    public void setSha1Fingerprint(String sha1Fingerprint) {
        this.sha1Fingerprint = sha1Fingerprint;
    }

    public String getSha256Fingerprint() {
        return sha256Fingerprint;
    }

    public void setSha256Fingerprint(String sha256Fingerprint) {
        this.sha256Fingerprint = sha256Fingerprint;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public List<String> getKeyUsage() {
        return keyUsage;
    }

    public void setKeyUsage(List<String> keyUsage) {
        this.keyUsage = keyUsage;
    }

    public List<String> getExtendedKeyUsage() {
        return extendedKeyUsage;
    }

    public void setExtendedKeyUsage(List<String> extendedKeyUsage) {
        this.extendedKeyUsage = extendedKeyUsage;
    }

    public List<String> getSubjectAlternativeNames() {
        return subjectAlternativeNames;
    }

    public void setSubjectAlternativeNames(List<String> subjectAlternativeNames) {
        this.subjectAlternativeNames = subjectAlternativeNames;
    }

    public List<String> getCertificatePolicies() {
        return certificatePolicies;
    }

    public void setCertificatePolicies(List<String> certificatePolicies) {
        this.certificatePolicies = certificatePolicies;
    }

    public List<String> getCrlDistributionPoints() {
        return crlDistributionPoints;
    }

    public void setCrlDistributionPoints(List<String> crlDistributionPoints) {
        this.crlDistributionPoints = crlDistributionPoints;
    }

    public List<String> getOcspUrls() {
        return ocspUrls;
    }

    public void setOcspUrls(List<String> ocspUrls) {
        this.ocspUrls = ocspUrls;
    }

    public String getAuthorityKeyIdentifier() {
        return authorityKeyIdentifier;
    }

    public void setAuthorityKeyIdentifier(String authorityKeyIdentifier) {
        this.authorityKeyIdentifier = authorityKeyIdentifier;
    }

    public String getSubjectKeyIdentifier() {
        return subjectKeyIdentifier;
    }

    public void setSubjectKeyIdentifier(String subjectKeyIdentifier) {
        this.subjectKeyIdentifier = subjectKeyIdentifier;
    }

    public String getBasicConstraints() {
        return basicConstraints;
    }

    public void setBasicConstraints(String basicConstraints) {
        this.basicConstraints = basicConstraints;
    }

    public boolean isPeppolCompliant() {
        return peppolCompliant;
    }

    public void setPeppolCompliant(boolean peppolCompliant) {
        this.peppolCompliant = peppolCompliant;
    }

    public Long getDaysUntilExpiry() {
        return daysUntilExpiry;
    }

    public void setDaysUntilExpiry(Long daysUntilExpiry) {
        this.daysUntilExpiry = daysUntilExpiry;
    }

    /**
     * Calculate days until certificate expiry
     */
    private Long calculateDaysUntilExpiry(Instant notAfter) {
        if (notAfter == null) {
            return null;
        }
        long secondsUntilExpiry = notAfter.getEpochSecond() - Instant.now().getEpochSecond();
        return secondsUntilExpiry / (24 * 60 * 60); // Convert to days
    }

    /**
     * Check if certificate is currently valid (not expired and not before validity period)
     */
    public boolean isCurrentlyValid() {
        Instant now = Instant.now();
        return notBefore != null && notAfter != null && 
               now.isAfter(notBefore) && now.isBefore(notAfter);
    }

    /**
     * Check if certificate is expiring soon (within specified days)
     */
    public boolean isExpiringSoon(int days) {
        return daysUntilExpiry != null && daysUntilExpiry <= days && daysUntilExpiry > 0;
    }

    @Override
    public String toString() {
        return "CertificateDetails{" +
                "subject='" + subject + '\'' +
                ", issuer='" + issuer + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                ", notBefore=" + notBefore +
                ", notAfter=" + notAfter +
                ", keyAlgorithm='" + keyAlgorithm + '\'' +
                ", keyLength=" + keyLength +
                ", signatureAlgorithm='" + signatureAlgorithm + '\'' +
                ", sha1Fingerprint='" + sha1Fingerprint + '\'' +
                ", sha256Fingerprint='" + sha256Fingerprint + '\'' +
                ", version=" + version +
                ", peppolCompliant=" + peppolCompliant +
                ", daysUntilExpiry=" + daysUntilExpiry +
                '}';
    }
}