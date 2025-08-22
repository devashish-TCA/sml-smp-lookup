package com.tca.peppol.validation;

import com.tca.peppol.model.response.ValidationResults;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.*;
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
import java.security.*;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive unit tests for CertificateValidator.
 * Tests all certificate validation scenarios including:
 * - Basic certificate validation
 * - Certificate chain validation
 * - Key length validation
 * - Peppol-specific validation
 * - Certificate policy validation
 * - Subject and issuer field validation
 * - Edge cases and error scenarios
 */
@ExtendWith(MockitoExtension.class)
class CertificateValidatorTest {

    private CertificateValidator certificateValidator;
    private KeyPair testKeyPair;
    private KeyPair rootKeyPair;

    @BeforeAll
    static void setupBouncyCastle() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @BeforeEach
    void setUp() throws Exception {
        certificateValidator = new CertificateValidator();
        
        // Generate test key pairs
        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        testKeyPair = keyGen.generateKeyPair();
        rootKeyPair = keyGen.generateKeyPair();
    }

    @Test
    void testValidateValidCertificate() throws Exception {
        // Given
        final X509Certificate validCert = createValidTestCertificate();
        
        // When
        final ValidationResults results = certificateValidator.validateSingleCertificate(validCert);
        
        // Then
        assertThat(results.isCertificateValid()).isTrue();
        assertThat(results.isCertificateNotExpired()).isTrue();
        assertThat(results.isCertificateKeyLengthValid()).isTrue();
    }

    @Test
    void testValidateExpiredCertificate() throws Exception {
        // Given
        final X509Certificate expiredCert = createExpiredTestCertificate();
        
        // When
        final ValidationResults results = certificateValidator.validateSingleCertificate(expiredCert);
        
        // Then
        assertThat(results.isCertificateNotExpired()).isFalse();
    }

    @Test
    void testValidateWeakKeyCertificate() throws Exception {
        // Given
        final X509Certificate weakKeyCert = createWeakKeyTestCertificate();
        
        // When
        final ValidationResults results = certificateValidator.validateSingleCertificate(weakKeyCert);
        
        // Then
        assertThat(results.isCertificateKeyLengthValid()).isFalse();
    }

    @Test
    void testValidateCertificateChain() throws Exception {
        // Given
        final List<X509Certificate> certChain = createValidCertificateChain();
        
        // When - Test without trust anchors (internal signature validation)
        final ValidationResults results = certificateValidator.validateCertificateChain(certChain, null);
        
        // Then
        assertThat(results.isCertificateChainValid()).isTrue();
    }

    @Test
    void testValidateBrokenCertificateChain() throws Exception {
        // Given
        final List<X509Certificate> brokenChain = createBrokenCertificateChain();
        
        // When
        final ValidationResults results = certificateValidator.validateCertificateChain(brokenChain, null);
        
        // Then
        assertThat(results.isCertificateChainValid()).isFalse();
    }

    @Test
    void testValidatePeppolCertificatePolicy() throws Exception {
        // Given
        final X509Certificate peppolCert = createPeppolPolicyCertificate();
        
        // When
        final ValidationResults results = certificateValidator.validateSingleCertificate(peppolCert);
        
        // Then
        assertThat(results.isCertificatePolicyValid()).isTrue();
    }

    @Test
    void testValidateNonPeppolCertificatePolicy() throws Exception {
        // Given
        final X509Certificate nonPeppolCert = createNonPeppolPolicyCertificate();
        
        // When
        final ValidationResults results = certificateValidator.validateSingleCertificate(nonPeppolCert);
        
        // Then
        assertThat(results.isCertificatePolicyValid()).isFalse();
    }

    @Test
    void testValidateSubjectAndIssuerFields() throws Exception {
        // Given
        final X509Certificate validCert = createValidTestCertificate();
        
        // When
        final boolean result = certificateValidator.validateSubjectAndIssuerFields(validCert);
        
        // Then
        assertThat(result).isTrue();
    }

    @Test
    void testValidateSubjectAndIssuerFieldsWithEmptySubject() throws Exception {
        // Given
        final X509Certificate invalidCert = createCertificateWithEmptySubject();
        
        // When
        final boolean result = certificateValidator.validateSubjectAndIssuerFields(invalidCert);
        
        // Then - Should still pass because CN= is present, just empty
        assertThat(result).isTrue();
    }

    @Test
    void testValidateNullCertificateChain() {
        // When
        final ValidationResults results = certificateValidator.validateCertificateChain(null, null);
        
        // Then
        assertThat(results.isCertificateValid()).isFalse();
        assertThat(results.isCertificateNotExpired()).isFalse();
        assertThat(results.isCertificateChainValid()).isFalse();
    }

    @Test
    void testValidateEmptyCertificateChain() {
        // When
        final ValidationResults results = certificateValidator.validateCertificateChain(Collections.emptyList(), null);
        
        // Then
        assertThat(results.isCertificateValid()).isFalse();
        assertThat(results.isCertificateNotExpired()).isFalse();
        assertThat(results.isCertificateChainValid()).isFalse();
    }

