package com.tca.peppol.util;

import org.slf4j.MDC;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Utility class for managing correlation IDs throughout the request lifecycle.
 * Provides thread-safe correlation ID generation, propagation, and cleanup.
 */
public final class CorrelationIdUtils {
    
    public static final String CORRELATION_ID_KEY = "correlationId";
    public static final String REQUEST_ID_KEY = "requestId";
    public static final String LAMBDA_REQUEST_ID_KEY = "lambdaRequestId";
    
    private CorrelationIdUtils() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Generates a new correlation ID and sets it in the MDC.
     * 
     * @return the generated correlation ID
     */
    public static String generateAndSetCorrelationId() {
        String correlationId = generateCorrelationId();
        setCorrelationId(correlationId);
        return correlationId;
    }
    
    /**
     * Generates a new correlation ID without setting it in MDC.
     * 
     * @return a new correlation ID
     */
    public static String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Sets the correlation ID in the MDC.
     * 
     * @param correlationId the correlation ID to set
     */
    public static void setCorrelationId(String correlationId) {
        if (correlationId != null && !correlationId.trim().isEmpty()) {
            MDC.put(CORRELATION_ID_KEY, correlationId);
        }
    }
    
    /**
     * Gets the current correlation ID from MDC.
     * 
     * @return the current correlation ID, or null if not set
     */
    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID_KEY);
    }
    
    /**
     * Sets the request ID in the MDC.
     * 
     * @param requestId the request ID to set
     */
    public static void setRequestId(String requestId) {
        if (requestId != null && !requestId.trim().isEmpty()) {
            MDC.put(REQUEST_ID_KEY, requestId);
        }
    }
    
    /**
     * Gets the current request ID from MDC.
     * 
     * @return the current request ID, or null if not set
     */
    public static String getRequestId() {
        return MDC.get(REQUEST_ID_KEY);
    }
    
    /**
     * Sets the Lambda request ID in the MDC.
     * 
     * @param lambdaRequestId the Lambda request ID to set
     */
    public static void setLambdaRequestId(String lambdaRequestId) {
        if (lambdaRequestId != null && !lambdaRequestId.trim().isEmpty()) {
            MDC.put(LAMBDA_REQUEST_ID_KEY, lambdaRequestId);
        }
    }
    
    /**
     * Gets the current Lambda request ID from MDC.
     * 
     * @return the current Lambda request ID, or null if not set
     */
    public static String getLambdaRequestId() {
        return MDC.get(LAMBDA_REQUEST_ID_KEY);
    }
    
    /**
     * Executes a supplier with a specific correlation ID, ensuring cleanup.
     * 
     * @param correlationId the correlation ID to use
     * @param supplier the supplier to execute
     * @param <T> the return type
     * @return the result of the supplier
     */
    public static <T> T executeWithCorrelationId(String correlationId, Supplier<T> supplier) {
        String previousCorrelationId = getCorrelationId();
        try {
            setCorrelationId(correlationId);
            return supplier.get();
        } finally {
            if (previousCorrelationId != null) {
                setCorrelationId(previousCorrelationId);
            } else {
                clearCorrelationId();
            }
        }
    }
    
    /**
     * Executes a runnable with a specific correlation ID, ensuring cleanup.
     * 
     * @param correlationId the correlation ID to use
     * @param runnable the runnable to execute
     */
    public static void executeWithCorrelationId(String correlationId, Runnable runnable) {
        String previousCorrelationId = getCorrelationId();
        try {
            setCorrelationId(correlationId);
            runnable.run();
        } finally {
            if (previousCorrelationId != null) {
                setCorrelationId(previousCorrelationId);
            } else {
                clearCorrelationId();
            }
        }
    }
    
    /**
     * Clears the correlation ID from MDC.
     */
    public static void clearCorrelationId() {
        MDC.remove(CORRELATION_ID_KEY);
    }
    
    /**
     * Clears the request ID from MDC.
     */
    public static void clearRequestId() {
        MDC.remove(REQUEST_ID_KEY);
    }
    
    /**
     * Clears the Lambda request ID from MDC.
     */
    public static void clearLambdaRequestId() {
        MDC.remove(LAMBDA_REQUEST_ID_KEY);
    }
    
    /**
     * Clears all correlation-related data from MDC.
     */
    public static void clearAll() {
        clearCorrelationId();
        clearRequestId();
        clearLambdaRequestId();
    }
}