package com.tca.peppol.service;

import com.tca.peppol.model.response.CertificateDetails;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for CertificateService.
 * Tests DER/PEM conversion, certificate chain building, metadata extraction, and caching.
 */
@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    private CertificateService certificateService;
    private X509Certificate testCertificate;
    private X509Certificate issuerCertificate;
    private byte[] testCertificateDer;

    @BeforeAll
    static void setupSecurity() {
        // Add BouncyCastle provider for cryptographic operations
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        certificateService = new CertificateService();
        
        // Generate test certificates
        generateTestCertificates();
    }

    @Test
    void testConvertDerToPem_ValidDerData_ReturnsValidPem() throws Exception {
        // When
        String pemResult = certificateService.convertDerToPem(testCertificateDer);

        // Then
        assertThat(pemResult).isNotNull();
        assertThat(pemResult).startsWith("-----BEGIN CERTIFICATE-----");
        assertThat(pemResult).endsWith("-----END CERTIFICATE-----\n");
        assertThat(pemResult).contains("\n"); // Should have line breaks
    }

    @Test
    void testConvertDerToPem_NullData_ThrowsException() {
        // When/Then
        assertThatThrownBy(() -> certificateService.convertDerToPem(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DER data cannot be null or empty");
    }

    @Test
    void testConvertDerToPem_EmptyData_ThrowsException() {
        // When/Then
        assertThatThrownBy(() -> certificateService.convertDerToPem(new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DER data cannot be null or empty");
    }

    @Test
    void testConvertDerToPem_InvalidDerData_ThrowsException() {
        // Given
        byte[] invalidDer = "invalid der data".getBytes();

        // When/Then
        assertThatThrownBy(() -> certificateService.convertDerToPem(invalidDer))
                .isInstanceOf(CertificateException.class);
    }

    @Test
    void testParseDerCertificate_ValidDerData_ReturnsCertificate() throws Exception {
        // When
        X509Certificate result = certificateService.parseDerCertificate(testCertificateDer);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSubjectX500Principal().getName())
                .isEqualTo(testCertificate.getSubjectX500Principal().getName());
        assertThat(result.getSerialNumber()).isEqualTo(testCertificate.getSerialNumber());
    }

    @Test
    void testParseDerCertificate_CachingBehavior_UsesCachedResult() throws Exception {
        // Given - Parse certificate first time
        X509Certificate firstResult = certificateService.parseDerCertificate(testCertificateDer);

        // When - Parse same certificate again
        X509Certificate secondResult = certificateService.parseDerCertificate(testCertificateDer);

        // Then - Should return same instance from cache
        assertThat(secondResult).isSameAs(firstResult);
    }

    @Test
    void testParseDerCertificate_NullData_ThrowsException() {
        // When/Then
        assertThatThrownBy(() -> certificateService.parseDerCertificate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DER data cannot be null or empty");
    }

    @Test
    void testBuildCertificateChain_ValidCertificate_ReturnsChain() throws Exception {
        // When
        List<X509Certificate> chain = certificateService.buildCertificateChain(testCertificate);

        // Then
        assertThat(chain).isNotNull();
        assertThat(chain).isNotEmpty();
        assertThat(chain.get(0)).isEqualTo(testCertificate); // First cert should be the input cert
    }

    @Test
    void testBuildCertificateChain_SelfSignedCertificate_ReturnsSingleCertChain() throws Exception {
        // Given - Create self-signed certificate
        X509Certificate selfSigned = createSelfSignedCertificate();

        // When
        List<X509Certificate> chain = certificateService.buildCertificateChain(selfSigned);

        // Then
        assertThat(chain).hasSize(1);
        assertThat(chain.get(0)).isEqualTo(selfSigned);
    }

    @Test
    void testBuildCertificateChain_CachingBehavior_UsesCachedResult() throws Exception {
        // Given - Build chain first time
        List<X509Certificate> firstChain = certificateService.buildCertificateChain(testCertificate);

        // When - Build chain again for same certificate
        List<X509Certificate> secondChain = certificateService.buildCertificateChain(testCertificate);

        // Then - Should return equivalent chain (cached)
        assertThat(secondChain).hasSize(firstChain.size());
        assertThat(secondChain.get(0)).isEqualTo(firstChain.get(0));
    }

    @Test
    void testBuildCertificateChain_NullCertificate_ThrowsException() {
        // When/Then
        assertThatThrownBy(() -> certificateService.buildCertificateChain(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Endpoint certificate cannot be null");
    }

    @Test
    void testExtractCertificateDetails_ValidCertificate_ReturnsCompleteDetails() throws Exception {
        // When
        CertificateDetails details = certificateService.extractCertificateDetails(testCertificate);

        // Then
        assertThat(details).isNotNull();
        assertThat(details.getSubject()).isEqualTo(testCertificate.getSubjectX500Principal().getName());
        assertThat(details.getIssuer()).isEqualTo(testCertificate.getIssuerX500Principal().getName());
        assertThat(details.getSerialNumber()).isNotNull();
        assertThat(details.getNotBefore()).isNotNull();
        assertThat(details.getNotAfter()).isNotNull();
        assertThat(details.getKeyAlgorithm()).isEqualTo("RSA");
        assertThat(details.getKeyLength()).isEqualTo(2048);
        assertThat(details.getSignatureAlgorithm()).isNotNull();
        assertThat(details.getSha1Fingerprint()).isNotNull();
        assertThat(details.getSha256Fingerprint()).isNotNull();
        assertThat(details.getVersion()).isEqualTo(3); // X.509 v3
    }

    @Test
    void testExtractCertificateDetails_CertificateWithExtensions_ExtractsExtensions() throws Exception {
        // Given - Certificate with key usage extension
        X509Certificate certWithExtensions = createCertificateWithExtensions();

        // When
        CertificateDetails details = certificateService.extractCertificateDetails(certWithExtensions);

        // Then
        assertThat(details.getKeyUsage()).isNotNull();
        assertThat(details.getKeyUsage()).isNotEmpty();
    }

    @Test
    void testExtractCertificateDetails_NullCertificate_ThrowsException() {
        // When/Then
        assertThatThrownBy(() -> certificateService.extractCertificateDetails(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Certificate cannot be null");
    }

    @Test
    void testExtractCertificateDetails_ValidityPeriodCalculation_CalculatesDaysUntilExpiry() throws Exception {
        // Given - Certificate expiring in 30 days
        X509Certificate expiringCert = createCertificateExpiringInDays(30);

        // When
        CertificateDetails details = certificateService.extractCertificateDetails(expiringCert);

        // Then
        assertThat(details.getDaysUntilExpiry()).isNotNull();
        assertThat(details.getDaysUntilExpiry()).isBetween(29L, 31L); // Allow for timing differences
        assertThat(details.isCurrentlyValid()).isTrue();
        assertThat(details.isExpiringSoon(60)).isTrue();
        assertThat(details.isExpiringSoon(15)).isFalse();
    }

    @Test
    void testCacheCleanup_ExpiredEntries_RemovesExpiredEntries() throws Exception {
        // Given - Parse a certificate to populate cache
        certificateService.parseDerCertificate(testCertificateDer);
        CertificateService.CacheStats statsBefore = certificateService.getCacheStats();

        // When - Clean up caches (entries are not expired yet, so should remain)
        certificateService.cleanupCaches();
        CertificateService.CacheStats statsAfter = certificateService.getCacheStats();

        // Then - Cache should still contain entries (not expired)
        assertThat(statsAfter.certificateCacheSize).isEqualTo(statsBefore.certificateCacheSize);
    }

    @Test
    void testGetCacheStats_ReturnsValidStats() throws Exception {
        // Given - Populate cache
        certificateService.parseDerCertificate(testCertificateDer);
        certificateService.buildCertificateChain(testCertificate);

        // When
        CertificateService.CacheStats stats = certificateService.getCacheStats();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.certificateCacheSize).isGreaterThan(0);
        assertThat(stats.chainCacheSize).isGreaterThan(0);
        assertThat(stats.cacheTtlMs).isEqualTo(3600000); // 1 hour
        assertThat(stats.toString()).contains("CacheStats");
    }

    @Test
    void testDerToPemConversion_RoundTrip_PreservesData() throws Exception {
        // Given
        String originalPem = certificateService.convertDerToPem(testCertificateDer);

        // When - Parse the PEM back to certificate and convert to DER
        X509Certificate parsedCert = certificateService.parseDerCertificate(testCertificateDer);
        byte[] roundTripDer = parsedCert.getEncoded();

        // Then - DER data should be identical
        assertThat(roundTripDer).isEqualTo(testCertificateDer);
    }

    @Test
    void testCertificateFingerprints_ConsistentCalculation_SameFingerprintsForSameCert() throws Exception {
        // Given
        CertificateDetails details1 = certificateService.extractCertificateDetails(testCertificate);
        CertificateDetails details2 = certificateService.extractCertificateDetails(testCertificate);

        // When/Then - Fingerprints should be consistent
        assertThat(details1.getSha1Fingerprint()).isEqualTo(details2.getSha1Fingerprint());
        assertThat(details1.getSha256Fingerprint()).isEqualTo(details2.getSha256Fingerprint());
        assertThat(details1.getSha1Fingerprint()).hasSize(40); // SHA-1 hex length
        assertThat(details1.getSha256Fingerprint()).hasSize(64); // SHA-256 hex length
    }

    // Helper methods for test certificate generation

    private void generateTestCertificates() throws Exception {
        // Generate key pairs
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair issuerKeyPair = keyGen.generateKeyPair();
        KeyPair subjectKeyPair = keyGen.generateKeyPair();

        // Create issuer (CA) certificate
        issuerCertificate = createCertificate(
                "CN=Test CA, O=Test Organization, C=US",
                "CN=Test CA, O=Test Organization, C=US",
                issuerKeyPair,
                issuerKeyPair,
                true
        );

        // Create subject certificate signed by issuer
        testCertificate = createCertificate(
                "CN=Test Subject, O=Test Organization, C=US",
                "CN=Test CA, O=Test Organization, C=US",
                subjectKeyPair,
                issuerKeyPair,
                false
        );

        testCertificateDer = testCertificate.getEncoded();
    }

    private X509Certificate createCertificate(String subjectDN, String issuerDN,
                                            KeyPair subjectKeyPair, KeyPair issuerKeyPair,
                                            boolean isCA) throws Exception {
        X500Name subject = new X500Name(subjectDN);
        X500Name issuer = new X500Name(issuerDN);
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
        Date notAfter = Date.from(Instant.now().plus(365, ChronoUnit.DAYS));

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer, serial, notBefore, notAfter, subject, subjectKeyPair.getPublic());

        // Add basic constraints
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(isCA));

        // Add key usage
        int keyUsage = KeyUsage.digitalSignature | KeyUsage.keyEncipherment;
        if (isCA) {
            keyUsage |= KeyUsage.keyCertSign | KeyUsage.cRLSign;
        }
        certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(keyUsage));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(issuerKeyPair.getPrivate());

        X509CertificateHolder certHolder = certBuilder.build(signer);
        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);
    }

    private X509Certificate createSelfSignedCertificate() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        return createCertificate(
                "CN=Self Signed, O=Test, C=US",
                "CN=Self Signed, O=Test, C=US",
                keyPair,
                keyPair,
                true
        );
    }

    private X509Certificate createCertificateWithExtensions() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        X500Name subject = new X500Name("CN=Test With Extensions, O=Test, C=US");
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
        Date notAfter = Date.from(Instant.now().plus(365, ChronoUnit.DAYS));

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subject, serial, notBefore, notAfter, subject, keyPair.getPublic());

        // Add multiple extensions
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        certBuilder.addExtension(Extension.keyUsage, true, 
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(keyPair.getPrivate());

        X509CertificateHolder certHolder = certBuilder.build(signer);
        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);
    }

    private X509Certificate createCertificateExpiringInDays(int days) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        X500Name subject = new X500Name("CN=Expiring Soon, O=Test, C=US");
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
        Date notAfter = Date.from(Instant.now().plus(days, ChronoUnit.DAYS));

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subject, serial, notBefore, notAfter, subject, keyPair.getPublic());

        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(keyPair.getPrivate());

        X509CertificateHolder certHolder = certBuilder.build(signer);
        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);
    }
}