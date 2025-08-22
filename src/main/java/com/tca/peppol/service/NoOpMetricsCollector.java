package com.tca.peppol.service;

/**
 * No-operation implementation of MetricsCollector for testing and when metrics are disabled.
 * All methods are empty implementations that do nothing.
 */
public class NoOpMetricsCollector implements MetricsCollector {
    
    @Override
    public void recordSuccess() {
        // No-op
    }
    
    @Override
    public void recordFailure(String errorCategory) {
        // No-op
    }
    
    @Override
    public void recordProcessingTime(long processingTimeMs) {
        // No-op
    }
    
    @Override
    public void recordSmlLookupTime(long lookupTimeMs) {
        // No-op
    }
    
    @Override
    public void recordSmpQueryTime(long queryTimeMs) {
        // No-op
    }
    
    @Override
    public void recordValidationTime(long validationTimeMs) {
        // No-op
    }
    
    @Override
    public void recordExternalServiceHealth(String serviceName, boolean healthy) {
        // No-op
    }
    
    @Override
    public void recordCertificateExpiry(long daysUntilExpiry) {
        // No-op
    }
    
    @Override
    public void recordMemoryUsage() {
        // No-op
    }
    
    @Override
    public void recordCacheHit(String cacheType) {
        // No-op
    }
    
    @Override
    public void recordCacheMiss(String cacheType) {
        // No-op
    }
    
    @Override
    public void flushMetrics() {
        // No-op
    }
    
    @Override
    public void close() {
        // No-op
    }
}