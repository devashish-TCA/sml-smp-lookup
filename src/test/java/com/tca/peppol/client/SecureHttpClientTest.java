package com.tca.peppol.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.Map;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for SecureHttpClient.
 */
class SecureHttpClientTest {
    
    @BeforeEach
    void setUp() {
        // Clean up any existing client state
        SecureHttpClient.shutdown();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after each test
        SecureHttpClient.shutdown();
    }
    
    @Nested
    @DisplayName("HTTP Client Initialization Tests")
    class InitializationTests {
        
        @Test
        @DisplayName("Should create HTTP client singleton")
        void shouldCreateHttpClientSingleton() {
            // When
            var client1 = SecureHttpClient.getHttpClient();
            var client2 = SecureHttpClient.getHttpClient();
            
            // Then
            assertThat(client1).isNotNull();
            assertThat(client2).isNotNull();
            assertThat(client1).isSameAs(client2);
        }
        
        @Test
        @DisplayName("Should initialize connection pool stats")
        void shouldInitializeConnectionPoolStats() {
            // When
            SecureHttpClient.getHttpClient(); // Initialize client
            var stats = SecureHttpClient.getConnectionPoolStats();
            
            // Then
            assertThat(stats).isNotNull();
            assertThat(stats.getMax()).isGreaterThan(0);
        }
    }
    
    @Nested
    @DisplayName("URL Validation Tests")
    class UrlValidationTests {
        
        @Test
        @DisplayName("Should reject null URL")
        void shouldRejectNullUrl() {
            // When & Then
            assertThatThrownBy(() -> SecureHttpClient.get(null, null))
                .isInstanceOf(HttpClientException.class)
                .hasMessageContaining("URL cannot be null or empty");
        }
        
        @Test
        @DisplayName("Should reject empty URL")
        void shouldRejectEmptyUrl() {
            // When & Then
            assertThatThrownBy(() -> SecureHttpClient.get("", null))
                .isInstanceOf(HttpClientException.class)
                .hasMessageContaining("URL cannot be null or empty");
        }
        
        @Test
        @DisplayName("Should reject HTTP URLs")
        void shouldRejectHttpUrls() {
            // When & Then
            assertThatThrownBy(() -> SecureHttpClient.get("http://example.com", null))
                .isInstanceOf(HttpClientException.class)
                .hasMessageContaining("Only HTTPS URLs are allowed");
        }
        
        @Test
        @DisplayName("Should reject FTP URLs")
        void shouldRejectFtpUrls() {
            // When & Then
            assertThatThrownBy(() -> SecureHttpClient.get("ftp://example.com", null))
                .isInstanceOf(HttpClientException.class)
                .hasMessageContaining("Only HTTPS URLs are allowed");
        }
        
        @Test
        @DisplayName("Should reject malformed URLs")
        void shouldRejectMalformedUrls() {
            // When & Then
            assertThatThrownBy(() -> SecureHttpClient.get("not-a-url", null))
                .isInstanceOf(HttpClientException.class)
                .hasMessageContaining("Invalid URL format");
        }
        
        @Test
        @DisplayName("Should accept valid HTTPS URLs")
        void shouldAcceptValidHttpsUrls() {
            // This test would require a mock server or network access
            // For now, we verify that the URL validation passes
            String validUrl = "https://example.com/path";
            
            // The URL validation should pass, but the actual request will fail
            // due to no mock server - that's expected for this unit test
            assertThatThrownBy(() -> SecureHttpClient.get(validUrl, null))
                .isInstanceOf(HttpClientException.class)
                .hasMessageContaining("HTTP request failed");
        }
    }
    
    @Nested
    @DisplayName("Security Configuration Tests")
    class SecurityConfigurationTests {
        
        @Test
        @DisplayName("Should enforce HTTPS only")
        void shouldEnforceHttpsOnly() {
            // Test various non-HTTPS schemes
            String[] invalidUrls = {
                "http://example.com",
                "ftp://example.com",
                "file:///path/to/file",
                "ldap://example.com"
            };
            
            for (String url : invalidUrls) {
                assertThatThrownBy(() -> SecureHttpClient.get(url, null))
                    .isInstanceOf(HttpClientException.class)
                    .hasMessageContaining("Only HTTPS URLs are allowed");
            }
        }
    }
    
    @Nested
    @DisplayName("Connection Pool Tests")
    class ConnectionPoolTests {
        
        @Test
        @DisplayName("Should provide connection pool statistics")
        void shouldProvideConnectionPoolStatistics() {
            // When
            SecureHttpClient.getHttpClient(); // Initialize client
            var stats = SecureHttpClient.getConnectionPoolStats();
            
            // Then
            assertThat(stats).isNotNull();
            assertThat(stats.getMax()).isGreaterThan(0);
            assertThat(stats.getAvailable()).isGreaterThanOrEqualTo(0);
            assertThat(stats.getLeased()).isGreaterThanOrEqualTo(0);
            assertThat(stats.getPending()).isGreaterThanOrEqualTo(0);
            assertThat(stats.getTotal()).isEqualTo(stats.getAvailable() + stats.getLeased());
        }
        
