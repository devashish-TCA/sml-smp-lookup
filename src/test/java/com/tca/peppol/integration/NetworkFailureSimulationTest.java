package com.tca.peppol.integration;

import com.tca.peppol.client.CrlClient;
import com.tca.peppol.client.OcspClient;
import com.tca.peppol.service.*;
import com.tca.peppol.validation.CertificateValidator;
import com.tca.peppol.validation.EndpointValidator;
import com.tca.peppol.validation.XmlSignatureValidator;
import com.tca.peppol.client.SecureHttpClient;
import com.tca.peppol.model.request.LookupRequest;
import com.tca.peppol.model.response.LookupResponse;
import com.tca.peppol.service.*;
import com.tca.peppol.util.CorrelationIdUtils;
import com.tca.peppol.validation.RequestValidator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;

/**
 * Network failure simulation and timeout scenario testing for Peppol integration.
 * 
 * This test class specifically focuses on testing network failure scenarios,
 * timeout handling, and resilience patterns in the Peppol lookup system.
 * 
 * Test scenarios include:
 * - DNS resolution failures and timeouts
 * - HTTP connection failures and timeouts
 * - Circuit breaker behavior under failure conditions
 * - Graceful degradation and error handling
 * - Recovery scenarios after network issues
 * 
 * Requirements tested: 12.2, 12.3 (network failure simulation and timeout scenarios)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfEnvironmentVariable(named = "PEPPOL_INTEGRATION_TESTS", matches = "true")
class NetworkFailureSimulationTest {

    private static final Logger logger = LoggerFactory.getLogger(NetworkFailureSimulationTest.class);
    
    private PeppolLookupService lookupService;
    private CircuitBreakerService circuitBreakerService;

    @BeforeEach
    void setUp() {
        // Initialize services for network failure testing
        SecureHttpClient httpClient = new SecureHttpClient();
        SmlLookupService smlLookupService = new SmlLookupService();
        SmpQueryService smpQueryService = new SmpQueryService(httpClient, null);
        CertificateService certificateService = new CertificateService();
        ValidationOrchestrator validationOrchestrator = createValidationOrchestrator();
        RequestValidator requestValidator = new RequestValidator();
        circuitBreakerService = new CircuitBreakerService();
        
        lookupService = new PeppolLookupService(
            smlLookupService,
            smpQueryService,
            validationOrchestrator,
            certificateService,
            requestValidator,
            circuitBreakerService
        );
        
        logger.info("Network failure simulation test setup completed");
    }

    // ========== DNS Resolution Failure Tests ==========

    @Test
    @Order(1)
    @DisplayName("Test DNS resolution failures with invalid domains")
    void testDnsResolutionFailures() {
        logger.info("Testing DNS resolution failures");
        
        // Test with completely invalid participant ID that will cause DNS failure
        LookupRequest request = createTestRequest("iso6523-actorid-upis::9999:nonexistent-participant", "test");
        
        Instant startTime = Instant.now();
        LookupResponse response = lookupService.performLookup(request);
        long responseTime = Duration.between(startTime, Instant.now()).toMillis();
        
        // Verify graceful handling of DNS failure
        Assertions.assertNotNull(response, "Response should not be null even on DNS failure");
        Assertions.assertFalse(response.isSuccess(), "Response should indicate failure");
        Assertions.assertTrue(response.hasErrors(), "Response should contain error details");
        
        // Verify error classification
        boolean hasDnsError = response.getErrors().stream()
            .anyMatch(error -> error.getCategory().equals("SML") || 
                              error.getCategory().equals("NETWORK") ||
                              error.getCode().startsWith("E1") ||
                              error.getCode().startsWith("E4"));
        
        Assertions.assertTrue(hasDnsError, "Should have DNS/SML/Network related error");
        
        // Verify reasonable response time (should fail fast)
        Assertions.assertTrue(responseTime < 30000, 
            "DNS failure should be detected within 30 seconds, was: " + responseTime + "ms");
        
        logger.info("DNS resolution failure test completed - Response time: {}ms", responseTime);
    }

    @ParameterizedTest
    @Order(2)
    @ValueSource(strings = {
        "iso6523-actorid-upis::0000:invalid",
        "iso6523-actorid-upis::1111:nonexistent", 
        "iso6523-actorid-upis::2222:timeout-test"
    })
    @DisplayName("Test various DNS failure scenarios")
    void testVariousDnsFailureScenarios(String participantId) {
        logger.info("Testing DNS failure scenario for participant: {}", participantId);
        
        LookupRequest request = createTestRequest(participantId, "test");
        
        Instant startTime = Instant.now();
        LookupResponse response = lookupService.performLookup(request);
        long responseTime = Duration.between(startTime, Instant.now()).toMillis();
        
        // Log the failure details
        logger.info("Participant: {} - Success: {} - Time: {}ms", 
            participantId, response.isSuccess(), responseTime);
        
        if (!response.isSuccess() && response.hasErrors()) {
            response.getErrors().forEach(error -> 
                logger.info("  Error: {} - {} - {}", error.getCode(), error.getCategory(), error.getMessage()));
        }
        
        // Verify error handling
        Assertions.assertNotNull(response, "Response should not be null");
        
        // Verify response time is reasonable (DNS should timeout/fail within reasonable time)
        Assertions.assertTrue(responseTime < 45000, 
            "DNS failure detection should complete within 45 seconds");
    }

    // ========== HTTP Connection Failure Tests ==========

    @Test
    @Order(3)
    @DisplayName("Test HTTP connection failures and timeouts")
    void testHttpConnectionFailuresAndTimeouts() {
        logger.info("Testing HTTP connection failures and timeouts");
        
        // Test with participant that might resolve in DNS but have unreachable SMP
        LookupRequest request = createTestRequest("iso6523-actorid-upis::9915:unreachable-smp", "test");
        
        Instant startTime = Instant.now();
        LookupResponse response = lookupService.performLookup(request);
        long responseTime = Duration.between(startTime, Instant.now()).toMillis();
        
        logger.info("HTTP connection test - Success: {} - Time: {}ms", response.isSuccess(), responseTime);
        
        if (!response.isSuccess() && response.hasErrors()) {
            // Verify error classification for HTTP failures
            boolean hasHttpError = response.getErrors().stream()
                .anyMatch(error -> error.getCategory().equals("SMP") || 
                                  error.getCategory().equals("NETWORK") ||
                                  error.getCode().startsWith("E2") ||
                                  error.getCode().startsWith("E4"));
            
            if (hasHttpError) {
                logger.info("HTTP error correctly identified");
                response.getErrors().forEach(error -> 
                    logger.info("  HTTP Error: {} - {} - {}", error.getCode(), error.getCategory(), error.getMessage()));
            }
        }
        
        // Verify reasonable timeout behavior
        Assertions.assertTrue(responseTime < 60000, 
            "HTTP timeout should be handled within 60 seconds");
    }

    // ========== Circuit Breaker Behavior Tests ==========

    @Test
    @Order(4)
    @DisplayName("Test circuit breaker behavior under failure conditions")
    void testCircuitBreakerBehaviorUnderFailureConditions() {
        logger.info("Testing circuit breaker behavior under failure conditions");
        
        // Generate multiple failures to trigger circuit breaker
        String failingParticipant = "iso6523-actorid-upis::9999:circuit-breaker-test";
        
        for (int i = 0; i < 7; i++) { // More than the failure threshold (5)
            LookupRequest request = createTestRequest(failingParticipant, "test");
            
            Instant startTime = Instant.now();
            LookupResponse response = lookupService.performLookup(request);
            long responseTime = Duration.between(startTime, Instant.now()).toMillis();
            
            logger.info("Circuit breaker test {} - Success: {} - Time: {}ms", 
                i + 1, response.isSuccess(), responseTime);
            
            // After several failures, responses should be faster (circuit open)
            if (i > 5) {
                // Circuit should be open, so responses should be very fast
                Assertions.assertTrue(responseTime < 5000, 
                    "Circuit breaker should provide fast failure when open, was: " + responseTime + "ms");
            }
            
            // Small delay between requests
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        logger.info("Circuit breaker behavior test completed");
    }

    // ========== Timeout Scenario Tests ==========

    @Test
    @Order(5)
    @DisplayName("Test various timeout scenarios")
    void testVariousTimeoutScenarios() {
        logger.info("Testing various timeout scenarios");
        
        // Test concurrent requests to check for timeout handling under load
        int concurrentRequests = 3;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CompletableFuture<Void>[] futures = new CompletableFuture[concurrentRequests];
        
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestId = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                LookupRequest request = createTestRequest("iso6523-actorid-upis::9999:timeout-test-" + requestId, "test");
                
                Instant startTime = Instant.now();
                LookupResponse response = lookupService.performLookup(request);
                long responseTime = Duration.between(startTime, Instant.now()).toMillis();
                
                logger.info("Concurrent timeout test {} - Success: {} - Time: {}ms", 
                    requestId, response.isSuccess(), responseTime);
                
                // Verify timeout handling
                Assertions.assertTrue(responseTime < 90000, 
                    "Request should timeout within 90 seconds, was: " + responseTime + "ms");
                
            }, executor);
        }
        
        // Wait for all requests to complete
        try {
            CompletableFuture.allOf(futures).get(180, TimeUnit.SECONDS); // 3 minute total timeout
        } catch (Exception e) {
            logger.error("Concurrent timeout test failed", e);
            Assertions.fail("Concurrent timeout test failed: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
        
        logger.info("Timeout scenario tests completed");
    }

    // ========== Recovery Scenario Tests ==========

    @Test
    @Order(6)
    @DisplayName("Test recovery scenarios after network issues")
    void testRecoveryScenariosAfterNetworkIssues() {
        logger.info("Testing recovery scenarios after network issues");
        
        // First, cause some failures
        String testParticipant = "iso6523-actorid-upis::9999:recovery-test";
        
        // Generate failures
        for (int i = 0; i < 3; i++) {
            LookupRequest request = createTestRequest(testParticipant, "test");
            LookupResponse response = lookupService.performLookup(request);
            
            logger.info("Recovery test failure {} - Success: {}", i + 1, response.isSuccess());
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // Wait for circuit breaker recovery period
        logger.info("Waiting for circuit breaker recovery period...");
        try {
            Thread.sleep(5000); // Wait 5 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Now test with a potentially valid participant to see recovery
        LookupRequest recoveryRequest = createTestRequest("iso6523-actorid-upis::9915:test", "test");
        
        Instant startTime = Instant.now();
        LookupResponse recoveryResponse = lookupService.performLookup(recoveryRequest);
        long responseTime = Duration.between(startTime, Instant.now()).toMillis();
        
        logger.info("Recovery test - Success: {} - Time: {}ms", 
            recoveryResponse.isSuccess(), responseTime);
        
        // Verify that the system can still process requests after failures
        Assertions.assertNotNull(recoveryResponse, "Recovery response should not be null");
        
        // The response time should be reasonable (not immediate failure)
        Assertions.assertTrue(responseTime > 100, 
            "Recovery request should take some time to process, was: " + responseTime + "ms");
        
        logger.info("Recovery scenario test completed");
    }

    // ========== Graceful Degradation Tests ==========

    @Test
    @Order(7)
    @DisplayName("Test graceful degradation under various failure conditions")
    void testGracefulDegradationUnderVariousFailureConditions() {
        logger.info("Testing graceful degradation under various failure conditions");
        
        // Test with a request that should provide partial results even if some validations fail
        LookupRequest request = createTestRequest("iso6523-actorid-upis::9915:degradation-test", "test");
        request.setIncludeTechnicalDetails(true);
        
        Instant startTime = Instant.now();
        LookupResponse response = lookupService.performLookup(request);
        long responseTime = Duration.between(startTime, Instant.now()).toMillis();
        
        logger.info("Graceful degradation test - Success: {} - Time: {}ms", 
            response.isSuccess(), responseTime);
        
        // Even if the overall lookup fails, we should get structured error information
        Assertions.assertNotNull(response, "Response should not be null");
        Assertions.assertNotNull(response.getRequestId(), "Request ID should always be present");
        Assertions.assertTrue(response.getProcessingTimeMs() > 0, "Processing time should be recorded");
        Assertions.assertNotNull(response.getTimestamp(), "Timestamp should be present");
        
        if (!response.isSuccess()) {
            // Verify error information is comprehensive
            Assertions.assertTrue(response.hasErrors(), "Failed response should have error details");
            Assertions.assertNotNull(response.getErrors(), "Error list should not be null");
            
            // Verify error structure
            response.getErrors().forEach(error -> {
                Assertions.assertNotNull(error.getCode(), "Error code should be present");
                Assertions.assertNotNull(error.getCategory(), "Error category should be present");
                Assertions.assertNotNull(error.getMessage(), "Error message should be present");
                
                logger.info("  Degradation Error: {} - {} - {}", 
                    error.getCode(), error.getCategory(), error.getMessage());
            });
        }
        
        // Verify technical details are included if requested
        if (request.isIncludeTechnicalDetails() && response.getTechnicalDetails() != null) {
            Assertions.assertNotNull(response.getTechnicalDetails().getCorrelationId(), 
                "Correlation ID should be present in technical details");
            
            logger.info("Technical details preserved during degradation");
        }
        
        logger.info("Graceful degradation test completed");
    }

    // ========== Helper Methods ==========

    private LookupRequest createTestRequest(String participantId, String environment) {
        LookupRequest request = new LookupRequest();
        request.setParticipantId(participantId);
        request.setDocumentTypeId("busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1");
        request.setProcessId("cenbii-procid-ubl::urn:fdc:peppol.eu:2017:poacc:billing:01:1.0");
        request.setEnvironment(environment);
        request.setRequestId(CorrelationIdUtils.generateCorrelationId());
        return request;
    }

    private ValidationOrchestrator createValidationOrchestrator() {
        return new ValidationOrchestrator(
            new CertificateValidator(),
            new XmlSignatureValidator(),
            new EndpointValidator(new SecureHttpClient()),
            new OcspClient(new SecureHttpClient()),
            new CrlClient(new SecureHttpClient())
        );
    }
}