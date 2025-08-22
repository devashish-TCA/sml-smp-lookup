package com.tca.peppol.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.Map;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for HttpResponse.
 */
class HttpResponseTest {
    
    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        
        @Test
        @DisplayName("Should create response with all parameters")
        void shouldCreateResponseWithAllParameters() {
            // Given
            int statusCode = 200;
            String reasonPhrase = "OK";
            String body = "response body";
            Map<String, String> headers = Map.of("Content-Type", "application/xml");
            long responseTime = 150L;
            
            // When
            HttpResponse response = new HttpResponse(statusCode, reasonPhrase, body, headers, responseTime);
            
            // Then
            assertThat(response.getStatusCode()).isEqualTo(statusCode);
            assertThat(response.getReasonPhrase()).isEqualTo(reasonPhrase);
            assertThat(response.getBody()).isEqualTo(body);
            assertThat(response.getHeaders()).isEqualTo(headers);
            assertThat(response.getResponseTimeMs()).isEqualTo(responseTime);
        }
        
        @Test
        @DisplayName("Should handle null headers")
        void shouldHandleNullHeaders() {
            // When
            HttpResponse response = new HttpResponse(200, "OK", "body", null, 100L);
            
            // Then
            assertThat(response.getHeaders()).isNotNull();
            assertThat(response.getHeaders()).isEmpty();
        }
        
