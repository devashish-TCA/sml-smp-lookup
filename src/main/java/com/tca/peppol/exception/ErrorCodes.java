package com.tca.peppol.exception;

/**
 * Comprehensive error code constants for the Peppol SML/SMP Lookup system.
 * Error codes follow the pattern Exxxx where:
 * - E1xxx: SML-related errors (DNS resolution, hash calculation)
 * - E2xxx: SMP-related errors (HTTP communication, XML parsing)
 * - E3xxx: Certificate-related errors (validation, revocation)
 * - E4xxx: Network-related errors (timeouts, connectivity)
 * - E5xxx: Validation-related errors (compliance, format)
 * - E6xxx: External service errors (OCSP, CRL)
 * - E7xxx: Input validation errors
 * - E8xxx: System errors (configuration, internal)
 */
public final class ErrorCodes {

    private ErrorCodes() {
        // Utility class - prevent instantiation
    }

    // E1xxx: SML-related errors
    public static final String SML_DNS_RESOLUTION_FAILED = "E1001";
    public static final String SML_HASH_CALCULATION_FAILED = "E1002";
    public static final String SML_DNS_QUERY_CONSTRUCTION_FAILED = "E1003";
    public static final String SML_DNS_TIMEOUT = "E1004";
    public static final String SML_DNSSEC_VALIDATION_FAILED = "E1005";
    public static final String SML_INVALID_DOMAIN = "E1006";
    public static final String SML_DNS_SERVER_ERROR = "E1007";
    public static final String SML_PARTICIPANT_NOT_FOUND = "E1008";
    public static final String SML_MULTIPLE_RESULTS = "E1009";
    public static final String SML_CACHE_ERROR = "E1010";

    // E2xxx: SMP-related errors
    public static final String SMP_HTTP_REQUEST_FAILED = "E2001";
    public static final String SMP_XML_PARSING_FAILED = "E2002";
    public static final String SMP_URL_CONSTRUCTION_FAILED = "E2003";
    public static final String SMP_HTTP_TIMEOUT = "E2004";
    public static final String SMP_INVALID_RESPONSE = "E2005";
    public static final String SMP_SERVICE_NOT_FOUND = "E2006";
    public static final String SMP_CERTIFICATE_EXTRACTION_FAILED = "E2007";
    public static final String SMP_XML_SIGNATURE_MISSING = "E2008";
    public static final String SMP_MALFORMED_XML = "E2009";
    public static final String SMP_UNSUPPORTED_VERSION = "E2010";

    // E3xxx: Certificate-related errors
    public static final String CERT_VALIDATION_FAILED = "E3001";
    public static final String CERT_EXPIRED = "E3002";
    public static final String CERT_NOT_YET_VALID = "E3003";
    public static final String CERT_REVOKED = "E3004";
    public static final String CERT_CHAIN_INVALID = "E3005";
    public static final String CERT_SIGNATURE_INVALID = "E3006";
    public static final String CERT_KEY_LENGTH_INSUFFICIENT = "E3007";
    public static final String CERT_POLICY_VIOLATION = "E3008";
    public static final String CERT_NOT_PEPPOL_COMPLIANT = "E3009";
    public static final String CERT_PARSING_FAILED = "E3010";
    public static final String CERT_CHAIN_BUILDING_FAILED = "E3011";
    public static final String CERT_ROOT_CA_INVALID = "E3012";

    // E4xxx: Network-related errors
    public static final String NETWORK_CONNECTION_FAILED = "E4001";
    public static final String NETWORK_TIMEOUT = "E4002";
    public static final String NETWORK_SSL_HANDSHAKE_FAILED = "E4003";
    public static final String NETWORK_DNS_RESOLUTION_FAILED = "E4004";
    public static final String NETWORK_PROXY_ERROR = "E4005";
    public static final String NETWORK_CONNECTION_REFUSED = "E4006";
    public static final String NETWORK_HOST_UNREACHABLE = "E4007";
    public static final String NETWORK_SOCKET_TIMEOUT = "E4008";
    public static final String NETWORK_SSL_CERTIFICATE_ERROR = "E4009";
    public static final String NETWORK_TOO_MANY_REDIRECTS = "E4010";

