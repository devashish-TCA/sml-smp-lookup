package com.tca.peppol.service;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import com.tca.peppol.util.CorrelationIdUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for XRayTracingService.
 * Tests X-Ray integration, tracing functionality, and error handling.
 */
@ExtendWith(MockitoExtension.class)
class XRayTracingServiceTest {

    @Mock
    private Segment mockSegment;
    
    @Mock
    private Subsegment mockSubsegment;
    
    private XRayTracingService xrayTracingService;
    private MockedStatic<AWSXRay> mockedAWSXRay;
    private MockedStatic<CorrelationIdUtils> mockedCorrelationIdUtils;

    @BeforeEach
    void setUp() {
        xrayTracingService = new XRayTracingService();
        mockedAWSXRay = mockStatic(AWSXRay.class);
        mockedCorrelationIdUtils = mockStatic(CorrelationIdUtils.class);
        
        // Setup default mocks
        mockedCorrelationIdUtils.when(CorrelationIdUtils::getCorrelationId)
            .thenReturn("test-correlation-id");
    }

    @AfterEach
    void tearDown() {
        if (mockedAWSXRay != null) {
            mockedAWSXRay.close();
        }
        if (mockedCorrelationIdUtils != null) {
            mockedCorrelationIdUtils.close();
        }
    }

    @Test
    void testCreateExternalServiceSubsegment_Success() {
        // Given
        String serviceName = "SML";
        String operation = "DNS_LOOKUP";
        mockedAWSXRay.when(() -> AWSXRay.beginSubsegment("sml_dns_lookup"))
            .thenReturn(mockSubsegment);

        // When
        Subsegment result = xrayTracingService.createExternalServiceSubsegment(serviceName, operation);

        // Then
        assertThat(result).isEqualTo(mockSubsegment);
        verify(mockSubsegment).putAnnotation("service_type", "external");
        verify(mockSubsegment).putAnnotation("service_name", serviceName);
        verify(mockSubsegment).putAnnotation("operation", operation);
        verify(mockSubsegment).putAnnotation("correlation_id", "test-correlation-id");
    }

    @Test
    void testCreateExternalServiceSubsegment_NullSubsegment() {
        // Given
        String serviceName = "SMP";
        String operation = "HTTP_GET";
        mockedAWSXRay.when(() -> AWSXRay.beginSubsegment("smp_http_get"))
            .thenReturn(null);

        // When
        Subsegment result = xrayTracingService.createExternalServiceSubsegment(serviceName, operation);

        // Then
        assertThat(result).isNull();
        // Should not throw exception when subsegment is null
    }

    @Test
    void testCreateInternalOperationSubsegment_Success() {
        // Given
        String operationName = "CERTIFICATE_VALIDATION";
        mockedAWSXRay.when(() -> AWSXRay.beginSubsegment("internal_certificate_validation"))
            .thenReturn(mockSubsegment);

        // When
        Subsegment result = xrayTracingService.createInternalOperationSubsegment(operationName);

        // Then
        assertThat(result).isEqualTo(mockSubsegment);
        verify(mockSubsegment).putAnnotation("service_type", "internal");
        verify(mockSubsegment).putAnnotation("operation", operationName);
        verify(mockSubsegment).putAnnotation("correlation_id", "test-correlation-id");
    }

    @Test
    void testTraceExternalServiceCall_Success() throws Exception {
        // Given
        String serviceName = "OCSP";
        String operation = "REVOCATION_CHECK";
        String expectedResult = "success";
        Callable<String> callable = () -> expectedResult;
        
        mockedAWSXRay.when(() -> AWSXRay.beginSubsegment("ocsp_revocation_check"))
            .thenReturn(mockSubsegment);

        // When
        String result = xrayTracingService.traceExternalServiceCall(serviceName, operation, callable);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        verify(mockSubsegment).putAnnotation("success", true);
        verify(mockSubsegment).putMetadata(eq("peppol_lookup"), eq("performance"), any(Map.class));
        mockedAWSXRay.verify(AWSXRay::endSubsegment);
    }

    @Test
    void testTraceExternalServiceCall_Exception() {
        // Given
        String serviceName = "CRL";
        String operation = "DOWNLOAD";
        RuntimeException expectedException = new RuntimeException("Test exception");
        Callable<String> callable = () -> { throw expectedException; };
        
        mockedAWSXRay.when(() -> AWSXRay.beginSubsegment("crl_download"))
            .thenReturn(mockSubsegment);

        // When & Then
        assertThatThrownBy(() -> xrayTracingService.traceExternalServiceCall(serviceName, operation, callable))
            .isEqualTo(expectedException);
        
        verify(mockSubsegment).putAnnotation("success", false);
        verify(mockSubsegment).addException(expectedException);
        verify(mockSubsegment).putMetadata(eq("peppol_lookup"), eq("error_details"), any(Map.class));
        mockedAWSXRay.verify(AWSXRay::endSubsegment);
    }

