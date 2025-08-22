package com.tca.peppol.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Technical details model containing comprehensive technical information
 * about the lookup process, including timing, intermediate results,
 * and detailed validation information.
 */
public class TechnicalDetails {

    @JsonProperty("smlLookupTimeMs")
    private Long smlLookupTimeMs;

    @JsonProperty("smpQueryTimeMs")
    private Long smpQueryTimeMs;

    @JsonProperty("certificateValidationTimeMs")
    private Long certificateValidationTimeMs;

    @JsonProperty("xmlSignatureValidationTimeMs")
    private Long xmlSignatureValidationTimeMs;

    @JsonProperty("ocspValidationTimeMs")
    private Long ocspValidationTimeMs;

    @JsonProperty("crlValidationTimeMs")
    private Long crlValidationTimeMs;

    @JsonProperty("totalProcessingTimeMs")
    private Long totalProcessingTimeMs;

    @JsonProperty("smlDnsQuery")
    private String smlDnsQuery;

    @JsonProperty("smlMd5Hash")
    private String smlMd5Hash;

    @JsonProperty("smpUrl")
    private String smpUrl;

    @JsonProperty("smpQueryUrl")
    private String smpQueryUrl;

    @JsonProperty("dnsIntermediateResults")
    private List<String> dnsIntermediateResults;

    @JsonProperty("httpResponseCode")
    private Integer httpResponseCode;

    @JsonProperty("httpResponseHeaders")
    private Map<String, String> httpResponseHeaders;

    @JsonProperty("xmlSignatureAlgorithm")
    private String xmlSignatureAlgorithm;

    @JsonProperty("xmlCanonicalizationMethod")
    private String xmlCanonicalizationMethod;

    @JsonProperty("certificateChainLength")
    private Integer certificateChainLength;

    @JsonProperty("ocspResponderUrl")
    private String ocspResponderUrl;

    @JsonProperty("crlDistributionPoints")
    private List<String> crlDistributionPoints;

    @JsonProperty("dnssecValidated")
    private Boolean dnssecValidated;

    @JsonProperty("circuitBreakerStates")
    private Map<String, String> circuitBreakerStates;

    @JsonProperty("cacheHits")
    private Map<String, Boolean> cacheHits;

    @JsonProperty("retryAttempts")
    private Map<String, Integer> retryAttempts;

    @JsonProperty("validationErrors")
    private List<String> validationErrors;

    @JsonProperty("validationWarnings")
    private List<String> validationWarnings;

    @JsonProperty("peppolEnvironment")
    private String peppolEnvironment;

    @JsonProperty("smlDomain")
    private String smlDomain;

    @JsonProperty("transportProfile")
    private String transportProfile;

    @JsonProperty("endpointConnectivityTest")
    private EndpointConnectivityResult endpointConnectivityTest;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("stepTimings")
    private Map<String, Long> stepTimings;

    // Default constructor
    public TechnicalDetails() {}

    // Getters and setters
    public Long getSmlLookupTimeMs() {
        return smlLookupTimeMs;
    }

    public void setSmlLookupTimeMs(Long smlLookupTimeMs) {
        this.smlLookupTimeMs = smlLookupTimeMs;
    }

    public Long getSmpQueryTimeMs() {
        return smpQueryTimeMs;
    }

    public void setSmpQueryTimeMs(Long smpQueryTimeMs) {
        this.smpQueryTimeMs = smpQueryTimeMs;
    }

    public Long getCertificateValidationTimeMs() {
        return certificateValidationTimeMs;
    }

    public void setCertificateValidationTimeMs(Long certificateValidationTimeMs) {
        this.certificateValidationTimeMs = certificateValidationTimeMs;
    }

    public Long getXmlSignatureValidationTimeMs() {
        return xmlSignatureValidationTimeMs;
    }

    public void setXmlSignatureValidationTimeMs(Long xmlSignatureValidationTimeMs) {
        this.xmlSignatureValidationTimeMs = xmlSignatureValidationTimeMs;
    }

