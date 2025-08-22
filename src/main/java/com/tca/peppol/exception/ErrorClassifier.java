package com.tca.peppol.exception;

import com.tca.peppol.model.response.ErrorDetail;
import com.tca.peppol.model.response.ErrorDetail.ErrorCategory;
import com.tca.peppol.model.response.ErrorDetail.ErrorSeverity;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for classifying and creating structured error responses.
 * Provides methods to create ErrorDetail instances with proper categorization,
 * severity levels, and retryability flags based on error codes.
 */
public final class ErrorClassifier {

    private ErrorClassifier() {
        // Utility class - prevent instantiation
    }

    // Base help URL for error documentation
    private static final String HELP_URL_BASE = "https://docs.peppol-lookup.com/errors/";

    // Error code to category mapping
    private static final Map<String, ErrorCategory> ERROR_CATEGORIES = new HashMap<>();
    
    // Error code to retryability mapping
    private static final Map<String, Boolean> ERROR_RETRYABILITY = new HashMap<>();
    
    // Error code to severity mapping
    private static final Map<String, ErrorSeverity> ERROR_SEVERITIES = new HashMap<>();

    static {
        initializeErrorMappings();
    }

    /**
     * Initialize error code mappings for categories, retryability, and severities
     */
    private static void initializeErrorMappings() {
        // SML errors (E1xxx)
        mapError(ErrorCodes.SML_DNS_RESOLUTION_FAILED, ErrorCategory.SML, ErrorSeverity.ERROR, true);
        mapError(ErrorCodes.SML_HASH_CALCULATION_FAILED, ErrorCategory.SML, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.SML_DNS_QUERY_CONSTRUCTION_FAILED, ErrorCategory.SML, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.SML_DNS_TIMEOUT, ErrorCategory.SML, ErrorSeverity.ERROR, true);
        mapError(ErrorCodes.SML_DNSSEC_VALIDATION_FAILED, ErrorCategory.SML, ErrorSeverity.WARNING, false);
        mapError(ErrorCodes.SML_INVALID_DOMAIN, ErrorCategory.SML, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.SML_DNS_SERVER_ERROR, ErrorCategory.SML, ErrorSeverity.ERROR, true);
        mapError(ErrorCodes.SML_PARTICIPANT_NOT_FOUND, ErrorCategory.SML, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.SML_MULTIPLE_RESULTS, ErrorCategory.SML, ErrorSeverity.WARNING, false);
        mapError(ErrorCodes.SML_CACHE_ERROR, ErrorCategory.SML, ErrorSeverity.WARNING, true);

        // SMP errors (E2xxx)
        mapError(ErrorCodes.SMP_HTTP_REQUEST_FAILED, ErrorCategory.SMP, ErrorSeverity.ERROR, true);
        mapError(ErrorCodes.SMP_XML_PARSING_FAILED, ErrorCategory.SMP, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.SMP_URL_CONSTRUCTION_FAILED, ErrorCategory.SMP, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.SMP_HTTP_TIMEOUT, ErrorCategory.SMP, ErrorSeverity.ERROR, true);
        mapError(ErrorCodes.SMP_INVALID_RESPONSE, ErrorCategory.SMP, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.SMP_SERVICE_NOT_FOUND, ErrorCategory.SMP, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.SMP_CERTIFICATE_EXTRACTION_FAILED, ErrorCategory.SMP, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.SMP_XML_SIGNATURE_MISSING, ErrorCategory.SMP, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.SMP_MALFORMED_XML, ErrorCategory.SMP, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.SMP_UNSUPPORTED_VERSION, ErrorCategory.SMP, ErrorSeverity.ERROR, false);

        // Certificate errors (E3xxx)
        mapError(ErrorCodes.CERT_VALIDATION_FAILED, ErrorCategory.CERTIFICATE, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.CERT_EXPIRED, ErrorCategory.CERTIFICATE, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.CERT_NOT_YET_VALID, ErrorCategory.CERTIFICATE, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.CERT_REVOKED, ErrorCategory.CERTIFICATE, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.CERT_CHAIN_INVALID, ErrorCategory.CERTIFICATE, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.CERT_SIGNATURE_INVALID, ErrorCategory.CERTIFICATE, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.CERT_KEY_LENGTH_INSUFFICIENT, ErrorCategory.CERTIFICATE, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.CERT_POLICY_VIOLATION, ErrorCategory.CERTIFICATE, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.CERT_NOT_PEPPOL_COMPLIANT, ErrorCategory.CERTIFICATE, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.CERT_PARSING_FAILED, ErrorCategory.CERTIFICATE, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.CERT_CHAIN_BUILDING_FAILED, ErrorCategory.CERTIFICATE, ErrorSeverity.ERROR, true);
        mapError(ErrorCodes.CERT_ROOT_CA_INVALID, ErrorCategory.CERTIFICATE, ErrorSeverity.ERROR, false);

        // Network errors (E4xxx)
        mapError(ErrorCodes.NETWORK_CONNECTION_FAILED, ErrorCategory.NETWORK, ErrorSeverity.ERROR, true);
        mapError(ErrorCodes.NETWORK_TIMEOUT, ErrorCategory.NETWORK, ErrorSeverity.ERROR, true);
        mapError(ErrorCodes.NETWORK_SSL_HANDSHAKE_FAILED, ErrorCategory.NETWORK, ErrorSeverity.ERROR, true);
        mapError(ErrorCodes.NETWORK_DNS_RESOLUTION_FAILED, ErrorCategory.NETWORK, ErrorSeverity.ERROR, true);
        mapError(ErrorCodes.NETWORK_PROXY_ERROR, ErrorCategory.NETWORK, ErrorSeverity.ERROR, true);
        mapError(ErrorCodes.NETWORK_CONNECTION_REFUSED, ErrorCategory.NETWORK, ErrorSeverity.ERROR, true);
        mapError(ErrorCodes.NETWORK_HOST_UNREACHABLE, ErrorCategory.NETWORK, ErrorSeverity.ERROR, true);
        mapError(ErrorCodes.NETWORK_SOCKET_TIMEOUT, ErrorCategory.NETWORK, ErrorSeverity.ERROR, true);
        mapError(ErrorCodes.NETWORK_SSL_CERTIFICATE_ERROR, ErrorCategory.NETWORK, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.NETWORK_TOO_MANY_REDIRECTS, ErrorCategory.NETWORK, ErrorSeverity.ERROR, false);

        // Validation errors (E5xxx)
        mapError(ErrorCodes.VALIDATION_XML_SIGNATURE_INVALID, ErrorCategory.VALIDATION, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.VALIDATION_CANONICALIZATION_FAILED, ErrorCategory.VALIDATION, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.VALIDATION_SIGNATURE_ALGORITHM_WEAK, ErrorCategory.VALIDATION, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.VALIDATION_REFERENCE_INVALID, ErrorCategory.VALIDATION, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.VALIDATION_ENDPOINT_INVALID, ErrorCategory.VALIDATION, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.VALIDATION_TRANSPORT_PROFILE_UNSUPPORTED, ErrorCategory.VALIDATION, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.VALIDATION_ENDPOINT_NOT_ACCESSIBLE, ErrorCategory.VALIDATION, ErrorSeverity.WARNING, true);
        mapError(ErrorCodes.VALIDATION_PEPPOL_COMPLIANCE_FAILED, ErrorCategory.VALIDATION, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.VALIDATION_BUSINESS_RULES_VIOLATED, ErrorCategory.VALIDATION, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.VALIDATION_SCHEMA_VALIDATION_FAILED, ErrorCategory.VALIDATION, ErrorSeverity.ERROR, false);

        // External service errors (E6xxx)
        mapError(ErrorCodes.OCSP_REQUEST_FAILED, ErrorCategory.EXTERNAL_SERVICE, ErrorSeverity.WARNING, true);
        mapError(ErrorCodes.OCSP_RESPONSE_INVALID, ErrorCategory.EXTERNAL_SERVICE, ErrorSeverity.WARNING, true);
        mapError(ErrorCodes.OCSP_SERVICE_UNAVAILABLE, ErrorCategory.EXTERNAL_SERVICE, ErrorSeverity.WARNING, true);
        mapError(ErrorCodes.OCSP_TIMEOUT, ErrorCategory.EXTERNAL_SERVICE, ErrorSeverity.WARNING, true);
        mapError(ErrorCodes.CRL_DOWNLOAD_FAILED, ErrorCategory.EXTERNAL_SERVICE, ErrorSeverity.WARNING, true);
        mapError(ErrorCodes.CRL_SIGNATURE_INVALID, ErrorCategory.EXTERNAL_SERVICE, ErrorSeverity.WARNING, false);
        mapError(ErrorCodes.CRL_EXPIRED, ErrorCategory.EXTERNAL_SERVICE, ErrorSeverity.WARNING, false);
        mapError(ErrorCodes.CRL_PARSING_FAILED, ErrorCategory.EXTERNAL_SERVICE, ErrorSeverity.WARNING, false);
        mapError(ErrorCodes.EXTERNAL_SERVICE_RATE_LIMITED, ErrorCategory.EXTERNAL_SERVICE, ErrorSeverity.WARNING, true);
        mapError(ErrorCodes.EXTERNAL_SERVICE_CIRCUIT_BREAKER_OPEN, ErrorCategory.EXTERNAL_SERVICE, ErrorSeverity.WARNING, true);

        // Input validation errors (E7xxx)
        mapError(ErrorCodes.INPUT_PARTICIPANT_ID_INVALID, ErrorCategory.INPUT, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.INPUT_DOCUMENT_TYPE_INVALID, ErrorCategory.INPUT, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.INPUT_PROCESS_ID_INVALID, ErrorCategory.INPUT, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.INPUT_ENVIRONMENT_INVALID, ErrorCategory.INPUT, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.INPUT_REQUEST_TOO_LARGE, ErrorCategory.INPUT, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.INPUT_MALICIOUS_CONTENT_DETECTED, ErrorCategory.INPUT, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.INPUT_ENCODING_INVALID, ErrorCategory.INPUT, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.INPUT_REQUIRED_FIELD_MISSING, ErrorCategory.INPUT, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.INPUT_FORMAT_INVALID, ErrorCategory.INPUT, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.INPUT_LENGTH_EXCEEDED, ErrorCategory.INPUT, ErrorSeverity.ERROR, false);

        // System errors (E8xxx)
        mapError(ErrorCodes.SYSTEM_CONFIGURATION_ERROR, ErrorCategory.SYSTEM, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.SYSTEM_INTERNAL_ERROR, ErrorCategory.SYSTEM, ErrorSeverity.ERROR, true);
        mapError(ErrorCodes.SYSTEM_MEMORY_ERROR, ErrorCategory.SYSTEM, ErrorSeverity.ERROR, true);
        mapError(ErrorCodes.SYSTEM_THREAD_POOL_EXHAUSTED, ErrorCategory.SYSTEM, ErrorSeverity.ERROR, true);
        mapError(ErrorCodes.SYSTEM_CACHE_ERROR, ErrorCategory.SYSTEM, ErrorSeverity.WARNING, true);
        mapError(ErrorCodes.SYSTEM_SERIALIZATION_ERROR, ErrorCategory.SYSTEM, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.SYSTEM_RESOURCE_UNAVAILABLE, ErrorCategory.SYSTEM, ErrorSeverity.ERROR, true);
        mapError(ErrorCodes.SYSTEM_INITIALIZATION_FAILED, ErrorCategory.SYSTEM, ErrorSeverity.ERROR, false);
        mapError(ErrorCodes.SYSTEM_SHUTDOWN_ERROR, ErrorCategory.SYSTEM, ErrorSeverity.WARNING, false);
        mapError(ErrorCodes.SYSTEM_DEPENDENCY_UNAVAILABLE, ErrorCategory.SYSTEM, ErrorSeverity.ERROR, true);
    }

