# Implementation Plan

**Strategic Approach**: This implementation plan leverages the production-proven peppol-commons library as the foundation while adding Lambda-specific optimizations and enhancements. This approach reduces implementation risk, ensures Peppol compliance, and accelerates development by focusing on AWS-specific requirements rather than rebuilding core Peppol functionality.

- [x] 1. Set up Maven project structure and peppol-commons dependencies
  - Create Maven project with proper directory structure following the developer guide
  - Configure pom.xml with peppol-commons libraries (peppol-commons, peppol-smp-client, peppol-id)
  - Add AWS Lambda, Jackson, HTTP client, XML processing, cryptography, DNS, circuit breaker, validation, logging, testing dependencies
  - Set up build configuration with Shade plugin for Lambda packaging and test plugins
  - Configure Java 17 compilation with parameter names preserved
  - _Requirements: 9.1, 9.2, 16.1_

- [x] 2. Implement core data models and validation annotations
  - Create LookupRequest model with Bean Validation annotations for participant ID, document type, process ID patterns
  - Implement LookupResponse model with all required fields (success, endpoint URL, certificate, validation results)
  - Create ValidationResults model with comprehensive boolean flags for all validation types
  - Implement ErrorDetail model with structured error information (code, category, severity, retryability)
  - Create supporting models (CertificateDetails, TechnicalDetails, SmlResult, SmpResult)
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 8.1, 8.2_

- [x] 3. Create secure XML processing utilities
  - Implement secure XML parser configuration that completely disables DTD processing and external entities
  - Create XML security utility class with entity expansion limits and document size limits
  - Implement XML processing time and memory limits to prevent XML bomb attacks
  - Write comprehensive unit tests for XML security measures against XXE and XML bomb attacks
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 4. Implement SML DNS lookup service with peppol-commons integration
  - Create SmlLookupService leveraging peppol-commons identifier validation
  - Implement MD5 hash calculation method (NOT SHA-256) for participant IDs using peppol-commons utilities where applicable
  - Implement DNS query construction as B-{hex}.{scheme}.{sml-domain} format with lowercase hexadecimal encoding
  - Create DNS resolution with 10-second timeout and 3 retry attempts with exponential backoff (1s, 2s, 4s)
  - Implement environment-specific SML domain configuration (production/test)
  - Add DNSSEC validation when available and DNS result caching
  - Write unit tests for hash calculation, DNS query construction, and retry logic
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 16.2_

- [x] 5. Implement HTTP client with connection pooling and security
  - Create HttpClient with HTTP/2 support and fallback to HTTP/1.1
  - Implement connection pooling across Lambda invocations for performance optimization
  - Configure 30-second HTTP timeout with proper SSL/TLS certificate validation
  - Set up secure cipher suites and HTTPS-only enforcement
  - Add request/response logging and error handling for different HTTP status codes
  - Write unit tests for HTTP client configuration and security features
  - _Requirements: 9.2, 11.4_

- [x] 6. Create SMP query service with peppol-commons integration and secure XML parsing
  - Implement SmpQueryService using peppol-commons PeppolIdentifierFactory for identifier validation
  - Construct SMP URLs as {smpUrl}/{participantId}/services/{documentTypeId} with proper URL encoding
  - Add proper URL encoding for path parameters and HTTP headers (Accept: application/xml)
  - Integrate secure XML parsing to extract endpoint URL, transport profile, certificate, and service dates
  - Implement certificate extraction from Base64 DER format in XML using peppol-commons utilities where applicable
  - Preserve XML signature for later validation
  - Write unit tests for URL construction, XML parsing, and certificate extraction
  - _Requirements: 1.1, 1.5, 11.1, 11.4, 16.2, 16.3_

- [x] 7. Implement certificate processing with peppol-commons integration
  - Create CertificateService that leverages peppol-commons certificate utilities where applicable
  - Implement certificate conversion from DER to PEM format
  - Implement certificate chain building from endpoint certificate to root CA using peppol-commons
  - Add certificate metadata extraction (subject, issuer, validity dates, key algorithm, fingerprints)
  - Create certificate caching mechanism with appropriate TTL
  - Write unit tests for DER/PEM conversion and certificate chain building
  - _Requirements: 4.1, 4.2, 4.3, 16.4_

