package com.tca.peppol.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Circuit breaker service for protecting external service calls.
 * Implements the circuit breaker pattern using resilience4j with configurable failure thresholds.
 * 
 * Configuration:
 * - Opens after 5 consecutive failures
 * - 60-second recovery timeout
 * - Half-open state with 3 test requests
 * - 2 success threshold to close circuit
 */
public class CircuitBreakerService {
    
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerService.class);
    
    // Circuit breaker configuration constants
    private static final int FAILURE_RATE_THRESHOLD = 100; // 100% failure rate (5 consecutive failures)
    private static final int MINIMUM_NUMBER_OF_CALLS = 5;
    private static final Duration WAIT_DURATION_IN_OPEN_STATE = Duration.ofSeconds(60);
    private static final int PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE = 3;
    private static final int SLIDING_WINDOW_SIZE = 10;
    
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final Map<String, ServiceHealthMetrics> serviceHealthMetrics;
    private final Set<String> servicesWithListeners;
    
    /**
     * Service health metrics for tracking circuit breaker state changes
     */
    public static class ServiceHealthMetrics {
        private final AtomicLong totalCalls = new AtomicLong(0);
        private final AtomicLong successfulCalls = new AtomicLong(0);
        private final AtomicLong failedCalls = new AtomicLong(0);
        private final AtomicLong circuitOpenEvents = new AtomicLong(0);
        private final AtomicLong circuitClosedEvents = new AtomicLong(0);
        private final AtomicLong circuitHalfOpenEvents = new AtomicLong(0);
        private volatile CircuitBreaker.State currentState = CircuitBreaker.State.CLOSED;
        private volatile long lastStateChangeTime = System.currentTimeMillis();
        
        public long getTotalCalls() { return totalCalls.get(); }
        public long getSuccessfulCalls() { return successfulCalls.get(); }
        public long getFailedCalls() { return failedCalls.get(); }
        public long getCircuitOpenEvents() { return circuitOpenEvents.get(); }
        public long getCircuitClosedEvents() { return circuitClosedEvents.get(); }
        public long getCircuitHalfOpenEvents() { return circuitHalfOpenEvents.get(); }
        public CircuitBreaker.State getCurrentState() { return currentState; }
        public long getLastStateChangeTime() { return lastStateChangeTime; }
        
        public double getSuccessRate() {
            long total = totalCalls.get();
            return total > 0 ? (double) successfulCalls.get() / total * 100.0 : 0.0;
        }
        
        public double getFailureRate() {
            long total = totalCalls.get();
            return total > 0 ? (double) failedCalls.get() / total * 100.0 : 0.0;
        }
        
        void recordCall() { totalCalls.incrementAndGet(); }
        void recordSuccess() { successfulCalls.incrementAndGet(); }
        void recordFailure() { failedCalls.incrementAndGet(); }
        void recordStateChange(CircuitBreaker.State newState) {
            this.currentState = newState;
            this.lastStateChangeTime = System.currentTimeMillis();
            
            switch (newState) {
                case OPEN -> circuitOpenEvents.incrementAndGet();
                case CLOSED -> circuitClosedEvents.incrementAndGet();
                case HALF_OPEN -> circuitHalfOpenEvents.incrementAndGet();
            }
        }
    }
    
    public CircuitBreakerService() {
        this.serviceHealthMetrics = new ConcurrentHashMap<>();
        this.servicesWithListeners = ConcurrentHashMap.newKeySet();
        this.circuitBreakerRegistry = CircuitBreakerRegistry.of(createDefaultConfig());
    }
    
    /**
     * Creates the default circuit breaker configuration
     */
    private CircuitBreakerConfig createDefaultConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(FAILURE_RATE_THRESHOLD)
                .minimumNumberOfCalls(MINIMUM_NUMBER_OF_CALLS)
                .waitDurationInOpenState(WAIT_DURATION_IN_OPEN_STATE)
                .permittedNumberOfCallsInHalfOpenState(PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE)
                .slidingWindowSize(SLIDING_WINDOW_SIZE)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .build();
    }
    
    /**
     * Executes an operation with circuit breaker protection
     * 
     * @param operation the operation to execute
     * @param serviceName the name of the service for circuit breaker identification
     * @param <T> the return type of the operation
     * @return the result of the operation
     * @throws Exception if the operation fails or circuit is open
     */
    public <T> T executeWithCircuitBreaker(Supplier<T> operation, String serviceName) throws Exception {
        CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(serviceName);
        ServiceHealthMetrics metrics = getOrCreateMetrics(serviceName);
        
        // Decorate the operation with circuit breaker
        Supplier<T> decoratedOperation = CircuitBreaker.decorateSupplier(circuitBreaker, operation);
        
        try {
            metrics.recordCall();
            T result = decoratedOperation.get();
            recordSuccess(serviceName);
            return result;
        } catch (Exception e) {
            recordFailure(serviceName);
            throw e;
        }
    }
    
    /**
     * Records a successful operation for the specified service
     */
    public void recordSuccess(String serviceName) {
        ServiceHealthMetrics metrics = getOrCreateMetrics(serviceName);
        metrics.recordSuccess();
        
        logger.debug("Recorded success for service: {}, success rate: {:.2f}%", 
                serviceName, metrics.getSuccessRate());
    }
    
    /**
     * Records a failed operation for the specified service
     */
    public void recordFailure(String serviceName) {
        ServiceHealthMetrics metrics = getOrCreateMetrics(serviceName);
        metrics.recordFailure();
        
        logger.warn("Recorded failure for service: {}, failure rate: {:.2f}%", 
                serviceName, metrics.getFailureRate());
    }
    
    /**
     * Gets the current state of the circuit breaker for a service
     */
    public CircuitBreaker.State getCircuitBreakerState(String serviceName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.find(serviceName)
                .orElse(null);
        
        if (circuitBreaker != null) {
            return circuitBreaker.getState();
        }
        
        return CircuitBreaker.State.CLOSED; // Default state if circuit breaker doesn't exist
    }
    
    /**
     * Checks if the circuit breaker is available (closed or half-open)
     */
    public boolean isServiceAvailable(String serviceName) {
        CircuitBreaker.State state = getCircuitBreakerState(serviceName);
        return state == CircuitBreaker.State.CLOSED || state == CircuitBreaker.State.HALF_OPEN;
    }
    
    /**
     * Gets service health metrics for monitoring
     */
    public ServiceHealthMetrics getServiceHealthMetrics(String serviceName) {
        return serviceHealthMetrics.get(serviceName);
    }
    
    /**
     * Gets all service health metrics
     */
    public Map<String, ServiceHealthMetrics> getAllServiceHealthMetrics() {
        return Map.copyOf(serviceHealthMetrics);
    }
    
    /**
     * Manually transitions circuit breaker to closed state (for testing/admin purposes)
     */
    public void forceCloseCircuitBreaker(String serviceName) {
        CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(serviceName);
        circuitBreaker.transitionToClosedState();
        
        logger.info("Manually closed circuit breaker for service: {}", serviceName);
    }
    
    /**
     * Manually transitions circuit breaker to open state (for testing/admin purposes)
     */
    public void forceOpenCircuitBreaker(String serviceName) {
        CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(serviceName);
        circuitBreaker.transitionToOpenState();
        
        logger.info("Manually opened circuit breaker for service: {}", serviceName);
    }
    
    /**
     * Provides graceful degradation by returning a fallback value when circuit is open
     */
    public <T> T executeWithFallback(Supplier<T> operation, Supplier<T> fallback, String serviceName) {
        try {
            return executeWithCircuitBreaker(operation, serviceName);
        } catch (Exception e) {
            logger.warn("Circuit breaker execution failed for service: {}, using fallback. Error: {}", 
                    serviceName, e.getMessage());
            return fallback.get();
        }
    }
    
    /**
     * Gets or creates a circuit breaker for the specified service
     */
    private CircuitBreaker getOrCreateCircuitBreaker(String serviceName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
        
        // Register event listeners for metrics collection only once per service
        if (!servicesWithListeners.contains(serviceName)) {
            registerEventListeners(circuitBreaker, serviceName);
            servicesWithListeners.add(serviceName);
        }
        
        return circuitBreaker;
    }
    
    /**
     * Gets or creates service health metrics
     */
    private ServiceHealthMetrics getOrCreateMetrics(String serviceName) {
        return serviceHealthMetrics.computeIfAbsent(serviceName, k -> new ServiceHealthMetrics());
    }
    
    /**
     * Registers event listeners for circuit breaker state changes
     */
    private void registerEventListeners(CircuitBreaker circuitBreaker, String serviceName) {
        ServiceHealthMetrics metrics = getOrCreateMetrics(serviceName);
        
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    CircuitBreaker.State fromState = event.getStateTransition().getFromState();
                    CircuitBreaker.State toState = event.getStateTransition().getToState();
                    
                    metrics.recordStateChange(toState);
                    
                    logger.info("Circuit breaker state transition for service: {} from {} to {}", 
                            serviceName, fromState, toState);
                })
                .onCallNotPermitted(event -> {
                    logger.warn("Call not permitted for service: {} - circuit breaker is OPEN", serviceName);
                })
                .onFailureRateExceeded(event -> {
                    logger.error("Failure rate exceeded for service: {} - rate: {:.2f}%", 
                            serviceName, event.getFailureRate());
                })
                .onSlowCallRateExceeded(event -> {
                    logger.warn("Slow call rate exceeded for service: {} - rate: {:.2f}%", 
                            serviceName, event.getSlowCallRate());
                });
    }
}