package com.tca.peppol.client;

import com.tca.peppol.validation.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Consolidated test suite for result classes that follow similar patterns.
 * This reduces duplication and makes it easier to maintain consistent testing patterns.
 */
@DisplayName("Result Classes Test Suite")
class ResultClassesTestSuite {

    @Nested
    @DisplayName("CrlResult Tests")
    class CrlResultTests {
        
        private static final String TEST_CRL_URL = "https://example.com/test.crl";
        private static final Instant TEST_TIME = Instant.parse("2024-01-01T12:00:00Z");
        private static final Instant NEXT_UPDATE = Instant.parse("2024-01-02T12:00:00Z");
        
        @Test
        @DisplayName("Should create good result with correct properties")
        void shouldCreateGoodResult() {
            CrlResult result = CrlResult.good(TEST_CRL_URL, TEST_TIME, NEXT_UPDATE);
            
            assertEquals(CrlResult.Status.GOOD, result.getStatus());
            assertEquals("Certificate is not revoked according to CRL", result.getMessage());
            assertEquals(TEST_CRL_URL, result.getCrlUrl());
            assertEquals(TEST_TIME, result.getThisUpdate());
            assertEquals(NEXT_UPDATE, result.getNextUpdate());
            assertNull(result.getRevocationTime());
            assertNull(result.getRevocationReason());
            assertEquals(0, result.getResponseTimeMs());
            assertNotNull(result.getTimestamp());
            
            assertTrue(result.isSuccessful());
            assertTrue(result.isValid());
            assertFalse(result.isRevoked());
            assertFalse(result.isUnavailable());
            assertFalse(result.isError());
        }
        
        @Test
        @DisplayName("Should create revoked result with correct properties")
        void shouldCreateRevokedResult() {
            Instant revocationTime = Instant.parse("2023-12-01T10:00:00Z");
            int revocationReason = 1; // Key Compromise
            
            CrlResult result = CrlResult.revoked(TEST_CRL_URL, revocationTime, revocationReason, TEST_TIME, NEXT_UPDATE);
            
            assertEquals(CrlResult.Status.REVOKED, result.getStatus());
            assertTrue(result.getMessage().contains("Certificate revoked"));
            assertTrue(result.getMessage().contains("Key Compromise"));
            assertEquals(TEST_CRL_URL, result.getCrlUrl());
            assertEquals(revocationTime, result.getRevocationTime());
            assertEquals(Integer.valueOf(revocationReason), result.getRevocationReason());
            assertEquals(TEST_TIME, result.getThisUpdate());
            assertEquals(NEXT_UPDATE, result.getNextUpdate());
            
            assertTrue(result.isSuccessful());
            assertFalse(result.isValid());
            assertTrue(result.isRevoked());
            assertFalse(result.isUnavailable());
            assertFalse(result.isError());
        }
        
        @Test
        @DisplayName("Should create unavailable result")
        void shouldCreateUnavailableResult() {
            String reason = "CRL server is down";
            CrlResult result = CrlResult.unavailable(TEST_CRL_URL, reason);
            
            assertEquals(CrlResult.Status.UNAVAILABLE, result.getStatus());
            assertEquals("CRL unavailable: " + reason, result.getMessage());
            assertEquals(TEST_CRL_URL, result.getCrlUrl());
            assertNull(result.getRevocationTime());
            assertNull(result.getRevocationReason());
            assertNull(result.getThisUpdate());
            assertNull(result.getNextUpdate());
            
            assertFalse(result.isSuccessful());
            assertFalse(result.isValid());
            assertFalse(result.isRevoked());
            assertTrue(result.isUnavailable());
            assertFalse(result.isError());
        }
        
        @Test
        @DisplayName("Should create error result")
        void shouldCreateErrorResult() {
            String errorMessage = "Network timeout";
            CrlResult result = CrlResult.error(TEST_CRL_URL, errorMessage);
            
            assertEquals(CrlResult.Status.ERROR, result.getStatus());
            assertEquals("CRL check error: " + errorMessage, result.getMessage());
            assertEquals(TEST_CRL_URL, result.getCrlUrl());
            assertNull(result.getRevocationTime());
            assertNull(result.getRevocationReason());
            assertNull(result.getThisUpdate());
            assertNull(result.getNextUpdate());
            
            assertFalse(result.isSuccessful());
            assertFalse(result.isValid());
            assertFalse(result.isRevoked());
            assertFalse(result.isUnavailable());
            assertTrue(result.isError());
        }
        
