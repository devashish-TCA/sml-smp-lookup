package com.tca.peppol.service;

import com.tca.peppol.client.CrlResult;
import com.tca.peppol.client.OcspResult;
import com.tca.peppol.model.internal.SmpResult;
import com.tca.peppol.model.response.ValidationResults;
import com.tca.peppol.service.ValidationOrchestrator.ComprehensiveValidationResult;
import com.tca.peppol.service.ValidationOrchestrator.ValidationContext;
import com.tca.peppol.service.ValidationOrchestrator.ValidationOptions;
import com.tca.peppol.validation.CertificateValidator;
import com.tca.peppol.validation.EndpointValidator;
import com.tca.peppol.validation.ValidationResult;
import com.tca.peppol.validation.XmlSignatureValidationResult;
import com.tca.peppol.validation.XmlSignatureValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ValidationOrchestrator.
 * Tests validation workflow orchestration, result aggregation, caching, and error handling.
 */
@ExtendWith(MockitoExtension.class)
class ValidationOrchestratorTest {

    @Mock
    private CertificateValidator certificateValidator;

    @Mock
    private XmlSignatureValidator xmlSignatureValidator;

    @Mock
    private EndpointValidator endpointValidator;

    @Mock
    private CircuitBreakerService circuitBreakerService;

    @Mock
    private X509Certificate mockCertificate;

    @Mock
    private Document mockXmlDocument;

    private ValidationOrchestrator orchestrator;
    private SmpResult smpResult;
    private ValidationContext validationContext;

    @BeforeEach
    void setUp() {
        orchestrator = new ValidationOrchestrator(
            certificateValidator, xmlSignatureValidator, endpointValidator, circuitBreakerService);

        // Create test SMP result
        smpResult = SmpResult.builder()
            .endpointUrl("https://example.com/as4")
            .transportProfile("peppol-transport-as4-v2_0")
            .certificate(mockCertificate)
            .serviceActivationDate(Instant.now().minusSeconds(3600))
            .serviceExpirationDate(Instant.now().plusSeconds(3600))
            .xmlDocument(mockXmlDocument)
            .successful(true)
            .build();

        // Create validation context
        validationContext = new ValidationContext(
            smpResult,
            "9915:test-participant",
            "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice",
            "test",
            ValidationOptions.defaultOptions(),
            "test-correlation-id"
        );
    }

    @Test
    void testSuccessfulValidationOrchestration() {
        // Arrange
        setupSuccessfulValidationMocks();

        // Act
        ComprehensiveValidationResult result = orchestrator.orchestrateValidations(validationContext);

        // Assert
        assertNotNull(result);
        assertTrue(result.isOverallValid());
        assertTrue(result.isPeppolCompliant());
        assertNotNull(result.getValidationResults());
        assertTrue(result.getTotalValidationTimeMs() > 0);
        assertEquals("test-correlation-id", result.getCorrelationId());

        // Verify all validation methods were called
        verify(certificateValidator).validateCertificateChain(anyList(), any());
        try {
            verify(xmlSignatureValidator).validateXmlSignature(any(Document.class), any(X509Certificate.class));
        } catch (Exception e) {
            // Expected in test
        }
        verify(endpointValidator).validateTransportProfile(anyString());
        verify(endpointValidator).validateEndpointUrl(anyString());
        try {
            verify(circuitBreakerService, times(2)).executeWithCircuitBreaker(any(), anyString());
        } catch (Exception e) {
            // Expected in test
        }
    }

    @Test
    void testValidationOrchestrationWithFailedCertificateValidation() {
        // Arrange
        setupFailedCertificateValidationMocks();

        // Act
        ComprehensiveValidationResult result = orchestrator.orchestrateValidations(validationContext);

        // Assert
        assertNotNull(result);
        assertFalse(result.isOverallValid());
        assertFalse(result.isPeppolCompliant());
        
        ValidationResults validationResults = result.getValidationResults();
        assertFalse(validationResults.isCertificateValid());
        assertFalse(validationResults.isCertificateNotExpired());
        
        // Verify certificate validation was called but revocation was skipped due to invalid certificate
        verify(certificateValidator).validateCertificateChain(anyList(), any());
        try {
            verify(circuitBreakerService, never()).executeWithCircuitBreaker(any(), anyString());
        } catch (Exception e) {
            // Expected in test
        }
    }

