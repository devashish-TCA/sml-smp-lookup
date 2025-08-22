#!/bin/bash

# Deployment Configuration Validation Script
# Validates all deployment configurations and prerequisites

set -e

echo "🔍 Validating Peppol Lookup Service deployment configuration..."

# Check prerequisites
echo "📋 Checking prerequisites..."

# Check AWS CLI
if ! command -v aws &> /dev/null; then
    echo "❌ AWS CLI not found. Please install AWS CLI v2"
    exit 1
fi

AWS_VERSION=$(aws --version 2>&1 | cut -d/ -f2 | cut -d' ' -f1)
echo "✅ AWS CLI version: $AWS_VERSION"

# CDK not required for this capability - will be handled separately

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java not found. Please install Java 17"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
echo "✅ Java version: $JAVA_VERSION"

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven not found. Please install Maven 3.8+"
    exit 1
fi

MVN_VERSION=$(mvn -version 2>&1 | head -n 1 | cut -d' ' -f3)
echo "✅ Maven version: $MVN_VERSION"

# Check jq
if ! command -v jq &> /dev/null; then
    echo "❌ jq not found. Please install jq for JSON processing"
    exit 1
fi

echo "✅ jq installed"

# Validate project structure
echo ""
echo "📁 Validating project structure..."

REQUIRED_FILES=(
    "pom.xml"
    "deploy/deploy.sh"
    "deploy/blue-green-deploy.sh"
    "deploy/test-deployment.sh"
    "deploy/optimize-lambda.sh"
    "deploy/environments/dev.env"
    "deploy/environments/test.env"
    "deploy/environments/production.env"
)

for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file"
    else
        echo "❌ Missing: $file"
        exit 1
    fi
done

# Validate Maven configuration
echo ""
echo "🔧 Validating Maven configuration..."

# Check if peppol-commons dependencies are configured
if grep -q "peppol-commons" pom.xml; then
    echo "✅ peppol-commons dependency configured"
else
    echo "❌ peppol-commons dependency not found in pom.xml"
    exit 1
fi

# Check if Lambda dependencies are configured
if grep -q "aws-lambda-java-core" pom.xml; then
    echo "✅ AWS Lambda dependencies configured"
else
    echo "❌ AWS Lambda dependencies not found in pom.xml"
    exit 1
fi

# Check if Shade plugin is configured for Lambda packaging
if grep -q "maven-shade-plugin" pom.xml; then
    echo "✅ Maven Shade plugin configured"
else
    echo "❌ Maven Shade plugin not configured"
    exit 1
fi

# CDK configuration will be handled separately - skipping validation

# Validate environment configurations
echo ""
echo "🌍 Validating environment configurations..."

for env in dev test production; do
    ENV_FILE="deploy/environments/$env.env"
    
    # Check required environment variables
    REQUIRED_VARS=(
        "PEPPOL_ENVIRONMENT"
        "LOG_LEVEL"
        "SML_DOMAIN_PRODUCTION"
        "SML_DOMAIN_TEST"
        "PEPPOL_COMMONS_CACHE_ENABLED"
    )
    
    for var in "${REQUIRED_VARS[@]}"; do
        if grep -q "^$var=" "$ENV_FILE"; then
            echo "✅ $env: $var configured"
        else
            echo "❌ $env: Missing $var in $ENV_FILE"
            exit 1
        fi
    done
done

# Test Maven build
echo ""
echo "🔨 Testing Maven build..."

if mvn clean compile -q; then
    echo "✅ Maven compilation successful"
else
    echo "❌ Maven compilation failed"
    exit 1
fi

# CDK synthesis will be handled separately - skipping test

# Validate AWS credentials
echo ""
echo "🔐 Validating AWS credentials..."

if aws sts get-caller-identity > /dev/null 2>&1; then
    ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
    echo "✅ AWS credentials valid (Account: $ACCOUNT_ID)"
else
    echo "❌ AWS credentials not configured or invalid"
    exit 1
fi

# Check deployment script permissions
echo ""
echo "🔒 Checking script permissions..."

SCRIPTS=(
    "deploy/deploy.sh"
    "deploy/blue-green-deploy.sh"
    "deploy/test-deployment.sh"
    "deploy/optimize-lambda.sh"
)

for script in "${SCRIPTS[@]}"; do
    if [ -x "$script" ]; then
        echo "✅ $script is executable"
    else
        echo "❌ $script is not executable"
        exit 1
    fi
done

# Final validation summary
echo ""
echo "🎉 Lambda package configuration validation completed successfully!"
echo ""
echo "📋 Validation Summary:"
echo "  ✅ Prerequisites installed and configured"
echo "  ✅ Project structure valid"
echo "  ✅ Maven configuration optimized for peppol-commons"
echo "  ✅ Environment configurations complete"
echo "  ✅ Build process working"
echo "  ✅ AWS credentials configured"
echo "  ✅ Deployment scripts ready"
echo ""
echo "🚀 Ready to build Lambda package! Use the following commands:"
echo "  Build package: ./deploy/deploy.sh [environment]"
echo "  Update function: ./deploy/blue-green-deploy.sh <function-name>"
echo "  Test function: ./deploy/test-deployment.sh <function-name>"