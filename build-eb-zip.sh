#!/bin/bash
set -e

# Determine the script location and project root
SCRIPT_DIR=$(dirname "$0")
PROJECT_ROOT="$SCRIPT_DIR"   # script is already in root

echo "Building Spring Boot JAR..."

cd "$PROJECT_ROOT"

mvn -q -DskipTests package

JAR_FILE=$(ls target/*.jar | head -n 1)

if [[ -z "$JAR_FILE" ]]; then
  echo "ERROR: No JAR produced."
  exit 1
fi

echo "Building EB ZIP package..."
rm -f backbreaker-latest.zip
rm -rf eb_build
mkdir eb_build

# Copy JAR
cp "$JAR_FILE" eb_build/application.jar

# EB Procfile
cat <<EOF > eb_build/Procfile
web: java -jar application.jar --server.port=5000
EOF

cd eb_build
zip -rq ../backbreaker-latest.zip .
cd ..

echo "EB ZIP built: backbreaker-latest.zip"