        @Test
        @DisplayName("Should support response time tracking")
        void shouldSupportResponseTimeTracking() {
            long responseTime = 1500L;
            CrlResult original = CrlResult.good(TEST_CRL_URL, TEST_TIME, NEXT_UPDATE);
            CrlResult withTime = original.withResponseTime(responseTime);
            
            assertEquals(responseTime, withTime.getResponseTimeMs());
            assertEquals(original.getStatus(), withTime.getStatus());
            assertEquals(original.getMessage(), withTime.getMessage());
            assertEquals(original.getCrlUrl(), withTime.getCrlUrl());
        }
    }

    @Nested
    @DisplayName("OcspResult Tests")
    class OcspResultTests {
        
        @Test
        @DisplayName("Should create good result with correct properties")
        void shouldCreateGoodResult() {
            Instant thisUpdate = Instant.now();
            Instant nextUpdate = thisUpdate.plus(1, ChronoUnit.HOURS);
            
            OcspResult result = OcspResult.good(thisUpdate, nextUpdate);
            
            assertThat(result.getStatus()).isEqualTo(OcspResult.Status.GOOD);
            assertThat(result.getMessage()).isEqualTo("Certificate is not revoked");
            assertThat(result.getThisUpdate()).isEqualTo(thisUpdate);
            assertThat(result.getNextUpdate()).isEqualTo(nextUpdate);
            assertThat(result.getRevocationTime()).isNull();
            assertThat(result.getRevocationReason()).isNull();
            assertThat(result.isValid()).isTrue();
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.isRevoked()).isFalse();
            assertThat(result.isUnknown()).isFalse();
            assertThat(result.isUnavailable()).isFalse();
            assertThat(result.isError()).isFalse();
        }
        
