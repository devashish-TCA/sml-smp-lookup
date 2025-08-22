package com.tca.peppol.performance;

import com.tca.peppol.model.request.LookupRequest;
import com.tca.peppol.validation.RequestValidator;
import com.tca.peppol.validation.RequestValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Performance and memory usage test suite for critical paths.
 * 
 * Requirements addressed: 12.4 (Performance testing for critical paths and memory usage)
 */
@DisplayName("Performance Test Suite")
class PerformanceTestSuite {

    private RequestValidator requestValidator;
    private LookupRequest validRequest;
    private MemoryMXBean memoryBean;

    @BeforeEach
    void setUp() {
        requestValidator = new RequestValidator();
        memoryBean = ManagementFactory.getMemoryMXBean();
        
        validRequest = new LookupRequest();
        validRequest.setParticipantId("iso6523-actorid-upis::9915:test");
        validRequest.setDocumentTypeId("busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1");
        validRequest.setProcessId("cenbii-procid-ubl::urn:fdc:peppol.eu:2017:poacc:billing:01:1.0");
        validRequest.setEnvironment("production");
    }

    @Nested
    @DisplayName("Request Validation Performance Tests")
    class RequestValidationPerformanceTests {

        @Test
        @Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
        @DisplayName("Should validate request within 100ms")
        void shouldValidateRequestWithin100ms() {
            // When & Then
            RequestValidationResult result = requestValidator.validateRequest(validRequest);
            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should handle high volume validation requests efficiently")
        void shouldHandleHighVolumeValidationRequests() {
            // Given
            int requestCount = 1000;
            long startTime = System.currentTimeMillis();

            // When
            for (int i = 0; i < requestCount; i++) {
                RequestValidationResult result = requestValidator.validateRequest(validRequest);
                assertThat(result.isValid()).isTrue();
            }

            // Then
            long duration = System.currentTimeMillis() - startTime;
            double requestsPerSecond = (requestCount * 1000.0) / duration;
            
            assertThat(requestsPerSecond).isGreaterThan(100); // Should handle at least 100 requests/second
            assertThat(duration).isLessThan(10000); // Should complete within 10 seconds
        }

        @Test
        @DisplayName("Should maintain consistent performance under load")
        void shouldMaintainConsistentPerformanceUnderLoad() {
            // Given
            int iterations = 100;
            List<Long> durations = new ArrayList<>();

            // When
            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                RequestValidationResult result = requestValidator.validateRequest(validRequest);
                long duration = System.nanoTime() - start;
                
                durations.add(duration);
                assertThat(result.isValid()).isTrue();
            }

            // Then
            double averageDuration = durations.stream().mapToLong(Long::longValue).average().orElse(0.0);
            double maxDuration = durations.stream().mapToLong(Long::longValue).max().orElse(0L);
            
            // Max duration should not be more than 10x the average (consistency check)
            assertThat(maxDuration).isLessThan(averageDuration * 10);
            assertThat(averageDuration).isLessThan(1_000_000); // Less than 1ms average
        }
    }

    @Nested
    @DisplayName("Memory Usage Tests")
    class MemoryUsageTests {

        @Test
        @DisplayName("Should not cause memory leaks during repeated validations")
        void shouldNotCauseMemoryLeaksDuringRepeatedValidations() {
            // Given
            System.gc(); // Force garbage collection before test
            MemoryUsage initialMemory = memoryBean.getHeapMemoryUsage();
            long initialUsed = initialMemory.getUsed();

            // When - Perform many validations
            for (int i = 0; i < 10000; i++) {
                RequestValidationResult result = requestValidator.validateRequest(validRequest);
                assertThat(result.isValid()).isTrue();
                
                // Periodically suggest garbage collection
                if (i % 1000 == 0) {
                    System.gc();
                }
            }

            // Then
            System.gc(); // Force garbage collection after test
            MemoryUsage finalMemory = memoryBean.getHeapMemoryUsage();
            long finalUsed = finalMemory.getUsed();
            long memoryIncrease = finalUsed - initialUsed;

            // Memory increase should be reasonable (less than 50MB)
            assertThat(memoryIncrease).isLessThan(50 * 1024 * 1024);
        }

        @Test
        @DisplayName("Should handle large request objects efficiently")
        void shouldHandleLargeRequestObjectsEfficiently() {
            // Given
            LookupRequest largeRequest = new LookupRequest();
            largeRequest.setParticipantId("iso6523-actorid-upis::9915:" + "A".repeat(1000));
            largeRequest.setDocumentTypeId("busdox-docid-qns::" + "B".repeat(1000));
            largeRequest.setProcessId("cenbii-procid-ubl::" + "C".repeat(1000));
            largeRequest.setEnvironment("production");

            MemoryUsage beforeMemory = memoryBean.getHeapMemoryUsage();
            long startTime = System.currentTimeMillis();

            // When
            RequestValidationResult result = requestValidator.validateRequest(largeRequest);

            // Then
            long duration = System.currentTimeMillis() - startTime;
            MemoryUsage afterMemory = memoryBean.getHeapMemoryUsage();
            
            assertThat(result.isValid()).isFalse(); // Should be invalid due to length
            assertThat(duration).isLessThan(1000); // Should complete within 1 second
            
            // Memory usage should not spike excessively
            long memoryDiff = afterMemory.getUsed() - beforeMemory.getUsed();
            assertThat(memoryDiff).isLessThan(10 * 1024 * 1024); // Less than 10MB increase
        }

