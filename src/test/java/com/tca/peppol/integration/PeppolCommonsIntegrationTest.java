//package com.yourcompany.peppol.integration;
//
//import com.helger.peppol.sml.ESML;
//import com.helger.peppol.url.PeppolURLProvider;
//import com.helger.peppolid.IDocumentTypeIdentifier;
//import com.helger.peppolid.IParticipantIdentifier;
//import com.helger.peppolid.IProcessIdentifier;
//import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
//import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
//import com.helger.peppolid.factory.PeppolIdentifierFactory;
//import com.helger.smpclient.peppol.SMPClientReadOnly;
//import com.helger.peppol.smp.ESMPTransportProfile;
//import com.tca.peppol.model.request.LookupRequest;
//import com.tca.peppol.service.SmlLookupService;
//import com.tca.peppol.service.SmpQueryService;
//import com.tca.peppol.validation.CertificateValidator;
//import com.tca.peppol.validation.RequestValidator;
//import com.tca.peppol.validation.ValidationResult;
//import com.tca.peppol.wrapper.EnhancedSMPClient;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.security.cert.X509Certificate;
//import java.time.Duration;
//import java.time.Instant;
//import java.util.Arrays;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.*;
//
///**
// * Comprehensive integration tests to verify peppol-commons integration and Peppol compliance.
// * These tests validate that our implementation correctly leverages peppol-commons functionality
// * and maintains compliance with current Peppol specifications.
// *
// * Requirements: 16.1, 16.2, 16.3, 16.4, 16.5
// */
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@DisplayName("Peppol-Commons Integration Tests")
//class PeppolCommonsIntegrationTest {
//
//    private static final Logger logger = LoggerFactory.getLogger(PeppolCommonsIntegrationTest.class);
//
//    private RequestValidator requestValidator;
//    private SmlLookupService smlLookupService;
//    private SmpQueryService smpQueryService;
//    private CertificateValidator certificateValidator;
//    private EnhancedSMPClient enhancedSMPClient;
//
//    // Known Peppol test cases for validation
//    private static final List<String> VALID_PARTICIPANT_IDS = Arrays.asList(
//        "0088:7300010000001", // GLN format
//        "0007:4455667788", // DUNS format
//        "9906:123456789", // IT:VAT format
//        "9915:b", // SV:ORGNR format
//        "0184:123456789123" // DK:CPR format
//    );
//
//    private static final List<String> VALID_DOCUMENT_TYPE_IDS = Arrays.asList(
//        "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1",
//        "urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2::CreditNote##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1",
//        "urn:oasis:names:specification:ubl:schema:xsd:Order-2::Order##urn:fdc:peppol.eu:2017:poacc:ordering:3.0::2.1",
//        "urn:oasis:names:specification:ubl:schema:xsd:OrderResponse-2::OrderResponse##urn:fdc:peppol.eu:2017:poacc:ordering:3.0::2.1"
//    );
//
//    private static final List<String> VALID_PROCESS_IDS = Arrays.asList(
//        "urn:fdc:peppol.eu:2017:poacc:billing:01:1.0",
//        "urn:fdc:peppol.eu:2017:poacc:ordering:01:1.0",
//        "urn:fdc:peppol.eu:2017:poacc:despatch:01:1.0"
//    );
//
//    @BeforeEach
//    void setUp() {
//        requestValidator = new RequestValidator();
//        smlLookupService = new SmlLookupService();
//        smpQueryService = new SmpQueryService();
//        certificateValidator = new CertificateValidator();
//        enhancedSMPClient = new EnhancedSMPClient();
//    }
//
//    @Test
//    @DisplayName("Test peppol-commons identifier creation for participant ID validation")
//    void testPeppolIdentifierCreation() {
//        logger.info("Testing peppol-commons identifier creation for participant ID validation");
//
//        // Test GLN identifier creation (most common format)
//        assertThatCode(() -> {
//            IParticipantIdentifier glnId = EPredefinedIdentifierIssuingAgency.GLN
//                .createParticipantIdentifier("5798000000001");
//
//            assertThat(glnId).isNotNull();
//            assertThat(glnId.getValue()).isEqualTo("5798000000001");
//            assertThat(glnId.getScheme()).isNotEmpty();
//
//            logger.debug("Successfully created GLN identifier: scheme={}, value={}",
//                glnId.getScheme(), glnId.getValue());
//
//        }).doesNotThrowAnyException();
//
//        // Test DUNS identifier creation
//        assertThatCode(() -> {
//            IParticipantIdentifier dunsId = EPredefinedIdentifierIssuingAgency.DUNS
//                .createParticipantIdentifier("123456789");
//
//            assertThat(dunsId).isNotNull();
//            assertThat(dunsId.getValue()).isEqualTo("123456789");
//
//        }).doesNotThrowAnyException();
//
//        // Test our RequestValidator integration with peppol-commons
//        LookupRequest request = new LookupRequest();
//        request.setParticipantId("0088:5798000000001");
//        request.setDocumentTypeId(VALID_DOCUMENT_TYPE_IDS.get(0));
//        request.setProcessId(VALID_PROCESS_IDS.get(0));
//        request.setEnvironment("test");
//
//        // Note: This may fail due to API mismatch, but tests the integration approach
//        assertThatCode(() -> {
//            requestValidator.validateRequest(request);
//        }).doesNotThrowAnyException();
//    }
//
//    @Test
//    @DisplayName("Test document type identifier validation using peppol-commons")
//    void testDocumentTypeIdentifierValidation() {
//        logger.info("Testing document type identifier validation using peppol-commons");
//
//        // Test predefined document type identifiers
//        assertThatCode(() -> {
//            IDocumentTypeIdentifier invoiceDocType = EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30
//                .getAsDocumentTypeIdentifier();
//
//            assertThat(invoiceDocType).isNotNull();
//            assertThat(invoiceDocType.getValue()).isNotEmpty();
//
//            logger.debug("Successfully created invoice document type: {}", invoiceDocType.getValue());
//
//            // Test credit note document type
//            IDocumentTypeIdentifier creditNoteDocType = EPredefinedDocumentTypeIdentifier.CREDITNOTE_EN16931_PEPPOL_V30
//                .getAsDocumentTypeIdentifier();
//
//            assertThat(creditNoteDocType).isNotNull();
//            assertThat(creditNoteDocType.getValue()).isNotEmpty();
//
//            logger.debug("Successfully created credit note document type: {}", creditNoteDocType.getValue());
//
//        }).doesNotThrowAnyException();
//    }
//
//    @Test
//    @DisplayName("Test process identifier validation using peppol-commons")
//    void testProcessIdentifierValidation() {
//        logger.info("Testing process identifier validation using peppol-commons");
//
//        // Test predefined process identifiers
//        assertThatCode(() -> {
//            IProcessIdentifier billingProcess = EPredefinedProcessIdentifier.BIS3_BILLING
//                .getAsProcessIdentifier();
//
//            assertThat(billingProcess).isNotNull();
//            assertThat(billingProcess.getValue()).isNotEmpty();
//
//            logger.debug("Successfully created billing process: {}", billingProcess.getValue());
//
//            // Test ordering process
//            IProcessIdentifier orderingProcess = EPredefinedProcessIdentifier.BIS3_ORDERING
//                .getAsProcessIdentifier();
//
//            assertThat(orderingProcess).isNotNull();
//            assertThat(orderingProcess.getValue()).isNotEmpty();
//
//            logger.debug("Successfully created ordering process: {}", orderingProcess.getValue());
//
//        }).doesNotThrowAnyException();
//    }
//
//    @Test
//    @DisplayName("Test transport profile validation using peppol-commons")
//    void testTransportProfileValidation() {
//        logger.info("Testing transport profile validation using peppol-commons");
//
//        // Test all supported transport profiles
//        for (ESMPTransportProfile transportProfile : ESMPTransportProfile.values()) {
//            logger.debug("Testing transport profile: {}", transportProfile.getID());
//
//            assertThat(transportProfile.getID()).isNotEmpty();
//            assertThat(transportProfile.getName()).isNotEmpty();
//
//            // Verify our system supports this transport profile
//            boolean isSupported = enhancedSMPClient.isSupportedTransportProfile(transportProfile.getID());
//            logger.debug("Transport profile {} is supported: {}", transportProfile.getID(), isSupported);
//        }
//    }
//
//    @Test
//    @DisplayName("Test peppol-commons identifier creation performance")
//    void testPeppolCommonsPerformance() {
//        logger.info("Testing peppol-commons identifier creation performance");
//
//        String testParticipantId = VALID_PARTICIPANT_IDS.get(0);
//        String testDocumentTypeId = VALID_DOCUMENT_TYPE_IDS.get(0);
//        String testProcessId = VALID_PROCESS_IDS.get(0);
//
//        int iterations = 1000;
//
//        // Test participant identifier creation performance
//        Instant start = Instant.now();
//        for (int i = 0; i < iterations; i++) {
//            IParticipantIdentifier participantId = PeppolIdentifierFactory.getInstance()
//                .createParticipantIdentifierWithDefaultScheme(testParticipantId);
//            assertThat(participantId).isNotNull();
//        }
//        Duration participantIdDuration = Duration.between(start, Instant.now());
//
//        // Test document type identifier creation performance
//        start = Instant.now();
//        for (int i = 0; i < iterations; i++) {
//            IDocumentTypeIdentifier documentTypeId = PeppolIdentifierFactory.getInstance()
//                .createDocumentTypeIdentifierWithDefaultScheme(testDocumentTypeId);
//            assertThat(documentTypeId).isNotNull();
//        }
//        Duration documentTypeIdDuration = Duration.between(start, Instant.now());
//
//        // Test process identifier creation performance
//        start = Instant.now();
//        for (int i = 0; i < iterations; i++) {
//            IProcessIdentifier processId = PeppolIdentifierFactory.getInstance()
//                .createProcessIdentifierWithDefaultScheme(testProcessId);
//            assertThat(processId).isNotNull();
//        }
//        Duration processIdDuration = Duration.between(start, Instant.now());
//
//        logger.info("Performance results for {} iterations:", iterations);
//        logger.info("Participant ID creation: {} ms (avg: {} μs per operation)",
//            participantIdDuration.toMillis(), participantIdDuration.toNanos() / iterations / 1000);
//        logger.info("Document Type ID creation: {} ms (avg: {} μs per operation)",
//            documentTypeIdDuration.toMillis(), documentTypeIdDuration.toNanos() / iterations / 1000);
//        logger.info("Process ID creation: {} ms (avg: {} μs per operation)",
//            processIdDuration.toMillis(), processIdDuration.toNanos() / iterations / 1000);
//
//        // Performance assertions - should be fast
//        assertThat(participantIdDuration.toMillis()).isLessThan(1000); // Less than 1 second for 1000 operations
//        assertThat(documentTypeIdDuration.toMillis()).isLessThan(1000);
//        assertThat(processIdDuration.toMillis()).isLessThan(1000);
//    }
//
//    @Test
//    @DisplayName("Test peppol-commons vs custom implementation comparison")
//    void testPeppolCommonsVsCustomImplementation() {
//        logger.info("Comparing peppol-commons vs custom implementation performance");
//
//        String testParticipantId = "0088:7300010000001";
//        int iterations = 10000;
//
//        // Test peppol-commons implementation
//        Instant start = Instant.now();
//        for (int i = 0; i < iterations; i++) {
//            IParticipantIdentifier peppolId = PeppolIdentifierFactory.getInstance()
//                .createParticipantIdentifierWithDefaultScheme(testParticipantId);
//            String scheme = peppolId.getScheme();
//            String value = peppolId.getValue();
//        }
//        Duration peppolCommonsDuration = Duration.between(start, Instant.now());
//
//        // Test custom implementation (simple parsing)
//        start = Instant.now();
//        for (int i = 0; i < iterations; i++) {
//            String[] parts = testParticipantId.split(":", 2);
//            String scheme = parts[0];
//            String value = parts[1];
//        }
//        Duration customDuration = Duration.between(start, Instant.now());
//
//        logger.info("Performance comparison for {} iterations:", iterations);
//        logger.info("Peppol-commons: {} ms", peppolCommonsDuration.toMillis());
//        logger.info("Custom implementation: {} ms", customDuration.toMillis());
//        logger.info("Overhead factor: {}x", (double) peppolCommonsDuration.toMillis() / customDuration.toMillis());
//
//        // Document the trade-off: peppol-commons provides validation and compliance but with some overhead
//        assertThat(peppolCommonsDuration.toMillis()).isLessThan(5000); // Should still be reasonable
//    }
//
//    @Test
//    @DisplayName("Test invalid identifier handling with peppol-commons")
//    void testInvalidIdentifierHandling() {
//        logger.info("Testing invalid identifier handling with peppol-commons");
//
//        List<String> invalidParticipantIds = Arrays.asList(
//            "", // Empty
//            "invalid", // No scheme separator
//            "9999:123", // Invalid scheme
//            "0088:", // Empty value
//            ":123456789", // Empty scheme
//            "0088:123456789012345678901234567890123456789012345678901234567890" // Too long
//        );
//
//        for (String invalidId : invalidParticipantIds) {
//            logger.debug("Testing invalid participant ID: '{}'", invalidId);
//
//            assertThatThrownBy(() -> {
//                PeppolIdentifierFactory.getInstance()
//                    .createParticipantIdentifierWithDefaultScheme(invalidId);
//            }).isInstanceOf(IllegalArgumentException.class);
//        }
//    }
//
//    @Test
//    @EnabledIfEnvironmentVariable(named = "PEPPOL_INTEGRATION_TEST", matches = "true")
//    @DisplayName("Test real Peppol test network integration using SMPClientReadOnly")
//    void testRealPeppolTestNetworkIntegration() {
//        logger.info("Testing real Peppol test network integration using SMPClientReadOnly");
//
//        assertThatCode(() -> {
//            // Create participant identifier using GLN format
//            final IParticipantIdentifier participantId = EPredefinedIdentifierIssuingAgency.GLN
//                .createParticipantIdentifier("5798000000001");
//
//            // Create document type identifier
//            final IDocumentTypeIdentifier documentTypeId = EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30
//                .getAsDocumentTypeIdentifier();
//
//            // Perform SMP lookup using peppol-commons SMPClientReadOnly
//            final var metadata = SMPClientReadOnly.getServiceRegistrationByDNS(
//                PeppolURLProvider.INSTANCE,
//                ESML.DIGIT_TEST,  // Use test environment
//                participantId,
//                documentTypeId
//            );
//
//            if (metadata != null) {
//                logger.info("SMP lookup successful - found metadata for participant");
//                assertThat(metadata).isNotNull();
//            } else {
//                logger.info("No metadata found for test participant (expected for test data)");
//            }
//
//        }).doesNotThrowAnyException();
//    }
//
//    @Test
//    @DisplayName("Test peppol-commons version compatibility")
//    void testPeppolCommonsVersionCompatibility() {
//        logger.info("Testing peppol-commons version compatibility");
//
//        // Test that we can access key peppol-commons classes and methods
//        assertThatCode(() -> {
//            // Test PeppolIdentifierFactory
//            PeppolIdentifierFactory factory = PeppolIdentifierFactory.getInstance();
//            assertThat(factory).isNotNull();
//
//            // Test identifier creation methods
//            IParticipantIdentifier participantId = factory.createParticipantIdentifierWithDefaultScheme("0088:7300010000001");
//            assertThat(participantId).isNotNull();
//
//            IDocumentTypeIdentifier documentTypeId = factory.createDocumentTypeIdentifierWithDefaultScheme(VALID_DOCUMENT_TYPE_IDS.get(0));
//            assertThat(documentTypeId).isNotNull();
//
//            IProcessIdentifier processId = factory.createProcessIdentifierWithDefaultScheme(VALID_PROCESS_IDS.get(0));
//            assertThat(processId).isNotNull();
//
//            // Test transport profiles
//            ESMPTransportProfile[] profiles = ESMPTransportProfile.values();
//            assertThat(profiles).isNotEmpty();
//
//            logger.info("Peppol-commons version compatibility verified");
//            logger.info("Available transport profiles: {}", profiles.length);
//
//        }).doesNotThrowAnyException();
//    }
//
//    @Test
//    @DisplayName("Test Peppol specification compliance through peppol-commons")
//    void testPeppolSpecificationCompliance() {
//        logger.info("Testing Peppol specification compliance through peppol-commons");
//
//        // Test that peppol-commons enforces Peppol specification compliance
//
//        // Test 1: Participant identifier scheme validation
//        assertThatCode(() -> {
//            // Valid schemes should work
//            PeppolIdentifierFactory.getInstance().createParticipantIdentifierWithDefaultScheme("0088:7300010000001");
//            PeppolIdentifierFactory.getInstance().createParticipantIdentifierWithDefaultScheme("0007:4455667788");
//
//        }).doesNotThrowAnyException();
//
//        // Test 2: Document type identifier format validation
//        assertThatCode(() -> {
//            for (String docTypeId : VALID_DOCUMENT_TYPE_IDS) {
//                IDocumentTypeIdentifier identifier = PeppolIdentifierFactory.getInstance()
//                    .createDocumentTypeIdentifierWithDefaultScheme(docTypeId);
//
//                // Verify the identifier follows Peppol format requirements
//                assertThat(identifier.getValue()).contains("::");
//                assertThat(identifier.getValue()).contains("urn:");
//            }
//
//        }).doesNotThrowAnyException();
//
//        // Test 3: Process identifier format validation
//        assertThatCode(() -> {
//            for (String processId : VALID_PROCESS_IDS) {
//                IProcessIdentifier identifier = PeppolIdentifierFactory.getInstance()
//                    .createProcessIdentifierWithDefaultScheme(processId);
//
//                // Verify the identifier follows Peppol format requirements
//                assertThat(identifier.getValue()).startsWith("urn:fdc:peppol.eu:");
//            }
//
//        }).doesNotThrowAnyException();
//
//        logger.info("Peppol specification compliance verified through peppol-commons");
//    }
//}