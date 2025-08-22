package com.tca.peppol.client;

/**
 * Exception thrown by the HTTP client for various error conditions.
 */
public class HttpClientException extends Exception {
    
    private final Integer statusCode;
    private final HttpResponse response;
    
    /**
     * Create a new HTTP client exception.
     * 
     * @param message Error message
     */
    public HttpClientException(String message) {
        super(message);
        this.statusCode = null;
        this.response = null;
    }
    
    /**
     * Create a new HTTP client exception with a cause.
     * 
     * @param message Error message
     * @param cause Underlying cause
     */
    public HttpClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
        this.response = null;
    }
    
    /**
     * Create a new HTTP client exception with status code and response.
     * 
     * @param message Error message
     * @param statusCode HTTP status code
     * @param response HTTP response object
     */
    public HttpClientException(String message, int statusCode, HttpResponse response) {
        super(message);
        this.statusCode = statusCode;
        this.response = response;
    }
    
    /**
     * Get the HTTP status code if available.
     */
    public Integer getStatusCode() {
        return statusCode;
    }
    
    /**
     * Get the HTTP response if available.
     */
    public HttpResponse getResponse() {
        return response;
    }
    
    /**
     * Check if this exception represents a client error (4xx status).
     */
    public boolean isClientError() {
        return statusCode != null && statusCode >= 400 && statusCode < 500;
    }
    
    /**
     * Check if this exception represents a server error (5xx status).
     */
    public boolean isServerError() {
        return statusCode != null && statusCode >= 500 && statusCode < 600;
    }
    
    /**
     * Check if this exception represents a network/connectivity error.
     */
    public boolean isNetworkError() {
        return statusCode == null && getCause() != null;
    }
}