package com.tca.peppol.exception;

import com.tca.peppol.model.response.ErrorDetail;
import com.tca.peppol.model.response.ErrorDetail.ErrorCategory;
import com.tca.peppol.model.response.ErrorDetail.ErrorSeverity;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ErrorClassifier utility class.
 * Tests error classification, structured error creation, and proper categorization.
 */
class ErrorClassifierTest {

    @Test
    void testCreateErrorWithCodeAndMessage() {
        ErrorDetail error = ErrorClassifier.createError(
            ErrorCodes.SML_DNS_RESOLUTION_FAILED, 
            "DNS resolution failed for participant"
        );

        assertNotNull(error);
        assertEquals(ErrorCodes.SML_DNS_RESOLUTION_FAILED, error.getCode());
        assertEquals(ErrorCategory.SML, error.getCategory());
        assertEquals(ErrorSeverity.ERROR, error.getSeverity());
        assertEquals("DNS resolution failed for participant", error.getMessage());
        assertTrue(error.isRetryable());
        assertNotNull(error.getTimestamp());
        assertEquals("https://docs.peppol-lookup.com/errors/e1001", error.getHelpUrl());
    }

    @Test
    void testCreateErrorWithTechnicalDetails() {
        String technicalDetails = "DNS server returned NXDOMAIN for query";
        ErrorDetail error = ErrorClassifier.createError(
            ErrorCodes.SML_DNS_RESOLUTION_FAILED,
            "DNS resolution failed",
            technicalDetails
        );

        assertNotNull(error);
        assertEquals(technicalDetails, error.getTechnicalDetails());
    }

    @Test
    void testCreateErrorWithException() {
        RuntimeException exception = new RuntimeException("Connection timeout");
        ErrorDetail error = ErrorClassifier.createError(
            ErrorCodes.NETWORK_TIMEOUT,
            "Network operation timed out",
            "Connection to SMP server failed",
            exception
        );

        assertNotNull(error);
        assertEquals(ErrorCodes.NETWORK_TIMEOUT, error.getCode());
        assertEquals(ErrorCategory.NETWORK, error.getCategory());
        assertTrue(error.getTechnicalDetails().contains("Connection to SMP server failed"));
        assertTrue(error.getTechnicalDetails().contains("RuntimeException"));
        assertTrue(error.getTechnicalDetails().contains("Connection timeout"));
    }

    @Test
    void testCreateErrorWithContext() {
        Map<String, Object> context = new HashMap<>();
        context.put("participantId", "0088:1234567890");
        context.put("smpUrl", "https://smp.example.com");
        context.put("attemptNumber", 3);

        ErrorDetail error = ErrorClassifier.createErrorWithContext(
            ErrorCodes.SMP_HTTP_REQUEST_FAILED,
            "SMP request failed",
            "HTTP 500 Internal Server Error",
            context
        );

        assertNotNull(error);
        assertEquals(ErrorCodes.SMP_HTTP_REQUEST_FAILED, error.getCode());
        assertEquals(ErrorCategory.SMP, error.getCategory());
        assertNotNull(error.getAdditionalContext());
        assertEquals("0088:1234567890", error.getAdditionalContext().get("participantId"));
        assertEquals("https://smp.example.com", error.getAdditionalContext().get("smpUrl"));
        assertEquals(3, error.getAdditionalContext().get("attemptNumber"));
    }

    @Test
    void testSmlErrorClassification() {
        ErrorDetail error = ErrorClassifier.createError(ErrorCodes.SML_DNS_TIMEOUT, "DNS timeout");
        assertEquals(ErrorCategory.SML, error.getCategory());
        assertEquals(ErrorSeverity.ERROR, error.getSeverity());
        assertTrue(error.isRetryable());
    }

    @Test
    void testSmpErrorClassification() {
        ErrorDetail error = ErrorClassifier.createError(ErrorCodes.SMP_XML_PARSING_FAILED, "XML parsing failed");
        assertEquals(ErrorCategory.SMP, error.getCategory());
        assertEquals(ErrorSeverity.ERROR, error.getSeverity());
        assertFalse(error.isRetryable());
    }

