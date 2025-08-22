package com.tca.peppol.client;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents the result of a CRL (Certificate Revocation List) revocation check.
 * 
 * This class encapsulates the various possible outcomes of a CRL query:
 * - GOOD: Certificate is not revoked according to CRL
 * - REVOKED: Certificate has been revoked
 * - UNAVAILABLE: CRL is unavailable or cannot be downloaded
 * - ERROR: An error occurred during the check
 */
public class CrlResult {
    
    /**
     * CRL check status enumeration.
     */
    public enum Status {
        GOOD,           // Certificate is valid and not revoked
        REVOKED,        // Certificate has been revoked
        UNAVAILABLE,    // CRL is unavailable or cannot be downloaded
        ERROR           // An error occurred during the check
    }
    
    private final Status status;
    private final String message;
    private final Instant revocationTime;
    private final Integer revocationReason;
    private final Instant thisUpdate;
    private final Instant nextUpdate;
    private final String crlUrl;
    private final long responseTimeMs;
    private final Instant timestamp;
    
    /**
     * Private constructor for creating CRL results.
     */
    private CrlResult(Status status, String message, Instant revocationTime, 
                     Integer revocationReason, Instant thisUpdate, Instant nextUpdate,
                     String crlUrl, long responseTimeMs) {
        this.status = status;
        this.message = message;
        this.revocationTime = revocationTime;
        this.revocationReason = revocationReason;
        this.thisUpdate = thisUpdate;
        this.nextUpdate = nextUpdate;
        this.crlUrl = crlUrl;
        this.responseTimeMs = responseTimeMs;
        this.timestamp = Instant.now();
    }
    
    /**
     * Create a GOOD status result (certificate is not revoked).
     */
    public static CrlResult good(String crlUrl, Instant thisUpdate, Instant nextUpdate) {
        return new CrlResult(Status.GOOD, "Certificate is not revoked according to CRL", 
                            null, null, thisUpdate, nextUpdate, crlUrl, 0);
    }
    
    /**
     * Create a REVOKED status result (certificate has been revoked).
     */
    public static CrlResult revoked(String crlUrl, Instant revocationTime, int revocationReason, 
                                   Instant thisUpdate, Instant nextUpdate) {
        String message = String.format("Certificate revoked at %s, reason: %s", 
                                      revocationTime, getRevocationReasonDescription(revocationReason));
        return new CrlResult(Status.REVOKED, message, revocationTime, revocationReason, 
                            thisUpdate, nextUpdate, crlUrl, 0);
    }
    
    /**
     * Create an UNAVAILABLE status result (CRL is unavailable).
     */
    public static CrlResult unavailable(String crlUrl, String reason) {
        return new CrlResult(Status.UNAVAILABLE, "CRL unavailable: " + reason, 
                            null, null, null, null, crlUrl, 0);
    }
    
    /**
     * Create an ERROR status result (an error occurred during the check).
     */
    public static CrlResult error(String crlUrl, String errorMessage) {
        return new CrlResult(Status.ERROR, "CRL check error: " + errorMessage, 
                            null, null, null, null, crlUrl, 0);
    }
    
    /**
     * Create a result with response time information.
     */
    public CrlResult withResponseTime(long responseTimeMs) {
        return new CrlResult(this.status, this.message, this.revocationTime, 
                            this.revocationReason, this.thisUpdate, this.nextUpdate, 
                            this.crlUrl, responseTimeMs);
    }
    
    /**
     * Get the CRL status.
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
     * Get the thisUpdate time from CRL.
     */
    public Instant getThisUpdate() {
        return thisUpdate;
    }
    
    /**
     * Get the nextUpdate time from CRL.
     */
    public Instant getNextUpdate() {
        return nextUpdate;
    }
    
    /**
     * Get the CRL URL that was checked.
     */
    public String getCrlUrl() {
        return crlUrl;
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
     * Check if the CRL check was successful (not ERROR or UNAVAILABLE).
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
     * Check if the CRL was unavailable.
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
        CrlResult that = (CrlResult) o;
        return responseTimeMs == that.responseTimeMs &&
               status == that.status &&
               Objects.equals(message, that.message) &&
               Objects.equals(revocationTime, that.revocationTime) &&
               Objects.equals(revocationReason, that.revocationReason) &&
               Objects.equals(thisUpdate, that.thisUpdate) &&
               Objects.equals(nextUpdate, that.nextUpdate) &&
               Objects.equals(crlUrl, that.crlUrl);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(status, message, revocationTime, revocationReason, 
                           thisUpdate, nextUpdate, crlUrl, responseTimeMs);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CrlResult{");
        sb.append("status=").append(status);
        sb.append(", message='").append(message).append('\'');
        
        if (crlUrl != null) {
            sb.append(", crlUrl='").append(crlUrl).append('\'');
        }
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