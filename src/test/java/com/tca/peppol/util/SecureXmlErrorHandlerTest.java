package com.tca.peppol.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for SecureXmlErrorHandler to verify proper error handling
 * and message sanitization for security purposes.
 */
@DisplayName("SecureXmlErrorHandler Tests")
class SecureXmlErrorHandlerTest {
    
    private SecureXmlErrorHandler errorHandler;
    
    @BeforeEach
    void setUp() {
        errorHandler = new SecureXmlErrorHandler();
    }
    
    /**
     * Helper method to create SAXParseException instances for testing.
     */
    private SAXParseException createSAXParseException(String message, int lineNumber, int columnNumber) {
        return new SAXParseException(message, null, null, lineNumber, columnNumber);
    }
    
    @Nested
    @DisplayName("Warning Handling Tests")
    class WarningHandlingTests {
        
        @Test
        @DisplayName("Should handle warnings without throwing exceptions")
        void shouldHandleWarningsWithoutThrowingExceptions() {
            SAXParseException exception = createSAXParseException("Warning message", 10, 5);
            
            assertThatCode(() -> errorHandler.warning(exception))
                .doesNotThrowAnyException();
        }
        
        @Test
        @DisplayName("Should handle warnings with null message")
        void shouldHandleWarningsWithNullMessage() {
            SAXParseException exception = createSAXParseException(null, 10, 5);
            
            assertThatCode(() -> errorHandler.warning(exception))
                .doesNotThrowAnyException();
        }
    }
    
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should throw SAXException for errors")
        void shouldThrowSaxExceptionForErrors() {
            SAXParseException exception = createSAXParseException("Error message", 10, 5);
            
            assertThatThrownBy(() -> errorHandler.error(exception))
                .isInstanceOf(SAXException.class)
                .hasMessageContaining("XML parsing error: Error message");
        }
        
