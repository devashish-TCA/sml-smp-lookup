package com.tca.peppol.service;

import com.tca.peppol.model.response.CertificateDetails;
import com.tca.peppol.validation.RequestValidationResult;
import com.tca.peppol.model.internal.SmlResult;
import com.tca.peppol.model.internal.SmpResult;
import com.tca.peppol.model.request.LookupRequest;
import com.tca.peppol.model.response.LookupResponse;
import com.tca.peppol.model.response.ValidationResults;
import com.tca.peppol.service.ValidationOrchestrator.ComprehensiveValidationResult;
import com.tca.peppol.service.ValidationOrchestrator.ValidationContext;
import com.tca.peppol.validation.RequestValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for PeppolLookupService complete end-to-end workflow.
 * 
 * These tests verify the integration of all services and the main lookup workflow
 * with peppol-commons integration, including:
 * - Complete workflow orchestration
 * - Service integration and error handling
 * - Performance timing collection
 * - Response building with all required fields
 * - Optional features (endpoint connectivity, certificate chain, technical details)
 * - Partial result handling throughout the workflow
 * 
 * Requirements tested: 1.1, 1.2, 1.3, 1.4, 1.5, 16.1, 16.5
 */
@ExtendWith(MockitoExtension.class)
class PeppolLookupServiceIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(PeppolLookupServiceIntegrationTest.class);

    @Mock
    private SmlLookupService mockSmlLookupService;
    
    @Mock
    private SmpQueryService mockSmpQueryService;
    
    @Mock
    private ValidationOrchestrator mockValidationOrchestrator;
    
    @Mock
    private CertificateService mockCertificateService;
    
    @Mock
    private RequestValidator mockRequestValidator;
    
    @Mock
    private CircuitBreakerService mockCircuitBreakerService;
    
    @Mock
    private X509Certificate mockCertificate;

    private PeppolLookupService peppolLookupService;

    @BeforeEach
    void setUp() {
        peppolLookupService = new PeppolLookupService(
            mockSmlLookupService,
            mockSmpQueryService,
            mockValidationOrchestrator,
            mockCertificateService,
            mockRequestValidator,
            mockCircuitBreakerService
        );
    }

    @Test
    void testCompleteSuccessfulWorkflow() throws Exception {
        // Given: Valid request
        LookupRequest request = createValidLookupRequest();
        
        // Mock successful input validation
        when(mockRequestValidator.validateRequest(any())).thenReturn(createValidValidationResult());
        
        // Mock successful SML lookup
        SmlResult smlResult = createSuccessfulSmlResult();
        when(mockCircuitBreakerService.executeWithCircuitBreaker(any(), eq("sml-service")))
            .thenReturn(smlResult);
        
        // Mock successful SMP query
        SmpResult smpResult = createSuccessfulSmpResult();
        when(mockCircuitBreakerService.executeWithCircuitBreaker(any(), eq("smp-service")))
            .thenReturn(smpResult);
        
        // Mock successful validation orchestration
        ComprehensiveValidationResult validationResult = createSuccessfulValidationResult();
        when(mockValidationOrchestrator.orchestrateValidations(any(ValidationContext.class)))
            .thenReturn(validationResult);
        
        // Mock certificate processing
        when(mockCertificateService.extractCertificateDetails(any()))
            .thenReturn(createCertificateDetails());
        when(mockCertificateService.convertDerToPem(any()))
            .thenReturn("-----BEGIN CERTIFICATE-----\nMOCK_CERTIFICATE\n-----END CERTIFICATE-----");

        // When: Performing lookup
        LookupResponse response = peppolLookupService.performLookup(request);

        // Then: Verify successful response
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(request.getParticipantId(), response.getParticipantId());
        assertEquals(request.getDocumentTypeId(), response.getDocumentTypeId());
        assertEquals(request.getProcessId(), response.getProcessId());
        assertEquals(request.getEnvironment(), response.getEnvironment());
        
        // Verify endpoint information
        assertEquals("https://test-endpoint.example.com/as4", response.getEndpointUrl());
        assertEquals("peppol-transport-as4-v2_0", response.getTransportProfile());
        
        // Verify certificate information
        assertNotNull(response.getCertificate());
        assertTrue(response.getCertificate().contains("MOCK_CERTIFICATE"));
        
        // Verify validation results
        assertNotNull(response.getValidationResults());
        assertTrue(response.getValidationResults().isPeppolCompliant());
        
        // Verify timing information
        assertTrue(response.getProcessingTimeMs() > 0);
        assertNotNull(response.getTimestamp());
        assertNotNull(response.getRequestId());
        
        // Verify service interactions
        verify(mockRequestValidator).validateRequest(request);
        verify(mockCircuitBreakerService).executeWithCircuitBreaker(any(), eq("sml-service"));
        verify(mockCircuitBreakerService).executeWithCircuitBreaker(any(), eq("smp-service"));
        verify(mockValidationOrchestrator).orchestrateValidations(any(ValidationContext.class));
        verify(mockCertificateService).extractCertificateDetails(mockCertificate);
        verify(mockCertificateService).convertDerToPem(any());
        
        logger.info("Complete successful workflow test passed");
    }

    @Test
    void testWorkflowWithOptionalFeatures() throws Exception {
        // Given: Request with all optional features enabled
        LookupRequest request = createValidLookupRequest();
        request.setValidateEndpointConnectivity(true);
        request.setIncludeFullCertificateChain(true);
        request.setIncludeTechnicalDetails(true);
        
        // Mock successful responses
        setupSuccessfulMocks();
        
        // Mock certificate chain building
        when(mockCertificateService.buildCertificateChain(any()))
            .thenReturn(Collections.singletonList(mockCertificate));
        when(mockCertificate.getEncoded())
            .thenReturn("mock-certificate-bytes".getBytes());

        // When: Performing lookup
        LookupResponse response = peppolLookupService.performLookup(request);

        // Then: Verify optional features are included
        assertNotNull(response);
        assertTrue(response.isSuccess());
        
        // Verify technical details are included
        assertNotNull(response.getTechnicalDetails());
        assertNotNull(response.getTechnicalDetails().getCorrelationId());
        assertNotNull(response.getTechnicalDetails().getStepTimings());
        assertTrue(response.getTechnicalDetails().getStepTimings().size() > 0);
        
        // Verify certificate details are included
        assertNotNull(response.getCertificateDetails());
        
        // Verify validation options were configured correctly
        verify(mockValidationOrchestrator).orchestrateValidations(argThat(context -> 
            context.getOptions().isTestEndpointConnectivity() &&
            context.getOptions().isValidateSslCertificateMatching()
        ));
        
        // Verify certificate chain building was called
        verify(mockCertificateService).buildCertificateChain(mockCertificate);
        
        logger.info("Workflow with optional features test passed");
    }

    @Test
    void testWorkflowWithSmlFailure() throws Exception {
        // Given: Valid request but SML lookup fails
        LookupRequest request = createValidLookupRequest();
        
        // Mock successful input validation
        when(mockRequestValidator.validateRequest(any())).thenReturn(createValidValidationResult());
        
        // Mock SML lookup failure
        SmlResult failedSmlResult = SmlResult.builder()
            .successful(false)
            .errorMessage("DNS resolution failed")
            .dnsQuery("B-abc123.iso6523-actorid-upis.edelivery.tech.ec.europa.eu")
            .resolutionTimeMs(5000L)
            .build();
        
        when(mockCircuitBreakerService.executeWithCircuitBreaker(any(), eq("sml-service")))
            .thenReturn(failedSmlResult);

        // When: Performing lookup
        LookupResponse response = peppolLookupService.performLookup(request);

        // Then: Verify error response with partial results
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals(request.getParticipantId(), response.getParticipantId());
        
        // Verify errors are present
        assertTrue(response.hasErrors());
        assertNotNull(response.getErrors());
        assertTrue(response.getErrors().size() > 0);
        
        // Verify SMP query was not attempted
        verify(mockCircuitBreakerService, never()).executeWithCircuitBreaker(any(), eq("smp-service"));
        verify(mockValidationOrchestrator, never()).orchestrateValidations(any());
        
        logger.info("Workflow with SML failure test passed");
    }

    @Test
    void testWorkflowWithSmpFailure() throws Exception {
        // Given: Valid request, successful SML but SMP failure
        LookupRequest request = createValidLookupRequest();
        
        // Mock successful input validation and SML lookup
        when(mockRequestValidator.validateRequest(any())).thenReturn(createValidValidationResult());
        when(mockCircuitBreakerService.executeWithCircuitBreaker(any(), eq("sml-service")))
            .thenReturn(createSuccessfulSmlResult());
        
        // Mock SMP query failure
        SmpResult failedSmpResult = SmpResult.builder()
            .successful(false)
            .errorMessage("HTTP 404 - Service not found")
            .queryTimeMs(2000L)
            .build();
        
        when(mockCircuitBreakerService.executeWithCircuitBreaker(any(), eq("smp-service")))
            .thenReturn(failedSmpResult);

        // When: Performing lookup
        LookupResponse response = peppolLookupService.performLookup(request);

        // Then: Verify error response with SML partial results
        assertNotNull(response);
        assertFalse(response.isSuccess());
        
        // Verify errors are present
        assertTrue(response.hasErrors());
        
        // Verify SML results are included in technical details if requested
        if (request.isIncludeTechnicalDetails()) {
            assertNotNull(response.getTechnicalDetails());
            assertNotNull(response.getTechnicalDetails().getSmlDnsQuery());
        }
        
        // Verify validation was not attempted
        verify(mockValidationOrchestrator, never()).orchestrateValidations(any());
        
        logger.info("Workflow with SMP failure test passed");
    }

    @Test
    void testWorkflowWithValidationFailure() throws Exception {
        // Given: Successful SML/SMP but validation failures
        LookupRequest request = createValidLookupRequest();
        
        // Mock successful input validation, SML, and SMP
        setupSuccessfulMocks();
        
        // Mock validation failure
        ValidationResults failedValidation = new ValidationResults();
        failedValidation.setCertificateValid(false);
        failedValidation.setSmpSignatureValid(false);
        failedValidation.setPeppolCompliant(false);
        
        ComprehensiveValidationResult validationResult = new ComprehensiveValidationResult(
            failedValidation, false, false, 1000L, "test-correlation-id", Map.of());
        
        when(mockValidationOrchestrator.orchestrateValidations(any(ValidationContext.class)))
            .thenReturn(validationResult);

        // When: Performing lookup
        LookupResponse response = peppolLookupService.performLookup(request);

        // Then: Verify response indicates validation failure but includes results
        assertNotNull(response);
        assertFalse(response.isSuccess()); // Overall success should be false due to validation failure
        
        // Verify endpoint information is still present
        assertEquals("https://test-endpoint.example.com/as4", response.getEndpointUrl());
        assertEquals("peppol-transport-as4-v2_0", response.getTransportProfile());
        
        // Verify validation results show failures
        assertNotNull(response.getValidationResults());
        assertFalse(response.getValidationResults().isCertificateValid());
        assertFalse(response.getValidationResults().isSmpSignatureValid());
        assertFalse(response.getValidationResults().isPeppolCompliant());
        
        logger.info("Workflow with validation failure test passed");
    }

    @Test
    void testWorkflowWithInputValidationFailure() throws Exception {
        // Given: Invalid request
        LookupRequest request = createValidLookupRequest();
        request.setParticipantId("invalid-participant-id"); // Invalid format
        
        // Mock input validation failure
        java.util.List<String> errors = java.util.Arrays.asList("Invalid participant ID format");
        RequestValidationResult validationResult =
            RequestValidationResult.failure(errors);
        
        when(mockRequestValidator.validateRequest(any())).thenReturn(validationResult);

        // When: Performing lookup
        LookupResponse response = peppolLookupService.performLookup(request);

        // Then: Verify validation error response
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertTrue(response.hasErrors());
        
        // Verify no further processing was attempted
        verify(mockCircuitBreakerService, never()).executeWithCircuitBreaker(any(), anyString());
        verify(mockValidationOrchestrator, never()).orchestrateValidations(any());
        
        logger.info("Workflow with input validation failure test passed");
    }

    @Test
    void testWorkflowPerformanceTiming() throws Exception {
        // Given: Valid request
        LookupRequest request = createValidLookupRequest();
        request.setIncludeTechnicalDetails(true);
        
        // Mock successful responses with realistic timing
        setupSuccessfulMocks();

        // When: Performing lookup
        long startTime = System.currentTimeMillis();
        LookupResponse response = peppolLookupService.performLookup(request);
        long endTime = System.currentTimeMillis();

        // Then: Verify timing information
        assertNotNull(response);
        assertTrue(response.isSuccess());
        
        // Verify processing time is reasonable
        assertTrue(response.getProcessingTimeMs() > 0);
        assertTrue(response.getProcessingTimeMs() <= (endTime - startTime) + 100); // Allow some margin
        
        // Verify technical details include step timings
        assertNotNull(response.getTechnicalDetails());
        assertNotNull(response.getTechnicalDetails().getStepTimings());
        
        Map<String, Long> stepTimings = response.getTechnicalDetails().getStepTimings();
        assertTrue(stepTimings.containsKey("inputValidation"));
        assertTrue(stepTimings.containsKey("smlLookup"));
        assertTrue(stepTimings.containsKey("smpQuery"));
        assertTrue(stepTimings.containsKey("comprehensiveValidation"));
        assertTrue(stepTimings.containsKey("certificateProcessing"));
        assertTrue(stepTimings.containsKey("responseBuilding"));
        
        // Verify all step timings are positive
        stepTimings.values().forEach(timing -> assertTrue(timing >= 0));
        
        logger.info("Workflow performance timing test passed - Total time: {}ms", response.getProcessingTimeMs());
    }

    @Test
    void testWorkflowErrorHandlingAndRecovery() throws Exception {
        // Given: Request that causes various recoverable errors
        LookupRequest request = createValidLookupRequest();
        
        // Mock input validation success
        when(mockRequestValidator.validateRequest(any())).thenReturn(createValidValidationResult());
        
        // Mock SML success
        when(mockCircuitBreakerService.executeWithCircuitBreaker(any(), eq("sml-service")))
            .thenReturn(createSuccessfulSmlResult());
        
        // Mock SMP success
        when(mockCircuitBreakerService.executeWithCircuitBreaker(any(), eq("smp-service")))
            .thenReturn(createSuccessfulSmpResult());
        
        // Mock validation orchestrator success
        when(mockValidationOrchestrator.orchestrateValidations(any()))
            .thenReturn(createSuccessfulValidationResult());
        
        // Mock certificate service failure (non-critical)
        when(mockCertificateService.extractCertificateDetails(any()))
            .thenThrow(new RuntimeException("Certificate processing failed"));
        
        // Mock certificate conversion success
        when(mockCertificateService.convertDerToPem(any()))
            .thenReturn("-----BEGIN CERTIFICATE-----\nMOCK_CERTIFICATE\n-----END CERTIFICATE-----");

        // When: Performing lookup
        LookupResponse response = peppolLookupService.performLookup(request);

        // Then: Verify graceful handling of non-critical errors
        assertNotNull(response);
        assertTrue(response.isSuccess()); // Should still succeed despite certificate processing failure
        
        // Verify core information is present
        assertEquals("https://test-endpoint.example.com/as4", response.getEndpointUrl());
        assertEquals("peppol-transport-as4-v2_0", response.getTransportProfile());
        
        // Verify certificate details are null due to processing failure
        assertNull(response.getCertificateDetails());
        
        // Verify certificate PEM is still present (different processing path)
        assertNotNull(response.getCertificate());
        
        logger.info("Workflow error handling and recovery test passed");
    }

    // Helper methods for creating test data

    private LookupRequest createValidLookupRequest() {
        LookupRequest request = new LookupRequest();
        request.setParticipantId("iso6523-actorid-upis::9915:test");
        request.setDocumentTypeId("busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1");
        request.setProcessId("cenbii-procid-ubl::urn:fdc:peppol.eu:2017:poacc:billing:01:1.0");
        request.setEnvironment("test");
        return request;
    }

    private RequestValidationResult createValidValidationResult() {
        return RequestValidationResult.success(); // Valid by default
    }

    private SmlResult createSuccessfulSmlResult() {
        return SmlResult.builder()
            .successful(true)
            .smpUrl("https://test-smp.example.com")
            .dnsQuery("B-abc123.iso6523-actorid-upis.edelivery.tech.ec.europa.eu")
            .resolutionTimeMs(500L)
            .dnssecValid(true)
            .build();
    }

    private SmpResult createSuccessfulSmpResult() {
        return SmpResult.builder()
            .successful(true)
            .endpointUrl("https://test-endpoint.example.com/as4")
            .transportProfile("peppol-transport-as4-v2_0")
            .certificate(mockCertificate)
            .serviceActivationDate(Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS))
            .serviceExpirationDate(Instant.now().plus(365, java.time.temporal.ChronoUnit.DAYS))
            .queryTimeMs(1000L)
            .participantId("iso6523-actorid-upis::9915:test")
            .documentTypeId("busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1")
            .smpQueryUrl("https://test-smp.example.com/iso6523-actorid-upis%3A%3A9915%3Atest/services/busdox-docid-qns%3A%3Aurn%3Aoasis%3Anames%3Aspecification%3Aubl%3Aschema%3Axsd%3AInvoice-2%3A%3AInvoice%23%23urn%3Acen.eu%3Aen16931%3A2017%23compliant%23urn%3Afdc%3Apeppol.eu%3A2017%3Apoacc%3Abilling%3A3.0%3A%3A2.1")
            .build();
    }

    private ComprehensiveValidationResult createSuccessfulValidationResult() {
        ValidationResults validationResults = new ValidationResults();
        validationResults.setCertificateValid(true);
        validationResults.setCertificateNotExpired(true);
        validationResults.setCertificateNotRevoked(true);
        validationResults.setCertificateFromOpenPeppol(true);
        validationResults.setCertificateChainValid(true);
        validationResults.setCertificateKeyLengthValid(true);
        validationResults.setCertificatePolicyValid(true);
        validationResults.setSmpSignatureValid(true);
        validationResults.setXmlCanonicalizationValid(true);
        validationResults.setSignatureAlgorithmValid(true);
        validationResults.setEndpointAccessible(true);
        validationResults.setTransportProfileSupported(true);
        validationResults.setServiceAvailable(true);
        validationResults.setOcspCheckPassed(true);
        validationResults.setCrlCheckPassed(true);
        validationResults.setSmlDnsResolutionSuccessful(true);
        validationResults.setDnsSecValid(true);
        validationResults.setSmpAccessible(true);
        validationResults.setPeppolCompliant(true);
        validationResults.setProductionNetworkCompliant(true);
        
        return new ComprehensiveValidationResult(
            validationResults, true, true, 2000L, "test-correlation-id", Map.of());
    }

    private CertificateDetails createCertificateDetails() {
        CertificateDetails details =
            new CertificateDetails();
        details.setSubject("CN=Test Certificate, O=Test Organization, C=US");
        details.setIssuer("CN=Test CA, O=Test CA Organization, C=US");
        details.setSerialNumber("123456789");
        details.setNotBefore(Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS));
        details.setNotAfter(Instant.now().plus(365, java.time.temporal.ChronoUnit.DAYS));
        details.setKeyAlgorithm("RSA");
        details.setKeyLength(2048);
        details.setSignatureAlgorithm("SHA256withRSA");
        details.setPeppolCompliant(true);
        return details;
    }

    private void setupSuccessfulMocks() throws Exception {
        when(mockRequestValidator.validateRequest(any())).thenReturn(createValidValidationResult());
        when(mockCircuitBreakerService.executeWithCircuitBreaker(any(), eq("sml-service")))
            .thenReturn(createSuccessfulSmlResult());
        when(mockCircuitBreakerService.executeWithCircuitBreaker(any(), eq("smp-service")))
            .thenReturn(createSuccessfulSmpResult());
        when(mockValidationOrchestrator.orchestrateValidations(any(ValidationContext.class)))
            .thenReturn(createSuccessfulValidationResult());
        when(mockCertificateService.extractCertificateDetails(any()))
            .thenReturn(createCertificateDetails());
        when(mockCertificateService.convertDerToPem(any()))
            .thenReturn("-----BEGIN CERTIFICATE-----\nMOCK_CERTIFICATE\n-----END CERTIFICATE-----");
    }
}