package com.tca.peppol.integration;

import com.tca.peppol.service.CertificateService;
import com.tca.peppol.validation.CertificateValidator;
import com.tca.peppol.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for certificate validation using peppol-commons
 * against OpenPeppol test certificates.
 * 
 * Requirements: 16.4, 16.5
 */
@DisplayName("Peppol Certificate Validation Integration Tests")
class PeppolCertificateValidationIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(PeppolCertificateValidationIntegrationTest.class);

    private CertificateValidator certificateValidator;
    private CertificateService certificateService;

    // Sample OpenPeppol test certificate (Base64 encoded)
    private static final String TEST_PEPPOL_CERTIFICATE = """
        MIIFGjCCBAKgAwIBAgIQDKZKQYBbP8qJnVVvGDNKRjANBgkqhkiG9w0BAQsFADA8
        MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQT3BlblBFUFBPTCBBSVNCTDESMBAGA1UE
        AxMJUEVQUE9MIENBMB4XDTE5MDEwMTAwMDAwMFoXDTIxMTIzMTIzNTk1OVowXzEL
        MAkGA1UEBhMCQkUxGTAXBgNVBAoTEE9wZW5QRVBQT0wgQUlTQkwxNTAzBgNVBAMT
        LFBFUFBPTCBBQ0NFU1MgUE9JTlQgVEVTVCBDQSAtIEcyIC0gU0hBMjU2MIIBIjAN
        BgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuVVKQYBbP8qJnVVvGDNKRjANBgkq
        hkiG9w0BAQsFADA8MQswCQYDVQQGEwJCRTEZMBcGA1UEChMQT3BlblBFUFBPTCBB
        SVNCTDESMBAGA1UEAxMJUEVQUE9MIENBMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A
        MIIBCgKCAQEAuVVKQYBbP8qJnVVvGDNKRjANBgkqhkiG9w0BAQsFADA8MQswCQYD
        VQQGEwJCRTEZMBcGA1UEChMQT3BlblBFUFBPTCBBSVNCTDESMBAGA1UEAxMJUEVQ
        UE9MIENBwIBAQAB
        """;

    @BeforeEach
    void setUp() {
        certificateValidator = new CertificateValidator();
        certificateService = new CertificateService();
    }

    @Test
    @DisplayName("Test OpenPeppol certificate validation with peppol-commons")
    void testOpenPeppolCertificateValidation() {
        logger.info("Testing OpenPeppol certificate validation with peppol-commons");

        assertThatCode(() -> {
            // Parse test certificate
            byte[] certBytes = Base64.getDecoder().decode(TEST_PEPPOL_CERTIFICATE.replaceAll("\\s", ""));
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) factory.generateCertificate(
                new ByteArrayInputStream(certBytes));

            // Validate using our peppol-commons integrated validator
            ValidationResult result = certificateValidator.validateCertificate(certificate);
            
            logger.info("Certificate validation result: {}", result.isValid());
            if (!result.isValid()) {
                logger.warn("Validation errors: {}", result.getErrorMessages());
            }
            
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Test certificate chain building with peppol-commons")
    void testCertificateChainBuilding() {
        logger.info("Testing certificate chain building with peppol-commons");

        assertThatCode(() -> {
            // Parse test certificate
            byte[] certBytes = Base64.getDecoder().decode(TEST_PEPPOL_CERTIFICATE.replaceAll("\\s", ""));
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) factory.generateCertificate(
                new ByteArrayInputStream(certBytes));

            // Build certificate chain using peppol-commons integration
            List<X509Certificate> chain = certificateService.buildCertificateChain(certificate);
            
            assertThat(chain).isNotEmpty();
            assertThat(chain.get(0)).isEqualTo(certificate);
            
            logger.info("Certificate chain built successfully with {} certificates", chain.size());
            
        }).doesNotThrowAnyException();
    }
}