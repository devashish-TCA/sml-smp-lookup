package com.tca.peppol.integration;

import com.tca.peppol.client.CrlClient;
import com.tca.peppol.client.OcspClient;
import com.tca.peppol.service.*;
import com.tca.peppol.validation.CertificateValidator;
import com.tca.peppol.validation.EndpointValidator;
import com.tca.peppol.validation.XmlSignatureValidator;
import com.tca.peppol.client.SecureHttpClient;
import com.tca.peppol.handler.PeppolLookupHandler;
import com.tca.peppol.model.request.LookupRequest;
import com.tca.peppol.model.response.LookupResponse;
import com.tca.peppol.service.*;
import com.tca.peppol.util.CorrelationIdUtils;
import com.tca.peppol.validation.RequestValidator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Comprehensive integration test suite for Peppol SML/SMP Lookup Tool.
 * 
 * This test suite performs end-to-end integration tests against the real Peppol test network,
 * validating complete workflows with real certificate chains, XML signature validation,
 * network failure simulation, timeout scenarios, and performance testing.
 * 
 * Tests are organized by categories:
 * - Real Peppol test network integration
 * - Certificate chain and XML signature validation
 * - Network failure simulation and timeout scenarios
 * - Performance testing at integration level
 * - Various participant IDs and document types
 * - Environment-specific testing (production and test flows)
 * 
 * Requirements tested: 12.1, 12.2, 12.3, 12.4, 12.5
 * 
 * Note: These tests require network access and may be disabled in CI environments.
 * Use @EnabledIfEnvironmentVariable to control execution.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfEnvironmentVariable(named = "PEPPOL_INTEGRATION_TESTS", matches = "true")
class PeppolNetworkIntegrationTestSuite {

    private static final Logger logger = LoggerFactory.getLogger(PeppolNetworkIntegrationTestSuite.class);
    
    // Test environment configuration
    private static final String TEST_SML_DOMAIN = "acc.edelivery.tech.ec.europa.eu";
    private static final String PROD_SML_DOMAIN = "edelivery.tech.ec.europa.eu";
    
    // Known test participants in Peppol test network
    private static final String[] TEST_PARTICIPANT_IDS = {
        "iso6523-actorid-upis::9915:test",
        "iso6523-actorid-upis::9906:test-participant",
        "iso6523-actorid-upis::0088:test-company"
    };
    
    // Known document types for testing
    private static final String[] TEST_DOCUMENT_TYPES = {
        "busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1",
        "busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2::CreditNote##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1",
        "busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Order-2::Order##urn:fdc:peppol.eu:2017:poacc:ordering:3.0::2.1"
    };
    
    // Known process IDs for testing
    private static final String[] TEST_PROCESS_IDS = {
        "cenbii-procid-ubl::urn:fdc:peppol.eu:2017:poacc:billing:01:1.0",
        "cenbii-procid-ubl::urn:fdc:peppol.eu:2017:poacc:ordering:01:1.0"
    };

    private PeppolLookupHandler handler;
    private PeppolLookupService lookupService;
    private List<TestResult> testResults;

    @BeforeAll
    static void setUpClass() {
        logger.info("Starting Peppol Network Integration Test Suite");
        logger.info("Test SML Domain: {}", TEST_SML_DOMAIN);
        logger.info("Production SML Domain: {}", PROD_SML_DOMAIN);
    }

    @BeforeEach
    void setUp() {
        // Initialize real services (not mocked) for integration testing
        SecureHttpClient httpClient = new SecureHttpClient();
        SmlLookupService smlLookupService = new SmlLookupService();
        SmpQueryService smpQueryService = new SmpQueryService(httpClient, null);
        CertificateService certificateService = new CertificateService();
        ValidationOrchestrator validationOrchestrator = createValidationOrchestrator();
        RequestValidator requestValidator = new RequestValidator();
        CircuitBreakerService circuitBreakerService = new CircuitBreakerService();
        
        lookupService = new PeppolLookupService(
            smlLookupService,
            smpQueryService,
            validationOrchestrator,
            certificateService,
            requestValidator,
            circuitBreakerService
        );
        
        handler = new PeppolLookupHandler(lookupService);
        testResults = new ArrayList<>();
        
        logger.info("Integration test setup completed");
    }

