package com.tca.peppol.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class NoOpMetricsCollectorTest {

    private NoOpMetricsCollector metricsCollector;

    @BeforeEach
    void setUp() {
        metricsCollector = new NoOpMetricsCollector();
    }

    @Test
    void shouldNotThrowExceptionOnRecordSuccess() {
        assertThatCode(() -> metricsCollector.recordSuccess())
                .doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrowExceptionOnRecordFailure() {
        assertThatCode(() -> metricsCollector.recordFailure("SML"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrowExceptionOnRecordProcessingTime() {
        assertThatCode(() -> metricsCollector.recordProcessingTime(1500L))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrowExceptionOnRecordSmlLookupTime() {
        assertThatCode(() -> metricsCollector.recordSmlLookupTime(500L))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrowExceptionOnRecordSmpQueryTime() {
        assertThatCode(() -> metricsCollector.recordSmpQueryTime(800L))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrowExceptionOnRecordValidationTime() {
        assertThatCode(() -> metricsCollector.recordValidationTime(300L))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrowExceptionOnRecordExternalServiceHealth() {
        assertThatCode(() -> metricsCollector.recordExternalServiceHealth("OCSP", true))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrowExceptionOnRecordCertificateExpiry() {
        assertThatCode(() -> metricsCollector.recordCertificateExpiry(15L))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrowExceptionOnRecordMemoryUsage() {
        assertThatCode(() -> metricsCollector.recordMemoryUsage())
                .doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrowExceptionOnRecordCacheHit() {
        assertThatCode(() -> metricsCollector.recordCacheHit("DNS"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrowExceptionOnRecordCacheMiss() {
        assertThatCode(() -> metricsCollector.recordCacheMiss("Certificate"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrowExceptionOnFlushMetrics() {
        assertThatCode(() -> metricsCollector.flushMetrics())
                .doesNotThrowAnyException();
    }

    @Test
    void shouldNotThrowExceptionOnClose() {
        assertThatCode(() -> metricsCollector.close())
                .doesNotThrowAnyException();
    }
}