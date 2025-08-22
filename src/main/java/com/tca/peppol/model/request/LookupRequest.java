package com.tca.peppol.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request model for Peppol participant lookup operations.
 * Contains participant identification, document type, process information,
 * and optional configuration flags for extended validation and details.
 */
public class LookupRequest {
    
    // Peppol participant ID pattern: scheme::identifier (e.g., iso6523-actorid-upis::9915:test)
    private static final String PEPPOL_PARTICIPANT_PATTERN = 
        "^[a-zA-Z0-9][a-zA-Z0-9\\-]*::[a-zA-Z0-9][a-zA-Z0-9\\-.:]*$";
    
    // Peppol document type pattern: typically URN format
    private static final String PEPPOL_DOCUMENT_TYPE_PATTERN = 
        "^[a-zA-Z][a-zA-Z0-9\\-.:]*::[a-zA-Z0-9][a-zA-Z0-9\\-.:]*$";
    
    // Peppol process ID pattern: typically URN format
    private static final String PEPPOL_PROCESS_PATTERN = 
        "^[a-zA-Z][a-zA-Z0-9\\-.:]*::[a-zA-Z0-9][a-zA-Z0-9\\-.:]*$";

    @NotNull(message = "Participant ID is required")
    @Pattern(regexp = PEPPOL_PARTICIPANT_PATTERN, 
             message = "Participant ID must follow Peppol identifier pattern (scheme::identifier)")
    @Size(max = 1000, message = "Participant ID must not exceed 1000 characters")
    @JsonProperty("participantId")
    private String participantId;

    @NotNull(message = "Document type ID is required")
    @Pattern(regexp = PEPPOL_DOCUMENT_TYPE_PATTERN, 
             message = "Document type ID must follow Peppol document type pattern")
    @Size(max = 1000, message = "Document type ID must not exceed 1000 characters")
    @JsonProperty("documentTypeId")
    private String documentTypeId;

    @NotNull(message = "Process ID is required")
    @Pattern(regexp = PEPPOL_PROCESS_PATTERN, 
             message = "Process ID must follow Peppol process identifier pattern")
    @Size(max = 1000, message = "Process ID must not exceed 1000 characters")
    @JsonProperty("processId")
    private String processId;

    @NotNull(message = "Environment is required")
    @Pattern(regexp = "^(production|test)$", 
             message = "Environment must be either 'production' or 'test'")
    @JsonProperty("environment")
    private String environment;

    @JsonProperty("validateEndpointConnectivity")
    private boolean validateEndpointConnectivity = false;

    @JsonProperty("includeFullCertificateChain")
    private boolean includeFullCertificateChain = false;

    @JsonProperty("includeTechnicalDetails")
    private boolean includeTechnicalDetails = false;

    @Size(max = 100, message = "Request ID must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\-_]*$", 
             message = "Request ID can only contain alphanumeric characters, hyphens, and underscores")
    @JsonProperty("requestId")
    private String requestId;

    // Default constructor
    public LookupRequest() {}

    // Constructor with required fields
    public LookupRequest(String participantId, String documentTypeId, String processId, String environment) {
        this.participantId = participantId;
        this.documentTypeId = documentTypeId;
        this.processId = processId;
        this.environment = environment;
    }

    // Getters and setters
    public String getParticipantId() {
        return participantId;
    }

    public void setParticipantId(String participantId) {
        this.participantId = participantId;
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

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public boolean isValidateEndpointConnectivity() {
        return validateEndpointConnectivity;
    }

    public void setValidateEndpointConnectivity(boolean validateEndpointConnectivity) {
        this.validateEndpointConnectivity = validateEndpointConnectivity;
    }

    public boolean isIncludeFullCertificateChain() {
        return includeFullCertificateChain;
    }

    public void setIncludeFullCertificateChain(boolean includeFullCertificateChain) {
        this.includeFullCertificateChain = includeFullCertificateChain;
    }

    public boolean isIncludeTechnicalDetails() {
        return includeTechnicalDetails;
    }

    public void setIncludeTechnicalDetails(boolean includeTechnicalDetails) {
        this.includeTechnicalDetails = includeTechnicalDetails;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public String toString() {
        return "LookupRequest{" +
                "participantId='" + participantId + '\'' +
                ", documentTypeId='" + documentTypeId + '\'' +
                ", processId='" + processId + '\'' +
                ", environment='" + environment + '\'' +
                ", validateEndpointConnectivity=" + validateEndpointConnectivity +
                ", includeFullCertificateChain=" + includeFullCertificateChain +
                ", includeTechnicalDetails=" + includeTechnicalDetails +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}