        @Test
        @DisplayName("Should create revoked result with correct properties")
        void shouldCreateRevokedResult() {
            Instant revocationTime = Instant.now().minus(1, ChronoUnit.DAYS);
            Instant thisUpdate = Instant.now();
            Instant nextUpdate = thisUpdate.plus(1, ChronoUnit.HOURS);
            int revocationReason = 1; // Key Compromise
            
            OcspResult result = OcspResult.revoked(revocationTime, revocationReason, thisUpdate, nextUpdate);
            
            assertThat(result.getStatus()).isEqualTo(OcspResult.Status.REVOKED);
            assertThat(result.getMessage()).contains("Certificate revoked");
            assertThat(result.getMessage()).contains("Key Compromise");
            assertThat(result.getRevocationTime()).isEqualTo(revocationTime);
            assertThat(result.getRevocationReason()).isEqualTo(revocationReason);
            assertThat(result.getThisUpdate()).isEqualTo(thisUpdate);
            assertThat(result.getNextUpdate()).isEqualTo(nextUpdate);
            assertThat(result.isValid()).isFalse();
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.isRevoked()).isTrue();
            assertThat(result.isUnknown()).isFalse();
            assertThat(result.isUnavailable()).isFalse();
            assertThat(result.isError()).isFalse();
        }
        
        @Test
        @DisplayName("Should create unknown result")
        void shouldCreateUnknownResult() {
            Instant thisUpdate = Instant.now();
            Instant nextUpdate = thisUpdate.plus(1, ChronoUnit.HOURS);
            
            OcspResult result = OcspResult.unknown(thisUpdate, nextUpdate);
            
            assertThat(result.getStatus()).isEqualTo(OcspResult.Status.UNKNOWN);
            assertThat(result.getMessage()).isEqualTo("Certificate status is unknown");
            assertThat(result.getThisUpdate()).isEqualTo(thisUpdate);
            assertThat(result.getNextUpdate()).isEqualTo(nextUpdate);
            assertThat(result.getRevocationTime()).isNull();
            assertThat(result.getRevocationReason()).isNull();
            assertThat(result.isValid()).isFalse();
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.isRevoked()).isFalse();
            assertThat(result.isUnknown()).isTrue();
            assertThat(result.isUnavailable()).isFalse();
            assertThat(result.isError()).isFalse();
        }
        
        @Test
        @DisplayName("Should create unavailable result")
        void shouldCreateUnavailableResult() {
            String reason = "OCSP responder is down";
            
            OcspResult result = OcspResult.unavailable(reason);
            
            assertThat(result.getStatus()).isEqualTo(OcspResult.Status.UNAVAILABLE);
            assertThat(result.getMessage()).isEqualTo("OCSP service unavailable: " + reason);
            assertThat(result.getThisUpdate()).isNull();
            assertThat(result.getNextUpdate()).isNull();
            assertThat(result.getRevocationTime()).isNull();
            assertThat(result.getRevocationReason()).isNull();
            assertThat(result.isValid()).isFalse();
            assertThat(result.isSuccessful()).isFalse();
            assertThat(result.isRevoked()).isFalse();
            assertThat(result.isUnknown()).isFalse();
            assertThat(result.isUnavailable()).isTrue();
            assertThat(result.isError()).isFalse();
        }
        
        @Test
        @DisplayName("Should create error result")
        void shouldCreateErrorResult() {
            String errorMessage = "Network connection failed";
            
            OcspResult result = OcspResult.error(errorMessage);
            
            assertThat(result.getStatus()).isEqualTo(OcspResult.Status.ERROR);
            assertThat(result.getMessage()).isEqualTo("OCSP check error: " + errorMessage);
            assertThat(result.getThisUpdate()).isNull();
            assertThat(result.getNextUpdate()).isNull();
            assertThat(result.getRevocationTime()).isNull();
            assertThat(result.getRevocationReason()).isNull();
            assertThat(result.isValid()).isFalse();
            assertThat(result.isSuccessful()).isFalse();
            assertThat(result.isRevoked()).isFalse();
            assertThat(result.isUnknown()).isFalse();
            assertThat(result.isUnavailable()).isFalse();
            assertThat(result.isError()).isTrue();
        }
        
        @Test
        @DisplayName("Should support response time tracking")
        void shouldSupportResponseTimeTracking() {
            OcspResult original = OcspResult.good(Instant.now(), null);
            long responseTime = 1500L;
            
            OcspResult withTime = original.withResponseTime(responseTime);
            
            assertThat(withTime.getResponseTimeMs()).isEqualTo(responseTime);
            assertThat(withTime.getStatus()).isEqualTo(original.getStatus());
            assertThat(withTime.getMessage()).isEqualTo(original.getMessage());
        }
    }

    @Nested
    @DisplayName("ValidationResult Tests")
    class ValidationResultTests {
        
        @Test
        @DisplayName("Should create success result")
        void shouldCreateSuccessResult() {
            String message = "Validation successful";
            ValidationResult result = ValidationResult.success(message);
            
            assertTrue(result.isValid());
            assertFalse(result.isFailure());
            assertEquals(message, result.getMessage());
            assertNull(result.getErrorCode());
            assertEquals(0, result.getValidationTimeMs());
            assertNotNull(result.getTimestamp());
        }
        
        @Test
        @DisplayName("Should create success result with timing")
        void shouldCreateSuccessResultWithTiming() {
            String message = "Validation successful";
            long validationTime = 150;
            ValidationResult result = ValidationResult.success(message, validationTime);
            
            assertTrue(result.isValid());
            assertFalse(result.isFailure());
            assertEquals(message, result.getMessage());
            assertNull(result.getErrorCode());
            assertEquals(validationTime, result.getValidationTimeMs());
            assertNotNull(result.getTimestamp());
        }
        
        @Test
        @DisplayName("Should create failure result")
        void shouldCreateFailureResult() {
            String errorCode = "E5001";
            String message = "Validation failed";
            ValidationResult result = ValidationResult.failure(errorCode, message);
            
            assertFalse(result.isValid());
            assertTrue(result.isFailure());
            assertEquals(message, result.getMessage());
            assertEquals(errorCode, result.getErrorCode());
            assertEquals(0, result.getValidationTimeMs());
            assertNotNull(result.getTimestamp());
        }
        
        @Test
        @DisplayName("Should create failure result with timing")
        void shouldCreateFailureResultWithTiming() {
            String errorCode = "E5001";
            String message = "Validation failed";
            long validationTime = 75;
            ValidationResult result = ValidationResult.failure(errorCode, message, validationTime);
            
            assertFalse(result.isValid());
            assertTrue(result.isFailure());
            assertEquals(message, result.getMessage());
            assertEquals(errorCode, result.getErrorCode());
            assertEquals(validationTime, result.getValidationTimeMs());
            assertNotNull(result.getTimestamp());
        }
        
        @Test
        @DisplayName("Should have recent timestamp")
        void shouldHaveRecentTimestamp() {
            Instant before = Instant.now();
            ValidationResult result = ValidationResult.success("Test");
            Instant after = Instant.now();
            
            assertTrue(result.getTimestamp().isAfter(before.minusSeconds(1)));
            assertTrue(result.getTimestamp().isBefore(after.plusSeconds(1)));
        }
        
        @Test
        @DisplayName("Should support equality and hashCode")
        void shouldSupportEqualityAndHashCode() {
            ValidationResult result1 = ValidationResult.success("Test message", 100);
            ValidationResult result2 = ValidationResult.success("Test message", 100);
            ValidationResult result3 = ValidationResult.failure("E001", "Test message", 100);
            ValidationResult result4 = ValidationResult.success("Different message", 100);
            
            // Note: equals doesn't compare timestamps, so these should be equal
            assertEquals(result1, result2);
            assertEquals(result1.hashCode(), result2.hashCode());
            
            assertNotEquals(result1, result3);
            assertNotEquals(result1, result4);
            assertNotEquals(result1.hashCode(), result3.hashCode());
            assertNotEquals(result1.hashCode(), result4.hashCode());
        }
    }

    @Nested
    @DisplayName("Common Result Patterns")
    class CommonResultPatterns {
        
        @Test
        @DisplayName("All result classes should have meaningful toString")
        void allResultClassesShouldHaveMeaningfulToString() {
            // Test CrlResult toString
            CrlResult crlResult = CrlResult.good("https://test.com/crl", Instant.now(), null);
            String crlString = crlResult.toString();
            assertThat(crlString).contains("CrlResult");
            assertThat(crlString).contains("GOOD");
            
            // Test OcspResult toString
            OcspResult ocspResult = OcspResult.good(Instant.now(), null);
            String ocspString = ocspResult.toString();
            assertThat(ocspString).contains("OcspResult");
            assertThat(ocspString).contains("GOOD");
            
            // Test ValidationResult toString
            ValidationResult validationResult = ValidationResult.success("Test", 100);
            String validationString = validationResult.toString();
            assertThat(validationString).contains("valid=true");
            assertThat(validationString).contains("Test");
        }
        
        @Test
        @DisplayName("All result classes should handle null values gracefully")
        void allResultClassesShouldHandleNullValuesGracefully() {
            // Test CrlResult with null values
            CrlResult crlResult = CrlResult.good(null, null, null);
            assertThat(crlResult.getCrlUrl()).isNull();
            assertThat(crlResult.getThisUpdate()).isNull();
            assertThat(crlResult.getNextUpdate()).isNull();
            assertThat(crlResult.toString()).isNotNull();
            
            // Test ValidationResult with null message
            ValidationResult validationResult = ValidationResult.success(null);
            assertThat(validationResult.getMessage()).isNull();
            assertThat(validationResult.isValid()).isTrue();
        }
        
        @Test
        @DisplayName("All result classes should have consistent timestamp behavior")
        void allResultClassesShouldHaveConsistentTimestampBehavior() {
            Instant before = Instant.now();
            
            CrlResult crlResult = CrlResult.good("test", null, null);
            OcspResult ocspResult = OcspResult.good(null, null);
            ValidationResult validationResult = ValidationResult.success("test");
            
            Instant after = Instant.now();
            
            // All should have timestamps set
            assertThat(crlResult.getTimestamp()).isBetween(before, after);
            assertThat(ocspResult.getTimestamp()).isBetween(before, after);
            assertThat(validationResult.getTimestamp()).isBetween(before, after);
        }
    }
}