    // E5xxx: Validation-related errors
    public static final String VALIDATION_XML_SIGNATURE_INVALID = "E5001";
    public static final String VALIDATION_CANONICALIZATION_FAILED = "E5002";
    public static final String VALIDATION_SIGNATURE_ALGORITHM_WEAK = "E5003";
    public static final String VALIDATION_REFERENCE_INVALID = "E5004";
    public static final String VALIDATION_ENDPOINT_INVALID = "E5005";
    public static final String VALIDATION_TRANSPORT_PROFILE_UNSUPPORTED = "E5006";
    public static final String VALIDATION_ENDPOINT_NOT_ACCESSIBLE = "E5007";
    public static final String VALIDATION_PEPPOL_COMPLIANCE_FAILED = "E5008";
    public static final String VALIDATION_BUSINESS_RULES_VIOLATED = "E5009";
    public static final String VALIDATION_SCHEMA_VALIDATION_FAILED = "E5010";

    // E6xxx: External service errors
    public static final String OCSP_REQUEST_FAILED = "E6001";
    public static final String OCSP_RESPONSE_INVALID = "E6002";
    public static final String OCSP_SERVICE_UNAVAILABLE = "E6003";
    public static final String OCSP_TIMEOUT = "E6004";
    public static final String CRL_DOWNLOAD_FAILED = "E6005";
    public static final String CRL_SIGNATURE_INVALID = "E6006";
    public static final String CRL_EXPIRED = "E6007";
    public static final String CRL_PARSING_FAILED = "E6008";
    public static final String EXTERNAL_SERVICE_RATE_LIMITED = "E6009";
    public static final String EXTERNAL_SERVICE_CIRCUIT_BREAKER_OPEN = "E6010";

    // E7xxx: Input validation errors
    public static final String E7001 = "E7001"; // Request validation failed
    public static final String E7002 = "E7002"; // Participant ID required
    public static final String E7003 = "E7003"; // Document type ID required
    public static final String E7004 = "E7004"; // Process ID required
    public static final String E7005 = "E7005"; // Environment required
    public static final String E7006 = "E7006"; // Invalid participant identifier
    public static final String E7007 = "E7007"; // Invalid document type identifier
    public static final String E7008 = "E7008"; // Invalid process identifier
    public static final String E7009 = "E7009"; // Invalid environment
    public static final String E7010 = "E7010"; // Invalid request ID format
    public static final String E7011 = "E7011"; // Field length exceeded
    public static final String E7012 = "E7012"; // Invalid characters detected
    public static final String E7013 = "E7013"; // Unsafe characters detected
    
    // Legacy error codes for backward compatibility
    public static final String INPUT_PARTICIPANT_ID_INVALID = "E7001";
    public static final String INPUT_DOCUMENT_TYPE_INVALID = "E7002";
    public static final String INPUT_PROCESS_ID_INVALID = "E7003";
    public static final String INPUT_ENVIRONMENT_INVALID = "E7004";
    public static final String INPUT_REQUEST_TOO_LARGE = "E7005";
    public static final String INPUT_MALICIOUS_CONTENT_DETECTED = "E7006";
    public static final String INPUT_ENCODING_INVALID = "E7007";
    public static final String INPUT_REQUIRED_FIELD_MISSING = "E7008";
    public static final String INPUT_FORMAT_INVALID = "E7009";
    public static final String INPUT_LENGTH_EXCEEDED = "E7010";

    // E8xxx: System errors
    public static final String SYSTEM_CONFIGURATION_ERROR = "E8001";
    public static final String SYSTEM_INTERNAL_ERROR = "E8002";
    public static final String SYSTEM_MEMORY_ERROR = "E8003";
    public static final String SYSTEM_THREAD_POOL_EXHAUSTED = "E8004";
    public static final String SYSTEM_CACHE_ERROR = "E8005";
    public static final String SYSTEM_SERIALIZATION_ERROR = "E8006";
    public static final String SYSTEM_RESOURCE_UNAVAILABLE = "E8007";
    public static final String SYSTEM_INITIALIZATION_FAILED = "E8008";
    public static final String SYSTEM_SHUTDOWN_ERROR = "E8009";
    public static final String SYSTEM_DEPENDENCY_UNAVAILABLE = "E8010";
}