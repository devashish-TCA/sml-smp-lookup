package com.tca.peppol.validation;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents the result of a validation operation.
 * Contains validation status, error information, and timing details.
 */
public class ValidationResult {
    
    private final boolean valid;
    private final String message;
    private final String errorCode;
    private final Instant timestamp;
    private final long validationTimeMs;
    
    private ValidationResult(boolean valid, String message, String errorCode, long validationTimeMs) {
        this.valid = valid;
        this.message = message;
        this.errorCode = errorCode;
        this.timestamp = Instant.now();
        this.validationTimeMs = validationTimeMs;
    }
    
    /**
     * Create a successful validation result.
     */
    public static ValidationResult success(String message) {
        return new ValidationResult(true, message, null, 0);
    }
    
    /**
     * Create a successful validation result with timing information.
     */
    public static ValidationResult success(String message, long validationTimeMs) {
        return new ValidationResult(true, message, null, validationTimeMs);
    }
    
    /**
     * Create a failed validation result.
     */
    public static ValidationResult failure(String errorCode, String message) {
        return new ValidationResult(false, message, errorCode, 0);
    }
    
    /**
     * Create a failed validation result with timing information.
     */
    public static ValidationResult failure(String errorCode, String message, long validationTimeMs) {
        return new ValidationResult(false, message, errorCode, validationTimeMs);
    }
    
    /**
     * Check if the validation was successful.
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Get the validation message (success or error description).
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Get the error code (null for successful validations).
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Get the timestamp when the validation was performed.
     */
    public Instant getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get the validation execution time in milliseconds.
     */
    public long getValidationTimeMs() {
        return validationTimeMs;
    }
    
    /**
     * Check if this is a failure result.
     */
    public boolean isFailure() {
        return !valid;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationResult that = (ValidationResult) o;
        return valid == that.valid &&
               validationTimeMs == that.validationTimeMs &&
               Objects.equals(message, that.message) &&
               Objects.equals(errorCode, that.errorCode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(valid, message, errorCode, validationTimeMs);
    }
    
    @Override
    public String toString() {
        if (valid) {
            return String.format("ValidationResult{valid=true, message='%s', time=%dms}", 
                message, validationTimeMs);
        } else {
            return String.format("ValidationResult{valid=false, errorCode='%s', message='%s', time=%dms}", 
                errorCode, message, validationTimeMs);
        }
    }
}