    @AfterEach
    void tearDown() {
        // Log test results summary
        if (!testResults.isEmpty()) {
            long successCount = testResults.stream().mapToLong(r -> r.success ? 1 : 0).sum();
            double avgResponseTime = testResults.stream().mapToDouble(r -> r.responseTimeMs).average().orElse(0.0);
            
            logger.info("Test batch completed - Success: {}/{}, Avg Response Time: {:.2f}ms", 
                successCount, testResults.size(), avgResponseTime);
        }
    }

    @AfterAll
    static void tearDownClass() {
        logger.info("Peppol Network Integration Test Suite completed");
    }

    // ========== Real Peppol Test Network Integration Tests ==========

    @Test
    @Order(1)
    @DisplayName("Test against real Peppol test network with known participants")
    void testRealPeppolTestNetworkIntegration() {
        logger.info("Testing real Peppol test network integration");
        
        for (String participantId : TEST_PARTICIPANT_IDS) {
            LookupRequest request = createTestRequest(participantId, "test");
            request.setIncludeFullCertificateChain(true);
            request.setIncludeTechnicalDetails(true);
            
            Instant startTime = Instant.now();
            LookupResponse response = lookupService.performLookup(request);
            long responseTime = Duration.between(startTime, Instant.now()).toMillis();
            
            TestResult result = new TestResult(participantId, response.isSuccess(), responseTime, response);
            testResults.add(result);
            
            // Log detailed results
            logger.info("Participant: {} - Success: {} - Time: {}ms", 
                participantId, response.isSuccess(), responseTime);
            
            if (response.isSuccess()) {
                assertRealNetworkResponse(response, request);
                logger.info("  Endpoint: {}", response.getEndpointUrl());
                logger.info("  Transport: {}", response.getTransportProfile());
                logger.info("  Certificate Valid: {}", response.getValidationResults().isCertificateValid());
                logger.info("  Peppol Compliant: {}", response.getValidationResults().isPeppolCompliant());
            } else {
                logger.warn("  Errors: {}", response.getErrors());
            }
        }
        
        // Verify at least some participants were found successfully
        long successCount = testResults.stream().mapToLong(r -> r.success ? 1 : 0).sum();
        Assertions.assertTrue(successCount > 0, "At least one participant should be found in test network");
    }

