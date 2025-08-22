package com.tca.peppol.util;

import com.tca.peppol.service.MetricsCollector;

import java.util.function.Supplier;

/**
 * Utility class for timing operations and recording metrics.
 */
public class MetricsTimer {
    
    private final long startTime;
    private final MetricsCollector metricsCollector;
    
    private MetricsTimer(MetricsCollector metricsCollector) {
        this.startTime = System.currentTimeMillis();
        this.metricsCollector = metricsCollector;
    }
    
    /**
     * Creates a new timer
     * @param metricsCollector The metrics collector to use
     * @return New MetricsTimer instance
     */
    public static MetricsTimer start(MetricsCollector metricsCollector) {
        return new MetricsTimer(metricsCollector);
    }
    
    /**
     * Stops the timer and records the elapsed time as processing time
     */
    public void stopAndRecordProcessingTime() {
        long elapsedTime = System.currentTimeMillis() - startTime;
        metricsCollector.recordProcessingTime(elapsedTime);
    }
    
    /**
     * Stops the timer and records the elapsed time as SML lookup time
     */
    public void stopAndRecordSmlLookupTime() {
        long elapsedTime = System.currentTimeMillis() - startTime;
        metricsCollector.recordSmlLookupTime(elapsedTime);
    }
    
    /**
     * Stops the timer and records the elapsed time as SMP query time
     */
    public void stopAndRecordSmpQueryTime() {
        long elapsedTime = System.currentTimeMillis() - startTime;
        metricsCollector.recordSmpQueryTime(elapsedTime);
    }
    
    /**
     * Stops the timer and records the elapsed time as validation time
     */
    public void stopAndRecordValidationTime() {
        long elapsedTime = System.currentTimeMillis() - startTime;
        metricsCollector.recordValidationTime(elapsedTime);
    }
    
    /**
     * Gets the elapsed time without recording it
     * @return Elapsed time in milliseconds
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * Times a supplier operation and records the time as processing time
     * @param operation The operation to time
     * @param metricsCollector The metrics collector
     * @param <T> Return type of the operation
     * @return Result of the operation
     */
    public static <T> T timeProcessing(Supplier<T> operation, MetricsCollector metricsCollector) {
        MetricsTimer timer = start(metricsCollector);
        try {
            return operation.get();
        } finally {
            timer.stopAndRecordProcessingTime();
        }
    }
    
    /**
     * Times a supplier operation and records the time as SML lookup time
     * @param operation The operation to time
     * @param metricsCollector The metrics collector
     * @param <T> Return type of the operation
     * @return Result of the operation
     */
    public static <T> T timeSmlLookup(Supplier<T> operation, MetricsCollector metricsCollector) {
        MetricsTimer timer = start(metricsCollector);
        try {
            return operation.get();
        } finally {
            timer.stopAndRecordSmlLookupTime();
        }
    }
    
    /**
     * Times a supplier operation and records the time as SMP query time
     * @param operation The operation to time
     * @param metricsCollector The metrics collector
     * @param <T> Return type of the operation
     * @return Result of the operation
     */
    public static <T> T timeSmpQuery(Supplier<T> operation, MetricsCollector metricsCollector) {
        MetricsTimer timer = start(metricsCollector);
        try {
            return operation.get();
        } finally {
            timer.stopAndRecordSmpQueryTime();
        }
    }
    
    /**
     * Times a supplier operation and records the time as validation time
     * @param operation The operation to time
     * @param metricsCollector The metrics collector
     * @param <T> Return type of the operation
     * @return Result of the operation
     */
    public static <T> T timeValidation(Supplier<T> operation, MetricsCollector metricsCollector) {
        MetricsTimer timer = start(metricsCollector);
        try {
            return operation.get();
        } finally {
            timer.stopAndRecordValidationTime();
        }
    }
}