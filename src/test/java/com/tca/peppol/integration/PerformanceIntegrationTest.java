//package com.tca.peppol.integration;
//
//import com.tca.peppol.client.CrlClient;
//import com.tca.peppol.client.OcspClient;
//import com.tca.peppol.service.*;
//import com.tca.peppol.validation.CertificateValidator;
//import com.tca.peppol.validation.EndpointValidator;
//import com.tca.peppol.validation.XmlSignatureValidator;
//import com.tca.peppol.client.SecureHttpClient;
//import com.tca.peppol.model.request.LookupRequest;
//import com.tca.peppol.model.response.LookupResponse;
//import com.tca.peppol.service.*;
//import com.tca.peppol.util.CorrelationIdUtils;
//import com.tca.peppol.validation.RequestValidator;
//import org.junit.jupiter.api.*;
//import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.time.Duration;
//import java.time.Instant;
//import java.util.*;
//import java.util.concurrent.*;
//import java.util.stream.Collectors;
//
///**
// * Performance integration testing for Peppol SML/SMP Lookup Tool.
// *
// * This test class focuses on performance characteristics of the complete system
// * under various load conditions and scenarios. Tests include:
// *
// * - Single request performance benchmarking
// * - Concurrent request handling and throughput
// * - Sustained load performance over time
// * - Memory usage and resource consumption
// * - Performance degradation under stress
// * - Cold start vs warm performance
// * - Caching effectiveness
// *
// * Requirements tested: 12.4 (performance testing at integration level)
// */
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//@EnabledIfEnvironmentVariable(named = "PEPPOL_INTEGRATION_TESTS", matches = "true")
//class PerformanceIntegrationTest {
//
//    private static final Logger logger = LoggerFactory.getLogger(PerformanceIntegrationTest.class);
//
//    // Performance test configuration
//    private static final int WARMUP_REQUESTS = 5;
//    private static final int BENCHMARK_REQUESTS = 20;
//    private static final int CONCURRENT_USERS = 10;
//    private static final int SUSTAINED_LOAD_DURATION_SECONDS = 30;
//    private static final int STRESS_TEST_REQUESTS = 50;
//
//    // Performance thresholds (in milliseconds)
//    private static final long MAX_SINGLE_REQUEST_TIME = 30000; // 30 seconds
//    private static final long MAX_P95_RESPONSE_TIME = 45000;   // 45 seconds
//    private static final long MAX_P99_RESPONSE_TIME = 60000;   // 60 seconds
//    private static final double MIN_SUCCESS_RATE = 0.8;       // 80%
//
//    // Test participants for performance testing
//    private static final String[] PERFORMANCE_TEST_PARTICIPANTS = {
//        "iso6523-actorid-upis::9915:test",
//        "iso6523-actorid-upis::9906:test-participant"
//    };
//
//    private PeppolLookupService lookupService;
//    private List<PerformanceMetric> performanceMetrics;
//
//    @BeforeAll
//    static void setUpClass() {
//        logger.info("Starting Peppol Performance Integration Tests");
//        logger.info("Performance thresholds:");
//        logger.info("  Max single request: {}ms", MAX_SINGLE_REQUEST_TIME);
//        logger.info("  Max P95 response: {}ms", MAX_P95_RESPONSE_TIME);
//        logger.info("  Max P99 response: {}ms", MAX_P99_RESPONSE_TIME);
//        logger.info("  Min success rate: {}%", MIN_SUCCESS_RATE * 100);
//    }
//
//    @BeforeEach
//    void setUp() {
//        // Initialize real services for performance testing
//        SecureHttpClient httpClient = new SecureHttpClient();
//        SmlLookupService smlLookupService = new SmlLookupService();
//        SmpQueryService smpQueryService = new SmpQueryService(httpClient, null);
//        CertificateService certificateService = new CertificateService();
//        ValidationOrchestrator validationOrchestrator = createValidationOrchestrator();
//        RequestValidator requestValidator = new RequestValidator();
//        CircuitBreakerService circuitBreakerService = new CircuitBreakerService();
//
//        lookupService = new PeppolLookupService(
//            smlLookupService,
//            smpQueryService,
//            validationOrchestrator,
//            certificateService,
//            requestValidator,
//            circuitBreakerService
//        );
//
//        performanceMetrics = new ArrayList<>();
//
//        logger.info("Performance test setup completed");
//    }
//
//    @AfterEach
//    void tearDown() {
//        if (!performanceMetrics.isEmpty()) {
//            logPerformanceSummary();
//        }
//    }
//
//    // ========== Single Request Performance Tests ==========
//
//    @Test
//    @Order(1)
//    @DisplayName("Test single request performance baseline")
//    void testSingleRequestPerformanceBaseline() {
//        logger.info("Testing single request performance baseline");
//
//        // Warmup requests
//        performWarmup();
//
//        // Benchmark requests
//        List<Long> responseTimes = new ArrayList<>();
//        List<Boolean> successResults = new ArrayList<>();
//
//        for (int i = 0; i < BENCHMARK_REQUESTS; i++) {
//            LookupRequest request = createPerformanceTestRequest(PERFORMANCE_TEST_PARTICIPANTS[0]);
//
//            Instant startTime = Instant.now();
//            LookupResponse response = lookupService.performLookup(request);
//            long responseTime = Duration.between(startTime, Instant.now()).toMillis();
//
//            responseTimes.add(responseTime);
//            successResults.add(response.isSuccess());
//
//            PerformanceMetric metric = new PerformanceMetric(
//                "single-request-" + i, responseTime, response.isSuccess(), 1, startTime);
//            performanceMetrics.add(metric);
//
//            logger.debug("Single request {} - Success: {} - Time: {}ms", i + 1, response.isSuccess(), responseTime);
//        }
//
//        // Calculate statistics
//        PerformanceStatistics stats = calculateStatistics(responseTimes, successResults);
//
//        logger.info("Single request performance results:");
//        logger.info("  Average: {:.2f}ms", stats.averageResponseTime);
//        logger.info("  Median: {}ms", stats.medianResponseTime);
//        logger.info("  P95: {}ms", stats.p95ResponseTime);
//        logger.info("  P99: {}ms", stats.p99ResponseTime);
//        logger.info("  Min: {}ms", stats.minResponseTime);
//        logger.info("  Max: {}ms", stats.maxResponseTime);
//        logger.info("  Success Rate: {:.1f}%", stats.successRate * 100);
//
//        // Performance assertions
//        Assertions.assertTrue(stats.averageResponseTime < MAX_SINGLE_REQUEST_TIME,
//            "Average response time should be less than " + MAX_SINGLE_REQUEST_TIME + "ms, was: " + stats.averageResponseTime + "ms");
//        Assertions.assertTrue(stats.p95ResponseTime < MAX_P95_RESPONSE_TIME,
//            "P95 response time should be less than " + MAX_P95_RESPONSE_TIME + "ms, was: " + stats.p95ResponseTime + "ms");
//        Assertions.assertTrue(stats.p99ResponseTime < MAX_P99_RESPONSE_TIME,
//            "P99 response time should be less than " + MAX_P99_RESPONSE_TIME + "ms, was: " + stats.p99ResponseTime + "ms");
//        Assertions.assertTrue(stats.successRate >= MIN_SUCCESS_RATE,
//            "Success rate should be at least " + (MIN_SUCCESS_RATE * 100) + "%, was: " + (stats.successRate * 100) + "%");
//    }
//
//    // ========== Concurrent Request Performance Tests ==========
//
//    @Test
//    @Order(2)
//    @DisplayName("Test concurrent request handling and throughput")
//    void testConcurrentRequestHandlingAndThroughput() {
//        logger.info("Testing concurrent request handling with {} concurrent users", CONCURRENT_USERS);
//
//        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
//        List<Future<PerformanceMetric>> futures = new ArrayList<>();
//
//        Instant testStartTime = Instant.now();
//
//        // Submit concurrent requests
//        for (int i = 0; i < CONCURRENT_USERS; i++) {
//            final int userId = i;
//            Future<PerformanceMetric> future = executor.submit(() -> {
//                String participantId = PERFORMANCE_TEST_PARTICIPANTS[userId % PERFORMANCE_TEST_PARTICIPANTS.length];
//                LookupRequest request = createPerformanceTestRequest(participantId);
//
//                Instant startTime = Instant.now();
//                LookupResponse response = lookupService.performLookup(request);
//                long responseTime = Duration.between(startTime, Instant.now()).toMillis();
//
//                return new PerformanceMetric(
//                    "concurrent-user-" + userId, responseTime, response.isSuccess(), 1, startTime);
//            });
//            futures.add(future);
//        }
//
//        // Collect results
//        List<PerformanceMetric> concurrentResults = new ArrayList<>();
//        for (Future<PerformanceMetric> future : futures) {
//            try {
//                PerformanceMetric result = future.get(120, TimeUnit.SECONDS);
//                concurrentResults.add(result);
//                performanceMetrics.add(result);
//            } catch (Exception e) {
//                logger.error("Concurrent request failed", e);
//                Assertions.fail("Concurrent request failed: " + e.getMessage());
//            }
//        }
//
//        executor.shutdown();
//
//        long totalTestTime = Duration.between(testStartTime, Instant.now()).toMillis();
//
//        // Calculate concurrent performance statistics
//        List<Long> responseTimes = concurrentResults.stream()
//            .map(m -> m.responseTimeMs)
//            .collect(Collectors.toList());
//        List<Boolean> successResults = concurrentResults.stream()
//            .map(m -> m.success)
//            .collect(Collectors.toList());
//
//        PerformanceStatistics stats = calculateStatistics(responseTimes, successResults);
//        double throughput = (double) CONCURRENT_USERS / (totalTestTime / 1000.0); // requests per second
//
//        logger.info("Concurrent request performance results:");
//        logger.info("  Total test time: {}ms", totalTestTime);
//        logger.info("  Throughput: {:.2f} requests/second", throughput);
//        logger.info("  Average response: {:.2f}ms", stats.averageResponseTime);
//        logger.info("  P95 response: {}ms", stats.p95ResponseTime);
//        logger.info("  Success rate: {:.1f}%", stats.successRate * 100);
//
//        // Concurrent performance assertions
//        Assertions.assertTrue(stats.successRate >= MIN_SUCCESS_RATE,
//            "Concurrent success rate should be at least " + (MIN_SUCCESS_RATE * 100) + "%");
//        Assertions.assertTrue(stats.p95ResponseTime < MAX_P95_RESPONSE_TIME * 1.5,
//            "Concurrent P95 should be reasonable even under load");
//        Assertions.assertTrue(throughput > 0.1, "Should achieve reasonable throughput");
//    }
//
//    // ========== Sustained Load Performance Tests ==========
//
//    @Test
//    @Order(3)
//    @DisplayName("Test sustained load performance over time")
//    void testSustainedLoadPerformanceOverTime() {
//        logger.info("Testing sustained load performance for {} seconds", SUSTAINED_LOAD_DURATION_SECONDS);
//
//        List<PerformanceMetric> sustainedResults = new ArrayList<>();
//        Instant testStartTime = Instant.now();
//        Instant testEndTime = testStartTime.plus(Duration.ofSeconds(SUSTAINED_LOAD_DURATION_SECONDS));
//
//        int requestCount = 0;
//        while (Instant.now().isBefore(testEndTime)) {
//            String participantId = PERFORMANCE_TEST_PARTICIPANTS[requestCount % PERFORMANCE_TEST_PARTICIPANTS.length];
//            LookupRequest request = createPerformanceTestRequest(participantId);
//
//            Instant startTime = Instant.now();
//            LookupResponse response = lookupService.performLookup(request);
//            long responseTime = Duration.between(startTime, Instant.now()).toMillis();
//
//            PerformanceMetric metric = new PerformanceMetric(
//                "sustained-" + requestCount, responseTime, response.isSuccess(), 1, startTime);
//            sustainedResults.add(metric);
//            performanceMetrics.add(metric);
//
//            requestCount++;
//
//            // Log progress every 10 requests
//            if (requestCount % 10 == 0) {
//                long elapsedSeconds = Duration.between(testStartTime, Instant.now()).getSeconds();
//                double currentThroughput = (double) requestCount / elapsedSeconds;
//                logger.info("Sustained load progress: {} requests in {}s ({:.2f} req/s)",
//                    requestCount, elapsedSeconds, currentThroughput);
//            }
//
//            // Small delay to avoid overwhelming the system
//            try {
//                Thread.sleep(500); // 2 requests per second
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                break;
//            }
//        }
//
//        long totalTestTime = Duration.between(testStartTime, Instant.now()).toMillis();
//
//        // Analyze sustained load performance
//        List<Long> responseTimes = sustainedResults.stream()
//            .map(m -> m.responseTimeMs)
//            .collect(Collectors.toList());
//        List<Boolean> successResults = sustainedResults.stream()
//            .map(m -> m.success)
//            .collect(Collectors.toList());
//
//        PerformanceStatistics stats = calculateStatistics(responseTimes, successResults);
//        double averageThroughput = (double) requestCount / (totalTestTime / 1000.0);
//
//        logger.info("Sustained load performance results:");
//        logger.info("  Total requests: {}", requestCount);
//        logger.info("  Total time: {}ms", totalTestTime);
//        logger.info("  Average throughput: {:.2f} requests/second", averageThroughput);
//        logger.info("  Average response: {:.2f}ms", stats.averageResponseTime);
//        logger.info("  P95 response: {}ms", stats.p95ResponseTime);
//        logger.info("  Success rate: {:.1f}%", stats.successRate * 100);
//
//        // Sustained load assertions
//        Assertions.assertTrue(requestCount >= 10, "Should complete at least 10 requests during sustained load");
//        Assertions.assertTrue(stats.successRate >= MIN_SUCCESS_RATE * 0.9,
//            "Sustained load success rate should be reasonable");
//        Assertions.assertTrue(averageThroughput > 0.5, "Should maintain reasonable throughput");
//    }
//
//    // ========== Memory and Resource Performance Tests ==========
//
//    @Test
//    @Order(4)
//    @DisplayName("Test memory usage and resource consumption")
//    void testMemoryUsageAndResourceConsumption() {
//        logger.info("Testing memory usage and resource consumption");
//
//        Runtime runtime = Runtime.getRuntime();
//
//        // Measure initial memory
//        System.gc(); // Suggest garbage collection
//        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
//
//        logger.info("Initial memory usage: {} MB", initialMemory / (1024 * 1024));
//
//        // Perform multiple requests to test memory behavior
//        List<Long> memoryMeasurements = new ArrayList<>();
//
//        for (int i = 0; i < 20; i++) {
//            LookupRequest request = createPerformanceTestRequest(PERFORMANCE_TEST_PARTICIPANTS[0]);
//            request.setIncludeFullCertificateChain(true);
//            request.setIncludeTechnicalDetails(true);
//
//            Instant startTime = Instant.now();
//            LookupResponse response = lookupService.performLookup(request);
//            long responseTime = Duration.between(startTime, Instant.now()).toMillis();
//
//            // Measure memory after each request
//            long currentMemory = runtime.totalMemory() - runtime.freeMemory();
//            memoryMeasurements.add(currentMemory);
//
//            PerformanceMetric metric = new PerformanceMetric(
//                "memory-test-" + i, responseTime, response.isSuccess(), 1, startTime);
//            performanceMetrics.add(metric);
//
//            if (i % 5 == 0) {
//                logger.info("Memory test {} - Memory: {} MB - Response: {}ms",
//                    i, currentMemory / (1024 * 1024), responseTime);
//            }
//        }
//
//        // Analyze memory usage
//        long maxMemory = memoryMeasurements.stream().mapToLong(Long::longValue).max().orElse(0);
//        long avgMemory = (long) memoryMeasurements.stream().mapToLong(Long::longValue).average().orElse(0);
//        long memoryIncrease = maxMemory - initialMemory;
//
//        logger.info("Memory usage analysis:");
//        logger.info("  Initial memory: {} MB", initialMemory / (1024 * 1024));
//        logger.info("  Maximum memory: {} MB", maxMemory / (1024 * 1024));
//        logger.info("  Average memory: {} MB", avgMemory / (1024 * 1024));
//        logger.info("  Memory increase: {} MB", memoryIncrease / (1024 * 1024));
//
//        // Memory usage assertions
//        long maxAllowedMemory = 512 * 1024 * 1024; // 512 MB
//        Assertions.assertTrue(maxMemory < maxAllowedMemory,
//            "Memory usage should stay within reasonable bounds");
//
//        // Memory should not continuously increase (no major memory leaks)
//        long memoryGrowthLimit = 100 * 1024 * 1024; // 100 MB growth limit
//        Assertions.assertTrue(memoryIncrease < memoryGrowthLimit,
//            "Memory increase should be reasonable, was: " + (memoryIncrease / (1024 * 1024)) + " MB");
//    }
//
//    // ========== Cold Start vs Warm Performance Tests ==========
//
//    @Test
//    @Order(5)
//    @DisplayName("Test cold start vs warm performance")
//    void testColdStartVsWarmPerformance() {
//        logger.info("Testing cold start vs warm performance");
//
//        // Simulate cold start (first request)
//        LookupRequest coldStartRequest = createPerformanceTestRequest(PERFORMANCE_TEST_PARTICIPANTS[0]);
//
//        Instant coldStartTime = Instant.now();
//        LookupResponse coldStartResponse = lookupService.performLookup(coldStartRequest);
//        long coldStartDuration = Duration.between(coldStartTime, Instant.now()).toMillis();
//
//        logger.info("Cold start performance: {}ms - Success: {}", coldStartDuration, coldStartResponse.isSuccess());
//
//        // Warm up with a few requests
//        for (int i = 0; i < 3; i++) {
//            LookupRequest warmupRequest = createPerformanceTestRequest(PERFORMANCE_TEST_PARTICIPANTS[0]);
//            lookupService.performLookup(warmupRequest);
//        }
//
//        // Measure warm performance
//        List<Long> warmResponseTimes = new ArrayList<>();
//        for (int i = 0; i < 5; i++) {
//            LookupRequest warmRequest = createPerformanceTestRequest(PERFORMANCE_TEST_PARTICIPANTS[0]);
//
//            Instant warmStartTime = Instant.now();
//            LookupResponse warmResponse = lookupService.performLookup(warmRequest);
//            long warmDuration = Duration.between(warmStartTime, Instant.now()).toMillis();
//
//            warmResponseTimes.add(warmDuration);
//
//            logger.debug("Warm request {} performance: {}ms - Success: {}", i + 1, warmDuration, warmResponse.isSuccess());
//        }
//
//        double avgWarmTime = warmResponseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
//
//        logger.info("Performance comparison:");
//        logger.info("  Cold start: {}ms", coldStartDuration);
//        logger.info("  Warm average: {:.2f}ms", avgWarmTime);
//        logger.info("  Performance improvement: {:.1f}%", ((coldStartDuration - avgWarmTime) / coldStartDuration) * 100);
//
//        // Cold start should not be excessively slow
//        Assertions.assertTrue(coldStartDuration < MAX_SINGLE_REQUEST_TIME * 2,
//            "Cold start should complete within reasonable time");
//
//        // Warm requests should generally be faster than cold start
//        if (coldStartResponse.isSuccess() && avgWarmTime > 0) {
//            // Allow some variance, but warm should generally be faster or similar
//            Assertions.assertTrue(avgWarmTime <= coldStartDuration * 1.2,
//                "Warm requests should not be significantly slower than cold start");
//        }
//    }
//
//    // ========== Helper Methods ==========
//
//    private void performWarmup() {
//        logger.info("Performing warmup with {} requests", WARMUP_REQUESTS);
//
//        for (int i = 0; i < WARMUP_REQUESTS; i++) {
//            LookupRequest request = createPerformanceTestRequest(PERFORMANCE_TEST_PARTICIPANTS[0]);
//            lookupService.performLookup(request);
//        }
//
//        logger.info("Warmup completed");
//    }
//
//    private LookupRequest createPerformanceTestRequest(String participantId) {
//        LookupRequest request = new LookupRequest();
//        request.setParticipantId(participantId);
//        request.setDocumentTypeId("busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1");
//        request.setProcessId("cenbii-procid-ubl::urn:fdc:peppol.eu:2017:poacc:billing:01:1.0");
//        request.setEnvironment("test");
//        request.setRequestId(CorrelationIdUtils.generateCorrelationId());
//        return request;
//    }
//
//    private ValidationOrchestrator createValidationOrchestrator() {
//        return new ValidationOrchestrator(
//            new CertificateValidator(),
//            new XmlSignatureValidator(),
//            new EndpointValidator(new SecureHttpClient()),
//            new OcspClient(new SecureHttpClient()),
//            new CrlClient(new SecureHttpClient())
//        );
//    }
//
//    private PerformanceStatistics calculateStatistics(List<Long> responseTimes, List<Boolean> successResults) {
//        Collections.sort(responseTimes);
//
//        double averageResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
//        long medianResponseTime = responseTimes.get(responseTimes.size() / 2);
//        long p95ResponseTime = responseTimes.get((int) (responseTimes.size() * 0.95));
//        long p99ResponseTime = responseTimes.get((int) (responseTimes.size() * 0.99));
//        long minResponseTime = responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);
//        long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
//
//        double successRate = successResults.stream().mapToDouble(success -> success ? 1.0 : 0.0).average().orElse(0.0);
//
//        return new PerformanceStatistics(
//            averageResponseTime, medianResponseTime, p95ResponseTime, p99ResponseTime,
//            minResponseTime, maxResponseTime, successRate
//        );
//    }
//
//    private void logPerformanceSummary() {
//        logger.info("Performance test summary:");
//        logger.info("  Total metrics collected: {}", performanceMetrics.size());
//
//        List<Long> allResponseTimes = performanceMetrics.stream()
//            .map(m -> m.responseTimeMs)
//            .collect(Collectors.toList());
//        List<Boolean> allSuccessResults = performanceMetrics.stream()
//            .map(m -> m.success)
//            .collect(Collectors.toList());
//
//        if (!allResponseTimes.isEmpty()) {
//            PerformanceStatistics overallStats = calculateStatistics(allResponseTimes, allSuccessResults);
//
//            logger.info("  Overall average response: {:.2f}ms", overallStats.averageResponseTime);
//            logger.info("  Overall P95 response: {}ms", overallStats.p95ResponseTime);
//            logger.info("  Overall success rate: {:.1f}%", overallStats.successRate * 100);
//        }
//    }
//
//    // ========== Performance Data Classes ==========
//
//    private static class PerformanceMetric {
//        final String testName;
//        final long responseTimeMs;
//        final boolean success;
//        final int requestCount;
//        final Instant timestamp;
//
//        PerformanceMetric(String testName, long responseTimeMs, boolean success, int requestCount, Instant timestamp) {
//            this.testName = testName;
//            this.responseTimeMs = responseTimeMs;
//            this.success = success;
//            this.requestCount = requestCount;
//            this.timestamp = timestamp;
//        }
//    }
//
//    private static class PerformanceStatistics {
//        final double averageResponseTime;
//        final long medianResponseTime;
//        final long p95ResponseTime;
//        final long p99ResponseTime;
//        final long minResponseTime;
//        final long maxResponseTime;
//        final double successRate;
//
//        PerformanceStatistics(double averageResponseTime, long medianResponseTime, long p95ResponseTime,
//                            long p99ResponseTime, long minResponseTime, long maxResponseTime, double successRate) {
//            this.averageResponseTime = averageResponseTime;
//            this.medianResponseTime = medianResponseTime;
//            this.p95ResponseTime = p95ResponseTime;
//            this.p99ResponseTime = p99ResponseTime;
//            this.minResponseTime = minResponseTime;
//            this.maxResponseTime = maxResponseTime;
//            this.successRate = successRate;
//        }
//    }
//}