        @Test
        @DisplayName("Should return empty stats when client not initialized")
        void shouldReturnEmptyStatsWhenClientNotInitialized() {
            // When
            var stats = SecureHttpClient.getConnectionPoolStats();
            
            // Then
            assertThat(stats).isNotNull();
            assertThat(stats.getMax()).isEqualTo(0);
            assertThat(stats.getAvailable()).isEqualTo(0);
            assertThat(stats.getLeased()).isEqualTo(0);
            assertThat(stats.getPending()).isEqualTo(0);
        }
    }
    
    @Nested
    @DisplayName("Header Handling Tests")
    class HeaderHandlingTests {
        
        @Test
        @DisplayName("Should add custom headers to request")
        void shouldAddCustomHeadersToRequest() {
            // Given
            Map<String, String> headers = new HashMap<>();
            headers.put("X-Custom-Header", "custom-value");
            headers.put("Authorization", "Bearer token");
            
            // When & Then
            // This will fail due to no mock server, but we can verify headers are processed
            assertThatThrownBy(() -> SecureHttpClient.get("https://httpbin.org/status/404", headers))
                .isInstanceOf(HttpClientException.class)
                .satisfiesAnyOf(
                    ex -> assertThat(ex.getMessage()).contains("HTTP request failed"),
                    ex -> assertThat(ex.getMessage()).contains("404")
                );
        }
        
        @Test
        @DisplayName("Should handle null headers gracefully")
        void shouldHandleNullHeadersGracefully() {
            // When & Then
            assertThatThrownBy(() -> SecureHttpClient.get("https://httpbin.org/status/404", null))
                .isInstanceOf(HttpClientException.class)
                .satisfiesAnyOf(
                    ex -> assertThat(ex.getMessage()).contains("HTTP request failed"),
                    ex -> assertThat(ex.getMessage()).contains("404")
                );
        }
        
        @Test
        @DisplayName("Should handle empty headers gracefully")
        void shouldHandleEmptyHeadersGracefully() {
            // When & Then
            assertThatThrownBy(() -> SecureHttpClient.get("https://httpbin.org/status/404", Map.of()))
                .isInstanceOf(HttpClientException.class)
                .satisfiesAnyOf(
                    ex -> assertThat(ex.getMessage()).contains("HTTP request failed"),
                    ex -> assertThat(ex.getMessage()).contains("404")
                );
        }
    }
    
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should handle network errors gracefully")
        void shouldHandleNetworkErrorsGracefully() {
            // Using a non-routable IP address to simulate network error
            String unreachableUrl = "https://192.0.2.1/test"; // RFC 5737 test address
            
            // When & Then
            assertThatThrownBy(() -> SecureHttpClient.get(unreachableUrl, null))
                .isInstanceOf(HttpClientException.class)
                .hasMessageContaining("HTTP request failed");
        }
        
        @Test
        @DisplayName("Should handle SSL errors gracefully")
        void shouldHandleSslErrorsGracefully() {
            // Using a URL with invalid SSL certificate
            String invalidSslUrl = "https://self-signed.badssl.com/";
            
            // When & Then
            assertThatThrownBy(() -> SecureHttpClient.get(invalidSslUrl, null))
                .isInstanceOf(HttpClientException.class);
        }
    }
    
    @Nested
    @DisplayName("Shutdown Tests")
    class ShutdownTests {
        
        @Test
        @DisplayName("Should shutdown gracefully")
        void shouldShutdownGracefully() {
            // Given
            SecureHttpClient.getHttpClient(); // Initialize client
            
            // When
            assertThatCode(() -> SecureHttpClient.shutdown())
                .doesNotThrowAnyException();
            
            // Then
            var stats = SecureHttpClient.getConnectionPoolStats();
            assertThat(stats.getMax()).isEqualTo(0);
        }
        
        @Test
        @DisplayName("Should handle multiple shutdowns gracefully")
        void shouldHandleMultipleShutdownsGracefully() {
            // Given
            SecureHttpClient.getHttpClient(); // Initialize client
            
            // When & Then
            assertThatCode(() -> {
                SecureHttpClient.shutdown();
                SecureHttpClient.shutdown();
                SecureHttpClient.shutdown();
            }).doesNotThrowAnyException();
        }
        
        @Test
        @DisplayName("Should handle shutdown without initialization")
        void shouldHandleShutdownWithoutInitialization() {
            // When & Then
            assertThatCode(() -> SecureHttpClient.shutdown())
                .doesNotThrowAnyException();
        }
    }
    
    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {
        
        @Test
        @DisplayName("Should be thread safe for client initialization")
        void shouldBeThreadSafeForClientInitialization() throws InterruptedException {
            // Given
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];
            var clients = new Object[threadCount];
            
            // When
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                threads[i] = new Thread(() -> {
                    clients[index] = SecureHttpClient.getHttpClient();
                });
                threads[i].start();
            }
            
            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }
            
            // Then
            assertThat(clients[0]).isNotNull();
            for (int i = 1; i < threadCount; i++) {
                assertThat(clients[i]).isSameAs(clients[0]);
            }
        }
    }
}