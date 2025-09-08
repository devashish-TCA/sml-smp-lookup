package com.tca.peppol.model.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

/**
 * Request model for Peppol participant lookup operations.
 *
 * This class represents a request to lookup Peppol participant information including
 * their service metadata publisher (SMP) endpoints, supported document types, and
 * process configurations within the Peppol network infrastructure.
 *
 * The validation is designed to work with real-world Peppol identifiers and follows
 * the Peppol Policy for Identifiers and Transport Infrastructure specifications.
 *
 * @author TCA Peppol Team
 * @version 1.0
 * @since 2025-08-23
 */
@LookupRequest.ValidPeppolCombination
public class LookupRequest {

    // Known Peppol participant ID schemes
    private static final Set<String> VALID_PARTICIPANT_SCHEMES = new HashSet<>(Arrays.asList(
            "iso6523-actorid-upis", "9915", "9914", "9913", "9912", "9910", "9908", "9907",
            "9906", "9905", "9904", "9902", "9901", "0002", "0007", "0009", "0037", "0060",
            "0088", "0096", "0135", "0142", "0184", "0190", "0191", "0192", "0193", "0195",
            "0196", "0198", "0199", "0200", "0201", "0202", "0204", "0208", "0209", "0210",
            "0211", "0212", "0213", "9901", "9906", "9907", "9910", "9913", "9914", "9915",
            "9918", "9919", "9920", "9922", "9923", "9924", "9925", "9926", "9927", "9928",
            "9929", "9930", "9931", "9932", "9933", "9934", "9935", "9936", "9937", "9938",
            "9939", "9940", "9941", "9942", "9943", "9944", "9945", "9946", "9947", "9948",
            "9949", "9950", "9951", "9952", "9953", "9955", "9957", "9958", "an", "duns",
            "gln", "upik"
    ));

    // Realistic Peppol participant ID pattern - allows for all valid characters used in practice
    private static final String PEPPOL_PARTICIPANT_PATTERN =
            "^[a-zA-Z0-9][a-zA-Z0-9\\-_.]*::[\\w\\-.:+/*=()@#%&,]+$";

    // Peppol document type pattern - very permissive for complex URNs
    private static final String PEPPOL_DOCUMENT_TYPE_PATTERN =
            "^[a-zA-Z][\\w\\-.:+/*=()@#%&,]*::[\\w\\-.:+/*=()@#%&,;\\s]*$";

    // Peppol process ID pattern - similar to document type
    private static final String PEPPOL_PROCESS_PATTERN =
            "^[a-zA-Z][\\w\\-.:+/*=()@#%&,]*::[\\w\\-.:+/*=()@#%&,;\\s]*$";

    // Environment pattern - more flexible for real infrastructure
    private static final String ENVIRONMENT_PATTERN =
            "^(production|prod|test|testing|development|dev|staging|stage|sandbox|demo)$";

    @NotNull(message = "Participant ID is required")
    @Pattern(regexp = PEPPOL_PARTICIPANT_PATTERN,
            message = "Participant ID must follow Peppol identifier pattern (scheme::identifier)")
    @Size(min = 5, max = 1500, message = "Participant ID must be between 5 and 1500 characters")
    @ValidPeppolParticipant
    @JsonProperty("participantId")
    private String participantId;

    @NotNull(message = "Document type ID is required")
    @Pattern(regexp = PEPPOL_DOCUMENT_TYPE_PATTERN,
            message = "Document type ID must follow Peppol document type pattern")
    @Size(min = 5, max = 2000, message = "Document type ID must be between 5 and 2000 characters")
    @JsonProperty("documentTypeId")
    private String documentTypeId;

    @NotNull(message = "Process ID is required")
    @Pattern(regexp = PEPPOL_PROCESS_PATTERN,
            message = "Process ID must follow Peppol process identifier pattern")
    @Size(min = 5, max = 2000, message = "Process ID must be between 5 and 2000 characters")
    @JsonProperty("processId")
    private String processId;

    @NotNull(message = "Environment is required")
    @Pattern(regexp = ENVIRONMENT_PATTERN,
            message = "Environment must be one of: production, prod, test, testing, development, dev, staging, stage, sandbox, demo")
    @JsonProperty("environment")
    private String environment;

