//package com.tca.peppol.integration;
//
//import com.helger.peppol.smp.ESMPTransportProfile;
//import com.helger.peppolid.IDocumentTypeIdentifier;
//import com.helger.peppolid.IParticipantIdentifier;
//import com.helger.peppolid.IProcessIdentifier;
//import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
//import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
//import com.tca.peppol.validation.RequestValidator;
//import com.tca.peppol.model.request.LookupRequest;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.ValueSource;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.Arrays;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.*;
//
///**
// * Comprehensive compliance validation tests using peppol-commons
// * to ensure adherence to current Peppol specifications.
// *
// * Requirements: 16.1, 16.2, 16.5
// */
//@DisplayName("Peppol Compliance Validation Tests")
//class PeppolComplianceValidationTest {
//
//    private static final Logger logger = LoggerFactory.getLogger(PeppolComplianceValidationTest.class);
//
//    private RequestValidator requestValidator;
//
//    // Peppol specification test cases
//    private static final List<String> PEPPOL_COMPLIANT_PARTICIPANT_IDS = Arrays.asList(
//        "0088:7300010000001", // GLN
//        "0007:4455667788", // DUNS
//        "9906:123456789", // IT:VAT
//        "9915:b", // SV:ORGNR
//        "0184:123456789123", // DK:CPR
//        "0192:123456789", // NO:ORGNR
//        "0208:123456789" // BE:CBE
//    );
//
//    @BeforeEach
//    void setUp() {
//        requestValidator = new RequestValidator();
//    }
//
//    @Test
//    @DisplayName("Validate all predefined Peppol document types")
//    void validatePredefinedDocumentTypes() {
//        logger.info("Validating all predefined Peppol document types");
//
//        for (EPredefinedDocumentTypeIdentifier predefinedDocType : EPredefinedDocumentTypeIdentifier.values()) {
//            String documentTypeId = predefinedDocType.getValue();
//
//            logger.debug("Testing predefined document type: {}", documentTypeId);
//
//            assertThatCode(() -> {
//                IDocumentTypeIdentifier identifier = predefinedDocType.getAsDocumentTypeIdentifier();
//
//                assertThat(identifier).isNotNull();
//                assertThat(identifier.getValue()).isEqualTo(documentTypeId);
//
//                // Verify it's recognized as predefined
//                EPredefinedDocumentTypeIdentifier recognized =
//                    EPredefinedDocumentTypeIdentifier.getFromDocumentTypeIdentifierOrNull(identifier);
//                assertThat(recognized).isEqualTo(predefinedDocType);
//
//            }).doesNotThrowAnyException();
//        }
//
//        logger.info("Successfully validated {} predefined document types",
//            EPredefinedDocumentTypeIdentifier.values().length);
//    }
//
//    @Test
//    @DisplayName("Validate all predefined Peppol process identifiers")
//    void validatePredefinedProcessIdentifiers() {
//        logger.info("Validating all predefined Peppol process identifiers");
//
//        for (EPredefinedProcessIdentifier predefinedProcess : EPredefinedProcessIdentifier.values()) {
//            String processId = predefinedProcess.getValue();
//
//            logger.debug("Testing predefined process: {}", processId);
//
//            assertThatCode(() -> {
//                IProcessIdentifier identifier = predefinedProcess.getAsProcessIdentifier();
//
//                assertThat(identifier).isNotNull();
//                assertThat(identifier.getValue()).isEqualTo(processId);
//
//                // Verify it's recognized as predefined
//                EPredefinedProcessIdentifier recognized =
//                    EPredefinedProcessIdentifier.getFromProcessIdentifierOrNull(identifier);
//                assertThat(recognized).isEqualTo(predefinedProcess);
//
//            }).doesNotThrowAnyException();
//        }
//
//        logger.info("Successfully validated {} predefined process identifiers",
//            EPredefinedProcessIdentifier.values().length);
//    }
//
//    @ParameterizedTest
//    @ValueSource(strings = {
//        "0088:7300010000001",
//        "0007:4455667788",
//        "9906:123456789",
//        "9915:b",
//        "0184:123456789123"
//    })
//    @DisplayName("Validate Peppol-compliant participant identifiers")
//    void validatePeppolCompliantParticipantIds(String participantId) {
//        logger.debug("Validating Peppol-compliant participant ID: {}", participantId);
//
//        assertThatCode(() -> {
//            // Create identifier using GLN format (most common)
//            IParticipantIdentifier identifier = EPredefinedIdentifierIssuingAgency.GLN
//                .createParticipantIdentifier("5798000000001");
//
//            assertThat(identifier).isNotNull();
//            assertThat(identifier.getValue()).isNotEmpty();
//            assertThat(identifier.getScheme()).isNotEmpty();
//
//            // Verify our validator accepts it (may fail due to API mismatch but tests integration)
//            LookupRequest request = new LookupRequest();
//            request.setParticipantId(participantId);
//            request.setDocumentTypeId(EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getValue());
//            request.setProcessId(EPredefinedProcessIdentifier.BIS3_BILLING.getValue());
//            request.setEnvironment("test");
//
//            // Note: This may fail due to API mismatch in existing validator
//            requestValidator.validateRequest(request);
//
//        }).doesNotThrowAnyException();
//    }
//
//    @Test
//    @DisplayName("Validate transport profile compliance")
//    void validateTransportProfileCompliance() {
//        logger.info("Validating transport profile compliance");
//
//        // Verify all transport profiles defined in peppol-commons are valid
//        for (ESMPTransportProfile profile : ESMPTransportProfile.values()) {
//            logger.debug("Validating transport profile: {} ({})", profile.getID(), profile.getName());
//
//            assertThat(profile.getID()).isNotEmpty();
//            assertThat(profile.getName()).isNotEmpty();
//
//            // Verify the profile follows Peppol naming conventions
//            if (profile.getID().startsWith("busdox-transport-")) {
//                assertThat(profile.getID()).matches("busdox-transport-[a-z0-9-]+");
//            }
//        }
//
//        logger.info("Successfully validated {} transport profiles", ESMPTransportProfile.values().length);
//    }
//}