    @Test
    void testValidationOrchestrationWithXmlSignatureFailure() {
        // Arrange
        setupSuccessfulCertificateValidationMocks();
        setupFailedXmlSignatureValidationMocks();
        setupSuccessfulEndpointValidationMocks();
        setupSuccessfulRevocationValidationMocks();

        // Act
        ComprehensiveValidationResult result = orchestrator.orchestrateValidations(validationContext);

        // Assert
        assertNotNull(result);
        assertFalse(result.isOverallValid());
        assertFalse(result.isPeppolCompliant());
        
        ValidationResults validationResults = result.getValidationResults();
        assertTrue(validationResults.isCertificateValid());
        assertFalse(validationResults.isSmpSignatureValid());
        
        try {
            verify(xmlSignatureValidator).validateXmlSignature(any(Document.class), any(X509Certificate.class));
        } catch (Exception e) {
            // Expected in test
        }
    }

    @Test
    void testValidationOrchestrationWithRevocationFailure() {
        // Arrange
        setupSuccessfulCertificateValidationMocks();
        setupSuccessfulXmlSignatureValidationMocks();
        setupSuccessfulEndpointValidationMocks();
        setupFailedRevocationValidationMocks();

        // Act
        ComprehensiveValidationResult result = orchestrator.orchestrateValidations(validationContext);

        // Assert
        assertNotNull(result);
        assertFalse(result.isOverallValid());
        assertFalse(result.isPeppolCompliant());
        
        ValidationResults validationResults = result.getValidationResults();
        assertTrue(validationResults.isCertificateValid());
        assertTrue(validationResults.isSmpSignatureValid());
        assertFalse(validationResults.isCertificateNotRevoked());
        assertFalse(validationResults.isOcspCheckPassed());
        assertFalse(validationResults.isCrlCheckPassed());
    }

    @Test
    void testValidationOrchestrationWithEndpointFailure() {
        // Arrange
        setupSuccessfulCertificateValidationMocks();
        setupSuccessfulXmlSignatureValidationMocks();
        setupFailedEndpointValidationMocks();
        setupSuccessfulRevocationValidationMocks();

        // Act
        ComprehensiveValidationResult result = orchestrator.orchestrateValidations(validationContext);

        // Assert
        assertNotNull(result);
        assertFalse(result.isOverallValid());
        assertFalse(result.isPeppolCompliant());
        
        ValidationResults validationResults = result.getValidationResults();
        assertTrue(validationResults.isCertificateValid());
        assertTrue(validationResults.isSmpSignatureValid());
        assertFalse(validationResults.isTransportProfileSupported());
        assertFalse(validationResults.isEndpointAccessible());
    }

    @Test
    void testValidationOrchestrationWithSelectiveValidation() {
        // Arrange
        ValidationOptions options = ValidationOptions.defaultOptions()
            .setValidateXmlSignature(false)
            .setCheckRevocation(false);
        
        ValidationContext context = new ValidationContext(
            smpResult, "9915:test-participant", "test-doc-type", "test", options, "test-correlation-id");
        
        setupSuccessfulCertificateValidationMocks();
        setupSuccessfulEndpointValidationMocks();

        // Act
        ComprehensiveValidationResult result = orchestrator.orchestrateValidations(context);

        // Assert
        assertNotNull(result);
        
        // Verify only certificate and endpoint validation were called
        verify(certificateValidator).validateCertificateChain(anyList(), any());
        try {
            verify(xmlSignatureValidator, never()).validateXmlSignature(any(), any());
            verify(circuitBreakerService, never()).executeWithCircuitBreaker(any(), anyString());
        } catch (Exception e) {
            // Expected in test
        }
        verify(endpointValidator).validateTransportProfile(anyString());
        verify(endpointValidator).validateEndpointUrl(anyString());
    }

    @Test
    void testValidationOrchestrationWithFullValidationOptions() {
        // Arrange
        ValidationOptions options = ValidationOptions.fullValidation();
        ValidationContext context = new ValidationContext(
            smpResult, "9915:test-participant", "test-doc-type", "test", options, "test-correlation-id");
        
        setupSuccessfulValidationMocks();
        when(endpointValidator.testConnectivity(anyString()))
            .thenReturn(ValidationResult.success("Endpoint is accessible"));
        when(endpointValidator.validateSslCertificateMatching(anyString(), any(X509Certificate.class)))
            .thenReturn(ValidationResult.success("SSL certificate matches"));

        // Act
        ComprehensiveValidationResult result = orchestrator.orchestrateValidations(context);

        // Assert
        assertNotNull(result);
        assertTrue(result.isOverallValid());
        
        // Verify additional validations were called
        verify(endpointValidator).testConnectivity(anyString());
        verify(endpointValidator).validateSslCertificateMatching(anyString(), any(X509Certificate.class));
        
        Map<String, Object> details = result.getValidationDetails();
        assertTrue((Boolean) details.get("connectivityTested"));
        assertTrue((Boolean) details.get("sslMatchingTested"));
    }