    @JsonProperty("validateEndpointConnectivity")
    private boolean validateEndpointConnectivity = false;

    @JsonProperty("includeFullCertificateChain")
    private boolean includeFullCertificateChain = false;

    @JsonProperty("includeTechnicalDetails")
    private boolean includeTechnicalDetails = false;

    @JsonProperty("includeTransportProfiles")
    private boolean includeTransportProfiles = false;

    @JsonProperty("validateCertificates")
    private boolean validateCertificates = false;

    @Size(max = 100, message = "Request ID must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\-_]*$",
            message = "Request ID can only contain alphanumeric characters, hyphens, and underscores")
    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("timeoutSeconds")
    @Min(value = 1, message = "Timeout must be at least 1 second")
    @Max(value = 300, message = "Timeout must not exceed 300 seconds")
    private Integer timeoutSeconds = 30;

    // Default constructor
    public LookupRequest() {}

    // Constructor with required fields
    public LookupRequest(String participantId, String documentTypeId, String processId, String environment) {
        this.participantId = participantId;
        this.documentTypeId = documentTypeId;
        this.processId = processId;
        this.environment = environment;
    }

    // Full constructor
    public LookupRequest(String participantId, String documentTypeId, String processId,
                         String environment, boolean validateEndpointConnectivity,
                         boolean includeFullCertificateChain, boolean includeTechnicalDetails) {
        this(participantId, documentTypeId, processId, environment);
        this.validateEndpointConnectivity = validateEndpointConnectivity;
        this.includeFullCertificateChain = includeFullCertificateChain;
        this.includeTechnicalDetails = includeTechnicalDetails;
    }

    /**
     * Extracts the scheme from the participant ID
     * @return the scheme part of the participant ID, or null if invalid format
     */
    @JsonIgnore
    public String getParticipantScheme() {
        if (participantId == null || !participantId.contains("::")) {
            return null;
        }
        return participantId.split("::", 2)[0];
    }

    /**
     * Extracts the identifier from the participant ID
     * @return the identifier part of the participant ID, or null if invalid format
     */
    @JsonIgnore
    public String getParticipantIdentifier() {
        if (participantId == null || !participantId.contains("::")) {
            return null;
        }
        String[] parts = participantId.split("::", 2);
        return parts.length > 1 ? parts[1] : null;
    }

    /**
     * Checks if this is a production environment lookup
     * @return true if environment is production or prod
     */
    @JsonIgnore
    public boolean isProductionEnvironment() {
        return "production".equalsIgnoreCase(environment) || "prod".equalsIgnoreCase(environment);
    }

    /**
     * Normalizes the environment to standard values
     * @return normalized environment string
     */
    @JsonIgnore
    public String getNormalizedEnvironment() {
        if (environment == null) return null;

        switch (environment.toLowerCase()) {
            case "prod":
            case "production":
                return "production";
            case "test":
            case "testing":
                return "test";
            case "dev":
            case "development":
                return "development";
            case "stage":
            case "staging":
                return "staging";
            default:
                return environment.toLowerCase();
        }
    }

    // Getters and setters
    public String getParticipantId() { return participantId; }
    public void setParticipantId(String participantId) { this.participantId = participantId; }

    public String getDocumentTypeId() { return documentTypeId; }
    public void setDocumentTypeId(String documentTypeId) { this.documentTypeId = documentTypeId; }

