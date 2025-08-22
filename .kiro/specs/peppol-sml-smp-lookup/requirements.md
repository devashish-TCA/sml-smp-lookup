# Requirements Document

## Introduction

The Peppol SML/SMP Lookup Tool is an AWS Lambda-based service that provides comprehensive participant lookup functionality for the Peppol network. This service leverages the production-proven peppol-commons library as its foundation while adding Lambda-specific optimizations and enhancements. The system enables secure discovery of participant endpoints by performing SML (Service Metadata Locator) DNS lookups, SMP (Service Metadata Publisher) queries, and extensive validation of certificates and XML signatures according to Peppol standards. By building upon peppol-commons, the system ensures compliance with Peppol specifications while reducing implementation risk and accelerating development. The system must handle both production and test environments while maintaining high security, performance, and compliance with Peppol AP certification requirements.

## Requirements

### Requirement 1

**User Story:** As a Peppol network participant, I want to lookup endpoint information for other participants, so that I can establish secure document exchange connections.

#### Acceptance Criteria

1. WHEN a valid participant ID, document type ID, and process ID are provided THEN the system SHALL return the participant's endpoint URL and certificate information
2. WHEN the environment parameter is "production" THEN the system SHALL use the production SML domain (edelivery.tech.ec.europa.eu)
3. WHEN the environment parameter is "test" THEN the system SHALL use the test SML domain (acc.edelivery.tech.ec.europa.eu)
4. WHEN UAE participants are queried THEN the system SHALL use the same EU domains as confirmed by research
5. WHEN the lookup is successful THEN the system SHALL return endpoint URL, transport profile, certificate in PEM format, and service dates

### Requirement 2

**User Story:** As a system administrator, I want comprehensive input validation using peppol-commons, so that the service is protected against malicious inputs and ensures Peppol compliance.

#### Acceptance Criteria

1. WHEN participant ID is provided THEN the system SHALL validate it using PeppolIdentifierFactory from peppol-commons
2. WHEN document type ID is provided THEN the system SHALL validate it using peppol-commons document type validation
3. WHEN process ID is provided THEN the system SHALL validate it using peppol-commons process identifier validation
4. WHEN any input exceeds 1000 characters THEN the system SHALL reject the request with appropriate error
5. WHEN malicious patterns are detected THEN the system SHALL sanitize inputs and prevent SQL injection, XSS, and XML injection attacks

### Requirement 3

**User Story:** As a Peppol network participant, I want accurate SML DNS resolution using proper hashing, so that I can locate the correct SMP for any participant.

#### Acceptance Criteria

1. WHEN performing SML lookup THEN the system SHALL use MD5 hashing (NOT SHA-256) as per Peppol specification
2. WHEN generating DNS query THEN the system SHALL convert MD5 hash to lowercase hexadecimal encoding
3. WHEN constructing DNS query THEN the system SHALL format as B-{hex}.{scheme}.{sml-domain}
4. WHEN DNS resolution fails THEN the system SHALL retry 3 times with exponential backoff (1s, 2s, 4s delays)
5. WHEN DNS timeout occurs THEN the system SHALL fail after 10 seconds with proper error classification

### Requirement 4

**User Story:** As a security-conscious system, I want comprehensive certificate validation, so that only valid Peppol certificates are accepted.

#### Acceptance Criteria

1. WHEN certificate is received THEN the system SHALL validate complete RFC 5280 certificate chain
2. WHEN validating certificate chain THEN the system SHALL verify signatures using parent public keys
3. WHEN checking certificate validity THEN the system SHALL verify notBefore and notAfter dates
4. WHEN validating Peppol compliance THEN the system SHALL ensure chain terminates at proper OpenPeppol root CA
5. WHEN checking key requirements THEN the system SHALL enforce minimum 2048-bit RSA keys
6. WHEN validating certificate policies THEN the system SHALL check Peppol-specific certificate policy OIDs

### Requirement 5