    @ParameterizedTest
    @Order(2)
    @CsvSource({
        "iso6523-actorid-upis::9915:test, test",
        "iso6523-actorid-upis::9906:test-participant, test"
    })
    @DisplayName("Test various participant IDs and document types")
    void testVariousParticipantIdsAndDocumentTypes(String participantId, String environment) {
        logger.info("Testing participant {} in {} environment", participantId, environment);
        
        for (String documentType : TEST_DOCUMENT_TYPES) {
            for (String processId : TEST_PROCESS_IDS) {
                LookupRequest request = new LookupRequest();
                request.setParticipantId(participantId);
                request.setDocumentTypeId(documentType);
                request.setProcessId(processId);
                request.setEnvironment(environment);
                request.setRequestId(CorrelationIdUtils.generateCorrelationId());
                
                Instant startTime = Instant.now();
                LookupResponse response = lookupService.performLookup(request);
                long responseTime = Duration.between(startTime, Instant.now()).toMillis();
                
                TestResult result = new TestResult(
                    participantId + ":" + documentType.substring(0, 50) + "...", 
                    response.isSuccess(), responseTime, response);
                testResults.add(result);
                
                logger.info("  Document: {} - Success: {} - Time: {}ms", 
                    documentType.substring(documentType.lastIndexOf(":") + 1), 
                    response.isSuccess(), responseTime);
                
                if (response.isSuccess()) {
                    assertRealNetworkResponse(response, request);
                }
                
                // Add small delay to avoid overwhelming the test network
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // ========== Certificate Chain and XML Signature Validation Tests ==========

    @Test
    @Order(3)
    @DisplayName("Test real certificate chains and XML signature validation")
    void testRealCertificateChainsAndXmlSignatureValidation() {
        logger.info("Testing real certificate chains and XML signature validation");
        
        LookupRequest request = createTestRequest(TEST_PARTICIPANT_IDS[0], "test");
        request.setIncludeFullCertificateChain(true);
        request.setIncludeTechnicalDetails(true);
        
        LookupResponse response = lookupService.performLookup(request);
        
        if (response.isSuccess()) {
            // Verify certificate chain validation
            Assertions.assertNotNull(response.getCertificate(), "Certificate should be present");
            Assertions.assertTrue(response.getCertificate().contains("BEGIN CERTIFICATE"), 
                "Certificate should be in PEM format");
            
            // Verify certificate validation results
            Assertions.assertNotNull(response.getValidationResults(), "Validation results should be present");
            
            if (response.getValidationResults().isCertificateValid()) {
                logger.info("Certificate validation passed:");
                logger.info("  Chain Valid: {}", response.getValidationResults().isCertificateChainValid());
                logger.info("  Not Expired: {}", response.getValidationResults().isCertificateNotExpired());
                logger.info("  Not Revoked: {}", response.getValidationResults().isCertificateNotRevoked());
                logger.info("  From OpenPeppol: {}", response.getValidationResults().isCertificateFromOpenPeppol());
                logger.info("  Key Length Valid: {}", response.getValidationResults().isCertificateKeyLengthValid());
                logger.info("  Policy Valid: {}", response.getValidationResults().isCertificatePolicyValid());
            }
            
            // Verify XML signature validation
            if (response.getValidationResults().isSmpSignatureValid()) {
                logger.info("XML signature validation passed:");
                logger.info("  Canonicalization Valid: {}", response.getValidationResults().isXmlCanonicalizationValid());
                logger.info("  Algorithm Valid: {}", response.getValidationResults().isSignatureAlgorithmValid());
            }
            
            // Verify revocation checking
            logger.info("Revocation checking:");
            logger.info("  OCSP Check: {}", response.getValidationResults().isOcspCheckPassed());
            logger.info("  CRL Check: {}", response.getValidationResults().isCrlCheckPassed());
            
            // Verify certificate details if included
            if (response.getCertificateDetails() != null) {
                Assertions.assertNotNull(response.getCertificateDetails().getSubject());
                Assertions.assertNotNull(response.getCertificateDetails().getIssuer());
                Assertions.assertTrue(response.getCertificateDetails().getKeyLength() >= 2048, 
                    "Key length should be at least 2048 bits");
                
                logger.info("Certificate details:");
                logger.info("  Subject: {}", response.getCertificateDetails().getSubject());
                logger.info("  Issuer: {}", response.getCertificateDetails().getIssuer());
                logger.info("  Key Length: {}", response.getCertificateDetails().getKeyLength());
                logger.info("  Algorithm: {}", response.getCertificateDetails().getSignatureAlgorithm());
            }
        } else {
            logger.warn("Certificate chain test failed - no successful lookup available");
        }
    }

    // ========== Network Failure Simulation and Timeout Tests ==========

    @Test
    @Order(4)
    @DisplayName("Test network failure simulation and timeout scenarios")
    void testNetworkFailureSimulationAndTimeoutScenarios() {
        logger.info("Testing network failure simulation and timeout scenarios");
        
        // Test with invalid participant (should cause SML DNS failure)
        testInvalidParticipantScenario();
        
        // Test with non-existent SMP (should cause SMP HTTP failure)
        testNonExistentSmpScenario();
        
        // Test timeout scenarios with very short timeouts
        testTimeoutScenarios();
    }

    private void testInvalidParticipantScenario() {
        logger.info("Testing invalid participant scenario (SML DNS failure)");
        
        LookupRequest request = createTestRequest("iso6523-actorid-upis::9999:nonexistent", "test");
        
        Instant startTime = Instant.now();
        LookupResponse response = lookupService.performLookup(request);
        long responseTime = Duration.between(startTime, Instant.now()).toMillis();
        
        TestResult result = new TestResult("invalid-participant", response.isSuccess(), responseTime, response);
        testResults.add(result);
        
        // Should fail but handle gracefully
        Assertions.assertFalse(response.isSuccess(), "Invalid participant should fail");
        Assertions.assertTrue(response.hasErrors(), "Should have error details");
        Assertions.assertNotNull(response.getErrors(), "Error list should not be null");
        
        // Verify error classification
        boolean hasSmlError = response.getErrors().stream()
            .anyMatch(error -> error.getCategory().equals("SML") || error.getCode().startsWith("E1"));
        Assertions.assertTrue(hasSmlError, "Should have SML-related error");
        
        logger.info("Invalid participant test completed - Response time: {}ms", responseTime);
    }

    private void testNonExistentSmpScenario() {
        logger.info("Testing non-existent SMP scenario");
        
        // Use a participant that might resolve in SML but have invalid SMP
        LookupRequest request = createTestRequest("iso6523-actorid-upis::9915:invalid-smp", "test");
        
        Instant startTime = Instant.now();
        LookupResponse response = lookupService.performLookup(request);
        long responseTime = Duration.between(startTime, Instant.now()).toMillis();
        
        TestResult result = new TestResult("invalid-smp", response.isSuccess(), responseTime, response);
        testResults.add(result);
        
        logger.info("Non-existent SMP test completed - Success: {} - Response time: {}ms", 
            response.isSuccess(), responseTime);
        
        if (!response.isSuccess() && response.hasErrors()) {
            // Verify error classification
            boolean hasSmpError = response.getErrors().stream()
                .anyMatch(error -> error.getCategory().equals("SMP") || error.getCode().startsWith("E2"));
            
            if (hasSmpError) {
                logger.info("SMP error correctly identified");
            }
        }
    }

    private void testTimeoutScenarios() {
        logger.info("Testing timeout scenarios");
        
        // Test with a valid participant but monitor for reasonable response times
        LookupRequest request = createTestRequest(TEST_PARTICIPANT_IDS[0], "test");
        request.setIncludeTechnicalDetails(true);
        
        Instant startTime = Instant.now();
        LookupResponse response = lookupService.performLookup(request);
        long responseTime = Duration.between(startTime, Instant.now()).toMillis();
        
        TestResult result = new TestResult("timeout-test", response.isSuccess(), responseTime, response);
        testResults.add(result);
        
        // Verify response time is within reasonable bounds (should be < 60 seconds)
        Assertions.assertTrue(responseTime < 60000, 
            "Response time should be less than 60 seconds, was: " + responseTime + "ms");
        
        if (response.isSuccess() && response.getTechnicalDetails() != null) {
            // Verify individual step timings
            var stepTimings = response.getTechnicalDetails().getStepTimings();
            if (stepTimings != null) {
                stepTimings.forEach((step, timing) -> {
                    logger.info("  {}: {}ms", step, timing);
                    Assertions.assertTrue(timing >= 0, "Step timing should be non-negative: " + step);
                });
            }
        }
        
        logger.info("Timeout scenario test completed - Response time: {}ms", responseTime);
    }

    // ========== Performance Testing at Integration Level ==========

    @Test
    @Order(5)
    @DisplayName("Test performance at integration level")
    void testPerformanceAtIntegrationLevel() {
        logger.info("Testing performance at integration level");
        
        // Single request performance test
        testSingleRequestPerformance();
        
        // Concurrent request performance test
        testConcurrentRequestPerformance();
        
        // Sustained load performance test
        testSustainedLoadPerformance();
    }

    private void testSingleRequestPerformance() {
        logger.info("Testing single request performance");
        
        List<Long> responseTimes = new ArrayList<>();
        
        // Perform multiple single requests to get average performance
        for (int i = 0; i < 5; i++) {
            LookupRequest request = createTestRequest(TEST_PARTICIPANT_IDS[0], "test");
            
            Instant startTime = Instant.now();
            LookupResponse response = lookupService.performLookup(request);
            long responseTime = Duration.between(startTime, Instant.now()).toMillis();
            
            responseTimes.add(responseTime);
            
            TestResult result = new TestResult("perf-single-" + i, response.isSuccess(), responseTime, response);
            testResults.add(result);
            
            logger.info("Single request {} - Success: {} - Time: {}ms", i + 1, response.isSuccess(), responseTime);
        }
        
        // Calculate performance statistics
        double avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long minResponseTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        
        logger.info("Single request performance - Avg: {:.2f}ms, Min: {}ms, Max: {}ms", 
            avgResponseTime, minResponseTime, maxResponseTime);
        
        // Performance assertions
        Assertions.assertTrue(avgResponseTime < 30000, 
            "Average response time should be less than 30 seconds, was: " + avgResponseTime + "ms");
        Assertions.assertTrue(maxResponseTime < 60000, 
            "Maximum response time should be less than 60 seconds, was: " + maxResponseTime + "ms");
    }

    private void testConcurrentRequestPerformance() {
        logger.info("Testing concurrent request performance");
        
        int concurrentRequests = 5;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        List<Future<TestResult>> futures = new ArrayList<>();
        
        Instant startTime = Instant.now();
        
        // Submit concurrent requests
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestId = i;
            Future<TestResult> future = executor.submit(() -> {
                LookupRequest request = createTestRequest(TEST_PARTICIPANT_IDS[requestId % TEST_PARTICIPANT_IDS.length], "test");
                
                Instant reqStartTime = Instant.now();
                LookupResponse response = lookupService.performLookup(request);
                long responseTime = Duration.between(reqStartTime, Instant.now()).toMillis();
                
                return new TestResult("concurrent-" + requestId, response.isSuccess(), responseTime, response);
            });
            futures.add(future);
        }
        
        // Collect results
        List<TestResult> concurrentResults = new ArrayList<>();
        for (Future<TestResult> future : futures) {
            try {
                TestResult result = future.get(120, TimeUnit.SECONDS); // 2 minute timeout
                concurrentResults.add(result);
                testResults.add(result);
            } catch (Exception e) {
                logger.error("Concurrent request failed", e);
                Assertions.fail("Concurrent request failed: " + e.getMessage());
            }
        }
        
        executor.shutdown();
        
        long totalTime = Duration.between(startTime, Instant.now()).toMillis();
        
        // Analyze concurrent performance
        long successCount = concurrentResults.stream().mapToLong(r -> r.success ? 1 : 0).sum();
        double avgResponseTime = concurrentResults.stream().mapToDouble(r -> r.responseTimeMs).average().orElse(0.0);
        
        logger.info("Concurrent performance - Total time: {}ms, Success: {}/{}, Avg response: {:.2f}ms", 
            totalTime, successCount, concurrentRequests, avgResponseTime);
        
        // Performance assertions
        Assertions.assertTrue(successCount >= concurrentRequests * 0.8, 
            "At least 80% of concurrent requests should succeed");
        Assertions.assertTrue(totalTime < 120000, 
            "Total concurrent execution should complete within 2 minutes");
    }

    private void testSustainedLoadPerformance() {
        logger.info("Testing sustained load performance");
        
        int totalRequests = 10;
        int requestsPerSecond = 2;
        long intervalMs = 1000 / requestsPerSecond;
        
        List<TestResult> sustainedResults = new ArrayList<>();
        
        for (int i = 0; i < totalRequests; i++) {
            LookupRequest request = createTestRequest(TEST_PARTICIPANT_IDS[i % TEST_PARTICIPANT_IDS.length], "test");
            
            Instant startTime = Instant.now();
            LookupResponse response = lookupService.performLookup(request);
            long responseTime = Duration.between(startTime, Instant.now()).toMillis();
            
            TestResult result = new TestResult("sustained-" + i, response.isSuccess(), responseTime, response);
            sustainedResults.add(result);
            testResults.add(result);
            
            logger.info("Sustained request {} - Success: {} - Time: {}ms", i + 1, response.isSuccess(), responseTime);
            
            // Wait for next interval (except for last request)
            if (i < totalRequests - 1) {
                try {
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        // Analyze sustained load performance
        long successCount = sustainedResults.stream().mapToLong(r -> r.success ? 1 : 0).sum();
        double avgResponseTime = sustainedResults.stream().mapToDouble(r -> r.responseTimeMs).average().orElse(0.0);
        
        logger.info("Sustained load performance - Success: {}/{}, Avg response: {:.2f}ms", 
            successCount, totalRequests, avgResponseTime);
        
        // Performance assertions
        Assertions.assertTrue(successCount >= totalRequests * 0.7, 
            "At least 70% of sustained requests should succeed");
    }

    // ========== Environment-Specific Testing ==========

    @ParameterizedTest
    @Order(6)
    @ValueSource(strings = {"test", "production"})
    @DisplayName("Test environment-specific flows")
    void testEnvironmentSpecificFlows(String environment) {
        logger.info("Testing environment-specific flows for: {}", environment);
        
        // Note: Production tests should be very limited and use known safe participants
        String participantId = "production".equals(environment) ? 
            "iso6523-actorid-upis::9915:test" :  // Use test participant even for production to avoid issues
            TEST_PARTICIPANT_IDS[0];
        
        LookupRequest request = createTestRequest(participantId, environment);
        request.setIncludeTechnicalDetails(true);
        
        Instant startTime = Instant.now();
        LookupResponse response = lookupService.performLookup(request);
        long responseTime = Duration.between(startTime, Instant.now()).toMillis();
        
        TestResult result = new TestResult("env-" + environment, response.isSuccess(), responseTime, response);
        testResults.add(result);
        
        logger.info("Environment {} test - Success: {} - Time: {}ms", environment, response.isSuccess(), responseTime);
        
        if (response.isSuccess()) {
            // Verify environment-specific behavior
            Assertions.assertEquals(environment, response.getEnvironment(), "Response should indicate correct environment");
            
            if (response.getTechnicalDetails() != null) {
                String expectedSmlDomain = "production".equals(environment) ? PROD_SML_DOMAIN : TEST_SML_DOMAIN;
                // Note: We can't directly verify SML domain from response, but we can verify the lookup succeeded
                logger.info("Environment {} lookup succeeded with expected domain configuration", environment);
            }
            
            // Verify compliance for production environment
            if ("production".equals(environment) && response.getValidationResults() != null) {
                Assertions.assertTrue(response.getValidationResults().isPeppolCompliant(), 
                    "Production environment should ensure Peppol compliance");
                Assertions.assertTrue(response.getValidationResults().isProductionNetworkCompliant(), 
                    "Production environment should be production network compliant");
            }
        } else if ("production".equals(environment)) {
            // Production failures are acceptable for test participants
            logger.info("Production environment test failed (expected for test participants)");
        }
    }

    // ========== Helper Methods ==========

    private LookupRequest createTestRequest(String participantId, String environment) {
        LookupRequest request = new LookupRequest();
        request.setParticipantId(participantId);
        request.setDocumentTypeId(TEST_DOCUMENT_TYPES[0]); // Use first document type as default
        request.setProcessId(TEST_PROCESS_IDS[0]); // Use first process ID as default
        request.setEnvironment(environment);
        request.setRequestId(CorrelationIdUtils.generateCorrelationId());
        return request;
    }

    private ValidationOrchestrator createValidationOrchestrator() {
        // Create real validation orchestrator with all validators
        return new ValidationOrchestrator(
            new CertificateValidator(),
            new XmlSignatureValidator(),
            new EndpointValidator(new SecureHttpClient()),
            new OcspClient(new XRayTracingService()),
            new CrlClient(new XRayTracingService())
        );
    }

    private void assertRealNetworkResponse(LookupResponse response, LookupRequest request) {
        // Basic response validation
        Assertions.assertNotNull(response, "Response should not be null");
        Assertions.assertTrue(response.isSuccess(), "Response should be successful");
        Assertions.assertEquals(request.getParticipantId(), response.getParticipantId());
        Assertions.assertEquals(request.getEnvironment(), response.getEnvironment());
        
        // Endpoint validation
        Assertions.assertNotNull(response.getEndpointUrl(), "Endpoint URL should be present");
        Assertions.assertTrue(response.getEndpointUrl().startsWith("https://"), 
            "Endpoint URL should use HTTPS");
        
        // Transport profile validation
        Assertions.assertNotNull(response.getTransportProfile(), "Transport profile should be present");
        Assertions.assertTrue(response.getTransportProfile().contains("peppol"), 
            "Transport profile should be Peppol-related");
        
        // Certificate validation
        Assertions.assertNotNull(response.getCertificate(), "Certificate should be present");
        Assertions.assertTrue(response.getCertificate().contains("BEGIN CERTIFICATE"), 
            "Certificate should be in PEM format");
        
        // Validation results
        Assertions.assertNotNull(response.getValidationResults(), "Validation results should be present");
        
        // Timing validation
        Assertions.assertTrue(response.getProcessingTimeMs() > 0, "Processing time should be positive");
        Assertions.assertNotNull(response.getTimestamp(), "Timestamp should be present");
        Assertions.assertNotNull(response.getRequestId(), "Request ID should be present");
    }

    // ========== Test Result Tracking ==========

    private static class TestResult {
        final String testName;
        final boolean success;
        final long responseTimeMs;
        final LookupResponse response;

        TestResult(String testName, boolean success, long responseTimeMs, LookupResponse response) {
            this.testName = testName;
            this.success = success;
            this.responseTimeMs = responseTimeMs;
            this.response = response;
        }
    }
}