    /**
     * Helper method to map error codes to their properties
     */
    private static void mapError(String errorCode, ErrorCategory category, ErrorSeverity severity, boolean retryable) {
        ERROR_CATEGORIES.put(errorCode, category);
        ERROR_SEVERITIES.put(errorCode, severity);
        ERROR_RETRYABILITY.put(errorCode, retryable);
    }

    /**
     * Create a structured ErrorDetail with automatic classification
     */
    public static ErrorDetail createError(String errorCode, String message) {
        return createError(errorCode, message, null, null);
    }

    /**
     * Create a structured ErrorDetail with technical details
     */
    public static ErrorDetail createError(String errorCode, String message, String technicalDetails) {
        return createError(errorCode, message, technicalDetails, null);
    }

    /**
     * Create a structured ErrorDetail with technical details and exception
     */
    public static ErrorDetail createError(String errorCode, String message, String technicalDetails, Throwable exception) {
        ErrorCategory category = ERROR_CATEGORIES.getOrDefault(errorCode, ErrorCategory.SYSTEM);
        ErrorSeverity severity = ERROR_SEVERITIES.getOrDefault(errorCode, ErrorSeverity.ERROR);
        boolean retryable = ERROR_RETRYABILITY.getOrDefault(errorCode, false);

        String enhancedTechnicalDetails = technicalDetails;
        if (exception != null) {
            enhancedTechnicalDetails = (technicalDetails != null ? technicalDetails + ". " : "") +
                    "Exception: " + exception.getClass().getSimpleName() + " - " + exception.getMessage();
        }

        return new ErrorDetail.Builder(errorCode, category, severity, message)
                .technicalDetails(enhancedTechnicalDetails)
                .retryable(retryable)
                .helpUrl(HELP_URL_BASE + errorCode.toLowerCase())
                .build();
    }

