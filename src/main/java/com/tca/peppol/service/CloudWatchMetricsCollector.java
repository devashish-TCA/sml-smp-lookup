package com.tca.peppol.service;

/**
 * CloudWatch implementation of the MetricsCollector interface.
 * Delegates all metric collection to the CloudWatchMetricsService.
 */
public class CloudWatchMetricsCollector implements MetricsCollector {
    
    private final CloudWatchMetricsService metricsService;
    
    public CloudWatchMetricsCollector(CloudWatchMetricsService metricsService) {
        this.metricsService = metricsService;
    }
    
    @Override
    public void recordSuccess() {
        metricsService.recordSuccess();
    }
    
    @Override
    public void recordFailure(String errorCategory) {
        metricsService.recordFailure(errorCategory);
    }
    
    @Override
    public void recordProcessingTime(long processingTimeMs) {
        metricsService.recordProcessingTime(processingTimeMs);
    }
    
    @Override
    public void recordSmlLookupTime(long lookupTimeMs) {
        metricsService.recordSmlLookupTime(lookupTimeMs);
    }
    
    @Override
    public void recordSmpQueryTime(long queryTimeMs) {
        metricsService.recordSmpQueryTime(queryTimeMs);
    }
    
    @Override
    public void recordValidationTime(long validationTimeMs) {
        metricsService.recordValidationTime(validationTimeMs);
    }
    
    @Override
    public void recordExternalServiceHealth(String serviceName, boolean healthy) {
        metricsService.recordExternalServiceHealth(serviceName, healthy);
    }
    
    @Override
    public void recordCertificateExpiry(long daysUntilExpiry) {
        metricsService.recordCertificateExpiry(daysUntilExpiry);
    }
    
    @Override
    public void recordMemoryUsage() {
        metricsService.recordMemoryUsage();
    }
    
    @Override
    public void recordCacheHit(String cacheType) {
        metricsService.recordCacheHit(cacheType);
    }
    
    @Override
    public void recordCacheMiss(String cacheType) {
        metricsService.recordCacheMiss(cacheType);
    }
    
    @Override
    public void flushMetrics() {
        metricsService.flushMetrics();
    }
    
    @Override
    public void close() {
        metricsService.close();
    }
}