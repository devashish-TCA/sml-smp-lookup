package com.tca.peppol.validation;

import java.security.cert.X509Certificate;

/**
 * Result of XML signature validation containing detailed validation information.
 * 
 * This class provides comprehensive information about the XML signature validation process,
 * including individual validation steps and their results.
 */
public class XmlSignatureValidationResult {
    
    private final boolean signaturePresent;
    private final boolean valid;
    private final boolean signatureAlgorithmValid;
    private final boolean canonicalizationValid;
    private final boolean referencesValid;
    private final boolean keyInfoValid;
    private final boolean certificateMatches;
    private final boolean cryptographicSignatureValid;
    private final X509Certificate signingCertificate;
    private final String signatureAlgorithm;
    private final String canonicalizationAlgorithm;
    private final String errorMessage;
    
    private XmlSignatureValidationResult(Builder builder) {
        this.signaturePresent = builder.signaturePresent;
        this.valid = builder.valid;
        this.signatureAlgorithmValid = builder.signatureAlgorithmValid;
        this.canonicalizationValid = builder.canonicalizationValid;
        this.referencesValid = builder.referencesValid;
        this.keyInfoValid = builder.keyInfoValid;
        this.certificateMatches = builder.certificateMatches;
        this.cryptographicSignatureValid = builder.cryptographicSignatureValid;
        this.signingCertificate = builder.signingCertificate;
        this.signatureAlgorithm = builder.signatureAlgorithm;
        this.canonicalizationAlgorithm = builder.canonicalizationAlgorithm;
        this.errorMessage = builder.errorMessage;
    }
    
    /**
     * @return true if XML signature element is present in the document
     */
    public boolean isSignaturePresent() {
        return signaturePresent;
    }
    
    /**
     * @return true if the XML signature is valid and all validation checks passed
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * @return true if the signature algorithm is approved (RSA-SHA256 or stronger)
     */
    public boolean isSignatureAlgorithmValid() {
        return signatureAlgorithmValid;
    }
    
    /**
     * @return true if the canonicalization algorithm is valid (Canonical XML 1.0)
     */
    public boolean isCanonicalizationValid() {
        return canonicalizationValid;
    }
    
    /**
     * @return true if all signature references are valid
     */
    public boolean isReferencesValid() {
        return referencesValid;
    }
    
    /**
     * @return true if the key info is valid and certificate was extracted
     */
    public boolean isKeyInfoValid() {
        return keyInfoValid;
    }
    
    /**
     * @return true if the signing certificate matches the expected certificate
     */
    public boolean isCertificateMatches() {
        return certificateMatches;
    }
    
    /**
     * @return true if the cryptographic signature verification passed
     */
    public boolean isCryptographicSignatureValid() {
        return cryptographicSignatureValid;
    }
    
    /**
     * @return the certificate used to sign the XML, or null if not available
     */
    public X509Certificate getSigningCertificate() {
        return signingCertificate;
    }
    
    /**
     * @return the signature algorithm used, or null if not available
     */
    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }
    
    /**
     * @return the canonicalization algorithm used, or null if not available
     */
    public String getCanonicalizationAlgorithm() {
        return canonicalizationAlgorithm;
    }
    
    /**
     * @return error message if validation failed, or null if successful
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Creates a new builder for XmlSignatureValidationResult.
     * 
     * @return new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder class for XmlSignatureValidationResult.
     */
    public static class Builder {
        private boolean signaturePresent = false;
        private boolean valid = false;
        private boolean signatureAlgorithmValid = false;
        private boolean canonicalizationValid = false;
        private boolean referencesValid = false;
        private boolean keyInfoValid = false;
        private boolean certificateMatches = false;
        private boolean cryptographicSignatureValid = false;
        private X509Certificate signingCertificate;
        private String signatureAlgorithm;
        private String canonicalizationAlgorithm;
        private String errorMessage;
        
        public Builder signaturePresent(boolean signaturePresent) {
            this.signaturePresent = signaturePresent;
            return this;
        }
        
        public Builder valid(boolean valid) {
            this.valid = valid;
            return this;
        }
        
        public Builder signatureAlgorithmValid(boolean signatureAlgorithmValid) {
            this.signatureAlgorithmValid = signatureAlgorithmValid;
            return this;
        }
        
        public Builder canonicalizationValid(boolean canonicalizationValid) {
            this.canonicalizationValid = canonicalizationValid;
            return this;
        }
        
        public Builder referencesValid(boolean referencesValid) {
            this.referencesValid = referencesValid;
            return this;
        }
        
        public Builder keyInfoValid(boolean keyInfoValid) {
            this.keyInfoValid = keyInfoValid;
            return this;
        }
        
        public Builder certificateMatches(boolean certificateMatches) {
            this.certificateMatches = certificateMatches;
            return this;
        }
        
        public Builder cryptographicSignatureValid(boolean cryptographicSignatureValid) {
            this.cryptographicSignatureValid = cryptographicSignatureValid;
            return this;
        }
        
        public Builder signingCertificate(X509Certificate signingCertificate) {
            this.signingCertificate = signingCertificate;
            return this;
        }
        
        public Builder signatureAlgorithm(String signatureAlgorithm) {
            this.signatureAlgorithm = signatureAlgorithm;
            return this;
        }
        
        public Builder canonicalizationAlgorithm(String canonicalizationAlgorithm) {
            this.canonicalizationAlgorithm = canonicalizationAlgorithm;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public XmlSignatureValidationResult build() {
            return new XmlSignatureValidationResult(this);
        }
    }
    
    @Override
    public String toString() {
        return "XmlSignatureValidationResult{" +
                "signaturePresent=" + signaturePresent +
                ", valid=" + valid +
                ", signatureAlgorithmValid=" + signatureAlgorithmValid +
                ", canonicalizationValid=" + canonicalizationValid +
                ", referencesValid=" + referencesValid +
                ", keyInfoValid=" + keyInfoValid +
                ", certificateMatches=" + certificateMatches +
                ", cryptographicSignatureValid=" + cryptographicSignatureValid +
                ", signatureAlgorithm='" + signatureAlgorithm + '\'' +
                ", canonicalizationAlgorithm='" + canonicalizationAlgorithm + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}