    /**
     * Create an ErrorDetail with additional context
     */
    public static ErrorDetail createErrorWithContext(String errorCode, String message, 
                                                   String technicalDetails, Map<String, Object> context) {
        ErrorDetail error = createError(errorCode, message, technicalDetails);
        if (context != null) {
            error.setAdditionalContext(new HashMap<>(context));
        }
        return error;
    }

    /**
     * Get error category for a given error code
     */
    public static ErrorCategory getErrorCategory(String errorCode) {
        return ERROR_CATEGORIES.getOrDefault(errorCode, ErrorCategory.SYSTEM);
    }

    /**
     * Check if an error is retryable
     */
    public static boolean isRetryable(String errorCode) {
        return ERROR_RETRYABILITY.getOrDefault(errorCode, false);
    }

    /**
     * Get error severity for a given error code
     */
    public static ErrorSeverity getErrorSeverity(String errorCode) {
        return ERROR_SEVERITIES.getOrDefault(errorCode, ErrorSeverity.ERROR);
    }

    /**
     * Check if error code is valid
     */
    public static boolean isValidErrorCode(String errorCode) {
        return ERROR_CATEGORIES.containsKey(errorCode);
    }

    /**
     * Get help URL for error code
     */
    public static String getHelpUrl(String errorCode) {
        return HELP_URL_BASE + errorCode.toLowerCase();
    }
}