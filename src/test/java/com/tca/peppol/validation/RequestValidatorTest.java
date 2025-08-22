package com.tca.peppol.validation;

import com.tca.peppol.model.request.LookupRequest;
import com.tca.peppol.util.CorrelationIdUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive unit tests for RequestValidator covering all validation scenarios
 * including peppol-commons integration, security validation, and edge cases.
 */
class RequestValidatorTest {

    private RequestValidator validator;
    private LookupRequest validRequest;

    @BeforeEach
    void setUp() {
        validator = new RequestValidator();
        CorrelationIdUtils.setCorrelationId("test-correlation-id");
        
        // Create a valid request for testing
        validRequest = new LookupRequest();
        validRequest.setParticipantId("iso6523-actorid-upis::9915:test");
        validRequest.setDocumentTypeId("busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1");
        validRequest.setProcessId("cenbii-procid-ubl::urn:fdc:peppol.eu:2017:poacc:billing:01:1.0");
        validRequest.setEnvironment("test");
        validRequest.setRequestId("test-request-123");
    }

    @Nested
    @DisplayName("Valid Request Tests")
    class ValidRequestTests {

        @Test
        @DisplayName("Should validate a completely valid request")
        void shouldValidateValidRequest() {
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrors()).isEmpty();
        }

