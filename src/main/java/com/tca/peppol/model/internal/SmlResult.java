package com.tca.peppol.model.internal;

import java.util.List;

/**
 * Internal model representing the result of an SML (Service Metadata Locator) DNS lookup.
 * Contains the resolved SMP URL, DNS query details, timing information,
 * and DNSSEC validation results.
 */
public class SmlResult {

    private String smpUrl;
    private String dnsQuery;
    private String md5Hash;
    private long resolutionTimeMs;
    private boolean dnssecValid;
    private List<String> intermediateResults;
    private boolean successful;
    private String errorMessage;
    private int retryAttempts;

    // Default constructor
    public SmlResult() {}

    // Constructor for successful result
    public SmlResult(String smpUrl, String dnsQuery, String md5Hash, long resolutionTimeMs) {
        this.smpUrl = smpUrl;
        this.dnsQuery = dnsQuery;
        this.md5Hash = md5Hash;
        this.resolutionTimeMs = resolutionTimeMs;
        this.successful = true;
    }

    // Constructor for failed result
    public SmlResult(String dnsQuery, String md5Hash, long resolutionTimeMs, String errorMessage) {
        this.dnsQuery = dnsQuery;
        this.md5Hash = md5Hash;
        this.resolutionTimeMs = resolutionTimeMs;
        this.errorMessage = errorMessage;
        this.successful = false;
    }

    // Getters and setters
    public String getSmpUrl() {
        return smpUrl;
    }

    public void setSmpUrl(String smpUrl) {
        this.smpUrl = smpUrl;
    }

    public String getDnsQuery() {
        return dnsQuery;
    }

    public void setDnsQuery(String dnsQuery) {
        this.dnsQuery = dnsQuery;
    }

    public String getMd5Hash() {
        return md5Hash;
    }

    public void setMd5Hash(String md5Hash) {
        this.md5Hash = md5Hash;
    }

    public long getResolutionTimeMs() {
        return resolutionTimeMs;
    }

    public void setResolutionTimeMs(long resolutionTimeMs) {
        this.resolutionTimeMs = resolutionTimeMs;
    }

    public boolean isDnssecValid() {
        return dnssecValid;
    }

    public void setDnssecValid(boolean dnssecValid) {
        this.dnssecValid = dnssecValid;
    }

    public List<String> getIntermediateResults() {
        return intermediateResults;
    }

    public void setIntermediateResults(List<String> intermediateResults) {
        this.intermediateResults = intermediateResults;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    /**
     * Check if the SML lookup was successful and returned a valid SMP URL
     */
    public boolean hasValidSmpUrl() {
        return successful && smpUrl != null && !smpUrl.trim().isEmpty();
    }

    /**
     * Get the base SMP URL without trailing slash
     */
    public String getBaseSmpUrl() {
        if (smpUrl == null) {
            return null;
        }
        return smpUrl.endsWith("/") ? smpUrl.substring(0, smpUrl.length() - 1) : smpUrl;
    }

    /**
     * Create a successful SML result
     */
    public static SmlResult success(String smpUrl, String dnsQuery, String md5Hash, long resolutionTimeMs) {
        return new SmlResult(smpUrl, dnsQuery, md5Hash, resolutionTimeMs);
    }

    /**
     * Create a failed SML result
     */
    public static SmlResult failure(String dnsQuery, String md5Hash, long resolutionTimeMs, String errorMessage) {
        return new SmlResult(dnsQuery, md5Hash, resolutionTimeMs, errorMessage);
    }

    /**
     * Create a builder for SmlResult
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for SmlResult
     */
    public static class Builder {
        private String smpUrl;
        private String dnsQuery;
        private String md5Hash;
        private long resolutionTimeMs;
        private boolean dnssecValid;
        private List<String> intermediateResults;
        private boolean successful;
        private String errorMessage;
        private int retryAttempts;

        public Builder smpUrl(String smpUrl) {
            this.smpUrl = smpUrl;
            return this;
        }

        public Builder dnsQuery(String dnsQuery) {
            this.dnsQuery = dnsQuery;
            return this;
        }

        public Builder md5Hash(String md5Hash) {
            this.md5Hash = md5Hash;
            return this;
        }

        public Builder resolutionTimeMs(long resolutionTimeMs) {
            this.resolutionTimeMs = resolutionTimeMs;
            return this;
        }

        public Builder dnssecValid(boolean dnssecValid) {
            this.dnssecValid = dnssecValid;
            return this;
        }

        public Builder intermediateResults(List<String> intermediateResults) {
            this.intermediateResults = intermediateResults;
            return this;
        }

        public Builder successful(boolean successful) {
            this.successful = successful;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder retryAttempts(int retryAttempts) {
            this.retryAttempts = retryAttempts;
            return this;
        }

        public SmlResult build() {
            SmlResult result = new SmlResult();
            result.smpUrl = this.smpUrl;
            result.dnsQuery = this.dnsQuery;
            result.md5Hash = this.md5Hash;
            result.resolutionTimeMs = this.resolutionTimeMs;
            result.dnssecValid = this.dnssecValid;
            result.intermediateResults = this.intermediateResults;
            result.successful = this.successful;
            result.errorMessage = this.errorMessage;
            result.retryAttempts = this.retryAttempts;
            return result;
        }
    }

    @Override
    public String toString() {
        return "SmlResult{" +
                "smpUrl='" + smpUrl + '\'' +
                ", dnsQuery='" + dnsQuery + '\'' +
                ", md5Hash='" + md5Hash + '\'' +
                ", resolutionTimeMs=" + resolutionTimeMs +
                ", dnssecValid=" + dnssecValid +
                ", successful=" + successful +
                ", errorMessage='" + errorMessage + '\'' +
                ", retryAttempts=" + retryAttempts +
                '}';
    }
}