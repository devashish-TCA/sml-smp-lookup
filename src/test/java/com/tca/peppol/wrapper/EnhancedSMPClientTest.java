package com.tca.peppol.wrapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for EnhancedSMPClient wrapper
 */
@ExtendWith(MockitoExtension.class)
class EnhancedSMPClientTest {

    private EnhancedSMPClient enhancedSMPClient;

    @BeforeEach
    void setUp() {
        // Use a test SMP URL
        URI testSmpUrl = URI.create("https://test-smp.example.com");
        enhancedSMPClient = new EnhancedSMPClient(testSmpUrl);
    }

    @Test
    void testClientInitialization() {
        assertThat(enhancedSMPClient).isNotNull();
        assertThat(enhancedSMPClient.getUnderlyingClient()).isNotNull();
        assertThat(enhancedSMPClient.getCircuitBreakerMetrics()).isNotNull();
        assertThat(enhancedSMPClient.getRetryMetrics()).isNotNull();
        assertThat(enhancedSMPClient.getMetricsCollector()).isNotNull();
    }

    @Test
    void testMetricsCollectorInitialState() {
        EnhancedSMPClient.MetricsCollector metrics = enhancedSMPClient.getMetricsCollector();
        
        assertThat(metrics.getTotalRequests()).isEqualTo(0);
        assertThat(metrics.getSuccessfulRequests()).isEqualTo(0);
        assertThat(metrics.getFailedRequests()).isEqualTo(0);
        assertThat(metrics.getSuccessRate()).isEqualTo(0.0);
        assertThat(metrics.getAverageResponseTime()).isEqualTo(0.0);
    }

    @Test
    void testMetricsCollectorRecordSuccess() {
        EnhancedSMPClient.MetricsCollector metrics = enhancedSMPClient.getMetricsCollector();
        
        metrics.recordSuccess("test-operation", 100L);
        
        assertThat(metrics.getTotalRequests()).isEqualTo(1);
        assertThat(metrics.getSuccessfulRequests()).isEqualTo(1);
        assertThat(metrics.getFailedRequests()).isEqualTo(0);
        assertThat(metrics.getSuccessRate()).isEqualTo(1.0);
        assertThat(metrics.getAverageResponseTime()).isEqualTo(100.0);
    }

    @Test
    void testMetricsCollectorRecordFailure() {
        EnhancedSMPClient.MetricsCollector metrics = enhancedSMPClient.getMetricsCollector();
        
        metrics.recordFailure("test-operation", 200L, "Test error");
        
        assertThat(metrics.getTotalRequests()).isEqualTo(1);
        assertThat(metrics.getSuccessfulRequests()).isEqualTo(0);
        assertThat(metrics.getFailedRequests()).isEqualTo(1);
        assertThat(metrics.getSuccessRate()).isEqualTo(0.0);
        assertThat(metrics.getAverageResponseTime()).isEqualTo(200.0);
    }

    @Test
    void testMetricsCollectorMixedResults() {
        EnhancedSMPClient.MetricsCollector metrics = enhancedSMPClient.getMetricsCollector();
        
        metrics.recordSuccess("test-operation", 100L);
        metrics.recordFailure("test-operation", 200L, "Test error");
        metrics.recordSuccess("test-operation", 150L);
        
        assertThat(metrics.getTotalRequests()).isEqualTo(3);
        assertThat(metrics.getSuccessfulRequests()).isEqualTo(2);
        assertThat(metrics.getFailedRequests()).isEqualTo(1);
        assertThat(metrics.getSuccessRate()).isEqualTo(2.0/3.0);
        assertThat(metrics.getAverageResponseTime()).isEqualTo(150.0); // (100 + 200 + 150) / 3
    }

    @Test
    void testCircuitBreakerConfiguration() {
        // Verify circuit breaker is properly configured
        assertThat(enhancedSMPClient.getCircuitBreakerMetrics().getFailureRate()).isEqualTo(-1.0f); // No calls yet
        assertThat(enhancedSMPClient.getCircuitBreakerMetrics().getNumberOfBufferedCalls()).isEqualTo(0);
    }

    @Test
    void testRetryConfiguration() {
        // Verify retry is properly configured
        assertThat(enhancedSMPClient.getRetryMetrics().getNumberOfSuccessfulCallsWithRetryAttempt()).isEqualTo(0);
        assertThat(enhancedSMPClient.getRetryMetrics().getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(0);
    }
}