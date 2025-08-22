#!/bin/bash

# Lambda Package Update Script for Peppol Lookup Service
# Updates existing Lambda function with new optimized package

set -e

FUNCTION_NAME=${1}
REGION=${2:-us-east-1}
PROFILE=${3:-default}

if [ -z "$FUNCTION_NAME" ]; then
    echo "❌ Usage: $0 <function-name> [region] [aws-profile]"
    echo "   Example: $0 peppol-lookup-production us-east-1 default"
    exit 1
fi

echo "🔄 Updating Lambda function: $FUNCTION_NAME"

# Build optimized package
echo "📦 Building optimized Lambda package..."
cd "$(dirname "$0")/.."

# Build the package
./deploy/deploy.sh

# Check if function exists
echo "🔍 Checking if Lambda function exists..."
if ! aws lambda get-function \
    --profile $PROFILE \
    --region $REGION \
    --function-name $FUNCTION_NAME \
    --output table > /dev/null 2>&1; then
    echo "❌ Lambda function '$FUNCTION_NAME' not found in region $REGION"
    echo "💡 Please create the Lambda function first using your CDK infrastructure"
    exit 1
fi

# Update Lambda function code
echo "🚀 Updating Lambda function code..."
aws lambda update-function-code \
    --profile $PROFILE \
    --region $REGION \
    --function-name $FUNCTION_NAME \
    --zip-file fileb://target/peppol-lookup-lambda.jar \
    --publish > /tmp/update_result.json

NEW_VERSION=$(cat /tmp/update_result.json | jq -r '.Version')
echo "📦 Updated to version: $NEW_VERSION"

# Wait for function to be ready
echo "⏳ Waiting for function update to complete..."
aws lambda wait function-updated \
    --profile $PROFILE \
    --region $REGION \
    --function-name $FUNCTION_NAME

# Test the updated function
echo "🧪 Testing updated function..."
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
    /tmp/test_response.json

TEST_STATUS=$(cat /tmp/test_response.json | jq -r '.statusCode // 500')

if [ "$TEST_STATUS" = "200" ]; then
    echo "✅ Function update successful and tested"
else
    echo "❌ Function test failed"
    echo "📄 Response:"
    cat /tmp/test_response.json
    exit 1
fi

echo "🎉 Lambda function updated successfully!"
echo ""
echo "📋 Update Summary:"
echo "  🔧 Function: $FUNCTION_NAME"
echo "  🆕 Version: $NEW_VERSION"
echo "  📏 Package: target/peppol-lookup-lambda.jar"
echo "  ✅ Status: Ready"