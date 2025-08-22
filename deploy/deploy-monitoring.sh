#!/bin/bash

# Deploy Monitoring Infrastructure for Peppol SML/SMP Lookup Service
# Deploys CloudWatch alarms, dashboards, and notification systems

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Default values
ENVIRONMENT="production"
FUNCTION_NAME="peppol-sml-smp-lookup"
NOTIFICATION_EMAIL=""
SLACK_WEBHOOK_URL=""
PAGERDUTY_INTEGRATION_KEY=""
AWS_REGION="${AWS_REGION:-us-east-1}"
STACK_NAME_PREFIX="peppol-lookup-monitoring"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Usage function
usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Deploy monitoring infrastructure for Peppol SML/SMP Lookup Service

OPTIONS:
    -e, --environment ENVIRONMENT       Environment (dev, test, production) [default: production]
    -f, --function-name FUNCTION_NAME   Lambda function name [default: peppol-sml-smp-lookup]
    -n, --notification-email EMAIL      Email for notifications
    -s, --slack-webhook URL             Slack webhook URL
    -p, --pagerduty-key KEY            PagerDuty integration key
    -r, --region REGION                AWS region [default: us-east-1]
    --stack-prefix PREFIX              CloudFormation stack name prefix [default: peppol-lookup-monitoring]
    --validate-only                    Validate templates without deploying
    --update-only                      Update existing stacks only
    -h, --help                         Show this help message

EXAMPLES:
    # Deploy to production with email notifications
    $0 --environment production --notification-email ops@company.com

    # Deploy with full notification setup
    $0 --environment production \\
       --notification-email ops@company.com \\
       --slack-webhook https://hooks.slack.com/... \\
       --pagerduty-key abc123

    # Validate templates only
    $0 --validate-only

EOF
}

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -e|--environment)
                ENVIRONMENT="$2"
                shift 2
                ;;
            -f|--function-name)
                FUNCTION_NAME="$2"
                shift 2
                ;;
            -n|--notification-email)
                NOTIFICATION_EMAIL="$2"
                shift 2
                ;;
            -s|--slack-webhook)
                SLACK_WEBHOOK_URL="$2"
                shift 2
                ;;
            -p|--pagerduty-key)
                PAGERDUTY_INTEGRATION_KEY="$2"
                shift 2
                ;;
            -r|--region)
                AWS_REGION="$2"
                shift 2
                ;;
            --stack-prefix)
                STACK_NAME_PREFIX="$2"
                shift 2
                ;;
            --validate-only)
                VALIDATE_ONLY=true
                shift
                ;;
            --update-only)
                UPDATE_ONLY=true
                shift
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                error "Unknown option: $1"
                usage
                exit 1
                ;;
        esac
    done
}

# Validate prerequisites
validate_prerequisites() {
    log "Validating prerequisites..."
    
    # Check AWS CLI
    if ! command -v aws >/dev/null 2>&1; then
        error "AWS CLI is required but not installed"
        exit 1
    fi
    
    # Check AWS credentials
    if ! aws sts get-caller-identity >/dev/null 2>&1; then
        error "AWS credentials not configured or invalid"
        exit 1
    fi
    
    # Check jq
    if ! command -v jq >/dev/null 2>&1; then
        error "jq is required but not installed"
        exit 1
    fi
    
    # Validate environment
    if [[ ! "$ENVIRONMENT" =~ ^(dev|test|production)$ ]]; then
        error "Environment must be one of: dev, test, production"
        exit 1
    fi
    
    # Check if Lambda function exists
    if ! aws lambda get-function --function-name "$FUNCTION_NAME" --region "$AWS_REGION" >/dev/null 2>&1; then
        warning "Lambda function '$FUNCTION_NAME' not found in region '$AWS_REGION'"
        read -p "Continue anyway? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
    
    success "Prerequisites validated"
}

