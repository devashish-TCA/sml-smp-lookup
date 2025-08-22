package com.tca.peppol.client;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents the result of an OCSP (Online Certificate Status Protocol) revocation check.
 * 
 * This class encapsulates the various possible outcomes of an OCSP query:
 * - GOOD: Certificate is not revoked
 * - REVOKED: Certificate has been revoked
 * - UNKNOWN: Certificate status is unknown
 * - UNAVAILABLE: OCSP service is unavailable
 * - ERROR: An error occurred during the check
 */
public class OcspResult {
    
    /**
     * OCSP response status enumeration.
     */
    public enum Status {
        GOOD,           // Certificate is valid and not revoked
        REVOKED,        // Certificate has been revoked
        UNKNOWN,        // Certificate status is unknown
        UNAVAILABLE,    // OCSP service is unavailable
        ERROR           // An error occurred during the check
    }
    
    private final Status status;
    private final String message;
    private final Instant revocationTime;
    private final Integer revocationReason;
    private final Instant thisUpdate;
    private final Instant nextUpdate;
    private final long responseTimeMs;
    private final Instant timestamp;
    
    /**
     * Private constructor for creating OCSP results.
     */
    private OcspResult(Status status, String message, Instant revocationTime, 
                      Integer revocationReason, Instant thisUpdate, Instant nextUpdate,
                      long responseTimeMs) {
        this.status = status;
        this.message = message;
        this.revocationTime = revocationTime;
        this.revocationReason = revocationReason;
        this.thisUpdate = thisUpdate;
        this.nextUpdate = nextUpdate;
        this.responseTimeMs = responseTimeMs;
        this.timestamp = Instant.now();
    }
    
    /**
     * Create a GOOD status result (certificate is not revoked).
     */
    public static OcspResult good(Instant thisUpdate, Instant nextUpdate) {
        return new OcspResult(Status.GOOD, "Certificate is not revoked", null, null, 
                             thisUpdate, nextUpdate, 0);
    }
    
    /**
     * Create a REVOKED status result (certificate has been revoked).
     */
    public static OcspResult revoked(Instant revocationTime, int revocationReason, 
                                   Instant thisUpdate, Instant nextUpdate) {
        String message = String.format("Certificate revoked at %s, reason: %s", 
                                      revocationTime, getRevocationReasonDescription(revocationReason));
        return new OcspResult(Status.REVOKED, message, revocationTime, revocationReason, 
                             thisUpdate, nextUpdate, 0);
    }
    
    /**
     * Create an UNKNOWN status result (certificate status is unknown).
     */
    public static OcspResult unknown(Instant thisUpdate, Instant nextUpdate) {
        return new OcspResult(Status.UNKNOWN, "Certificate status is unknown", null, null, 
                             thisUpdate, nextUpdate, 0);
    }
    
    /**
     * Create an UNAVAILABLE status result (OCSP service is unavailable).
     */
    public static OcspResult unavailable(String reason) {
        return new OcspResult(Status.UNAVAILABLE, "OCSP service unavailable: " + reason, 
                             null, null, null, null, 0);
    }
    
    /**
     * Create an ERROR status result (an error occurred during the check).
     */
    public static OcspResult error(String errorMessage) {
        return new OcspResult(Status.ERROR, "OCSP check error: " + errorMessage, 
                             null, null, null, null, 0);
    }
    
    /**
     * Create a result with response time information.
     */
    public OcspResult withResponseTime(long responseTimeMs) {
        return new OcspResult(this.status, this.message, this.revocationTime, 
                             this.revocationReason, this.thisUpdate, this.nextUpdate, 
                             responseTimeMs);
    }
    
    /**
     * Get the OCSP status.
     */
    public Status getStatus() {
        return status;
    }
    
    /**
     * Get the status message.
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Get the revocation time (only for REVOKED status).
     */
    public Instant getRevocationTime() {
        return revocationTime;
    }
    
    /**
     * Get the revocation reason code (only for REVOKED status).
     */
    public Integer getRevocationReason() {
        return revocationReason;
    }
    
    /**
     * Get the thisUpdate time from OCSP response.
     */
    public Instant getThisUpdate() {
        return thisUpdate;
    }
    
    /**
     * Get the nextUpdate time from OCSP response.
     */
    public Instant getNextUpdate() {
        return nextUpdate;
    }
    
    /**
     * Get the response time in milliseconds.
     */
    public long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    /**
     * Get the timestamp when this result was created.
     */
    public Instant getTimestamp() {
        return timestamp;
    }
    
    /**
     * Check if the OCSP check was successful (not ERROR or UNAVAILABLE).
     */
    public boolean isSuccessful() {
        return status != Status.ERROR && status != Status.UNAVAILABLE;
    }
    
    /**
     * Check if the certificate is considered valid (GOOD status).
     */
    public boolean isValid() {
        return status == Status.GOOD;
    }
    
    /**
     * Check if the certificate is revoked.
     */
    public boolean isRevoked() {
        return status == Status.REVOKED;
    }
    
    /**
     * Check if the certificate status is unknown.
     */
    public boolean isUnknown() {
        return status == Status.UNKNOWN;
    }
    
    /**
     * Check if the OCSP service was unavailable.
     */
    public boolean isUnavailable() {
        return status == Status.UNAVAILABLE;
    }
    
    /**
     * Check if an error occurred during the check.
     */
    public boolean isError() {
        return status == Status.ERROR;
    }
    
    /**
     * Get description for revocation reason code.
     */
    private static String getRevocationReasonDescription(int reason) {
        return switch (reason) {
            case 0 -> "Unspecified";
            case 1 -> "Key Compromise";
            case 2 -> "CA Compromise";
            case 3 -> "Affiliation Changed";
            case 4 -> "Superseded";
            case 5 -> "Cessation of Operation";
            case 6 -> "Certificate Hold";
            case 8 -> "Remove from CRL";
            case 9 -> "Privilege Withdrawn";
            case 10 -> "AA Compromise";
            default -> "Unknown Reason (" + reason + ")";
        };
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OcspResult that = (OcspResult) o;
        return responseTimeMs == that.responseTimeMs &&
               status == that.status &&
               Objects.equals(message, that.message) &&
               Objects.equals(revocationTime, that.revocationTime) &&
               Objects.equals(revocationReason, that.revocationReason) &&
               Objects.equals(thisUpdate, that.thisUpdate) &&
               Objects.equals(nextUpdate, that.nextUpdate);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(status, message, revocationTime, revocationReason, 
                           thisUpdate, nextUpdate, responseTimeMs);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OcspResult{");
        sb.append("status=").append(status);
        sb.append(", message='").append(message).append('\'');
        
        if (revocationTime != null) {
            sb.append(", revocationTime=").append(revocationTime);
        }
        if (revocationReason != null) {
            sb.append(", revocationReason=").append(revocationReason);
        }
        if (thisUpdate != null) {
            sb.append(", thisUpdate=").append(thisUpdate);
        }
        if (nextUpdate != null) {
            sb.append(", nextUpdate=").append(nextUpdate);
        }
        if (responseTimeMs > 0) {
            sb.append(", responseTimeMs=").append(responseTimeMs);
        }
        
        sb.append(", timestamp=").append(timestamp);
        sb.append('}');
        
        return sb.toString();
    }
}