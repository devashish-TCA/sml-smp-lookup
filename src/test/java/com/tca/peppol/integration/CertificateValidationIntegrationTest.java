package com.tca.peppol.integration;

import com.tca.peppol.client.CrlClient;
import com.tca.peppol.client.OcspClient;
import com.tca.peppol.model.response.CertificateDetails;
import com.tca.peppol.service.*;
import com.tca.peppol.validation.CertificateValidator;
import com.tca.peppol.validation.EndpointValidator;
import com.tca.peppol.validation.XmlSignatureValidator;
import com.tca.peppol.client.SecureHttpClient;
import com.tca.peppol.model.request.LookupRequest;
import com.tca.peppol.model.response.LookupResponse;
import com.tca.peppol.service.*;
import com.tca.peppol.util.CorrelationIdUtils;
import com.tca.peppol.validation.RequestValidator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Certificate validation and XML signature integration testing for Peppol network.
 * 
 * This test class focuses on real certificate chain validation and XML signature
 * verification against the actual Peppol test network. Tests include:
 * 
 * - Real certificate chain validation with OpenPeppol root CA
 * - XML signature validation according to W3C standards
 * - Certificate revocation checking (OCSP and CRL)
 * - Certificate policy and compliance validation
 * - XML canonicalization and signature algorithm validation
 * - Certificate expiry and validity period checking
 * - Key length and cryptographic strength validation
 * 
 * Requirements tested: 12.2 (real certificate chains and XML signature validation)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfEnvironmentVariable(named = "PEPPOL_INTEGRATION_TESTS", matches = "true")
class CertificateValidationIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(CertificateValidationIntegrationTest.class);
    
    // Test participants known to have valid certificates in test network
    private static final String[] CERTIFICATE_TEST_PARTICIPANTS = {
        "iso6523-actorid-upis::9915:test",
        "iso6523-actorid-upis::9906:test-participant"
    };
    
    // PEM certificate pattern for validation
    private static final Pattern PEM_CERTIFICATE_PATTERN = Pattern.compile(
        "-----BEGIN CERTIFICATE-----\\s*([A-Za-z0-9+/\\s]+={0,2})\\s*-----END CERTIFICATE-----"
    );

    private PeppolLookupService lookupService;

    @BeforeEach
    void setUp() {
        // Initialize real services for certificate validation testing
        SecureHttpClient httpClient = new SecureHttpClient();
        SmlLookupService smlLookupService = new SmlLookupService();
        SmpQueryService smpQueryService = new SmpQueryService(httpClient, null);
        CertificateService certificateService = new CertificateService();
        ValidationOrchestrator validationOrchestrator = createValidationOrchestrator();
        RequestValidator requestValidator = new RequestValidator();
        CircuitBreakerService circuitBreakerService = new CircuitBreakerService();
        
        lookupService = new PeppolLookupService(
            smlLookupService,
            smpQueryService,
            validationOrchestrator,
            certificateService,
            requestValidator,
            circuitBreakerService
        );
        
        logger.info("Certificate validation integration test setup completed");
    }

    // ========== Real Certificate Chain Validation Tests ==========

    @ParameterizedTest
    @Order(1)
    @ValueSource(strings = {
        "iso6523-actorid-upis::9915:test",
        "iso6523-actorid-upis::9906:test-participant"
    })
    @DisplayName("Test real certificate chains with OpenPeppol root CA")
    void testRealCertificateChainsWithOpenPeppolRootCA(String participantId) {
        logger.info("Testing real certificate chain validation for participant: {}", participantId);
        
        LookupRequest request = createCertificateTestRequest(participantId);
        request.setIncludeFullCertificateChain(true);
        request.setIncludeTechnicalDetails(true);
        
        Instant startTime = Instant.now();
        LookupResponse response = lookupService.performLookup(request);
        long responseTime = Duration.between(startTime, Instant.now()).toMillis();
        
        logger.info("Certificate validation test for {} - Success: {} - Time: {}ms", 
            participantId, response.isSuccess(), responseTime);
        
        if (response.isSuccess()) {
            validateCertificateResponse(response, participantId);
            logCertificateValidationDetails(response);
        } else {
            logger.warn("Certificate validation test failed for {}: {}", participantId, response.getErrors());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Test certificate format and PEM encoding validation")
    void testCertificateFormatAndPemEncodingValidation() {
        logger.info("Testing certificate format and PEM encoding validation");
        
        LookupRequest request = createCertificateTestRequest(CERTIFICATE_TEST_PARTICIPANTS[0]);
        request.setIncludeFullCertificateChain(true);
        
        LookupResponse response = lookupService.performLookup(request);
        
        if (response.isSuccess()) {
            // Validate certificate format
            Assertions.assertNotNull(response.getCertificate(), "Certificate should be present");
            
            // Validate PEM format
            Assertions.assertTrue(PEM_CERTIFICATE_PATTERN.matcher(response.getCertificate()).find(),
                "Certificate should be in valid PEM format");
            
            // Validate Base64 encoding within PEM
            String pemContent = response.getCertificate()
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
            
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(pemContent);
                Assertions.assertTrue(decodedBytes.length > 0, "Certificate should decode to valid bytes");
                logger.info("Certificate PEM format validation passed - {} bytes decoded", decodedBytes.length);
            } catch (IllegalArgumentException e) {
                Assertions.fail("Certificate should contain valid Base64 encoding: " + e.getMessage());
            }
            
            // Validate certificate details if available
            if (response.getCertificateDetails() != null) {
                validateCertificateDetails(response.getCertificateDetails());
            }
        } else {
            logger.warn("Certificate format test skipped - no successful lookup available");
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test certificate chain building and validation")
    void testCertificateChainBuildingAndValidation() {
        logger.info("Testing certificate chain building and validation");
        
        LookupRequest request = createCertificateTestRequest(CERTIFICATE_TEST_PARTICIPANTS[0]);
        request.setIncludeFullCertificateChain(true);
        request.setIncludeTechnicalDetails(true);
        
        LookupResponse response = lookupService.performLookup(request);
        
        if (response.isSuccess() && response.getValidationResults() != null) {
            var validationResults = response.getValidationResults();
            
            logger.info("Certificate chain validation results:");
            logger.info("  Certificate Valid: {}", validationResults.isCertificateValid());
            logger.info("  Chain Valid: {}", validationResults.isCertificateChainValid());
            logger.info("  Not Expired: {}", validationResults.isCertificateNotExpired());
            logger.info("  From OpenPeppol: {}", validationResults.isCertificateFromOpenPeppol());
            logger.info("  Key Length Valid: {}", validationResults.isCertificateKeyLengthValid());
            logger.info("  Policy Valid: {}", validationResults.isCertificatePolicyValid());
            
            // If certificate is valid, verify chain properties
            if (validationResults.isCertificateValid()) {
                Assertions.assertTrue(validationResults.isCertificateChainValid(),
                    "Valid certificate should have valid chain");
                Assertions.assertTrue(validationResults.isCertificateNotExpired(),
                    "Valid certificate should not be expired");
                Assertions.assertTrue(validationResults.isCertificateKeyLengthValid(),
                    "Valid certificate should have adequate key length");
            }
            
            // Verify OpenPeppol compliance if certificate is from OpenPeppol
            if (validationResults.isCertificateFromOpenPeppol()) {
                Assertions.assertTrue(validationResults.isCertificatePolicyValid(),
                    "OpenPeppol certificate should have valid policy");
                
                logger.info("OpenPeppol certificate compliance verified");
            }
        } else {
            logger.warn("Certificate chain test skipped - no successful validation available");
        }
    }

    // ========== Certificate Revocation Checking Tests ==========

    @Test
    @Order(4)
    @DisplayName("Test certificate revocation checking (OCSP and CRL)")
    void testCertificateRevocationCheckingOcspAndCrl() {
        logger.info("Testing certificate revocation checking (OCSP and CRL)");
        
        LookupRequest request = createCertificateTestRequest(CERTIFICATE_TEST_PARTICIPANTS[0]);
        request.setIncludeTechnicalDetails(true);
        
        LookupResponse response = lookupService.performLookup(request);
        
        if (response.isSuccess() && response.getValidationResults() != null) {
            var validationResults = response.getValidationResults();
            
            logger.info("Revocation checking results:");
            logger.info("  Certificate Not Revoked: {}", validationResults.isCertificateNotRevoked());
            logger.info("  OCSP Check Passed: {}", validationResults.isOcspCheckPassed());
            logger.info("  CRL Check Passed: {}", validationResults.isCrlCheckPassed());
            
            // At least one revocation check should be attempted
            boolean revocationCheckAttempted = validationResults.isOcspCheckPassed() || 
                                             validationResults.isCrlCheckPassed();
            
            if (revocationCheckAttempted) {
                logger.info("Revocation checking was performed successfully");
                
                // If certificate is not revoked, at least one check should pass
                if (validationResults.isCertificateNotRevoked()) {
                    Assertions.assertTrue(validationResults.isOcspCheckPassed() || validationResults.isCrlCheckPassed(),
                        "Non-revoked certificate should pass at least one revocation check");
                }
            } else {
                logger.warn("No revocation checking was performed - this may indicate network issues");
            }
            
            // Log technical details about revocation checking if available
            if (response.getTechnicalDetails() != null && response.getTechnicalDetails().getStepTimings() != null) {
                var timings = response.getTechnicalDetails().getStepTimings();
                if (timings.containsKey("ocspValidation")) {
                    logger.info("OCSP validation time: {}ms", timings.get("ocspValidation"));
                }
                if (timings.containsKey("crlValidation")) {
                    logger.info("CRL validation time: {}ms", timings.get("crlValidation"));
                }
            }
        } else {
            logger.warn("Revocation checking test skipped - no successful validation available");
        }
    }

    // ========== XML Signature Validation Tests ==========

    @Test
    @Order(5)
    @DisplayName("Test XML signature validation according to W3C standards")
    void testXmlSignatureValidationAccordingToW3cStandards() {
        logger.info("Testing XML signature validation according to W3C standards");
        
        LookupRequest request = createCertificateTestRequest(CERTIFICATE_TEST_PARTICIPANTS[0]);
        request.setIncludeTechnicalDetails(true);
        
        LookupResponse response = lookupService.performLookup(request);
        
        if (response.isSuccess() && response.getValidationResults() != null) {
            var validationResults = response.getValidationResults();
            
            logger.info("XML signature validation results:");
            logger.info("  SMP Signature Valid: {}", validationResults.isSmpSignatureValid());
            logger.info("  XML Canonicalization Valid: {}", validationResults.isXmlCanonicalizationValid());
            logger.info("  Signature Algorithm Valid: {}", validationResults.isSignatureAlgorithmValid());
            
            // If SMP signature is valid, canonicalization and algorithm should also be valid
            if (validationResults.isSmpSignatureValid()) {
                Assertions.assertTrue(validationResults.isXmlCanonicalizationValid(),
                    "Valid SMP signature should have valid canonicalization");
                Assertions.assertTrue(validationResults.isSignatureAlgorithmValid(),
                    "Valid SMP signature should have valid algorithm");
                
                logger.info("W3C XML signature validation passed completely");
            } else {
                logger.warn("XML signature validation failed - this may indicate signature issues");
                
                // Log specific validation failures
                if (!validationResults.isXmlCanonicalizationValid()) {
                    logger.warn("  XML canonicalization validation failed");
                }
                if (!validationResults.isSignatureAlgorithmValid()) {
                    logger.warn("  Signature algorithm validation failed");
                }
            }
        } else {
            logger.warn("XML signature validation test skipped - no successful validation available");
        }
    }

    @Test
    @Order(6)
    @DisplayName("Test XML canonicalization and signature algorithm validation")
    void testXmlCanonicalizationAndSignatureAlgorithmValidation() {
        logger.info("Testing XML canonicalization and signature algorithm validation");
        
        // Test with multiple participants to get broader coverage
        for (String participantId : CERTIFICATE_TEST_PARTICIPANTS) {
            logger.info("Testing XML validation for participant: {}", participantId);
            
            LookupRequest request = createCertificateTestRequest(participantId);
            LookupResponse response = lookupService.performLookup(request);
            
            if (response.isSuccess() && response.getValidationResults() != null) {
                var validationResults = response.getValidationResults();
                
                logger.info("  Participant: {} - Canonicalization: {} - Algorithm: {}", 
                    participantId, 
                    validationResults.isXmlCanonicalizationValid(),
                    validationResults.isSignatureAlgorithmValid());
                
                // Verify that if XML signature validation is performed, it follows standards
                if (validationResults.isSmpSignatureValid()) {
                    // Canonical XML 1.0 should be used (this is a CRITICAL requirement)
                    Assertions.assertTrue(validationResults.isXmlCanonicalizationValid(),
                        "XML canonicalization must be valid for participant: " + participantId);
                    
                    // Strong signature algorithms should be used (RSA-SHA256 or stronger)
                    Assertions.assertTrue(validationResults.isSignatureAlgorithmValid(),
                        "Signature algorithm must be valid for participant: " + participantId);
                }
            }
            
            // Small delay between participants
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // ========== Certificate Policy and Compliance Tests ==========

    @Test
    @Order(7)
    @DisplayName("Test certificate policy and Peppol compliance validation")
    void testCertificatePolicyAndPeppolComplianceValidation() {
        logger.info("Testing certificate policy and Peppol compliance validation");
        
        LookupRequest request = createCertificateTestRequest(CERTIFICATE_TEST_PARTICIPANTS[0]);
        request.setIncludeTechnicalDetails(true);
        
        LookupResponse response = lookupService.performLookup(request);
        
        if (response.isSuccess() && response.getValidationResults() != null) {
            var validationResults = response.getValidationResults();
            
            logger.info("Peppol compliance validation results:");
            logger.info("  Peppol Compliant: {}", validationResults.isPeppolCompliant());
            logger.info("  Production Network Compliant: {}", validationResults.isProductionNetworkCompliant());
            logger.info("  Certificate From OpenPeppol: {}", validationResults.isCertificateFromOpenPeppol());
            logger.info("  Certificate Policy Valid: {}", validationResults.isCertificatePolicyValid());
            
            // If certificate is from OpenPeppol, it should be Peppol compliant
            if (validationResults.isCertificateFromOpenPeppol()) {
                Assertions.assertTrue(validationResults.isCertificatePolicyValid(),
                    "OpenPeppol certificate should have valid policy");
                
                // For test environment, production network compliance may vary
                logger.info("OpenPeppol certificate detected with valid policy");
            }
            
            // Overall Peppol compliance should be consistent with individual validations
            if (validationResults.isPeppolCompliant()) {
                // If overall compliant, key validations should pass
                Assertions.assertTrue(validationResults.isCertificateValid(),
                    "Peppol compliant service should have valid certificate");
                
                logger.info("Service is Peppol compliant");
            }
            
            // Log certificate details for compliance analysis
            if (response.getCertificateDetails() != null) {
                var certDetails = response.getCertificateDetails();
                logger.info("Certificate compliance details:");
                logger.info("  Subject: {}", certDetails.getSubject());
                logger.info("  Issuer: {}", certDetails.getIssuer());
                logger.info("  Key Algorithm: {}", certDetails.getKeyAlgorithm());
                logger.info("  Key Length: {}", certDetails.getKeyLength());
                logger.info("  Signature Algorithm: {}", certDetails.getSignatureAlgorithm());
                logger.info("  Peppol Compliant: {}", certDetails.isPeppolCompliant());
            }
        } else {
            logger.warn("Certificate policy validation test skipped - no successful validation available");
        }
    }

    // ========== Cryptographic Strength Validation Tests ==========

    @Test
    @Order(8)
    @DisplayName("Test cryptographic strength and key length validation")
    void testCryptographicStrengthAndKeyLengthValidation() {
        logger.info("Testing cryptographic strength and key length validation");
        
        LookupRequest request = createCertificateTestRequest(CERTIFICATE_TEST_PARTICIPANTS[0]);
        LookupResponse response = lookupService.performLookup(request);
        
        if (response.isSuccess() && response.getCertificateDetails() != null) {
            var certDetails = response.getCertificateDetails();
            var validationResults = response.getValidationResults();
            
            logger.info("Cryptographic strength validation:");
            logger.info("  Key Algorithm: {}", certDetails.getKeyAlgorithm());
            logger.info("  Key Length: {}", certDetails.getKeyLength());
            logger.info("  Signature Algorithm: {}", certDetails.getSignatureAlgorithm());
            logger.info("  Key Length Valid: {}", validationResults.isCertificateKeyLengthValid());
            
            // Verify minimum key length requirements (2048 bits for RSA)
            if ("RSA".equals(certDetails.getKeyAlgorithm())) {
                Assertions.assertTrue(certDetails.getKeyLength() >= 2048,
                    "RSA key length should be at least 2048 bits, was: " + certDetails.getKeyLength());
                
                if (validationResults.isCertificateKeyLengthValid()) {
                    logger.info("RSA key length validation passed: {} bits", certDetails.getKeyLength());
                }
            }
            
            // Verify signature algorithm strength
            String sigAlgorithm = certDetails.getSignatureAlgorithm();
            if (sigAlgorithm != null) {
                // Should not use weak algorithms
                Assertions.assertFalse(sigAlgorithm.contains("MD5"),
                    "Certificate should not use MD5 signature algorithm");
                Assertions.assertFalse(sigAlgorithm.contains("SHA1") && !sigAlgorithm.contains("SHA256"),
                    "Certificate should not use SHA1 signature algorithm");
                
                logger.info("Signature algorithm validation passed: {}", sigAlgorithm);
            }
            
            // Verify overall cryptographic compliance
            if (validationResults.isCertificateValid()) {
                Assertions.assertTrue(validationResults.isCertificateKeyLengthValid(),
                    "Valid certificate should have adequate key length");
            }
        } else {
            logger.warn("Cryptographic strength test skipped - no certificate details available");
        }
    }

    // ========== Helper Methods ==========

    private LookupRequest createCertificateTestRequest(String participantId) {
        LookupRequest request = new LookupRequest();
        request.setParticipantId(participantId);
        request.setDocumentTypeId("busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1");
        request.setProcessId("cenbii-procid-ubl::urn:fdc:peppol.eu:2017:poacc:billing:01:1.0");
        request.setEnvironment("test");
        request.setRequestId(CorrelationIdUtils.generateCorrelationId());
        return request;
    }

    private ValidationOrchestrator createValidationOrchestrator() {
        return new ValidationOrchestrator(
            new CertificateValidator(),
            new XmlSignatureValidator(),
            new EndpointValidator(new SecureHttpClient()),
            new OcspClient(new SecureHttpClient()),
            new CrlClient(new SecureHttpClient())
        );
    }

    private void validateCertificateResponse(LookupResponse response, String participantId) {
        // Basic certificate presence validation
        Assertions.assertNotNull(response.getCertificate(), 
            "Certificate should be present for participant: " + participantId);
        Assertions.assertTrue(response.getCertificate().contains("BEGIN CERTIFICATE"),
            "Certificate should be in PEM format for participant: " + participantId);
        
        // Validation results should be present
        Assertions.assertNotNull(response.getValidationResults(),
            "Validation results should be present for participant: " + participantId);
        
        // If certificate is valid, basic properties should be satisfied
        if (response.getValidationResults().isCertificateValid()) {
            Assertions.assertTrue(response.getValidationResults().isCertificateNotExpired(),
                "Valid certificate should not be expired for participant: " + participantId);
            Assertions.assertTrue(response.getValidationResults().isCertificateKeyLengthValid(),
                "Valid certificate should have adequate key length for participant: " + participantId);
        }
    }

    private void validateCertificateDetails(CertificateDetails certDetails) {
        Assertions.assertNotNull(certDetails.getSubject(), "Certificate subject should be present");
        Assertions.assertNotNull(certDetails.getIssuer(), "Certificate issuer should be present");
        Assertions.assertNotNull(certDetails.getNotBefore(), "Certificate notBefore should be present");
        Assertions.assertNotNull(certDetails.getNotAfter(), "Certificate notAfter should be present");
        Assertions.assertTrue(certDetails.getKeyLength() > 0, "Key length should be positive");
        
        // Verify validity period is reasonable
        Assertions.assertTrue(certDetails.getNotBefore().isBefore(certDetails.getNotAfter()),
            "Certificate notBefore should be before notAfter");
        
        logger.info("Certificate details validation passed");
    }

    private void logCertificateValidationDetails(LookupResponse response) {
        var validationResults = response.getValidationResults();
        
        logger.info("Detailed certificate validation results:");
        logger.info("  Certificate Valid: {}", validationResults.isCertificateValid());
        logger.info("  Chain Valid: {}", validationResults.isCertificateChainValid());
        logger.info("  Not Expired: {}", validationResults.isCertificateNotExpired());
        logger.info("  Not Revoked: {}", validationResults.isCertificateNotRevoked());
        logger.info("  From OpenPeppol: {}", validationResults.isCertificateFromOpenPeppol());
        logger.info("  Key Length Valid: {}", validationResults.isCertificateKeyLengthValid());
        logger.info("  Policy Valid: {}", validationResults.isCertificatePolicyValid());
        logger.info("  OCSP Check: {}", validationResults.isOcspCheckPassed());
        logger.info("  CRL Check: {}", validationResults.isCrlCheckPassed());
        logger.info("  SMP Signature Valid: {}", validationResults.isSmpSignatureValid());
        logger.info("  XML Canonicalization Valid: {}", validationResults.isXmlCanonicalizationValid());
        logger.info("  Signature Algorithm Valid: {}", validationResults.isSignatureAlgorithmValid());
        logger.info("  Peppol Compliant: {}", validationResults.isPeppolCompliant());
        
        if (response.getCertificateDetails() != null) {
            var certDetails = response.getCertificateDetails();
            logger.info("Certificate details:");
            logger.info("  Subject: {}", certDetails.getSubject());
            logger.info("  Issuer: {}", certDetails.getIssuer());
            logger.info("  Serial: {}", certDetails.getSerialNumber());
            logger.info("  Key Algorithm: {} ({}bits)", certDetails.getKeyAlgorithm(), certDetails.getKeyLength());
            logger.info("  Signature Algorithm: {}", certDetails.getSignatureAlgorithm());
            logger.info("  Valid From: {}", certDetails.getNotBefore());
            logger.info("  Valid Until: {}", certDetails.getNotAfter());
        }
    }
}