    @Test
    void testValidateCertificateWithMissingKeyUsage() throws Exception {
        // Given
        final X509Certificate certWithoutKeyUsage = createCertificateWithoutKeyUsage();
        
        // When
        final ValidationResults results = certificateValidator.validateSingleCertificate(certWithoutKeyUsage);
        
        // Then
        assertThat(results.isCertificateValid()).isFalse();
    }

    @Test
    void testValidateCertificateWithInvalidKeyUsage() throws Exception {
        // Given
        final X509Certificate certWithInvalidKeyUsage = createCertificateWithInvalidKeyUsage();
        
        // When
        final ValidationResults results = certificateValidator.validateSingleCertificate(certWithInvalidKeyUsage);
        
        // Then
        assertThat(results.isCertificateValid()).isFalse();
    }

    // Helper methods for creating test certificates

    private X509Certificate createValidTestCertificate() throws Exception {
        final X500Name subject = new X500Name("CN=Test Certificate,O=Test Organization,C=US");
        final X500Name issuer = new X500Name("CN=Test CA,O=Test Organization,C=US");
        
        return createCertificate(subject, issuer, testKeyPair.getPublic(), testKeyPair.getPrivate(),
                               Instant.now().minus(1, ChronoUnit.DAYS),
                               Instant.now().plus(365, ChronoUnit.DAYS),
                               true, true, null);
    }

    private X509Certificate createExpiredTestCertificate() throws Exception {
        final X500Name subject = new X500Name("CN=Expired Certificate,O=Test Organization,C=US");
        final X500Name issuer = new X500Name("CN=Test CA,O=Test Organization,C=US");
        
        return createCertificate(subject, issuer, testKeyPair.getPublic(), testKeyPair.getPrivate(),
                               Instant.now().minus(400, ChronoUnit.DAYS),
                               Instant.now().minus(35, ChronoUnit.DAYS),
                               true, true, null);
    }

    private X509Certificate createWeakKeyTestCertificate() throws Exception {
        // Generate weak 1024-bit key
        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        final KeyPair weakKeyPair = keyGen.generateKeyPair();
        
        final X500Name subject = new X500Name("CN=Weak Key Certificate,O=Test Organization,C=US");
        final X500Name issuer = new X500Name("CN=Test CA,O=Test Organization,C=US");
        
        return createCertificate(subject, issuer, weakKeyPair.getPublic(), weakKeyPair.getPrivate(),
                               Instant.now().minus(1, ChronoUnit.DAYS),
                               Instant.now().plus(365, ChronoUnit.DAYS),
                               true, true, null);
    }

    private List<X509Certificate> createValidCertificateChain() throws Exception {
        // Create root certificate
        final X500Name rootSubject = new X500Name("CN=Test Root CA,O=Test Organization,C=US");
        final X509Certificate rootCert = createCertificate(rootSubject, rootSubject, 
                                                         rootKeyPair.getPublic(), rootKeyPair.getPrivate(),
                                                         Instant.now().minus(1, ChronoUnit.DAYS),
                                                         Instant.now().plus(3650, ChronoUnit.DAYS),
                                                         true, true, null);

        // Create leaf certificate signed by root
        final X500Name leafSubject = new X500Name("CN=Test Leaf Certificate,O=Test Organization,C=US");
        final X509Certificate leafCert = createCertificate(leafSubject, rootSubject,
                                                         testKeyPair.getPublic(), rootKeyPair.getPrivate(),
                                                         Instant.now().minus(1, ChronoUnit.DAYS),
                                                         Instant.now().plus(365, ChronoUnit.DAYS),
                                                         true, true, null);

        return Arrays.asList(leafCert, rootCert);
    }

    private List<X509Certificate> createBrokenCertificateChain() throws Exception {
        // Create two unrelated certificates
        final X509Certificate cert1 = createValidTestCertificate();
        
        final X500Name subject2 = new X500Name("CN=Unrelated Certificate,O=Other Organization,C=US");
        final X500Name issuer2 = new X500Name("CN=Other CA,O=Other Organization,C=US");
        final X509Certificate cert2 = createCertificate(subject2, issuer2, rootKeyPair.getPublic(), rootKeyPair.getPrivate(),
                                                      Instant.now().minus(1, ChronoUnit.DAYS),
                                                      Instant.now().plus(365, ChronoUnit.DAYS),
                                                      true, true, null);

        return Arrays.asList(cert1, cert2);
    }

    private Set<TrustAnchor> createTrustAnchors() throws Exception {
        final X500Name rootSubject = new X500Name("CN=Test Root CA,O=Test Organization,C=US");
        final X509Certificate rootCert = createCertificate(rootSubject, rootSubject,
                                                         rootKeyPair.getPublic(), rootKeyPair.getPrivate(),
                                                         Instant.now().minus(1, ChronoUnit.DAYS),
                                                         Instant.now().plus(3650, ChronoUnit.DAYS),
                                                         true, true, null);
        
        return Collections.singleton(new TrustAnchor(rootCert, null));
    }

