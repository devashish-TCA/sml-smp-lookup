# Peppol SML/SMP Lookup Service - Lambda Capability

This directory contains the build and deployment scripts for the Peppol SML/SMP Lookup Service Lambda capability, optimized for peppol-commons library integration.

## Overview

This capability provides:
- Optimized Lambda package building for peppol-commons library usage
- Environment-specific configurations for different deployment stages
- Lambda function testing and validation scripts
- Package optimization for AWS Lambda constraints
- Peppol-commons integration validation

## Prerequisites

- AWS CLI v2 configured with appropriate permissions
- Java 17 JDK
- Maven 3.8+
- jq (for JSON processing in scripts)
- Existing Lambda function (created via your CDK infrastructure)

## Quick Start

### 1. Build Lambda Package

```bash
# Build optimized Lambda package for development
./deploy/deploy.sh dev

# Build for production
./deploy/deploy.sh production
```

### 2. Update Existing Lambda Function

```bash
# Update your Lambda function with the new package
./deploy/blue-green-deploy.sh your-lambda-function-name us-east-1 default

# Test the updated function
./deploy/test-deployment.sh your-lambda-function-name us-east-1 default
```

## Deployment Scripts

### `deploy.sh`

Lambda package builder that:
- Builds optimized Lambda package with peppol-commons
- Validates peppol-commons dependencies
- Creates deployment-ready JAR file
- Validates package size and contents

**Usage:**
```bash
./deploy/deploy.sh [environment]
```

**Parameters:**
- `environment`: dev, test, staging, production (default: dev)

### `blue-green-deploy.sh`

Lambda function updater that:
- Updates existing Lambda function with new package
- Tests the updated function
- Provides rollback information if needed

**Usage:**
```bash
./deploy/blue-green-deploy.sh <function-name> [region] [aws-profile]
```

### `test-deployment.sh`

Lambda function testing script that:
- Tests health endpoint via direct Lambda invocation
- Validates Peppol lookup functionality
- Verifies peppol-commons integration
- Checks error handling
- Reports performance metrics

**Usage:**
```bash
./deploy/test-deployment.sh <function-name> [region] [aws-profile]
```

### `optimize-lambda.sh`

Lambda optimization script that:
- Configures memory and timeout based on environment
- Enables SnapStart for peppol-commons pre-loading
- Sets up CloudWatch alarms
- Configures dead letter queues

## Environment Configurations

### Development (`dev.env`)
- Relaxed timeouts for debugging
- Debug logging enabled
- Shorter cache TTL
- DNSSEC validation disabled
- Endpoint connectivity testing enabled

### Test (`test.env`)
- Balanced configuration for integration testing
- Info-level logging
- Standard timeouts
- All security features enabled

### Production (`production.env`)
- Optimized for performance and reliability
- Strict security settings
- Longer cache TTL
- Comprehensive monitoring

## Lambda Configuration Recommendations

### Memory and Performance
- **Development**: 512MB memory, 90s timeout
- **Test**: 512MB memory, 60s timeout  
- **Production**: 768MB memory, 60s timeout

Memory allocation is optimized for peppol-commons library usage, which can be memory-intensive during certificate processing and XML validation.

### SnapStart Optimization
When configuring your Lambda function, enable SnapStart to pre-load the peppol-commons library:
- Reduces cold start from ~10s to ~2s
- Pre-initializes peppol-commons classes
- Improves first request performance

### Environment Variables

Key configuration parameters:

```bash
# Peppol Configuration
PEPPOL_ENVIRONMENT=production
SML_DOMAIN_PRODUCTION=edelivery.tech.ec.europa.eu
SML_DOMAIN_TEST=acc.edelivery.tech.ec.europa.eu

# Performance Tuning
DNS_TIMEOUT_SECONDS=10
HTTP_TIMEOUT_SECONDS=30
CACHE_TTL_SECONDS=3600

# Peppol-Commons Optimization
PEPPOL_COMMONS_CACHE_ENABLED=true
PEPPOL_COMMONS_VALIDATION_STRICT=true
```

## Package Optimization

The Maven Shade plugin is configured to:
- Preserve all peppol-commons classes and resources
- Exclude unnecessary files to reduce package size
- Maintain service configurations for peppol-commons
- Target package size under 250MB for optimal Lambda performance

## Monitoring Recommendations

### CloudWatch Metrics
The Lambda function publishes custom metrics for:
- Peppol-specific operations (SML lookup time, certificate validation)
- Success rates and error categorization
- Performance timings

### Recommended Alarms
Consider setting up CloudWatch alarms for:
- Error rate > 5% over 5 minutes
- Average duration > 30 seconds
- Certificate validation failures
- SML lookup failures
- Memory pressure indicators

## Security Considerations

### IAM Permissions
Recommended minimal IAM permissions for the Lambda function:
- CloudWatch Logs write access
- CloudWatch Metrics write access
- X-Ray tracing permissions

### Network Security
- HTTPS-only communication
- Certificate validation for all external calls
- DNS over HTTPS support
- Secure cipher suites only

### Input Validation
- Comprehensive input sanitization
- Peppol identifier validation using peppol-commons
- XSS and injection attack prevention

## Troubleshooting

### Common Issues

1. **Large Package Size**
   - Check peppol-commons dependencies are not duplicated
   - Verify exclusions in Maven Shade plugin
   - Consider using Lambda layers for large dependencies

2. **Cold Start Performance**
   - Ensure SnapStart is enabled
   - Check memory allocation (may need increase for peppol-commons)
   - Verify peppol-commons caching is enabled

3. **Peppol Validation Failures**
   - Check peppol-commons library version compatibility
   - Verify Peppol test network connectivity
   - Review certificate validation configuration

4. **Memory Issues**
   - Monitor CloudWatch memory metrics
   - Consider increasing memory allocation
   - Check for memory leaks in peppol-commons usage

### Debugging

Enable debug logging:
```bash
# Set LOG_LEVEL=DEBUG in environment configuration
aws lambda update-function-configuration \
  --function-name peppol-lookup-dev \
  --environment Variables='{LOG_LEVEL=DEBUG}'
```

Check CloudWatch logs:
```bash
aws logs tail /aws/lambda/peppol-lookup-dev --follow
```

## Version Management

### Versioning Strategy
- Each deployment creates a new Lambda version
- Aliases point to specific versions for each environment
- Blue/green deployments use weighted routing between versions

### Rollback Procedure
```bash
# List recent versions
aws lambda list-versions-by-function --function-name peppol-lookup-production

# Rollback to previous version
aws lambda update-alias \
  --function-name peppol-lookup-production \
  --name production \
  --function-version [previous-version]
```

## Performance Optimization

### Peppol-Commons Optimization
- Enable caching for certificate validation
- Use connection pooling for HTTP clients
- Pre-load frequently used Peppol identifiers
- Optimize XML parsing configuration

### Lambda Optimization
- Use appropriate memory allocation
- Enable SnapStart for faster cold starts
- Implement connection reuse across invocations
- Use provisioned concurrency for consistent performance

## Support and Maintenance

### Regular Maintenance Tasks
1. Update peppol-commons library versions
2. Review and update security configurations
3. Monitor performance metrics and optimize
4. Update Peppol root certificates as needed
5. Review and update CloudWatch alarms

### Monitoring Checklist
- [ ] Error rates within acceptable limits
- [ ] Response times meeting SLA requirements
- [ ] Certificate validation success rates
- [ ] Peppol compliance validation working
- [ ] Memory usage optimized
- [ ] Cold start times acceptable

For additional support, refer to the main project documentation or contact the development team.