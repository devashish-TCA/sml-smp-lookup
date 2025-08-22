package com.tca.peppol.client;

import java.util.Map;
import java.util.Objects;

/**
 * Represents an HTTP response with status, headers, body, and timing information.
 */
public class HttpResponse {
    
    private final int statusCode;
    private final String reasonPhrase;
    private final String body;
    private final byte[] bodyBytes;
    private final Map<String, String> headers;
    private final long responseTimeMs;
    
    /**
     * Create a new HTTP response.
     * 
     * @param statusCode HTTP status code
     * @param reasonPhrase HTTP reason phrase
     * @param body Response body content
     * @param headers Response headers
     * @param responseTimeMs Response time in milliseconds
     */
    public HttpResponse(int statusCode, String reasonPhrase, String body, 
                       Map<String, String> headers, long responseTimeMs) {
        this(statusCode, reasonPhrase, body, null, headers, responseTimeMs);
    }
    
    /**
     * Create a new HTTP response with binary body support.
     * 
     * @param statusCode HTTP status code
     * @param reasonPhrase HTTP reason phrase
     * @param body Response body content as string
     * @param bodyBytes Response body content as bytes
     * @param headers Response headers
     * @param responseTimeMs Response time in milliseconds
     */
    public HttpResponse(int statusCode, String reasonPhrase, String body, byte[] bodyBytes,
                       Map<String, String> headers, long responseTimeMs) {
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
        this.body = body;
        this.bodyBytes = bodyBytes != null ? bodyBytes.clone() : null;
        this.headers = headers != null ? Map.copyOf(headers) : Map.of();
        this.responseTimeMs = responseTimeMs;
    }
    
    /**
     * Get the HTTP status code.
     */
    public int getStatusCode() {
        return statusCode;
    }
    
    /**
     * Get the HTTP reason phrase.
     */
    public String getReasonPhrase() {
        return reasonPhrase;
    }
    
    /**
     * Get the response body content.
     */
    public String getBody() {
        return body;
    }
    
    /**
     * Get the response body content as bytes.
     */
    public byte[] getBodyBytes() {
        return bodyBytes != null ? bodyBytes.clone() : null;
    }
    
    /**
     * Get the response headers.
     */
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    /**
     * Get a specific header value.
     */
    public String getHeader(String name) {
        return headers.get(name);
    }
    
    /**
     * Get the response time in milliseconds.
     */
    public long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    /**
     * Check if the response indicates success (2xx status code).
     */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }
    
    /**
     * Check if the response indicates a client error (4xx status code).
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }
    
    /**
     * Check if the response indicates a server error (5xx status code).
     */
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpResponse that = (HttpResponse) o;
        return statusCode == that.statusCode &&
               responseTimeMs == that.responseTimeMs &&
               Objects.equals(reasonPhrase, that.reasonPhrase) &&
               Objects.equals(body, that.body) &&
               java.util.Arrays.equals(bodyBytes, that.bodyBytes) &&
               Objects.equals(headers, that.headers);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(statusCode, reasonPhrase, body, java.util.Arrays.hashCode(bodyBytes), headers, responseTimeMs);
    }
    
    @Override
    public String toString() {
        return String.format("HttpResponse{statusCode=%d, reasonPhrase='%s', " +
                           "bodyLength=%d, headerCount=%d, responseTimeMs=%d}",
                           statusCode, reasonPhrase, 
                           body != null ? body.length() : 0,
                           headers.size(), responseTimeMs);
    }
}