    public String getProcessId() { return processId; }
    public void setProcessId(String processId) { this.processId = processId; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public boolean isValidateEndpointConnectivity() { return validateEndpointConnectivity; }
    public void setValidateEndpointConnectivity(boolean validateEndpointConnectivity) {
        this.validateEndpointConnectivity = validateEndpointConnectivity;
    }

    public boolean isIncludeFullCertificateChain() { return includeFullCertificateChain; }
    public void setIncludeFullCertificateChain(boolean includeFullCertificateChain) {
        this.includeFullCertificateChain = includeFullCertificateChain;
    }

    public boolean isIncludeTechnicalDetails() { return includeTechnicalDetails; }
    public void setIncludeTechnicalDetails(boolean includeTechnicalDetails) {
        this.includeTechnicalDetails = includeTechnicalDetails;
    }

    public boolean isIncludeTransportProfiles() { return includeTransportProfiles; }
    public void setIncludeTransportProfiles(boolean includeTransportProfiles) {
        this.includeTransportProfiles = includeTransportProfiles;
    }

    public boolean isValidateCertificates() { return validateCertificates; }
    public void setValidateCertificates(boolean validateCertificates) {
        this.validateCertificates = validateCertificates;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    /**
     * Masks sensitive data for logging purposes
     */
    private String maskParticipantId(String participantId) {
        if (participantId == null || participantId.length() <= 8) {
            return "***";
        }
        int colonIndex = participantId.indexOf("::");
        if (colonIndex > 0) {
            String scheme = participantId.substring(0, colonIndex + 2);
            String identifier = participantId.substring(colonIndex + 2);
            if (identifier.length() <= 4) {
                return scheme + "***";
            }
            return scheme + identifier.substring(0, 2) + "***" + identifier.substring(identifier.length() - 2);
        }
        return participantId.substring(0, 4) + "***" + participantId.substring(participantId.length() - 2);
    }

    @Override
    public String toString() {
        return "LookupRequest{" +
                "participantId='" + maskParticipantId(participantId) + '\'' +
                ", documentTypeId='" + (documentTypeId != null ? documentTypeId.substring(0, Math.min(50, documentTypeId.length())) + "..." : null) + '\'' +
                ", processId='" + (processId != null ? processId.substring(0, Math.min(50, processId.length())) + "..." : null) + '\'' +
                ", environment='" + environment + '\'' +
                ", validateEndpointConnectivity=" + validateEndpointConnectivity +
                ", includeFullCertificateChain=" + includeFullCertificateChain +
                ", includeTechnicalDetails=" + includeTechnicalDetails +
                ", includeTransportProfiles=" + includeTransportProfiles +
                ", validateCertificates=" + validateCertificates +
                ", requestId='" + requestId + '\'' +
                ", timeoutSeconds=" + timeoutSeconds +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LookupRequest)) return false;
        LookupRequest that = (LookupRequest) o;
        return java.util.Objects.equals(participantId, that.participantId) &&
                java.util.Objects.equals(documentTypeId, that.documentTypeId) &&
                java.util.Objects.equals(processId, that.processId) &&
                java.util.Objects.equals(environment, that.environment);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(participantId, documentTypeId, processId, environment);
    }

    // Custom validation annotations

    /**
     * Validates that the participant ID uses a known Peppol scheme
     */
    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = ValidPeppolParticipant.Validator.class)
    @Documented
    public @interface ValidPeppolParticipant {
        String message() default "Participant ID must use a valid Peppol identifier scheme";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};

        class Validator implements ConstraintValidator<ValidPeppolParticipant, String> {
            @Override
            public boolean isValid(String value, ConstraintValidatorContext context) {
                if (value == null || !value.contains("::")) {
                    return false;
                }
                String scheme = value.split("::", 2)[0];
                return VALID_PARTICIPANT_SCHEMES.contains(scheme) || scheme.startsWith("iso6523-actorid-upis");
            }
        }
    }

    /**
     * Validates that the participant/document type/process combination is logically valid
     */
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = ValidPeppolCombination.Validator.class)
    @Documented
    public @interface ValidPeppolCombination {
        String message() default "Invalid combination of participant ID, document type, and process";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};

        class Validator implements ConstraintValidator<ValidPeppolCombination, LookupRequest> {
            @Override
            public boolean isValid(LookupRequest request, ConstraintValidatorContext context) {
                if (request == null) return true;

                // Basic null checks
                if (request.getParticipantId() == null ||
                        request.getDocumentTypeId() == null ||
                        request.getProcessId() == null) {
                    return true; // Let @NotNull handle these
                }

                // Validate that all parts follow the scheme::identifier pattern
                if (!request.getParticipantId().contains("::") ||
                        !request.getDocumentTypeId().contains("::") ||
                        !request.getProcessId().contains("::")) {
                    return false;
                }

                // Additional business logic validation can be added here
                // For example: validate specific document type/process combinations

                return true;
            }
        }
    }
}