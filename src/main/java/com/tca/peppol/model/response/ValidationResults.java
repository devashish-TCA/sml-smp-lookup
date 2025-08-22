package com.tca.peppol.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Comprehensive validation results model containing boolean flags for all validation types.
 * Provides detailed validation status for certificates, XML signatures, endpoints,
 * revocation checking, DNS resolution, and overall Peppol compliance.
 */
public class ValidationResults {

    // Certificate Validation
    @JsonProperty("certificateValid")
    private boolean certificateValid;

    @JsonProperty("certificateNotExpired")
    private boolean certificateNotExpired;

    @JsonProperty("certificateNotRevoked")
    private boolean certificateNotRevoked;

    @JsonProperty("certificateFromOpenPeppol")
    private boolean certificateFromOpenPeppol;

    @JsonProperty("certificateChainValid")
    private boolean certificateChainValid;

    @JsonProperty("certificateKeyLengthValid")
    private boolean certificateKeyLengthValid;

    @JsonProperty("certificatePolicyValid")
    private boolean certificatePolicyValid;

    // XML Signature Validation
    @JsonProperty("smpSignatureValid")
    private boolean smpSignatureValid;

    @JsonProperty("xmlCanonicalizationValid")
    private boolean xmlCanonicalizationValid;

    @JsonProperty("signatureAlgorithmValid")
    private boolean signatureAlgorithmValid;

    // Endpoint Validation
    @JsonProperty("endpointAccessible")
    private boolean endpointAccessible;

    @JsonProperty("transportProfileSupported")
    private boolean transportProfileSupported;

    @JsonProperty("serviceAvailable")
    private boolean serviceAvailable;

    // Revocation Validation
    @JsonProperty("crlCheckPassed")
    private boolean crlCheckPassed;

    @JsonProperty("ocspCheckPassed")
    private boolean ocspCheckPassed;

    // DNS Validation
    @JsonProperty("smlDnsResolutionSuccessful")
    private boolean smlDnsResolutionSuccessful;

    @JsonProperty("dnsSecValid")
    private boolean dnsSecValid;

    @JsonProperty("smpAccessible")
    private boolean smpAccessible;

    // Compliance Validation
    @JsonProperty("peppolCompliant")
    private boolean peppolCompliant;

    @JsonProperty("productionNetworkCompliant")
    private boolean productionNetworkCompliant;

    // Default constructor
    public ValidationResults() {}

    // Constructor with all certificate validation flags
    public ValidationResults(boolean certificateValid, boolean certificateNotExpired, 
                           boolean certificateNotRevoked, boolean certificateFromOpenPeppol,
                           boolean certificateChainValid, boolean certificateKeyLengthValid,
                           boolean certificatePolicyValid) {
        this.certificateValid = certificateValid;
        this.certificateNotExpired = certificateNotExpired;
        this.certificateNotRevoked = certificateNotRevoked;
        this.certificateFromOpenPeppol = certificateFromOpenPeppol;
        this.certificateChainValid = certificateChainValid;
        this.certificateKeyLengthValid = certificateKeyLengthValid;
        this.certificatePolicyValid = certificatePolicyValid;
    }

    // Certificate Validation getters and setters
    public boolean isCertificateValid() {
        return certificateValid;
    }

    public void setCertificateValid(boolean certificateValid) {
        this.certificateValid = certificateValid;
    }

    public boolean isCertificateNotExpired() {
        return certificateNotExpired;
    }

    public void setCertificateNotExpired(boolean certificateNotExpired) {
        this.certificateNotExpired = certificateNotExpired;
    }

    public boolean isCertificateNotRevoked() {
        return certificateNotRevoked;
    }

    public void setCertificateNotRevoked(boolean certificateNotRevoked) {
        this.certificateNotRevoked = certificateNotRevoked;
    }

    public boolean isCertificateFromOpenPeppol() {
        return certificateFromOpenPeppol;
    }

    public void setCertificateFromOpenPeppol(boolean certificateFromOpenPeppol) {
        this.certificateFromOpenPeppol = certificateFromOpenPeppol;
    }

    public boolean isCertificateChainValid() {
        return certificateChainValid;
    }

    public void setCertificateChainValid(boolean certificateChainValid) {
        this.certificateChainValid = certificateChainValid;
    }

    public boolean isCertificateKeyLengthValid() {
        return certificateKeyLengthValid;
    }

    public void setCertificateKeyLengthValid(boolean certificateKeyLengthValid) {
        this.certificateKeyLengthValid = certificateKeyLengthValid;
    }

    public boolean isCertificatePolicyValid() {
        return certificatePolicyValid;
    }

    public void setCertificatePolicyValid(boolean certificatePolicyValid) {
        this.certificatePolicyValid = certificatePolicyValid;
    }

    // XML Signature Validation getters and setters
    public boolean isSmpSignatureValid() {
        return smpSignatureValid;
    }

    public void setSmpSignatureValid(boolean smpSignatureValid) {
        this.smpSignatureValid = smpSignatureValid;
    }

    public boolean isXmlCanonicalizationValid() {
        return xmlCanonicalizationValid;
    }

    public void setXmlCanonicalizationValid(boolean xmlCanonicalizationValid) {
        this.xmlCanonicalizationValid = xmlCanonicalizationValid;
    }

    public boolean isSignatureAlgorithmValid() {
        return signatureAlgorithmValid;
    }

