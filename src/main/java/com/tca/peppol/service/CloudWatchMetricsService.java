package com.tca.peppol.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for collecting and emitting CloudWatch metrics for Peppol lookup operations.
 * Provides comprehensive monitoring including business metrics, performance metrics,
 * error metrics, and resource utilization metrics.
 */
public class CloudWatchMetricsService {
    
    private static final Logger logger = LoggerFactory.getLogger(CloudWatchMetricsService.class);
    
    private static final String NAMESPACE = "Peppol/Lookup";
    private static final String ENVIRONMENT_DIMENSION = "Environment";
    private static final String ERROR_CATEGORY_DIMENSION = "ErrorCategory";
    private static final String SERVICE_DIMENSION = "Service";
    
    private final CloudWatchClient cloudWatchClient;
    private final String environment;
    private final boolean metricsEnabled;
    
    // In-memory counters for aggregation
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private final Map<String, AtomicLong> errorCategoryCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> serviceHealthCounters = new ConcurrentHashMap<>();
    
    public CloudWatchMetricsService(CloudWatchClient cloudWatchClient, String environment) {
        this.cloudWatchClient = cloudWatchClient;
        this.environment = environment != null ? environment : "unknown";
        this.metricsEnabled = Boolean.parseBoolean(System.getenv().getOrDefault("METRICS_ENABLED", "true"));
        
        logger.info("CloudWatch metrics service initialized - environment: {}, metricsEnabled: {}", 
                   this.environment, metricsEnabled);
    }
    
    /**
     * Records a successful lookup operation
     */
    public void recordSuccess() {
        if (!metricsEnabled) return;
        
        successCount.incrementAndGet();
        logger.debug("Recorded success metric");
    }
    
    /**
     * Records a failed lookup operation with error category
     */
    public void recordFailure(String errorCategory) {
        if (!metricsEnabled) return;
        
        failureCount.incrementAndGet();
        errorCategoryCounters.computeIfAbsent(errorCategory, k -> new AtomicLong(0)).incrementAndGet();
        
        logger.debug("Recorded failure metric - errorCategory: {}", errorCategory);
    }
    
    /**
     * Records processing time for the overall lookup operation
     */
    public void recordProcessingTime(long processingTimeMs) {
        if (!metricsEnabled) return;
        
        try {
            putMetric("ProcessingTime", processingTimeMs, StandardUnit.MILLISECONDS, 
                     createEnvironmentDimension());
            
            logger.debug("Recorded processing time metric - processingTimeMs: {}", processingTimeMs);
        } catch (Exception e) {
            logger.error("Failed to record processing time metric - processingTimeMs: {}", processingTimeMs, e);
        }
    }
    
    /**
     * Records SML lookup time
     */
    public void recordSmlLookupTime(long lookupTimeMs) {
        if (!metricsEnabled) return;
        
        try {
            putMetric("SmlLookupTime", lookupTimeMs, StandardUnit.MILLISECONDS, 
                     createServiceDimension("SML"));
            
            logger.debug("Recorded SML lookup time metric - lookupTimeMs: {}", lookupTimeMs);
        } catch (Exception e) {
            logger.error("Failed to record SML lookup time metric - lookupTimeMs: {}", lookupTimeMs, e);
        }
    }
    
    /**
     * Records SMP query time
     */
    public void recordSmpQueryTime(long queryTimeMs) {
        if (!metricsEnabled) return;
        
        try {
            putMetric("SmpQueryTime", queryTimeMs, StandardUnit.MILLISECONDS, 
                     createServiceDimension("SMP"));
            
            logger.debug("Recorded SMP query time metric - queryTimeMs: {}", queryTimeMs);
        } catch (Exception e) {
            logger.error("Failed to record SMP query time metric - queryTimeMs: {}", queryTimeMs, e);
        }
    }
    
    /**
     * Records validation time
     */
    public void recordValidationTime(long validationTimeMs) {
        if (!metricsEnabled) return;
        
        try {
            putMetric("ValidationTime", validationTimeMs, StandardUnit.MILLISECONDS, 
                     createServiceDimension("Validation"));
            
            logger.debug("Recorded validation time metric - validationTimeMs: {}", validationTimeMs);
        } catch (Exception e) {
            logger.error("Failed to record validation time metric - validationTimeMs: {}", validationTimeMs, e);
        }
    }
    
