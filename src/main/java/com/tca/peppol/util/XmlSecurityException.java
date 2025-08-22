package com.tca.peppol.util;

/**
 * Exception thrown when XML security violations are detected or
 * when secure XML processing fails.
 * 
 * This exception is used to indicate various XML security issues including:
 * - XXE (XML External Entity) attack attempts
 * - XML bomb attacks (entity expansion, billion laughs)
 * - Document size limit violations
 * - Processing time limit violations
 * - Configuration failures in secure XML processing
 */
public class XmlSecurityException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new XmlSecurityException with the specified detail message.
     * 
     * @param message the detail message
     */
    public XmlSecurityException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new XmlSecurityException with the specified detail message and cause.
     * 
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public XmlSecurityException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new XmlSecurityException with the specified cause.
     * 
     * @param cause the cause of this exception
     */
    public XmlSecurityException(Throwable cause) {
        super(cause);
    }
}