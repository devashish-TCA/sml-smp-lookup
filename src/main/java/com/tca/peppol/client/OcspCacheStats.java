package com.tca.peppol.client;

import java.util.Objects;

/**
 * Statistics for OCSP response cache monitoring and observability.
 * 
 * This class provides metrics about the OCSP cache performance including
 * total entries, expired entries, and cache capacity information.
 */
public class OcspCacheStats {
    
    private final int totalEntries;
    private final int expiredEntries;
    private final int maxCapacity;
    private final double utilizationPercentage;
    
    /**
     * Create OCSP cache statistics.
     * 
     * @param totalEntries Total number of entries in the cache
     * @param expiredEntries Number of expired entries in the cache
     * @param maxCapacity Maximum cache capacity
     */
    public OcspCacheStats(int totalEntries, int expiredEntries, int maxCapacity) {
        this.totalEntries = totalEntries;
        this.expiredEntries = expiredEntries;
        this.maxCapacity = maxCapacity;
        this.utilizationPercentage = maxCapacity > 0 ? (double) totalEntries / maxCapacity * 100.0 : 0.0;
    }
    
    /**
     * Get the total number of entries in the cache.
     */
    public int getTotalEntries() {
        return totalEntries;
    }
    
    /**
     * Get the number of expired entries in the cache.
     */
    public int getExpiredEntries() {
        return expiredEntries;
    }
    
    /**
     * Get the number of valid (non-expired) entries in the cache.
     */
    public int getValidEntries() {
        return totalEntries - expiredEntries;
    }
    
    /**
     * Get the maximum cache capacity.
     */
    public int getMaxCapacity() {
        return maxCapacity;
    }
    
    /**
     * Get the cache utilization percentage (0-100).
     */
    public double getUtilizationPercentage() {
        return utilizationPercentage;
    }
    
    /**
     * Check if the cache is near capacity (>80% full).
     */
    public boolean isNearCapacity() {
        return utilizationPercentage > 80.0;
    }
    
    /**
     * Check if the cache has a high percentage of expired entries (>30%).
     */
    public boolean hasHighExpiredRatio() {
        return totalEntries > 0 && (double) expiredEntries / totalEntries > 0.3;
    }
    
    /**
     * Get the cache hit ratio estimate based on valid entries.
     * This is a rough estimate assuming expired entries were once valid.
     */
    public double getEstimatedHitRatio() {
        if (totalEntries == 0) {
            return 0.0;
        }
        return (double) getValidEntries() / totalEntries;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OcspCacheStats that = (OcspCacheStats) o;
        return totalEntries == that.totalEntries &&
               expiredEntries == that.expiredEntries &&
               maxCapacity == that.maxCapacity;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(totalEntries, expiredEntries, maxCapacity);
    }
    
    @Override
    public String toString() {
        return String.format("OcspCacheStats{totalEntries=%d, validEntries=%d, expiredEntries=%d, " +
                           "maxCapacity=%d, utilization=%.1f%%, nearCapacity=%s, highExpiredRatio=%s}",
                           totalEntries, getValidEntries(), expiredEntries, maxCapacity, 
                           utilizationPercentage, isNearCapacity(), hasHighExpiredRatio());
    }
}