    @Test
    void testTraceInternalOperation_Success() {
        // Given
        String operationName = "XML_PARSING";
        String expectedResult = "parsed";
        Supplier<String> supplier = () -> expectedResult;
        
        mockedAWSXRay.when(() -> AWSXRay.beginSubsegment("internal_xml_parsing"))
            .thenReturn(mockSubsegment);

        // When
        String result = xrayTracingService.traceInternalOperation(operationName, supplier);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        verify(mockSubsegment).putAnnotation("success", true);
        verify(mockSubsegment).putMetadata(eq("peppol_lookup"), eq("performance"), any(Map.class));
        mockedAWSXRay.verify(AWSXRay::endSubsegment);
    }

    @Test
    void testTraceInternalOperation_Exception() {
        // Given
        String operationName = "VALIDATION";
        RuntimeException expectedException = new RuntimeException("Validation failed");
        Supplier<String> supplier = () -> { throw expectedException; };
        
        mockedAWSXRay.when(() -> AWSXRay.beginSubsegment("internal_validation"))
            .thenReturn(mockSubsegment);

        // When & Then
        assertThatThrownBy(() -> xrayTracingService.traceInternalOperation(operationName, supplier))
            .isEqualTo(expectedException);
        
        verify(mockSubsegment).putAnnotation("success", false);
        verify(mockSubsegment).addException(expectedException);
        mockedAWSXRay.verify(AWSXRay::endSubsegment);
    }

    @Test
    void testAddBusinessAnnotations_Success() {
        // Given
        String participantId = "9915:test";
        String documentType = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice";
        String processId = "urn:fdc:peppol.eu:2017:poacc:billing:01:1.0";
        String environment = "production";
        
        mockedAWSXRay.when(AWSXRay::getCurrentSegment).thenReturn(mockSegment);

        // When
        xrayTracingService.addBusinessAnnotations(participantId, documentType, processId, environment);

        // Then
        verify(mockSegment).putAnnotation(eq("participant_id"), anyString()); // Hashed value
        verify(mockSegment).putAnnotation("document_type", documentType);
        verify(mockSegment).putAnnotation("process_id", processId);
        verify(mockSegment).putAnnotation("environment", environment);
        verify(mockSegment).putAnnotation("correlation_id", "test-correlation-id");
        verify(mockSegment).putMetadata(eq("peppol_lookup"), eq("request_details"), any(Map.class));
    }

    @Test
    void testAddBusinessAnnotations_NullSegment() {
        // Given
        mockedAWSXRay.when(AWSXRay::getCurrentSegment).thenReturn(null);

        // When & Then - Should not throw exception
        assertThatCode(() -> xrayTracingService.addBusinessAnnotations(
            "9915:test", "invoice", "billing", "test"))
            .doesNotThrowAnyException();
    }

    @Test
    void testAddValidationResultsMetadata_Success() {
        // Given
        Map<String, Object> validationResults = Map.of(
            "certificateValid", true,
            "peppolCompliant", false,
            "smpSignatureValid", true
        );
        
        mockedAWSXRay.when(AWSXRay::getCurrentSegment).thenReturn(mockSegment);

        // When
        xrayTracingService.addValidationResultsMetadata(validationResults);

        // Then
        verify(mockSegment).putMetadata("peppol_lookup", "validation_results", validationResults);
        verify(mockSegment).putAnnotation("cert_valid", true);
        verify(mockSegment).putAnnotation("peppol_compliant", false);
    }

    @Test
    void testAddResponseMetadata_Success() {
        // Given
        boolean success = true;
        long processingTime = 1500L;
        String errorCode = null;
        String errorCategory = null;
        
        mockedAWSXRay.when(AWSXRay::getCurrentSegment).thenReturn(mockSegment);

        // When
        xrayTracingService.addResponseMetadata(success, processingTime, errorCode, errorCategory);

        // Then
        verify(mockSegment).putAnnotation("success", true);
        verify(mockSegment).putMetadata(eq("peppol_lookup"), eq("response_details"), any(Map.class));
    }

    @Test
    void testAddResponseMetadata_WithError() {
        // Given
        boolean success = false;
        long processingTime = 500L;
        String errorCode = "E3001";
        String errorCategory = "CERTIFICATE";
        
        mockedAWSXRay.when(AWSXRay::getCurrentSegment).thenReturn(mockSegment);

        // When
        xrayTracingService.addResponseMetadata(success, processingTime, errorCode, errorCategory);

        // Then
        verify(mockSegment).putAnnotation("success", false);
        verify(mockSegment).putAnnotation("error_code", errorCode);
        verify(mockSegment).putAnnotation("error_category", errorCategory);
        verify(mockSegment).putMetadata(eq("peppol_lookup"), eq("response_details"), any(Map.class));
    }

