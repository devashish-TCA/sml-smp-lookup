package com.tca.peppol.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MetricsCollectorFactoryTest {

    @Mock
    private CloudWatchClient mockCloudWatchClient;

    @AfterEach
    void cleanup() {
        System.clearProperty("METRICS_ENABLED");
    }

    @Test
    void shouldCreateNoOpCollectorWhenMetricsDisabled() {
        // Given
        System.setProperty("METRICS_ENABLED", "false");

        // When
        MetricsCollector collector = MetricsCollectorFactory.create("test");

        // Then
        assertThat(collector).isInstanceOf(NoOpMetricsCollector.class);
    }

    @Test
    void shouldCreateCloudWatchCollectorWhenMetricsEnabled() {
        // Given
        System.setProperty("METRICS_ENABLED", "true");

        // When
        MetricsCollector collector = MetricsCollectorFactory.create(mockCloudWatchClient, "test");

        // Then
        assertThat(collector).isInstanceOf(CloudWatchMetricsCollector.class);
    }

    @Test
    void shouldCreateNoOpCollectorExplicitly() {
        // When
        MetricsCollector collector = MetricsCollectorFactory.createNoOp();

        // Then
        assertThat(collector).isInstanceOf(NoOpMetricsCollector.class);
    }

    @Test
    void shouldDefaultToMetricsEnabledWhenPropertyNotSet() {
        // When - No METRICS_ENABLED property set, should default to enabled but fallback to NoOp due to no AWS credentials
        MetricsCollector collector = MetricsCollectorFactory.create("test");

        // Then - Should fallback to NoOp due to CloudWatch client creation failure
        assertThat(collector).isInstanceOf(NoOpMetricsCollector.class);
    }
}