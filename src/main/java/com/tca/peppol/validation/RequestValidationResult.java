package com.tca.peppol.validation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents the result of a request validation operation.
 * Contains validation status, error and warning lists, and timing details.
 */
public class RequestValidationResult {
    
    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;
    private final Instant timestamp;
    
    /**
     * Creates a new RequestValidationResult.
     *
     * @param valid true if validation passed, false otherwise
     * @param errors list of validation errors (empty if valid)
     * @param warnings list of validation warnings (may be present even if valid)
     */
    public RequestValidationResult(boolean valid, List<String> errors, List<String> warnings) {
        this.valid = valid;
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
        this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
        this.timestamp = Instant.now();
    }
    
    /**
     * Creates a successful validation result with no errors or warnings.
     */
    public static RequestValidationResult success() {
        return new RequestValidationResult(true, Collections.emptyList(), Collections.emptyList());
    }
    
    /**
     * Creates a successful validation result with warnings.
     */
    public static RequestValidationResult successWithWarnings(List<String> warnings) {
        return new RequestValidationResult(true, Collections.emptyList(), warnings);
    }
    
    /**
     * Creates a failed validation result with errors.
     */
    public static RequestValidationResult failure(List<String> errors) {
        return new RequestValidationResult(false, errors, Collections.emptyList());
    }
    
    /**
     * Creates a failed validation result with errors and warnings.
     */
    public static RequestValidationResult failure(List<String> errors, List<String> warnings) {
        return new RequestValidationResult(false, errors, warnings);
    }
    
    /**
     * Check if the validation was successful.
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Get the list of validation errors.
     */
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }
    
    /**
     * Get the list of validation warnings.
     */
    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }
    
    /**
     * Get the timestamp when the validation was performed.
     */
    public Instant getTimestamp() {
        return timestamp;
    }
    
    /**
     * Check if this is a failure result.
     */
    public boolean isFailure() {
        return !valid;
    }
    
    /**
     * Check if there are any errors.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * Check if there are any warnings.
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    /**
     * Get the total number of issues (errors + warnings).
     */
    public int getTotalIssueCount() {
        return errors.size() + warnings.size();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestValidationResult that = (RequestValidationResult) o;
        return valid == that.valid &&
               Objects.equals(errors, that.errors) &&
               Objects.equals(warnings, that.warnings);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(valid, errors, warnings);
    }
    
    @Override
    public String toString() {
        if (valid) {
            if (warnings.isEmpty()) {
                return "RequestValidationResult{valid=true, no issues}";
            } else {
                return String.format("RequestValidationResult{valid=true, warnings=%d}", warnings.size());
            }
        } else {
            return String.format("RequestValidationResult{valid=false, errors=%d, warnings=%d}", 
                errors.size(), warnings.size());
        }
    }
}