    @Test
    void testValidationOrchestrationWithMissingCertificate() {
        // Arrange
        SmpResult smpResultNoCert = SmpResult.builder()
            .endpointUrl("https://example.com/as4")
            .transportProfile("peppol-transport-as4-v2_0")
            .certificate(null) // No certificate
            .xmlDocument(mockXmlDocument)
            .successful(true)
            .build();
        
        ValidationContext context = new ValidationContext(
            smpResultNoCert, "9915:test-participant", "test-doc-type", "test", 
            ValidationOptions.defaultOptions(), "test-correlation-id");

        // Act
        ComprehensiveValidationResult result = orchestrator.orchestrateValidations(context);

        // Assert
        assertNotNull(result);
        assertFalse(result.isOverallValid());
        assertFalse(result.isPeppolCompliant());
        
        ValidationResults validationResults = result.getValidationResults();
        assertFalse(validationResults.isCertificateValid());
        
        // Verify certificate validator was not called with null certificate
        verify(certificateValidator, never()).validateCertificateChain(anyList(), any());
    }

    @Test
    void testValidationOrchestrationWithMissingXmlDocument() {
        // Arrange
        SmpResult smpResultNoXml = SmpResult.builder()
            .endpointUrl("https://example.com/as4")
            .transportProfile("peppol-transport-as4-v2_0")
            .certificate(mockCertificate)
            .xmlDocument(null) // No XML document
            .successful(true)
            .build();
        
        ValidationContext context = new ValidationContext(
            smpResultNoXml, "9915:test-participant", "test-doc-type", "test", 
            ValidationOptions.defaultOptions(), "test-correlation-id");
        
        setupSuccessfulCertificateValidationMocks();
        setupSuccessfulEndpointValidationMocks();
        setupSuccessfulRevocationValidationMocks();

        // Act
        ComprehensiveValidationResult result = orchestrator.orchestrateValidations(context);

        // Assert
        assertNotNull(result);
        assertFalse(result.isOverallValid());
        assertFalse(result.isPeppolCompliant());
        
        ValidationResults validationResults = result.getValidationResults();
        assertTrue(validationResults.isCertificateValid());
        assertFalse(validationResults.isSmpSignatureValid());
        
        // Verify XML signature validator was not called with null document
        try {
            verify(xmlSignatureValidator, never()).validateXmlSignature(any(), any());
        } catch (Exception e) {
            // Expected in test
        }
    }

    @Test
    void testValidationOrchestrationWithException() {
        // Arrange
        when(certificateValidator.validateCertificateChain(anyList(), any()))
            .thenThrow(new RuntimeException("Certificate validation failed"));

        // Act
        ComprehensiveValidationResult result = orchestrator.orchestrateValidations(validationContext);

        // Assert
        assertNotNull(result);
        assertFalse(result.isOverallValid());
        assertFalse(result.isPeppolCompliant());
        
        Map<String, Object> details = result.getValidationDetails();
        assertTrue(details.containsKey("certificateValidationError"));
        assertEquals("Certificate validation failed", details.get("certificateValidationError"));
    }