    @Test
    void testAddPerformanceMetadata_Success() {
        // Given
        Map<String, Long> operationTimings = Map.of(
            "sml_lookup", 2000L,
            "smp_query", 6000L, // Slow operation > 5 seconds
            "certificate_validation", 1000L
        );
        
        mockedAWSXRay.when(AWSXRay::getCurrentSegment).thenReturn(mockSegment);

        // When
        xrayTracingService.addPerformanceMetadata(operationTimings);

        // Then
        verify(mockSegment).putMetadata("peppol_lookup", "operation_timings", operationTimings);
        verify(mockSegment).putAnnotation("slow_smp_query", Boolean.TRUE); // Should mark slow operations
        verify(mockSegment, never()).putAnnotation(eq("slow_sml_lookup"), any(Boolean.class)); // Should not mark fast operations
    }

    @Test
    void testGetCurrentTraceId_Success() {
        // Given
        com.amazonaws.xray.entities.TraceID mockTraceId = mock(com.amazonaws.xray.entities.TraceID.class);
        when(mockTraceId.toString()).thenReturn("1-5e1b4025-1234567890abcdef");
        when(mockSegment.getTraceId()).thenReturn(mockTraceId);
        mockedAWSXRay.when(AWSXRay::getCurrentSegment).thenReturn(mockSegment);

        // When
        String traceId = xrayTracingService.getCurrentTraceId();

        // Then
        assertThat(traceId).isEqualTo("1-5e1b4025-1234567890abcdef");
    }

    @Test
    void testGetCurrentTraceId_NoSegment() {
        // Given
        mockedAWSXRay.when(AWSXRay::getCurrentSegment).thenReturn(null);

        // When
        String traceId = xrayTracingService.getCurrentTraceId();

        // Then
        assertThat(traceId).isNull();
    }

    @Test
    void testGetCurrentTraceId_Exception() {
        // Given
        mockedAWSXRay.when(AWSXRay::getCurrentSegment).thenThrow(new RuntimeException("X-Ray not available"));

        // When
        String traceId = xrayTracingService.getCurrentTraceId();

        // Then
        assertThat(traceId).isNull();
    }

    @Test
    void testIsTracingActive_True() {
        // Given
        mockedAWSXRay.when(AWSXRay::getCurrentSegment).thenReturn(mockSegment);

        // When
        boolean isActive = xrayTracingService.isTracingActive();

        // Then
        assertThat(isActive).isTrue();
    }

    @Test
    void testIsTracingActive_False() {
        // Given
        mockedAWSXRay.when(AWSXRay::getCurrentSegment).thenReturn(null);

        // When
        boolean isActive = xrayTracingService.isTracingActive();

        // Then
        assertThat(isActive).isFalse();
    }

    @Test
    void testIsTracingActive_Exception() {
        // Given
        mockedAWSXRay.when(AWSXRay::getCurrentSegment).thenThrow(new RuntimeException("X-Ray error"));

        // When
        boolean isActive = xrayTracingService.isTracingActive();

        // Then
        assertThat(isActive).isFalse();
    }

    @Test
    void testHashForPrivacy_ValidInput() {
        // Given
        String participantId = "9915:test";

        // When
        xrayTracingService.addBusinessAnnotations(participantId, "doc", "proc", "test");

        // Then
        // Verify that the participant ID is hashed (not stored in plain text)
        verify(mockSegment).putAnnotation(eq("participant_id"), argThat((String value) -> 
            !value.equals(participantId) && value.length() == 16));
    }

    @Test
    void testTraceExternalServiceCall_NullSubsegment() throws Exception {
        // Given
        String serviceName = "TEST";
        String operation = "TEST_OP";
        String expectedResult = "result";
        Callable<String> callable = () -> expectedResult;
        
        mockedAWSXRay.when(() -> AWSXRay.beginSubsegment(anyString())).thenReturn(null);

        // When
        String result = xrayTracingService.traceExternalServiceCall(serviceName, operation, callable);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        // Should complete successfully even with null subsegment
    }

    @Test
    void testTraceInternalOperation_NullSubsegment() {
        // Given
        String operationName = "TEST_OPERATION";
        String expectedResult = "result";
        Supplier<String> supplier = () -> expectedResult;
        
        mockedAWSXRay.when(() -> AWSXRay.beginSubsegment(anyString())).thenReturn(null);

        // When
        String result = xrayTracingService.traceInternalOperation(operationName, supplier);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        // Should complete successfully even with null subsegment
    }
}