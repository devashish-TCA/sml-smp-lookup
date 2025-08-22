package com.tca.peppol.exception;

import com.tca.peppol.model.response.ErrorDetail;
import com.tca.peppol.model.response.ErrorDetail.ErrorCategory;
import com.tca.peppol.model.response.ErrorDetail.ErrorSeverity;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for aggregating and managing multiple errors in a single response.
 * Provides methods to collect, prioritize, and organize errors for comprehensive
 * error reporting while maintaining structured error information.
 */
public class ErrorAggregator {

    private final List<ErrorDetail> errors;
    private final Map<ErrorCategory, List<ErrorDetail>> errorsByCategory;
    private final Map<ErrorSeverity, List<ErrorDetail>> errorsBySeverity;

    public ErrorAggregator() {
        this.errors = new ArrayList<>();
        this.errorsByCategory = new EnumMap<>(ErrorCategory.class);
        this.errorsBySeverity = new EnumMap<>(ErrorSeverity.class);
        
        // Initialize category maps
        for (ErrorCategory category : ErrorCategory.values()) {
            errorsByCategory.put(category, new ArrayList<>());
        }
        
        // Initialize severity maps
        for (ErrorSeverity severity : ErrorSeverity.values()) {
            errorsBySeverity.put(severity, new ArrayList<>());
        }
    }

    /**
     * Add an error to the aggregator
     */
    public ErrorAggregator addError(ErrorDetail error) {
        if (error != null) {
            errors.add(error);
            errorsByCategory.get(error.getCategory()).add(error);
            errorsBySeverity.get(error.getSeverity()).add(error);
        }
        return this;
    }

    /**
     * Add an error using error code and message
     */
    public ErrorAggregator addError(String errorCode, String message) {
        return addError(ErrorClassifier.createError(errorCode, message));
    }

    /**
     * Add an error with technical details
     */
    public ErrorAggregator addError(String errorCode, String message, String technicalDetails) {
        return addError(ErrorClassifier.createError(errorCode, message, technicalDetails));
    }

    /**
     * Add an error with exception
     */
    public ErrorAggregator addError(String errorCode, String message, Throwable exception) {
        return addError(ErrorClassifier.createError(errorCode, message, null, exception));
    }

    /**
     * Add multiple errors
     */
    public ErrorAggregator addErrors(Collection<ErrorDetail> errors) {
        if (errors != null) {
            errors.forEach(this::addError);
        }
        return this;
    }

    /**
     * Check if there are any errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Check if there are any critical errors (ERROR severity)
     */
    public boolean hasCriticalErrors() {
        return !errorsBySeverity.get(ErrorSeverity.ERROR).isEmpty();
    }

    /**
     * Check if there are any warnings
     */
    public boolean hasWarnings() {
        return !errorsBySeverity.get(ErrorSeverity.WARNING).isEmpty();
    }

    /**
     * Check if there are any retryable errors
     */
    public boolean hasRetryableErrors() {
        return errors.stream().anyMatch(ErrorDetail::isRetryable);
    }

    /**
     * Get total error count
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * Get error count by severity
     */
    public int getErrorCount(ErrorSeverity severity) {
        return errorsBySeverity.get(severity).size();
    }

    /**
     * Get error count by category
     */
    public int getErrorCount(ErrorCategory category) {
        return errorsByCategory.get(category).size();
    }

    /**
     * Get all errors
     */
    public List<ErrorDetail> getAllErrors() {
        return new ArrayList<>(errors);
    }

    /**
     * Get errors by severity (ordered by timestamp)
     */
    public List<ErrorDetail> getErrorsBySeverity(ErrorSeverity severity) {
        return errorsBySeverity.get(severity).stream()
                .sorted(Comparator.comparing(ErrorDetail::getTimestamp))
                .collect(Collectors.toList());
    }

    /**
     * Get errors by category (ordered by timestamp)
     */
    public List<ErrorDetail> getErrorsByCategory(ErrorCategory category) {
        return errorsByCategory.get(category).stream()
                .sorted(Comparator.comparing(ErrorDetail::getTimestamp))
                .collect(Collectors.toList());
    }

