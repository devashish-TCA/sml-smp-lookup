package com.tca.peppol.client;

import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CrlResult class.
 */
class CrlResultTest {
    
    private static final String TEST_CRL_URL = "https://example.com/test.crl";
    private static final Instant TEST_TIME = Instant.parse("2024-01-01T12:00:00Z");
    private static final Instant NEXT_UPDATE = Instant.parse("2024-01-02T12:00:00Z");
    
    @Test
    void testGoodResult() {
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
    void testRevokedResult() {
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
    void testUnavailableResult() {
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
    void testErrorResult() {
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
    void testWithResponseTime() {
        long responseTime = 1500L;
        CrlResult original = CrlResult.good(TEST_CRL_URL, TEST_TIME, NEXT_UPDATE);
        CrlResult withTime = original.withResponseTime(responseTime);
        
        assertEquals(responseTime, withTime.getResponseTimeMs());
        assertEquals(original.getStatus(), withTime.getStatus());
        assertEquals(original.getMessage(), withTime.getMessage());
        assertEquals(original.getCrlUrl(), withTime.getCrlUrl());
    }
    
    @Test
    void testRevocationReasonDescriptions() {
        // Test various revocation reasons
        CrlResult result0 = CrlResult.revoked(TEST_CRL_URL, TEST_TIME, 0, TEST_TIME, NEXT_UPDATE);
        assertTrue(result0.getMessage().contains("Unspecified"));
        
        CrlResult result1 = CrlResult.revoked(TEST_CRL_URL, TEST_TIME, 1, TEST_TIME, NEXT_UPDATE);
        assertTrue(result1.getMessage().contains("Key Compromise"));
        
        CrlResult result2 = CrlResult.revoked(TEST_CRL_URL, TEST_TIME, 2, TEST_TIME, NEXT_UPDATE);
        assertTrue(result2.getMessage().contains("CA Compromise"));
        
        CrlResult result3 = CrlResult.revoked(TEST_CRL_URL, TEST_TIME, 3, TEST_TIME, NEXT_UPDATE);
        assertTrue(result3.getMessage().contains("Affiliation Changed"));
        
        CrlResult result4 = CrlResult.revoked(TEST_CRL_URL, TEST_TIME, 4, TEST_TIME, NEXT_UPDATE);
        assertTrue(result4.getMessage().contains("Superseded"));
        
        CrlResult result5 = CrlResult.revoked(TEST_CRL_URL, TEST_TIME, 5, TEST_TIME, NEXT_UPDATE);
        assertTrue(result5.getMessage().contains("Cessation of Operation"));
        
        CrlResult result6 = CrlResult.revoked(TEST_CRL_URL, TEST_TIME, 6, TEST_TIME, NEXT_UPDATE);
        assertTrue(result6.getMessage().contains("Certificate Hold"));
        
        CrlResult result8 = CrlResult.revoked(TEST_CRL_URL, TEST_TIME, 8, TEST_TIME, NEXT_UPDATE);
        assertTrue(result8.getMessage().contains("Remove from CRL"));
        
        CrlResult result9 = CrlResult.revoked(TEST_CRL_URL, TEST_TIME, 9, TEST_TIME, NEXT_UPDATE);
        assertTrue(result9.getMessage().contains("Privilege Withdrawn"));
        
        CrlResult result10 = CrlResult.revoked(TEST_CRL_URL, TEST_TIME, 10, TEST_TIME, NEXT_UPDATE);
        assertTrue(result10.getMessage().contains("AA Compromise"));
        
        CrlResult resultUnknown = CrlResult.revoked(TEST_CRL_URL, TEST_TIME, 99, TEST_TIME, NEXT_UPDATE);
        assertTrue(resultUnknown.getMessage().contains("Unknown Reason (99)"));
    }
    
    @Test
    void testEqualsAndHashCode() {
        CrlResult result1 = CrlResult.good(TEST_CRL_URL, TEST_TIME, NEXT_UPDATE);
        CrlResult result2 = CrlResult.good(TEST_CRL_URL, TEST_TIME, NEXT_UPDATE);
        CrlResult result3 = CrlResult.good("https://different.com/crl", TEST_TIME, NEXT_UPDATE);
        
        assertEquals(result1, result2);
        assertEquals(result1.hashCode(), result2.hashCode());
        assertNotEquals(result1, result3);
        assertNotEquals(result1.hashCode(), result3.hashCode());
        
        assertNotEquals(result1, null);
        assertNotEquals(result1, "not a CrlResult");
        assertEquals(result1, result1);
    }
    
    @Test
    void testToString() {
        CrlResult goodResult = CrlResult.good(TEST_CRL_URL, TEST_TIME, NEXT_UPDATE);
        String goodString = goodResult.toString();
        
        assertTrue(goodString.contains("CrlResult"));
        assertTrue(goodString.contains("GOOD"));
        assertTrue(goodString.contains(TEST_CRL_URL));
        assertTrue(goodString.contains(TEST_TIME.toString()));
        assertTrue(goodString.contains(NEXT_UPDATE.toString()));
        
        Instant revocationTime = Instant.parse("2023-12-01T10:00:00Z");
        CrlResult revokedResult = CrlResult.revoked(TEST_CRL_URL, revocationTime, 1, TEST_TIME, NEXT_UPDATE);
        String revokedString = revokedResult.toString();
        
        assertTrue(revokedString.contains("REVOKED"));
        assertTrue(revokedString.contains(revocationTime.toString()));
        assertTrue(revokedString.contains("revocationReason=1"));
        
        CrlResult resultWithTime = goodResult.withResponseTime(1500L);
        String stringWithTime = resultWithTime.toString();
        assertTrue(stringWithTime.contains("responseTimeMs=1500"));
    }
    
    @Test
    void testNullValues() {
        CrlResult result = CrlResult.good(null, null, null);
        
        assertNull(result.getCrlUrl());
        assertNull(result.getThisUpdate());
        assertNull(result.getNextUpdate());
        assertNotNull(result.getTimestamp());
        
        String resultString = result.toString();
        assertNotNull(resultString);
        assertTrue(resultString.contains("CrlResult"));
    }
}