    @Test
    void testCertificateErrorClassification() {
        ErrorDetail error = ErrorClassifier.createError(ErrorCodes.CERT_EXPIRED, "Certificate expired");
        assertEquals(ErrorCategory.CERTIFICATE, error.getCategory());
        assertEquals(ErrorSeverity.ERROR, error.getSeverity());
        assertFalse(error.isRetryable());
    }

    @Test
    void testNetworkErrorClassification() {
        ErrorDetail error = ErrorClassifier.createError(ErrorCodes.NETWORK_CONNECTION_FAILED, "Connection failed");
        assertEquals(ErrorCategory.NETWORK, error.getCategory());
        assertEquals(ErrorSeverity.ERROR, error.getSeverity());
        assertTrue(error.isRetryable());
    }

    @Test
    void testValidationErrorClassification() {
        ErrorDetail error = ErrorClassifier.createError(ErrorCodes.VALIDATION_XML_SIGNATURE_INVALID, "Invalid signature");
        assertEquals(ErrorCategory.VALIDATION, error.getCategory());
        assertEquals(ErrorSeverity.ERROR, error.getSeverity());
        assertFalse(error.isRetryable());
    }

    @Test
    void testExternalServiceErrorClassification() {
        ErrorDetail error = ErrorClassifier.createError(ErrorCodes.OCSP_SERVICE_UNAVAILABLE, "OCSP unavailable");
        assertEquals(ErrorCategory.EXTERNAL_SERVICE, error.getCategory());
        assertEquals(ErrorSeverity.WARNING, error.getSeverity());
        assertTrue(error.isRetryable());
    }

    @Test
    void testInputErrorClassification() {
        ErrorDetail error = ErrorClassifier.createError(ErrorCodes.INPUT_PARTICIPANT_ID_INVALID, "Invalid participant ID");
        assertEquals(ErrorCategory.INPUT, error.getCategory());
        assertEquals(ErrorSeverity.ERROR, error.getSeverity());
        assertFalse(error.isRetryable());
    }

    @Test
    void testSystemErrorClassification() {
        ErrorDetail error = ErrorClassifier.createError(ErrorCodes.SYSTEM_MEMORY_ERROR, "Out of memory");
        assertEquals(ErrorCategory.SYSTEM, error.getCategory());
        assertEquals(ErrorSeverity.ERROR, error.getSeverity());
        assertTrue(error.isRetryable());
    }

    @Test
    void testGetErrorCategory() {
        assertEquals(ErrorCategory.SML, ErrorClassifier.getErrorCategory(ErrorCodes.SML_DNS_RESOLUTION_FAILED));
        assertEquals(ErrorCategory.SMP, ErrorClassifier.getErrorCategory(ErrorCodes.SMP_HTTP_REQUEST_FAILED));
        assertEquals(ErrorCategory.CERTIFICATE, ErrorClassifier.getErrorCategory(ErrorCodes.CERT_EXPIRED));
        assertEquals(ErrorCategory.NETWORK, ErrorClassifier.getErrorCategory(ErrorCodes.NETWORK_TIMEOUT));
        assertEquals(ErrorCategory.VALIDATION, ErrorClassifier.getErrorCategory(ErrorCodes.VALIDATION_XML_SIGNATURE_INVALID));
        assertEquals(ErrorCategory.EXTERNAL_SERVICE, ErrorClassifier.getErrorCategory(ErrorCodes.OCSP_REQUEST_FAILED));
        assertEquals(ErrorCategory.INPUT, ErrorClassifier.getErrorCategory(ErrorCodes.INPUT_PARTICIPANT_ID_INVALID));
        assertEquals(ErrorCategory.SYSTEM, ErrorClassifier.getErrorCategory(ErrorCodes.SYSTEM_INTERNAL_ERROR));
    }

    @Test
    void testIsRetryable() {
        assertTrue(ErrorClassifier.isRetryable(ErrorCodes.SML_DNS_RESOLUTION_FAILED));
        assertTrue(ErrorClassifier.isRetryable(ErrorCodes.NETWORK_TIMEOUT));
        assertTrue(ErrorClassifier.isRetryable(ErrorCodes.OCSP_SERVICE_UNAVAILABLE));
        
        assertFalse(ErrorClassifier.isRetryable(ErrorCodes.CERT_EXPIRED));
        assertFalse(ErrorClassifier.isRetryable(ErrorCodes.INPUT_PARTICIPANT_ID_INVALID));
        assertFalse(ErrorClassifier.isRetryable(ErrorCodes.VALIDATION_XML_SIGNATURE_INVALID));
    }

