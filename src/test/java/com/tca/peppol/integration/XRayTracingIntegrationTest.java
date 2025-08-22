package com.tca.peppol.integration;

import com.tca.peppol.config.XRayConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for X-Ray tracing functionality.
 * These tests verify X-Ray integration in a Lambda-like environment.
 */
class XRayTracingIntegrationTest {

    private XRayTracingService xrayTracingService;

    @BeforeEach
    void setUp() {
        xrayTracingService = new XRayTracingService();
    }

    @Test
    void testXRayConfiguration_Initialize() {
        // When
        assertThatCode(XRayConfiguration::initialize).doesNotThrowAnyException();
        
        // Then
        Map<String, Object> configInfo = XRayConfiguration.getConfigurationInfo();
        assertThat(configInfo).containsKeys("initialized", "available", "sampling_config", "lambda_trace_id");
        assertThat(configInfo.get("initialized")).isEqualTo(true);
    }

    @Test
    void testXRayTracingService_BasicFunctionality() {
        // Given
        String serviceName = "TEST_SERVICE";
        String operation = "TEST_OPERATION";

        // When & Then - Should not throw exceptions even without active X-Ray context
        assertThatCode(() -> {
            xrayTracingService.createExternalServiceSubsegment(serviceName, operation);
            xrayTracingService.createInternalOperationSubsegment(operation);
            xrayTracingService.addBusinessAnnotations("test-participant", "test-doc", "test-process", "test");
            xrayTracingService.addValidationResultsMetadata(Map.of("test", true));
            xrayTracingService.addResponseMetadata(true, 1000L, null, null);
            xrayTracingService.addPerformanceMetadata(Map.of("test_operation", 500L));
        }).doesNotThrowAnyException();
    }

    @Test
    void testXRayTracingService_TraceOperations() {
        // Given
        String expectedResult = "test-result";

        // When & Then - Should execute successfully even without active tracing
        assertThatCode(() -> {
            String result = xrayTracingService.traceExternalServiceCall("TEST", "OPERATION", 
                () -> expectedResult);
            assertThat(result).isEqualTo(expectedResult);
            
            String internalResult = xrayTracingService.traceInternalOperation("INTERNAL", 
                () -> expectedResult);
            assertThat(internalResult).isEqualTo(expectedResult);
        }).doesNotThrowAnyException();
    }

    @Test
    void testXRayTracingService_ErrorHandling() {
        // Given
        RuntimeException testException = new RuntimeException("Test error");

        // When & Then - Should propagate exceptions correctly
        assertThatThrownBy(() -> 
            xrayTracingService.traceExternalServiceCall("TEST", "OPERATION", () -> {
                throw testException;
            })
        ).isEqualTo(testException);

        assertThatThrownBy(() -> 
            xrayTracingService.traceInternalOperation("INTERNAL", () -> {
                throw testException;
            })
        ).isEqualTo(testException);
    }

    @Test
    void testXRayTracingService_TracingStatus() {
        // When
        boolean isActive = xrayTracingService.isTracingActive();
        String traceId = xrayTracingService.getCurrentTraceId();

        // Then - Should handle inactive tracing gracefully
        assertThat(isActive).isFalse(); // No active X-Ray context in test
        assertThat(traceId).isNull(); // No trace ID available
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "_X_AMZN_TRACE_ID", matches = ".*")
    void testXRayTracingService_WithActiveTracing() {
        // This test only runs when X-Ray tracing is actually enabled
        // (e.g., in a real Lambda environment)
        
        // When
        boolean isActive = xrayTracingService.isTracingActive();
        String traceId = xrayTracingService.getCurrentTraceId();

        // Then
        assertThat(isActive).isTrue();
        assertThat(traceId).isNotNull();
    }

    @Test
    void testXRayConfiguration_SamplingConfiguration() {
        // When
        String samplingConfig = XRayConfiguration.getSamplingConfiguration();

        // Then
        assertThat(samplingConfig).isNotNull();
        assertThat(samplingConfig).containsAnyOf("Production", "Test", "Default");
    }

    @Test
    void testXRayConfiguration_AvailabilityCheck() {
        // When
        boolean isAvailable = XRayConfiguration.isXRayAvailable();

        // Then - Should return false in test environment without X-Ray
        assertThat(isAvailable).isFalse();
    }
}