# Operational Procedures for Peppol SML/SMP Lookup Service

## Table of Contents
1. [Maintenance Procedures](#maintenance-procedures)
2. [Disaster Recovery Plans](#disaster-recovery-plans)
3. [Monitoring and Alerting](#monitoring-and-alerting)
4. [Peppol-Commons Library Management](#peppol-commons-library-management)
5. [Security Incident Response](#security-incident-response)
6. [Performance Optimization](#performance-optimization)
7. [Troubleshooting Guide](#troubleshooting-guide)

## Maintenance Procedures

### Regular Maintenance Tasks

#### Daily Tasks
- [ ] Review CloudWatch dashboard for anomalies
- [ ] Check certificate expiry alerts
- [ ] Verify external service availability metrics
- [ ] Review error logs for new patterns
- [ ] Monitor peppol-commons library performance metrics

#### Weekly Tasks
- [ ] Analyze performance trends and optimization opportunities
- [ ] Review security event logs
- [ ] Update operational documentation if needed
- [ ] Check for peppol-commons library updates
- [ ] Validate backup and recovery procedures

#### Monthly Tasks
- [ ] Comprehensive security audit
- [ ] Performance baseline review and adjustment
- [ ] Disaster recovery plan testing
- [ ] Certificate inventory and renewal planning
- [ ] Peppol-commons library version compatibility review

### Deployment Procedures

#### Pre-Deployment Checklist
```bash
# 1. Validate configuration
./deploy/validate-deployment-config.sh

# 2. Run comprehensive tests
mvn clean test -Dtest=ComprehensiveIntegrationTestSuite

# 3. Check peppol-commons compatibility
mvn dependency:analyze -DignoreNonCompile=true

# 4. Validate Lambda package size
./deploy/optimize-lambda.sh --validate-only

# 5. Review security configurations
./deploy/security-audit.sh
```

#### Deployment Steps
```bash
# 1. Deploy to test environment first
./deploy/deploy.sh --environment test --validate

# 2. Run integration tests against test environment
./deploy/test-deployment.sh --environment test

# 3. Deploy to production using blue-green strategy
./deploy/blue-green-deploy.sh --environment production

# 4. Monitor deployment metrics
./deploy/monitor-deployment.sh --environment production --duration 30m
```

#### Post-Deployment Verification
- [ ] Verify all CloudWatch alarms are in OK state
- [ ] Test critical user journeys
- [ ] Validate peppol-commons integration
- [ ] Check performance metrics baseline
- [ ] Confirm security configurations

### Configuration Management

#### Environment Variables
```bash
# Production Configuration
export SML_DOMAIN="edelivery.tech.ec.europa.eu"
export LOG_LEVEL="INFO"
export CACHE_TTL="3600"
export CIRCUIT_BREAKER_ENABLED="true"
export PEPPOL_COMMONS_CACHE_SIZE="1000"
export MEMORY_ALLOCATION="512"

# Test Configuration  
export SML_DOMAIN="acc.edelivery.tech.ec.europa.eu"
export LOG_LEVEL="DEBUG"
export CACHE_TTL="1800"
export CIRCUIT_BREAKER_ENABLED="true"
export PEPPOL_COMMONS_CACHE_SIZE="500"
export MEMORY_ALLOCATION="256"
```

#### Certificate Management
```bash
# Check certificate expiry
aws lambda invoke --function-name peppol-lookup-cert-check \
  --payload '{"action": "check_expiry", "days": 30}' response.json

# Update OpenPeppol root certificates
./scripts/update-peppol-certificates.sh --environment production

# Validate certificate chain
./scripts/validate-certificate-chain.sh --certificate-path /path/to/cert
```

## Disaster Recovery Plans

### Recovery Time Objectives (RTO) and Recovery Point Objectives (RPO)
- **RTO**: 15 minutes for critical functionality restoration
- **RPO**: 5 minutes for configuration and monitoring data
- **Service Level**: 99.9% availability target

### Disaster Scenarios and Response

#### Scenario 1: Lambda Function Failure
**Symptoms**: High error rates, function timeouts, memory issues

**Response Steps**:
1. **Immediate (0-5 minutes)**:
   ```bash
   # Check function status
   aws lambda get-function --function-name peppol-sml-smp-lookup
   
   # Review recent logs
   aws logs filter-log-events --log-group-name /aws/lambda/peppol-sml-smp-lookup \
     --start-time $(date -d '15 minutes ago' +%s)000
   ```

2. **Short-term (5-15 minutes)**:
   ```bash
   # Rollback to previous version if needed
   aws lambda update-function-code --function-name peppol-sml-smp-lookup \
     --s3-bucket deployment-bucket --s3-key previous-version.zip
   
   # Increase memory allocation if memory issues
   aws lambda update-function-configuration --function-name peppol-sml-smp-lookup \
     --memory-size 1024
   ```

3. **Long-term (15+ minutes)**:
   - Deploy fixed version using blue-green deployment
   - Conduct root cause analysis
   - Update monitoring and alerting if needed

#### Scenario 2: External Service Outage (SML/SMP)
**Symptoms**: Circuit breaker open, external service unavailable alerts

**Response Steps**:
1. **Verify outage scope**:
   ```bash
   # Test SML connectivity
   dig B-$(echo -n "0088:123456789" | md5sum | cut -d' ' -f1).iso6523-actorid-upis.edelivery.tech.ec.europa.eu
   
   # Test SMP connectivity
   curl -I https://smp.example.com/iso6523-actorid-upis%3A%3A0088%3A123456789
   ```

2. **Implement fallback procedures**:
   - Enable cached response mode if available
   - Communicate service degradation to stakeholders
   - Monitor for service restoration

3. **Recovery actions**:
   - Reset circuit breakers when services recover
   - Validate full functionality restoration
   - Update incident documentation

#### Scenario 3: Security Incident
**Symptoms**: Security event alerts, unusual traffic patterns, certificate issues

**Response Steps**:
1. **Immediate containment**:
   ```bash
   # Enable enhanced logging
   aws lambda update-function-configuration --function-name peppol-sml-smp-lookup \
     --environment Variables='{LOG_LEVEL=DEBUG,SECURITY_AUDIT_MODE=true}'
   
   # Review security logs
   aws logs filter-log-events --log-group-name /aws/lambda/peppol-sml-smp-lookup \
     --filter-pattern "{ $.level = \"ERROR\" && $.securityEvent = * }"
   ```

2. **Investigation and response**:
   - Analyze attack vectors and impact
   - Implement additional security controls if needed
   - Coordinate with security team
   - Document incident for future prevention

### Backup and Recovery Procedures

#### Configuration Backup
```bash
# Backup Lambda configuration
aws lambda get-function --function-name peppol-sml-smp-lookup > backup/lambda-config-$(date +%Y%m%d).json

# Backup CloudWatch alarms
aws cloudwatch describe-alarms --alarm-names $(aws cloudwatch describe-alarms --query 'MetricAlarms[?starts_with(AlarmName, `peppol-lookup`)].AlarmName' --output text) > backup/alarms-$(date +%Y%m%d).json

# Backup environment configurations
cp deploy/environments/*.env backup/environments-$(date +%Y%m%d)/
```

#### Recovery Procedures
```bash
# Restore Lambda function from backup
aws lambda update-function-code --function-name peppol-sml-smp-lookup \
  --s3-bucket backup-bucket --s3-key lambda-backup-$(date +%Y%m%d).zip

# Restore CloudWatch alarms
aws cloudwatch put-metric-alarm --cli-input-json file://backup/alarms-$(date +%Y%m%d).json

# Validate recovery
./deploy/test-deployment.sh --environment production --validate-recovery
```

## Monitoring and Alerting

### Key Performance Indicators (KPIs)
- **Availability**: > 99.9%
- **Error Rate**: < 1%
- **P99 Latency**: < 30 seconds
- **Certificate Validation Success**: > 99%
- **External Service Availability**: > 95%

### Alert Escalation Matrix

| Alert Level | Response Time | Escalation Path |
|-------------|---------------|-----------------|
| Critical | 5 minutes | Primary On-Call → Secondary On-Call → Manager |
| High | 15 minutes | Primary On-Call → Manager |
| Medium | 1 hour | Team Lead → Manager |
| Low | 4 hours | Team Review |

### Monitoring Queries
```bash
# Check error rates
aws logs start-query --log-group-name /aws/lambda/peppol-sml-smp-lookup \
  --start-time $(date -d '1 hour ago' +%s) --end-time $(date +%s) \
  --query-string 'fields @timestamp, level, errorCode | filter level = "ERROR" | stats count() by errorCode'

# Monitor performance
aws logs start-query --log-group-name /aws/lambda/peppol-sml-smp-lookup \
  --start-time $(date -d '1 hour ago' +%s) --end-time $(date +%s) \
  --query-string 'fields @timestamp, totalProcessingTime | stats avg(totalProcessingTime), max(totalProcessingTime), count()'
```

## Peppol-Commons Library Management

### Version Management
```bash
# Check current peppol-commons version
mvn dependency:tree | grep peppol-commons

# Check for updates
mvn versions:display-dependency-updates | grep peppol

# Update to specific version
mvn versions:use-dep-version -Dincludes=com.helger:peppol-commons -DdepVersion=9.0.0
```

### Compatibility Testing
```bash
# Run peppol-commons integration tests
mvn test -Dtest=PeppolCommonsIntegrationTest

# Validate identifier handling
mvn test -Dtest=PeppolCommonsBasicIntegrationTest

# Performance benchmark
mvn test -Dtest=PeppolCommonsPerformanceBenchmarkTest
```

### Security Advisory Monitoring
- Subscribe to Helger peppol-commons GitHub releases
- Monitor CVE databases for peppol-commons vulnerabilities
- Set up automated dependency vulnerability scanning

## Security Incident Response

### Incident Classification
- **P0**: Active security breach, data compromise
- **P1**: Potential security vulnerability, suspicious activity
- **P2**: Security configuration issue, compliance violation
- **P3**: Security enhancement opportunity

### Response Procedures
1. **Detection and Analysis** (0-15 minutes)
2. **Containment and Eradication** (15-60 minutes)
3. **Recovery and Post-Incident** (1+ hours)

### Security Monitoring
```bash
# Monitor for security events
aws logs filter-log-events --log-group-name /aws/lambda/peppol-sml-smp-lookup \
  --filter-pattern "{ $.securityEvent = * }"

# Check for malicious inputs
aws logs filter-log-events --log-group-name /aws/lambda/peppol-sml-smp-lookup \
  --filter-pattern "{ $.maliciousInput = true }"
```

## Performance Optimization

### Memory Optimization
```bash
# Monitor memory usage
aws logs filter-log-events --log-group-name /aws/lambda/peppol-sml-smp-lookup \
  --filter-pattern "{ $.memoryUtilization > 80 }"

# Optimize Lambda memory allocation
./deploy/optimize-lambda.sh --analyze-memory --environment production
```

### Cache Optimization
```bash
# Monitor cache performance
aws logs filter-log-events --log-group-name /aws/lambda/peppol-sml-smp-lookup \
  --filter-pattern "{ $.cacheHitRate < 0.8 }"

# Adjust cache settings
aws lambda update-function-configuration --function-name peppol-sml-smp-lookup \
  --environment Variables='{CACHE_TTL=7200,CACHE_SIZE=2000}'
```

## Troubleshooting Guide

### Common Issues and Solutions

#### High Error Rates
1. Check external service availability
2. Verify certificate validity
3. Review input validation failures
4. Check peppol-commons integration

#### Performance Issues
1. Monitor memory utilization
2. Check external service response times
3. Analyze cache hit rates
4. Review peppol-commons performance metrics

#### Certificate Validation Failures
1. Verify OpenPeppol root certificates
2. Check certificate expiry dates
3. Validate certificate chain
4. Test OCSP/CRL connectivity

### Diagnostic Commands
```bash
# Function health check
aws lambda invoke --function-name peppol-sml-smp-lookup \
  --payload '{"participantId":"0088:123456789","documentTypeId":"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1","processId":"urn:fdc:peppol.eu:2017:poacc:billing:01:1.0","environment":"test"}' \
  response.json

# Check logs for specific correlation ID
aws logs filter-log-events --log-group-name /aws/lambda/peppol-sml-smp-lookup \
  --filter-pattern "{ $.correlationId = \"specific-correlation-id\" }"

# Performance analysis
aws logs start-query --log-group-name /aws/lambda/peppol-sml-smp-lookup \
  --start-time $(date -d '1 hour ago' +%s) --end-time $(date +%s) \
  --query-string 'fields @timestamp, @duration, @memorySize, @maxMemoryUsed'
```

## Contact Information

### Escalation Contacts
- **Primary On-Call**: oncall-primary@yourcompany.com
- **Secondary On-Call**: oncall-secondary@yourcompany.com
- **Engineering Manager**: engineering-manager@yourcompany.com
- **Security Team**: security-team@yourcompany.com

### External Contacts
- **Peppol Support**: support@peppol.eu
- **OpenPeppol Technical**: technical@openpeppol.org
- **AWS Support**: [AWS Support Case]

---

*Last Updated: $(date)*
*Version: 1.0*
*Next Review: $(date -d '+3 months')*