**User Story:** As a security-conscious system, I want certificate revocation checking, so that revoked certificates are properly identified and rejected.

#### Acceptance Criteria

1. WHEN certificate is validated THEN the system SHALL check OCSP responder for real-time revocation status
2. WHEN OCSP is unavailable THEN the system SHALL fallback to CRL (Certificate Revocation List) checking
3. WHEN OCSP query times out THEN the system SHALL fail gracefully after 10 seconds
4. WHEN CRL is processed THEN the system SHALL verify CRL signatures before processing
5. WHEN revocation services are unavailable THEN the system SHALL handle failures gracefully with appropriate warnings

### Requirement 6

**User Story:** As a Peppol compliance officer, I want XML signature validation according to W3C standards, so that document integrity and authenticity are guaranteed.

#### Acceptance Criteria

1. WHEN XML signature is processed THEN the system SHALL validate according to W3C XML Signature specification
2. WHEN performing canonicalization THEN the system SHALL use Canonical XML 1.0 (CRITICAL requirement)
3. WHEN validating signature algorithm THEN the system SHALL accept RSA-SHA256 or stronger algorithms only
4. WHEN checking signature references THEN the system SHALL validate all signature references and transforms
5. WHEN weak algorithms are detected THEN the system SHALL reject MD5 and SHA1 signatures

### Requirement 7

**User Story:** As a system administrator, I want comprehensive security protection against XML attacks, so that the service is protected from XXE and XML bomb attacks.

#### Acceptance Criteria

1. WHEN processing XML THEN the system SHALL completely disable DTD processing
2. WHEN parsing XML THEN the system SHALL disable external entity resolution
3. WHEN handling XML entities THEN the system SHALL set strict limits on entity expansion
4. WHEN processing large XML THEN the system SHALL implement document size limits
5. WHEN XML processing takes too long THEN the system SHALL set limits on processing time and memory

### Requirement 8

**User Story:** As a system operator, I want robust error handling and monitoring, so that issues can be quickly identified and resolved.

#### Acceptance Criteria

1. WHEN errors occur THEN the system SHALL classify them with structured error codes (E1001, E2001, etc.)
2. WHEN errors are categorized THEN the system SHALL use categories: SML, SMP, CERTIFICATE, NETWORK, VALIDATION, EXTERNAL_SERVICE, INPUT, SYSTEM
3. WHEN logging events THEN the system SHALL use structured JSON logging with correlation IDs
4. WHEN sensitive data is logged THEN the system SHALL hash participant IDs and protect privacy
5. WHEN errors are retryable THEN the system SHALL mark them appropriately for client retry logic

### Requirement 9

**User Story:** As a performance-conscious system, I want optimized AWS Lambda deployment leveraging peppol-commons, so that cold starts are minimized and performance is maximized.

#### Acceptance Criteria

1. WHEN Lambda function starts THEN the system SHALL use Java 17 runtime with SnapStart enabled and peppol-commons library pre-loaded
2. WHEN connections are needed THEN the system SHALL implement HTTP connection pooling across invocations
3. WHEN caching is beneficial THEN the system SHALL cache certificates, DNS results, CRL/OCSP responses, and peppol-commons objects
4. WHEN memory is allocated THEN the system SHALL start with 512MB and monitor for optimization considering peppol-commons memory usage
5. WHEN timeout occurs THEN the system SHALL fail after maximum 60 seconds

### Requirement 10

**User Story:** As a compliance officer, I want comprehensive monitoring and observability, so that system health and compliance can be continuously verified.

#### Acceptance Criteria

1. WHEN processing requests THEN the system SHALL emit CloudWatch metrics for success/failure rates and processing times
2. WHEN errors exceed thresholds THEN the system SHALL trigger alarms when error rates exceed 5% over 5-minute window
3. WHEN latency is high THEN the system SHALL alert when P99 latency exceeds 30 seconds
4. WHEN certificates approach expiry THEN the system SHALL alert when certificates expire within 30 days
5. WHEN security events occur THEN the system SHALL provide immediate alerts for security violations

