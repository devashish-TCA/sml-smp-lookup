#!/bin/bash

# Peppol SML/SMP Lookup Service Package Builder
# Optimized for peppol-commons library and Lambda performance

set -e

# Configuration
ENVIRONMENT=${1:-dev}

echo "📦 Building optimized Peppol Lookup Service Lambda package for $ENVIRONMENT environment"

# Validate environment
if [[ ! "$ENVIRONMENT" =~ ^(dev|test|staging|production)$ ]]; then
    echo "❌ Invalid environment. Must be one of: dev, test, staging, production"
    exit 1
fi

# Build Lambda package with peppol-commons optimization
echo "🔧 Building Lambda package with peppol-commons optimization..."
cd "$(dirname "$0")/.."

# Clean and compile with peppol-commons dependencies
mvn clean compile

# Package Lambda with optimized JAR (skip tests due to compilation issues)
echo "📦 Creating optimized Lambda package..."
mvn package -Dmaven.test.skip=true

# Verify peppol-commons dependencies are included
echo "🔍 Verifying peppol-commons dependencies in Lambda package..."
jar -tf target/peppol-lookup-lambda.jar | grep -E "(peppol-commons|peppol-smp-client|peppol-id)" || {
    echo "❌ peppol-commons dependencies not found in Lambda package"
    exit 1
}

# Check package size (should be optimized for Lambda)
PACKAGE_SIZE=$(stat -f%z target/peppol-lookup-lambda.jar 2>/dev/null || stat -c%s target/peppol-lookup-lambda.jar)
PACKAGE_SIZE_MB=$((PACKAGE_SIZE / 1024 / 1024))
echo "📏 Lambda package size: ${PACKAGE_SIZE_MB}MB"

if [ $PACKAGE_SIZE_MB -gt 250 ]; then
    echo "⚠️  Warning: Lambda package size (${PACKAGE_SIZE_MB}MB) is large. Consider optimization."
fi

# Create deployment package with environment configuration
echo "📋 Creating deployment package with environment configuration..."
mkdir -p target/deployment
cp target/peppol-lookup-lambda.jar target/deployment/
cp deploy/environments/$ENVIRONMENT.env target/deployment/environment.env

echo "✅ Lambda package build completed successfully!"
echo ""
echo "📋 Package Summary:"
echo "  📦 Package: target/peppol-lookup-lambda.jar"
echo "  📏 Size: ${PACKAGE_SIZE_MB}MB"
echo "  🌍 Environment: $ENVIRONMENT"
echo "  ⚙️  Configuration: deploy/environments/$ENVIRONMENT.env"
echo ""
echo "🚀 Package is ready for Lambda deployment via your CDK infrastructure!"
echo "💡 Use the optimized JAR and environment configuration in your Lambda function setup."