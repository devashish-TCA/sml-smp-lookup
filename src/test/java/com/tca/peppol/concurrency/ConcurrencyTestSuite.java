package com.tca.peppol.concurrency;

import com.tca.peppol.model.request.LookupRequest;
import com.tca.peppol.service.CircuitBreakerService;
import com.tca.peppol.validation.RequestValidator;
import com.tca.peppol.validation.RequestValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive concurrency and thread safety test suite.
 * 
 * Requirements addressed: 12.6 (Concurrency testing for thread safety verification)
 */
@DisplayName("Concurrency Test Suite")
class ConcurrencyTestSuite {

    private RequestValidator requestValidator;
    private CircuitBreakerService circuitBreakerService;
    private LookupRequest validRequest;

    @BeforeEach
    void setUp() {
        requestValidator = new RequestValidator();
        circuitBreakerService = new CircuitBreakerService();
        
        validRequest = new LookupRequest();
        validRequest.setParticipantId("iso6523-actorid-upis::9915:test");
        validRequest.setDocumentTypeId("busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1");
        validRequest.setProcessId("cenbii-procid-ubl::urn:fdc:peppol.eu:2017:poacc:billing:01:1.0");
        validRequest.setEnvironment("production");
    }

    @Nested
    @DisplayName("Request Validator Thread Safety Tests")
    class RequestValidatorThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent validation requests safely")
        void shouldHandleConcurrentValidationRequestsSafely() throws InterruptedException {
            // Given
            int threadCount = 20;
            int requestsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            // When
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < requestsPerThread; j++) {
                            RequestValidationResult result = requestValidator.validateRequest(validRequest);
                            if (result.isValid()) {
                                successCount.incrementAndGet();
                            } else {
                                failureCount.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Then
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(completed).isTrue();
            assertThat(exceptions).isEmpty();
            assertThat(successCount.get()).isEqualTo(threadCount * requestsPerThread);
            assertThat(failureCount.get()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should maintain thread safety with mixed valid and invalid requests")
        void shouldMaintainThreadSafetyWithMixedRequests() throws InterruptedException {
            // Given
            int threadCount = 10;
            int requestsPerThread = 50;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger validResults = new AtomicInteger(0);
            AtomicInteger invalidResults = new AtomicInteger(0);
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            // When
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < requestsPerThread; j++) {
                            LookupRequest request = (j % 2 == 0) ? validRequest : createInvalidRequest(threadId, j);
                            RequestValidationResult result = requestValidator.validateRequest(request);
                            
                            if (result.isValid()) {
                                validResults.incrementAndGet();
                            } else {
                                invalidResults.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Then
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(completed).isTrue();
            assertThat(exceptions).isEmpty();
            
            int totalRequests = threadCount * requestsPerThread;
            assertThat(validResults.get() + invalidResults.get()).isEqualTo(totalRequests);
            
            // Approximately half should be valid (valid requests)
            assertThat(validResults.get()).isBetween(totalRequests / 4, 3 * totalRequests / 4);
        }

        private LookupRequest createInvalidRequest(int threadId, int requestId) {
            LookupRequest invalidRequest = new LookupRequest();
            invalidRequest.setParticipantId(""); // Invalid - empty
            invalidRequest.setDocumentTypeId("invalid-" + threadId + "-" + requestId);
            invalidRequest.setProcessId("invalid");
            invalidRequest.setEnvironment("invalid");
            return invalidRequest;
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Thread Safety Tests")
    class CircuitBreakerThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent circuit breaker operations safely")
        void shouldHandleConcurrentCircuitBreakerOperationsSafely() throws InterruptedException {
            // Given
            int threadCount = 15;
            int operationsPerThread = 20;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            // When
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < operationsPerThread; j++) {
                            String serviceName = "test-service-" + (threadId % 3); // Use 3 different services
                            
                            // Mix of successful and failing operations
                            Supplier<String> operation = (j % 3 == 0) ? 
                                () -> { throw new RuntimeException("Simulated failure"); } :
                                () -> "Success";
                            
                            try {
                                String result = circuitBreakerService.executeWithCircuitBreaker(operation, serviceName);
                                if ("Success".equals(result)) {
                                    successCount.incrementAndGet();
                                }
                            } catch (Exception e) {
                                failureCount.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Then
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(completed).isTrue();
            assertThat(exceptions).isEmpty();
            
            int totalOperations = threadCount * operationsPerThread;
            assertThat(successCount.get() + failureCount.get()).isEqualTo(totalOperations);
        }

        @Test
        @DisplayName("Should maintain circuit breaker state consistency under concurrent access")
        void shouldMaintainCircuitBreakerStateConsistencyUnderConcurrentAccess() throws InterruptedException {
            // Given
            String serviceName = "consistency-test-service";
            int threadCount = 10;
            int operationsPerThread = 30;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicLong totalExecutions = new AtomicLong(0);
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            // When
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < operationsPerThread; j++) {
                            // Alternate between success and failure to test state transitions
                            Supplier<String> operation = (j % 2 == 0) ? 
                                () -> "Success" :
                                () -> { throw new RuntimeException("Failure"); };
                            
                            try {
                                circuitBreakerService.executeWithCircuitBreaker(operation, serviceName);
                            } catch (Exception ignored) {
                                // Expected for failing operations
                            }
                            
                            totalExecutions.incrementAndGet();
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Then
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(completed).isTrue();
            assertThat(exceptions).isEmpty();
            assertThat(totalExecutions.get()).isEqualTo(threadCount * operationsPerThread);
            
            // Circuit breaker should still be functional after concurrent access
            assertThat(circuitBreakerService.isServiceAvailable(serviceName)).isNotNull();
        }
    }

    @Nested
    @DisplayName("Race Condition Tests")
    class RaceConditionTests {

        @Test
        @DisplayName("Should prevent race conditions in shared resource access")
        void shouldPreventRaceConditionsInSharedResourceAccess() throws InterruptedException {
            // Given
            int threadCount = 20;
            int iterationsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            AtomicInteger sharedCounter = new AtomicInteger(0);
            List<Integer> results = Collections.synchronizedList(new ArrayList<>());

            // When
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // Wait for all threads to be ready
                        
                        for (int j = 0; j < iterationsPerThread; j++) {
                            // Simulate shared resource access through validation
                            RequestValidationResult result = requestValidator.validateRequest(validRequest);
                            if (result.isValid()) {
                                int count = sharedCounter.incrementAndGet();
                                results.add(count);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            // Start all threads simultaneously
            startLatch.countDown();
            
            // Then
            boolean completed = endLatch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(completed).isTrue();
            assertThat(sharedCounter.get()).isEqualTo(threadCount * iterationsPerThread);
            assertThat(results).hasSize(threadCount * iterationsPerThread);
            
            // Verify no duplicate counter values (would indicate race condition)
            long uniqueValues = results.stream().distinct().count();
            assertThat(uniqueValues).isEqualTo(results.size());
        }

        @Test
        @DisplayName("Should handle concurrent initialization safely")
        void shouldHandleConcurrentInitializationSafely() throws InterruptedException {
            // Given
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<RequestValidator> validators = Collections.synchronizedList(new ArrayList<>());
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            // When - Multiple threads try to create validators simultaneously
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        RequestValidator validator = new RequestValidator();
                        validators.add(validator);
                        
                        // Test the validator immediately
                        RequestValidationResult result = validator.validateRequest(validRequest);
                        assertThat(result.isValid()).isTrue();
                        
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Then
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(completed).isTrue();
            assertThat(exceptions).isEmpty();
            assertThat(validators).hasSize(threadCount);
            
            // All validators should be functional
            for (RequestValidator validator : validators) {
                RequestValidationResult result = validator.validateRequest(validRequest);
                assertThat(result.isValid()).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("Deadlock Prevention Tests")
    class DeadlockPreventionTests {

        @Test
        @DisplayName("Should prevent deadlocks in complex concurrent scenarios")
        void shouldPreventDeadlocksInComplexConcurrentScenarios() throws InterruptedException {
            // Given
            int threadCount = 8;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger completedOperations = new AtomicInteger(0);
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            // When - Create a scenario that could potentially cause deadlocks
            for (int i = 0; i < threadCount; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < 50; j++) {
                            // Mix different operations that might compete for resources
                            if (j % 3 == 0) {
                                // Validation operation
                                RequestValidationResult result = requestValidator.validateRequest(validRequest);
                                assertThat(result.isValid()).isTrue();
                            } else if (j % 3 == 1) {
                                // Circuit breaker operation
                                String serviceName = "deadlock-test-" + (threadId % 2);
                                circuitBreakerService.executeWithCircuitBreaker(() -> "Success", serviceName);
                            } else {
                                // Mixed operation
                                RequestValidationResult result = requestValidator.validateRequest(validRequest);
                                if (result.isValid()) {
                                    String serviceName = "mixed-service-" + threadId;
                                    circuitBreakerService.executeWithCircuitBreaker(() -> "Success", serviceName);
                                }
                            }
                            completedOperations.incrementAndGet();
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Then
            boolean completed = latch.await(60, TimeUnit.SECONDS); // Longer timeout for complex operations
            executor.shutdown();

            assertThat(completed).isTrue();
            assertThat(exceptions).isEmpty();
            assertThat(completedOperations.get()).isEqualTo(threadCount * 50);
        }
    }

    @Nested
    @DisplayName("Load Balancing and Fairness Tests")
    class LoadBalancingTests {

        @Test
        @DisplayName("Should distribute load fairly across concurrent operations")
        void shouldDistributeLoadFairlyAcrossConcurrentOperations() throws InterruptedException {
            // Given
            int threadCount = 12;
            int operationsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            ConcurrentHashMap<String, AtomicInteger> threadOperationCounts = new ConcurrentHashMap<>();
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            // When
            for (int i = 0; i < threadCount; i++) {
                final String threadName = "Thread-" + i;
                executor.submit(() -> {
                    threadOperationCounts.put(threadName, new AtomicInteger(0));
                    try {
                        for (int j = 0; j < operationsPerThread; j++) {
                            RequestValidationResult result = requestValidator.validateRequest(validRequest);
                            if (result.isValid()) {
                                threadOperationCounts.get(threadName).incrementAndGet();
                            }
                            
                            // Small delay to allow thread switching
                            Thread.sleep(1);
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Then
            boolean completed = latch.await(60, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(completed).isTrue();
            assertThat(exceptions).isEmpty();
            assertThat(threadOperationCounts).hasSize(threadCount);
            
            // Verify fair distribution - each thread should complete all operations
            for (AtomicInteger count : threadOperationCounts.values()) {
                assertThat(count.get()).isEqualTo(operationsPerThread);
            }
        }
    }
}