    /**
     * Records external service health status
     */
    public void recordExternalServiceHealth(String serviceName, boolean healthy) {
        if (!metricsEnabled) return;
        
        String key = serviceName + (healthy ? "_healthy" : "_unhealthy");
        serviceHealthCounters.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        
        try {
            putMetric("ExternalServiceHealth", healthy ? 1.0 : 0.0, StandardUnit.COUNT, 
                     createServiceDimension(serviceName));
            
            logger.debug("Recorded external service health metric - serviceName: {}, healthy: {}", 
                        serviceName, healthy);
        } catch (Exception e) {
            logger.error("Failed to record external service health metric - serviceName: {}, healthy: {}", 
                        serviceName, healthy, e);
        }
    }
    
    /**
     * Records certificate expiry information
     */
    public void recordCertificateExpiry(long daysUntilExpiry) {
        if (!metricsEnabled) return;
        
        try {
            putMetric("CertificateExpiryDays", daysUntilExpiry, StandardUnit.COUNT, 
                     createEnvironmentDimension());
            
            // Also record if certificate is expiring soon (within 30 days)
            if (daysUntilExpiry <= 30) {
                putMetric("CertificateExpiringSoon", 1.0, StandardUnit.COUNT, 
                         createEnvironmentDimension());
            }
            
            logger.debug("Recorded certificate expiry metric - daysUntilExpiry: {}", daysUntilExpiry);
        } catch (Exception e) {
            logger.error("Failed to record certificate expiry metric - daysUntilExpiry: {}", daysUntilExpiry, e);
        }
    }
    
