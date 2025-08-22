package com.tca.peppol.exception;

import com.tca.peppol.model.response.ErrorDetail;
import com.tca.peppol.model.response.ErrorDetail.ErrorCategory;
import com.tca.peppol.model.response.ErrorDetail.ErrorSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ErrorAggregator utility class.
 * Tests error aggregation, categorization, and summary functionality.
 */
class ErrorAggregatorTest {

    private ErrorAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new ErrorAggregator();
    }

    @Test
    void testEmptyAggregator() {
        assertFalse(aggregator.hasErrors());
        assertFalse(aggregator.hasCriticalErrors());
        assertFalse(aggregator.hasWarnings());
        assertFalse(aggregator.hasRetryableErrors());
        assertEquals(0, aggregator.getErrorCount());
        assertTrue(aggregator.getAllErrors().isEmpty());
        assertNull(aggregator.getHighestSeverity());
        assertNull(aggregator.createSummaryError());
    }

    @Test
    void testAddSingleError() {
        ErrorDetail error = ErrorClassifier.createError(
            ErrorCodes.SML_DNS_RESOLUTION_FAILED, 
            "DNS resolution failed"
        );

        aggregator.addError(error);

        assertTrue(aggregator.hasErrors());
        assertTrue(aggregator.hasCriticalErrors());
        assertFalse(aggregator.hasWarnings());
        assertTrue(aggregator.hasRetryableErrors());
        assertEquals(1, aggregator.getErrorCount());
        assertEquals(1, aggregator.getErrorCount(ErrorSeverity.ERROR));
        assertEquals(0, aggregator.getErrorCount(ErrorSeverity.WARNING));
        assertEquals(1, aggregator.getErrorCount(ErrorCategory.SML));
        assertEquals(ErrorSeverity.ERROR, aggregator.getHighestSeverity());
    }

    @Test
    void testAddErrorWithCodeAndMessage() {
        aggregator.addError(ErrorCodes.CERT_EXPIRED, "Certificate has expired");

        assertTrue(aggregator.hasErrors());
        assertEquals(1, aggregator.getErrorCount());
        
        ErrorDetail error = aggregator.getAllErrors().get(0);
        assertEquals(ErrorCodes.CERT_EXPIRED, error.getCode());
        assertEquals("Certificate has expired", error.getMessage());
        assertEquals(ErrorCategory.CERTIFICATE, error.getCategory());
    }

    @Test
    void testAddErrorWithTechnicalDetails() {
        aggregator.addError(
            ErrorCodes.SMP_XML_PARSING_FAILED, 
            "XML parsing failed", 
            "Invalid XML structure at line 42"
        );

        assertEquals(1, aggregator.getErrorCount());
        ErrorDetail error = aggregator.getAllErrors().get(0);
        assertEquals("Invalid XML structure at line 42", error.getTechnicalDetails());
    }

    @Test
    void testAddErrorWithException() {
        RuntimeException exception = new RuntimeException("Connection refused");
        aggregator.addError(
            ErrorCodes.NETWORK_CONNECTION_FAILED, 
            "Network connection failed", 
            exception
        );

        assertEquals(1, aggregator.getErrorCount());
        ErrorDetail error = aggregator.getAllErrors().get(0);
        assertTrue(error.getTechnicalDetails().contains("RuntimeException"));
        assertTrue(error.getTechnicalDetails().contains("Connection refused"));
    }

    @Test
    void testAddMultipleErrors() {
        ErrorDetail error1 = ErrorClassifier.createError(ErrorCodes.SML_DNS_RESOLUTION_FAILED, "DNS failed");
        ErrorDetail error2 = ErrorClassifier.createError(ErrorCodes.CERT_EXPIRED, "Certificate expired");
        ErrorDetail error3 = ErrorClassifier.createError(ErrorCodes.OCSP_SERVICE_UNAVAILABLE, "OCSP unavailable");

        aggregator.addErrors(Arrays.asList(error1, error2, error3));

        assertEquals(3, aggregator.getErrorCount());
        assertEquals(2, aggregator.getErrorCount(ErrorSeverity.ERROR));
        assertEquals(1, aggregator.getErrorCount(ErrorSeverity.WARNING));
        assertTrue(aggregator.hasCriticalErrors());
        assertTrue(aggregator.hasWarnings());
        assertTrue(aggregator.hasRetryableErrors());
    }

    @Test
    void testErrorsByCategory() {
        aggregator.addError(ErrorCodes.SML_DNS_RESOLUTION_FAILED, "SML error 1");
        aggregator.addError(ErrorCodes.SML_DNS_TIMEOUT, "SML error 2");
        aggregator.addError(ErrorCodes.CERT_EXPIRED, "Certificate error");
        aggregator.addError(ErrorCodes.NETWORK_TIMEOUT, "Network error");

        assertEquals(2, aggregator.getErrorCount(ErrorCategory.SML));
        assertEquals(1, aggregator.getErrorCount(ErrorCategory.CERTIFICATE));
        assertEquals(1, aggregator.getErrorCount(ErrorCategory.NETWORK));
        assertEquals(0, aggregator.getErrorCount(ErrorCategory.INPUT));

        List<ErrorDetail> smlErrors = aggregator.getErrorsByCategory(ErrorCategory.SML);
        assertEquals(2, smlErrors.size());
        assertTrue(smlErrors.stream().allMatch(e -> e.getCategory() == ErrorCategory.SML));
    }

    @Test
    void testErrorsBySeverity() {
        aggregator.addError(ErrorCodes.SML_DNS_RESOLUTION_FAILED, "Critical error");
        aggregator.addError(ErrorCodes.OCSP_SERVICE_UNAVAILABLE, "Warning");
        aggregator.addError(ErrorCodes.CERT_EXPIRED, "Another critical error");

        assertEquals(2, aggregator.getErrorCount(ErrorSeverity.ERROR));
        assertEquals(1, aggregator.getErrorCount(ErrorSeverity.WARNING));
        assertEquals(0, aggregator.getErrorCount(ErrorSeverity.INFO));

        List<ErrorDetail> criticalErrors = aggregator.getErrorsBySeverity(ErrorSeverity.ERROR);
        assertEquals(2, criticalErrors.size());
        assertTrue(criticalErrors.stream().allMatch(e -> e.getSeverity() == ErrorSeverity.ERROR));
    }

    @Test
    void testErrorsByPriority() {
        // Add errors in mixed order
        aggregator.addError(ErrorCodes.OCSP_SERVICE_UNAVAILABLE, "Warning"); // WARNING
        aggregator.addError(ErrorCodes.SML_DNS_RESOLUTION_FAILED, "Error 1"); // ERROR
        aggregator.addError(ErrorCodes.CERT_EXPIRED, "Error 2"); // ERROR

        List<ErrorDetail> prioritizedErrors = aggregator.getErrorsByPriority();
        assertEquals(3, prioritizedErrors.size());
        
        // First two should be ERROR severity
        assertEquals(ErrorSeverity.ERROR, prioritizedErrors.get(0).getSeverity());
        assertEquals(ErrorSeverity.ERROR, prioritizedErrors.get(1).getSeverity());
        // Last should be WARNING severity
        assertEquals(ErrorSeverity.WARNING, prioritizedErrors.get(2).getSeverity());
    }

    @Test
    void testRetryableErrors() {
        aggregator.addError(ErrorCodes.SML_DNS_RESOLUTION_FAILED, "Retryable error"); // retryable
        aggregator.addError(ErrorCodes.CERT_EXPIRED, "Non-retryable error"); // not retryable
        aggregator.addError(ErrorCodes.NETWORK_TIMEOUT, "Another retryable error"); // retryable

        assertTrue(aggregator.hasRetryableErrors());
        
        List<ErrorDetail> retryableErrors = aggregator.getRetryableErrors();
        assertEquals(2, retryableErrors.size());
        assertTrue(retryableErrors.stream().allMatch(ErrorDetail::isRetryable));

        List<ErrorDetail> nonRetryableErrors = aggregator.getNonRetryableErrors();
        assertEquals(1, nonRetryableErrors.size());
        assertTrue(nonRetryableErrors.stream().noneMatch(ErrorDetail::isRetryable));
    }

    @Test
    void testErrorSummaries() {
        aggregator.addError(ErrorCodes.SML_DNS_RESOLUTION_FAILED, "SML error 1");
        aggregator.addError(ErrorCodes.SML_DNS_TIMEOUT, "SML error 2");
        aggregator.addError(ErrorCodes.CERT_EXPIRED, "Certificate error");
        aggregator.addError(ErrorCodes.OCSP_SERVICE_UNAVAILABLE, "OCSP warning");

        Map<ErrorCategory, Integer> categorySummary = aggregator.getErrorSummaryByCategory();
        assertEquals(2, categorySummary.get(ErrorCategory.SML).intValue());
        assertEquals(1, categorySummary.get(ErrorCategory.CERTIFICATE).intValue());
        assertEquals(1, categorySummary.get(ErrorCategory.EXTERNAL_SERVICE).intValue());

        Map<ErrorSeverity, Integer> severitySummary = aggregator.getErrorSummaryBySeverity();
        assertEquals(3, severitySummary.get(ErrorSeverity.ERROR).intValue());
        assertEquals(1, severitySummary.get(ErrorSeverity.WARNING).intValue());
    }

    @Test
    void testGetHighestSeverity() {
        // Test with only warnings
        aggregator.addError(ErrorCodes.OCSP_SERVICE_UNAVAILABLE, "Warning");
        assertEquals(ErrorSeverity.WARNING, aggregator.getHighestSeverity());

        // Add an error - should become highest
        aggregator.addError(ErrorCodes.CERT_EXPIRED, "Error");
        assertEquals(ErrorSeverity.ERROR, aggregator.getHighestSeverity());
    }

    @Test
    void testGetFirstError() {
        aggregator.addError(ErrorCodes.SML_DNS_RESOLUTION_FAILED, "First SML error");
        aggregator.addError(ErrorCodes.SML_DNS_TIMEOUT, "Second SML error");
        aggregator.addError(ErrorCodes.CERT_EXPIRED, "Certificate error");

        Optional<ErrorDetail> firstSmlError = aggregator.getFirstError(ErrorCategory.SML);
        assertTrue(firstSmlError.isPresent());
        assertEquals("First SML error", firstSmlError.get().getMessage());

        Optional<ErrorDetail> firstError = aggregator.getFirstError(ErrorSeverity.ERROR);
        assertTrue(firstError.isPresent());
        assertEquals("First SML error", firstError.get().getMessage());

        Optional<ErrorDetail> firstInputError = aggregator.getFirstError(ErrorCategory.INPUT);
        assertFalse(firstInputError.isPresent());
    }

    @Test
    void testCreateSummaryError() {
        aggregator.addError(ErrorCodes.SML_DNS_RESOLUTION_FAILED, "SML error");
        aggregator.addError(ErrorCodes.CERT_EXPIRED, "Certificate error");
        aggregator.addError(ErrorCodes.OCSP_SERVICE_UNAVAILABLE, "OCSP warning");

        ErrorDetail summaryError = aggregator.createSummaryError();
        
        assertNotNull(summaryError);
        assertEquals(ErrorCodes.SYSTEM_INTERNAL_ERROR, summaryError.getCode());
        assertEquals(ErrorCategory.SYSTEM, summaryError.getCategory());
        assertEquals(ErrorSeverity.ERROR, summaryError.getSeverity()); // Highest severity
        assertTrue(summaryError.getMessage().contains("Multiple errors occurred"));
        assertTrue(summaryError.getMessage().contains("3 total"));
        assertTrue(summaryError.getMessage().contains("2 errors"));
        assertTrue(summaryError.getMessage().contains("1 warnings"));
        
        Map<String, Object> context = (Map<String, Object>) summaryError.getAdditionalContext().get("errorSummary");
        assertNotNull(context);
        assertEquals(3, context.get("totalErrors"));
        assertTrue((Boolean) context.get("hasRetryableErrors"));
    }

    @Test
    void testClear() {
        aggregator.addError(ErrorCodes.SML_DNS_RESOLUTION_FAILED, "Error 1");
        aggregator.addError(ErrorCodes.CERT_EXPIRED, "Error 2");

        assertTrue(aggregator.hasErrors());
        assertEquals(2, aggregator.getErrorCount());

        aggregator.clear();

        assertFalse(aggregator.hasErrors());
        assertEquals(0, aggregator.getErrorCount());
        assertTrue(aggregator.getAllErrors().isEmpty());
    }

    @Test
    void testToString() {
        aggregator.addError(ErrorCodes.SML_DNS_RESOLUTION_FAILED, "Error 1");
        aggregator.addError(ErrorCodes.OCSP_SERVICE_UNAVAILABLE, "Warning 1");

        String toString = aggregator.toString();
        assertTrue(toString.contains("errors=2"));
        assertTrue(toString.contains("critical=1"));
        assertTrue(toString.contains("warnings=1"));
        assertTrue(toString.contains("retryable=2")); // Both errors are retryable
    }

    @Test
    void testNullErrorHandling() {
        aggregator.addError((ErrorDetail) null);
        aggregator.addErrors(null);

        assertFalse(aggregator.hasErrors());
        assertEquals(0, aggregator.getErrorCount());
    }

    @Test
    void testChainedOperations() {
        ErrorAggregator result = aggregator
            .addError(ErrorCodes.SML_DNS_RESOLUTION_FAILED, "Error 1")
            .addError(ErrorCodes.CERT_EXPIRED, "Error 2")
            .addError(ErrorCodes.OCSP_SERVICE_UNAVAILABLE, "Warning 1");

        assertSame(aggregator, result); // Should return same instance for chaining
        assertEquals(3, aggregator.getErrorCount());
    }
}