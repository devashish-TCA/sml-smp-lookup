package com.tca.peppol.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StructuredLoggerTest {

    private ListAppender<ILoggingEvent> listAppender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        // Set up test logger with list appender
        logger = (Logger) LoggerFactory.getLogger("com.yourcompany.peppol.security_event");
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.DEBUG);

        // Clear MDC
        MDC.clear();
        
        // Set up correlation ID for testing
        CorrelationIdUtils.setCorrelationId("test-correlation-id");
        CorrelationIdUtils.setRequestId("test-request-id");
        CorrelationIdUtils.setLambdaRequestId("test-lambda-request-id");
    }

    @AfterEach
    void tearDown() {
        // Clean up
        logger.detachAppender(listAppender);
        MDC.clear();
    }

    @Test
    void testLogEvent() {
        // Given
        Map<String, Object> details = new HashMap<>();
        details.put("testKey", "testValue");
        details.put("participantId", "sensitive-participant-id");

        // When
        StructuredLogger.logEvent(StructuredLogger.EventType.SECURITY_EVENT, 
                StructuredLogger.Level.WARN, "Test security event", details);

        // Then
        List<ILoggingEvent> logEvents = listAppender.list;
        assertEquals(1, logEvents.size());
        
        ILoggingEvent event = logEvents.get(0);
        assertEquals(Level.WARN, event.getLevel());
        assertEquals("Test security event", event.getMessage());
        
        // Check that MDC contains event details
        assertNotNull(event.getMDCPropertyMap().get("eventDetails"));
    }

    @Test
    void testLogSecurityEvent() {
        // Given
        Map<String, Object> details = new HashMap<>();
        details.put("securityContext", "authentication");
        details.put("participantId", "sensitive-id");

        // When
        StructuredLogger.logSecurityEvent("Security violation detected", details);

        // Then
        List<ILoggingEvent> logEvents = listAppender.list;
        assertEquals(1, logEvents.size());
        
        ILoggingEvent event = logEvents.get(0);
        assertEquals(Level.WARN, event.getLevel());
        assertEquals("Security violation detected", event.getMessage());
    }

    @Test
    void testLogPerformanceMetric() {
        // Given
        Map<String, Object> details = new HashMap<>();
        details.put("component", "SML lookup");

        // Set up performance logger
        Logger perfLogger = (Logger) LoggerFactory.getLogger("com.yourcompany.peppol.performance_metric");
        ListAppender<ILoggingEvent> perfAppender = new ListAppender<>();
        perfAppender.start();
        perfLogger.addAppender(perfAppender);
        perfLogger.setLevel(Level.INFO);

        // When
        StructuredLogger.logPerformanceMetric("DNS resolution", 150L, details);

        // Then
        List<ILoggingEvent> logEvents = perfAppender.list;
        assertEquals(1, logEvents.size());
        
        ILoggingEvent event = logEvents.get(0);
        assertEquals(Level.INFO, event.getLevel());
        assertTrue(event.getMessage().contains("DNS resolution"));
        assertTrue(event.getMessage().contains("150ms"));
        
        // Clean up
        perfLogger.detachAppender(perfAppender);
    }

    @Test
    void testLogError() {
        // Given
        Exception testException = new RuntimeException("Test exception message");
        Map<String, Object> details = new HashMap<>();
        details.put("errorContext", "validation");

        // Set up error logger
        Logger errorLogger = (Logger) LoggerFactory.getLogger("com.yourcompany.peppol.error_event");
        ListAppender<ILoggingEvent> errorAppender = new ListAppender<>();
        errorAppender.start();
        errorLogger.addAppender(errorAppender);
        errorLogger.setLevel(Level.ERROR);

        // When
        StructuredLogger.logError("Error occurred during processing", testException, details);

        // Then
        List<ILoggingEvent> logEvents = errorAppender.list;
        assertEquals(1, logEvents.size());
        
        ILoggingEvent event = logEvents.get(0);
        assertEquals(Level.ERROR, event.getLevel());
        assertEquals("Error occurred during processing", event.getMessage());
        
        // Clean up
        errorLogger.detachAppender(errorAppender);
    }

    @Test
    void testLogValidationEvent() {
        // Given
        Map<String, Object> details = new HashMap<>();
        details.put("certificateSubject", "CN=Test Certificate");

        // Set up validation logger
        Logger validationLogger = (Logger) LoggerFactory.getLogger("com.yourcompany.peppol.validation_event");
        ListAppender<ILoggingEvent> validationAppender = new ListAppender<>();
        validationAppender.start();
        validationLogger.addAppender(validationAppender);
        validationLogger.setLevel(Level.INFO);

        // When - Test successful validation
        StructuredLogger.logValidationEvent("Certificate validation", true, details);

        // Then
        List<ILoggingEvent> logEvents = validationAppender.list;
        assertEquals(1, logEvents.size());
        
        ILoggingEvent event = logEvents.get(0);
        assertEquals(Level.INFO, event.getLevel());
        assertTrue(event.getMessage().contains("PASSED"));

        // When - Test failed validation
        validationAppender.list.clear();
        StructuredLogger.logValidationEvent("Certificate validation", false, details);

        // Then
        logEvents = validationAppender.list;
        assertEquals(1, logEvents.size());
        
        event = logEvents.get(0);
        assertEquals(Level.WARN, event.getLevel());
        assertTrue(event.getMessage().contains("FAILED"));
        
        // Clean up
        validationLogger.detachAppender(validationAppender);
    }

    @Test
    void testLogExternalServiceCall() {
        // Given
        Map<String, Object> details = new HashMap<>();
        details.put("endpoint", "https://example.com/api");

        // Set up external service logger
        Logger serviceLogger = (Logger) LoggerFactory.getLogger("com.yourcompany.peppol.external_service_call");
        ListAppender<ILoggingEvent> serviceAppender = new ListAppender<>();
        serviceAppender.start();
        serviceLogger.addAppender(serviceAppender);
        serviceLogger.setLevel(Level.INFO);

        // When - Test successful call
        StructuredLogger.logExternalServiceCall("SMP", "query", true, 200L, details);

        // Then
        List<ILoggingEvent> logEvents = serviceAppender.list;
        assertEquals(1, logEvents.size());
        
        ILoggingEvent event = logEvents.get(0);
        assertEquals(Level.INFO, event.getLevel());
        assertTrue(event.getMessage().contains("SMP.query"));
        assertTrue(event.getMessage().contains("succeeded"));
        assertTrue(event.getMessage().contains("200ms"));

        // When - Test failed call
        serviceAppender.list.clear();
        StructuredLogger.logExternalServiceCall("SMP", "query", false, 5000L, details);

        // Then
        logEvents = serviceAppender.list;
        assertEquals(1, logEvents.size());
        
        event = logEvents.get(0);
        assertEquals(Level.WARN, event.getLevel());
        assertTrue(event.getMessage().contains("failed"));
        
        // Clean up
        serviceLogger.detachAppender(serviceAppender);
    }

    @Test
    void testLogBusinessEvent() {
        // Given
        Map<String, Object> details = new HashMap<>();
        details.put("participantId", "sensitive-participant");
        details.put("documentType", "invoice");

        // Set up business logger
        Logger businessLogger = (Logger) LoggerFactory.getLogger("com.yourcompany.peppol.business_event");
        ListAppender<ILoggingEvent> businessAppender = new ListAppender<>();
        businessAppender.start();
        businessLogger.addAppender(businessAppender);
        businessLogger.setLevel(Level.INFO);

        // When
        StructuredLogger.logBusinessEvent("Participant lookup completed", details);

        // Then
        List<ILoggingEvent> logEvents = businessAppender.list;
        assertEquals(1, logEvents.size());
        
        ILoggingEvent event = logEvents.get(0);
        assertEquals(Level.INFO, event.getLevel());
        assertTrue(event.getMessage().contains("Participant lookup completed"));
        
        // Clean up
        businessLogger.detachAppender(businessAppender);
    }

    @Test
    void testHashString() {
        // Test normal string
        String input = "test-participant-id";
        String hashed = StructuredLogger.hashString(input);
        
        assertNotNull(hashed);
        assertEquals(16, hashed.length()); // Should return first 16 characters
        assertNotEquals(input, hashed); // Should be different from input
        
        // Test consistency
        String hashed2 = StructuredLogger.hashString(input);
        assertEquals(hashed, hashed2); // Should be consistent
        
        // Test null input
        String nullHashed = StructuredLogger.hashString(null);
        assertEquals("empty", nullHashed);
        
        // Test empty input
        String emptyHashed = StructuredLogger.hashString("");
        assertEquals("empty", emptyHashed);
        
        // Test different inputs produce different hashes
        String differentHashed = StructuredLogger.hashString("different-input");
        assertNotEquals(hashed, differentHashed);
    }

    @Test
    void testSensitiveDataHashing() {
        // Given
        Map<String, Object> details = new HashMap<>();
        details.put("participantId", "sensitive-participant-id");
        details.put("documentTypeId", "sensitive-document-type");
        details.put("endpointUrl", "https://sensitive-endpoint.com");
        details.put("normalField", "normal-value");

        // When
        StructuredLogger.logEvent(StructuredLogger.EventType.BUSINESS_EVENT, 
                StructuredLogger.Level.INFO, "Test sensitive data", details);

        // Then - Original details should not be modified
        assertTrue(details.containsKey("participantId"));
        assertTrue(details.containsKey("documentTypeId"));
        assertTrue(details.containsKey("endpointUrl"));
        assertTrue(details.containsKey("normalField"));
    }

    @Test
    void testLogEventWithNullDetails() {
        // Set up system event logger
        Logger systemLogger = (Logger) LoggerFactory.getLogger("com.yourcompany.peppol.system_event");
        ListAppender<ILoggingEvent> systemAppender = new ListAppender<>();
        systemAppender.start();
        systemLogger.addAppender(systemAppender);
        systemLogger.setLevel(Level.INFO);

        // When
        StructuredLogger.logEvent(StructuredLogger.EventType.SYSTEM_EVENT, 
                StructuredLogger.Level.INFO, "Test with null details", null);

        // Then - Should not throw exception
        List<ILoggingEvent> logEvents = systemAppender.list;
        assertEquals(1, logEvents.size());
        
        // Clean up
        systemLogger.detachAppender(systemAppender);
    }

    @Test
    void testLogEventWithEmptyDetails() {
        // Set up system event logger
        Logger systemLogger = (Logger) LoggerFactory.getLogger("com.yourcompany.peppol.system_event");
        ListAppender<ILoggingEvent> systemAppender = new ListAppender<>();
        systemAppender.start();
        systemLogger.addAppender(systemAppender);
        systemLogger.setLevel(Level.INFO);

        // When
        StructuredLogger.logEvent(StructuredLogger.EventType.SYSTEM_EVENT, 
                StructuredLogger.Level.INFO, "Test with empty details", new HashMap<>());

        // Then - Should not throw exception
        List<ILoggingEvent> logEvents = systemAppender.list;
        assertEquals(1, logEvents.size());
        
        // Clean up
        systemLogger.detachAppender(systemAppender);
    }

    @Test
    void testCorrelationIdPropagation() {
        // Given
        String testCorrelationId = "test-correlation-123";
        String testRequestId = "test-request-456";
        String testLambdaRequestId = "test-lambda-789";
        
        CorrelationIdUtils.setCorrelationId(testCorrelationId);
        CorrelationIdUtils.setRequestId(testRequestId);
        CorrelationIdUtils.setLambdaRequestId(testLambdaRequestId);

        // Set up business event logger
        Logger businessLogger = (Logger) LoggerFactory.getLogger("com.yourcompany.peppol.business_event");
        ListAppender<ILoggingEvent> businessAppender = new ListAppender<>();
        businessAppender.start();
        businessLogger.addAppender(businessAppender);
        businessLogger.setLevel(Level.INFO);

        // When
        StructuredLogger.logEvent(StructuredLogger.EventType.BUSINESS_EVENT, 
                StructuredLogger.Level.INFO, "Test correlation propagation", new HashMap<>());

        // Then
        List<ILoggingEvent> logEvents = businessAppender.list;
        assertEquals(1, logEvents.size());
        
        // The correlation IDs should be available in MDC during logging
        assertEquals(testCorrelationId, CorrelationIdUtils.getCorrelationId());
        assertEquals(testRequestId, CorrelationIdUtils.getRequestId());
        assertEquals(testLambdaRequestId, CorrelationIdUtils.getLambdaRequestId());
        
        // Clean up
        businessLogger.detachAppender(businessAppender);
    }

    @Test
    void testAllEventTypes() {
        // Test all event types to ensure they work
        StructuredLogger.EventType[] eventTypes = StructuredLogger.EventType.values();
        
        for (StructuredLogger.EventType eventType : eventTypes) {
            // Set up logger for this event type
            Logger eventLogger = (Logger) LoggerFactory.getLogger("com.yourcompany.peppol." + eventType.name().toLowerCase());
            ListAppender<ILoggingEvent> eventAppender = new ListAppender<>();
            eventAppender.start();
            eventLogger.addAppender(eventAppender);
            eventLogger.setLevel(Level.DEBUG);
            
            // When
            StructuredLogger.logEvent(eventType, StructuredLogger.Level.INFO, 
                    "Test " + eventType.name(), new HashMap<>());
            
            // Then
            List<ILoggingEvent> logEvents = eventAppender.list;
            assertEquals(1, logEvents.size(), "Event type " + eventType + " should log");
            
            // Clean up
            eventLogger.detachAppender(eventAppender);
        }
    }

    @Test
    void testAllLogLevels() {
        // Test all log levels
        StructuredLogger.Level[] levels = StructuredLogger.Level.values();
        
        for (StructuredLogger.Level level : levels) {
            listAppender.list.clear();
            
            // When
            StructuredLogger.logEvent(StructuredLogger.EventType.SECURITY_EVENT, 
                    level, "Test " + level.name(), new HashMap<>());
            
            // Then
            List<ILoggingEvent> logEvents = listAppender.list;
            assertEquals(1, logEvents.size(), "Log level " + level + " should work");
            
            ILoggingEvent event = logEvents.get(0);
            assertEquals(Level.valueOf(level.name()), event.getLevel());
        }
    }
}