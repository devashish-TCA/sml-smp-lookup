package com.tca.peppol.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudWatchMetricsServiceTest {

    @Mock
    private CloudWatchClient mockCloudWatchClient;

    private CloudWatchMetricsService metricsService;
    private static final String TEST_ENVIRONMENT = "test";

    @BeforeEach
    void setUp() {
        metricsService = new CloudWatchMetricsService(mockCloudWatchClient, TEST_ENVIRONMENT);
    }

    @Test
    void shouldRecordSuccessMetric() {
        // When
        metricsService.recordSuccess();
        metricsService.flushMetrics();

        // Then
        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(mockCloudWatchClient).putMetricData(captor.capture());

        PutMetricDataRequest request = captor.getValue();
        assertThat(request.namespace()).isEqualTo("Peppol/Lookup");
        
        List<MetricDatum> metrics = request.metricData();
        assertThat(metrics).hasSize(2); // SuccessCount and SuccessRate
        
        MetricDatum successCount = metrics.stream()
                .filter(m -> "SuccessCount".equals(m.metricName()))
                .findFirst()
                .orElseThrow();
        
        assertThat(successCount.value()).isEqualTo(1.0);
        assertThat(successCount.unit()).isEqualTo(StandardUnit.COUNT);
        assertThat(successCount.dimensions()).hasSize(1);
        assertThat(successCount.dimensions().get(0).name()).isEqualTo("Environment");
        assertThat(successCount.dimensions().get(0).value()).isEqualTo(TEST_ENVIRONMENT);
    }

    @Test
    void shouldRecordFailureMetricWithCategory() {
        // Given
        String errorCategory = "SML";

        // When
        metricsService.recordFailure(errorCategory);
        metricsService.flushMetrics();

        // Then
        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(mockCloudWatchClient, atLeastOnce()).putMetricData(captor.capture());

        List<PutMetricDataRequest> requests = captor.getAllValues();
        
        // Find the error category metric
        boolean foundErrorMetric = requests.stream()
                .flatMap(req -> req.metricData().stream())
                .anyMatch(metric -> "ErrorsByCategory".equals(metric.metricName()) &&
                        metric.dimensions().stream().anyMatch(dim -> 
                                "ErrorCategory".equals(dim.name()) && errorCategory.equals(dim.value())));
        
        assertThat(foundErrorMetric).isTrue();
    }

    @Test
    void shouldRecordProcessingTime() {
        // Given
        long processingTime = 1500L;

        // When
        metricsService.recordProcessingTime(processingTime);

        // Then
        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(mockCloudWatchClient).putMetricData(captor.capture());

        PutMetricDataRequest request = captor.getValue();
        MetricDatum metric = request.metricData().get(0);
        
        assertThat(metric.metricName()).isEqualTo("ProcessingTime");
        assertThat(metric.value()).isEqualTo(processingTime);
        assertThat(metric.unit()).isEqualTo(StandardUnit.MILLISECONDS);
    }

    @Test
    void shouldRecordSmlLookupTime() {
        // Given
        long lookupTime = 500L;

        // When
        metricsService.recordSmlLookupTime(lookupTime);

        // Then
        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(mockCloudWatchClient).putMetricData(captor.capture());

        PutMetricDataRequest request = captor.getValue();
        MetricDatum metric = request.metricData().get(0);
        
        assertThat(metric.metricName()).isEqualTo("SmlLookupTime");
        assertThat(metric.value()).isEqualTo(lookupTime);
        assertThat(metric.unit()).isEqualTo(StandardUnit.MILLISECONDS);
        
        // Check service dimension
        assertThat(metric.dimensions()).hasSize(2);
        assertThat(metric.dimensions().stream().anyMatch(dim -> 
                "Service".equals(dim.name()) && "SML".equals(dim.value()))).isTrue();
    }

    @Test
    void shouldRecordSmpQueryTime() {
        // Given
        long queryTime = 800L;

        // When
        metricsService.recordSmpQueryTime(queryTime);

        // Then
        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(mockCloudWatchClient).putMetricData(captor.capture());

        PutMetricDataRequest request = captor.getValue();
        MetricDatum metric = request.metricData().get(0);
        
        assertThat(metric.metricName()).isEqualTo("SmpQueryTime");
        assertThat(metric.value()).isEqualTo(queryTime);
        assertThat(metric.unit()).isEqualTo(StandardUnit.MILLISECONDS);
        
        // Check service dimension
        assertThat(metric.dimensions().stream().anyMatch(dim -> 
                "Service".equals(dim.name()) && "SMP".equals(dim.value()))).isTrue();
    }

    @Test
    void shouldRecordValidationTime() {
        // Given
        long validationTime = 300L;

        // When
        metricsService.recordValidationTime(validationTime);

        // Then
        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(mockCloudWatchClient).putMetricData(captor.capture());

        PutMetricDataRequest request = captor.getValue();
        MetricDatum metric = request.metricData().get(0);
        
        assertThat(metric.metricName()).isEqualTo("ValidationTime");
        assertThat(metric.value()).isEqualTo(validationTime);
        assertThat(metric.unit()).isEqualTo(StandardUnit.MILLISECONDS);
        
        // Check service dimension
        assertThat(metric.dimensions().stream().anyMatch(dim -> 
                "Service".equals(dim.name()) && "Validation".equals(dim.value()))).isTrue();
    }

    @Test
    void shouldRecordExternalServiceHealth() {
        // Given
        String serviceName = "OCSP";
        boolean healthy = true;

        // When
        metricsService.recordExternalServiceHealth(serviceName, healthy);

        // Then
        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(mockCloudWatchClient).putMetricData(captor.capture());

        PutMetricDataRequest request = captor.getValue();
        MetricDatum metric = request.metricData().get(0);
        
        assertThat(metric.metricName()).isEqualTo("ExternalServiceHealth");
        assertThat(metric.value()).isEqualTo(1.0);
        assertThat(metric.unit()).isEqualTo(StandardUnit.COUNT);
        
        // Check dimensions
        assertThat(metric.dimensions().stream().anyMatch(dim -> 
                "Service".equals(dim.name()) && serviceName.equals(dim.value()))).isTrue();
        assertThat(metric.dimensions().stream().anyMatch(dim -> 
                "HealthStatus".equals(dim.name()) && "Healthy".equals(dim.value()))).isTrue();
    }

    @Test
    void shouldRecordCertificateExpiry() {
        // Given
        long daysUntilExpiry = 15L;

        // When
        metricsService.recordCertificateExpiry(daysUntilExpiry);

        // Then
        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(mockCloudWatchClient, times(2)).putMetricData(captor.capture());

        List<PutMetricDataRequest> requests = captor.getAllValues();
        
        // Should record both CertificateExpiryDays and CertificateExpiringSoon
        boolean foundExpiryDays = requests.stream()
                .flatMap(req -> req.metricData().stream())
                .anyMatch(metric -> "CertificateExpiryDays".equals(metric.metricName()) &&
                        metric.value() == daysUntilExpiry);
        
        boolean foundExpiringSoon = requests.stream()
                .flatMap(req -> req.metricData().stream())
                .anyMatch(metric -> "CertificateExpiringSoon".equals(metric.metricName()) &&
                        metric.value() == 1.0);
        
        assertThat(foundExpiryDays).isTrue();
        assertThat(foundExpiringSoon).isTrue();
    }

    @Test
    void shouldRecordMemoryUsage() {
        // When
        metricsService.recordMemoryUsage();

        // Then
        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(mockCloudWatchClient, times(2)).putMetricData(captor.capture());

        List<PutMetricDataRequest> requests = captor.getAllValues();
        
        // Should record both MemoryUsed and MemoryUtilization
        boolean foundMemoryUsed = requests.stream()
                .flatMap(req -> req.metricData().stream())
                .anyMatch(metric -> "MemoryUsed".equals(metric.metricName()) &&
                        metric.unit() == StandardUnit.MEGABYTES);
        
        boolean foundMemoryUtilization = requests.stream()
                .flatMap(req -> req.metricData().stream())
                .anyMatch(metric -> "MemoryUtilization".equals(metric.metricName()) &&
                        metric.unit() == StandardUnit.PERCENT);
        
        assertThat(foundMemoryUsed).isTrue();
        assertThat(foundMemoryUtilization).isTrue();
    }

    @Test
    void shouldRecordCacheHit() {
        // Given
        String cacheType = "DNS";

        // When
        metricsService.recordCacheHit(cacheType);

        // Then
        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(mockCloudWatchClient).putMetricData(captor.capture());

        PutMetricDataRequest request = captor.getValue();
        MetricDatum metric = request.metricData().get(0);
        
        assertThat(metric.metricName()).isEqualTo("CacheHit");
        assertThat(metric.value()).isEqualTo(1.0);
        assertThat(metric.unit()).isEqualTo(StandardUnit.COUNT);
        
        // Check cache type dimension
        assertThat(metric.dimensions().stream().anyMatch(dim -> 
                "CacheType".equals(dim.name()) && cacheType.equals(dim.value()))).isTrue();
    }

    @Test
    void shouldRecordCacheMiss() {
        // Given
        String cacheType = "Certificate";

        // When
        metricsService.recordCacheMiss(cacheType);

        // Then
        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(mockCloudWatchClient).putMetricData(captor.capture());

        PutMetricDataRequest request = captor.getValue();
        MetricDatum metric = request.metricData().get(0);
        
        assertThat(metric.metricName()).isEqualTo("CacheMiss");
        assertThat(metric.value()).isEqualTo(1.0);
        assertThat(metric.unit()).isEqualTo(StandardUnit.COUNT);
        
        // Check cache type dimension
        assertThat(metric.dimensions().stream().anyMatch(dim -> 
                "CacheType".equals(dim.name()) && cacheType.equals(dim.value()))).isTrue();
    }

    @Test
    void shouldCalculateSuccessRate() {
        // Given
        metricsService.recordSuccess();
        metricsService.recordSuccess();
        metricsService.recordFailure("SML");

        // When
        metricsService.flushMetrics();

        // Then
        ArgumentCaptor<PutMetricDataRequest> captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(mockCloudWatchClient, atLeastOnce()).putMetricData(captor.capture());

        List<PutMetricDataRequest> requests = captor.getAllValues();
        
        // Find success rate metric
        MetricDatum successRateMetric = requests.stream()
                .flatMap(req -> req.metricData().stream())
                .filter(metric -> "SuccessRate".equals(metric.metricName()))
                .findFirst()
                .orElseThrow();
        
        // Success rate should be 66.67% (2 successes out of 3 total)
        assertThat(successRateMetric.value()).isCloseTo(66.67, org.assertj.core.data.Offset.offset(0.01));
        assertThat(successRateMetric.unit()).isEqualTo(StandardUnit.PERCENT);
    }

    @Test
    void shouldHandleMetricsBatching() {
        // Given - Record many metrics to trigger batching
        for (int i = 0; i < 25; i++) {
            metricsService.recordSuccess();
        }

        // When
        metricsService.flushMetrics();

        // Then - Should make multiple calls due to 20-metric batch limit
        verify(mockCloudWatchClient, atLeast(2)).putMetricData(any(PutMetricDataRequest.class));
    }

    @Test
    void shouldHandleCloudWatchExceptions() {
        // Given
        when(mockCloudWatchClient.putMetricData(any(PutMetricDataRequest.class)))
                .thenThrow(new RuntimeException("CloudWatch error"));

        // When/Then - Should not throw exception
        metricsService.recordProcessingTime(1000L);
        metricsService.flushMetrics();
        
        // Verify the call was made despite the exception
        verify(mockCloudWatchClient, atLeastOnce()).putMetricData(any(PutMetricDataRequest.class));
    }

    @Test
    void shouldCloseCleanly() {
        // When
        metricsService.close();

        // Then
        verify(mockCloudWatchClient).close();
    }

    @Test
    void shouldRespectMetricsDisabledFlag() {
        // Given - Create service with metrics disabled
        System.setProperty("METRICS_ENABLED", "false");
        CloudWatchMetricsService disabledService = new CloudWatchMetricsService(mockCloudWatchClient, TEST_ENVIRONMENT);

        // When
        disabledService.recordSuccess();
        disabledService.recordProcessingTime(1000L);
        disabledService.flushMetrics();

        // Then - No CloudWatch calls should be made
        verifyNoInteractions(mockCloudWatchClient);
        
        // Cleanup
        System.clearProperty("METRICS_ENABLED");
    }
}