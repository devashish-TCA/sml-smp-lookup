package com.tca.peppol.validation;

import com.tca.peppol.client.SecureHttpClient;
import com.tca.peppol.client.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Validates transport profiles and endpoints according to Peppol specifications.
 * Handles transport profile validation, endpoint URL validation, HTTPS enforcement,
 * and optional endpoint connectivity testing.
 */
public class EndpointValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(EndpointValidator.class);
    
    // Approved Peppol transport profiles
    private static final Set<String> APPROVED_TRANSPORT_PROFILES = Set.of(
        "peppol-transport-as4-v2_0",
        "peppol-transport-as4-v1_0",
        "busdox-transport-as4-v1_0"
    );
    
    // URL validation pattern for RFC 3986 compliance
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^https://([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}(:[0-9]{1,5})?(/.*)?$"
    );
    
    private final SecureHttpClient httpClient;
    
    public EndpointValidator(SecureHttpClient httpClient) {
        this.httpClient = httpClient;
    }
    
    /**
     * Validates a transport profile against approved Peppol profiles.
     */
    public ValidationResult validateTransportProfile(String transportProfile) {
        logger.debug("Validating transport profile: {}", transportProfile);
        
        if (transportProfile == null || transportProfile.trim().isEmpty()) {
            return ValidationResult.failure("E5001", "Transport profile cannot be null or empty");
        }
        
        String normalizedProfile = transportProfile.trim().toLowerCase();
        
        if (!APPROVED_TRANSPORT_PROFILES.contains(normalizedProfile)) {
            return ValidationResult.failure("E5002", 
                String.format("Transport profile '%s' is not in the list of approved Peppol profiles: %s", 
                    transportProfile, APPROVED_TRANSPORT_PROFILES));
        }
        
        logger.debug("Transport profile validation successful: {}", transportProfile);
        return ValidationResult.success("Transport profile is approved");
    }
    
    /**
     * Validates endpoint URL according to RFC 3986 with HTTPS enforcement.
     */
    public ValidationResult validateEndpointUrl(String endpointUrl) {
        logger.debug("Validating endpoint URL: {}", endpointUrl);
        
        if (endpointUrl == null || endpointUrl.trim().isEmpty()) {
            return ValidationResult.failure("E5003", "Endpoint URL cannot be null or empty");
        }
        
        String trimmedUrl = endpointUrl.trim();
        
        // Check HTTPS requirement
        if (!trimmedUrl.toLowerCase().startsWith("https://")) {
            return ValidationResult.failure("E5004", 
                "Endpoint URL must use HTTPS protocol. HTTP is not allowed for Peppol endpoints");
        }
        
        // Validate URL format according to RFC 3986
        if (!URL_PATTERN.matcher(trimmedUrl).matches()) {
            return ValidationResult.failure("E5005", 
                "Endpoint URL does not conform to RFC 3986 format requirements");
        }
        
        // Additional URL validation using Java URL class
        try {
            URL url = new URL(trimmedUrl);
            URI uri = url.toURI();
            
            // Validate host is not empty
            if (url.getHost() == null || url.getHost().trim().isEmpty()) {
                return ValidationResult.failure("E5006", "Endpoint URL must have a valid host");
            }
            
            // Validate port if specified
            if (url.getPort() != -1 && (url.getPort() < 1 || url.getPort() > 65535)) {
                return ValidationResult.failure("E5007", "Endpoint URL port must be between 1 and 65535");
            }
            
        } catch (MalformedURLException e) {
            return ValidationResult.failure("E5008", 
                "Endpoint URL is malformed: " + e.getMessage());
        } catch (URISyntaxException e) {
            return ValidationResult.failure("E5009", 
                "Endpoint URL syntax is invalid: " + e.getMessage());
        }
        
        logger.debug("Endpoint URL validation successful: {}", endpointUrl);
        return ValidationResult.success("Endpoint URL is valid");
    }
    
    /**
     * Tests endpoint connectivity when requested.
     */
    public ValidationResult testConnectivity(String endpointUrl) {
        logger.debug("Testing connectivity to endpoint: {}", endpointUrl);
        
        // First validate the URL
        ValidationResult urlValidation = validateEndpointUrl(endpointUrl);
        if (!urlValidation.isValid()) {
            return urlValidation;
        }
        
        try {
            // Perform HTTP HEAD request to test connectivity
            HttpResponse response = httpClient.head(endpointUrl, Collections.emptyMap());
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 400) {
                logger.debug("Endpoint connectivity test successful: {} returned {}", 
                    endpointUrl, response.getStatusCode());
                return ValidationResult.success(
                    String.format("Endpoint is accessible (HTTP %d)", response.getStatusCode()));
            } else {
                return ValidationResult.failure("E4001", 
                    String.format("Endpoint returned HTTP %d", response.getStatusCode()));
            }
            
        } catch (Exception e) {
            logger.warn("Endpoint connectivity test failed for {}: {}", endpointUrl, e.getMessage());
            return ValidationResult.failure("E4002", 
                "Endpoint connectivity test failed: " + e.getMessage());
        }
    }
    
    /**
     * Validates SSL certificate matching between endpoint and SMP certificate.
     */
    public ValidationResult validateSslCertificateMatching(String endpointUrl, X509Certificate smpCertificate) {
        logger.debug("Validating SSL certificate matching for endpoint: {}", endpointUrl);
        
        if (smpCertificate == null) {
            return ValidationResult.failure("E3001", "SMP certificate cannot be null");
        }
        
        // First validate the URL
        ValidationResult urlValidation = validateEndpointUrl(endpointUrl);
        if (!urlValidation.isValid()) {
            return urlValidation;
        }
        
        try {
            URL url = new URL(endpointUrl);
            String host = url.getHost();
            int port = url.getPort() == -1 ? 443 : url.getPort();
            
            // Get SSL certificate from endpoint
            SSLContext sslContext = SSLContext.getDefault();
            SSLSocketFactory factory = sslContext.getSocketFactory();
            
            try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
                socket.startHandshake();
                Certificate[] serverCerts = socket.getSession().getPeerCertificates();
                
                if (serverCerts.length == 0) {
                    return ValidationResult.failure("E3002", "No SSL certificates found at endpoint");
                }
                
                X509Certificate endpointCert = (X509Certificate) serverCerts[0];
                
                // Compare certificates
                if (smpCertificate.equals(endpointCert)) {
                    logger.debug("SSL certificate matching successful - certificates are identical");
                    return ValidationResult.success("SSL certificate matches SMP certificate exactly");
                }
                
                // Compare public keys if certificates are not identical
                if (Arrays.equals(smpCertificate.getPublicKey().getEncoded(), 
                                endpointCert.getPublicKey().getEncoded())) {
                    logger.debug("SSL certificate matching successful - public keys match");
                    return ValidationResult.success("SSL certificate public key matches SMP certificate");
                }
                
                // Compare subject DN as fallback
                if (smpCertificate.getSubjectDN().equals(endpointCert.getSubjectDN())) {
                    logger.warn("SSL certificates have matching subject DN but different keys - potential security issue");
                    return ValidationResult.failure("E3003", 
                        "SSL certificate subject matches but public keys differ - potential security issue");
                }
                
                return ValidationResult.failure("E3004", 
                    "SSL certificate does not match SMP certificate");
                
            }
            
        } catch (Exception e) {
            logger.warn("SSL certificate matching validation failed for {}: {}", endpointUrl, e.getMessage());
            return ValidationResult.failure("E3005", 
                "SSL certificate matching validation failed: " + e.getMessage());
        }
    }
    
    /**
     * Performs comprehensive endpoint validation including transport profile,
     * URL validation, and optional connectivity testing.
     */
    public ValidationResult validateEndpoint(String endpointUrl, String transportProfile, 
                                           X509Certificate smpCertificate, boolean testConnectivity) {
        logger.debug("Performing comprehensive endpoint validation");
        
        List<ValidationResult> results = new ArrayList<>();
        
        // Validate transport profile
        results.add(validateTransportProfile(transportProfile));
        
        // Validate endpoint URL
        results.add(validateEndpointUrl(endpointUrl));
        
        // Test connectivity if requested
        if (testConnectivity) {
            results.add(testConnectivity(endpointUrl));
        }
        
        // Validate SSL certificate matching if certificate provided
        if (smpCertificate != null) {
            results.add(validateSslCertificateMatching(endpointUrl, smpCertificate));
        }
        
        // Aggregate results
        List<String> errors = new ArrayList<>();
        List<String> successes = new ArrayList<>();
        
        for (ValidationResult result : results) {
            if (result.isValid()) {
                successes.add(result.getMessage());
            } else {
                errors.add(result.getErrorCode() + ": " + result.getMessage());
            }
        }
        
        if (errors.isEmpty()) {
            logger.debug("Comprehensive endpoint validation successful");
            return ValidationResult.success("All endpoint validations passed: " + String.join(", ", successes));
        } else {
            logger.warn("Comprehensive endpoint validation failed: {}", errors);
            return ValidationResult.failure("E5010", 
                "Endpoint validation failed: " + String.join("; ", errors));
        }
    }
}