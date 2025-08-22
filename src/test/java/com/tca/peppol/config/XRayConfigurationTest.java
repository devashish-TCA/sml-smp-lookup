package com.tca.peppol.config;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for XRayConfiguration.
 * Tests X-Ray initialization, sampling configuration, and environment handling.
 */
@ExtendWith(MockitoExtension.class)
class XRayConfigurationTest {

    private MockedStatic<AWSXRay> mockedAWSXRay;
    private MockedStatic<AWSXRayRecorderBuilder> mockedBuilder;

    @BeforeEach
    void setUp() {
        mockedAWSXRay = mockStatic(AWSXRay.class);
        mockedBuilder = mockStatic(AWSXRayRecorderBuilder.class);
        
        // Reset initialization state for each test
        // Note: This is a limitation since the initialized field is private static
        // In a real scenario, you might want to make it package-private for testing
    }

    @AfterEach
    void tearDown() {
        if (mockedAWSXRay != null) {
            mockedAWSXRay.close();
        }
        if (mockedBuilder != null) {
            mockedBuilder.close();
        }
        
        // Clear environment variables
        clearEnvironmentVariable("_X_AMZN_TRACE_ID");
        clearEnvironmentVariable("XRAY_SAMPLING_RULES");
        clearEnvironmentVariable("ENVIRONMENT");
    }

    @Test
    void testInitialize_WithXRayEnabled() {
        // Given
        setEnvironmentVariable("_X_AMZN_TRACE_ID", "Root=1-5e1b4025-1234567890abcdef;Parent=53995c3f42cd8ad8;Sampled=1");
        
        AWSXRayRecorderBuilder mockBuilder = mock(AWSXRayRecorderBuilder.class);
        com.amazonaws.xray.AWSXRayRecorder mockRecorder = mock(com.amazonaws.xray.AWSXRayRecorder.class);
        
        mockedBuilder.when(AWSXRayRecorderBuilder::standard).thenReturn(mockBuilder);
        when(mockBuilder.withPlugin(any())).thenReturn(mockBuilder);
        when(mockBuilder.withSamplingStrategy(any())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockRecorder);

        // When
        XRayConfiguration.initialize();

        // Then
        mockedAWSXRay.verify(() -> AWSXRay.setGlobalRecorder(mockRecorder));
    }

    @Test
    void testInitialize_WithXRayDisabled() {
        // Given - No _X_AMZN_TRACE_ID environment variable

        // When
        XRayConfiguration.initialize();

        // Then
        mockedAWSXRay.verify(() -> AWSXRay.setGlobalRecorder(any()), never());
    }

    @Test
    void testInitialize_WithException() {
        // Given
        setEnvironmentVariable("_X_AMZN_TRACE_ID", "Root=1-5e1b4025-1234567890abcdef");
        
        mockedBuilder.when(AWSXRayRecorderBuilder::standard)
            .thenThrow(new RuntimeException("X-Ray initialization failed"));

        // When & Then - Should not throw exception
        assertThatCode(XRayConfiguration::initialize).doesNotThrowAnyException();
    }

    @Test
    void testInitialize_MultipleCallsIdempotent() {
        // Given
        setEnvironmentVariable("_X_AMZN_TRACE_ID", "Root=1-5e1b4025-1234567890abcdef");
        
        AWSXRayRecorderBuilder mockBuilder = mock(AWSXRayRecorderBuilder.class);
        com.amazonaws.xray.AWSXRayRecorder mockRecorder = mock(com.amazonaws.xray.AWSXRayRecorder.class);
        
        mockedBuilder.when(AWSXRayRecorderBuilder::standard).thenReturn(mockBuilder);
        when(mockBuilder.withPlugin(any())).thenReturn(mockBuilder);
        when(mockBuilder.withSamplingStrategy(any())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockRecorder);

        // When
        XRayConfiguration.initialize();
        XRayConfiguration.initialize(); // Second call should be no-op

        // Then - Should only be called once
        mockedAWSXRay.verify(() -> AWSXRay.setGlobalRecorder(mockRecorder), times(1));
    }

    @Test
    void testGetSamplingConfiguration_Production() {
        // Given
        setEnvironmentVariable("ENVIRONMENT", "production");

        // When
        String config = XRayConfiguration.getSamplingConfiguration();

        // Then
        assertThat(config).contains("Production sampling: 20%");
        assertThat(config).contains("2 requests/second");
    }

    @Test
    void testGetSamplingConfiguration_Test() {
        // Given
        setEnvironmentVariable("ENVIRONMENT", "test");

        // When
        String config = XRayConfiguration.getSamplingConfiguration();

        // Then
        assertThat(config).contains("Test sampling: 50%");
        assertThat(config).contains("1 request/second");
    }

