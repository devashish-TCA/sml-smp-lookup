package com.tca.peppol.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/**
 * Main response model for Peppol participant lookup operations.
 * Contains all required fields including success status, endpoint information,
 * certificate data, validation results, and optional technical details.
 */
public class LookupResponse {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("participantId")
    private String participantId;

    @JsonProperty("endpointUrl")
    private String endpointUrl;

    @JsonProperty("transportProfile")
    private String transportProfile;

    @JsonProperty("certificate")
    private String certificate;

    @JsonProperty("certificateChain")
    private List<String> certificateChain;

    @JsonProperty("serviceActivationDate")
    private Instant serviceActivationDate;

    @JsonProperty("serviceExpirationDate")
    private Instant serviceExpirationDate;

    @JsonProperty("validationResults")
    private ValidationResults validationResults;

    @JsonProperty("certificateDetails")
    private CertificateDetails certificateDetails;

    @JsonProperty("technicalDetails")
    private TechnicalDetails technicalDetails;

    @JsonProperty("errors")
    private List<ErrorDetail> errors;

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("processingTimeMs")
    private long processingTimeMs;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("environment")
    private String environment;

    @JsonProperty("documentTypeId")
    private String documentTypeId;

    @JsonProperty("processId")
    private String processId;

    // Enhanced fields for peppol-commons integration
    @JsonProperty("successful")
    private boolean successful;

    @JsonProperty("totalProcessingTimeMs")
    private long totalProcessingTimeMs;

    @JsonProperty("smlDnsQuery")
    private String smlDnsQuery;

    @JsonProperty("smlResolutionTimeMs")
    private long smlResolutionTimeMs;

    @JsonProperty("smpUrl")
    private String smpUrl;

    @JsonProperty("dnssecValid")
    private boolean dnssecValid;

    @JsonProperty("smpQueryTimeMs")
    private long smpQueryTimeMs;

    @JsonProperty("certificateSubject")
    private String certificateSubject;

    @JsonProperty("certificateValid")
    private boolean certificateValid;

    @JsonProperty("errorMessage")
    private String errorMessage;

    // Default constructor
    public LookupResponse() {
        this.timestamp = Instant.now();
    }

    // Constructor for successful response
    public LookupResponse(String participantId, String endpointUrl, String transportProfile, 
                         String certificate, ValidationResults validationResults) {
        this();
        this.success = true;
        this.participantId = participantId;
        this.endpointUrl = endpointUrl;
        this.transportProfile = transportProfile;
        this.certificate = certificate;
        this.validationResults = validationResults;
    }