        @Test
        @DisplayName("Should efficiently handle concurrent memory usage")
        void shouldEfficientlyHandleConcurrentMemoryUsage() throws InterruptedException {
            // Given
            ExecutorService executor = Executors.newFixedThreadPool(10);
            MemoryUsage initialMemory = memoryBean.getHeapMemoryUsage();
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            // When
            for (int i = 0; i < 100; i++) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    for (int j = 0; j < 100; j++) {
                        RequestValidationResult result = requestValidator.validateRequest(validRequest);
                        assertThat(result.isValid()).isTrue();
                    }
                }, executor);
                futures.add(future);
            }

            // Wait for all tasks to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);

            // Then
            System.gc();
            MemoryUsage finalMemory = memoryBean.getHeapMemoryUsage();
            long memoryIncrease = finalMemory.getUsed() - initialMemory.getUsed();

            // Memory increase should be reasonable even with concurrent access
            assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024); // Less than 100MB
        }
    }

    @Nested
    @DisplayName("Throughput Performance Tests")
    class ThroughputPerformanceTests {

        @Test
        @DisplayName("Should achieve minimum throughput requirements")
        void shouldAchieveMinimumThroughputRequirements() {
            // Given
            int warmupIterations = 100;
            int testIterations = 1000;

            // Warmup
            for (int i = 0; i < warmupIterations; i++) {
                requestValidator.validateRequest(validRequest);
            }

            // When
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < testIterations; i++) {
                RequestValidationResult result = requestValidator.validateRequest(validRequest);
                assertThat(result.isValid()).isTrue();
            }
            long duration = System.currentTimeMillis() - startTime;

            // Then
            double throughput = (testIterations * 1000.0) / duration;
            assertThat(throughput).isGreaterThan(500); // Should handle at least 500 requests/second
        }

        @Test
        @DisplayName("Should maintain throughput under different request types")
        void shouldMaintainThroughputUnderDifferentRequestTypes() {
            // Given
            List<LookupRequest> requests = createVariousRequestTypes();
            int iterations = 100;

            // When & Then
            for (LookupRequest request : requests) {
                long startTime = System.currentTimeMillis();
                
                for (int i = 0; i < iterations; i++) {
                    requestValidator.validateRequest(request);
                }
                
                long duration = System.currentTimeMillis() - startTime;
                double throughput = (iterations * 1000.0) / duration;
                
                // Each request type should maintain reasonable throughput
                assertThat(throughput).isGreaterThan(100);
            }
        }

        private List<LookupRequest> createVariousRequestTypes() {
            List<LookupRequest> requests = new ArrayList<>();
            
            // Valid request
            requests.add(validRequest);
            
            // Request with minimal data
            LookupRequest minimalRequest = new LookupRequest();
            minimalRequest.setParticipantId("iso6523-actorid-upis::9915:min");
            minimalRequest.setDocumentTypeId("busdox-docid-qns::minimal");
            minimalRequest.setProcessId("cenbii-procid-ubl::minimal");
            minimalRequest.setEnvironment("test");
            requests.add(minimalRequest);
            
            // Request with longer data
            LookupRequest longerRequest = new LookupRequest();
            longerRequest.setParticipantId("iso6523-actorid-upis::9915:longer-participant-identifier");
            longerRequest.setDocumentTypeId("busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1");
            longerRequest.setProcessId("cenbii-procid-ubl::urn:fdc:peppol.eu:2017:poacc:billing:01:1.0");
            longerRequest.setEnvironment("production");
            requests.add(longerRequest);
            
            return requests;
        }
    }

    @Nested
    @DisplayName("Resource Utilization Tests")
    class ResourceUtilizationTests {

        @Test
        @DisplayName("Should not exceed CPU usage limits during intensive operations")
        void shouldNotExceedCpuUsageLimitsDuringIntensiveOperations() {
            // Given
            int iterations = 5000;
            long startTime = System.currentTimeMillis();

            // When
            for (int i = 0; i < iterations; i++) {
                // Mix of valid and invalid requests to stress the validator
                LookupRequest request = (i % 2 == 0) ? validRequest : createInvalidRequest();
                requestValidator.validateRequest(request);
            }

            // Then
            long duration = System.currentTimeMillis() - startTime;
            
            // Should complete within reasonable time (indicating efficient CPU usage)
            assertThat(duration).isLessThan(30000); // Less than 30 seconds
            
            // Should maintain reasonable throughput
            double throughput = (iterations * 1000.0) / duration;
            assertThat(throughput).isGreaterThan(50); // At least 50 requests/second
        }

        private LookupRequest createInvalidRequest() {
            LookupRequest invalidRequest = new LookupRequest();
            invalidRequest.setParticipantId(""); // Invalid - empty
            invalidRequest.setDocumentTypeId("invalid");
            invalidRequest.setProcessId("invalid");
            invalidRequest.setEnvironment("invalid");
            return invalidRequest;
        }
    }
}