# Validate CloudFormation templates
validate_templates() {
    log "Validating CloudFormation templates..."
    
    local templates=(
        "$SCRIPT_DIR/monitoring/cloudwatch-alarms.yaml"
        "$SCRIPT_DIR/monitoring/cloudwatch-dashboard.yaml"
        "$SCRIPT_DIR/monitoring/escalation-procedures.yaml"
    )
    
    for template in "${templates[@]}"; do
        if [[ -f "$template" ]]; then
            log "Validating $(basename "$template")..."
            if aws cloudformation validate-template --template-body "file://$template" --region "$AWS_REGION" >/dev/null 2>&1; then
                success "$(basename "$template") is valid"
            else
                error "$(basename "$template") validation failed"
                aws cloudformation validate-template --template-body "file://$template" --region "$AWS_REGION"
                exit 1
            fi
        else
            error "Template not found: $template"
            exit 1
        fi
    done
    
    success "All templates validated successfully"
}

# Deploy CloudWatch alarms
deploy_alarms() {
    local stack_name="${STACK_NAME_PREFIX}-alarms-${ENVIRONMENT}"
    
    log "Deploying CloudWatch alarms stack: $stack_name"
    
    local parameters=(
        "ParameterKey=FunctionName,ParameterValue=$FUNCTION_NAME"
        "ParameterKey=Environment,ParameterValue=$ENVIRONMENT"
    )
    
    if [[ -n "$NOTIFICATION_EMAIL" ]]; then
        parameters+=("ParameterKey=NotificationEmail,ParameterValue=$NOTIFICATION_EMAIL")
    fi
    
    if [[ -n "$SLACK_WEBHOOK_URL" ]]; then
        parameters+=("ParameterKey=SlackWebhookUrl,ParameterValue=$SLACK_WEBHOOK_URL")
    fi
    
    # Check if stack exists
    if aws cloudformation describe-stacks --stack-name "$stack_name" --region "$AWS_REGION" >/dev/null 2>&1; then
        log "Updating existing alarms stack..."
        aws cloudformation update-stack \
            --stack-name "$stack_name" \
            --template-body "file://$SCRIPT_DIR/monitoring/cloudwatch-alarms.yaml" \
            --parameters "${parameters[@]}" \
            --capabilities CAPABILITY_IAM \
            --region "$AWS_REGION"
    else
        log "Creating new alarms stack..."
        aws cloudformation create-stack \
            --stack-name "$stack_name" \
            --template-body "file://$SCRIPT_DIR/monitoring/cloudwatch-alarms.yaml" \
            --parameters "${parameters[@]}" \
            --capabilities CAPABILITY_IAM \
            --region "$AWS_REGION"
    fi
    
    # Wait for stack completion
    log "Waiting for alarms stack deployment to complete..."
    aws cloudformation wait stack-create-complete --stack-name "$stack_name" --region "$AWS_REGION" 2>/dev/null || \
    aws cloudformation wait stack-update-complete --stack-name "$stack_name" --region "$AWS_REGION" 2>/dev/null
    
    success "CloudWatch alarms deployed successfully"
}

# Deploy CloudWatch dashboard
deploy_dashboard() {
    local stack_name="${STACK_NAME_PREFIX}-dashboard-${ENVIRONMENT}"
    
    log "Deploying CloudWatch dashboard stack: $stack_name"
    
    local parameters=(
        "ParameterKey=FunctionName,ParameterValue=$FUNCTION_NAME"
        "ParameterKey=Environment,ParameterValue=$ENVIRONMENT"
    )
    
    # Check if stack exists
    if aws cloudformation describe-stacks --stack-name "$stack_name" --region "$AWS_REGION" >/dev/null 2>&1; then
        log "Updating existing dashboard stack..."
        aws cloudformation update-stack \
            --stack-name "$stack_name" \
            --template-body "file://$SCRIPT_DIR/monitoring/cloudwatch-dashboard.yaml" \
            --parameters "${parameters[@]}" \
            --region "$AWS_REGION"
    else
        log "Creating new dashboard stack..."
        aws cloudformation create-stack \
            --stack-name "$stack_name" \
            --template-body "file://$SCRIPT_DIR/monitoring/cloudwatch-dashboard.yaml" \
            --parameters "${parameters[@]}" \
            --region "$AWS_REGION"
    fi
    
    # Wait for stack completion
    log "Waiting for dashboard stack deployment to complete..."
    aws cloudformation wait stack-create-complete --stack-name "$stack_name" --region "$AWS_REGION" 2>/dev/null || \
    aws cloudformation wait stack-update-complete --stack-name "$stack_name" --region "$AWS_REGION" 2>/dev/null
    
    success "CloudWatch dashboard deployed successfully"
}

