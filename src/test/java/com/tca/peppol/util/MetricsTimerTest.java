package com.tca.peppol.util;

import com.tca.peppol.service.MetricsCollector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MetricsTimerTest {

    @Mock
    private MetricsCollector mockMetricsCollector;

    @Test
    void shouldRecordProcessingTime() throws InterruptedException {
        // Given
        MetricsTimer timer = MetricsTimer.start(mockMetricsCollector);
        Thread.sleep(10); // Small delay to ensure measurable time

        // When
        timer.stopAndRecordProcessingTime();

        // Then
        ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mockMetricsCollector).recordProcessingTime(timeCaptor.capture());
        assertThat(timeCaptor.getValue()).isGreaterThan(0L);
    }

    @Test
    void shouldRecordSmlLookupTime() throws InterruptedException {
        // Given
        MetricsTimer timer = MetricsTimer.start(mockMetricsCollector);
        Thread.sleep(10);

        // When
        timer.stopAndRecordSmlLookupTime();

        // Then
        ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mockMetricsCollector).recordSmlLookupTime(timeCaptor.capture());
        assertThat(timeCaptor.getValue()).isGreaterThan(0L);
    }

    @Test
    void shouldRecordSmpQueryTime() throws InterruptedException {
        // Given
        MetricsTimer timer = MetricsTimer.start(mockMetricsCollector);
        Thread.sleep(10);

        // When
        timer.stopAndRecordSmpQueryTime();

        // Then
        ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mockMetricsCollector).recordSmpQueryTime(timeCaptor.capture());
        assertThat(timeCaptor.getValue()).isGreaterThan(0L);
    }

    @Test
    void shouldRecordValidationTime() throws InterruptedException {
        // Given
        MetricsTimer timer = MetricsTimer.start(mockMetricsCollector);
        Thread.sleep(10);

        // When
        timer.stopAndRecordValidationTime();

        // Then
        ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mockMetricsCollector).recordValidationTime(timeCaptor.capture());
        assertThat(timeCaptor.getValue()).isGreaterThan(0L);
    }

    @Test
    void shouldGetElapsedTimeWithoutRecording() throws InterruptedException {
        // Given
        MetricsTimer timer = MetricsTimer.start(mockMetricsCollector);
        Thread.sleep(10);

        // When
        long elapsedTime = timer.getElapsedTime();

        // Then
        assertThat(elapsedTime).isGreaterThan(0L);
        // Verify no metrics were recorded
        verify(mockMetricsCollector, org.mockito.Mockito.never()).recordProcessingTime(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void shouldTimeProcessingOperation() {
        // Given
        Supplier<String> operation = () -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "result";
        };

        // When
        String result = MetricsTimer.timeProcessing(operation, mockMetricsCollector);

        // Then
        assertThat(result).isEqualTo("result");
        ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mockMetricsCollector).recordProcessingTime(timeCaptor.capture());
        assertThat(timeCaptor.getValue()).isGreaterThan(0L);
    }

    @Test
    void shouldTimeSmlLookupOperation() {
        // Given
        Supplier<String> operation = () -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "sml-result";
        };

        // When
        String result = MetricsTimer.timeSmlLookup(operation, mockMetricsCollector);

        // Then
        assertThat(result).isEqualTo("sml-result");
        ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mockMetricsCollector).recordSmlLookupTime(timeCaptor.capture());
        assertThat(timeCaptor.getValue()).isGreaterThan(0L);
    }

    @Test
    void shouldTimeSmpQueryOperation() {
        // Given
        Supplier<String> operation = () -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "smp-result";
        };

        // When
        String result = MetricsTimer.timeSmpQuery(operation, mockMetricsCollector);

        // Then
        assertThat(result).isEqualTo("smp-result");
        ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mockMetricsCollector).recordSmpQueryTime(timeCaptor.capture());
        assertThat(timeCaptor.getValue()).isGreaterThan(0L);
    }

    @Test
    void shouldTimeValidationOperation() {
        // Given
        Supplier<String> operation = () -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "validation-result";
        };

        // When
        String result = MetricsTimer.timeValidation(operation, mockMetricsCollector);

        // Then
        assertThat(result).isEqualTo("validation-result");
        ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mockMetricsCollector).recordValidationTime(timeCaptor.capture());
        assertThat(timeCaptor.getValue()).isGreaterThan(0L);
    }

    @Test
    void shouldRecordTimeEvenWhenOperationThrowsException() {
        // Given
        Supplier<String> operation = () -> {
            throw new RuntimeException("Test exception");
        };

        // When/Then
        try {
            MetricsTimer.timeProcessing(operation, mockMetricsCollector);
        } catch (RuntimeException e) {
            // Expected
        }

        // Verify time was still recorded
        ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
        verify(mockMetricsCollector).recordProcessingTime(timeCaptor.capture());
        assertThat(timeCaptor.getValue()).isGreaterThanOrEqualTo(0L);
    }
}