    @Test
    void testGetErrorSeverity() {
        assertEquals(ErrorSeverity.ERROR, ErrorClassifier.getErrorSeverity(ErrorCodes.SML_DNS_RESOLUTION_FAILED));
        assertEquals(ErrorSeverity.ERROR, ErrorClassifier.getErrorSeverity(ErrorCodes.CERT_EXPIRED));
        assertEquals(ErrorSeverity.WARNING, ErrorClassifier.getErrorSeverity(ErrorCodes.OCSP_SERVICE_UNAVAILABLE));
        assertEquals(ErrorSeverity.WARNING, ErrorClassifier.getErrorSeverity(ErrorCodes.SML_DNSSEC_VALIDATION_FAILED));
    }

    @Test
    void testIsValidErrorCode() {
        assertTrue(ErrorClassifier.isValidErrorCode(ErrorCodes.SML_DNS_RESOLUTION_FAILED));
        assertTrue(ErrorClassifier.isValidErrorCode(ErrorCodes.SYSTEM_INTERNAL_ERROR));
        
        assertFalse(ErrorClassifier.isValidErrorCode("E9999"));
        assertFalse(ErrorClassifier.isValidErrorCode("INVALID"));
        assertFalse(ErrorClassifier.isValidErrorCode(null));
    }

    @Test
    void testGetHelpUrl() {
        assertEquals("https://docs.peppol-lookup.com/errors/e1001", 
                    ErrorClassifier.getHelpUrl(ErrorCodes.SML_DNS_RESOLUTION_FAILED));
        assertEquals("https://docs.peppol-lookup.com/errors/e8002", 
                    ErrorClassifier.getHelpUrl(ErrorCodes.SYSTEM_INTERNAL_ERROR));
    }

    @Test
    void testUnknownErrorCodeDefaults() {
        ErrorDetail error = ErrorClassifier.createError("E9999", "Unknown error");
        
        assertEquals("E9999", error.getCode());
        assertEquals(ErrorCategory.SYSTEM, error.getCategory()); // Default category
        assertEquals(ErrorSeverity.ERROR, error.getSeverity()); // Default severity
        assertFalse(error.isRetryable()); // Default retryability
    }

    @Test
    void testWarningLevelErrors() {
        // Test that some errors are properly classified as warnings
        ErrorDetail ocspError = ErrorClassifier.createError(ErrorCodes.OCSP_SERVICE_UNAVAILABLE, "OCSP unavailable");
        assertEquals(ErrorSeverity.WARNING, ocspError.getSeverity());
        
        ErrorDetail dnssecError = ErrorClassifier.createError(ErrorCodes.SML_DNSSEC_VALIDATION_FAILED, "DNSSEC failed");
        assertEquals(ErrorSeverity.WARNING, dnssecError.getSeverity());
        
        ErrorDetail endpointError = ErrorClassifier.createError(ErrorCodes.VALIDATION_ENDPOINT_NOT_ACCESSIBLE, "Endpoint not accessible");
        assertEquals(ErrorSeverity.WARNING, endpointError.getSeverity());
    }

    @Test
    void testErrorBuilderPattern() {
        ErrorDetail error = new ErrorDetail.Builder(
            ErrorCodes.SMP_HTTP_REQUEST_FAILED,
            ErrorCategory.SMP,
            ErrorSeverity.ERROR,
            "SMP request failed"
        )
        .technicalDetails("HTTP 500 Internal Server Error")
        .retryable(true)
        .helpUrl("https://custom-help.com")
        .addContext("statusCode", 500)
        .build();

        assertEquals(ErrorCodes.SMP_HTTP_REQUEST_FAILED, error.getCode());
        assertEquals(ErrorCategory.SMP, error.getCategory());
        assertEquals(ErrorSeverity.ERROR, error.getSeverity());
        assertEquals("SMP request failed", error.getMessage());
        assertEquals("HTTP 500 Internal Server Error", error.getTechnicalDetails());
        assertTrue(error.isRetryable());
        assertEquals("https://custom-help.com", error.getHelpUrl());
        assertEquals(500, error.getAdditionalContext().get("statusCode"));
    }
}