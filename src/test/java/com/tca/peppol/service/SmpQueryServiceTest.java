package com.tca.peppol.service;

import com.helger.smpclient.exception.SMPClientException;
import com.tca.peppol.client.HttpResponse;
import com.tca.peppol.client.SecureHttpClient;
import com.tca.peppol.model.internal.SmpResult;
import com.tca.peppol.util.XmlSecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmpQueryServiceTest {

    @Mock
    private SecureHttpClient httpClient;
    
    @Mock
    private XmlSecurityUtils xmlSecurityUtils;
    
    private SmpQueryService smpQueryService;
    
    // Test data
    private static final String TEST_SMP_URL = "https://test-smp.example.com";
    private static final String TEST_PARTICIPANT_ID = "0088:1234567890";
    private static final String TEST_DOCUMENT_TYPE_ID = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1";
    private static final String TEST_PROCESS_ID = "urn:fdc:peppol.eu:2017:poacc:billing:01:1.0";
    
    // Sample certificate in Base64 DER format (self-signed test certificate)
    private static final String TEST_CERTIFICATE_BASE64 = "MIICljCCAX4CCQCKmF8xGWXNVDANBgkqhkiG9w0BAQsFADANMQswCQYDVQQGEwJVUzAeFw0yMzEwMTAwMDAwMDBaFw0yNDEwMDkwMDAwMDBaMA0xCzAJBgNVBAYTAlVTMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1234567890";
    
    // Sample SMP XML response
    private static final String SAMPLE_SMP_XML = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<ServiceMetadata xmlns=\"http://busdox.org/serviceMetadata/publishing/1.0/\">\n" +
        "    <ServiceInformation>\n" +
        "        <ParticipantIdentifier scheme=\"iso6523-actorid-upis\">0088:1234567890</ParticipantIdentifier>\n" +
        "        <DocumentIdentifier scheme=\"busdox-docid-qns\">urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1</DocumentIdentifier>\n" +
        "        <ProcessList>\n" +
        "            <Process>\n" +
        "                <ProcessIdentifier scheme=\"cenbii-procid-ubl\">urn:fdc:peppol.eu:2017:poacc:billing:01:1.0</ProcessIdentifier>\n" +
        "                <ServiceEndpointList>\n" +
        "                    <Endpoint transportProfile=\"busdox-transport-as4-v1_0\">\n" +
        "                        <EndpointReference>https://test-endpoint.example.com/as4</EndpointReference>\n" +
        "                        <RequireBusinessLevelSignature>false</RequireBusinessLevelSignature>\n" +
        "                        <Certificate>" + TEST_CERTIFICATE_BASE64 + "</Certificate>\n" +
        "                        <ServiceActivationDate>2023-01-01T00:00:00Z</ServiceActivationDate>\n" +
        "                        <ServiceExpirationDate>2024-12-31T23:59:59Z</ServiceExpirationDate>\n" +
        "                    </Endpoint>\n" +
        "                </ServiceEndpointList>\n" +
        "            </Process>\n" +
        "        </ProcessList>\n" +
        "    </ServiceInformation>\n" +
        "</ServiceMetadata>";

    @BeforeEach
    void setUp() {
        smpQueryService = new SmpQueryService(httpClient, xmlSecurityUtils);
    }

    @Test
    void testQuerySmp_Success() throws Exception {
        // Arrange
        HttpResponse mockResponse = new HttpResponse(200, "OK", SAMPLE_SMP_XML, Map.of(), 100);
        Document mockDocument = createMockDocument();
        
        when(httpClient.get(anyString(), any())).thenReturn(mockResponse);
        when(xmlSecurityUtils.parseSecureXml(anyString())).thenReturn(mockDocument);
        
        // Act
        SmpResult result = smpQueryService.querySmp(
            TEST_SMP_URL, 
            TEST_PARTICIPANT_ID, 
            TEST_DOCUMENT_TYPE_ID, 
            TEST_PROCESS_ID
        );
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.getQueryTimeMs()).isGreaterThan(0);
        assertThat(result.getXmlDocument()).isEqualTo(mockDocument);
        assertThat(result.getParticipantId()).isEqualTo(TEST_PARTICIPANT_ID);
        assertThat(result.getDocumentTypeId()).isEqualTo(TEST_DOCUMENT_TYPE_ID);
        
        // Verify interactions
        verify(httpClient, times(1)).get(anyString(), any());
        verify(xmlSecurityUtils, times(1)).parseSecureXml(SAMPLE_SMP_XML);
    }

    @Test
    void testQuerySmp_HttpError() throws Exception {
        // Arrange
        HttpResponse mockResponse = new HttpResponse(404, "Not Found", "Participant not found", Map.of(), 100);
        
        when(httpClient.get(anyString(), any())).thenReturn(mockResponse);
        
        // Act & Assert
        assertThatThrownBy(() -> smpQueryService.querySmp(
            TEST_SMP_URL, 
            TEST_PARTICIPANT_ID, 
            TEST_DOCUMENT_TYPE_ID, 
            TEST_PROCESS_ID
        ))
        .isInstanceOf(SMPClientException.class)
        .hasMessageContaining("SMP returned HTTP 404");
    }

    @Test
    void testQuerySmp_EmptyResponse() throws Exception {
        // Arrange
        HttpResponse mockResponse = new HttpResponse(200, "OK", "", Map.of(), 100);
        
        when(httpClient.get(anyString(), any())).thenReturn(mockResponse);
        
        // Act & Assert
        assertThatThrownBy(() -> smpQueryService.querySmp(
            TEST_SMP_URL, 
            TEST_PARTICIPANT_ID, 
            TEST_DOCUMENT_TYPE_ID, 
            TEST_PROCESS_ID
        ))
        .isInstanceOf(SMPClientException.class)
        .hasMessageContaining("Empty response from SMP");
    }

    @Test
    void testQuerySmp_XmlParsingError() throws Exception {
        // Arrange
        HttpResponse mockResponse = new HttpResponse(200, "OK", SAMPLE_SMP_XML, Map.of(), 100);
        
        when(httpClient.get(anyString(), any())).thenReturn(mockResponse);
        when(xmlSecurityUtils.parseSecureXml(anyString()))
            .thenThrow(new RuntimeException("XML parsing failed"));
        
        // Act & Assert
        assertThatThrownBy(() -> smpQueryService.querySmp(
            TEST_SMP_URL, 
            TEST_PARTICIPANT_ID, 
            TEST_DOCUMENT_TYPE_ID, 
            TEST_PROCESS_ID
        ))
        .isInstanceOf(SMPClientException.class)
        .hasMessageContaining("SMP query failed");
    }

    @Test
    void testUrlConstruction_WithTrailingSlash() throws Exception {
        // Arrange
        String smpUrlWithSlash = TEST_SMP_URL + "/";
        HttpResponse mockResponse = new HttpResponse(200, "OK", SAMPLE_SMP_XML, Map.of(), 100);
        Document mockDocument = createMockDocument();
        
        when(httpClient.get(anyString(), any())).thenReturn(mockResponse);
        when(xmlSecurityUtils.parseSecureXml(anyString())).thenReturn(mockDocument);
        
        // Act
        smpQueryService.querySmp(
            smpUrlWithSlash, 
            TEST_PARTICIPANT_ID, 
            TEST_DOCUMENT_TYPE_ID, 
            TEST_PROCESS_ID
        );
        
        // Assert - verify trailing slash was removed
        String expectedUrl = TEST_SMP_URL + "/" + TEST_PARTICIPANT_ID + "/services/" + TEST_DOCUMENT_TYPE_ID;
        verify(httpClient).get(eq(expectedUrl), any());
    }

    @Test
    void testPerformanceMetrics_AreCollected() throws Exception {
        // Arrange
        HttpResponse mockResponse = new HttpResponse(200, "OK", SAMPLE_SMP_XML, Map.of(), 100);
        Document mockDocument = createMockDocument();
        
        when(httpClient.get(anyString(), any())).thenReturn(mockResponse);
        when(xmlSecurityUtils.parseSecureXml(anyString())).thenReturn(mockDocument);
        
        // Act
        SmpResult result = smpQueryService.querySmp(
            TEST_SMP_URL, 
            TEST_PARTICIPANT_ID, 
            TEST_DOCUMENT_TYPE_ID, 
            TEST_PROCESS_ID
        );
        
        // Assert
        assertThat(result.getQueryTimeMs()).isGreaterThan(0);
        assertThat(result.getQueryTimeMs()).isLessThan(10000); // Should be reasonable
    }

    @Test
    void testInvalidParticipantId_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> smpQueryService.querySmp(
            TEST_SMP_URL, 
            "invalid-participant-id", 
            TEST_DOCUMENT_TYPE_ID, 
            TEST_PROCESS_ID
        ))
        .isInstanceOf(SMPClientException.class);
    }

    @Test
    void testConstructSmpUrl_ProperEncoding() throws Exception {
        // Arrange
        String participantWithSpecialChars = "0088:test@example.com";
        String documentTypeWithSpecialChars = "urn:test:doc type with spaces";
        
        HttpResponse mockResponse = new HttpResponse(200, "OK", SAMPLE_SMP_XML, Map.of(), 100);
        Document mockDocument = createMockDocument();
        
        when(httpClient.get(anyString(), any())).thenReturn(mockResponse);
        when(xmlSecurityUtils.parseSecureXml(anyString())).thenReturn(mockDocument);
        
        // Act
        smpQueryService.querySmp(
            TEST_SMP_URL, 
            participantWithSpecialChars, 
            documentTypeWithSpecialChars, 
            TEST_PROCESS_ID
        );
        
        // Assert - verify URL encoding was applied
        verify(httpClient).get(
            argThat(url -> 
                url.contains("0088%3Atest%40example.com") && 
                url.contains("urn%3Atest%3Adoc+type+with+spaces")
            ),
            any()
        );
    }

    private Document createMockDocument() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(SAMPLE_SMP_XML.getBytes()));
    }
}