- [x] 8. Create comprehensive certificate validator leveraging peppol-commons
  - Implement CertificateValidator that leverages peppol-commons certificate validation utilities
  - Add RFC 5280 compliance validation using peppol-commons where available, supplemented with custom validation
  - Implement signature verification using parent public keys and validity period checking
  - Implement key usage validation and minimum 2048-bit RSA key enforcement
  - Create Peppol-specific validation using peppol-commons OpenPeppol root CA verification and certificate policy OID checking
  - Add subject and issuer field validation according to Peppol requirements using peppol-commons utilities
  - Write comprehensive unit tests for all certificate validation scenarios
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 16.4, 16.5_

- [x] 9. Implement OCSP client for certificate revocation checking
  - Create OcspClient that constructs OCSP requests and validates responses
  - Implement 10-second timeout with graceful failure handling
  - Add OCSP response caching based on validity period
  - Implement rate limiting protection for OCSP queries
  - Create fallback mechanism when OCSP is unavailable
  - Write unit tests for OCSP request/response handling and caching
  - _Requirements: 5.1, 5.2, 5.3, 5.5_

- [x] 10. Implement CRL client for certificate revocation list processing
  - Create CrlClient that downloads CRL from distribution points
  - Implement CRL signature validation before processing revocation data
  - Add CRL caching with appropriate TTL based on nextUpdate field
  - Create revocation status checking against CRL data
  - Write unit tests for CRL download, signature validation, and revocation checking
  - _Requirements: 5.2, 5.4, 5.5_

- [x] 11. Create XML signature validator with W3C compliance
  - Implement XmlSignatureValidator that extracts and validates XML digital signatures
  - Add W3C XML Signature specification compliance with Canonical XML 1.0 processing (CRITICAL)
  - Implement signature algorithm validation (RSA-SHA256 or stronger, reject MD5/SHA1)
  - Create signature reference validation and key info validation
  - Verify SMP signing certificate matches extracted certificate
  - Write unit tests for XML signature validation and canonicalization
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [x] 12. Implement transport profile and endpoint validator
  - Create EndpointValidator that validates transport profiles against approved Peppol profiles
  - Add endpoint URL validation according to RFC 3986 with HTTPS enforcement
  - Implement optional endpoint connectivity testing when requested
  - Create SSL certificate matching validation between endpoint and SMP certificate
  - Write unit tests for transport profile validation and endpoint accessibility
  - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_

- [x] 13. Create circuit breaker service for external service protection
  - Implement CircuitBreakerService using resilience4j with configurable failure thresholds
  - Configure circuit breaker to open after 5 consecutive failures with 60-second recovery timeout
  - Add half-open state with 3 test requests and 2 success threshold to close circuit
  - Implement service health tracking and graceful degradation
  - Create metrics collection for circuit breaker state changes
  - Write unit tests for circuit breaker behavior and state transitions
  - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5_

- [x] 14. Implement validation orchestrator to coordinate all validations
  - Create ValidationOrchestrator that coordinates certificate, XML signature, endpoint, and compliance validations
  - Implement validation workflow with proper sequencing and dependency handling
  - Add validation result aggregation and comprehensive reporting
  - Create validation caching and optimization for repeated validations
  - Implement overall Peppol compliance assessment
  - Write unit tests for validation orchestration and result aggregation
  - _Requirements: 4.1, 5.1, 6.1, 11.1, 14.1, 14.2, 14.3, 14.4_

- [x] 15. Create comprehensive error handling and classification system
  - Implement structured error codes (E1xxx-E8xxx) for different error categories
  - Create error classification for SML, SMP, CERTIFICATE, NETWORK, VALIDATION, EXTERNAL_SERVICE, INPUT, SYSTEM
  - Add error severity levels (ERROR, WARNING, INFO) and retryability flags
  - Implement error context collection with technical details and help URLs
  - Create error aggregation for multiple errors in single response
  - Write unit tests for error classification and structured error responses
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 16. Implement structured logging with correlation IDs
  - Create structured JSON logging configuration using Logback
  - Implement request correlation ID generation and propagation across all components
  - Add comprehensive logging for security events, performance metrics, and error contexts
  - Implement privacy protection by hashing sensitive data (participant IDs) in logs
  - Configure appropriate log levels (DEBUG, INFO, WARN, ERROR) and retention policies
  - Write unit tests for logging configuration and correlation ID propagation
  - _Requirements: 8.3, 8.4_

- [x] 17. Create input validation and sanitization components using peppol-commons
  - Implement RequestValidator using peppol-commons PeppolIdentifierFactory for comprehensive input validation
  - Add input sanitization to prevent SQL injection, XSS, and XML injection attacks
  - Create length limits validation (1000 characters max) and character set filtering
  - Implement business logic validation against known Peppol document types and processes using peppol-commons
  - Add rate limiting and request size limits for security
  - Write comprehensive unit tests for input validation and sanitization
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 16.2_

