package com.tca.peppol.model.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LookupRequest model validation
 */
class LookupRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidLookupRequest() {
        LookupRequest request = new LookupRequest();
        request.setParticipantId("iso6523-actorid-upis::9915:test");
        request.setDocumentTypeId("busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice");
        request.setProcessId("cenbii-procid-ubl::urn:www.cenbii.eu:profile:bii04:ver1.0");
        request.setEnvironment("test");
        request.setRequestId("test-123");

        Set<ConstraintViolation<LookupRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    void testInvalidParticipantId() {
        LookupRequest request = new LookupRequest();
        request.setParticipantId("invalid-participant-id");
        request.setDocumentTypeId("busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice");
        request.setProcessId("cenbii-procid-ubl::urn:www.cenbii.eu:profile:bii04:ver1.0");
        request.setEnvironment("test");

        Set<ConstraintViolation<LookupRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Invalid participant ID should cause violations");
        
        boolean hasParticipantIdViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("participantId"));
        assertTrue(hasParticipantIdViolation, "Should have participant ID validation error");
    }

    @Test
    void testInvalidEnvironment() {
        LookupRequest request = new LookupRequest();
        request.setParticipantId("iso6523-actorid-upis::9915:test");
        request.setDocumentTypeId("busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice");
        request.setProcessId("cenbii-procid-ubl::urn:www.cenbii.eu:profile:bii04:ver1.0");
        request.setEnvironment("invalid");

        Set<ConstraintViolation<LookupRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Invalid environment should cause violations");
        
        boolean hasEnvironmentViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("environment"));
        assertTrue(hasEnvironmentViolation, "Should have environment validation error");
    }

    @Test
    void testNullRequiredFields() {
        LookupRequest request = new LookupRequest();
        // Leave all required fields null

        Set<ConstraintViolation<LookupRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null required fields should cause violations");
        
        // Should have violations for all required fields
        assertEquals(4, violations.size(), "Should have 4 violations for required fields");
    }

    @Test
    void testRequestIdTooLong() {
        LookupRequest request = new LookupRequest();
        request.setParticipantId("iso6523-actorid-upis::9915:test");
        request.setDocumentTypeId("busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice");
        request.setProcessId("cenbii-procid-ubl::urn:www.cenbii.eu:profile:bii04:ver1.0");
        request.setEnvironment("test");
        request.setRequestId("a".repeat(101)); // Exceeds 100 character limit

        Set<ConstraintViolation<LookupRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Request ID too long should cause violations");
        
        boolean hasRequestIdViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("requestId"));
        assertTrue(hasRequestIdViolation, "Should have request ID validation error");
    }

    @Test
    void testConstructorWithRequiredFields() {
        LookupRequest request = new LookupRequest(
                "iso6523-actorid-upis::9915:test",
                "busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice",
                "cenbii-procid-ubl::urn:www.cenbii.eu:profile:bii04:ver1.0",
                "production"
        );

        assertEquals("iso6523-actorid-upis::9915:test", request.getParticipantId());
        assertEquals("busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice", request.getDocumentTypeId());
        assertEquals("cenbii-procid-ubl::urn:www.cenbii.eu:profile:bii04:ver1.0", request.getProcessId());
        assertEquals("production", request.getEnvironment());
        assertFalse(request.isValidateEndpointConnectivity());
        assertFalse(request.isIncludeFullCertificateChain());
        assertFalse(request.isIncludeTechnicalDetails());
    }

    @Test
    void testOptionalFlags() {
        LookupRequest request = new LookupRequest();
        request.setParticipantId("iso6523-actorid-upis::9915:test");
        request.setDocumentTypeId("busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice");
        request.setProcessId("cenbii-procid-ubl::urn:www.cenbii.eu:profile:bii04:ver1.0");
        request.setEnvironment("test");
        request.setValidateEndpointConnectivity(true);
        request.setIncludeFullCertificateChain(true);
        request.setIncludeTechnicalDetails(true);

        assertTrue(request.isValidateEndpointConnectivity());
        assertTrue(request.isIncludeFullCertificateChain());
        assertTrue(request.isIncludeTechnicalDetails());

        Set<ConstraintViolation<LookupRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request with optional flags should have no violations");
    }
}