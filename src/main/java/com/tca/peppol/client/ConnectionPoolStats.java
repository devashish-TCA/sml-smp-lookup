package com.tca.peppol.client;

/**
 * Connection pool statistics for monitoring HTTP client performance.
 */
public class ConnectionPoolStats {
    
    private final int available;
    private final int leased;
    private final int max;
    private final int pending;
    
    public ConnectionPoolStats(int available, int leased, int max, int pending) {
        this.available = available;
        this.leased = leased;
        this.max = max;
        this.pending = pending;
    }
    
    /**
     * Get the number of available connections in the pool.
     */
    public int getAvailable() {
        return available;
    }
    
    /**
     * Get the number of leased (in-use) connections.
     */
    public int getLeased() {
        return leased;
    }
    
    /**
     * Get the maximum number of connections allowed in the pool.
     */
    public int getMax() {
        return max;
    }
    
    /**
     * Get the number of pending connection requests.
     */
    public int getPending() {
        return pending;
    }
    
    /**
     * Get the total number of connections (available + leased).
     */
    public int getTotal() {
        return available + leased;
    }
    
    /**
     * Check if the pool is at capacity.
     */
    public boolean isAtCapacity() {
        return getTotal() >= max;
    }
    
    @Override
    public String toString() {
        return String.format("ConnectionPoolStats{available=%d, leased=%d, max=%d, pending=%d, total=%d}", 
            available, leased, max, pending, getTotal());
    }
}