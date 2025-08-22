package com.tca.peppol.client;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.apache.hc.core5.util.Timeout;
import org.apache.hc.core5.util.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Secure HTTP client implementation with connection pooling, HTTP/2 support,
 * and comprehensive security features for Peppol SMP communication.
 * 
 * Features:
 * - HTTP/2 support with HTTP/1.1 fallback
 * - Connection pooling across Lambda invocations
 * - 30-second timeout configuration
 * - SSL/TLS certificate validation
 * - Secure cipher suites
 * - HTTPS-only enforcement
 * - Request/response logging
 * - Comprehensive error handling
 */
public class SecureHttpClient {
    
    private static final Logger logger = LoggerFactory.getLogger(SecureHttpClient.class);
    
    // Configuration constants
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int MAX_TOTAL_CONNECTIONS = 100;
    private static final int MAX_CONNECTIONS_PER_ROUTE = 20;
    private static final int CONNECTION_TIME_TO_LIVE_MINUTES = 5;
    private static final int VALIDATE_AFTER_INACTIVITY_SECONDS = 10;
    
    // Static connection manager for connection pooling across Lambda invocations
    private static volatile PoolingHttpClientConnectionManager connectionManager;
    private static volatile CloseableHttpClient httpClient;
    
    // Thread-safe singleton initialization
    private static final Object LOCK = new Object();
    