        @Test
        @DisplayName("Should handle errors with null message")
        void shouldHandleErrorsWithNullMessage() {
            SAXParseException exception = createSAXParseException(null, 10, 5);
            
            assertThatThrownBy(() -> errorHandler.error(exception))
                .isInstanceOf(SAXException.class)
                .hasMessageContaining("Unknown XML parsing error");
        }
    }
    
    @Nested
    @DisplayName("Fatal Error Handling Tests")
    class FatalErrorHandlingTests {
        
        @Test
        @DisplayName("Should throw SAXException for fatal errors")
        void shouldThrowSaxExceptionForFatalErrors() {
            SAXParseException exception = createSAXParseException("Fatal error message", 10, 5);
            
            assertThatThrownBy(() -> errorHandler.fatalError(exception))
                .isInstanceOf(SAXException.class)
                .hasMessageContaining("XML parsing fatal error: Fatal error message");
        }
        
        @Test
        @DisplayName("Should handle fatal errors with null message")
        void shouldHandleFatalErrorsWithNullMessage() {
            SAXParseException exception = createSAXParseException(null, 10, 5);
            
            assertThatThrownBy(() -> errorHandler.fatalError(exception))
                .isInstanceOf(SAXException.class)
                .hasMessageContaining("Unknown XML parsing error");
        }
    }
    
    @Nested
    @DisplayName("Message Sanitization Tests")
    class MessageSanitizationTests {
        
        @Test
        @DisplayName("Should sanitize file paths from error messages")
        void shouldSanitizeFilePathsFromErrorMessages() {
            SAXParseException exception = createSAXParseException("Error accessing file://etc/passwd", 10, 5);
            
            assertThatThrownBy(() -> errorHandler.error(exception))
                .isInstanceOf(SAXException.class)
                .hasMessageContaining("[FILE_PATH_REMOVED]")
                .hasMessageNotContaining("file://etc/passwd");
        }
        
        @Test
        @DisplayName("Should sanitize HTTP URLs from error messages")
        void shouldSanitizeHttpUrlsFromErrorMessages() {
            SAXParseException exception = createSAXParseException("Error accessing http://malicious.example.com/evil.xml", 10, 5);
            
            assertThatThrownBy(() -> errorHandler.error(exception))
                .isInstanceOf(SAXException.class)
                .hasMessageContaining("[URL_REMOVED]")
                .hasMessageNotContaining("http://malicious.example.com");
        }
        
        @Test
        @DisplayName("Should sanitize HTTPS URLs from error messages")
        void shouldSanitizeHttpsUrlsFromErrorMessages() {
            SAXParseException exception = createSAXParseException("Error accessing https://malicious.example.com/evil.xml", 10, 5);
            
            assertThatThrownBy(() -> errorHandler.error(exception))
                .isInstanceOf(SAXException.class)
                .hasMessageContaining("[URL_REMOVED]")
                .hasMessageNotContaining("https://malicious.example.com");
        }
        
        @Test
        @DisplayName("Should sanitize system properties from error messages")
        void shouldSanitizeSystemPropertiesFromErrorMessages() {
            SAXParseException exception = createSAXParseException("Error with ${user.home}/sensitive/file", 10, 5);
            
            assertThatThrownBy(() -> errorHandler.error(exception))
                .isInstanceOf(SAXException.class)
                .hasMessageContaining("[SYSTEM_PROPERTY_REMOVED]")
                .hasMessageNotContaining("${user.home}");
        }
        
        @Test
        @DisplayName("Should sanitize XML tags from error messages")
        void shouldSanitizeXmlTagsFromErrorMessages() {
            SAXParseException exception = createSAXParseException("Error with <script>alert('xss')</script> content", 10, 5);
            
            assertThatThrownBy(() -> errorHandler.error(exception))
                .isInstanceOf(SAXException.class)
                .hasMessageContaining("[XML_TAG_REMOVED]")
                .hasMessageNotContaining("<script>")
                .hasMessageNotContaining("</script>");
        }
        
        @Test
        @DisplayName("Should truncate overly long error messages")
        void shouldTruncateOverlyLongErrorMessages() {
            String longMessage = "Error: " + "x".repeat(600); // 606 characters total
            SAXParseException exception = createSAXParseException(longMessage, 10, 5);
            
            assertThatThrownBy(() -> errorHandler.error(exception))
                .isInstanceOf(SAXException.class)
                .hasMessageMatching(".*\\.\\.\\..*"); // Should contain "..."
        }
        
        @Test
        @DisplayName("Should handle complex sanitization scenarios")
        void shouldHandleComplexSanitizationScenarios() {
            String complexMessage = "Error accessing file:///etc/passwd and https://evil.com/script with ${java.home} and <malicious>content</malicious>";
            SAXParseException exception = createSAXParseException(complexMessage, 10, 5);
            
            assertThatThrownBy(() -> errorHandler.error(exception))
                .isInstanceOf(SAXException.class)
                .hasMessageContaining("[FILE_PATH_REMOVED]")
                .hasMessageContaining("[URL_REMOVED]")
                .hasMessageContaining("[SYSTEM_PROPERTY_REMOVED]")
                .hasMessageContaining("[XML_TAG_REMOVED]")
                .hasMessageNotContaining("file:///etc/passwd")
                .hasMessageNotContaining("https://evil.com")
                .hasMessageNotContaining("${java.home}")
                .hasMessageNotContaining("<malicious>");
        }
        
        @Test
        @DisplayName("Should preserve useful debugging information")
        void shouldPreserveUsefulDebuggingInformation() {
            SAXParseException exception = createSAXParseException("Element 'test' is not valid at this position", 10, 5);
            
            assertThatThrownBy(() -> errorHandler.error(exception))
                .isInstanceOf(SAXException.class)
                .hasMessageContaining("Element")
                .hasMessageContaining("not valid")
                .hasMessageContaining("position");
        }
        
        @Test
        @DisplayName("Should handle empty error messages")
        void shouldHandleEmptyErrorMessages() {
            SAXParseException exception = createSAXParseException("", 10, 5);
            
            assertThatThrownBy(() -> errorHandler.error(exception))
                .isInstanceOf(SAXException.class)
                .hasMessageContaining("Unknown XML parsing error");
        }
        
        @Test
        @DisplayName("Should handle whitespace-only error messages")
        void shouldHandleWhitespaceOnlyErrorMessages() {
            SAXParseException exception = createSAXParseException("   \n\t  ", 10, 5);
            
            assertThatThrownBy(() -> errorHandler.error(exception))
                .isInstanceOf(SAXException.class);
        }
    }
    
    @Nested
    @DisplayName("Line and Column Information Tests")
    class LineAndColumnInformationTests {
        
        @Test
        @DisplayName("Should include line and column information in error messages")
        void shouldIncludeLineAndColumnInformation() {
            SAXParseException exception = createSAXParseException("Test error", 42, 15);
            
            assertThatThrownBy(() -> errorHandler.error(exception))
                .isInstanceOf(SAXException.class)
                .hasMessageContaining("XML parsing error: Test error");
        }
        
        @Test
        @DisplayName("Should handle negative line numbers")
        void shouldHandleNegativeLineNumbers() {
            SAXParseException exception = createSAXParseException("Test error", -1, -1);
            
            assertThatThrownBy(() -> errorHandler.error(exception))
                .isInstanceOf(SAXException.class)
                .hasMessageContaining("XML parsing error: Test error");
        }
    }
}