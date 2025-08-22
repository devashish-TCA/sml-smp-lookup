package com.tca.peppol.integration;

import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Performance benchmark tests comparing peppol-commons vs custom implementations.
 * 
 * Requirements: 16.1, 16.5
 */
@DisplayName("Peppol-Commons Performance Benchmark Tests")
class PeppolCommonsPerformanceBenchmarkTest {

    private static final Logger logger = LoggerFactory.getLogger(PeppolCommonsPerformanceBenchmarkTest.class);

    private static final String TEST_PARTICIPANT_ID = "0088:7300010000001";
    private static final String TEST_DOCUMENT_TYPE_ID = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1";
    private static final String TEST_PROCESS_ID = "urn:fdc:peppol.eu:2017:poacc:billing:01:1.0";

    @Test
    @DisplayName("Benchmark identifier creation: peppol-commons vs custom")
    void benchmarkIdentifierCreation() {
        logger.info("Benchmarking identifier creation: peppol-commons vs custom implementation");

        int iterations = 10000;

        // Benchmark peppol-commons implementation
        Instant start = Instant.now();
        for (int i = 0; i < iterations; i++) {
            IParticipantIdentifier participantId = EPredefinedIdentifierIssuingAgency.GLN
                .createParticipantIdentifier("5798000000001");
            IDocumentTypeIdentifier documentTypeId = EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30
                .getAsDocumentTypeIdentifier();
            IProcessIdentifier processId = EPredefinedProcessIdentifier.BIS3_BILLING
                .getAsProcessIdentifier();
        }
        Duration peppolCommonsDuration = Duration.between(start, Instant.now());

        // Benchmark custom implementation (simple parsing)
        start = Instant.now();
        for (int i = 0; i < iterations; i++) {
            String[] participantParts = TEST_PARTICIPANT_ID.split(":", 2);
            String participantScheme = participantParts[0];
            String participantValue = participantParts[1];
            
            // Simple document type parsing (no validation)
            String documentTypeValue = TEST_DOCUMENT_TYPE_ID;
            
            // Simple process parsing (no validation)
            String processValue = TEST_PROCESS_ID;
        }
        Duration customDuration = Duration.between(start, Instant.now());

        // Log results
        logger.info("Performance benchmark results for {} iterations:", iterations);
        logger.info("Peppol-commons: {} ms (avg: {} μs per operation)", 
            peppolCommonsDuration.toMillis(), peppolCommonsDuration.toNanos() / iterations / 1000);
        logger.info("Custom implementation: {} ms (avg: {} μs per operation)", 
            customDuration.toMillis(), customDuration.toNanos() / iterations / 1000);
        
        double overheadFactor = (double) peppolCommonsDuration.toMillis() / customDuration.toMillis();
        logger.info("Peppol-commons overhead factor: {}x", String.format("%.2f", overheadFactor));

        // Assertions
        assertThat(peppolCommonsDuration.toMillis()).isLessThan(5000); // Should be reasonable
        assertThat(overheadFactor).isLessThan(50); // Overhead should not be excessive
    }

    @Test
    @DisplayName("Benchmark concurrent identifier creation")
    void benchmarkConcurrentIdentifierCreation() {
        logger.info("Benchmarking concurrent identifier creation with peppol-commons");

        int threads = 10;
        int iterationsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        Instant start = Instant.now();
        
        CompletableFuture<Void>[] futures = IntStream.range(0, threads)
            .mapToObj(threadIndex -> CompletableFuture.runAsync(() -> {
                for (int i = 0; i < iterationsPerThread; i++) {
                    IParticipantIdentifier participantId = EPredefinedIdentifierIssuingAgency.GLN
                        .createParticipantIdentifier("5798000000001");
                    assertThat(participantId).isNotNull();
                }
            }, executor))
            .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).join();
        Duration duration = Duration.between(start, Instant.now());

        executor.shutdown();

        int totalOperations = threads * iterationsPerThread;
        logger.info("Concurrent performance: {} ms for {} operations across {} threads", 
            duration.toMillis(), totalOperations, threads);
        logger.info("Average: {} μs per operation", duration.toNanos() / totalOperations / 1000);

        // Performance assertion
        assertThat(duration.toMillis()).isLessThan(10000); // Should complete within 10 seconds
    }
}