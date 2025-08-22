package com.tca.peppol.service;

import com.tca.peppol.model.request.LookupRequest;
import com.tca.peppol.model.response.LookupResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PeppolLookupService
 */
class PeppolLookupServiceTest {

    private PeppolLookupService peppolLookupService;

    @BeforeEach
    void setUp() {
        peppolLookupService = new PeppolLookupService();
    }

    @Test
    void testServiceInitialization() {
        assertThat(peppolLookupService).isNotNull();
        assertThat(peppolLookupService.getSmlLookupService()).isNotNull();
    }

    @Test
    void testPerformLookup_InvalidParticipantId() {
        // Arrange
        LookupRequest request = new LookupRequest();
        request.setParticipantId("invalid-participant-id");
        request.setDocumentTypeId("busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1");
        request.setEnvironment("test");

        // Act
        LookupResponse response = peppolLookupService.performLookup(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isSuccessful()).isFalse();
        assertThat(response.getErrorMessage()).contains("Invalid participant identifier");
        assertThat(response.getParticipantId()).isEqualTo(request.getParticipantId());
        assertThat(response.getDocumentTypeId()).isEqualTo(request.getDocumentTypeId());
        assertThat(response.getEnvironment()).isEqualTo(request.getEnvironment());
        assertThat(response.getTotalProcessingTimeMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testPerformLookup_InvalidDocumentTypeId() {
        // Arrange
        LookupRequest request = new LookupRequest();
        request.setParticipantId("iso6523-actorid-upis::9915:test");
        request.setDocumentTypeId("invalid-document-type");
        request.setEnvironment("test");

        // Act
        LookupResponse response = peppolLookupService.performLookup(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isSuccessful()).isFalse();
        assertThat(response.getErrorMessage()).contains("Invalid document type identifier");
        assertThat(response.getParticipantId()).isEqualTo(request.getParticipantId());
        assertThat(response.getDocumentTypeId()).isEqualTo(request.getDocumentTypeId());
        assertThat(response.getEnvironment()).isEqualTo(request.getEnvironment());
        assertThat(response.getTotalProcessingTimeMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testDefaultConstructor() {
        // Test that the default constructor works
        PeppolLookupService defaultService = new PeppolLookupService();
        assertThat(defaultService).isNotNull();
        assertThat(defaultService.getSmlLookupService()).isNotNull();
    }
}