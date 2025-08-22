package com.tca.peppol.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for HttpClientException.
 */
class HttpClientExceptionTest {
    
    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        
        @Test
        @DisplayName("Should create exception with message only")
        void shouldCreateExceptionWithMessageOnly() {
            // Given
            String message = "Test error message";
            
            // When
            HttpClientException exception = new HttpClientException(message);
            
            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getStatusCode()).isNull();
            assertThat(exception.getResponse()).isNull();
            assertThat(exception.getCause()).isNull();
        }
        
        @Test
        @DisplayName("Should create exception with message and cause")
        void shouldCreateExceptionWithMessageAndCause() {
            // Given
            String message = "Test error message";
            IOException cause = new IOException("Network error");
            
            // When
            HttpClientException exception = new HttpClientException(message, cause);
            
            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getStatusCode()).isNull();
            assertThat(exception.getResponse()).isNull();
        }
        
        @Test
        @DisplayName("Should create exception with status code and response")
        void shouldCreateExceptionWithStatusCodeAndResponse() {
            // Given
            String message = "HTTP error";
            int statusCode = 404;
            HttpResponse response = new HttpResponse(404, "Not Found", "Page not found", Map.of(), 100L);
            
            // When
            HttpClientException exception = new HttpClientException(message, statusCode, response);
            
            // Then
            assertThat(exception.getMessage()).isEqualTo(message);
            assertThat(exception.getStatusCode()).isEqualTo(statusCode);
            assertThat(exception.getResponse()).isEqualTo(response);
            assertThat(exception.getCause()).isNull();
        }
    }
    
    @Nested
    @DisplayName("Error Classification Tests")
    class ErrorClassificationTests {
        
        @Test
        @DisplayName("Should identify client errors")
        void shouldIdentifyClientErrors() {
            // Test various 4xx status codes
            int[] clientErrorCodes = {400, 401, 403, 404, 409, 422, 429};
            
            for (int code : clientErrorCodes) {
                HttpResponse response = new HttpResponse(code, "Client Error", null, null, 100L);
                HttpClientException exception = new HttpClientException("Client error", code, response);
                
                assertThat(exception.isClientError()).isTrue();
                assertThat(exception.isServerError()).isFalse();
                assertThat(exception.isNetworkError()).isFalse();
            }
        }
        
        @Test
        @DisplayName("Should identify server errors")
        void shouldIdentifyServerErrors() {
            // Test various 5xx status codes
            int[] serverErrorCodes = {500, 501, 502, 503, 504, 505};
            
            for (int code : serverErrorCodes) {
                HttpResponse response = new HttpResponse(code, "Server Error", null, null, 100L);
                HttpClientException exception = new HttpClientException("Server error", code, response);
                
                assertThat(exception.isServerError()).isTrue();
                assertThat(exception.isClientError()).isFalse();
                assertThat(exception.isNetworkError()).isFalse();
            }
        }
        
        @Test
        @DisplayName("Should identify network errors")
        void shouldIdentifyNetworkErrors() {
            // Given
            IOException networkCause = new IOException("Connection timeout");
            HttpClientException exception = new HttpClientException("Network error", networkCause);
            
            // When & Then
            assertThat(exception.isNetworkError()).isTrue();
            assertThat(exception.isClientError()).isFalse();
            assertThat(exception.isServerError()).isFalse();
        }
        
        @Test
        @DisplayName("Should handle edge case status codes")
        void shouldHandleEdgeCaseStatusCodes() {
            // Test boundary conditions
            HttpResponse response399 = new HttpResponse(399, "Unknown", null, null, 100L);
            HttpClientException exception399 = new HttpClientException("Error", 399, response399);
            assertThat(exception399.isClientError()).isFalse();
            assertThat(exception399.isServerError()).isFalse();
            
            HttpResponse response499 = new HttpResponse(499, "Client Error", null, null, 100L);
            HttpClientException exception499 = new HttpClientException("Error", 499, response499);
            assertThat(exception499.isClientError()).isTrue();
            
            HttpResponse response599 = new HttpResponse(599, "Server Error", null, null, 100L);
            HttpClientException exception599 = new HttpClientException("Error", 599, response599);
            assertThat(exception599.isServerError()).isTrue();
            
            HttpResponse response600 = new HttpResponse(600, "Unknown", null, null, 100L);
            HttpClientException exception600 = new HttpClientException("Error", 600, response600);
            assertThat(exception600.isServerError()).isFalse();
        }
        
        @Test
        @DisplayName("Should handle null status code")
        void shouldHandleNullStatusCode() {
            // Given
            HttpClientException exception = new HttpClientException("Error without status");
            
            // When & Then
            assertThat(exception.isClientError()).isFalse();
            assertThat(exception.isServerError()).isFalse();
            assertThat(exception.isNetworkError()).isFalse(); // No cause either
        }
        
        @Test
        @DisplayName("Should handle null status code with cause")
        void shouldHandleNullStatusCodeWithCause() {
            // Given
            IOException cause = new IOException("Network issue");
            HttpClientException exception = new HttpClientException("Network error", cause);
            
            // When & Then
            assertThat(exception.isNetworkError()).isTrue();
            assertThat(exception.isClientError()).isFalse();
            assertThat(exception.isServerError()).isFalse();
        }
    }
    
    @Nested
    @DisplayName("Response Access Tests")
    class ResponseAccessTests {
        
        @Test
        @DisplayName("Should provide access to HTTP response")
        void shouldProvideAccessToHttpResponse() {
            // Given
            HttpResponse response = new HttpResponse(500, "Internal Server Error", 
                "Error details", Map.of("Content-Type", "text/plain"), 200L);
            HttpClientException exception = new HttpClientException("Server error", 500, response);
            
            // When & Then
            assertThat(exception.getResponse()).isEqualTo(response);
            assertThat(exception.getResponse().getStatusCode()).isEqualTo(500);
            assertThat(exception.getResponse().getBody()).isEqualTo("Error details");
        }
        
        @Test
        @DisplayName("Should handle null response")
        void shouldHandleNullResponse() {
            // Given
            HttpClientException exception = new HttpClientException("Error without response");
            
            // When & Then
            assertThat(exception.getResponse()).isNull();
        }
    }
    
    @Nested
    @DisplayName("Status Code Access Tests")
    class StatusCodeAccessTests {
        
        @Test
        @DisplayName("Should provide access to status code")
        void shouldProvideAccessToStatusCode() {
            // Given
            HttpResponse response = new HttpResponse(404, "Not Found", null, null, 100L);
            HttpClientException exception = new HttpClientException("Not found", 404, response);
            
            // When & Then
            assertThat(exception.getStatusCode()).isEqualTo(404);
        }
        
        @Test
        @DisplayName("Should handle null status code")
        void shouldHandleNullStatusCode() {
            // Given
            HttpClientException exception = new HttpClientException("Error without status");
            
            // When & Then
            assertThat(exception.getStatusCode()).isNull();
        }
    }
}