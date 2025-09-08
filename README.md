# Peppol SML/SMP Lookup Tool

AWS Lambda-based service that provides comprehensive participant lookup functionality for the Peppol network.

## Overview

This service enables secure discovery of participant endpoints by performing:
- SML (Service Metadata Locator) DNS lookups with proper MD5 hashing
- SMP (Service Metadata Publisher) queries with secure XML parsing
- Comprehensive certificate validation including revocation checking
- XML signature validation according to W3C standards
- Full compliance with Peppol AP certification requirements

## Features

- **Security First**: Comprehensive input validation, XML security, and cryptographic validation
- **Peppol Compliant**: Full compliance with Peppol specifications and OpenPeppol CA requirements
- **High Performance**: Optimized for AWS Lambda with SnapStart, connection pooling, and caching
- **Comprehensive Monitoring**: CloudWatch metrics, X-Ray tracing, and structured logging
- **Test Coverage**: 90% code coverage target with comprehensive test suites

## Architecture

The system follows a layered architecture:
- **Handler Layer**: AWS Lambda entry points and request/response handling
- **Service Layer**: Business logic orchestration and workflow coordination
- **Client Layer**: External service communication (DNS, HTTP, OCSP, CRL)
- **Validation Layer**: Certificate, XML signature, and compliance validation
- **Model Layer**: Data models and DTOs

## Build and Deploy

```bash
# Build the project
mvn clean compile

# Run tests
mvn test

# Run integration tests
mvn integration-test

# Package for Lambda deployment
mvn package

# Security scan
mvn dependency-check:check

# Code quality analysis
mvn spotbugs:check pmd:check
```

## Configuration

All configuration is managed through environment variables. See `application.properties` for available settings.

## Testing

- Unit tests: `mvn test`
- Integration tests: `mvn integration-test`
- Coverage report: `target/site/jacoco/index.html`

## Security

This project follows security best practices:
- Input validation and sanitization
- XML security (XXE prevention, entity expansion limits)
- Comprehensive certificate validation
- Secure cryptographic operations
- Privacy protection in logging

## Compliance

- Peppol AP certification requirements
- W3C XML Signature specification
- RFC 5280 certificate validation
- OWASP security guidelines