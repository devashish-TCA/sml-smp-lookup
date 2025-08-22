package com.tca.peppol.validation;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ValidationResultTest {
    
    @Test
    void testSuccessResult() {
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
    void testSuccessResultWithTiming() {
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
    void testFailureResult() {
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
    void testFailureResultWithTiming() {
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
    void testTimestampIsRecent() {
        Instant before = Instant.now();
        ValidationResult result = ValidationResult.success("Test");
        Instant after = Instant.now();
        
        assertTrue(result.getTimestamp().isAfter(before.minusSeconds(1)));
        assertTrue(result.getTimestamp().isBefore(after.plusSeconds(1)));
    }
    
    @Test
    void testEqualsAndHashCode() {
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
    
    @Test
    void testToStringSuccess() {
        ValidationResult result = ValidationResult.success("Test successful", 150);
        String toString = result.toString();
        
        assertTrue(toString.contains("valid=true"));
        assertTrue(toString.contains("Test successful"));
        assertTrue(toString.contains("time=150ms"));
        assertFalse(toString.contains("errorCode"));
    }
    
    @Test
    void testToStringFailure() {
        ValidationResult result = ValidationResult.failure("E5001", "Test failed", 75);
        String toString = result.toString();
        
        assertTrue(toString.contains("valid=false"));
        assertTrue(toString.contains("errorCode='E5001'"));
        assertTrue(toString.contains("Test failed"));
        assertTrue(toString.contains("time=75ms"));
    }
    
    @Test
    void testEqualsWithNull() {
        ValidationResult result = ValidationResult.success("Test");
        
        assertNotEquals(result, null);
        assertNotEquals(result, "not a ValidationResult");
    }
    
    @Test
    void testEqualsSameInstance() {
        ValidationResult result = ValidationResult.success("Test");
        
        assertEquals(result, result);
    }
    
    @Test
    void testEqualsWithDifferentValidationTimes() {
        ValidationResult result1 = ValidationResult.success("Test", 100);
        ValidationResult result2 = ValidationResult.success("Test", 200);
        
        assertNotEquals(result1, result2);
    }
    
    @Test
    void testNullMessageHandling() {
        ValidationResult successResult = ValidationResult.success(null);
        ValidationResult failureResult = ValidationResult.failure("E001", null);
        
        assertNull(successResult.getMessage());
        assertNull(failureResult.getMessage());
        
        assertTrue(successResult.isValid());
        assertFalse(failureResult.isValid());
    }
}