    @Test
    void testValidationOrchestrationWithNullContext() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            orchestrator.orchestrateValidations(null);
        });
    }

    @Test
    void testValidationCaching() {
        // Arrange
        setupSuccessfulValidationMocks();
        ValidationOptions options = ValidationOptions.defaultOptions().setUseCache(true);
        ValidationContext context = new ValidationContext(
            smpResult, "9915:test-participant", "test-doc-type", "test", options, "test-correlation-id");

        // Act - First call
        ComprehensiveValidationResult result1 = orchestrator.orchestrateValidations(context);
        
        // Act - Second call (should use cache)
        ComprehensiveValidationResult result2 = orchestrator.orchestrateValidations(context);

        // Assert
        assertNotNull(result1);
        assertNotNull(result2);
        assertTrue(result1.isOverallValid());
        assertTrue(result2.isOverallValid());
        
        // Verify validators were only called once (second call used cache)
        verify(certificateValidator, times(1)).validateCertificateChain(anyList(), any());
        try {
            verify(xmlSignatureValidator, times(1)).validateXmlSignature(any(), any());
        } catch (Exception e) {
            // Expected in test
        }
    }

    @Test
    void testValidationCacheDisabled() {
        // Arrange
        setupSuccessfulValidationMocks();
        ValidationOptions options = ValidationOptions.defaultOptions().setUseCache(false);
        ValidationContext context = new ValidationContext(
            smpResult, "9915:test-participant", "test-doc-type", "test", options, "test-correlation-id");

        // Act - Two calls
        ComprehensiveValidationResult result1 = orchestrator.orchestrateValidations(context);
        ComprehensiveValidationResult result2 = orchestrator.orchestrateValidations(context);

        // Assert
        assertNotNull(result1);
        assertNotNull(result2);
        
        // Verify validators were called twice (cache disabled)
        verify(certificateValidator, times(2)).validateCertificateChain(anyList(), any());
        try {
            verify(xmlSignatureValidator, times(2)).validateXmlSignature(any(), any());
        } catch (Exception e) {
            // Expected in test
        }
    }

    @Test
    void testGetCacheStats() {
        // Act
        Map<String, Object> stats = orchestrator.getCacheStats();

        // Assert
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalEntries"));
        assertTrue(stats.containsKey("expiredEntries"));
        assertTrue(stats.containsKey("maxCacheSize"));
        assertTrue(stats.containsKey("cacheTtlMinutes"));
        
        assertEquals(0, stats.get("totalEntries"));
        assertEquals(0, stats.get("expiredEntries"));
        assertEquals(1000, stats.get("maxCacheSize"));
        assertEquals(30, stats.get("cacheTtlMinutes"));
    }

    @Test
    void testClearCache() {
        // Arrange
        setupSuccessfulValidationMocks();
        ValidationOptions options = ValidationOptions.defaultOptions().setUseCache(true);
        ValidationContext context = new ValidationContext(
            smpResult, "9915:test-participant", "test-doc-type", "test", options, "test-correlation-id");
        
        // Add something to cache
        orchestrator.orchestrateValidations(context);

        // Act
        orchestrator.clearCache();

        // Assert - Second call should not use cache
        orchestrator.orchestrateValidations(context);
        verify(certificateValidator, times(2)).validateCertificateChain(anyList(), any());
    }

    @Test
    void testValidationResultAggregation() {
        // Arrange
        setupMixedValidationResults();

        // Act
        ComprehensiveValidationResult result = orchestrator.orchestrateValidations(validationContext);

        // Assert
        assertNotNull(result);
        ValidationResults validationResults = result.getValidationResults();
        
        // Check that results are properly aggregated
        assertTrue(validationResults.isCertificateValid());
        assertTrue(validationResults.isCertificateNotExpired());
        assertFalse(validationResults.isSmpSignatureValid()); // Failed XML signature
        assertTrue(validationResults.isTransportProfileSupported());
        assertFalse(validationResults.isEndpointAccessible()); // Failed endpoint
        
        // Overall should be false due to failures
        assertFalse(result.isOverallValid());
        assertFalse(result.isPeppolCompliant());
        
        // Check validation details
        Map<String, Object> details = result.getValidationDetails();
        assertTrue(details.containsKey("certificateValidationTimeMs"));
        assertTrue(details.containsKey("xmlSignatureValidationTimeMs"));
        assertTrue(details.containsKey("endpointValidationTimeMs"));
    }

    // Helper methods for setting up mocks

    private void setupSuccessfulValidationMocks() {
        setupSuccessfulCertificateValidationMocks();
        setupSuccessfulXmlSignatureValidationMocks();
        setupSuccessfulEndpointValidationMocks();
        setupSuccessfulRevocationValidationMocks();
    }

    private void setupSuccessfulCertificateValidationMocks() {
        ValidationResults certResults = new ValidationResults();
        certResults.setCertificateValid(true);
        certResults.setCertificateNotExpired(true);
        certResults.setCertificateFromOpenPeppol(true);
        certResults.setCertificateChainValid(true);
        certResults.setCertificateKeyLengthValid(true);
        certResults.setCertificatePolicyValid(true);
        
        when(certificateValidator.validateCertificateChain(anyList(), any()))
            .thenReturn(certResults);
        
        when(mockCertificate.getSubjectX500Principal())
            .thenReturn(new javax.security.auth.x500.X500Principal("CN=Test Certificate"));
        when(mockCertificate.getIssuerX500Principal())
            .thenReturn(new javax.security.auth.x500.X500Principal("CN=Test Issuer"));
    }

    private void setupFailedCertificateValidationMocks() {
        ValidationResults certResults = new ValidationResults();
        certResults.setCertificateValid(false);
        certResults.setCertificateNotExpired(false);
        certResults.setCertificateFromOpenPeppol(false);
        certResults.setCertificateChainValid(false);
        certResults.setCertificateKeyLengthValid(false);
        certResults.setCertificatePolicyValid(false);
        
        when(certificateValidator.validateCertificateChain(anyList(), any()))
            .thenReturn(certResults);
        
        when(mockCertificate.getSubjectX500Principal())
            .thenReturn(new javax.security.auth.x500.X500Principal("CN=Invalid Certificate"));
    }

    private void setupSuccessfulXmlSignatureValidationMocks() {
        XmlSignatureValidationResult xmlResult = XmlSignatureValidationResult.builder()
            .signaturePresent(true)
            .valid(true)
            .signatureAlgorithmValid(true)
            .canonicalizationValid(true)
            .referencesValid(true)
            .keyInfoValid(true)
            .certificateMatches(true)
            .cryptographicSignatureValid(true)
            .signatureAlgorithm("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256")
            .canonicalizationAlgorithm("http://www.w3.org/TR/2001/REC-xml-c14n-20010315")
            .build();
        
        try {
            when(xmlSignatureValidator.validateXmlSignature(any(Document.class), any(X509Certificate.class)))
                .thenReturn(xmlResult);
        } catch (Exception e) {
            // Mock setup exception
        }
    }

    private void setupFailedXmlSignatureValidationMocks() {
        XmlSignatureValidationResult xmlResult = XmlSignatureValidationResult.builder()
            .signaturePresent(true)
            .valid(false)
            .signatureAlgorithmValid(false)
            .canonicalizationValid(false)
            .errorMessage("Invalid XML signature")
            .build();
        
        try {
            when(xmlSignatureValidator.validateXmlSignature(any(Document.class), any(X509Certificate.class)))
                .thenReturn(xmlResult);
        } catch (Exception e) {
            // Mock setup exception
        }
    }

    private void setupSuccessfulEndpointValidationMocks() {
        when(endpointValidator.validateTransportProfile(anyString()))
            .thenReturn(ValidationResult.success("Transport profile is approved"));
        when(endpointValidator.validateEndpointUrl(anyString()))
            .thenReturn(ValidationResult.success("Endpoint URL is valid"));
    }

    private void setupFailedEndpointValidationMocks() {
        when(endpointValidator.validateTransportProfile(anyString()))
            .thenReturn(ValidationResult.failure("E5002", "Transport profile not approved"));
        when(endpointValidator.validateEndpointUrl(anyString()))
            .thenReturn(ValidationResult.failure("E5004", "Endpoint URL must use HTTPS"));
    }

    private void setupSuccessfulRevocationValidationMocks() {
        OcspResult ocspResult = OcspResult.good(Instant.now(), Instant.now().plusSeconds(3600));
        CrlResult crlResult = CrlResult.good("https://crl.example.com", Instant.now(), Instant.now().plusSeconds(3600));
        
        try {
            when(circuitBreakerService.executeWithCircuitBreaker(any(), eq("ocsp-service")))
                .thenReturn(ocspResult);
            when(circuitBreakerService.executeWithCircuitBreaker(any(), eq("crl-service")))
                .thenReturn(crlResult);
        } catch (Exception e) {
            // Mock setup exception
        }
    }

    private void setupFailedRevocationValidationMocks() {
        OcspResult ocspResult = OcspResult.error("OCSP service unavailable");
        CrlResult crlResult = CrlResult.error("https://crl.example.com", "CRL download failed");
        
        try {
            when(circuitBreakerService.executeWithCircuitBreaker(any(), eq("ocsp-service")))
                .thenReturn(ocspResult);
            when(circuitBreakerService.executeWithCircuitBreaker(any(), eq("crl-service")))
                .thenReturn(crlResult);
        } catch (Exception e) {
            // Mock setup exception
        }
    }

    private void setupMixedValidationResults() {
        // Successful certificate validation
        setupSuccessfulCertificateValidationMocks();
        
        // Failed XML signature validation
        setupFailedXmlSignatureValidationMocks();
        
        // Mixed endpoint validation (transport profile OK, endpoint URL failed)
        when(endpointValidator.validateTransportProfile(anyString()))
            .thenReturn(ValidationResult.success("Transport profile is approved"));
        when(endpointValidator.validateEndpointUrl(anyString()))
            .thenReturn(ValidationResult.failure("E5004", "Endpoint URL must use HTTPS"));
        
        // Successful revocation validation
        setupSuccessfulRevocationValidationMocks();
    }
}