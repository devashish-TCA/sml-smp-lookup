package com.tca.peppol.validation;

import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.tca.peppol.exception.ErrorCodes;
import com.tca.peppol.model.request.LookupRequest;
import com.tca.peppol.util.StructuredLogger;
import com.tca.peppol.util.CorrelationIdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Comprehensive request validator that leverages peppol-commons for Peppol-compliant
 * input validation and sanitization. Provides protection against various injection
 * attacks while ensuring compliance with Peppol specifications.
 */
public class RequestValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestValidator.class);
    
    // Security patterns for input sanitization
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|vbscript|onload|onerror|\\bor\\b|\\band\\b|--|/\\*|\\*/|;)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script|</script|javascript:|vbscript:|onload=|onerror=|onclick=|onmouseover=|<iframe|</iframe)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern XML_INJECTION_PATTERN = Pattern.compile(
        "(?i)(<!\\[CDATA\\[|]]>|<!DOCTYPE|<!ENTITY|&[a-zA-Z0-9]+;)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Character set validation - allow Peppol-safe characters including single quotes for SQL injection detection
    private static final Pattern SAFE_CHARACTERS_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9\\-.:_@#/\\s=+%']*$"
    );
    
    // Maximum input lengths for security
    private static final int MAX_INPUT_LENGTH = 1000;
    private static final int MAX_REQUEST_ID_LENGTH = 100;
    
    // Rate limiting configuration (requests per minute per IP)
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int MAX_REQUEST_SIZE_BYTES = 10240; // 10KB
    
    private final PeppolIdentifierFactory identifierFactory;
    
    public RequestValidator() {
        this.identifierFactory = PeppolIdentifierFactory.INSTANCE;
    }
    
    /**
     * Validates a complete lookup request using peppol-commons and security checks.
     *
     * @param request The lookup request to validate
     * @return RequestValidationResult containing validation status and any errors
     */
    public RequestValidationResult validateRequest(LookupRequest request) {
        String correlationId = CorrelationIdUtils.getCorrelationId();
        
        Map<String, Object> logDetails = new HashMap<>();
        logDetails.put("correlationId", correlationId);
        if (request != null && request.getParticipantId() != null) {
            logDetails.put("participantId", request.getParticipantId());
        }
        
        StructuredLogger.logValidationEvent("request_validation_start", true, logDetails);
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        try {
            // Basic null and size validation
            validateBasicConstraints(request, errors);
            
            // Input sanitization and security validation
            validateInputSecurity(request, errors);
            
            // Peppol-specific validation using peppol-commons
            validatePeppolCompliance(request, errors, warnings);
            
            // Environment and configuration validation
            validateEnvironmentConfiguration(request, errors);
            
            boolean isValid = errors.isEmpty();
            
            Map<String, Object> completionDetails = new HashMap<>();
            completionDetails.put("correlationId", correlationId);
            completionDetails.put("isValid", isValid);
            completionDetails.put("errorCount", errors.size());
            completionDetails.put("warningCount", warnings.size());
            
            StructuredLogger.logValidationEvent("request_validation_completed", isValid, completionDetails);
            
            return new RequestValidationResult(isValid, errors, warnings);
            
        } catch (Exception e) {
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("correlationId", correlationId);
            StructuredLogger.logError("Unexpected error during request validation", e, errorDetails);
            
            errors.add(ErrorCodes.E7001 + ": Validation failed due to internal error");
            return new RequestValidationResult(false, errors, warnings);
        }
    }
    
    /**
     * Validates basic constraints like null checks and length limits.
     */
    private void validateBasicConstraints(LookupRequest request, List<String> errors) {
        if (request == null) {
            errors.add(ErrorCodes.E7001 + ": Request cannot be null");
            return;
        }
        
        // Required field validation
        if (request.getParticipantId() == null || request.getParticipantId().trim().isEmpty()) {
            errors.add(ErrorCodes.E7002 + ": Participant ID is required");
        }
        
        if (request.getDocumentTypeId() == null || request.getDocumentTypeId().trim().isEmpty()) {
            errors.add(ErrorCodes.E7003 + ": Document type ID is required");
        }
        
        if (request.getProcessId() == null || request.getProcessId().trim().isEmpty()) {
            errors.add(ErrorCodes.E7004 + ": Process ID is required");
        }
        
        if (request.getEnvironment() == null || request.getEnvironment().trim().isEmpty()) {
            errors.add(ErrorCodes.E7005 + ": Environment is required");
        }
        
        // Length validation
        validateFieldLength("Participant ID", request.getParticipantId(), MAX_INPUT_LENGTH, errors);
        validateFieldLength("Document type ID", request.getDocumentTypeId(), MAX_INPUT_LENGTH, errors);
        validateFieldLength("Process ID", request.getProcessId(), MAX_INPUT_LENGTH, errors);
        validateFieldLength("Request ID", request.getRequestId(), MAX_REQUEST_ID_LENGTH, errors);
    }
    
    /**
     * Validates input security to prevent injection attacks.
     */
    private void validateInputSecurity(LookupRequest request, List<String> errors) {
        if (request == null) {
            return; // Basic validation will catch this
        }
        
        // SQL injection protection
        validateAgainstPattern("Participant ID", request.getParticipantId(), 
            SQL_INJECTION_PATTERN, "SQL injection patterns detected", errors);
        validateAgainstPattern("Document type ID", request.getDocumentTypeId(), 
            SQL_INJECTION_PATTERN, "SQL injection patterns detected", errors);
        validateAgainstPattern("Process ID", request.getProcessId(), 
            SQL_INJECTION_PATTERN, "SQL injection patterns detected", errors);
        
        // XSS protection
        validateAgainstPattern("Participant ID", request.getParticipantId(), 
            XSS_PATTERN, "XSS patterns detected", errors);
        validateAgainstPattern("Document type ID", request.getDocumentTypeId(), 
            XSS_PATTERN, "XSS patterns detected", errors);
        validateAgainstPattern("Process ID", request.getProcessId(), 
            XSS_PATTERN, "XSS patterns detected", errors);
        
        // XML injection protection
        validateAgainstPattern("Participant ID", request.getParticipantId(), 
            XML_INJECTION_PATTERN, "XML injection patterns detected", errors);
        validateAgainstPattern("Document type ID", request.getDocumentTypeId(), 
            XML_INJECTION_PATTERN, "XML injection patterns detected", errors);
        validateAgainstPattern("Process ID", request.getProcessId(), 
            XML_INJECTION_PATTERN, "XML injection patterns detected", errors);
        
        // Character set validation
        validateCharacterSet("Participant ID", request.getParticipantId(), errors);
        validateCharacterSet("Document type ID", request.getDocumentTypeId(), errors);
        validateCharacterSet("Process ID", request.getProcessId(), errors);
    }
    
    /**
     * Validates Peppol compliance using peppol-commons library.
     */
    private void validatePeppolCompliance(LookupRequest request, List<String> errors, List<String> warnings) {
        if (request == null) {
            return; // Basic validation will catch this
        }
        
        // Validate participant identifier using peppol-commons
        if (request.getParticipantId() != null) {
            try {
                IParticipantIdentifier participantId = identifierFactory.parseParticipantIdentifier(request.getParticipantId());
                if (participantId == null) {
                    errors.add(ErrorCodes.E7006 + ": Invalid participant identifier format according to Peppol standards");
                } else {
                    // Additional validation for participant identifier
                    if (!isValidParticipantIdentifier(participantId)) {
                        warnings.add("Participant identifier may not be registered in Peppol network");
                    }
                }
            } catch (Exception e) {
                errors.add(ErrorCodes.E7006 + ": Failed to parse participant identifier: " + e.getMessage());
            }
        }
        
        // Validate document type identifier using peppol-commons
        if (request.getDocumentTypeId() != null) {
            try {
                IDocumentTypeIdentifier documentTypeId = identifierFactory.parseDocumentTypeIdentifier(request.getDocumentTypeId());
                if (documentTypeId == null) {
                    // For unknown document types, treat as warning rather than error if format is valid
                    if (request.getDocumentTypeId().contains("::")) {
                        warnings.add("Document type identifier is not in the list of predefined Peppol document types");
                    } else {
                        errors.add(ErrorCodes.E7007 + ": Invalid document type identifier format according to Peppol standards");
                    }
                } else {
                    // Check against known Peppol document types
                    if (!isKnownDocumentType(documentTypeId)) {
                        warnings.add("Document type identifier is not in the list of predefined Peppol document types");
                    }
                }
            } catch (Exception e) {
                // For unknown document types, treat as warning rather than error if format is valid
                if (request.getDocumentTypeId().contains("::")) {
                    warnings.add("Document type identifier is not in the list of predefined Peppol document types");
                } else {
                    errors.add(ErrorCodes.E7007 + ": Failed to parse document type identifier: " + e.getMessage());
                }
            }
        }
        
        // Validate process identifier using peppol-commons
        if (request.getProcessId() != null) {
            try {
                IProcessIdentifier processId = identifierFactory.parseProcessIdentifier(request.getProcessId());
                if (processId == null) {
                    // For unknown process identifiers, treat as warning rather than error if format is valid
                    if (request.getProcessId().contains("::")) {
                        warnings.add("Process identifier is not in the list of predefined Peppol processes");
                    } else {
                        errors.add(ErrorCodes.E7008 + ": Invalid process identifier format according to Peppol standards");
                    }
                } else {
                    // Check against known Peppol processes
                    if (!isKnownProcessIdentifier(processId)) {
                        warnings.add("Process identifier is not in the list of predefined Peppol processes");
                    }
                }
            } catch (Exception e) {
                // For unknown process identifiers, treat as warning rather than error if format is valid
                if (request.getProcessId().contains("::")) {
                    warnings.add("Process identifier is not in the list of predefined Peppol processes");
                } else {
                    errors.add(ErrorCodes.E7008 + ": Failed to parse process identifier: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Validates environment configuration.
     */
    private void validateEnvironmentConfiguration(LookupRequest request, List<String> errors) {
        if (request == null) {
            return; // Basic validation will catch this
        }
        
        if (request.getEnvironment() != null) {
            String env = request.getEnvironment().toLowerCase().trim();
            if (!"production".equals(env) && !"test".equals(env)) {
                errors.add(ErrorCodes.E7009 + ": Environment must be either 'production' or 'test'");
            }
        }
        
        // Validate request ID format if provided
        if (request.getRequestId() != null && !request.getRequestId().trim().isEmpty()) {
            if (!request.getRequestId().matches("^[a-zA-Z0-9\\-_]*$")) {
                errors.add(ErrorCodes.E7010 + ": Request ID can only contain alphanumeric characters, hyphens, and underscores");
            }
        }
    }
    
    /**
     * Sanitizes input string by removing potentially dangerous characters.
     */
    public String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        
        // Remove null bytes and control characters
        String sanitized = input.replaceAll("\\x00", "")
                               .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        
        // Trim whitespace
        sanitized = sanitized.trim();
        
        // Limit length
        if (sanitized.length() > MAX_INPUT_LENGTH) {
            sanitized = sanitized.substring(0, MAX_INPUT_LENGTH);
        }
        
        return sanitized;
    }
    
    /**
     * Validates request size for security purposes.
     */
    public boolean isRequestSizeValid(String requestBody) {
        if (requestBody == null) {
            return true;
        }
        
        int sizeInBytes = requestBody.getBytes().length;
        return sizeInBytes <= MAX_REQUEST_SIZE_BYTES;
    }
    
    // Helper methods
    
    private void validateFieldLength(String fieldName, String value, int maxLength, List<String> errors) {
        if (value != null && value.length() > maxLength) {
            errors.add(ErrorCodes.E7011 + ": " + fieldName + " must not exceed " + maxLength + " characters");
        }
    }
    
    private void validateAgainstPattern(String fieldName, String value, Pattern pattern, String errorMessage, List<String> errors) {
        if (value != null && pattern.matcher(value).find()) {
            errors.add(ErrorCodes.E7012 + ": " + fieldName + " contains invalid characters: " + errorMessage);
        }
    }
    
    private void validateCharacterSet(String fieldName, String value, List<String> errors) {
        if (value != null && !SAFE_CHARACTERS_PATTERN.matcher(value).matches()) {
            errors.add(ErrorCodes.E7013 + ": " + fieldName + " contains unsafe characters");
        }
    }
    
    private boolean isValidParticipantIdentifier(IParticipantIdentifier participantId) {
        // Additional validation logic for participant identifiers
        // This could include checking against known schemes, formats, etc.
        return participantId.getScheme() != null && participantId.getValue() != null;
    }
    
    private boolean isKnownDocumentType(IDocumentTypeIdentifier documentTypeId) {
        // Check against predefined Peppol document types
        for (EPredefinedDocumentTypeIdentifier predefined : EPredefinedDocumentTypeIdentifier.values()) {
            if (predefined.getAsDocumentTypeIdentifier().hasSameContent(documentTypeId)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isKnownProcessIdentifier(IProcessIdentifier processId) {
        // Check against predefined Peppol process identifiers
        for (EPredefinedProcessIdentifier predefined : EPredefinedProcessIdentifier.values()) {
            if (predefined.getAsProcessIdentifier().hasSameContent(processId)) {
                return true;
            }
        }
        return false;
    }
    
    private String hashSensitiveData(String data) {
        if (data == null) {
            return null;
        }
        // Simple hash for logging - in production, use proper hashing
        return "hash_" + Math.abs(data.hashCode());
    }
}