    @Test
    void testGetSamplingConfiguration_Default() {
        // Given - No ENVIRONMENT variable set

        // When
        String config = XRayConfiguration.getSamplingConfiguration();

        // Then
        assertThat(config).contains("Default sampling: 10%");
        assertThat(config).contains("1 request/second");
    }

    @Test
    void testIsXRayAvailable_True() {
        // Given
        setEnvironmentVariable("_X_AMZN_TRACE_ID", "Root=1-5e1b4025-1234567890abcdef");
        
        // Initialize first
        XRayConfiguration.initialize();

        // When
        boolean available = XRayConfiguration.isXRayAvailable();

        // Then
        assertThat(available).isTrue();
    }

    @Test
    void testIsXRayAvailable_False() {
        // Given - No trace ID environment variable

        // When
        boolean available = XRayConfiguration.isXRayAvailable();

        // Then
        assertThat(available).isFalse();
    }

    @Test
    void testGetConfigurationInfo() {
        // Given
        setEnvironmentVariable("_X_AMZN_TRACE_ID", "Root=1-5e1b4025-1234567890abcdef");
        setEnvironmentVariable("ENVIRONMENT", "production");

        // When
        Map<String, Object> info = XRayConfiguration.getConfigurationInfo();

        // Then
        assertThat(info).containsKeys("initialized", "available", "sampling_config", "lambda_trace_id");
        assertThat(info.get("lambda_trace_id")).isEqualTo("present");
        assertThat(info.get("sampling_config")).asString().contains("Production sampling");
    }

    @Test
    void testGetConfigurationInfo_NoTraceId() {
        // Given - No trace ID

        // When
        Map<String, Object> info = XRayConfiguration.getConfigurationInfo();

        // Then
        assertThat(info).containsKeys("initialized", "available", "sampling_config", "lambda_trace_id");
        assertThat(info.get("lambda_trace_id")).isEqualTo("absent");
        assertThat(info.get("available")).isEqualTo(false);
    }

    @Test
    void testCustomSamplingRules() {
        // Given
        String customRules = """
            {
              "version": 2,
              "default": {
                "fixed_target": 2,
                "rate": 0.2
              },
              "rules": []
            }
            """;
        setEnvironmentVariable("_X_AMZN_TRACE_ID", "Root=1-5e1b4025-1234567890abcdef");
        setEnvironmentVariable("XRAY_SAMPLING_RULES", customRules);
        
        AWSXRayRecorderBuilder mockBuilder = mock(AWSXRayRecorderBuilder.class);
        com.amazonaws.xray.AWSXRayRecorder mockRecorder = mock(com.amazonaws.xray.AWSXRayRecorder.class);
        
        mockedBuilder.when(AWSXRayRecorderBuilder::standard).thenReturn(mockBuilder);
        when(mockBuilder.withPlugin(any())).thenReturn(mockBuilder);
        when(mockBuilder.withSamplingStrategy(any())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockRecorder);

        // When
        XRayConfiguration.initialize();

        // Then
        verify(mockBuilder).withSamplingStrategy(any());
    }

    @Test
    void testSamplingRulesCreation_InvalidRules() {
        // Given
        setEnvironmentVariable("_X_AMZN_TRACE_ID", "Root=1-5e1b4025-1234567890abcdef");
        setEnvironmentVariable("XRAY_SAMPLING_RULES", "invalid json");
        
        AWSXRayRecorderBuilder mockBuilder = mock(AWSXRayRecorderBuilder.class);
        com.amazonaws.xray.AWSXRayRecorder mockRecorder = mock(com.amazonaws.xray.AWSXRayRecorder.class);
        
        mockedBuilder.when(AWSXRayRecorderBuilder::standard).thenReturn(mockBuilder);
        when(mockBuilder.withPlugin(any())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockRecorder);

        // When & Then - Should not throw exception, should use default sampling
        assertThatCode(XRayConfiguration::initialize).doesNotThrowAnyException();
    }

    // Helper methods for environment variable manipulation
    private void setEnvironmentVariable(String key, String value) {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            java.lang.reflect.Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.put(key, value);
        } catch (Exception e) {
            // Fallback: Use system property
            System.setProperty(key, value);
        }
    }

    private void clearEnvironmentVariable(String key) {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            java.lang.reflect.Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.remove(key);
        } catch (Exception e) {
            // Fallback: Clear system property
            System.clearProperty(key);
        }
    }
}