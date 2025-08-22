# Monitoring Infrastructure for Peppol SML/SMP Lookup Service

This directory contains comprehensive monitoring, alerting, and operational infrastructure for the Peppol SML/SMP Lookup Service.

## Overview

The monitoring infrastructure provides:
- **CloudWatch Alarms**: Automated alerting for critical metrics
- **CloudWatch Dashboard**: Real-time operational visibility
- **Escalation Procedures**: Multi-level incident response
- **Log Analysis**: Structured query templates for troubleshooting
- **Peppol-Commons Monitoring**: Specialized monitoring for the peppol-commons library
- **Operational Procedures**: Comprehensive maintenance and disaster recovery plans

## Quick Start

### 1. Deploy Monitoring Infrastructure

```bash
# Deploy with basic email notifications
./deploy/deploy-monitoring.sh \
  --environment production \
  --notification-email ops@yourcompany.com

# Deploy with full notification setup
./deploy/deploy-monitoring.sh \
  --environment production \
  --notification-email ops@yourcompany.com \
  --slack-webhook https://hooks.slack.com/services/... \
  --pagerduty-key your-pagerduty-integration-key
```

### 2. Validate Deployment

```bash
# Validate templates only (no deployment)
./deploy/deploy-monitoring.sh --validate-only

# Test monitoring setup after deployment
aws cloudwatch describe-alarms --region us-east-1 \
  --query "MetricAlarms[?starts_with(AlarmName, 'peppol-lookup')].{Name:AlarmName,State:StateValue}"
```

### 3. Set Up Automated Monitoring

```bash
# Monitor peppol-commons library for updates and security advisories
./scripts/monitor-peppol-commons.sh monitor

# Set up cron job for automated monitoring
echo "0 9 * * 1 /path/to/scripts/monitor-peppol-commons.sh monitor" | crontab -
```

## Components

### CloudWatch Alarms (`cloudwatch-alarms.yaml`)

Comprehensive alerting covering:

#### Critical Alarms (Immediate Response)
- **Error Rate**: > 5% over 5 minutes
- **P99 Latency**: > 30 seconds over 10 minutes
- **Security Events**: Any security event detected
- **External Service Unavailable**: < 95% availability over 15 minutes

#### Warning Alarms (Standard Response)
- **Certificate Expiry**: Certificates expiring within 30 days
- **Memory Utilization**: > 85% over 10 minutes
- **Circuit Breaker Open**: Any circuit breaker in open state
- **Peppol-Commons Errors**: Any peppol-commons integration errors

### CloudWatch Dashboard (`cloudwatch-dashboard.yaml`)

Real-time operational dashboard with:
- Lambda function metrics (invocations, errors, duration)
- Business metrics (successful/failed lookups, validation results)
- Performance metrics (SML lookup time, SMP query time, validation time)
- External service health (availability, circuit breaker status)
- Resource utilization (memory, CPU, connection pools)
- Peppol-commons specific metrics
- Certificate health tracking
- Security event monitoring
- Error categorization
- Cache performance

### Escalation Procedures (`escalation-procedures.yaml`)

Multi-level incident response:
- **Primary On-Call**: Immediate notification for critical alerts
- **Secondary On-Call**: Escalation after 15 minutes
- **Manager**: Notification for all critical incidents
- **PagerDuty Integration**: Automated incident creation and escalation
- **Slack Integration**: Real-time team notifications

### Log Analysis (`log-insights-queries.json`)

Pre-built CloudWatch Logs Insights queries:
- Error analysis and trending
- Performance analysis with percentiles
- Peppol-commons library usage tracking
- Security event analysis
- Certificate expiry monitoring
- External service health tracking
- Validation failure analysis
- Circuit breaker event tracking
- Cache performance analysis
- Compliance violation tracking

## Configuration

### Environment Variables

The monitoring infrastructure uses these environment variables:

```bash
# Required
export AWS_REGION="us-east-1"
export FUNCTION_NAME="peppol-sml-smp-lookup"
export ENVIRONMENT="production"

# Optional - Notifications
export NOTIFICATION_EMAIL="ops@yourcompany.com"
export SLACK_WEBHOOK_URL="https://hooks.slack.com/services/..."
export PAGERDUTY_INTEGRATION_KEY="your-integration-key"
```

### Monitoring Configuration (`monitoring-config.yaml`)

Centralized configuration for:
- Performance thresholds
- Business metric thresholds
- Security thresholds
- Peppol-commons specific metrics
- Alert definitions
- Dashboard widget configuration
- Notification channels
- Escalation procedures
- Log analysis queries
- Maintenance schedules

## Usage

### Daily Operations

```bash
# Check system health
aws cloudwatch get-dashboard --dashboard-name "Peppol-Lookup-production"

# Review recent errors
aws logs start-query \
  --log-group-name "/aws/lambda/peppol-sml-smp-lookup" \
  --start-time $(date -d '1 hour ago' +%s) \
  --end-time $(date +%s) \
  --query-string 'fields @timestamp, level, errorCode, message | filter level = "ERROR" | sort @timestamp desc'

# Monitor peppol-commons library
./scripts/monitor-peppol-commons.sh monitor
```

