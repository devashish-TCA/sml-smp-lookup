package com.tca.peppol.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Structured logging utility that provides privacy protection and consistent logging format.
 * Automatically hashes sensitive data like participant IDs to protect privacy while maintaining
 * traceability for debugging purposes.
 */
public final class StructuredLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(StructuredLogger.class);
    
    // Event types for structured logging
    public enum EventType {
        SECURITY_EVENT,
        PERFORMANCE_METRIC,
        ERROR_EVENT,
        BUSINESS_EVENT,
        SYSTEM_EVENT,
        VALIDATION_EVENT,
        EXTERNAL_SERVICE_CALL,
        CACHE_EVENT
    }
    
    // Log levels
    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }
    
    private StructuredLogger() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Logs a structured event with privacy protection.
     * 
     * @param eventType the type of event
     * @param level the log level
     * @param message the log message
     * @param details additional details to include
     */
    public static void logEvent(EventType eventType, Level level, String message, Map<String, Object> details) {
        try {
            // Create a copy of details to avoid modifying the original
            Map<String, Object> logDetails = new HashMap<>(details != null ? details : new HashMap<>());
            
            // Add standard fields
            logDetails.put("eventType", eventType.name());
            logDetails.put("timestamp", Instant.now().toString());
            logDetails.put("correlationId", CorrelationIdUtils.getCorrelationId());
            logDetails.put("requestId", CorrelationIdUtils.getRequestId());
            logDetails.put("lambdaRequestId", CorrelationIdUtils.getLambdaRequestId());
            
            // Hash sensitive data
            hashSensitiveData(logDetails);
            
            // Set details in MDC for JSON logging
            String originalMdc = MDC.get("eventDetails");
            try {
                MDC.put("eventDetails", logDetails.toString());
                
                Logger targetLogger = LoggerFactory.getLogger("com.yourcompany.peppol." + eventType.name().toLowerCase());
                
                switch (level) {
                    case DEBUG:
                        targetLogger.debug(message);
                        break;
                    case INFO:
                        targetLogger.info(message);
                        break;
                    case WARN:
                        targetLogger.warn(message);
                        break;
                    case ERROR:
                        targetLogger.error(message);
                        break;
                }
            } finally {
                if (originalMdc != null) {
                    MDC.put("eventDetails", originalMdc);
                } else {
                    MDC.remove("eventDetails");
                }
            }
        } catch (Exception e) {
            // Fallback logging if structured logging fails
            logger.error("Failed to log structured event: {}", message, e);
        }
    }
    
    /**
     * Logs a security event.
     * 
     * @param message the security event message
     * @param details additional security context
     */
    public static void logSecurityEvent(String message, Map<String, Object> details) {
        logEvent(EventType.SECURITY_EVENT, Level.WARN, message, details);
    }
    
    /**
     * Logs a performance metric.
     * 
     * @param operation the operation being measured
     * @param durationMs the duration in milliseconds
     * @param details additional performance context
     */
    public static void logPerformanceMetric(String operation, long durationMs, Map<String, Object> details) {
        Map<String, Object> perfDetails = new HashMap<>(details != null ? details : new HashMap<>());
        perfDetails.put("operation", operation);
        perfDetails.put("durationMs", durationMs);
        perfDetails.put("performanceCategory", "timing");
        
        logEvent(EventType.PERFORMANCE_METRIC, Level.INFO, 
                String.format("Performance: %s completed in %dms", operation, durationMs), 
                perfDetails);
    }
    
    /**
     * Logs an error event with structured context.
     * 
     * @param message the error message
     * @param throwable the exception (optional)
     * @param details additional error context
     */
    public static void logError(String message, Throwable throwable, Map<String, Object> details) {
        Map<String, Object> errorDetails = new HashMap<>(details != null ? details : new HashMap<>());
        
        if (throwable != null) {
            errorDetails.put("exceptionClass", throwable.getClass().getSimpleName());
            errorDetails.put("exceptionMessage", throwable.getMessage());
            errorDetails.put("stackTrace", getStackTraceString(throwable));
        }
        
        logEvent(EventType.ERROR_EVENT, Level.ERROR, message, errorDetails);
    }
    
    /**
     * Logs a validation event.
     * 
     * @param validationType the type of validation
     * @param success whether validation succeeded
     * @param details validation details
     */
    public static void logValidationEvent(String validationType, boolean success, Map<String, Object> details) {
        Map<String, Object> validationDetails = new HashMap<>(details != null ? details : new HashMap<>());
        validationDetails.put("validationType", validationType);
        validationDetails.put("validationSuccess", success);
        
        Level level = success ? Level.INFO : Level.WARN;
        String message = String.format("Validation %s: %s", validationType, success ? "PASSED" : "FAILED");
        
        logEvent(EventType.VALIDATION_EVENT, level, message, validationDetails);
    }
    
    /**
     * Logs an external service call.
     * 
     * @param serviceName the name of the external service
     * @param operation the operation performed
     * @param success whether the call succeeded
     * @param durationMs the duration in milliseconds
     * @param details additional service call context
     */
    public static void logExternalServiceCall(String serviceName, String operation, boolean success, 
                                            long durationMs, Map<String, Object> details) {
        Map<String, Object> serviceDetails = new HashMap<>(details != null ? details : new HashMap<>());
        serviceDetails.put("serviceName", serviceName);
        serviceDetails.put("operation", operation);
        serviceDetails.put("success", success);
        serviceDetails.put("durationMs", durationMs);
        
        Level level = success ? Level.INFO : Level.WARN;
        String message = String.format("External service call: %s.%s %s in %dms", 
                serviceName, operation, success ? "succeeded" : "failed", durationMs);
        
        logEvent(EventType.EXTERNAL_SERVICE_CALL, level, message, serviceDetails);
    }
    
    /**
     * Logs a business event.
     * 
     * @param eventName the business event name
     * @param details business event details
     */
    public static void logBusinessEvent(String eventName, Map<String, Object> details) {
        Map<String, Object> businessDetails = new HashMap<>(details != null ? details : new HashMap<>());
        businessDetails.put("businessEvent", eventName);
        
        logEvent(EventType.BUSINESS_EVENT, Level.INFO, 
                String.format("Business event: %s", eventName), businessDetails);
    }
    
    /**
     * Hashes sensitive data in the details map to protect privacy.
     * 
     * @param details the details map to process
     */
    private static void hashSensitiveData(Map<String, Object> details) {
        // List of sensitive field names that should be hashed
        String[] sensitiveFields = {
            "participantId", "participantIdentifier", "participant_id",
            "documentTypeId", "document_type_id", "processId", "process_id",
            "endpointUrl", "endpoint_url", "certificateSubject", "certificate_subject"
        };
        
        for (String field : sensitiveFields) {
            Object value = details.get(field);
            if (value != null && value instanceof String) {
                String hashedValue = hashString((String) value);
                details.put(field + "_hash", hashedValue);
                details.put(field + "_original_length", ((String) value).length());
                details.remove(field); // Remove original sensitive data
            }
        }
    }
    
    /**
     * Hashes a string using SHA-256 for privacy protection.
     * 
     * @param input the string to hash
     * @return the hashed string in hexadecimal format
     */
    public static String hashString(String input) {
        if (input == null || input.isEmpty()) {
            return "empty";
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            // Return first 16 characters for readability while maintaining uniqueness
            return hexString.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash if SHA-256 is not available
            return String.valueOf(input.hashCode());
        }
    }
    
    /**
     * Converts a throwable's stack trace to a string.
     * 
     * @param throwable the throwable
     * @return the stack trace as a string
     */
    private static String getStackTraceString(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getName()).append(": ").append(throwable.getMessage());
        
        StackTraceElement[] elements = throwable.getStackTrace();
        for (int i = 0; i < Math.min(elements.length, 10); i++) { // Limit to first 10 stack frames
            sb.append("\n\tat ").append(elements[i].toString());
        }
        
        if (elements.length > 10) {
            sb.append("\n\t... ").append(elements.length - 10).append(" more");
        }
        
        return sb.toString();
    }
}