    public void setSignatureAlgorithmValid(boolean signatureAlgorithmValid) {
        this.signatureAlgorithmValid = signatureAlgorithmValid;
    }

    // Endpoint Validation getters and setters
    public boolean isEndpointAccessible() {
        return endpointAccessible;
    }

    public void setEndpointAccessible(boolean endpointAccessible) {
        this.endpointAccessible = endpointAccessible;
    }

    public boolean isTransportProfileSupported() {
        return transportProfileSupported;
    }

    public void setTransportProfileSupported(boolean transportProfileSupported) {
        this.transportProfileSupported = transportProfileSupported;
    }

    public boolean isServiceAvailable() {
        return serviceAvailable;
    }

    public void setServiceAvailable(boolean serviceAvailable) {
        this.serviceAvailable = serviceAvailable;
    }

    // Revocation Validation getters and setters
    public boolean isCrlCheckPassed() {
        return crlCheckPassed;
    }

    public void setCrlCheckPassed(boolean crlCheckPassed) {
        this.crlCheckPassed = crlCheckPassed;
    }

    public boolean isOcspCheckPassed() {
        return ocspCheckPassed;
    }

    public void setOcspCheckPassed(boolean ocspCheckPassed) {
        this.ocspCheckPassed = ocspCheckPassed;
    }

    // DNS Validation getters and setters
    public boolean isSmlDnsResolutionSuccessful() {
        return smlDnsResolutionSuccessful;
    }

    public void setSmlDnsResolutionSuccessful(boolean smlDnsResolutionSuccessful) {
        this.smlDnsResolutionSuccessful = smlDnsResolutionSuccessful;
    }

    public boolean isDnsSecValid() {
        return dnsSecValid;
    }

    public void setDnsSecValid(boolean dnsSecValid) {
        this.dnsSecValid = dnsSecValid;
    }

    public boolean isSmpAccessible() {
        return smpAccessible;
    }

    public void setSmpAccessible(boolean smpAccessible) {
        this.smpAccessible = smpAccessible;
    }

    // Compliance Validation getters and setters
    public boolean isPeppolCompliant() {
        return peppolCompliant;
    }

    public void setPeppolCompliant(boolean peppolCompliant) {
        this.peppolCompliant = peppolCompliant;
    }

    public boolean isProductionNetworkCompliant() {
        return productionNetworkCompliant;
    }

    public void setProductionNetworkCompliant(boolean productionNetworkCompliant) {
        this.productionNetworkCompliant = productionNetworkCompliant;
    }

    /**
     * Check if all certificate validations passed
     */
    public boolean isAllCertificateValidationsPassed() {
        return certificateValid && certificateNotExpired && certificateNotRevoked &&
               certificateFromOpenPeppol && certificateChainValid && 
               certificateKeyLengthValid && certificatePolicyValid;
    }

    /**
     * Check if all XML signature validations passed
     */
    public boolean isAllXmlValidationsPassed() {
        return smpSignatureValid && xmlCanonicalizationValid && signatureAlgorithmValid;
    }

    /**
     * Check if all endpoint validations passed
     */
    public boolean isAllEndpointValidationsPassed() {
        return endpointAccessible && transportProfileSupported && serviceAvailable;
    }

    /**
     * Check if all revocation validations passed
     */
    public boolean isAllRevocationValidationsPassed() {
        return crlCheckPassed && ocspCheckPassed;
    }

    /**
     * Check if all DNS validations passed
     */
    public boolean isAllDnsValidationsPassed() {
        return smlDnsResolutionSuccessful && dnsSecValid && smpAccessible;
    }

    /**
     * Check if all validations passed
     */
    public boolean isAllValidationsPassed() {
        return isAllCertificateValidationsPassed() && isAllXmlValidationsPassed() &&
               isAllEndpointValidationsPassed() && isAllRevocationValidationsPassed() &&
               isAllDnsValidationsPassed() && peppolCompliant && productionNetworkCompliant;
    }

    @Override
    public String toString() {
        return "ValidationResults{" +
                "certificateValid=" + certificateValid +
                ", certificateNotExpired=" + certificateNotExpired +
                ", certificateNotRevoked=" + certificateNotRevoked +
                ", certificateFromOpenPeppol=" + certificateFromOpenPeppol +
                ", certificateChainValid=" + certificateChainValid +
                ", certificateKeyLengthValid=" + certificateKeyLengthValid +
                ", certificatePolicyValid=" + certificatePolicyValid +
                ", smpSignatureValid=" + smpSignatureValid +
                ", xmlCanonicalizationValid=" + xmlCanonicalizationValid +
                ", signatureAlgorithmValid=" + signatureAlgorithmValid +
                ", endpointAccessible=" + endpointAccessible +
                ", transportProfileSupported=" + transportProfileSupported +
                ", serviceAvailable=" + serviceAvailable +
                ", crlCheckPassed=" + crlCheckPassed +
                ", ocspCheckPassed=" + ocspCheckPassed +
                ", smlDnsResolutionSuccessful=" + smlDnsResolutionSuccessful +
                ", dnsSecValid=" + dnsSecValid +
                ", smpAccessible=" + smpAccessible +
                ", peppolCompliant=" + peppolCompliant +
                ", productionNetworkCompliant=" + productionNetworkCompliant +
                '}';
    }
}