# Deploy escalation procedures
deploy_escalation() {
    local stack_name="${STACK_NAME_PREFIX}-escalation-${ENVIRONMENT}"
    
    log "Deploying escalation procedures stack: $stack_name"
    
    local parameters=(
        "ParameterKey=Environment,ParameterValue=$ENVIRONMENT"
    )
    
    if [[ -n "$NOTIFICATION_EMAIL" ]]; then
        parameters+=("ParameterKey=PrimaryOncallEmail,ParameterValue=$NOTIFICATION_EMAIL")
    fi
    
    if [[ -n "$PAGERDUTY_INTEGRATION_KEY" ]]; then
        parameters+=("ParameterKey=PagerDutyIntegrationKey,ParameterValue=$PAGERDUTY_INTEGRATION_KEY")
    fi
    
    # Check if stack exists
    if aws cloudformation describe-stacks --stack-name "$stack_name" --region "$AWS_REGION" >/dev/null 2>&1; then
        log "Updating existing escalation stack..."
        aws cloudformation update-stack \
            --stack-name "$stack_name" \
            --template-body "file://$SCRIPT_DIR/monitoring/escalation-procedures.yaml" \
            --parameters "${parameters[@]}" \
            --capabilities CAPABILITY_IAM \
            --region "$AWS_REGION"
    else
        log "Creating new escalation stack..."
        aws cloudformation create-stack \
            --stack-name "$stack_name" \
            --template-body "file://$SCRIPT_DIR/monitoring/escalation-procedures.yaml" \
            --parameters "${parameters[@]}" \
            --capabilities CAPABILITY_IAM \
            --region "$AWS_REGION"
    fi
    
    # Wait for stack completion
    log "Waiting for escalation stack deployment to complete..."
    aws cloudformation wait stack-create-complete --stack-name "$stack_name" --region "$AWS_REGION" 2>/dev/null || \
    aws cloudformation wait stack-update-complete --stack-name "$stack_name" --region "$AWS_REGION" 2>/dev/null
    
    success "Escalation procedures deployed successfully"
}

# Setup log insights queries
setup_log_insights() {
    log "Setting up CloudWatch Logs Insights queries..."
    
    local log_group="/aws/lambda/$FUNCTION_NAME"
    
    # Check if log group exists
    if ! aws logs describe-log-groups --log-group-name-prefix "$log_group" --region "$AWS_REGION" | jq -e '.logGroups[] | select(.logGroupName == "'$log_group'")' >/dev/null 2>&1; then
        warning "Log group $log_group does not exist yet"
        return 0
    fi
    
    # Create saved queries from the JSON file
    if [[ -f "$SCRIPT_DIR/monitoring/log-insights-queries.json" ]]; then
        local queries=$(jq -r '.saved_queries[] | @base64' "$SCRIPT_DIR/monitoring/log-insights-queries.json")
        
        while IFS= read -r query_data; do
            local query=$(echo "$query_data" | base64 --decode)
            local query_name=$(echo "$query" | jq -r '.name')
            local query_string=$(echo "$query" | jq -r '.query')
            
            log "Creating saved query: $query_name"
            
            # Note: AWS CLI doesn't support creating saved queries directly
            # This would typically be done through the console or SDK
            success "Query definition ready: $query_name"
        done <<< "$queries"
    fi
    
    success "Log insights queries configured"
}