    /**
     * Get errors sorted by priority (ERROR > WARNING > INFO, then by timestamp)
     */
    public List<ErrorDetail> getErrorsByPriority() {
        return errors.stream()
                .sorted(Comparator
                        .comparing((ErrorDetail e) -> e.getSeverity().ordinal())
                        .thenComparing(ErrorDetail::getTimestamp))
                .collect(Collectors.toList());
    }

    /**
     * Get only retryable errors
     */
    public List<ErrorDetail> getRetryableErrors() {
        return errors.stream()
                .filter(ErrorDetail::isRetryable)
                .sorted(Comparator.comparing(ErrorDetail::getTimestamp))
                .collect(Collectors.toList());
    }

    /**
     * Get only non-retryable errors
     */
    public List<ErrorDetail> getNonRetryableErrors() {
        return errors.stream()
                .filter(error -> !error.isRetryable())
                .sorted(Comparator.comparing(ErrorDetail::getTimestamp))
                .collect(Collectors.toList());
    }

    /**
     * Get error summary by category
     */
    public Map<ErrorCategory, Integer> getErrorSummaryByCategory() {
        return errorsByCategory.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size()
                ));
    }

    /**
     * Get error summary by severity
     */
    public Map<ErrorSeverity, Integer> getErrorSummaryBySeverity() {
        return errorsBySeverity.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size()
                ));
    }

    /**
     * Get the most severe error level present
     */
    public ErrorSeverity getHighestSeverity() {
        if (hasCriticalErrors()) {
            return ErrorSeverity.ERROR;
        } else if (hasWarnings()) {
            return ErrorSeverity.WARNING;
        } else if (!errors.isEmpty()) {
            return ErrorSeverity.INFO;
        }
        return null;
    }

    /**
     * Get first error of specified severity
     */
    public Optional<ErrorDetail> getFirstError(ErrorSeverity severity) {
        return errorsBySeverity.get(severity).stream()
                .min(Comparator.comparing(ErrorDetail::getTimestamp));
    }

    /**
     * Get first error of specified category
     */
    public Optional<ErrorDetail> getFirstError(ErrorCategory category) {
        return errorsByCategory.get(category).stream()
                .min(Comparator.comparing(ErrorDetail::getTimestamp));
    }

    /**
     * Clear all errors
     */
    public void clear() {
        errors.clear();
        errorsByCategory.values().forEach(List::clear);
        errorsBySeverity.values().forEach(List::clear);
    }

    /**
     * Create a summary error that aggregates multiple errors
     */
    public ErrorDetail createSummaryError() {
        if (errors.isEmpty()) {
            return null;
        }

        ErrorSeverity highestSeverity = getHighestSeverity();
        boolean hasRetryable = hasRetryableErrors();
        
        String summaryMessage = String.format(
                "Multiple errors occurred: %d total (%d errors, %d warnings, %d info)",
                getErrorCount(),
                getErrorCount(ErrorSeverity.ERROR),
                getErrorCount(ErrorSeverity.WARNING),
                getErrorCount(ErrorSeverity.INFO)
        );

        Map<String, Object> context = new HashMap<>();
        context.put("totalErrors", getErrorCount());
        context.put("errorsByCategory", getErrorSummaryByCategory());
        context.put("errorsBySeverity", getErrorSummaryBySeverity());
        context.put("hasRetryableErrors", hasRetryable);
        context.put("firstErrorTimestamp", errors.stream()
                .min(Comparator.comparing(ErrorDetail::getTimestamp))
                .map(ErrorDetail::getTimestamp)
                .orElse(null));

        return new ErrorDetail.Builder(
                ErrorCodes.SYSTEM_INTERNAL_ERROR,
                ErrorCategory.SYSTEM,
                highestSeverity,
                summaryMessage
        )
                .technicalDetails("Multiple validation or processing errors occurred. See individual errors for details.")
                .retryable(hasRetryable)
                .addContext("errorSummary", context)
                .build();
    }

    /**
     * Convert to string representation
     */
    @Override
    public String toString() {
        return String.format("ErrorAggregator{errors=%d, critical=%d, warnings=%d, retryable=%d}",
                getErrorCount(),
                getErrorCount(ErrorSeverity.ERROR),
                getErrorCount(ErrorSeverity.WARNING),
                hasRetryableErrors() ? getRetryableErrors().size() : 0);
    }
}