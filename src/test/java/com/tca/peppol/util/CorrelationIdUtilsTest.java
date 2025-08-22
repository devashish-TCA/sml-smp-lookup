package com.tca.peppol.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdUtilsTest {

    @BeforeEach
    void setUp() {
        // Clear MDC before each test
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        // Clear MDC after each test
        MDC.clear();
    }

    @Test
    void testGenerateAndSetCorrelationId() {
        // When
        String correlationId = CorrelationIdUtils.generateAndSetCorrelationId();

        // Then
        assertNotNull(correlationId);
        assertFalse(correlationId.trim().isEmpty());
        assertEquals(correlationId, CorrelationIdUtils.getCorrelationId());
        assertEquals(correlationId, MDC.get(CorrelationIdUtils.CORRELATION_ID_KEY));
    }

    @Test
    void testGenerateCorrelationId() {
        // When
        String correlationId1 = CorrelationIdUtils.generateCorrelationId();
        String correlationId2 = CorrelationIdUtils.generateCorrelationId();

        // Then
        assertNotNull(correlationId1);
        assertNotNull(correlationId2);
        assertNotEquals(correlationId1, correlationId2);
        assertNull(CorrelationIdUtils.getCorrelationId()); // Should not be set in MDC
    }

    @Test
    void testSetAndGetCorrelationId() {
        // Given
        String testCorrelationId = "test-correlation-id-123";

        // When
        CorrelationIdUtils.setCorrelationId(testCorrelationId);

        // Then
        assertEquals(testCorrelationId, CorrelationIdUtils.getCorrelationId());
        assertEquals(testCorrelationId, MDC.get(CorrelationIdUtils.CORRELATION_ID_KEY));
    }

    @Test
    void testSetCorrelationIdWithNullValue() {
        // Given
        CorrelationIdUtils.setCorrelationId("initial-value");

        // When
        CorrelationIdUtils.setCorrelationId(null);

        // Then
        assertEquals("initial-value", CorrelationIdUtils.getCorrelationId()); // Should remain unchanged
    }

    @Test
    void testSetCorrelationIdWithEmptyValue() {
        // Given
        CorrelationIdUtils.setCorrelationId("initial-value");

        // When
        CorrelationIdUtils.setCorrelationId("");

        // Then
        assertEquals("initial-value", CorrelationIdUtils.getCorrelationId()); // Should remain unchanged
    }

    @Test
    void testSetAndGetRequestId() {
        // Given
        String testRequestId = "test-request-id-456";

        // When
        CorrelationIdUtils.setRequestId(testRequestId);

        // Then
        assertEquals(testRequestId, CorrelationIdUtils.getRequestId());
        assertEquals(testRequestId, MDC.get(CorrelationIdUtils.REQUEST_ID_KEY));
    }

    @Test
    void testSetAndGetLambdaRequestId() {
        // Given
        String testLambdaRequestId = "test-lambda-request-id-789";

        // When
        CorrelationIdUtils.setLambdaRequestId(testLambdaRequestId);

        // Then
        assertEquals(testLambdaRequestId, CorrelationIdUtils.getLambdaRequestId());
        assertEquals(testLambdaRequestId, MDC.get(CorrelationIdUtils.LAMBDA_REQUEST_ID_KEY));
    }

    @Test
    void testExecuteWithCorrelationIdSupplier() {
        // Given
        String testCorrelationId = "test-correlation-id-execute";
        String initialCorrelationId = "initial-correlation-id";
        CorrelationIdUtils.setCorrelationId(initialCorrelationId);

        // When
        String result = CorrelationIdUtils.executeWithCorrelationId(testCorrelationId, () -> {
            assertEquals(testCorrelationId, CorrelationIdUtils.getCorrelationId());
            return "test-result";
        });

        // Then
        assertEquals("test-result", result);
        assertEquals(initialCorrelationId, CorrelationIdUtils.getCorrelationId()); // Should be restored
    }

    @Test
    void testExecuteWithCorrelationIdRunnable() {
        // Given
        String testCorrelationId = "test-correlation-id-runnable";
        String initialCorrelationId = "initial-correlation-id";
        CorrelationIdUtils.setCorrelationId(initialCorrelationId);
        AtomicReference<String> capturedCorrelationId = new AtomicReference<>();

        // When
        CorrelationIdUtils.executeWithCorrelationId(testCorrelationId, () -> {
            capturedCorrelationId.set(CorrelationIdUtils.getCorrelationId());
        });

        // Then
        assertEquals(testCorrelationId, capturedCorrelationId.get());
        assertEquals(initialCorrelationId, CorrelationIdUtils.getCorrelationId()); // Should be restored
    }

    @Test
    void testExecuteWithCorrelationIdWithoutInitialValue() {
        // Given
        String testCorrelationId = "test-correlation-id-no-initial";
        assertNull(CorrelationIdUtils.getCorrelationId()); // No initial value

        // When
        String result = CorrelationIdUtils.executeWithCorrelationId(testCorrelationId, () -> {
            assertEquals(testCorrelationId, CorrelationIdUtils.getCorrelationId());
            return "test-result";
        });

        // Then
        assertEquals("test-result", result);
        assertNull(CorrelationIdUtils.getCorrelationId()); // Should be cleared
    }

    @Test
    void testExecuteWithCorrelationIdWithException() {
        // Given
        String testCorrelationId = "test-correlation-id-exception";
        String initialCorrelationId = "initial-correlation-id";
        CorrelationIdUtils.setCorrelationId(initialCorrelationId);

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            CorrelationIdUtils.executeWithCorrelationId(testCorrelationId, () -> {
                assertEquals(testCorrelationId, CorrelationIdUtils.getCorrelationId());
                throw new RuntimeException("Test exception");
            });
        });

        assertEquals("Test exception", exception.getMessage());
        assertEquals(initialCorrelationId, CorrelationIdUtils.getCorrelationId()); // Should be restored even after exception
    }

    @Test
    void testClearCorrelationId() {
        // Given
        CorrelationIdUtils.setCorrelationId("test-correlation-id");

        // When
        CorrelationIdUtils.clearCorrelationId();

        // Then
        assertNull(CorrelationIdUtils.getCorrelationId());
        assertNull(MDC.get(CorrelationIdUtils.CORRELATION_ID_KEY));
    }

    @Test
    void testClearRequestId() {
        // Given
        CorrelationIdUtils.setRequestId("test-request-id");

        // When
        CorrelationIdUtils.clearRequestId();

        // Then
        assertNull(CorrelationIdUtils.getRequestId());
        assertNull(MDC.get(CorrelationIdUtils.REQUEST_ID_KEY));
    }

    @Test
    void testClearLambdaRequestId() {
        // Given
        CorrelationIdUtils.setLambdaRequestId("test-lambda-request-id");

        // When
        CorrelationIdUtils.clearLambdaRequestId();

        // Then
        assertNull(CorrelationIdUtils.getLambdaRequestId());
        assertNull(MDC.get(CorrelationIdUtils.LAMBDA_REQUEST_ID_KEY));
    }

    @Test
    void testClearAll() {
        // Given
        CorrelationIdUtils.setCorrelationId("test-correlation-id");
        CorrelationIdUtils.setRequestId("test-request-id");
        CorrelationIdUtils.setLambdaRequestId("test-lambda-request-id");

        // When
        CorrelationIdUtils.clearAll();

        // Then
        assertNull(CorrelationIdUtils.getCorrelationId());
        assertNull(CorrelationIdUtils.getRequestId());
        assertNull(CorrelationIdUtils.getLambdaRequestId());
        assertNull(MDC.get(CorrelationIdUtils.CORRELATION_ID_KEY));
        assertNull(MDC.get(CorrelationIdUtils.REQUEST_ID_KEY));
        assertNull(MDC.get(CorrelationIdUtils.LAMBDA_REQUEST_ID_KEY));
    }

    @Test
    void testThreadSafety() throws ExecutionException, InterruptedException {
        // Given
        String correlationId1 = "correlation-id-1";
        String correlationId2 = "correlation-id-2";

        // When - Execute in parallel threads
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> 
            CorrelationIdUtils.executeWithCorrelationId(correlationId1, () -> {
                try {
                    Thread.sleep(100); // Simulate some work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return CorrelationIdUtils.getCorrelationId();
            })
        );

        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> 
            CorrelationIdUtils.executeWithCorrelationId(correlationId2, () -> {
                try {
                    Thread.sleep(100); // Simulate some work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return CorrelationIdUtils.getCorrelationId();
            })
        );

        // Then
        assertEquals(correlationId1, future1.get());
        assertEquals(correlationId2, future2.get());
    }

    @Test
    void testCorrelationIdFormat() {
        // When
        String correlationId = CorrelationIdUtils.generateCorrelationId();

        // Then
        assertNotNull(correlationId);
        assertTrue(correlationId.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"),
                "Correlation ID should be a valid UUID format");
    }
}