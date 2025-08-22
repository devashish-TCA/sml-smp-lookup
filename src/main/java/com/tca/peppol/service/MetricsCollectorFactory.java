package com.tca.peppol.service;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;

/**
 * Factory for creating MetricsCollector instances based on configuration.
 */
public class MetricsCollectorFactory {
    
    /**
     * Creates a MetricsCollector based on environment configuration
     * @param environment The environment (production, test, etc.)
     * @return MetricsCollector instance
     */
    public static MetricsCollector create(String environment) {
        boolean metricsEnabled = Boolean.parseBoolean(
            System.getenv().getOrDefault("METRICS_ENABLED", "true"));
        
        if (!metricsEnabled) {
            return new NoOpMetricsCollector();
        }
        
        try {
            CloudWatchClient cloudWatchClient = CloudWatchClient.builder().build();
            CloudWatchMetricsService metricsService = new CloudWatchMetricsService(cloudWatchClient, environment);
            return new CloudWatchMetricsCollector(metricsService);
        } catch (Exception e) {
            // Fallback to no-op if CloudWatch client creation fails
            return new NoOpMetricsCollector();
        }
    }
    
    /**
     * Creates a MetricsCollector with a specific CloudWatch client
     * @param cloudWatchClient The CloudWatch client to use
     * @param environment The environment
     * @return MetricsCollector instance
     */
    public static MetricsCollector create(CloudWatchClient cloudWatchClient, String environment) {
        CloudWatchMetricsService metricsService = new CloudWatchMetricsService(cloudWatchClient, environment);
        return new CloudWatchMetricsCollector(metricsService);
    }
    
    /**
     * Creates a no-op MetricsCollector for testing
     * @return NoOpMetricsCollector instance
     */
    public static MetricsCollector createNoOp() {
        return new NoOpMetricsCollector();
    }
}