    // Constructor for failed response
    public LookupResponse(String participantId, List<ErrorDetail> errors) {
        this();
        this.success = false;
        this.participantId = participantId;
        this.errors = errors;
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getParticipantId() {
        return participantId;
    }

    public void setParticipantId(String participantId) {
        this.participantId = participantId;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getTransportProfile() {
        return transportProfile;
    }

    public void setTransportProfile(String transportProfile) {
        this.transportProfile = transportProfile;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public List<String> getCertificateChain() {
        return certificateChain;
    }

    public void setCertificateChain(List<String> certificateChain) {
        this.certificateChain = certificateChain;
    }

    public Instant getServiceActivationDate() {
        return serviceActivationDate;
    }

    public void setServiceActivationDate(Instant serviceActivationDate) {
        this.serviceActivationDate = serviceActivationDate;
    }

    public Instant getServiceExpirationDate() {
        return serviceExpirationDate;
    }

    public void setServiceExpirationDate(Instant serviceExpirationDate) {
        this.serviceExpirationDate = serviceExpirationDate;
    }

    public ValidationResults getValidationResults() {
        return validationResults;
    }

    public void setValidationResults(ValidationResults validationResults) {
        this.validationResults = validationResults;
    }

    public CertificateDetails getCertificateDetails() {
        return certificateDetails;
    }

    public void setCertificateDetails(CertificateDetails certificateDetails) {
        this.certificateDetails = certificateDetails;
    }

    public TechnicalDetails getTechnicalDetails() {
        return technicalDetails;
    }

    public void setTechnicalDetails(TechnicalDetails technicalDetails) {
        this.technicalDetails = technicalDetails;
    }

    public List<ErrorDetail> getErrors() {
        return errors;
    }

    public void setErrors(List<ErrorDetail> errors) {
        this.errors = errors;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getDocumentTypeId() {
        return documentTypeId;
    }

    public void setDocumentTypeId(String documentTypeId) {
        this.documentTypeId = documentTypeId;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    // Enhanced getters and setters for peppol-commons integration
    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public long getTotalProcessingTimeMs() {
        return totalProcessingTimeMs;
    }

    public void setTotalProcessingTimeMs(long totalProcessingTimeMs) {
        this.totalProcessingTimeMs = totalProcessingTimeMs;
    }

    public String getSmlDnsQuery() {
        return smlDnsQuery;
    }

    public void setSmlDnsQuery(String smlDnsQuery) {
        this.smlDnsQuery = smlDnsQuery;
    }

    public long getSmlResolutionTimeMs() {
        return smlResolutionTimeMs;
    }

    public void setSmlResolutionTimeMs(long smlResolutionTimeMs) {
        this.smlResolutionTimeMs = smlResolutionTimeMs;
    }

    public String getSmpUrl() {
        return smpUrl;
    }

    public void setSmpUrl(String smpUrl) {
        this.smpUrl = smpUrl;
    }

    public boolean isDnssecValid() {
        return dnssecValid;
    }

    public void setDnssecValid(boolean dnssecValid) {
        this.dnssecValid = dnssecValid;
    }

    public long getSmpQueryTimeMs() {
        return smpQueryTimeMs;
    }

    public void setSmpQueryTimeMs(long smpQueryTimeMs) {
        this.smpQueryTimeMs = smpQueryTimeMs;
    }

    public String getCertificateSubject() {
        return certificateSubject;
    }

    public void setCertificateSubject(String certificateSubject) {
        this.certificateSubject = certificateSubject;
    }

    public boolean isCertificateValid() {
        return certificateValid;
    }

    public void setCertificateValid(boolean certificateValid) {
        this.certificateValid = certificateValid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Check if the response contains any errors
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    /**
     * Check if the response contains any critical errors (severity = ERROR)
     */
    public boolean hasCriticalErrors() {
        return errors != null && errors.stream()
                .anyMatch(error -> error.getSeverity() == ErrorDetail.ErrorSeverity.ERROR);
    }

    /**
     * Check if the response contains any warnings
     */
    public boolean hasWarnings() {
        return errors != null && errors.stream()
                .anyMatch(error -> error.getSeverity() == ErrorDetail.ErrorSeverity.WARNING);
    }

    /**
     * Check if the service is currently active based on service dates
     */
    public boolean isServiceActive() {
        if (serviceActivationDate == null && serviceExpirationDate == null) {
            return true; // No date restrictions
        }
        
        Instant now = Instant.now();
        boolean afterActivation = serviceActivationDate == null || now.isAfter(serviceActivationDate);
        boolean beforeExpiration = serviceExpirationDate == null || now.isBefore(serviceExpirationDate);
        
        return afterActivation && beforeExpiration;
    }

    /**
     * Check if all validations passed (if validation results are available)
     */
    public boolean areAllValidationsPassed() {
        return validationResults != null && validationResults.isAllValidationsPassed();
    }

    /**
     * Builder pattern for creating LookupResponse instances
     */
    public static class Builder {
        private final LookupResponse response;

        public Builder(String participantId) {
            this.response = new LookupResponse();
            this.response.setParticipantId(participantId);
        }

        public Builder success(boolean success) {
            this.response.setSuccess(success);
            return this;
        }

        public Builder endpointUrl(String endpointUrl) {
            this.response.setEndpointUrl(endpointUrl);
            return this;
        }

        public Builder transportProfile(String transportProfile) {
            this.response.setTransportProfile(transportProfile);
            return this;
        }

        public Builder certificate(String certificate) {
            this.response.setCertificate(certificate);
            return this;
        }

        public Builder certificateChain(List<String> certificateChain) {
            this.response.setCertificateChain(certificateChain);
            return this;
        }

        public Builder serviceActivationDate(Instant serviceActivationDate) {
            this.response.setServiceActivationDate(serviceActivationDate);
            return this;
        }

        public Builder serviceExpirationDate(Instant serviceExpirationDate) {
            this.response.setServiceExpirationDate(serviceExpirationDate);
            return this;
        }

        public Builder validationResults(ValidationResults validationResults) {
            this.response.setValidationResults(validationResults);
            return this;
        }

        public Builder certificateDetails(CertificateDetails certificateDetails) {
            this.response.setCertificateDetails(certificateDetails);
            return this;
        }

        public Builder technicalDetails(TechnicalDetails technicalDetails) {
            this.response.setTechnicalDetails(technicalDetails);
            return this;
        }

        public Builder errors(List<ErrorDetail> errors) {
            this.response.setErrors(errors);
            return this;
        }

        public Builder requestId(String requestId) {
            this.response.setRequestId(requestId);
            return this;
        }

        public Builder processingTimeMs(long processingTimeMs) {
            this.response.setProcessingTimeMs(processingTimeMs);
            return this;
        }

        public Builder environment(String environment) {
            this.response.setEnvironment(environment);
            return this;
        }

        public Builder documentTypeId(String documentTypeId) {
            this.response.setDocumentTypeId(documentTypeId);
            return this;
        }

        public Builder processId(String processId) {
            this.response.setProcessId(processId);
            return this;
        }

        public LookupResponse build() {
            return this.response;
        }
    }

    @Override
    public String toString() {
        return "LookupResponse{" +
                "success=" + success +
                ", participantId='" + participantId + '\'' +
                ", endpointUrl='" + endpointUrl + '\'' +
                ", transportProfile='" + transportProfile + '\'' +
                ", certificate='" + (certificate != null ? "[CERTIFICATE_DATA]" : "null") + '\'' +
                ", serviceActivationDate=" + serviceActivationDate +
                ", serviceExpirationDate=" + serviceExpirationDate +
                ", requestId='" + requestId + '\'' +
                ", processingTimeMs=" + processingTimeMs +
                ", timestamp=" + timestamp +
                ", environment='" + environment + '\'' +
                ", hasErrors=" + hasErrors() +
                '}';
    }
}