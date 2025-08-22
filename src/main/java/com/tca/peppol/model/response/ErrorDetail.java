package com.tca.peppol.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Structured error information model for comprehensive error reporting.
 * Provides categorized error codes, severity levels, and retryability flags
 * to enable proper error handling and client retry logic.
 */
public class ErrorDetail {

    /**
     * Error categories for classification
     */
    public enum ErrorCategory {
        SML,                // SML-related errors (DNS resolution, hash calculation)
        SMP,                // SMP-related errors (HTTP communication, XML parsing)
        CERTIFICATE,        // Certificate-related errors (validation, revocation)
        NETWORK,           // Network-related errors (timeouts, connectivity)
        VALIDATION,        // Validation-related errors (compliance, format)
        EXTERNAL_SERVICE,  // External service errors (OCSP, CRL)
        INPUT,             // Input validation errors
        SYSTEM             // System errors (configuration, internal)
    }

    /**
     * Error severity levels
     */
    public enum ErrorSeverity {
        ERROR,    // Critical errors that prevent operation completion
        WARNING,  // Non-critical issues that may affect functionality
        INFO      // Informational messages
    }

    @JsonProperty("code")
    private String code;

    @JsonProperty("category")
    private ErrorCategory category;

    @JsonProperty("severity")
    private ErrorSeverity severity;

    @JsonProperty("message")
    private String message;

    @JsonProperty("technicalDetails")
    private String technicalDetails;

    @JsonProperty("retryable")
    private boolean retryable;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("helpUrl")
    private String helpUrl;

    @JsonProperty("additionalContext")
    private Map<String, Object> additionalContext;

    // Default constructor
    public ErrorDetail() {
        this.timestamp = Instant.now();
        this.additionalContext = new HashMap<>();
    }

    // Constructor with required fields
    public ErrorDetail(String code, ErrorCategory category, ErrorSeverity severity, String message) {
        this();
        this.code = code;
        this.category = category;
        this.severity = severity;
        this.message = message;
    }

    // Constructor with all common fields
    public ErrorDetail(String code, ErrorCategory category, ErrorSeverity severity, 
                      String message, String technicalDetails, boolean retryable) {
        this(code, category, severity, message);
        this.technicalDetails = technicalDetails;
        this.retryable = retryable;
    }

    // Getters and setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public ErrorCategory getCategory() {
        return category;
    }

    public void setCategory(ErrorCategory category) {
        this.category = category;
    }

    public ErrorSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(ErrorSeverity severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTechnicalDetails() {
        return technicalDetails;
    }

    public void setTechnicalDetails(String technicalDetails) {
        this.technicalDetails = technicalDetails;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public void setRetryable(boolean retryable) {
        this.retryable = retryable;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getHelpUrl() {
        return helpUrl;
    }

    public void setHelpUrl(String helpUrl) {
        this.helpUrl = helpUrl;
    }

    public Map<String, Object> getAdditionalContext() {
        return additionalContext;
    }

    public void setAdditionalContext(Map<String, Object> additionalContext) {
        this.additionalContext = additionalContext != null ? additionalContext : new HashMap<>();
    }

    /**
     * Add additional context information
     */
    public void addContext(String key, Object value) {
        if (this.additionalContext == null) {
            this.additionalContext = new HashMap<>();
        }
        this.additionalContext.put(key, value);
    }

    /**
     * Builder pattern for creating ErrorDetail instances
     */
    public static class Builder {
        private final ErrorDetail errorDetail;

        public Builder(String code, ErrorCategory category, ErrorSeverity severity, String message) {
            this.errorDetail = new ErrorDetail(code, category, severity, message);
        }

        public Builder technicalDetails(String technicalDetails) {
            this.errorDetail.setTechnicalDetails(technicalDetails);
            return this;
        }

        public Builder retryable(boolean retryable) {
            this.errorDetail.setRetryable(retryable);
            return this;
        }

        public Builder helpUrl(String helpUrl) {
            this.errorDetail.setHelpUrl(helpUrl);
            return this;
        }

        public Builder addContext(String key, Object value) {
            this.errorDetail.addContext(key, value);
            return this;
        }

        public ErrorDetail build() {
            return this.errorDetail;
        }
    }

    @Override
    public String toString() {
        return "ErrorDetail{" +
                "code='" + code + '\'' +
                ", category=" + category +
                ", severity=" + severity +
                ", message='" + message + '\'' +
                ", technicalDetails='" + technicalDetails + '\'' +
                ", retryable=" + retryable +
                ", timestamp=" + timestamp +
                ", helpUrl='" + helpUrl + '\'' +
                ", additionalContext=" + additionalContext +
                '}';
    }
}