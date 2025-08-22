package com.tca.peppol.service;

/**
 * Interface for collecting various types of metrics throughout the Peppol lookup process.
 * This abstraction allows for different metric collection implementations and testing.
 */
public interface MetricsCollector {
    
    /**
     * Records a successful lookup operation
     */
    void recordSuccess();
    
    /**
     * Records a failed lookup operation with error category
     * @param errorCategory The category of error (SML, SMP, CERTIFICATE, etc.)
     */
    void recordFailure(String errorCategory);
    
    /**
     * Records the total processing time for a lookup operation
     * @param processingTimeMs Processing time in milliseconds
     */
    void recordProcessingTime(long processingTimeMs);
    
    /**
     * Records SML lookup time
     * @param lookupTimeMs Lookup time in milliseconds
     */
    void recordSmlLookupTime(long lookupTimeMs);
    
    /**
     * Records SMP query time
     * @param queryTimeMs Query time in milliseconds
     */
    void recordSmpQueryTime(long queryTimeMs);
    
    /**
     * Records validation time
     * @param validationTimeMs Validation time in milliseconds
     */
    void recordValidationTime(long validationTimeMs);
    
    /**
     * Records external service health status
     * @param serviceName Name of the external service (OCSP, CRL, DNS, etc.)
     * @param healthy Whether the service is healthy
     */
    void recordExternalServiceHealth(String serviceName, boolean healthy);
    
    /**
     * Records certificate expiry information
     * @param daysUntilExpiry Number of days until certificate expires
     */
    void recordCertificateExpiry(long daysUntilExpiry);
    
    /**
     * Records current memory usage
     */
    void recordMemoryUsage();
    
    /**
     * Records cache hit
     * @param cacheType Type of cache (DNS, Certificate, CRL, OCSP)
     */
    void recordCacheHit(String cacheType);
    
    /**
     * Records cache miss
     * @param cacheType Type of cache (DNS, Certificate, CRL, OCSP)
     */
    void recordCacheMiss(String cacheType);
    
    /**
     * Flushes any accumulated metrics
     */
    void flushMetrics();
    
    /**
     * Closes the metrics collector and releases resources
     */
    void close();
}