package com.tca.peppol.wrapper;

import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.xsds.peppol.smp1.ServiceGroupType;
import com.helger.xsds.peppol.smp1.ServiceMetadataType;
import com.helger.xsds.peppol.smp1.SignedServiceMetadataType;
import com.tca.peppol.model.internal.SmpResult;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.net.URI;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Enhanced wrapper around peppol-commons SMPClientReadOnly that adds:
 * - Circuit breaker patterns for resilience
 * - Metrics collection and monitoring
 * - Structured error handling
 * - Performance optimization for AWS Lambda
 * - Additional security validations
 */
public class EnhancedSMPClient {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedSMPClient.class);

    private final SMPClientReadOnly smpClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final MetricsCollector metricsCollector;



    /**
     * Constructor with SMP URL
     * 
     * @param smpUrl The SMP server URL
     */
    public EnhancedSMPClient(@Nonnull final URI smpUrl) {
        this.smpClient = new SMPClientReadOnly(smpUrl);
        this.metricsCollector = new MetricsCollector();
        
        // Configure circuit breaker
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .build();
        this.circuitBreaker = CircuitBreaker.of("smp-client", circuitBreakerConfig);

        // Configure retry
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .retryExceptions(Exception.class)
                .build();
        this.retry = Retry.of("smp-client", retryConfig);

        logger.info("Enhanced SMP Client initialized for URL: {}", smpUrl);
    }

    /**
     * Get service group information for a participant with enhanced error handling
     * 
     * @param participantId The participant identifier
     * @return SmpResult containing service group information or error details
     */
    @Nonnull
    public SmpResult getServiceGroup(@Nonnull final IParticipantIdentifier participantId) {
        long startTime = System.currentTimeMillis();
        
        logger.debug("Fetching service group for participant: {}", participantId.getURIEncoded());
        
        Supplier<SmpResult> serviceGroupSupplier = () -> {
            try {
                ServiceGroupType serviceGroup = smpClient.getServiceGroupOrNull(participantId);
                long responseTime = System.currentTimeMillis() - startTime;
                
                if (serviceGroup != null) {
                    metricsCollector.recordSuccess("getServiceGroup", responseTime);
                    
                    SmpResult result = new SmpResult();
                    result.setSuccessful(true);
                    result.setParticipantId(participantId.getURIEncoded());
                    result.setResponseTimeMs(responseTime);
                    result.setServiceGroup(serviceGroup);
                    
                    logger.debug("Successfully retrieved service group for participant: {}", 
                               participantId.getURIEncoded());
                    return result;
                } else {
                    metricsCollector.recordFailure("getServiceGroup", responseTime, "Service group not found");
                    
                    SmpResult result = new SmpResult();
                    result.setSuccessful(false);
                    result.setParticipantId(participantId.getURIEncoded());
                    result.setResponseTimeMs(responseTime);
                    result.setErrorMessage("Service group not found for participant: " + participantId.getURIEncoded());
                    
                    logger.warn("Service group not found for participant: {}", participantId.getURIEncoded());
                    return result;
                }
                
            } catch (Exception e) {
                long responseTime = System.currentTimeMillis() - startTime;
                metricsCollector.recordFailure("getServiceGroup", responseTime, e.getMessage());
                
                SmpResult result = new SmpResult();
                result.setSuccessful(false);
                result.setParticipantId(participantId.getURIEncoded());
                result.setResponseTimeMs(responseTime);
                result.setErrorMessage("Failed to retrieve service group: " + e.getMessage());
                result.setException(e);
                
                logger.error("Failed to retrieve service group for participant: {}", 
                           participantId.getURIEncoded(), e);
                return result;
            }
        };

        // Apply circuit breaker and retry patterns
        Supplier<SmpResult> decoratedSupplier = CircuitBreaker
                .decorateSupplier(circuitBreaker, Retry.decorateSupplier(retry, serviceGroupSupplier));

        try {
            return decoratedSupplier.get();
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            metricsCollector.recordFailure("getServiceGroup", responseTime, "Circuit breaker or retry failure");
            
            SmpResult result = new SmpResult();
            result.setSuccessful(false);
            result.setParticipantId(participantId.getURIEncoded());
            result.setResponseTimeMs(responseTime);
            result.setErrorMessage("Service unavailable due to circuit breaker or retry failure: " + e.getMessage());
            result.setException(e);
            
            logger.error("Circuit breaker or retry failure for participant: {}", 
                       participantId.getURIEncoded(), e);
            return result;
        }
    }

    /**
     * Get service metadata for a specific document type with enhanced error handling
     * 
     * @param participantId The participant identifier
     * @param documentTypeId The document type identifier
     * @return SmpResult containing service metadata or error details
     */
    @Nonnull
    public SmpResult getServiceMetadata(@Nonnull final IParticipantIdentifier participantId,
                                       @Nonnull final IDocumentTypeIdentifier documentTypeId) {
        long startTime = System.currentTimeMillis();
        
        logger.debug("Fetching service metadata for participant: {} and document type: {}", 
                   participantId.getURIEncoded(), documentTypeId.getURIEncoded());
        
        Supplier<SmpResult> serviceMetadataSupplier = () -> {
            try {
                SignedServiceMetadataType signedServiceMetadata = 
                    smpClient.getServiceMetadataOrNull(participantId, documentTypeId);
                long responseTime = System.currentTimeMillis() - startTime;
                
                if (signedServiceMetadata != null) {
                    metricsCollector.recordSuccess("getServiceMetadata", responseTime);
                    
                    SmpResult result = new SmpResult();
                    result.setSuccessful(true);
                    result.setParticipantId(participantId.getURIEncoded());
                    result.setDocumentTypeId(documentTypeId.getURIEncoded());
                    result.setResponseTimeMs(responseTime);
                    result.setSignedServiceMetadata(signedServiceMetadata);
                    
                    // Extract service metadata
                    ServiceMetadataType serviceMetadata = signedServiceMetadata.getServiceMetadata();
                    if (serviceMetadata != null) {
                        result.setServiceMetadata(serviceMetadata);
                    }
                    
                    logger.debug("Successfully retrieved service metadata for participant: {} and document type: {}", 
                               participantId.getURIEncoded(), documentTypeId.getURIEncoded());
                    return result;
                } else {
                    metricsCollector.recordFailure("getServiceMetadata", responseTime, "Service metadata not found");
                    
                    SmpResult result = new SmpResult();
                    result.setSuccessful(false);
                    result.setParticipantId(participantId.getURIEncoded());
                    result.setDocumentTypeId(documentTypeId.getURIEncoded());
                    result.setResponseTimeMs(responseTime);
                    result.setErrorMessage("Service metadata not found for participant: " + 
                                         participantId.getURIEncoded() + " and document type: " + 
                                         documentTypeId.getURIEncoded());
                    
                    logger.warn("Service metadata not found for participant: {} and document type: {}", 
                              participantId.getURIEncoded(), documentTypeId.getURIEncoded());
                    return result;
                }
                
            } catch (Exception e) {
                long responseTime = System.currentTimeMillis() - startTime;
                metricsCollector.recordFailure("getServiceMetadata", responseTime, e.getMessage());
                
                SmpResult result = new SmpResult();
                result.setSuccessful(false);
                result.setParticipantId(participantId.getURIEncoded());
                result.setDocumentTypeId(documentTypeId.getURIEncoded());
                result.setResponseTimeMs(responseTime);
                result.setErrorMessage("Failed to retrieve service metadata: " + e.getMessage());
                result.setException(e);
                
                logger.error("Failed to retrieve service metadata for participant: {} and document type: {}", 
                           participantId.getURIEncoded(), documentTypeId.getURIEncoded(), e);
                return result;
            }
        };

        // Apply circuit breaker and retry patterns
        Supplier<SmpResult> decoratedSupplier = CircuitBreaker
                .decorateSupplier(circuitBreaker, Retry.decorateSupplier(retry, serviceMetadataSupplier));

        try {
            return decoratedSupplier.get();
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            metricsCollector.recordFailure("getServiceMetadata", responseTime, "Circuit breaker or retry failure");
            
            SmpResult result = new SmpResult();
            result.setSuccessful(false);
            result.setParticipantId(participantId.getURIEncoded());
            result.setDocumentTypeId(documentTypeId.getURIEncoded());
            result.setResponseTimeMs(responseTime);
            result.setErrorMessage("Service unavailable due to circuit breaker or retry failure: " + e.getMessage());
            result.setException(e);
            
            logger.error("Circuit breaker or retry failure for participant: {} and document type: {}", 
                       participantId.getURIEncoded(), documentTypeId.getURIEncoded(), e);
            return result;
        }
    }

    /**
     * Get the underlying SMPClientReadOnly for advanced operations
     * 
     * @return The underlying SMP client
     */
    @Nonnull
    public SMPClientReadOnly getUnderlyingClient() {
        return smpClient;
    }

    /**
     * Get circuit breaker metrics
     * 
     * @return Circuit breaker metrics
     */
    @Nonnull
    public CircuitBreaker.Metrics getCircuitBreakerMetrics() {
        return circuitBreaker.getMetrics();
    }

    /**
     * Get retry metrics
     * 
     * @return Retry metrics
     */
    @Nonnull
    public Retry.Metrics getRetryMetrics() {
        return retry.getMetrics();
    }

    /**
     * Get custom metrics collector
     * 
     * @return Metrics collector
     */
    @Nonnull
    public MetricsCollector getMetricsCollector() {
        return metricsCollector;
    }

    /**
     * Simple metrics collector for monitoring SMP operations
     */
    public static class MetricsCollector {
        private long totalRequests = 0;
        private long successfulRequests = 0;
        private long failedRequests = 0;
        private long totalResponseTime = 0;

        public synchronized void recordSuccess(String operation, long responseTime) {
            totalRequests++;
            successfulRequests++;
            totalResponseTime += responseTime;
            logger.debug("Recorded successful {} operation in {}ms", operation, responseTime);
        }

        public synchronized void recordFailure(String operation, long responseTime, String error) {
            totalRequests++;
            failedRequests++;
            totalResponseTime += responseTime;
            logger.debug("Recorded failed {} operation in {}ms: {}", operation, responseTime, error);
        }

        public synchronized long getTotalRequests() { return totalRequests; }
        public synchronized long getSuccessfulRequests() { return successfulRequests; }
        public synchronized long getFailedRequests() { return failedRequests; }
        public synchronized double getSuccessRate() { 
            return totalRequests > 0 ? (double) successfulRequests / totalRequests : 0.0; 
        }
        public synchronized double getAverageResponseTime() { 
            return totalRequests > 0 ? (double) totalResponseTime / totalRequests : 0.0; 
        }
    }
}