    public Long getOcspValidationTimeMs() {
        return ocspValidationTimeMs;
    }

    public void setOcspValidationTimeMs(Long ocspValidationTimeMs) {
        this.ocspValidationTimeMs = ocspValidationTimeMs;
    }

    public Long getCrlValidationTimeMs() {
        return crlValidationTimeMs;
    }

    public void setCrlValidationTimeMs(Long crlValidationTimeMs) {
        this.crlValidationTimeMs = crlValidationTimeMs;
    }

    public Long getTotalProcessingTimeMs() {
        return totalProcessingTimeMs;
    }

    public void setTotalProcessingTimeMs(Long totalProcessingTimeMs) {
        this.totalProcessingTimeMs = totalProcessingTimeMs;
    }

    public String getSmlDnsQuery() {
        return smlDnsQuery;
    }

    public void setSmlDnsQuery(String smlDnsQuery) {
        this.smlDnsQuery = smlDnsQuery;
    }

    public String getSmlMd5Hash() {
        return smlMd5Hash;
    }

    public void setSmlMd5Hash(String smlMd5Hash) {
        this.smlMd5Hash = smlMd5Hash;
    }

    public String getSmpUrl() {
        return smpUrl;
    }

    public void setSmpUrl(String smpUrl) {
        this.smpUrl = smpUrl;
    }

    public String getSmpQueryUrl() {
        return smpQueryUrl;
    }

    public void setSmpQueryUrl(String smpQueryUrl) {
        this.smpQueryUrl = smpQueryUrl;
    }

    public List<String> getDnsIntermediateResults() {
        return dnsIntermediateResults;
    }

    public void setDnsIntermediateResults(List<String> dnsIntermediateResults) {
        this.dnsIntermediateResults = dnsIntermediateResults;
    }

    public Integer getHttpResponseCode() {
        return httpResponseCode;
    }

    public void setHttpResponseCode(Integer httpResponseCode) {
        this.httpResponseCode = httpResponseCode;
    }

    public Map<String, String> getHttpResponseHeaders() {
        return httpResponseHeaders;
    }

    public void setHttpResponseHeaders(Map<String, String> httpResponseHeaders) {
        this.httpResponseHeaders = httpResponseHeaders;
    }

    public String getXmlSignatureAlgorithm() {
        return xmlSignatureAlgorithm;
    }

    public void setXmlSignatureAlgorithm(String xmlSignatureAlgorithm) {
        this.xmlSignatureAlgorithm = xmlSignatureAlgorithm;
    }

    public String getXmlCanonicalizationMethod() {
        return xmlCanonicalizationMethod;
    }

    public void setXmlCanonicalizationMethod(String xmlCanonicalizationMethod) {
        this.xmlCanonicalizationMethod = xmlCanonicalizationMethod;
    }

    public Integer getCertificateChainLength() {
        return certificateChainLength;
    }

    public void setCertificateChainLength(Integer certificateChainLength) {
        this.certificateChainLength = certificateChainLength;
    }

    public String getOcspResponderUrl() {
        return ocspResponderUrl;
    }

    public void setOcspResponderUrl(String ocspResponderUrl) {
        this.ocspResponderUrl = ocspResponderUrl;
    }

    public List<String> getCrlDistributionPoints() {
        return crlDistributionPoints;
    }

    public void setCrlDistributionPoints(List<String> crlDistributionPoints) {
        this.crlDistributionPoints = crlDistributionPoints;
    }

    public Boolean getDnssecValidated() {
        return dnssecValidated;
    }

    public void setDnssecValidated(Boolean dnssecValidated) {
        this.dnssecValidated = dnssecValidated;
    }

    public Map<String, String> getCircuitBreakerStates() {
        return circuitBreakerStates;
    }

    public void setCircuitBreakerStates(Map<String, String> circuitBreakerStates) {
        this.circuitBreakerStates = circuitBreakerStates;
    }