    /**
     * Records memory usage
     */
    public void recordMemoryUsage() {
        if (!metricsEnabled) return;
        
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            double memoryUtilization = (double) usedMemory / maxMemory * 100.0;
            
            putMetric("MemoryUsed", usedMemory / (1024.0 * 1024.0), StandardUnit.MEGABYTES, 
                     createEnvironmentDimension());
            putMetric("MemoryUtilization", memoryUtilization, StandardUnit.PERCENT, 
                     createEnvironmentDimension());
            
            logger.debug("Recorded memory usage metrics - usedMemoryMB: {}, utilizationPercent: {}", 
                        usedMemory / (1024 * 1024), memoryUtilization);
        } catch (Exception e) {
            logger.error("Failed to record memory usage metrics", e);
        }
    }
    
    /**
     * Records cache hit/miss statistics
     */
    public void recordCacheHit(String cacheType) {
        if (!metricsEnabled) return;
        
        try {
            putMetric("CacheHit", 1.0, StandardUnit.COUNT, 
                     createCacheDimension(cacheType));
            
            logger.debug("Recorded cache hit metric - cacheType: {}", cacheType);
        } catch (Exception e) {
            logger.error("Failed to record cache hit metric - cacheType: {}", cacheType, e);
        }
    }
    
    /**
     * Records cache miss
     */
    public void recordCacheMiss(String cacheType) {
        if (!metricsEnabled) return;
        
        try {
            putMetric("CacheMiss", 1.0, StandardUnit.COUNT, 
                     createCacheDimension(cacheType));
            
            logger.debug("Recorded cache miss metric - cacheType: {}", cacheType);
        } catch (Exception e) {
            logger.error("Failed to record cache miss metric - cacheType: {}", cacheType, e);
        }
    }
    
    /**
     * Flushes all accumulated metrics to CloudWatch
     */
    public void flushMetrics() {
        if (!metricsEnabled) return;
        
        try {
            List<MetricDatum> metrics = new ArrayList<>();
            
            // Add success/failure rates
            long totalSuccess = successCount.getAndSet(0);
            long totalFailure = failureCount.getAndSet(0);
            
            if (totalSuccess > 0) {
                metrics.add(createMetricDatum("SuccessCount", totalSuccess, StandardUnit.COUNT, 
                                            createEnvironmentDimension()));
            }
            
            if (totalFailure > 0) {
                metrics.add(createMetricDatum("FailureCount", totalFailure, StandardUnit.COUNT, 
                                            createEnvironmentDimension()));
            }
            
            // Add success rate percentage
            long total = totalSuccess + totalFailure;
            if (total > 0) {
                double successRate = (double) totalSuccess / total * 100.0;
                metrics.add(createMetricDatum("SuccessRate", successRate, StandardUnit.PERCENT, 
                                            createEnvironmentDimension()));
            }
            
            // Add error category metrics
            errorCategoryCounters.forEach((category, counter) -> {
                long count = counter.getAndSet(0);
                if (count > 0) {
                    metrics.add(createMetricDatum("ErrorsByCategory", count, StandardUnit.COUNT, 
                                                createErrorCategoryDimension(category)));
                }
            });
            
            // Add service health metrics
            serviceHealthCounters.forEach((key, counter) -> {
                long count = counter.getAndSet(0);
                if (count > 0) {
                    String[] parts = key.split("_");
                    String serviceName = parts[0];
                    boolean healthy = "healthy".equals(parts[1]);
                    
                    metrics.add(createMetricDatum("ServiceHealthCount", count, StandardUnit.COUNT, 
                                                createServiceHealthDimension(serviceName, healthy)));
                }
            });
            
            // Send metrics in batches (CloudWatch limit is 20 metrics per request)
            if (!metrics.isEmpty()) {
                sendMetricsBatch(metrics);
                logger.info("Flushed metrics to CloudWatch - metricCount: {}", metrics.size());
            }
            
        } catch (Exception e) {
            logger.error("Failed to flush metrics to CloudWatch", e);
        }
    }
    
    private void sendMetricsBatch(List<MetricDatum> metrics) {
        int batchSize = 20;
        for (int i = 0; i < metrics.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, metrics.size());
            List<MetricDatum> batch = metrics.subList(i, endIndex);
            
            PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace(NAMESPACE)
                    .metricData(batch)
                    .build();
            
            cloudWatchClient.putMetricData(request);
        }
    }
    
    private void putMetric(String metricName, double value, StandardUnit unit, List<Dimension> dimensions) {
        MetricDatum metric = createMetricDatum(metricName, value, unit, dimensions);
        
        PutMetricDataRequest request = PutMetricDataRequest.builder()
                .namespace(NAMESPACE)
                .metricData(metric)
                .build();
        
        cloudWatchClient.putMetricData(request);
    }
    
    private MetricDatum createMetricDatum(String metricName, double value, StandardUnit unit, 
                                         List<Dimension> dimensions) {
        return MetricDatum.builder()
                .metricName(metricName)
                .value(value)
                .unit(unit)
                .dimensions(dimensions)
                .timestamp(Instant.now())
                .build();
    }
    
    private List<Dimension> createEnvironmentDimension() {
        return List.of(Dimension.builder()
                .name(ENVIRONMENT_DIMENSION)
                .value(environment)
                .build());
    }
    
    private List<Dimension> createServiceDimension(String serviceName) {
        return List.of(
                Dimension.builder().name(ENVIRONMENT_DIMENSION).value(environment).build(),
                Dimension.builder().name(SERVICE_DIMENSION).value(serviceName).build()
        );
    }
    
    private List<Dimension> createErrorCategoryDimension(String errorCategory) {
        return List.of(
                Dimension.builder().name(ENVIRONMENT_DIMENSION).value(environment).build(),
                Dimension.builder().name(ERROR_CATEGORY_DIMENSION).value(errorCategory).build()
        );
    }
    
    private List<Dimension> createServiceHealthDimension(String serviceName, boolean healthy) {
        return List.of(
                Dimension.builder().name(ENVIRONMENT_DIMENSION).value(environment).build(),
                Dimension.builder().name(SERVICE_DIMENSION).value(serviceName).build(),
                Dimension.builder().name("HealthStatus").value(healthy ? "Healthy" : "Unhealthy").build()
        );
    }
    
    private List<Dimension> createCacheDimension(String cacheType) {
        return List.of(
                Dimension.builder().name(ENVIRONMENT_DIMENSION).value(environment).build(),
                Dimension.builder().name("CacheType").value(cacheType).build()
        );
    }
    
    /**
     * Closes the CloudWatch client and flushes any remaining metrics
     */
    public void close() {
        try {
            flushMetrics();
            if (cloudWatchClient != null) {
                cloudWatchClient.close();
            }
            logger.info("CloudWatch metrics service closed");
        } catch (Exception e) {
            logger.error("Error closing CloudWatch metrics service", e);
        }
    }
}