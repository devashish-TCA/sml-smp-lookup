package com.tca.peppol.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Secure XML processing utilities that protect against XXE, XML bomb attacks,
 * and other XML-based security vulnerabilities.
 * 
 * This class implements comprehensive security measures according to OWASP guidelines
 * and Peppol security requirements.
 */
public final class XmlSecurityUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(XmlSecurityUtils.class);
    
    // Security limits to prevent XML bomb attacks
    private static final int MAX_DOCUMENT_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final int MAX_ENTITY_EXPANSION_LIMIT = 64000;
    private static final int MAX_ELEMENT_DEPTH = 100;
    private static final int MAX_GENERAL_ENTITY_SIZE_LIMIT = 64000;
    private static final int MAX_PARAMETER_ENTITY_SIZE_LIMIT = 1000;
    private static final long MAX_PROCESSING_TIME_MS = 30000; // 30 seconds
    
    // Feature names for XML security configuration
    private static final String FEATURE_DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String FEATURE_EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
    private static final String FEATURE_EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
    private static final String FEATURE_LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    private static final String FEATURE_SECURE_PROCESSING = XMLConstants.FEATURE_SECURE_PROCESSING;
    
    // Property names for entity expansion limits
    private static final String PROPERTY_ENTITY_EXPANSION_LIMIT = "http://www.oracle.com/xml/jaxp/properties/entityExpansionLimit";
    private static final String PROPERTY_ELEMENT_ATTRIBUTE_LIMIT = "http://www.oracle.com/xml/jaxp/properties/elementAttributeLimit";
    private static final String PROPERTY_MAX_OCCUR_LIMIT = "http://www.oracle.com/xml/jaxp/properties/maxOccurLimit";
    private static final String PROPERTY_TOTAL_ENTITY_SIZE_LIMIT = "http://www.oracle.com/xml/jaxp/properties/totalEntitySizeLimit";
    private static final String PROPERTY_GENERAL_ENTITY_SIZE_LIMIT = "http://www.oracle.com/xml/jaxp/properties/maxGeneralEntitySizeLimit";
    private static final String PROPERTY_PARAMETER_ENTITY_SIZE_LIMIT = "http://www.oracle.com/xml/jaxp/properties/maxParameterEntitySizeLimit";
    private static final String PROPERTY_MAX_ELEMENT_DEPTH = "http://www.oracle.com/xml/jaxp/properties/maxElementDepth";
    
    private XmlSecurityUtils() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Creates a secure DocumentBuilderFactory with all security features enabled
     * and attack vectors disabled.
     * 
     * @return Configured secure DocumentBuilderFactory
     * @throws XmlSecurityException if security configuration fails
     */
    public static DocumentBuilderFactory createSecureDocumentBuilderFactory() throws XmlSecurityException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            
            // Disable DTD processing completely to prevent XXE attacks
            factory.setFeature(FEATURE_DISALLOW_DOCTYPE, true);
            
            // Disable external entity processing
            factory.setFeature(FEATURE_EXTERNAL_GENERAL_ENTITIES, false);
            factory.setFeature(FEATURE_EXTERNAL_PARAMETER_ENTITIES, false);
            factory.setFeature(FEATURE_LOAD_EXTERNAL_DTD, false);
            
            // Enable secure processing
            factory.setFeature(FEATURE_SECURE_PROCESSING, true);
            
            // Enable namespace processing for XML signature validation
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            factory.setExpandEntityReferences(false);
            
            // Set entity expansion limits to prevent XML bomb attacks
            // Try to set these properties, but don't fail if they're not supported
            setAttributeSafely(factory, PROPERTY_ENTITY_EXPANSION_LIMIT, MAX_ENTITY_EXPANSION_LIMIT);
            setAttributeSafely(factory, PROPERTY_ELEMENT_ATTRIBUTE_LIMIT, 10000);
            setAttributeSafely(factory, PROPERTY_MAX_OCCUR_LIMIT, 5000);
            setAttributeSafely(factory, PROPERTY_TOTAL_ENTITY_SIZE_LIMIT, MAX_GENERAL_ENTITY_SIZE_LIMIT);
            setAttributeSafely(factory, PROPERTY_GENERAL_ENTITY_SIZE_LIMIT, MAX_GENERAL_ENTITY_SIZE_LIMIT);
            setAttributeSafely(factory, PROPERTY_PARAMETER_ENTITY_SIZE_LIMIT, MAX_PARAMETER_ENTITY_SIZE_LIMIT);
            setAttributeSafely(factory, PROPERTY_MAX_ELEMENT_DEPTH, MAX_ELEMENT_DEPTH);
            
            logger.debug("Created secure DocumentBuilderFactory with security features enabled");
            return factory;
            
        } catch (ParserConfigurationException e) {
            logger.error("Failed to configure secure DocumentBuilderFactory", e);
            throw new XmlSecurityException("Failed to create secure XML parser configuration", e);
        }
    }
    
    /**
     * Creates a secure DocumentBuilder with all security features enabled.
     * 
     * @return Configured secure DocumentBuilder
     * @throws XmlSecurityException if security configuration fails
     */
    public static DocumentBuilder createSecureDocumentBuilder() throws XmlSecurityException {
        try {
            DocumentBuilderFactory factory = createSecureDocumentBuilderFactory();
            DocumentBuilder builder = factory.newDocumentBuilder();
            
            // Set custom error handler to prevent information disclosure
            builder.setErrorHandler(new SecureXmlErrorHandler());
            
            // Disable entity resolver to prevent external entity access
            builder.setEntityResolver((publicId, systemId) -> {
                logger.warn("Attempted to resolve external entity: publicId={}, systemId={}", publicId, systemId);
                throw new SAXException("External entity resolution is disabled for security");
            });
            
            return builder;
            
        } catch (ParserConfigurationException e) {
            logger.error("Failed to create secure DocumentBuilder", e);
            throw new XmlSecurityException("Failed to create secure XML document builder", e);
        }
    }
    
    /**
     * Securely parses XML content from a string with comprehensive security checks.
     * 
     * @param xmlContent The XML content to parse
     * @return Parsed Document
     * @throws XmlSecurityException if parsing fails or security violations are detected
     */
    public static Document parseSecureXml(String xmlContent) throws XmlSecurityException {
        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            throw new XmlSecurityException("XML content cannot be null or empty");
        }
        
        // Check document size limit
        byte[] xmlBytes = xmlContent.getBytes(StandardCharsets.UTF_8);
        if (xmlBytes.length > MAX_DOCUMENT_SIZE_BYTES) {
            logger.warn("XML document size {} exceeds maximum allowed size {}", 
                       xmlBytes.length, MAX_DOCUMENT_SIZE_BYTES);
            throw new XmlSecurityException("XML document size exceeds maximum allowed limit");
        }
        
        return parseSecureXml(new ByteArrayInputStream(xmlBytes));
    }
    
    /**
     * Securely parses XML content from an InputStream with comprehensive security checks.
     * 
     * @param xmlInputStream The XML input stream to parse
     * @return Parsed Document
     * @throws XmlSecurityException if parsing fails or security violations are detected
     */
    public static Document parseSecureXml(InputStream xmlInputStream) throws XmlSecurityException {
        if (xmlInputStream == null) {
            throw new XmlSecurityException("XML input stream cannot be null");
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            DocumentBuilder builder = createSecureDocumentBuilder();
            
            // Parse with timeout protection
            Document document = parseWithTimeout(builder, xmlInputStream);
            
            long processingTime = System.currentTimeMillis() - startTime;
            if (processingTime > MAX_PROCESSING_TIME_MS) {
                logger.warn("XML processing took {} ms, exceeding maximum allowed time {}", 
                           processingTime, MAX_PROCESSING_TIME_MS);
                throw new XmlSecurityException("XML processing time exceeded maximum allowed limit");
            }
            
            // Validate document structure
            validateDocumentStructure(document);
            
            logger.debug("Successfully parsed XML document in {} ms", processingTime);
            return document;
            
        } catch (SAXException e) {
            logger.error("XML parsing failed due to SAX exception", e);
            throw new XmlSecurityException("XML parsing failed: " + e.getMessage(), e);
        } catch (IOException e) {
            logger.error("XML parsing failed due to IO exception", e);
            throw new XmlSecurityException("XML parsing failed due to IO error", e);
        }
    }
    
    /**
     * Creates a secure TransformerFactory with security features enabled.
     * 
     * @return Configured secure TransformerFactory
     * @throws XmlSecurityException if security configuration fails
     */
    public static TransformerFactory createSecureTransformerFactory() throws XmlSecurityException {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            
            // Enable secure processing
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            
            // Disable access to external DTDs and stylesheets
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            
            logger.debug("Created secure TransformerFactory with security features enabled");
            return factory;
            
        } catch (Exception e) {
            logger.error("Failed to create secure TransformerFactory", e);
            throw new XmlSecurityException("Failed to create secure XML transformer", e);
        }
    }
    
    /**
     * Creates a secure SchemaFactory with security features enabled.
     * 
     * @return Configured secure SchemaFactory
     * @throws XmlSecurityException if security configuration fails
     */
    public static SchemaFactory createSecureSchemaFactory() throws XmlSecurityException {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            
            // Enable secure processing
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            
            // Disable access to external DTDs and schemas - try but don't fail if not supported
            setPropertySafely(factory, XMLConstants.ACCESS_EXTERNAL_DTD, "");
            setPropertySafely(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            
            logger.debug("Created secure SchemaFactory with security features enabled");
            return factory;
            
        } catch (Exception e) {
            logger.error("Failed to create secure SchemaFactory", e);
            throw new XmlSecurityException("Failed to create secure XML schema factory", e);
        }
    }
    
    /**
     * Validates the structure of a parsed XML document to detect potential attacks.
     * 
     * @param document The document to validate
     * @throws XmlSecurityException if validation fails or security violations are detected
     */
    private static void validateDocumentStructure(Document document) throws XmlSecurityException {
        if (document == null) {
            throw new XmlSecurityException("Document cannot be null");
        }
        
        // Check for excessive nesting depth
        int maxDepth = calculateMaxDepth(document.getDocumentElement(), 0);
        if (maxDepth > MAX_ELEMENT_DEPTH) {
            logger.warn("XML document depth {} exceeds maximum allowed depth {}", 
                       maxDepth, MAX_ELEMENT_DEPTH);
            throw new XmlSecurityException("XML document nesting depth exceeds maximum allowed limit");
        }
        
        logger.debug("XML document structure validation passed, max depth: {}", maxDepth);
    }
    
    /**
     * Calculates the maximum nesting depth of an XML element.
     * 
     * @param element The element to analyze
     * @param currentDepth The current depth
     * @return Maximum depth found
     */
    private static int calculateMaxDepth(org.w3c.dom.Element element, int currentDepth) {
        int maxDepth = currentDepth;
        
        org.w3c.dom.NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (child instanceof org.w3c.dom.Element) {
                int childDepth = calculateMaxDepth((org.w3c.dom.Element) child, currentDepth + 1);
                maxDepth = Math.max(maxDepth, childDepth);
            }
        }
        
        return maxDepth;
    }
    
    /**
     * Parses XML with timeout protection to prevent denial of service attacks.
     * 
     * @param builder The DocumentBuilder to use
     * @param inputStream The input stream to parse
     * @return Parsed Document
     * @throws SAXException if parsing fails
     * @throws IOException if IO error occurs
     * @throws XmlSecurityException if timeout occurs
     */
    private static Document parseWithTimeout(DocumentBuilder builder, InputStream inputStream) 
            throws SAXException, IOException, XmlSecurityException {
        
        // Create a timeout-protected parsing task
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        java.util.concurrent.Future<Document> future = executor.submit(() -> {
            try {
                return builder.parse(inputStream);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        try {
            return future.get(MAX_PROCESSING_TIME_MS, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            logger.error("XML parsing timed out after {} ms", MAX_PROCESSING_TIME_MS);
            throw new XmlSecurityException("XML parsing timed out");
        } catch (Exception e) {
            logger.error("XML parsing failed with exception", e);
            if (e.getCause() instanceof SAXException) {
                throw (SAXException) e.getCause();
            } else if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else {
                throw new XmlSecurityException("XML parsing failed", e);
            }
        } finally {
            executor.shutdown();
        }
    }
    
    /**
     * Safely sets an attribute on a DocumentBuilderFactory, logging warnings if not supported.
     * 
     * @param factory The factory to configure
     * @param attribute The attribute name
     * @param value The attribute value
     */
    private static void setAttributeSafely(DocumentBuilderFactory factory, String attribute, Object value) {
        try {
            factory.setAttribute(attribute, value);
            logger.debug("Successfully set attribute {} to {}", attribute, value);
        } catch (IllegalArgumentException e) {
            logger.warn("Attribute {} not supported by this XML parser implementation: {}", attribute, e.getMessage());
        }
    }
    
    /**
     * Safely sets a property on a SchemaFactory, logging warnings if not supported.
     * 
     * @param factory The factory to configure
     * @param property The property name
     * @param value The property value
     */
    private static void setPropertySafely(SchemaFactory factory, String property, String value) {
        try {
            factory.setProperty(property, value);
            logger.debug("Successfully set property {} to {}", property, value);
        } catch (Exception e) {
            logger.warn("Property {} not supported by this XML parser implementation: {}", property, e.getMessage());
        }
    }
}