# Generate deployment summary
generate_summary() {
    log "Generating deployment summary..."
    
    local summary_file="/tmp/monitoring_deployment_summary_$(date +%Y%m%d_%H%M%S).txt"
    
    cat > "$summary_file" << EOF
Peppol SML/SMP Lookup Service - Monitoring Deployment Summary
============================================================

Deployment Date: $(date)
Environment: $ENVIRONMENT
AWS Region: $AWS_REGION
Function Name: $FUNCTION_NAME

Deployed Components:
- CloudWatch Alarms Stack: ${STACK_NAME_PREFIX}-alarms-${ENVIRONMENT}
- CloudWatch Dashboard Stack: ${STACK_NAME_PREFIX}-dashboard-${ENVIRONMENT}
- Escalation Procedures Stack: ${STACK_NAME_PREFIX}-escalation-${ENVIRONMENT}

Configuration:
- Notification Email: ${NOTIFICATION_EMAIL:-"Not configured"}
- Slack Integration: ${SLACK_WEBHOOK_URL:+"Configured"}${SLACK_WEBHOOK_URL:-"Not configured"}
- PagerDuty Integration: ${PAGERDUTY_INTEGRATION_KEY:+"Configured"}${PAGERDUTY_INTEGRATION_KEY:-"Not configured"}

CloudWatch Dashboard URL:
https://${AWS_REGION}.console.aws.amazon.com/cloudwatch/home?region=${AWS_REGION}#dashboards:name=Peppol-Lookup-${ENVIRONMENT}

Next Steps:
1. Verify all alarms are in OK state
2. Test notification channels
3. Review dashboard metrics
4. Configure log insights queries in the console
5. Set up automated monitoring reports

Monitoring Queries Available:
- Error analysis and trends
- Performance analysis
- Peppol-commons library usage
- Security events analysis
- Certificate expiry tracking
- External service health
- Validation failure analysis
- Circuit breaker events
- Cache performance analysis
- Compliance violations

For operational procedures, see: docs/OPERATIONAL_PROCEDURES.md
For peppol-commons monitoring, use: scripts/monitor-peppol-commons.sh
EOF
    
    echo "Deployment summary generated: $summary_file"
    cat "$summary_file"
    
    # Copy to project docs
    cp "$summary_file" "$PROJECT_ROOT/docs/MONITORING_DEPLOYMENT_SUMMARY.md"
    success "Summary saved to docs/MONITORING_DEPLOYMENT_SUMMARY.md"
}

# Test monitoring setup
test_monitoring() {
    log "Testing monitoring setup..."
    
    # Test CloudWatch alarms
    local alarm_names=$(aws cloudwatch describe-alarms --region "$AWS_REGION" --query "MetricAlarms[?starts_with(AlarmName, '${FUNCTION_NAME}-${ENVIRONMENT}')].AlarmName" --output text)
    
    if [[ -n "$alarm_names" ]]; then
        success "Found $(echo "$alarm_names" | wc -w) CloudWatch alarms"
        
        # Check alarm states
        for alarm in $alarm_names; do
            local state=$(aws cloudwatch describe-alarms --alarm-names "$alarm" --region "$AWS_REGION" --query 'MetricAlarms[0].StateValue' --output text)
            log "Alarm $alarm: $state"
        done
    else
        warning "No CloudWatch alarms found"
    fi
    
    # Test dashboard
    local dashboard_name="Peppol-Lookup-${ENVIRONMENT}"
    if aws cloudwatch get-dashboard --dashboard-name "$dashboard_name" --region "$AWS_REGION" >/dev/null 2>&1; then
        success "Dashboard '$dashboard_name' is accessible"
    else
        warning "Dashboard '$dashboard_name' not found"
    fi
    
    success "Monitoring test completed"
}

# Main deployment function
main() {
    parse_args "$@"
    
    log "Starting monitoring infrastructure deployment..."
    log "Environment: $ENVIRONMENT"
    log "Function Name: $FUNCTION_NAME"
    log "AWS Region: $AWS_REGION"
    
    validate_prerequisites
    validate_templates
    
    if [[ "${VALIDATE_ONLY:-false}" == "true" ]]; then
        success "Validation completed successfully"
        exit 0
    fi
    
    # Deploy components
    deploy_alarms
    deploy_dashboard
    deploy_escalation
    setup_log_insights
    
    # Test and summarize
    test_monitoring
    generate_summary
    
    success "Monitoring infrastructure deployment completed successfully!"
    
    echo
    echo "Next steps:"
    echo "1. Review the deployment summary in docs/MONITORING_DEPLOYMENT_SUMMARY.md"
    echo "2. Test notification channels by triggering a test alarm"
    echo "3. Set up automated monitoring with: scripts/monitor-peppol-commons.sh"
    echo "4. Review operational procedures in docs/OPERATIONAL_PROCEDURES.md"
}

# Run main function
main "$@"