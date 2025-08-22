#!/bin/bash

# Script to run Maven commands with Java 17
# Usage: ./run-with-java17.sh mvn compile
# Usage: ./run-with-java17.sh mvn test

export JAVA_HOME=/Users/devashishbeniwal/Library/Java/JavaVirtualMachines/corretto-17.0.12/Contents/Home

echo "Using Java 17: $JAVA_HOME"
java -version

# Execute the provided command
exec "$@"