package com.tca.peppol.client;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for OcspResult.
 * 
 * Tests cover:
 * - Factory methods for different result types
 * - Status checking methods
 * - Equality and toString implementations
 * - Response time handling
 * - Revocation reason descriptions
 */
class OcspResultTest {
    
    @Test
    void testGoodResult_CreatesCorrectStatus() {
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
    void testRevokedResult_CreatesCorrectStatus() {
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
    void testUnknownResult_CreatesCorrectStatus() {
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
    void testUnavailableResult_CreatesCorrectStatus() {
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
    void testErrorResult_CreatesCorrectStatus() {
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
    void testWithResponseTime_AddsResponseTime() {
        OcspResult original = OcspResult.good(Instant.now(), null);
        long responseTime = 1500L;
        
        OcspResult withTime = original.withResponseTime(responseTime);
        
        assertThat(withTime.getResponseTimeMs()).isEqualTo(responseTime);
        assertThat(withTime.getStatus()).isEqualTo(original.getStatus());
        assertThat(withTime.getMessage()).isEqualTo(original.getMessage());
    }
    
    @Test
    void testRevocationReasonDescriptions() {
        // Test various revocation reason codes
        assertRevocationReason(0, "Unspecified");
        assertRevocationReason(1, "Key Compromise");
        assertRevocationReason(2, "CA Compromise");
        assertRevocationReason(3, "Affiliation Changed");
        assertRevocationReason(4, "Superseded");
        assertRevocationReason(5, "Cessation of Operation");
        assertRevocationReason(6, "Certificate Hold");
        assertRevocationReason(8, "Remove from CRL");
        assertRevocationReason(9, "Privilege Withdrawn");
        assertRevocationReason(10, "AA Compromise");
        assertRevocationReason(99, "Unknown Reason (99)");
    }
    
    private void assertRevocationReason(int reasonCode, String expectedDescription) {
        OcspResult result = OcspResult.revoked(Instant.now(), reasonCode, Instant.now(), null);
        assertThat(result.getMessage()).contains(expectedDescription);
    }
    
    @Test
    void testEquality() {
        Instant now = Instant.now();
        Instant later = now.plus(1, ChronoUnit.HOURS);
        
        OcspResult result1 = OcspResult.good(now, later);
        OcspResult result2 = OcspResult.good(now, later);
        OcspResult result3 = OcspResult.good(now, null);
        
        assertThat(result1).isEqualTo(result2);
        assertThat(result1).isNotEqualTo(result3);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }
    
    @Test
    void testToString() {
        Instant now = Instant.now();
        OcspResult result = OcspResult.good(now, null).withResponseTime(1000L);
        
        String toString = result.toString();
        
        assertThat(toString).contains("OcspResult{");
        assertThat(toString).contains("status=GOOD");
        assertThat(toString).contains("message='Certificate is not revoked'");
        assertThat(toString).contains("thisUpdate=" + now);
        assertThat(toString).contains("responseTimeMs=1000");
        assertThat(toString).contains("timestamp=");
    }
    
    @Test
    void testToString_RevokedResult() {
        Instant revocationTime = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant thisUpdate = Instant.now();
        
        OcspResult result = OcspResult.revoked(revocationTime, 1, thisUpdate, null);
        
        String toString = result.toString();
        
        assertThat(toString).contains("status=REVOKED");
        assertThat(toString).contains("revocationTime=" + revocationTime);
        assertThat(toString).contains("revocationReason=1");
    }
    
    @Test
    void testTimestamp_IsSetOnCreation() {
        Instant before = Instant.now();
        OcspResult result = OcspResult.good(Instant.now(), null);
        Instant after = Instant.now();
        
        assertThat(result.getTimestamp()).isBetween(before, after);
    }
}