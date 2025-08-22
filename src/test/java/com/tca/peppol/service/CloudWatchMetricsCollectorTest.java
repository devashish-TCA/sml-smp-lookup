package com.tca.peppol.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CloudWatchMetricsCollectorTest {

    @Mock
    private CloudWatchMetricsService mockMetricsService;

    private CloudWatchMetricsCollector metricsCollector;

    @BeforeEach
    void setUp() {
        metricsCollector = new CloudWatchMetricsCollector(mockMetricsService);
    }

    @Test
    void shouldDelegateRecordSuccess() {
        // When
        metricsCollector.recordSuccess();

        // Then
        verify(mockMetricsService).recordSuccess();
    }

    @Test
    void shouldDelegateRecordFailure() {
        // Given
        String errorCategory = "SML";

        // When
        metricsCollector.recordFailure(errorCategory);

        // Then
        verify(mockMetricsService).recordFailure(errorCategory);
    }

    @Test
    void shouldDelegateRecordProcessingTime() {
        // Given
        long processingTime = 1500L;

        // When
        metricsCollector.recordProcessingTime(processingTime);

        // Then
        verify(mockMetricsService).recordProcessingTime(processingTime);
    }

    @Test
    void shouldDelegateRecordSmlLookupTime() {
        // Given
        long lookupTime = 500L;

        // When
        metricsCollector.recordSmlLookupTime(lookupTime);

        // Then
        verify(mockMetricsService).recordSmlLookupTime(lookupTime);
    }

    @Test
    void shouldDelegateRecordSmpQueryTime() {
        // Given
        long queryTime = 800L;

        // When
        metricsCollector.recordSmpQueryTime(queryTime);

        // Then
        verify(mockMetricsService).recordSmpQueryTime(queryTime);
    }

    @Test
    void shouldDelegateRecordValidationTime() {
        // Given
        long validationTime = 300L;

        // When
        metricsCollector.recordValidationTime(validationTime);

        // Then
        verify(mockMetricsService).recordValidationTime(validationTime);
    }

    @Test
    void shouldDelegateRecordExternalServiceHealth() {
        // Given
        String serviceName = "OCSP";
        boolean healthy = true;

        // When
        metricsCollector.recordExternalServiceHealth(serviceName, healthy);

        // Then
        verify(mockMetricsService).recordExternalServiceHealth(serviceName, healthy);
    }

    @Test
    void shouldDelegateRecordCertificateExpiry() {
        // Given
        long daysUntilExpiry = 15L;

        // When
        metricsCollector.recordCertificateExpiry(daysUntilExpiry);

        // Then
        verify(mockMetricsService).recordCertificateExpiry(daysUntilExpiry);
    }

    @Test
    void shouldDelegateRecordMemoryUsage() {
        // When
        metricsCollector.recordMemoryUsage();

        // Then
        verify(mockMetricsService).recordMemoryUsage();
    }

    @Test
    void shouldDelegateRecordCacheHit() {
        // Given
        String cacheType = "DNS";

        // When
        metricsCollector.recordCacheHit(cacheType);

        // Then
        verify(mockMetricsService).recordCacheHit(cacheType);
    }

    @Test
    void shouldDelegateRecordCacheMiss() {
        // Given
        String cacheType = "Certificate";

        // When
        metricsCollector.recordCacheMiss(cacheType);

        // Then
        verify(mockMetricsService).recordCacheMiss(cacheType);
    }

    @Test
    void shouldDelegateFlushMetrics() {
        // When
        metricsCollector.flushMetrics();

        // Then
        verify(mockMetricsService).flushMetrics();
    }

    @Test
    void shouldDelegateClose() {
        // When
        metricsCollector.close();

        // Then
        verify(mockMetricsService).close();
    }
}