- [x] 18. Implement AWS Lambda handler with proper request/response handling
  - Create PeppolLookupHandler as main Lambda entry point with APIGatewayProxyRequestEvent handling
  - Implement request deserialization with proper error handling and validation
  - Add response serialization with security-conscious data handling
  - Create global exception handling with structured error responses
  - Implement request correlation ID generation and security header validation
  - Write unit tests for Lambda handler request/response processing
  - _Requirements: 1.1, 1.5, 8.1, 8.2_

- [x] 19. Integrate all services and implement main lookup workflow with peppol-commons
  - Wire together SML lookup, SMP query, and all validation services in the main workflow using peppol-commons data structures
  - Implement proper error handling and partial result handling throughout the workflow
  - Add performance timing collection for each workflow step
  - Create comprehensive response building with all required fields leveraging peppol-commons utilities
  - Implement optional features (endpoint connectivity testing, full certificate chain, technical details)
  - Write integration tests for complete end-to-end workflow including peppol-commons integration
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 16.1, 16.5_

- [x] 20. Add CloudWatch metrics and monitoring integration
  - Implement CloudWatch metrics emission for success/failure rates and processing times
  - Add performance metrics for SML lookup time, SMP query time, and validation time
  - Create error metrics by category and external service health metrics
  - Implement custom Peppol-specific metrics like certificate expiry tracking
  - Add resource utilization metrics (memory usage, CPU utilization)
  - Write unit tests for metrics collection and emission
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [x] 21. Implement AWS X-Ray distributed tracing
  - Add AWS X-Ray tracing integration for complete request flow tracking
  - Implement tracing for external service calls (SML, SMP, OCSP, CRL)
  - Add custom annotations for business-specific analysis and performance bottleneck identification
  - Create trace correlation with CloudWatch logs using correlation IDs
  - Configure appropriate sampling strategy for production workloads
  - Write unit tests for X-Ray tracing integration
  - _Requirements: 10.1, 10.4_

- [x] 22. Create comprehensive unit test suite
  - Implement unit tests for all service classes with 90% code coverage target
  - Create mock-based tests for all external dependencies (DNS, HTTP, OCSP, CRL)
  - Add security testing with malicious payloads and attack vectors
  - Implement performance testing for critical paths and memory usage
  - Create edge case and boundary condition tests for all validation logic
  - Add concurrency testing for thread safety verification
  - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_

- [x] 23. Create integration test suite with Peppol test environment
  - Implement end-to-end integration tests against Peppol test network
  - Create tests with real certificate chains and XML signature validation
  - Add network failure simulation and timeout scenario testing
  - Implement performance testing at integration level
  - Create tests for various participant IDs and document types
  - Add environment-specific testing for both production and test flows
  - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_

- [x] 24. Configure AWS Lambda deployment with peppol-commons optimization
  - Create Lambda function configuration with Java 17 runtime and SnapStart enabled for peppol-commons pre-loading
  - Configure 512MB memory allocation and 60-second timeout (monitor for peppol-commons memory usage)
  - Set up environment variables for all configuration parameters
  - Create minimal IAM role with CloudWatch Logs write access only
  - Implement CDK integration for infrastructure as code deployment
  - Add version management and blue/green deployment configuration
  - Optimize Lambda package size considering peppol-commons dependencies
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 13.1, 13.2, 13.3, 13.4, 16.1_

- [x] 25. Verify peppol-commons integration and Peppol compliance
  - Create comprehensive integration tests to verify peppol-commons functionality
  - Test identifier validation using PeppolIdentifierFactory against known Peppol test cases
  - Verify SMP client integration with real Peppol test network endpoints
  - Test certificate validation using peppol-commons against OpenPeppol test certificates
  - Validate compliance with current Peppol specifications through peppol-commons
  - Create performance benchmarks comparing peppol-commons vs custom implementations
  - Document peppol-commons version compatibility and upgrade procedures
  - _Requirements: 16.1, 16.2, 16.3, 16.4, 16.5_

- [x] 26. Set up monitoring, alerting, and operational procedures
  - Configure CloudWatch alarms for error rates > 5% and P99 latency > 30 seconds
  - Set up certificate expiry alerts for certificates expiring within 30 days
  - Create external service availability monitoring and security event alerts
  - Implement escalation procedures and notification channels
  - Create operational dashboards and log analysis queries including peppol-commons metrics
  - Document maintenance procedures and disaster recovery plans
  - Monitor peppol-commons library updates and security advisories
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 13.1, 13.2, 13.3, 13.4_