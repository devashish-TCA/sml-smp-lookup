package com.tca.peppol.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Secure XML error handler that prevents information disclosure through
 * XML parsing error messages while maintaining proper error logging.
 * 
 * This handler ensures that sensitive information from XML parsing errors
 * is not exposed to potential attackers while still providing adequate
 * logging for debugging and monitoring purposes.
 */
public class SecureXmlErrorHandler implements ErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(SecureXmlErrorHandler.class);
    
    /**
     * Handles XML parsing warnings.
     * Logs the warning but does not throw an exception.
     * 
     * @param exception the warning information
     */
    @Override
    public void warning(SAXParseException exception) {
        logger.warn("XML parsing warning at line {}, column {}: {}", 
                   exception.getLineNumber(), 
                   exception.getColumnNumber(), 
                   sanitizeErrorMessage(exception.getMessage()));
    }
    
    /**
     * Handles XML parsing errors.
     * Logs the error and throws a SAXException with a sanitized message.
     * 
     * @param exception the error information
     * @throws SAXException always thrown to indicate parsing failure
     */
    @Override
    public void error(SAXParseException exception) throws SAXException {
        String sanitizedMessage = sanitizeErrorMessage(exception.getMessage());
        logger.error("XML parsing error at line {}, column {}: {}", 
                    exception.getLineNumber(), 
                    exception.getColumnNumber(), 
                    sanitizedMessage);
        
        throw new SAXException("XML parsing error: " + sanitizedMessage);
    }
    
    /**
     * Handles fatal XML parsing errors.
     * Logs the error and throws a SAXException with a sanitized message.
     * 
     * @param exception the fatal error information
     * @throws SAXException always thrown to indicate parsing failure
     */
    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        String sanitizedMessage = sanitizeErrorMessage(exception.getMessage());
        logger.error("XML parsing fatal error at line {}, column {}: {}", 
                    exception.getLineNumber(), 
                    exception.getColumnNumber(), 
                    sanitizedMessage);
        
        throw new SAXException("XML parsing fatal error: " + sanitizedMessage);
    }
    
    /**
     * Sanitizes error messages to prevent information disclosure.
     * Removes potentially sensitive information while preserving
     * useful debugging information.
     * 
     * @param originalMessage the original error message
     * @return sanitized error message
     */
    private String sanitizeErrorMessage(String originalMessage) {
        if (originalMessage == null || originalMessage.trim().isEmpty()) {
            return "Unknown XML parsing error";
        }
        
        // Remove file paths and URLs that might contain sensitive information
        String sanitized = originalMessage.replaceAll("file://[^\\s]+", "[FILE_PATH_REMOVED]");
        sanitized = sanitized.replaceAll("http[s]?://[^\\s]+", "[URL_REMOVED]");
        
        // Remove system property references
        sanitized = sanitized.replaceAll("\\$\\{[^}]+\\}", "[SYSTEM_PROPERTY_REMOVED]");
        
        // Limit message length to prevent log injection
        if (sanitized.length() > 500) {
            sanitized = sanitized.substring(0, 497) + "...";
        }
        
        // Remove potential XML injection attempts
        sanitized = sanitized.replaceAll("<[^>]*>", "[XML_TAG_REMOVED]");
        
        return sanitized;
    }
}