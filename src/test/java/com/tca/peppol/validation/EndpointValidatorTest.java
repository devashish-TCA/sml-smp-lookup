package com.tca.peppol.validation;

import com.tca.peppol.client.HttpClientException;
import com.tca.peppol.client.HttpResponse;
import com.tca.peppol.client.SecureHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.cert.X509Certificate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EndpointValidatorTest {
    
    @Mock
    private SecureHttpClient mockHttpClient;
    
    @Mock
    private X509Certificate mockCertificate;
    
    private EndpointValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new EndpointValidator(mockHttpClient);
    }
    
    @Test
    void testValidateTransportProfile_ValidProfile() {
        // Test with approved transport profile
        ValidationResult result = validator.validateTransportProfile("peppol-transport-as4-v2_0");
        
        assertTrue(result.isValid());
        assertEquals("Transport profile is approved", result.getMessage());
        assertNull(result.getErrorCode());
    }
    
    @Test
    void testValidateTransportProfile_ValidProfileCaseInsensitive() {
        // Test case insensitive validation
        ValidationResult result = validator.validateTransportProfile("PEPPOL-TRANSPORT-AS4-V2_0");
        
        assertTrue(result.isValid());
        assertEquals("Transport profile is approved", result.getMessage());
    }
    
    @Test
    void testValidateTransportProfile_InvalidProfile() {
        ValidationResult result = validator.validateTransportProfile("invalid-transport-profile");
        
        assertFalse(result.isValid());
        assertEquals("E5002", result.getErrorCode());
        assertTrue(result.getMessage().contains("not in the list of approved Peppol profiles"));
    }
    
    @Test
    void testValidateTransportProfile_NullProfile() {
        ValidationResult result = validator.validateTransportProfile(null);
        
        assertFalse(result.isValid());
        assertEquals("E5001", result.getErrorCode());
        assertEquals("Transport profile cannot be null or empty", result.getMessage());
    }
    
    @Test
    void testValidateTransportProfile_EmptyProfile() {
        ValidationResult result = validator.validateTransportProfile("   ");
        
        assertFalse(result.isValid());
        assertEquals("E5001", result.getErrorCode());
        assertEquals("Transport profile cannot be null or empty", result.getMessage());
    }
    
    @Test
    void testValidateEndpointUrl_ValidHttpsUrl() {
        String validUrl = "https://example.com/endpoint";
        ValidationResult result = validator.validateEndpointUrl(validUrl);
        
        assertTrue(result.isValid());
        assertEquals("Endpoint URL is valid", result.getMessage());
    }
    
    @Test
    void testValidateEndpointUrl_ValidHttpsUrlWithPort() {
        String validUrl = "https://example.com:8443/endpoint";
        ValidationResult result = validator.validateEndpointUrl(validUrl);
        
        assertTrue(result.isValid());
        assertEquals("Endpoint URL is valid", result.getMessage());
    }
    
    @Test
    void testValidateEndpointUrl_ValidHttpsUrlWithPath() {
        String validUrl = "https://example.com/path/to/endpoint?param=value";
        ValidationResult result = validator.validateEndpointUrl(validUrl);
        
        assertTrue(result.isValid());
        assertEquals("Endpoint URL is valid", result.getMessage());
    }
    
    @Test
    void testValidateEndpointUrl_HttpNotAllowed() {
        String httpUrl = "http://example.com/endpoint";
        ValidationResult result = validator.validateEndpointUrl(httpUrl);
        
        assertFalse(result.isValid());
        assertEquals("E5004", result.getErrorCode());
        assertTrue(result.getMessage().contains("must use HTTPS protocol"));
    }
    
    @Test
    void testValidateEndpointUrl_NullUrl() {
        ValidationResult result = validator.validateEndpointUrl(null);
        
        assertFalse(result.isValid());
        assertEquals("E5003", result.getErrorCode());
        assertEquals("Endpoint URL cannot be null or empty", result.getMessage());
    }
    
    @Test
    void testValidateEndpointUrl_EmptyUrl() {
        ValidationResult result = validator.validateEndpointUrl("   ");
        
        assertFalse(result.isValid());
        assertEquals("E5003", result.getErrorCode());
        assertEquals("Endpoint URL cannot be null or empty", result.getMessage());
    }
    
    @Test
    void testValidateEndpointUrl_MalformedUrl() {
        String malformedUrl = "https://";
        ValidationResult result = validator.validateEndpointUrl(malformedUrl);
        
        assertFalse(result.isValid());
        assertEquals("E5005", result.getErrorCode());
        assertTrue(result.getMessage().contains("does not conform to RFC 3986"));
    }
    
    @Test
    void testValidateEndpointUrl_InvalidPort() {
        String invalidPortUrl = "https://example.com:99999/endpoint";
        ValidationResult result = validator.validateEndpointUrl(invalidPortUrl);
        
        assertFalse(result.isValid());
        assertEquals("E5007", result.getErrorCode());
        assertTrue(result.getMessage().contains("port must be between 1 and 65535"));
    }
    
    @Test
    void testTestConnectivity_Success() throws Exception {
        String testUrl = "https://example.com/endpoint";
        HttpResponse mockResponse = new HttpResponse(200, "OK", null, Collections.emptyMap(), 100);
        
        try (MockedStatic<SecureHttpClient> mockedStatic = mockStatic(SecureHttpClient.class)) {
            mockedStatic.when(() -> SecureHttpClient.head(eq(testUrl), any()))
                       .thenReturn(mockResponse);
            
            ValidationResult result = validator.testConnectivity(testUrl);
            
            assertTrue(result.isValid());
            assertTrue(result.getMessage().contains("Endpoint is accessible (HTTP 200)"));
        }
    }
    
    @Test
    void testTestConnectivity_HttpError() throws Exception {
        String testUrl = "https://example.com/endpoint";
        HttpResponse mockResponse = new HttpResponse(404, "Not Found", null, Collections.emptyMap(), 100);
        
        try (MockedStatic<SecureHttpClient> mockedStatic = mockStatic(SecureHttpClient.class)) {
            mockedStatic.when(() -> SecureHttpClient.head(eq(testUrl), any()))
                       .thenReturn(mockResponse);
            
            ValidationResult result = validator.testConnectivity(testUrl);
            
            assertFalse(result.isValid());
            assertEquals("E4001", result.getErrorCode());
            assertTrue(result.getMessage().contains("returned HTTP 404"));
        }
    }
    
    @Test
    void testTestConnectivity_NetworkException() throws Exception {
        String testUrl = "https://example.com/endpoint";
        
        try (MockedStatic<SecureHttpClient> mockedStatic = mockStatic(SecureHttpClient.class)) {
            mockedStatic.when(() -> SecureHttpClient.head(eq(testUrl), any()))
                       .thenThrow(new HttpClientException("Connection timeout"));
            
            ValidationResult result = validator.testConnectivity(testUrl);
            
            assertFalse(result.isValid());
            assertEquals("E4002", result.getErrorCode());
            assertTrue(result.getMessage().contains("Endpoint connectivity test failed"));
        }
    }
    
    @Test
    void testTestConnectivity_InvalidUrl() {
        String invalidUrl = "http://example.com/endpoint";
        
        ValidationResult result = validator.testConnectivity(invalidUrl);
        
        assertFalse(result.isValid());
        assertEquals("E5004", result.getErrorCode());
        assertTrue(result.getMessage().contains("must use HTTPS protocol"));
    }
    
    @Test
    void testValidateSslCertificateMatching_NullCertificate() {
        String testUrl = "https://example.com/endpoint";
        
        ValidationResult result = validator.validateSslCertificateMatching(testUrl, null);
        
        assertFalse(result.isValid());
        assertEquals("E3001", result.getErrorCode());
        assertEquals("SMP certificate cannot be null", result.getMessage());
    }
    
    @Test
    void testValidateSslCertificateMatching_InvalidUrl() {
        String invalidUrl = "http://example.com/endpoint";
        
        ValidationResult result = validator.validateSslCertificateMatching(invalidUrl, mockCertificate);
        
        assertFalse(result.isValid());
        assertEquals("E5004", result.getErrorCode());
        assertTrue(result.getMessage().contains("must use HTTPS protocol"));
    }
    
    @Test
    void testValidateEndpoint_AllValidationsPass() throws Exception {
        String testUrl = "https://example.com/endpoint";
        String transportProfile = "peppol-transport-as4-v2_0";
        HttpResponse mockResponse = new HttpResponse(200, "OK", null, Collections.emptyMap(), 100);
        
        try (MockedStatic<SecureHttpClient> mockedStatic = mockStatic(SecureHttpClient.class)) {
            mockedStatic.when(() -> SecureHttpClient.head(eq(testUrl), any()))
                       .thenReturn(mockResponse);
            
            ValidationResult result = validator.validateEndpoint(testUrl, transportProfile, null, true);
            
            assertTrue(result.isValid());
            assertTrue(result.getMessage().contains("All endpoint validations passed"));
        }
    }
    
    @Test
    void testValidateEndpoint_SomeValidationsFail() {
        String testUrl = "http://example.com/endpoint"; // Invalid - HTTP instead of HTTPS
        String transportProfile = "invalid-profile"; // Invalid transport profile
        
        ValidationResult result = validator.validateEndpoint(testUrl, transportProfile, null, false);
        
        assertFalse(result.isValid());
        assertEquals("E5010", result.getErrorCode());
        assertTrue(result.getMessage().contains("Endpoint validation failed"));
        assertTrue(result.getMessage().contains("E5002")); // Transport profile error
        assertTrue(result.getMessage().contains("E5004")); // HTTPS error
    }
    
    @Test
    void testValidateEndpoint_WithoutConnectivityTest() {
        String testUrl = "https://example.com/endpoint";
        String transportProfile = "peppol-transport-as4-v2_0";
        
        ValidationResult result = validator.validateEndpoint(testUrl, transportProfile, null, false);
        
        assertTrue(result.isValid());
        assertTrue(result.getMessage().contains("All endpoint validations passed"));
        // Should not contain connectivity test results
        assertFalse(result.getMessage().contains("accessible"));
    }
    
    @Test
    void testApprovedTransportProfiles() {
        // Test all approved transport profiles
        String[] approvedProfiles = {
            "peppol-transport-as4-v2_0",
            "peppol-transport-as4-v1_0", 
            "busdox-transport-as4-v1_0"
        };
        
        for (String profile : approvedProfiles) {
            ValidationResult result = validator.validateTransportProfile(profile);
            assertTrue(result.isValid(), "Profile should be valid: " + profile);
        }
    }
    
    @Test
    void testUrlPatternValidation() {
        // Test various valid URL patterns
        String[] validUrls = {
            "https://example.com",
            "https://example.com/",
            "https://example.com/path",
            "https://example.com:443/path",
            "https://sub.example.com/path/to/endpoint",
            "https://example.com/path?query=value",
            "https://example.com/path#fragment"
        };
        
        for (String url : validUrls) {
            ValidationResult result = validator.validateEndpointUrl(url);
            assertTrue(result.isValid(), "URL should be valid: " + url);
        }
        
        // Test invalid URL patterns
        String[] invalidUrls = {
            "ftp://example.com",
            "https://",
            "https:///path",
            "https://example",
            "https://example./path"
        };
        
        for (String url : invalidUrls) {
            ValidationResult result = validator.validateEndpointUrl(url);
            assertFalse(result.isValid(), "URL should be invalid: " + url);
        }
    }
}