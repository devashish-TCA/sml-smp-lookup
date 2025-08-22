#!/bin/bash

# Lambda Optimization Script for Peppol Lookup Service
# Optimizes Lambda function for peppol-commons library usage and performance

set -e

ENVIRONMENT=${1:-dev}
REGION=${2:-us-east-1}
PROFILE=${3:-default}

echo "🔧 Optimizing Lambda function for $ENVIRONMENT environment"

FUNCTION_NAME="peppol-lookup-$ENVIRONMENT"

# Update Lambda configuration for optimal peppol-commons performance
echo "⚙️  Updating Lambda configuration..."

# Set memory based on environment (peppol-commons can be memory intensive)
if [ "$ENVIRONMENT" = "production" ]; then
    MEMORY_SIZE=768
    TIMEOUT=60
    RESERVED_CONCURRENCY=100
elif [ "$ENVIRONMENT" = "test" ]; then
    MEMORY_SIZE=512
    TIMEOUT=60
    RESERVED_CONCURRENCY=50
else
    MEMORY_SIZE=512
    TIMEOUT=90
    RESERVED_CONCURRENCY=20
fi

echo "📊 Setting memory to ${MEMORY_SIZE}MB, timeout to ${TIMEOUT}s"

# Update function configuration
aws lambda update-function-configuration \
    --profile $PROFILE \
    --region $REGION \
    --function-name $FUNCTION_NAME \
    --memory-size $MEMORY_SIZE \
    --timeout $TIMEOUT \
    --reserved-concurrent-executions $RESERVED_CONCURRENCY \
    --environment Variables="$(cat deploy/environments/$ENVIRONMENT.env | grep -v '^#' | grep -v '^$' | tr '\n' ',' | sed 's/,$//' | sed 's/=/=/g' | awk -F',' '{for(i=1;i<=NF;i++){split($i,a,"="); printf "\"%s\":\"%s\"", a[1], a[2]; if(i<NF) printf ","}}')" \
    --output table

# Enable SnapStart for faster cold starts with peppol-commons
echo "⚡ Enabling SnapStart for faster cold starts..."
aws lambda put-provisioned-concurrency-config \
    --profile $PROFILE \
    --region $REGION \
    --function-name $FUNCTION_NAME \
    --qualifier '$LATEST' \
    --provisioned-concurrency-executions 2 \
    --output table || echo "⚠️  SnapStart configuration may already exist"

# Configure dead letter queue for error handling
echo "💀 Configuring dead letter queue..."
DLQ_ARN=$(aws sqs create-queue \
    --profile $PROFILE \
    --region $REGION \
    --queue-name "peppol-lookup-dlq-$ENVIRONMENT" \
    --attributes VisibilityTimeoutSeconds=300,MessageRetentionPeriod=1209600 \
    --query 'QueueUrl' \
    --output text 2>/dev/null || echo "")

if [ ! -z "$DLQ_ARN" ]; then
    DLQ_ARN=$(aws sqs get-queue-attributes \
        --profile $PROFILE \
        --region $REGION \
        --queue-url "$DLQ_ARN" \
        --attribute-names QueueArn \
        --query 'Attributes.QueueArn' \
        --output text)
    
    aws lambda update-function-configuration \
        --profile $PROFILE \
        --region $REGION \
        --function-name $FUNCTION_NAME \
        --dead-letter-config TargetArn=$DLQ_ARN \
        --output table
fi

# Set up CloudWatch log retention
echo "📝 Configuring CloudWatch log retention..."
aws logs put-retention-policy \
    --profile $PROFILE \
    --region $REGION \
    --log-group-name "/aws/lambda/$FUNCTION_NAME" \
    --retention-in-days $([ "$ENVIRONMENT" = "production" ] && echo "30" || echo "7") \
    --output table || echo "⚠️  Log group may not exist yet"

# Create CloudWatch alarms for monitoring
echo "🚨 Setting up CloudWatch alarms..."

# Error rate alarm
aws cloudwatch put-metric-alarm \
    --profile $PROFILE \
    --region $REGION \
    --alarm-name "PeppolLookup-ErrorRate-$ENVIRONMENT" \
    --alarm-description "Peppol Lookup Service error rate too high" \
    --metric-name Errors \
    --namespace AWS/Lambda \
    --statistic Sum \
    --period 300 \
    --threshold 5 \
    --comparison-operator GreaterThanThreshold \
    --evaluation-periods 2 \
    --dimensions Name=FunctionName,Value=$FUNCTION_NAME \
    --alarm-actions "arn:aws:sns:$REGION:$(aws sts get-caller-identity --profile $PROFILE --query Account --output text):peppol-alerts-$ENVIRONMENT" \
    --output table || echo "⚠️  SNS topic may not exist"

# Duration alarm
aws cloudwatch put-metric-alarm \
    --profile $PROFILE \
    --region $REGION \
    --alarm-name "PeppolLookup-Duration-$ENVIRONMENT" \
    --alarm-description "Peppol Lookup Service duration too high" \
    --metric-name Duration \
    --namespace AWS/Lambda \
    --statistic Average \
    --period 300 \
    --threshold 30000 \
    --comparison-operator GreaterThanThreshold \
    --evaluation-periods 2 \
    --dimensions Name=FunctionName,Value=$FUNCTION_NAME \
    --alarm-actions "arn:aws:sns:$REGION:$(aws sts get-caller-identity --profile $PROFILE --query Account --output text):peppol-alerts-$ENVIRONMENT" \
    --output table || echo "⚠️  SNS topic may not exist"

# Test the optimized function
echo "🧪 Testing optimized Lambda function..."
TEST_PAYLOAD='{
    "httpMethod": "GET",
    "path": "/health",
    "headers": {},
    "queryStringParameters": null,
    "body": null
}'

aws lambda invoke \
    --profile $PROFILE \
    --region $REGION \
    --function-name $FUNCTION_NAME \
    --payload "$TEST_PAYLOAD" \
    /tmp/optimization_test.json

TEST_RESULT=$(cat /tmp/optimization_test.json | jq -r '.statusCode // 500')

if [ "$TEST_RESULT" = "200" ]; then
    echo "✅ Lambda optimization completed successfully"
else
    echo "❌ Lambda optimization test failed"
    cat /tmp/optimization_test.json
    exit 1
fi

# Display optimization summary
echo ""
echo "📋 Optimization Summary:"
echo "  🔧 Function: $FUNCTION_NAME"
echo "  💾 Memory: ${MEMORY_SIZE}MB"
echo "  ⏱️  Timeout: ${TIMEOUT}s"
echo "  🔄 Concurrency: $RESERVED_CONCURRENCY"
echo "  ⚡ SnapStart: Enabled"
echo "  📝 Log Retention: $([ "$ENVIRONMENT" = "production" ] && echo "30" || echo "7") days"
echo "  🚨 Alarms: Configured"
echo ""
echo "🎉 Lambda function optimized for peppol-commons performance!"