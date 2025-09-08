package com.tca.peppol.service;

import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.smpclient.exception.SMPClientException;
import com.tca.peppol.client.HttpResponse;
import com.tca.peppol.client.SecureHttpClient;
import com.tca.peppol.model.internal.SmpResult;
import com.tca.peppol.util.XmlSecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for querying SMP (Service Metadata Publisher) endpoints using peppol-commons library.
 * Handles SMP URL construction, secure XML parsing, and certificate extraction.
 * 
 * This service leverages the production-proven peppol-commons library while adding
 * Lambda-specific optimizations and enhanced security measures.
 */
public class SmpQueryService {

    private static final Logger logger = LoggerFactory.getLogger(SmpQueryService.class);
    
    private final SecureHttpClient httpClient;
    private final XmlSecurityUtils xmlSecurityUtils;
    
    public SmpQueryService(SecureHttpClient httpClient, XmlSecurityUtils xmlSecurityUtils) {
        this.httpClient = httpClient;
        this.xmlSecurityUtils = xmlSecurityUtils;
    }
    
    /**
     * Query SMP for participant endpoint information.
     * Constructs SMP URLs as {smpUrl}/{participantId}/services/{documentTypeId}
     * and performs secure XML parsing to extract endpoint information.
     * 
     * @param smpUrl The SMP base URL
     * @param participantId The participant identifier
     * @param documentTypeId The document type identifier  
     * @param processId The process identifier
     * @return SmpResult containing endpoint information and metadata
     * @throws SMPClientException if SMP query fails
     */
    @Nonnull
    public SmpResult querySmp(@Nonnull String smpUrl, 
                             @Nonnull String participantId,
                             @Nonnull String documentTypeId,
                             @Nonnull String processId) throws SMPClientException {

        logger.info("Starting SMP query for participant: {} at SMP: {}", 
                   hashParticipantId(participantId), smpUrl);
        
        try {
            // Validate identifiers using peppol-commons
            IParticipantIdentifier peppolParticipantId = PeppolIdentifierFactory.INSTANCE
                .createParticipantIdentifierWithDefaultScheme(participantId);
            IDocumentTypeIdentifier peppolDocumentTypeId = PeppolIdentifierFactory.INSTANCE
                .createDocumentTypeIdentifierWithDefaultScheme(documentTypeId);
            IProcessIdentifier peppolProcessId = PeppolIdentifierFactory.INSTANCE
                .createProcessIdentifierWithDefaultScheme(processId);
            
            // Construct SMP URL as specified: {smpUrl}/{participantId}/services/{documentTypeId}
//            String requestUrl = constructSmpUrl(smpUrl, participantId, documentTypeId);
            if (peppolParticipantId == null) {
                throw new SMPClientException("Failed to parse participant identifier");
            }
            if (peppolDocumentTypeId == null) {
                throw new SMPClientException("Failed to parse document type identifier");
            }

            String requestUrl = constructSmpUrl(smpUrl, peppolParticipantId.getURIEncoded(),
                    peppolDocumentTypeId.getURIEncoded());
            
            // Set appropriate HTTP headers
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/xml");
            headers.put("User-Agent", "Peppol-SML-SMP-Lookup/1.0.0");
            
            logger.debug("Making SMP request to: {}", requestUrl);
            
            // Make HTTP request to SMP
            HttpResponse httpResponse = httpClient.get(requestUrl, headers);
            
            if (httpResponse.getStatusCode() != 200) {
                throw new SMPClientException("SMP returned HTTP " + httpResponse.getStatusCode() + 
                    ": " + httpResponse.getReasonPhrase());
            }
            
            String xmlContent = httpResponse.getBody();
            if (xmlContent == null || xmlContent.trim().isEmpty()) {
                throw new SMPClientException("Empty response from SMP");
            }
            
            // Parse XML securely
            Document xmlDocument = xmlSecurityUtils.parseSecureXml(xmlContent);
            
            // Extract endpoint information from XML
            String endpointUrl = extractEndpointUrl(xmlDocument, processId);
            String transportProfile = extractTransportProfile(xmlDocument, processId);
            X509Certificate certificate = extractCertificateFromXml(xmlDocument, processId);
            Instant serviceActivationDate = extractServiceActivationDate(xmlDocument, processId);
            Instant serviceExpirationDate = extractServiceExpirationDate(xmlDocument, processId);
            String xmlSignature = extractXmlSignature(xmlDocument);

            
            logger.info("SMP query completed successfully for participant: {}", hashParticipantId(participantId));
            
            return SmpResult.builder()
                .endpointUrl(endpointUrl)
                .transportProfile(transportProfile)
                .certificate(certificate)
                .serviceActivationDate(serviceActivationDate)
                .serviceExpirationDate(serviceExpirationDate)
                .xmlDocument(xmlDocument)
                .xmlSignature(xmlSignature)
                .successful(true)
                .participantId(participantId)
                .documentTypeId(documentTypeId)
                .smpQueryUrl(requestUrl)
                .rawXmlContent(xmlContent)
                .build();
                
        } catch (SMPClientException e) {
            logger.error("SMP client error for participant {}: {}", hashParticipantId(participantId), e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during SMP query for participant {}: {}", 
                        hashParticipantId(participantId), e.getMessage());
            throw new SMPClientException("SMP query failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Construct SMP URL with proper URL encoding for path parameters.
     * Format: {smpUrl}/{participantId}/services/{documentTypeId}
     */
    @Nonnull
    private String constructSmpUrl(@Nonnull String smpUrl, 
                                  @Nonnull String participantId,
                                  @Nonnull String documentTypeId) {
        try {
            // Remove trailing slash from SMP URL
            String baseUrl = smpUrl.replaceAll("/$", "");
            
            // URL encode path parameters
            String encodedParticipantId = URLEncoder.encode(participantId, StandardCharsets.UTF_8);
            String encodedDocumentTypeId = URLEncoder.encode(documentTypeId, StandardCharsets.UTF_8);
            
            return String.format("%s/%s/services/%s", 
                baseUrl, encodedParticipantId, encodedDocumentTypeId);
                
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct SMP URL: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract endpoint URL from SMP XML response for the specified process.
     */
    @Nullable
    private String extractEndpointUrl(@Nonnull Document xmlDocument, @Nonnull String processId) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            String expression = String.format(
                "//Process[ProcessIdentifier='%s']/ServiceEndpointList/Endpoint/EndpointReference", 
                processId);
            
            String endpointUrl = (String) xpath.evaluate(expression, xmlDocument, XPathConstants.STRING);
            return endpointUrl != null && !endpointUrl.trim().isEmpty() ? endpointUrl.trim() : null;
            
        } catch (Exception e) {
            logger.warn("Failed to extract endpoint URL from XML: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract transport profile from SMP XML response for the specified process.
     */
    @Nullable
    private String extractTransportProfile(@Nonnull Document xmlDocument, @Nonnull String processId) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            String expression = String.format(
                "//Process[ProcessIdentifier='%s']/ServiceEndpointList/Endpoint/@transportProfile", 
                processId);
            
            String transportProfile = (String) xpath.evaluate(expression, xmlDocument, XPathConstants.STRING);
            return transportProfile != null && !transportProfile.trim().isEmpty() ? transportProfile.trim() : null;
            
        } catch (Exception e) {
            logger.warn("Failed to extract transport profile from XML: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract certificate from SMP XML response for the specified process.
     */
    @Nullable
    private X509Certificate extractCertificateFromXml(@Nonnull Document xmlDocument, @Nonnull String processId) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            String expression = String.format(
                "//Process[ProcessIdentifier='%s']/ServiceEndpointList/Endpoint/Certificate", 
                processId);
            
            String certificateData = (String) xpath.evaluate(expression, xmlDocument, XPathConstants.STRING);
            return extractCertificateFromEndpoint(certificateData);
            
        } catch (Exception e) {
            logger.warn("Failed to extract certificate from XML: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract service activation date from SMP XML response.
     */
    @Nullable
    private Instant extractServiceActivationDate(@Nonnull Document xmlDocument, @Nonnull String processId) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            String expression = String.format(
                "//Process[ProcessIdentifier='%s']/ServiceEndpointList/Endpoint/ServiceActivationDate", 
                processId);
            
            String dateStr = (String) xpath.evaluate(expression, xmlDocument, XPathConstants.STRING);
            if (dateStr != null && !dateStr.trim().isEmpty()) {
                return Instant.parse(dateStr.trim());
            }
            return null;
            
        } catch (Exception e) {
            logger.warn("Failed to extract service activation date from XML: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract service expiration date from SMP XML response.
     */
    @Nullable
    private Instant extractServiceExpirationDate(@Nonnull Document xmlDocument, @Nonnull String processId) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            String expression = String.format(
                "//Process[ProcessIdentifier='%s']/ServiceEndpointList/Endpoint/ServiceExpirationDate", 
                processId);
            
            String dateStr = (String) xpath.evaluate(expression, xmlDocument, XPathConstants.STRING);
            if (dateStr != null && !dateStr.trim().isEmpty()) {
                return Instant.parse(dateStr.trim());
            }
            return null;
            
        } catch (Exception e) {
            logger.warn("Failed to extract service expiration date from XML: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract XML signature from SMP response for later validation.
     */
    @Nullable
    private String extractXmlSignature(@Nonnull Document xmlDocument) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            String expression = "//Signature";
            
            NodeList signatureNodes = (NodeList) xpath.evaluate(expression, xmlDocument, XPathConstants.NODESET);
            if (signatureNodes.getLength() > 0) {
                Element signatureElement = (Element) signatureNodes.item(0);
                // Return the signature element as string for later validation
                return signatureElement.toString();
            }
            return null;
            
        } catch (Exception e) {
            logger.warn("Failed to extract XML signature: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract X.509 certificate from Base64 DER format.
     */
    @Nullable
    private X509Certificate extractCertificateFromEndpoint(@Nullable String certificateData) {
        if (certificateData == null || certificateData.trim().isEmpty()) {
            logger.warn("No certificate data provided in endpoint");
            return null;
        }
        
        try {
            // Clean up certificate data - remove whitespace and newlines
            String cleanCertData = certificateData.replaceAll("\\s+", "");
            
            // Decode Base64 DER certificate
            byte[] certBytes = Base64.getDecoder().decode(cleanCertData);
            
            // Create certificate from DER bytes
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(
                new ByteArrayInputStream(certBytes));
            
            logger.debug("Successfully extracted certificate with subject: {}", 
                        certificate.getSubjectDN().getName());
            
            return certificate;
            
        } catch (Exception e) {
            logger.error("Failed to extract certificate from endpoint data: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Hash participant ID for privacy-conscious logging.
     */
    private String hashParticipantId(String participantId) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(participantId.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash).substring(0, 8);
        } catch (Exception e) {
            return "HASH_ERROR";
        }
    }
}