### Requirement 11

**User Story:** As a Peppol network participant, I want support for all approved transport profiles, so that I can connect using any valid Peppol transport method.

#### Acceptance Criteria

1. WHEN transport profile is validated THEN the system SHALL support currently approved Peppol AS4 transport profiles
2. WHEN specific profiles are checked THEN the system SHALL validate against latest Peppol transport profile specifications
3. WHEN endpoint connectivity is requested THEN the system SHALL optionally test endpoint accessibility
4. WHEN HTTPS is required THEN the system SHALL ensure all endpoints use HTTPS protocol only
5. WHEN certificate matching is needed THEN the system SHALL verify endpoint SSL certificate matches SMP certificate

### Requirement 12

**User Story:** As a system administrator, I want comprehensive testing coverage, so that the system reliability and security are thoroughly validated.

#### Acceptance Criteria

1. WHEN code is tested THEN the system SHALL achieve minimum 90% code coverage
2. WHEN security is tested THEN the system SHALL test against malicious payloads and attack vectors
3. WHEN integration is tested THEN the system SHALL test against real Peppol test services
4. WHEN Peppol compliance is tested THEN the system SHALL run official Peppol compliance tests
5. WHEN performance is tested THEN the system SHALL test unit-level and integration-level performance


### Requirement 13

**User Story:** As a system administrator, I want centralized configuration management, 
so that the system can be properly maintained and updated.

Acceptance Criteria:
1. WHEN configuration is needed THEN the system SHALL use environment variables for all parameters
2. WHEN CA certificates are updated THEN the system SHALL support OpenPeppol CA certificate rotation
3. WHEN Peppol standards change THEN the system SHALL support specification updates
4. WHEN features are disabled THEN the system SHALL support feature flags without redeployment
5. WHEN environments differ THEN the system SHALL maintain separate test and production configurations

### Requirement 14

**User Story:** As a compliance officer, I want the system to meet all Peppol AP certification requirements,
so that we can obtain and maintain AP provider status.

Acceptance Criteria:
1. WHEN certification is required THEN the system SHALL pass all official Peppol test cases
2. WHEN SML lookup is tested THEN the system SHALL demonstrate proper MD5 hashing compliance
3. WHEN XML signatures are tested THEN the system SHALL demonstrate W3C compliance
4. WHEN certificate validation is tested THEN the system SHALL meet all Peppol CA requirements
5. WHEN documentation is needed THEN the system SHALL maintain complete certification documentation

### Requirement 15

**User Story:** As a system operator, I want circuit breaker protection for external services,
so that the system remains stable during external service failures.

Acceptance Criteria:
1. WHEN external services fail THEN the system SHALL implement circuit breaker pattern
2. WHEN failure thresholds are reached THEN the system SHALL open circuit after 5 consecutive failures
3. WHEN circuit is open THEN the system SHALL attempt recovery after 60 seconds
4. WHEN rate limits are hit THEN the system SHALL implement backoff for external service calls
5. WHEN services recover THEN the system SHALL gradually resume normal operation

### Requirement 16

**User Story:** As a development team, I want to leverage the production-proven peppol-commons library,
so that we can ensure Peppol compliance while reducing implementation risk and development time.

#### Acceptance Criteria

1. WHEN implementing Peppol functionality THEN the system SHALL use peppol-commons as the primary foundation
2. WHEN validating identifiers THEN the system SHALL use PeppolIdentifierFactory and related peppol-commons classes
3. WHEN processing SMP data THEN the system SHALL leverage peppol-commons SMP client capabilities where appropriate
4. WHEN handling certificates THEN the system SHALL integrate with peppol-commons certificate handling utilities
5. WHEN ensuring compliance THEN the system SHALL rely on peppol-commons built-in Peppol specification compliance