    public Map<String, Boolean> getCacheHits() {
        return cacheHits;
    }

    public void setCacheHits(Map<String, Boolean> cacheHits) {
        this.cacheHits = cacheHits;
    }

    public Map<String, Integer> getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(Map<String, Integer> retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<String> validationErrors) {
        this.validationErrors = validationErrors;
    }

    public List<String> getValidationWarnings() {
        return validationWarnings;
    }

    public void setValidationWarnings(List<String> validationWarnings) {
        this.validationWarnings = validationWarnings;
    }

    public String getPeppolEnvironment() {
        return peppolEnvironment;
    }

    public void setPeppolEnvironment(String peppolEnvironment) {
        this.peppolEnvironment = peppolEnvironment;
    }

    public String getSmlDomain() {
        return smlDomain;
    }

    public void setSmlDomain(String smlDomain) {
        this.smlDomain = smlDomain;
    }

    public String getTransportProfile() {
        return transportProfile;
    }

    public void setTransportProfile(String transportProfile) {
        this.transportProfile = transportProfile;
    }

    public EndpointConnectivityResult getEndpointConnectivityTest() {
        return endpointConnectivityTest;
    }

    public void setEndpointConnectivityTest(EndpointConnectivityResult endpointConnectivityTest) {
        this.endpointConnectivityTest = endpointConnectivityTest;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Map<String, Long> getStepTimings() {
        return stepTimings;
    }

    public void setStepTimings(Map<String, Long> stepTimings) {
        this.stepTimings = stepTimings;
    }

    /**
     * Nested class for endpoint connectivity test results
     */
    public static class EndpointConnectivityResult {
        @JsonProperty("tested")
        private boolean tested;

        @JsonProperty("accessible")
        private boolean accessible;

        @JsonProperty("responseTimeMs")
        private Long responseTimeMs;

        @JsonProperty("httpStatusCode")
        private Integer httpStatusCode;

        @JsonProperty("sslCertificateValid")
        private Boolean sslCertificateValid;

        @JsonProperty("errorMessage")
        private String errorMessage;

        // Default constructor
        public EndpointConnectivityResult() {}

        // Constructor
        public EndpointConnectivityResult(boolean tested, boolean accessible) {
            this.tested = tested;
            this.accessible = accessible;
        }

        // Getters and setters
        public boolean isTested() {
            return tested;
        }

        public void setTested(boolean tested) {
            this.tested = tested;
        }

        public boolean isAccessible() {
            return accessible;
        }

        public void setAccessible(boolean accessible) {
            this.accessible = accessible;
        }

        public Long getResponseTimeMs() {
            return responseTimeMs;
        }

        public void setResponseTimeMs(Long responseTimeMs) {
            this.responseTimeMs = responseTimeMs;
        }

        public Integer getHttpStatusCode() {
            return httpStatusCode;
        }

        public void setHttpStatusCode(Integer httpStatusCode) {
            this.httpStatusCode = httpStatusCode;
        }

        public Boolean getSslCertificateValid() {
            return sslCertificateValid;
        }

        public void setSslCertificateValid(Boolean sslCertificateValid) {
            this.sslCertificateValid = sslCertificateValid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    @Override
    public String toString() {
        return "TechnicalDetails{" +
                "smlLookupTimeMs=" + smlLookupTimeMs +
                ", smpQueryTimeMs=" + smpQueryTimeMs +
                ", certificateValidationTimeMs=" + certificateValidationTimeMs +
                ", xmlSignatureValidationTimeMs=" + xmlSignatureValidationTimeMs +
                ", totalProcessingTimeMs=" + totalProcessingTimeMs +
                ", smlDnsQuery='" + smlDnsQuery + '\'' +
                ", smpUrl='" + smpUrl + '\'' +
                ", peppolEnvironment='" + peppolEnvironment + '\'' +
                ", transportProfile='" + transportProfile + '\'' +
                '}';
    }
}