### Performance Analysis

```bash
# Analyze performance trends
aws logs start-query \
  --log-group-name "/aws/lambda/peppol-sml-smp-lookup" \
  --start-time $(date -d '24 hours ago' +%s) \
  --end-time $(date +%s) \
  --query-string 'fields @timestamp, totalProcessingTime | stats avg(totalProcessingTime), p50(totalProcessingTime), p95(totalProcessingTime), p99(totalProcessingTime) by bin(1h)'

# Check external service health
aws logs start-query \
  --log-group-name "/aws/lambda/peppol-sml-smp-lookup" \
  --start-time $(date -d '1 hour ago' +%s) \
  --end-time $(date +%s) \
  --query-string 'fields @timestamp, externalService, responseTime, statusCode | filter ispresent(externalService) | stats avg(responseTime), count() by externalService, statusCode'
```

### Security Monitoring

```bash
# Check for security events
aws logs start-query \
  --log-group-name "/aws/lambda/peppol-sml-smp-lookup" \
  --start-time $(date -d '24 hours ago' +%s) \
  --end-time $(date +%s) \
  --query-string 'fields @timestamp, securityEvent, sourceIp | filter ispresent(securityEvent) | stats count() by securityEvent, sourceIp'

# Monitor certificate expiry
aws logs start-query \
  --log-group-name "/aws/lambda/peppol-sml-smp-lookup" \
  --start-time $(date -d '24 hours ago' +%s) \
  --end-time $(date +%s) \
  --query-string 'fields @timestamp, certificateSubject, daysUntilExpiry | filter ispresent(daysUntilExpiry) and daysUntilExpiry <= 30 | sort daysUntilExpiry asc'
```

## Troubleshooting

### Common Issues

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
aws lambda invoke \
  --function-name peppol-sml-smp-lookup \
  --payload '{"participantId":"0088:123456789","documentTypeId":"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1","processId":"urn:fdc:peppol.eu:2017:poacc:billing:01:1.0","environment":"test"}' \
  response.json

# Check logs for specific correlation ID
aws logs filter-log-events \
  --log-group-name "/aws/lambda/peppol-sml-smp-lookup" \
  --filter-pattern '{ $.correlationId = "specific-correlation-id" }'

# Performance analysis
aws logs start-query \
  --log-group-name "/aws/lambda/peppol-sml-smp-lookup" \
  --start-time $(date -d '1 hour ago' +%s) \
  --end-time $(date +%s) \
  --query-string 'fields @timestamp, @duration, @memorySize, @maxMemoryUsed'
```

## Maintenance

### Regular Tasks

#### Daily
- [ ] Review CloudWatch dashboard for anomalies
- [ ] Check certificate expiry alerts
- [ ] Verify external service availability metrics
- [ ] Review error logs for new patterns
- [ ] Monitor peppol-commons library performance metrics

#### Weekly
- [ ] Analyze performance trends and optimization opportunities
- [ ] Review security event logs
- [ ] Update operational documentation if needed
- [ ] Check for peppol-commons library updates
- [ ] Validate backup and recovery procedures

#### Monthly
- [ ] Comprehensive security audit
- [ ] Performance baseline review and adjustment
- [ ] Disaster recovery plan testing
- [ ] Certificate inventory and renewal planning
- [ ] Peppol-commons library version compatibility review

### Automated Maintenance

Set up cron jobs for automated monitoring:

```bash
# Peppol-commons monitoring (weekly)
0 9 * * 1 /path/to/scripts/monitor-peppol-commons.sh monitor

# Certificate expiry check (daily)
0 8 * * * /path/to/scripts/check-certificates.sh

# Performance report (weekly)
0 18 * * 5 /path/to/scripts/generate-performance-report.sh
```

## Security

### Security Event Monitoring

The monitoring infrastructure tracks:
- Malicious input attempts
- XML attack attempts (XXE, XML bombs)
- Certificate validation failures
- Unusual traffic patterns
- Authentication/authorization failures

### Incident Response

For security incidents:
1. **Immediate**: Check security event logs
2. **Short-term**: Enable enhanced logging and monitoring
3. **Long-term**: Implement additional security controls

### Compliance

The monitoring setup helps maintain:
- Peppol AP certification requirements
- Security compliance standards
- Performance SLA requirements
- Operational excellence standards

## Support

### Documentation
- [Operational Procedures](../../docs/OPERATIONAL_PROCEDURES.md)
- [Peppol-Commons Integration](../../docs/PEPPOL_COMMONS_INTEGRATION.md)
- [Deployment Guide](../README.md)

### Scripts
- `deploy-monitoring.sh`: Deploy monitoring infrastructure
- `monitor-peppol-commons.sh`: Monitor peppol-commons library
- `validate-deployment-config.sh`: Validate deployment configuration

### Contact Information
- **Primary On-Call**: oncall-primary@yourcompany.com
- **Secondary On-Call**: oncall-secondary@yourcompany.com
- **Engineering Manager**: engineering-manager@yourcompany.com
- **Security Team**: security-team@yourcompany.com

---

*Last Updated: $(date)*
*Version: 1.0*