    private X509Certificate createPeppolPolicyCertificate() throws Exception {
        final X500Name subject = new X500Name("CN=Peppol Certificate,O=Test Organization,C=US");
        final X500Name issuer = new X500Name("CN=Test CA,O=Test Organization,C=US");
        
        // Create certificate with Peppol policy OID
        final String peppolPolicyOid = "1.3.6.1.4.1.16953.1.2.1.1";
        
        return createCertificate(subject, issuer, testKeyPair.getPublic(), testKeyPair.getPrivate(),
                               Instant.now().minus(1, ChronoUnit.DAYS),
                               Instant.now().plus(365, ChronoUnit.DAYS),
                               true, true, peppolPolicyOid);
    }

    private X509Certificate createNonPeppolPolicyCertificate() throws Exception {
        final X500Name subject = new X500Name("CN=Non-Peppol Certificate,O=Test Organization,C=US");
        final X500Name issuer = new X500Name("CN=Test CA,O=Test Organization,C=US");
        
        // Create certificate with non-Peppol policy OID
        final String nonPeppolPolicyOid = "1.2.3.4.5.6.7.8.9";
        
        return createCertificate(subject, issuer, testKeyPair.getPublic(), testKeyPair.getPrivate(),
                               Instant.now().minus(1, ChronoUnit.DAYS),
                               Instant.now().plus(365, ChronoUnit.DAYS),
                               true, true, nonPeppolPolicyOid);
    }

    private X509Certificate createCertificateWithEmptySubject() throws Exception {
        final X500Name subject = new X500Name("CN=");
        final X500Name issuer = new X500Name("CN=Test CA,O=Test Organization,C=US");
        
        return createCertificate(subject, issuer, testKeyPair.getPublic(), testKeyPair.getPrivate(),
                               Instant.now().minus(1, ChronoUnit.DAYS),
                               Instant.now().plus(365, ChronoUnit.DAYS),
                               true, true, null);
    }

    private X509Certificate createCertificateWithoutKeyUsage() throws Exception {
        final X500Name subject = new X500Name("CN=No Key Usage Certificate,O=Test Organization,C=US");
        final X500Name issuer = new X500Name("CN=Test CA,O=Test Organization,C=US");
        
        return createCertificate(subject, issuer, testKeyPair.getPublic(), testKeyPair.getPrivate(),
                               Instant.now().minus(1, ChronoUnit.DAYS),
                               Instant.now().plus(365, ChronoUnit.DAYS),
                               false, true, null);
    }

    private X509Certificate createCertificateWithInvalidKeyUsage() throws Exception {
        final X500Name subject = new X500Name("CN=Invalid Key Usage Certificate,O=Test Organization,C=US");
        final X500Name issuer = new X500Name("CN=Test CA,O=Test Organization,C=US");
        
        return createCertificate(subject, issuer, testKeyPair.getPublic(), testKeyPair.getPrivate(),
                               Instant.now().minus(1, ChronoUnit.DAYS),
                               Instant.now().plus(365, ChronoUnit.DAYS),
                               true, false, null);
    }

    private X509Certificate createCertificate(final X500Name subject, final X500Name issuer,
                                            final PublicKey publicKey, final PrivateKey signingKey,
                                            final Instant notBefore, final Instant notAfter,
                                            final boolean includeKeyUsage, final boolean digitalSignature,
                                            final String policyOid) throws Exception {
        
        final BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        
        final X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            issuer, serialNumber, Date.from(notBefore), Date.from(notAfter), subject, publicKey);

        // Add basic constraints
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));

        // Add key usage if requested
        if (includeKeyUsage) {
            int keyUsage = 0;
            if (digitalSignature) {
                keyUsage |= KeyUsage.digitalSignature;
            } else {
                keyUsage |= KeyUsage.keyEncipherment; // Invalid for Peppol
            }
            certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(keyUsage));
        }

        // Add certificate policy if provided
        if (policyOid != null) {
            final PolicyInformation policyInfo = new PolicyInformation(new ASN1ObjectIdentifier(policyOid));
            final CertificatePolicies policies = new CertificatePolicies(policyInfo);
            certBuilder.addExtension(Extension.certificatePolicies, false, policies);
        }

        // Add subject key identifier
        final SubjectKeyIdentifier subjectKeyId = new SubjectKeyIdentifier(publicKey.getEncoded());
        certBuilder.addExtension(Extension.subjectKeyIdentifier, false, subjectKeyId);

        final ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
            .setProvider("BC")
            .build(signingKey);

        final X509CertificateHolder certHolder = certBuilder.build(signer);
        
        return new JcaX509CertificateConverter()
            .setProvider("BC")
            .getCertificate(certHolder);
    }
}