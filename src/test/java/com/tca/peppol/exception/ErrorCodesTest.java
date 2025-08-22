package com.tca.peppol.exception;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ErrorCodes constants class.
 * Validates error code format, uniqueness, and proper categorization.
 */
class ErrorCodesTest {

    @Test
    void testErrorCodeFormat() throws IllegalAccessException {
        Field[] fields = ErrorCodes.class.getDeclaredFields();
        
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) && 
                Modifier.isFinal(field.getModifiers()) && 
                field.getType() == String.class) {
                
                String errorCode = (String) field.get(null);
                
                // Test error code format: Exxxx where xxxx is 4 digits
                assertTrue(errorCode.matches("E\\d{4}"), 
                    "Error code " + errorCode + " does not match format Exxxx");
                
                // Test error code starts with correct category prefix
                String fieldName = field.getName();
                char categoryPrefix = errorCode.charAt(1);
                
                if (fieldName.startsWith("SML_")) {
                    assertEquals('1', categoryPrefix, 
                        "SML error code " + errorCode + " should start with E1");
                } else if (fieldName.startsWith("SMP_")) {
                    assertEquals('2', categoryPrefix, 
                        "SMP error code " + errorCode + " should start with E2");
                } else if (fieldName.startsWith("CERT_")) {
                    assertEquals('3', categoryPrefix, 
                        "CERT error code " + errorCode + " should start with E3");
                } else if (fieldName.startsWith("NETWORK_")) {
                    assertEquals('4', categoryPrefix, 
                        "NETWORK error code " + errorCode + " should start with E4");
                } else if (fieldName.startsWith("VALIDATION_")) {
                    assertEquals('5', categoryPrefix, 
                        "VALIDATION error code " + errorCode + " should start with E5");
                } else if (fieldName.startsWith("OCSP_") || fieldName.startsWith("CRL_") || 
                          fieldName.startsWith("EXTERNAL_SERVICE_")) {
                    assertEquals('6', categoryPrefix, 
                        "EXTERNAL_SERVICE error code " + errorCode + " should start with E6");
                } else if (fieldName.startsWith("INPUT_")) {
                    assertEquals('7', categoryPrefix, 
                        "INPUT error code " + errorCode + " should start with E7");
                } else if (fieldName.startsWith("SYSTEM_")) {
                    assertEquals('8', categoryPrefix, 
                        "SYSTEM error code " + errorCode + " should start with E8");
                }
            }
        }
    }

    @Test
    void testErrorCodeUniqueness() throws IllegalAccessException {
        Field[] fields = ErrorCodes.class.getDeclaredFields();
        Set<String> errorCodes = new HashSet<>();
        Set<String> legacyFields = Set.of(
            "INPUT_PARTICIPANT_ID_INVALID", "INPUT_DOCUMENT_TYPE_INVALID", 
            "INPUT_PROCESS_ID_INVALID", "INPUT_ENVIRONMENT_INVALID",
            "INPUT_REQUEST_TOO_LARGE", "INPUT_MALICIOUS_CONTENT_DETECTED",
            "INPUT_ENCODING_INVALID", "INPUT_REQUIRED_FIELD_MISSING",
            "INPUT_FORMAT_INVALID", "INPUT_LENGTH_EXCEEDED"
        );
        
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) && 
                Modifier.isFinal(field.getModifiers()) && 
                field.getType() == String.class) {
                
                String errorCode = (String) field.get(null);
                
                // Skip legacy fields as they are intentional aliases
                if (legacyFields.contains(field.getName())) {
                    continue;
                }
                
                assertFalse(errorCodes.contains(errorCode), 
                    "Duplicate error code found: " + errorCode);
                errorCodes.add(errorCode);
            }
        }
        
        // Verify we have a reasonable number of error codes
        assertTrue(errorCodes.size() >= 80, 
            "Expected at least 80 error codes, found: " + errorCodes.size());
    }

    @Test
    void testSmlErrorCodes() {
        // Test SML error codes (E1xxx)
        assertEquals("E1001", ErrorCodes.SML_DNS_RESOLUTION_FAILED);
        assertEquals("E1002", ErrorCodes.SML_HASH_CALCULATION_FAILED);
        assertEquals("E1003", ErrorCodes.SML_DNS_QUERY_CONSTRUCTION_FAILED);
        assertEquals("E1004", ErrorCodes.SML_DNS_TIMEOUT);
        assertEquals("E1005", ErrorCodes.SML_DNSSEC_VALIDATION_FAILED);
        assertEquals("E1006", ErrorCodes.SML_INVALID_DOMAIN);
        assertEquals("E1007", ErrorCodes.SML_DNS_SERVER_ERROR);
        assertEquals("E1008", ErrorCodes.SML_PARTICIPANT_NOT_FOUND);
        assertEquals("E1009", ErrorCodes.SML_MULTIPLE_RESULTS);
        assertEquals("E1010", ErrorCodes.SML_CACHE_ERROR);
    }

    @Test
    void testSmpErrorCodes() {
        // Test SMP error codes (E2xxx)
        assertEquals("E2001", ErrorCodes.SMP_HTTP_REQUEST_FAILED);
        assertEquals("E2002", ErrorCodes.SMP_XML_PARSING_FAILED);
        assertEquals("E2003", ErrorCodes.SMP_URL_CONSTRUCTION_FAILED);
        assertEquals("E2004", ErrorCodes.SMP_HTTP_TIMEOUT);
        assertEquals("E2005", ErrorCodes.SMP_INVALID_RESPONSE);
        assertEquals("E2006", ErrorCodes.SMP_SERVICE_NOT_FOUND);
        assertEquals("E2007", ErrorCodes.SMP_CERTIFICATE_EXTRACTION_FAILED);
        assertEquals("E2008", ErrorCodes.SMP_XML_SIGNATURE_MISSING);
        assertEquals("E2009", ErrorCodes.SMP_MALFORMED_XML);
        assertEquals("E2010", ErrorCodes.SMP_UNSUPPORTED_VERSION);
    }

    @Test
    void testCertificateErrorCodes() {
        // Test Certificate error codes (E3xxx)
        assertEquals("E3001", ErrorCodes.CERT_VALIDATION_FAILED);
        assertEquals("E3002", ErrorCodes.CERT_EXPIRED);
        assertEquals("E3003", ErrorCodes.CERT_NOT_YET_VALID);
        assertEquals("E3004", ErrorCodes.CERT_REVOKED);
        assertEquals("E3005", ErrorCodes.CERT_CHAIN_INVALID);
        assertEquals("E3006", ErrorCodes.CERT_SIGNATURE_INVALID);
        assertEquals("E3007", ErrorCodes.CERT_KEY_LENGTH_INSUFFICIENT);
        assertEquals("E3008", ErrorCodes.CERT_POLICY_VIOLATION);
        assertEquals("E3009", ErrorCodes.CERT_NOT_PEPPOL_COMPLIANT);
        assertEquals("E3010", ErrorCodes.CERT_PARSING_FAILED);
        assertEquals("E3011", ErrorCodes.CERT_CHAIN_BUILDING_FAILED);
        assertEquals("E3012", ErrorCodes.CERT_ROOT_CA_INVALID);
    }

    @Test
    void testNetworkErrorCodes() {
        // Test Network error codes (E4xxx)
        assertEquals("E4001", ErrorCodes.NETWORK_CONNECTION_FAILED);
        assertEquals("E4002", ErrorCodes.NETWORK_TIMEOUT);
        assertEquals("E4003", ErrorCodes.NETWORK_SSL_HANDSHAKE_FAILED);
        assertEquals("E4004", ErrorCodes.NETWORK_DNS_RESOLUTION_FAILED);
        assertEquals("E4005", ErrorCodes.NETWORK_PROXY_ERROR);
        assertEquals("E4006", ErrorCodes.NETWORK_CONNECTION_REFUSED);
        assertEquals("E4007", ErrorCodes.NETWORK_HOST_UNREACHABLE);
        assertEquals("E4008", ErrorCodes.NETWORK_SOCKET_TIMEOUT);
        assertEquals("E4009", ErrorCodes.NETWORK_SSL_CERTIFICATE_ERROR);
        assertEquals("E4010", ErrorCodes.NETWORK_TOO_MANY_REDIRECTS);
    }

    @Test
    void testValidationErrorCodes() {
        // Test Validation error codes (E5xxx)
        assertEquals("E5001", ErrorCodes.VALIDATION_XML_SIGNATURE_INVALID);
        assertEquals("E5002", ErrorCodes.VALIDATION_CANONICALIZATION_FAILED);
        assertEquals("E5003", ErrorCodes.VALIDATION_SIGNATURE_ALGORITHM_WEAK);
        assertEquals("E5004", ErrorCodes.VALIDATION_REFERENCE_INVALID);
        assertEquals("E5005", ErrorCodes.VALIDATION_ENDPOINT_INVALID);
        assertEquals("E5006", ErrorCodes.VALIDATION_TRANSPORT_PROFILE_UNSUPPORTED);
        assertEquals("E5007", ErrorCodes.VALIDATION_ENDPOINT_NOT_ACCESSIBLE);
        assertEquals("E5008", ErrorCodes.VALIDATION_PEPPOL_COMPLIANCE_FAILED);
        assertEquals("E5009", ErrorCodes.VALIDATION_BUSINESS_RULES_VIOLATED);
        assertEquals("E5010", ErrorCodes.VALIDATION_SCHEMA_VALIDATION_FAILED);
    }

    @Test
    void testExternalServiceErrorCodes() {
        // Test External Service error codes (E6xxx)
        assertEquals("E6001", ErrorCodes.OCSP_REQUEST_FAILED);
        assertEquals("E6002", ErrorCodes.OCSP_RESPONSE_INVALID);
        assertEquals("E6003", ErrorCodes.OCSP_SERVICE_UNAVAILABLE);
        assertEquals("E6004", ErrorCodes.OCSP_TIMEOUT);
        assertEquals("E6005", ErrorCodes.CRL_DOWNLOAD_FAILED);
        assertEquals("E6006", ErrorCodes.CRL_SIGNATURE_INVALID);
        assertEquals("E6007", ErrorCodes.CRL_EXPIRED);
        assertEquals("E6008", ErrorCodes.CRL_PARSING_FAILED);
        assertEquals("E6009", ErrorCodes.EXTERNAL_SERVICE_RATE_LIMITED);
        assertEquals("E6010", ErrorCodes.EXTERNAL_SERVICE_CIRCUIT_BREAKER_OPEN);
    }

    @Test
    void testInputErrorCodes() {
        // Test Input error codes (E7xxx)
        assertEquals("E7001", ErrorCodes.INPUT_PARTICIPANT_ID_INVALID);
        assertEquals("E7002", ErrorCodes.INPUT_DOCUMENT_TYPE_INVALID);
        assertEquals("E7003", ErrorCodes.INPUT_PROCESS_ID_INVALID);
        assertEquals("E7004", ErrorCodes.INPUT_ENVIRONMENT_INVALID);
        assertEquals("E7005", ErrorCodes.INPUT_REQUEST_TOO_LARGE);
        assertEquals("E7006", ErrorCodes.INPUT_MALICIOUS_CONTENT_DETECTED);
        assertEquals("E7007", ErrorCodes.INPUT_ENCODING_INVALID);
        assertEquals("E7008", ErrorCodes.INPUT_REQUIRED_FIELD_MISSING);
        assertEquals("E7009", ErrorCodes.INPUT_FORMAT_INVALID);
        assertEquals("E7010", ErrorCodes.INPUT_LENGTH_EXCEEDED);
    }

    @Test
    void testSystemErrorCodes() {
        // Test System error codes (E8xxx)
        assertEquals("E8001", ErrorCodes.SYSTEM_CONFIGURATION_ERROR);
        assertEquals("E8002", ErrorCodes.SYSTEM_INTERNAL_ERROR);
        assertEquals("E8003", ErrorCodes.SYSTEM_MEMORY_ERROR);
        assertEquals("E8004", ErrorCodes.SYSTEM_THREAD_POOL_EXHAUSTED);
        assertEquals("E8005", ErrorCodes.SYSTEM_CACHE_ERROR);
        assertEquals("E8006", ErrorCodes.SYSTEM_SERIALIZATION_ERROR);
        assertEquals("E8007", ErrorCodes.SYSTEM_RESOURCE_UNAVAILABLE);
        assertEquals("E8008", ErrorCodes.SYSTEM_INITIALIZATION_FAILED);
        assertEquals("E8009", ErrorCodes.SYSTEM_SHUTDOWN_ERROR);
        assertEquals("E8010", ErrorCodes.SYSTEM_DEPENDENCY_UNAVAILABLE);
    }

    @Test
    void testErrorCodeCoverage() throws IllegalAccessException {
        // Ensure we have error codes for each category range
        Field[] fields = ErrorCodes.class.getDeclaredFields();
        Set<String> categories = new HashSet<>();
        
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) && 
                Modifier.isFinal(field.getModifiers()) && 
                field.getType() == String.class) {
                
                String errorCode = (String) field.get(null);
                String category = errorCode.substring(0, 2); // E1, E2, etc.
                categories.add(category);
            }
        }
        
        // Verify all 8 categories are covered
        assertTrue(categories.contains("E1"), "Missing SML error codes (E1xxx)");
        assertTrue(categories.contains("E2"), "Missing SMP error codes (E2xxx)");
        assertTrue(categories.contains("E3"), "Missing Certificate error codes (E3xxx)");
        assertTrue(categories.contains("E4"), "Missing Network error codes (E4xxx)");
        assertTrue(categories.contains("E5"), "Missing Validation error codes (E5xxx)");
        assertTrue(categories.contains("E6"), "Missing External Service error codes (E6xxx)");
        assertTrue(categories.contains("E7"), "Missing Input error codes (E7xxx)");
        assertTrue(categories.contains("E8"), "Missing System error codes (E8xxx)");
    }
}