        @Test
        @DisplayName("Should validate request with production environment")
        void shouldValidateProductionEnvironment() {
            validRequest.setEnvironment("production");
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrors()).isEmpty();
        }

        @Test
        @DisplayName("Should validate request without optional request ID")
        void shouldValidateWithoutRequestId() {
            validRequest.setRequestId(null);
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrors()).isEmpty();
        }

        @Test
        @DisplayName("Should validate request with boolean flags")
        void shouldValidateWithBooleanFlags() {
            validRequest.setValidateEndpointConnectivity(true);
            validRequest.setIncludeFullCertificateChain(true);
            validRequest.setIncludeTechnicalDetails(true);
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrors()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Basic Constraint Validation Tests")
    class BasicConstraintTests {

        @Test
        @DisplayName("Should reject null request")
        void shouldRejectNullRequest() {
            RequestValidationResult result = validator.validateRequest(null);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).contains("E7001: Request cannot be null");
        }

        @Test
        @DisplayName("Should reject request with null participant ID")
        void shouldRejectNullParticipantId() {
            validRequest.setParticipantId(null);
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(error -> error.contains("E7002: Participant ID is required"));
        }

        @Test
        @DisplayName("Should reject request with empty participant ID")
        void shouldRejectEmptyParticipantId() {
            validRequest.setParticipantId("   ");
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(error -> error.contains("E7002: Participant ID is required"));
        }

        @Test
        @DisplayName("Should reject request with null document type ID")
        void shouldRejectNullDocumentTypeId() {
            validRequest.setDocumentTypeId(null);
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(error -> error.contains("E7003: Document type ID is required"));
        }

        @Test
        @DisplayName("Should reject request with null process ID")
        void shouldRejectNullProcessId() {
            validRequest.setProcessId(null);
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(error -> error.contains("E7004: Process ID is required"));
        }

        @Test
        @DisplayName("Should reject request with null environment")
        void shouldRejectNullEnvironment() {
            validRequest.setEnvironment(null);
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(error -> error.contains("E7005: Environment is required"));
        }
    }

    @Nested
    @DisplayName("Length Validation Tests")
    class LengthValidationTests {

        @Test
        @DisplayName("Should reject participant ID exceeding 1000 characters")
        void shouldRejectLongParticipantId() {
            String longId = "iso6523-actorid-upis::" + "a".repeat(1000);
            validRequest.setParticipantId(longId);
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(error -> 
                error.contains("E7011") && error.contains("Participant ID must not exceed 1000 characters"));
        }

        @Test
        @DisplayName("Should reject document type ID exceeding 1000 characters")
        void shouldRejectLongDocumentTypeId() {
            String longId = "busdox-docid-qns::" + "a".repeat(1000);
            validRequest.setDocumentTypeId(longId);
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(error -> 
                error.contains("E7011") && error.contains("Document type ID must not exceed 1000 characters"));
        }

        @Test
        @DisplayName("Should reject process ID exceeding 1000 characters")
        void shouldRejectLongProcessId() {
            String longId = "cenbii-procid-ubl::" + "a".repeat(1000);
            validRequest.setProcessId(longId);
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(error -> 
                error.contains("E7011") && error.contains("Process ID must not exceed 1000 characters"));
        }

        @Test
        @DisplayName("Should reject request ID exceeding 100 characters")
        void shouldRejectLongRequestId() {
            String longId = "a".repeat(101);
            validRequest.setRequestId(longId);
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(error -> 
                error.contains("E7011") && error.contains("Request ID must not exceed 100 characters"));
        }
    }

    @Nested
    @DisplayName("Security Validation Tests")
    class SecurityValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {
            "iso6523-actorid-upis::9915:test'; DROP TABLE users; --",
            "iso6523-actorid-upis::9915:test UNION SELECT * FROM passwords",
            "iso6523-actorid-upis::9915:test' OR '1'='1",
            "iso6523-actorid-upis::9915:test; INSERT INTO logs VALUES ('hack')"
        })
        @DisplayName("Should detect SQL injection patterns in participant ID")
        void shouldDetectSqlInjectionInParticipantId(String maliciousId) {
            validRequest.setParticipantId(maliciousId);
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(error -> 
                error.contains("E7012") && error.contains("SQL injection patterns detected"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "busdox-docid-qns::<script>alert('xss')</script>",
            "busdox-docid-qns::javascript:alert('xss')",
            "busdox-docid-qns::<iframe src='evil.com'></iframe>",
            "busdox-docid-qns::onload=alert('xss')"
        })
        @DisplayName("Should detect XSS patterns in document type ID")
        void shouldDetectXssInDocumentTypeId(String maliciousId) {
            validRequest.setDocumentTypeId(maliciousId);
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(error -> 
                error.contains("E7012") && error.contains("XSS patterns detected"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "cenbii-procid-ubl::<![CDATA[malicious]]>",
            "cenbii-procid-ubl::<!DOCTYPE html>",
            "cenbii-procid-ubl::<!ENTITY xxe SYSTEM 'file:///etc/passwd'>",
            "cenbii-procid-ubl::&xxe;"
        })
        @DisplayName("Should detect XML injection patterns in process ID")
        void shouldDetectXmlInjectionInProcessId(String maliciousId) {
            validRequest.setProcessId(maliciousId);
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(error -> 
                error.contains("E7012") && error.contains("XML injection patterns detected"));
        }

        @Test
        @DisplayName("Should detect unsafe characters")
        void shouldDetectUnsafeCharacters() {
            validRequest.setParticipantId("iso6523-actorid-upis::9915:test<>\"'&");
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(error -> 
                error.contains("E7013") && error.contains("unsafe characters"));
        }
    }

    @Nested
    @DisplayName("Peppol Compliance Tests")
    class PeppolComplianceTests {

        @Test
        @DisplayName("Should validate known Peppol participant identifier")
        void shouldValidateKnownParticipantId() {
            validRequest.setParticipantId("iso6523-actorid-upis::9915:test");
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should reject invalid participant identifier format")
        void shouldRejectInvalidParticipantIdFormat() {
            validRequest.setParticipantId("invalid-format");
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(error -> 
                error.contains("E7006") && error.contains("Invalid participant identifier format"));
        }

        @Test
        @DisplayName("Should reject invalid document type identifier format")
        void shouldRejectInvalidDocumentTypeIdFormat() {
            validRequest.setDocumentTypeId("invalid-format");
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(error -> 
                error.contains("E7007") && error.contains("Invalid document type identifier format"));
        }

        @Test
        @DisplayName("Should reject invalid process identifier format")
        void shouldRejectInvalidProcessIdFormat() {
            validRequest.setProcessId("invalid-format");
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(error -> 
                error.contains("E7008") && error.contains("Invalid process identifier format"));
        }

        @Test
        @DisplayName("Should warn about unknown document type")
        void shouldWarnAboutUnknownDocumentType() {
            validRequest.setDocumentTypeId("busdox-docid-qns::unknown:document:type");
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            // Should still be valid but with warnings
            assertThat(result.isValid()).isTrue();
            assertThat(result.getWarnings()).anyMatch(warning -> 
                warning.contains("not in the list of predefined Peppol document types"));
        }

        @Test
        @DisplayName("Should warn about unknown process identifier")
        void shouldWarnAboutUnknownProcessId() {
            validRequest.setProcessId("cenbii-procid-ubl::unknown:process:id");
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            // Should still be valid but with warnings
            assertThat(result.isValid()).isTrue();
            assertThat(result.getWarnings()).anyMatch(warning -> 
                warning.contains("not in the list of predefined Peppol processes"));
        }
    }

    @Nested
    @DisplayName("Environment Configuration Tests")
    class EnvironmentConfigurationTests {

        @Test
        @DisplayName("Should accept production environment")
        void shouldAcceptProductionEnvironment() {
            validRequest.setEnvironment("production");
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should accept test environment")
        void shouldAcceptTestEnvironment() {
            validRequest.setEnvironment("test");
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should reject invalid environment")
        void shouldRejectInvalidEnvironment() {
            validRequest.setEnvironment("staging");
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(error -> 
                error.contains("E7009") && error.contains("Environment must be either 'production' or 'test'"));
        }

        @Test
        @DisplayName("Should handle case insensitive environment")
        void shouldHandleCaseInsensitiveEnvironment() {
            validRequest.setEnvironment("PRODUCTION");
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should reject invalid request ID format")
        void shouldRejectInvalidRequestIdFormat() {
            validRequest.setRequestId("invalid@request#id!");
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).anyMatch(error -> 
                error.contains("E7010") && error.contains("Request ID can only contain alphanumeric characters"));
        }

        @Test
        @DisplayName("Should accept valid request ID format")
        void shouldAcceptValidRequestIdFormat() {
            validRequest.setRequestId("valid-request_123");
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("Input Sanitization Tests")
    class InputSanitizationTests {

        @Test
        @DisplayName("Should sanitize null input")
        void shouldSanitizeNullInput() {
            String result = validator.sanitizeInput(null);
            
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should remove null bytes and control characters")
        void shouldRemoveNullBytesAndControlCharacters() {
            String input = "test\u0000string\u0001with\u0008control\u001fchars";
            String result = validator.sanitizeInput(input);
            
            assertThat(result).isEqualTo("teststringwithcontrolchars");
        }

        @Test
        @DisplayName("Should trim whitespace")
        void shouldTrimWhitespace() {
            String input = "   test string   ";
            String result = validator.sanitizeInput(input);
            
            assertThat(result).isEqualTo("test string");
        }

        @Test
        @DisplayName("Should limit input length")
        void shouldLimitInputLength() {
            String input = "a".repeat(1500);
            String result = validator.sanitizeInput(input);
            
            assertThat(result).hasSize(1000);
        }
    }

    @Nested
    @DisplayName("Request Size Validation Tests")
    class RequestSizeValidationTests {

        @Test
        @DisplayName("Should accept null request body")
        void shouldAcceptNullRequestBody() {
            boolean result = validator.isRequestSizeValid(null);
            
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should accept valid request size")
        void shouldAcceptValidRequestSize() {
            String requestBody = "a".repeat(5000);
            boolean result = validator.isRequestSizeValid(requestBody);
            
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should reject oversized request")
        void shouldRejectOversizedRequest() {
            String requestBody = "a".repeat(15000);
            boolean result = validator.isRequestSizeValid(requestBody);
            
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle multiple validation errors")
        void shouldHandleMultipleValidationErrors() {
            LookupRequest invalidRequest = new LookupRequest();
            invalidRequest.setParticipantId(null);
            invalidRequest.setDocumentTypeId(null);
            invalidRequest.setProcessId(null);
            invalidRequest.setEnvironment(null);
            
            RequestValidationResult result = validator.validateRequest(invalidRequest);
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).hasSize(4);
            assertThat(result.getErrors()).anyMatch(error -> error.contains("E7002"));
            assertThat(result.getErrors()).anyMatch(error -> error.contains("E7003"));
            assertThat(result.getErrors()).anyMatch(error -> error.contains("E7004"));
            assertThat(result.getErrors()).anyMatch(error -> error.contains("E7005"));
        }

        @Test
        @DisplayName("Should handle validation with both errors and warnings")
        void shouldHandleErrorsAndWarnings() {
            validRequest.setDocumentTypeId("busdox-docid-qns::unknown:document:type");
            validRequest.setProcessId("cenbii-procid-ubl::unknown:process:id");
            
            RequestValidationResult result = validator.validateRequest(validRequest);
            
            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrors()).isEmpty();
            assertThat(result.getWarnings()).hasSize(2);
        }
    }
}