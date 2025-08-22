# Peppol-Commons Integration Guide

## Overview

This document describes the integration of peppol-commons library into the Peppol SML/SMP Lookup Tool and provides guidance on version compatibility, upgrade procedures, and best practices.

## Current Integration

### Library Versions

- **peppol-commons**: 11.0.0
- **peppol-smp-client**: 11.0.0  
- **peppol-id**: 11.0.0

### Integration Architecture

The system uses a hybrid architecture that leverages peppol-commons as the foundation while adding Lambda-specific optimizations:

```
┌─────────────────────────────────────┐
│           Lambda Layer              │
├─────────────────────────────────────┤
│        Service Layer               │
│  (AWS-specific optimizations)      │
├─────────────────────────────────────┤
│       Peppol-Commons Layer         │
│  (Core Peppol functionality)       │
└─────────────────────────────────────┘
```

### Key Integration Points

#### 1. Identifier Creation and Validation
- **Classes**: `EPredefinedIdentifierIssuingAgency`, `EPredefinedDocumentTypeIdentifier`, `EPredefinedProcessIdentifier`
- **Usage**: Participant ID, Document Type ID, and Process ID creation and validation
- **Integration**: `RequestValidator` class integrates with peppol-commons validation

#### 2. SMP Client Functionality
- **Classes**: `SMPClientReadOnly`, `PeppolURLProvider`, `ESML`
- **Usage**: SMP lookups and service metadata retrieval
- **Integration**: `EnhancedSMPClient` wraps peppol-commons SMP client capabilities

#### 3. Certificate Handling
- **Classes**: Certificate utilities from peppol-commons
- **Usage**: Certificate validation and chain building
- **Integration**: `CertificateValidator` leverages peppol-commons validation

#### 4. Transport Profiles
- **Enum**: `ESMPTransportProfile`
- **Usage**: Transport profile validation and support checking
- **Integration**: `EndpointValidator` uses peppol-commons profiles

## Version Compatibility

### Supported Versions

| Peppol-Commons Version | Java Version | Status | Notes |
|----------------------|--------------|--------|-------|
| 11.0.0 | 17+ | Current | Recommended version |
| 10.x.x | 11+ | Compatible | Legacy support |
| 9.x.x | 8+ | Deprecated | End of life |

### Breaking Changes by Version

#### Version 11.0.0
- Requires Java 17+
- Updated Peppol specifications
- Enhanced security features
- Performance improvements

#### Version 10.x.x
- Requires Java 11+
- Updated identifier validation
- New transport profiles

### Compatibility Testing

Run the following test suites to verify compatibility:

```bash
# Run peppol-commons integration tests
mvn test -Dtest=PeppolCommonsIntegrationTest

# Run compliance validation tests  
mvn test -Dtest=PeppolComplianceValidationTest

# Run performance benchmarks
mvn test -Dtest=PeppolCommonsPerformanceBenchmarkTest
```

## Upgrade Procedures

### Pre-Upgrade Checklist

1. **Review Release Notes**: Check peppol-commons release notes for breaking changes
2. **Backup Configuration**: Save current pom.xml and configuration files
3. **Run Tests**: Execute full test suite with current version
4. **Performance Baseline**: Run performance benchmarks for comparison

### Upgrade Steps

#### 1. Update Dependencies

```xml
<properties>
    <peppol-commons.version>NEW_VERSION</peppol-commons.version>
</properties>
```

#### 2. Update Code (if needed)

Check for deprecated methods and API changes:

```java
// Example: Check if PeppolIdentifierFactory API changed
PeppolIdentifierFactory factory = PeppolIdentifierFactory.getInstance();
// Verify method signatures are still valid
```

#### 3. Run Integration Tests

```bash
# Run all integration tests
mvn test -Dtest=*IntegrationTest

# Run specific peppol-commons tests
mvn test -Dtest=PeppolCommonsIntegrationTest
```

#### 4. Performance Validation

```bash
# Run performance benchmarks
mvn test -Dtest=PeppolCommonsPerformanceBenchmarkTest

# Compare results with baseline
```

#### 5. Compliance Verification

```bash
# Verify Peppol compliance
mvn test -Dtest=PeppolComplianceValidationTest

# Check against official Peppol test cases
export PEPPOL_INTEGRATION_TEST=true
mvn test -Dtest=*IntegrationTest
```

### Post-Upgrade Validation

1. **Functional Testing**: Verify all lookup operations work correctly
2. **Performance Testing**: Ensure no performance regressions
3. **Compliance Testing**: Validate against Peppol specifications
4. **Integration Testing**: Test with real Peppol test network

## Best Practices

### 1. Dependency Management

- Always use the same version for all peppol-commons artifacts
- Pin versions explicitly in pom.xml
- Use dependency management to avoid version conflicts

### 2. Error Handling

```java
try {
    IParticipantIdentifier id = EPredefinedIdentifierIssuingAgency.GLN
        .createParticipantIdentifier(participantValue);
} catch (IllegalArgumentException e) {
    // Handle peppol-commons validation errors
    logger.warn("Invalid participant ID: {}", participantValue, e);
    throw new ValidationException("Invalid participant identifier", e);
}
```

### 3. Performance Optimization

- Cache identifier instances when possible
- Reuse predefined identifier enums
- Monitor memory usage with large volumes
- Use SMPClientReadOnly for read-only operations

### 4. Testing Strategy

- Always test with real Peppol test data
- Include performance regression tests
- Validate against official Peppol compliance tests

## Monitoring and Maintenance

### Key Metrics to Monitor

1. **Performance Metrics**
   - Identifier creation time
   - Memory usage per operation
   - Concurrent operation throughput

2. **Compliance Metrics**
   - Validation success rates
   - Error patterns
   - Specification adherence

3. **Integration Health**
   - External service connectivity
   - Certificate validation success
   - SMP query performance

### Maintenance Schedule

- **Monthly**: Check for peppol-commons updates
- **Quarterly**: Run full compliance validation
- **Annually**: Major version upgrade evaluation

## Troubleshooting

### Common Issues

#### 1. ClassNotFoundException
```
Cause: Missing peppol-commons dependencies
Solution: Verify all required artifacts are included in pom.xml
```

#### 2. IllegalArgumentException on Identifier Creation
```
Cause: Invalid identifier format
Solution: Validate input format against Peppol specifications
```

#### 3. Performance Degradation
```
Cause: Inefficient peppol-commons usage
Solution: Implement caching and object reuse
```

### Debug Configuration

Enable debug logging for peppol-commons:

```xml
<logger name="com.helger.peppol" level="DEBUG"/>
<logger name="com.helger.peppolid" level="DEBUG"/>
```

## Support and Resources

### Official Documentation
- [Peppol-Commons GitHub](https://github.com/phax/peppol-commons)
- [Peppol Specifications](https://peppol.eu/downloads/the-peppol-edelivery-network-specifications/)

### Internal Resources
- Integration test results: `target/surefire-reports/`
- Performance benchmarks: `target/benchmark-results/`
- Compliance reports: `target/compliance-reports/`

### Contact Information
- Technical Lead: [Technical Lead Email]
- Peppol Compliance Officer: [Compliance Officer Email]
- DevOps Team: [DevOps Team Email]