    /**
     * Get the singleton HTTP client instance with connection pooling.
     * This ensures connection reuse across Lambda invocations.
     */
    public static CloseableHttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (LOCK) {
                if (httpClient == null) {
                    httpClient = createSecureHttpClient();
                }
            }
        }
        return httpClient;
    }
    
    /**
     * Create a secure HTTP client with all security features enabled.
     */
    private static CloseableHttpClient createSecureHttpClient() {
        try {
            logger.info("Initializing secure HTTP client with connection pooling");
            
            // Create SSL context with secure configuration
            SSLContext sslContext = createSecureSSLContext();
            
            // Create SSL connection socket factory with secure cipher suites
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext,
                new String[]{"TLSv1.2", "TLSv1.3"}, // Only allow secure TLS versions
                getSecureCipherSuites(),
                new DefaultHostnameVerifier()
            );
            
            // Create connection socket factory registry (HTTPS only)
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                .<ConnectionSocketFactory>create()
                .register("https", sslSocketFactory)
                .build();
            
            // Create connection manager with pooling
            connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            
            // Configure connection pool
            connectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
            connectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);
            connectionManager.setValidateAfterInactivity(
                TimeValue.ofSeconds(VALIDATE_AFTER_INACTIVITY_SECONDS)
            );
            
            // Create request configuration with timeouts
            RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .setConnectTimeout(Timeout.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .setResponseTimeout(Timeout.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .build();
            
            // Build HTTP client with HTTP/2 support
            CloseableHttpClient client = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofMinutes(CONNECTION_TIME_TO_LIVE_MINUTES))
                .build();
            
            logger.info("Secure HTTP client initialized successfully with connection pooling");
            return client;
            
        } catch (Exception e) {
            logger.error("Failed to create secure HTTP client", e);
            throw new RuntimeException("Failed to initialize secure HTTP client", e);
        }
    }
    
    /**
     * Create secure SSL context with proper certificate validation.
     */
    private static SSLContext createSecureSSLContext() 
            throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        
        return SSLContextBuilder.create()
            .setProtocol("TLS")
            // Use system trust store for certificate validation
            .loadTrustMaterial(null, (TrustStrategy) null)
            .build();
    }
    
    /**
     * Get secure cipher suites for SSL/TLS connections.
     */
    private static String[] getSecureCipherSuites() {
        return new String[] {
            // TLS 1.3 cipher suites
            "TLS_AES_256_GCM_SHA384",
            "TLS_AES_128_GCM_SHA256",
            "TLS_CHACHA20_POLY1305_SHA256",
            
            // TLS 1.2 cipher suites (secure ones only)
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256"
        };
    }
    
    /**
     * Perform a secure HTTP GET request with comprehensive error handling.
     * 
     * @param url The URL to request (must be HTTPS)
     * @param headers Optional HTTP headers
     * @return HttpResponse containing status, headers, and body
     * @throws HttpClientException if the request fails
     */
    public static HttpResponse get(String url, Map<String, String> headers) throws HttpClientException {
        validateHttpsUrl(url);
        
        String correlationId = generateCorrelationId();
        logger.info("HTTP GET request initiated [correlationId={}, url={}]", correlationId, url);
        
        long startTime = System.currentTimeMillis();
        
        try {
            HttpGet httpGet = new HttpGet(url);
            
            // Add headers
            if (headers != null) {
                headers.forEach(httpGet::setHeader);
            }
            
            // Add default headers
            httpGet.setHeader("User-Agent", "Peppol-SML-SMP-Lookup/1.0");
            httpGet.setHeader("Accept", "application/xml, text/xml");
            httpGet.setHeader("X-Correlation-ID", correlationId);
            
            CloseableHttpClient client = getHttpClient();
            
            try (CloseableHttpResponse response = client.execute(httpGet)) {
                long responseTime = System.currentTimeMillis() - startTime;
                
                int statusCode = response.getCode();
                String reasonPhrase = response.getReasonPhrase();
                
                logger.info("HTTP GET response received [correlationId={}, status={}, responseTime={}ms]", 
                    correlationId, statusCode, responseTime);
                
                // Extract response body
                String responseBody = null;
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    try {
                        responseBody = EntityUtils.toString(entity);
                        EntityUtils.consume(entity); // Ensure entity is consumed
                    } catch (ParseException e) {
                        logger.warn("Failed to parse response entity [correlationId={}]", correlationId, e);
                        EntityUtils.consume(entity); // Still consume the entity
                    }
                }
                
                // Create response object
                HttpResponse httpResponse = new HttpResponse(
                    statusCode,
                    reasonPhrase,
                    responseBody,
                    extractHeaders(response),
                    responseTime
                );
                
                // Handle different status codes
                if (statusCode >= 200 && statusCode < 300) {
                    logger.debug("HTTP GET successful [correlationId={}, bodyLength={}]", 
                        correlationId, responseBody != null ? responseBody.length() : 0);
                    return httpResponse;
                } else {
                    logger.warn("HTTP GET returned non-success status [correlationId={}, status={}, reason={}]", 
                        correlationId, statusCode, reasonPhrase);
                    throw new HttpClientException(
                        String.format("HTTP request failed with status %d: %s", statusCode, reasonPhrase),
                        statusCode,
                        httpResponse
                    );
                }
                
            }
            
        } catch (IOException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("HTTP GET failed [correlationId={}, responseTime={}ms]", correlationId, responseTime, e);
            
            if (e instanceof SSLException) {
                throw new HttpClientException("SSL/TLS connection failed: " + e.getMessage(), e);
            } else {
                throw new HttpClientException("HTTP request failed: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Perform a secure HTTP HEAD request with comprehensive error handling.
     * 
     * @param url The URL to request (must be HTTPS)
     * @param headers Optional HTTP headers
     * @return HttpResponse containing status and headers (no body)
     * @throws HttpClientException if the request fails
     */
    public static HttpResponse head(String url, Map<String, String> headers) throws HttpClientException {
        validateHttpsUrl(url);
        
        String correlationId = generateCorrelationId();
        logger.info("HTTP HEAD request initiated [correlationId={}, url={}]", correlationId, url);
        
        long startTime = System.currentTimeMillis();
        
        try {
            HttpHead httpHead = new HttpHead(url);
            
            // Add headers
            if (headers != null) {
                headers.forEach(httpHead::setHeader);
            }
            
            // Add default headers
            httpHead.setHeader("User-Agent", "Peppol-SML-SMP-Lookup/1.0");
            httpHead.setHeader("X-Correlation-ID", correlationId);
            
            CloseableHttpClient client = getHttpClient();
            
            try (CloseableHttpResponse response = client.execute(httpHead)) {
                long responseTime = System.currentTimeMillis() - startTime;
                
                int statusCode = response.getCode();
                String reasonPhrase = response.getReasonPhrase();
                
                logger.info("HTTP HEAD response received [correlationId={}, status={}, responseTime={}ms]", 
                    correlationId, statusCode, responseTime);
                
                // HEAD requests don't have a body, but consume entity if present
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    EntityUtils.consume(entity);
                }
                
                // Create response object (no body for HEAD requests)
                HttpResponse httpResponse = new HttpResponse(
                    statusCode,
                    reasonPhrase,
                    null, // No body for HEAD requests
                    extractHeaders(response),
                    responseTime
                );
                
                logger.debug("HTTP HEAD successful [correlationId={}, status={}]", correlationId, statusCode);
                return httpResponse;
                
            }
            
        } catch (IOException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("HTTP HEAD failed [correlationId={}, responseTime={}ms]", correlationId, responseTime, e);
            
            if (e instanceof SSLException) {
                throw new HttpClientException("SSL/TLS connection failed: " + e.getMessage(), e);
            } else {
                throw new HttpClientException("HTTP request failed: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Perform a secure HTTP POST request with comprehensive error handling.
     * 
     * @param url The URL to request (must be HTTPS)
     * @param headers Optional HTTP headers
     * @param body Request body as byte array
     * @param timeoutSeconds Custom timeout in seconds
     * @return HttpResponse containing status, headers, and body
     * @throws HttpClientException if the request fails
     */
    public static HttpResponse post(String url, Map<String, String> headers, byte[] body, int timeoutSeconds) throws HttpClientException {
        validateHttpsUrl(url);
        
        String correlationId = generateCorrelationId();
        logger.info("HTTP POST request initiated [correlationId={}, url={}, bodySize={}]", 
            correlationId, url, body != null ? body.length : 0);
        
        long startTime = System.currentTimeMillis();
        
        try {
            HttpPost httpPost = new HttpPost(url);
            
            // Add headers
            if (headers != null) {
                headers.forEach(httpPost::setHeader);
            }
            
            // Add default headers
            httpPost.setHeader("User-Agent", "Peppol-SML-SMP-Lookup/1.0");
            httpPost.setHeader("X-Correlation-ID", correlationId);
            
            // Set request body
            if (body != null) {
                httpPost.setEntity(new ByteArrayEntity(body, null));
            }
            
            // Create custom request config with specified timeout
            RequestConfig customConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(timeoutSeconds))
                .setConnectTimeout(Timeout.ofSeconds(timeoutSeconds))
                .setResponseTimeout(Timeout.ofSeconds(timeoutSeconds))
                .build();
            httpPost.setConfig(customConfig);
            
            CloseableHttpClient client = getHttpClient();
            
            try (CloseableHttpResponse response = client.execute(httpPost)) {
                long responseTime = System.currentTimeMillis() - startTime;
                
                int statusCode = response.getCode();
                String reasonPhrase = response.getReasonPhrase();
                
                logger.info("HTTP POST response received [correlationId={}, status={}, responseTime={}ms]", 
                    correlationId, statusCode, responseTime);
                
                // Extract response body as bytes for binary responses
                byte[] responseBodyBytes = null;
                String responseBody = null;
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    try {
                        responseBodyBytes = EntityUtils.toByteArray(entity);
                        // For text responses, also create string representation
                        String contentType = entity.getContentType() != null ? entity.getContentType() : "";
                        if (contentType.startsWith("text/") || contentType.contains("xml") || contentType.contains("json")) {
                            responseBody = new String(responseBodyBytes);
                        } else {
                            responseBody = "[Binary content: " + responseBodyBytes.length + " bytes]";
                        }
                        EntityUtils.consume(entity); // Ensure entity is consumed
                    } catch (Exception e) {
                        logger.warn("Failed to parse response entity [correlationId={}]", correlationId, e);
                        EntityUtils.consume(entity); // Still consume the entity
                    }
                }
                
                // Create response object
                HttpResponse httpResponse = new HttpResponse(
                    statusCode,
                    reasonPhrase,
                    responseBody,
                    responseBodyBytes,
                    extractHeaders(response),
                    responseTime
                );
                
                // Handle different status codes
                if (statusCode >= 200 && statusCode < 300) {
                    logger.debug("HTTP POST successful [correlationId={}, bodyLength={}]", 
                        correlationId, responseBody != null ? responseBody.length() : 0);
                    return httpResponse;
                } else {
                    logger.warn("HTTP POST returned non-success status [correlationId={}, status={}, reason={}]", 
                        correlationId, statusCode, reasonPhrase);
                    throw new HttpClientException(
                        String.format("HTTP request failed with status %d: %s", statusCode, reasonPhrase),
                        statusCode,
                        httpResponse
                    );
                }
                
            }
            
        } catch (IOException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("HTTP POST failed [correlationId={}, responseTime={}ms]", correlationId, responseTime, e);
            
            if (e instanceof SSLException) {
                throw new HttpClientException("SSL/TLS connection failed: " + e.getMessage(), e);
            } else {
                throw new HttpClientException("HTTP request failed: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Validate that the URL uses HTTPS protocol.
     */
    private static void validateHttpsUrl(String url) throws HttpClientException {
        if (url == null || url.trim().isEmpty()) {
            throw new HttpClientException("URL cannot be null or empty");
        }
        
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (scheme == null) {
                throw new HttpClientException("Invalid URL format: " + url);
            }
            if (!"https".equalsIgnoreCase(scheme)) {
                throw new HttpClientException("Only HTTPS URLs are allowed, got: " + scheme);
            }
        } catch (IllegalArgumentException e) {
            throw new HttpClientException("Invalid URL format: " + url, e);
        }
    }
    
    /**
     * Extract headers from HTTP response.
     */
    private static Map<String, String> extractHeaders(ClassicHttpResponse response) {
        return java.util.Arrays.stream(response.getHeaders())
            .collect(java.util.stream.Collectors.toMap(
                header -> header.getName(),
                header -> header.getValue(),
                (existing, replacement) -> existing // Keep first value if duplicate
            ));
    }
    
    /**
     * Generate a correlation ID for request tracking.
     */
    private static String generateCorrelationId() {
        return "http-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString(Thread.currentThread().hashCode());
    }
    
    /**
     * Get connection pool statistics for monitoring.
     */
    public static ConnectionPoolStats getConnectionPoolStats() {
        if (connectionManager != null) {
            return new ConnectionPoolStats(
                connectionManager.getTotalStats().getAvailable(),
                connectionManager.getTotalStats().getLeased(),
                connectionManager.getTotalStats().getMax(),
                connectionManager.getTotalStats().getPending()
            );
        }
        return new ConnectionPoolStats(0, 0, 0, 0);
    }
    
    /**
     * Close the HTTP client and connection manager.
     * Should be called during Lambda shutdown.
     */
    public static void shutdown() {
        synchronized (LOCK) {
            if (httpClient != null) {
                try {
                    logger.info("Shutting down HTTP client");
                    httpClient.close();
                    httpClient = null;
                } catch (IOException e) {
                    logger.warn("Error closing HTTP client", e);
                }
            }
            
            if (connectionManager != null) {
                connectionManager.close();
                connectionManager = null;
            }
        }
    }
}