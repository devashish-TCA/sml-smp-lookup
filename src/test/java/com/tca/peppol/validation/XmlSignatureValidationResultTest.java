package com.tca.peppol.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for XmlSignatureValidationResult.
 */
class XmlSignatureValidationResultTest {
    
    @Test
    @DisplayName("Should create validation result with builder pattern")
    void shouldCreateValidationResultWithBuilder() throws Exception {
        // Generate test certificate
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        
        // Create mock certificate (simplified for testing)
        X509Certificate mockCert = null; // In real tests, this would be a proper certificate
        
        XmlSignatureValidationResult result = XmlSignatureValidationResult.builder()
            .signaturePresent(true)
            .valid(true)
            .signatureAlgorithmValid(true)
            .canonicalizationValid(true)
            .referencesValid(true)
            .keyInfoValid(true)
            .certificateMatches(true)
            .cryptographicSignatureValid(true)
            .signingCertificate(mockCert)
            .signatureAlgorithm("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256")
            .canonicalizationAlgorithm("http://www.w3.org/TR/2001/REC-xml-c14n-20010315")
            .errorMessage(null)
            .build();
        
        assertThat(result.isSignaturePresent()).isTrue();
        assertThat(result.isValid()).isTrue();
        assertThat(result.isSignatureAlgorithmValid()).isTrue();
        assertThat(result.isCanonicalizationValid()).isTrue();
        assertThat(result.isReferencesValid()).isTrue();
        assertThat(result.isKeyInfoValid()).isTrue();
        assertThat(result.isCertificateMatches()).isTrue();
        assertThat(result.isCryptographicSignatureValid()).isTrue();
        assertThat(result.getSigningCertificate()).isEqualTo(mockCert);
        assertThat(result.getSignatureAlgorithm()).isEqualTo("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
        assertThat(result.getCanonicalizationAlgorithm()).isEqualTo("http://www.w3.org/TR/2001/REC-xml-c14n-20010315");
        assertThat(result.getErrorMessage()).isNull();
    }
    
    @Test
    @DisplayName("Should create failed validation result")
    void shouldCreateFailedValidationResult() {
        XmlSignatureValidationResult result = XmlSignatureValidationResult.builder()
            .signaturePresent(true)
            .valid(false)
            .signatureAlgorithmValid(false)
            .canonicalizationValid(true)
            .referencesValid(false)
            .keyInfoValid(true)
            .certificateMatches(false)
            .cryptographicSignatureValid(false)
            .errorMessage("Signature validation failed")
            .build();
        
        assertThat(result.isSignaturePresent()).isTrue();
        assertThat(result.isValid()).isFalse();
        assertThat(result.isSignatureAlgorithmValid()).isFalse();
        assertThat(result.isCanonicalizationValid()).isTrue();
        assertThat(result.isReferencesValid()).isFalse();
        assertThat(result.isKeyInfoValid()).isTrue();
        assertThat(result.isCertificateMatches()).isFalse();
        assertThat(result.isCryptographicSignatureValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Signature validation failed");
    }
    
    @Test
    @DisplayName("Should create result for missing signature")
    void shouldCreateResultForMissingSignature() {
        XmlSignatureValidationResult result = XmlSignatureValidationResult.builder()
            .signaturePresent(false)
            .valid(false)
            .errorMessage("No XML signatures found in document")
            .build();
        
        assertThat(result.isSignaturePresent()).isFalse();
        assertThat(result.isValid()).isFalse();
        assertThat(result.isSignatureAlgorithmValid()).isFalse();
        assertThat(result.isCanonicalizationValid()).isFalse();
        assertThat(result.isReferencesValid()).isFalse();
        assertThat(result.isKeyInfoValid()).isFalse();
        assertThat(result.isCertificateMatches()).isFalse();
        assertThat(result.isCryptographicSignatureValid()).isFalse();
        assertThat(result.getSigningCertificate()).isNull();
        assertThat(result.getSignatureAlgorithm()).isNull();
        assertThat(result.getCanonicalizationAlgorithm()).isNull();
        assertThat(result.getErrorMessage()).isEqualTo("No XML signatures found in document");
    }
    
    @Test
    @DisplayName("Should have meaningful toString representation")
    void shouldHaveMeaningfulToStringRepresentation() {
        XmlSignatureValidationResult result = XmlSignatureValidationResult.builder()
            .signaturePresent(true)
            .valid(false)
            .signatureAlgorithmValid(false)
            .canonicalizationValid(true)
            .referencesValid(true)
            .keyInfoValid(true)
            .certificateMatches(false)
            .cryptographicSignatureValid(false)
            .signatureAlgorithm("http://www.w3.org/2000/09/xmldsig#rsa-md5")
            .canonicalizationAlgorithm("http://www.w3.org/TR/2001/REC-xml-c14n-20010315")
            .errorMessage("Weak signature algorithm")
            .build();
        
        String toString = result.toString();
        
        assertThat(toString).contains("XmlSignatureValidationResult");
        assertThat(toString).contains("signaturePresent=true");
        assertThat(toString).contains("valid=false");
        assertThat(toString).contains("signatureAlgorithmValid=false");
        assertThat(toString).contains("canonicalizationValid=true");
        assertThat(toString).contains("referencesValid=true");
        assertThat(toString).contains("keyInfoValid=true");
        assertThat(toString).contains("certificateMatches=false");
        assertThat(toString).contains("cryptographicSignatureValid=false");
        assertThat(toString).contains("signatureAlgorithm='http://www.w3.org/2000/09/xmldsig#rsa-md5'");
        assertThat(toString).contains("canonicalizationAlgorithm='http://www.w3.org/TR/2001/REC-xml-c14n-20010315'");
        assertThat(toString).contains("errorMessage='Weak signature algorithm'");
    }
    
    @Test
    @DisplayName("Should handle null values gracefully")
    void shouldHandleNullValuesGracefully() {
        XmlSignatureValidationResult result = XmlSignatureValidationResult.builder()
            .signaturePresent(false)
            .valid(false)
            .signingCertificate(null)
            .signatureAlgorithm(null)
            .canonicalizationAlgorithm(null)
            .errorMessage(null)
            .build();
        
        assertThat(result.getSigningCertificate()).isNull();
        assertThat(result.getSignatureAlgorithm()).isNull();
        assertThat(result.getCanonicalizationAlgorithm()).isNull();
        assertThat(result.getErrorMessage()).isNull();
        
        // toString should handle nulls gracefully
        String toString = result.toString();
        assertThat(toString).contains("signatureAlgorithm='null'");
        assertThat(toString).contains("canonicalizationAlgorithm='null'");
        assertThat(toString).contains("errorMessage='null'");
    }
    
    @Test
    @DisplayName("Should support method chaining in builder")
    void shouldSupportMethodChainingInBuilder() {
        XmlSignatureValidationResult result = XmlSignatureValidationResult.builder()
            .signaturePresent(true)
            .valid(true)
            .signatureAlgorithmValid(true)
            .canonicalizationValid(true)
            .referencesValid(true)
            .keyInfoValid(true)
            .certificateMatches(true)
            .cryptographicSignatureValid(true)
            .build();
        
        assertThat(result.isSignaturePresent()).isTrue();
        assertThat(result.isValid()).isTrue();
        assertThat(result.isSignatureAlgorithmValid()).isTrue();
        assertThat(result.isCanonicalizationValid()).isTrue();
        assertThat(result.isReferencesValid()).isTrue();
        assertThat(result.isKeyInfoValid()).isTrue();
        assertThat(result.isCertificateMatches()).isTrue();
        assertThat(result.isCryptographicSignatureValid()).isTrue();
    }
}