        @Test
        @DisplayName("Should create immutable headers map")
        void shouldCreateImmutableHeadersMap() {
            // Given
            Map<String, String> originalHeaders = new HashMap<>();
            originalHeaders.put("Content-Type", "application/xml");
            
            // When
            HttpResponse response = new HttpResponse(200, "OK", "body", originalHeaders, 100L);
            
            // Then
            assertThatThrownBy(() -> response.getHeaders().put("New-Header", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }
    
    @Nested
    @DisplayName("Status Code Classification Tests")
    class StatusCodeClassificationTests {
        
        @Test
        @DisplayName("Should identify success status codes")
        void shouldIdentifySuccessStatusCodes() {
            // Test various 2xx status codes
            int[] successCodes = {200, 201, 202, 204, 206};
            
            for (int code : successCodes) {
                HttpResponse response = new HttpResponse(code, "Success", null, null, 100L);
                assertThat(response.isSuccess()).isTrue();
                assertThat(response.isClientError()).isFalse();
                assertThat(response.isServerError()).isFalse();
            }
        }
        
        @Test
        @DisplayName("Should identify client error status codes")
        void shouldIdentifyClientErrorStatusCodes() {
            // Test various 4xx status codes
            int[] clientErrorCodes = {400, 401, 403, 404, 409, 422};
            
            for (int code : clientErrorCodes) {
                HttpResponse response = new HttpResponse(code, "Client Error", null, null, 100L);
                assertThat(response.isClientError()).isTrue();
                assertThat(response.isSuccess()).isFalse();
                assertThat(response.isServerError()).isFalse();
            }
        }
        
        @Test
        @DisplayName("Should identify server error status codes")
        void shouldIdentifyServerErrorStatusCodes() {
            // Test various 5xx status codes
            int[] serverErrorCodes = {500, 501, 502, 503, 504};
            
            for (int code : serverErrorCodes) {
                HttpResponse response = new HttpResponse(code, "Server Error", null, null, 100L);
                assertThat(response.isServerError()).isTrue();
                assertThat(response.isSuccess()).isFalse();
                assertThat(response.isClientError()).isFalse();
            }
        }
        
        @Test
        @DisplayName("Should handle edge case status codes")
        void shouldHandleEdgeCaseStatusCodes() {
            // Test boundary conditions
            HttpResponse response199 = new HttpResponse(199, "Info", null, null, 100L);
            assertThat(response199.isSuccess()).isFalse();
            
            HttpResponse response300 = new HttpResponse(300, "Redirect", null, null, 100L);
            assertThat(response300.isSuccess()).isFalse();
            assertThat(response300.isClientError()).isFalse();
            
            HttpResponse response399 = new HttpResponse(399, "Unknown", null, null, 100L);
            assertThat(response399.isClientError()).isFalse();
            
            HttpResponse response499 = new HttpResponse(499, "Client Error", null, null, 100L);
            assertThat(response499.isClientError()).isTrue();
            
            HttpResponse response599 = new HttpResponse(599, "Server Error", null, null, 100L);
            assertThat(response599.isServerError()).isTrue();
        }
    }
    
    @Nested
    @DisplayName("Header Access Tests")
    class HeaderAccessTests {
        
        @Test
        @DisplayName("Should get specific header value")
        void shouldGetSpecificHeaderValue() {
            // Given
            Map<String, String> headers = Map.of(
                "Content-Type", "application/xml",
                "Content-Length", "1234"
            );
            HttpResponse response = new HttpResponse(200, "OK", null, headers, 100L);
            
            // When & Then
            assertThat(response.getHeader("Content-Type")).isEqualTo("application/xml");
            assertThat(response.getHeader("Content-Length")).isEqualTo("1234");
            assertThat(response.getHeader("Non-Existent")).isNull();
        }
        
        @Test
        @DisplayName("Should handle case-sensitive header names")
        void shouldHandleCaseSensitiveHeaderNames() {
            // Given
            Map<String, String> headers = Map.of("Content-Type", "application/xml");
            HttpResponse response = new HttpResponse(200, "OK", null, headers, 100L);
            
            // When & Then
            assertThat(response.getHeader("Content-Type")).isEqualTo("application/xml");
            assertThat(response.getHeader("content-type")).isNull(); // Case sensitive
            assertThat(response.getHeader("CONTENT-TYPE")).isNull(); // Case sensitive
        }
    }
    
    @Nested
    @DisplayName("Equals and HashCode Tests")
    class EqualsAndHashCodeTests {
        
        @Test
        @DisplayName("Should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            // Given
            Map<String, String> headers = Map.of("Content-Type", "application/xml");
            HttpResponse response1 = new HttpResponse(200, "OK", "body", headers, 100L);
            HttpResponse response2 = new HttpResponse(200, "OK", "body", headers, 100L);
            
            // When & Then
            assertThat(response1).isEqualTo(response2);
            assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        }
        
        @Test
        @DisplayName("Should not be equal when fields differ")
        void shouldNotBeEqualWhenFieldsDiffer() {
            // Given
            Map<String, String> headers = Map.of("Content-Type", "application/xml");
            HttpResponse response1 = new HttpResponse(200, "OK", "body", headers, 100L);
            
            // Different status code
            HttpResponse response2 = new HttpResponse(201, "OK", "body", headers, 100L);
            assertThat(response1).isNotEqualTo(response2);
            
            // Different reason phrase
            HttpResponse response3 = new HttpResponse(200, "Created", "body", headers, 100L);
            assertThat(response1).isNotEqualTo(response3);
            
            // Different body
            HttpResponse response4 = new HttpResponse(200, "OK", "different", headers, 100L);
            assertThat(response1).isNotEqualTo(response4);
            
            // Different response time
            HttpResponse response5 = new HttpResponse(200, "OK", "body", headers, 200L);
            assertThat(response1).isNotEqualTo(response5);
        }
        
        @Test
        @DisplayName("Should handle null comparisons")
        void shouldHandleNullComparisons() {
            // Given
            HttpResponse response = new HttpResponse(200, "OK", null, null, 100L);
            
            // When & Then
            assertThat(response).isNotEqualTo(null);
            assertThat(response).isNotEqualTo("not a response");
        }
    }
    
    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {
        
        @Test
        @DisplayName("Should provide meaningful string representation")
        void shouldProvideMeaningfulStringRepresentation() {
            // Given
            Map<String, String> headers = Map.of("Content-Type", "application/xml");
            HttpResponse response = new HttpResponse(200, "OK", "response body", headers, 150L);
            
            // When
            String toString = response.toString();
            
            // Then
            assertThat(toString).contains("200");
            assertThat(toString).contains("OK");
            assertThat(toString).contains("13"); // body length
            assertThat(toString).contains("1"); // header count
            assertThat(toString).contains("150"); // response time
        }
        
        @Test
        @DisplayName("Should handle null body in toString")
        void shouldHandleNullBodyInToString() {
            // Given
            HttpResponse response = new HttpResponse(200, "OK", null, null, 100L);
            
            // When
            String toString = response.toString();
            
            // Then
            assertThat(toString).contains("bodyLength=0");
        }
    }
}