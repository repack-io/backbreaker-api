#!/bin/bash
set -e

# Determine the script location and project root
SCRIPT_DIR=$(dirname "$0")
PROJECT_ROOT="$SCRIPT_DIR"   # script is already in root

# Parse environment argument
ENV="${1:-qa}"  # Default to qa if not specified

# Validate environment
case "$ENV" in
  dev|qa|prd|demo|demo-*|test|test-*)
    echo "Building for environment: $ENV"
    ;;
  *)
    echo "ERROR: Invalid environment '$ENV'"
    echo "Valid environments: dev, qa, prd, demo, demo-*, test, test-*"
    exit 1
    ;;
esac

echo "Building Spring Boot JAR..."

cd "$PROJECT_ROOT"

./mvnw -q clean package -DskipTests

JAR_FILE=$(ls target/*.jar | head -n 1)

if [[ -z "$JAR_FILE" ]]; then
  echo "ERROR: No JAR produced."
  exit 1
fi

echo "Building EB ZIP package for environment: $ENV..."
rm -f backbreaker-latest.zip
rm -rf eb_build
mkdir eb_build

# Copy JAR
cp "$JAR_FILE" eb_build/application.jar

# EB Procfile with environment-specific profile
cat <<EOF > eb_build/Procfile
web: java -jar application.jar --server.port=5000 --spring.profiles.active=aws-$ENV
EOF

cd eb_build
zip -rq ../backbreaker-latest.zip .
cd ..

echo "EB ZIP built: backbreaker-latest.zip (profile: aws-$ENV)"
