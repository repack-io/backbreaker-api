#!/bin/bash
set -euo pipefail

############################################
# Usage & basic args
############################################
if [[ $# -lt 6 ]]; then
  echo "Usage: $0 <env> <vpc-id> <db-host> <db-name> <db-username> <db-password> [key-pair-name]"
  echo "Example: $0 demo10 vpc-0a2e693cd148f3275 db.example.com repackio backbreaker S3cretPass my-keypair"
  exit 1
fi

ENV="$1"
VPC_ID="$2"
DB_HOST="$3"
DB_NAME="$4"
DB_USER="$5"
DB_PASS="$6"
KEY_PAIR="${7:-}"

REGION="us-east-2"
STACK_NAME="backbreaker-$ENV"
SAM_BUCKET="repackio-sam-artifacts"
EB_BUCKET="repackio-eb-apps"

############################################
# Paths
############################################
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
TEMPLATE_FILE="$SCRIPT_DIR/backbreaker-processing.yaml"
ZIP_FILE="$PROJECT_ROOT/backbreaker-latest.zip"

# Unique key for this build's EB app version
TIMESTAMP="$(date +%Y%m%d%H%M%S)"
EB_KEY="backbreaker-${ENV}-${TIMESTAMP}.zip"

echo "SCRIPT_DIR: $SCRIPT_DIR"
echo "PROJECT_ROOT: $PROJECT_ROOT"
echo "TEMPLATE_FILE: $TEMPLATE_FILE"
echo "ZIP_FILE: $ZIP_FILE"
echo "EB_KEY (S3 key): $EB_KEY"
echo

############################################
# Ensure buckets exist
############################################
ensure_bucket () {
  local BUCKET_NAME="$1"

  if aws s3api head-bucket --bucket "$BUCKET_NAME" --region "$REGION" 2>/dev/null; then
    echo "Bucket $BUCKET_NAME already exists."
  else
    echo "Creating bucket $BUCKET_NAME..."
    aws s3 mb "s3://$BUCKET_NAME" --region "$REGION"
  fi
}

echo "Ensuring buckets exist..."
ensure_bucket "$SAM_BUCKET"
ensure_bucket "$EB_BUCKET"
echo

############################################
# Build EB ZIP (JAR + Procfile)
############################################
echo "Building EB ZIP..."
bash "$PROJECT_ROOT/build-eb-zip.sh"

if [[ ! -f "$ZIP_FILE" ]]; then
  echo "ERROR: Expected ZIP file not found at $ZIP_FILE"
  exit 1
fi

############################################
# Upload EB ZIP to EB bucket
############################################
echo
echo "Uploading EB ZIP to S3..."
aws s3 cp "$ZIP_FILE" "s3://$EB_BUCKET/$EB_KEY" --region "$REGION"

echo
echo "Uploaded to: s3://$EB_BUCKET/$EB_KEY"
echo

############################################
# Discover subnets for the given VPC
############################################
echo "Discovering subnets for VPC $VPC_ID..."

SUBNETS=$(aws ec2 describe-subnets \
  --filters "Name=vpc-id,Values=$VPC_ID" \
  --query "Subnets[].SubnetId" \
  --output text \
  --region "$REGION" || true)

if [[ -z "$SUBNETS" ]]; then
  echo "ERROR: No subnets found for VPC $VPC_ID in region $REGION"
  exit 1
fi

read -r -a SUBNET_ARRAY <<< "$SUBNETS"

if [[ ${#SUBNET_ARRAY[@]} -lt 2 ]]; then
  echo "ERROR: Need at least two subnets in VPC $VPC_ID, found: ${#SUBNET_ARRAY[@]}"
  echo "Found subnets: ${SUBNET_ARRAY[*]}"
  exit 1
fi

SUBNET_ONE="${SUBNET_ARRAY[0]}"
SUBNET_TWO="${SUBNET_ARRAY[1]}"

echo "Using subnets: $SUBNET_ONE, $SUBNET_TWO"
echo

############################################
# Handle existing stack in bad states
############################################
echo "Checking CloudFormation stack status for $STACK_NAME..."

STACK_STATUS="$(aws cloudformation describe-stacks \
  --stack-name "$STACK_NAME" \
  --region "$REGION" \
  --query "Stacks[0].StackStatus" \
  --output text 2>/dev/null || echo "STACK_NOT_FOUND")"

echo "Current stack status: $STACK_STATUS"

if [[ "$STACK_STATUS" == "ROLLBACK_COMPLETE" || "$STACK_STATUS" == "ROLLBACK_FAILED" || "$STACK_STATUS" == "CREATE_FAILED" ]]; then
  echo
  echo "Stack $STACK_NAME is in a failed/rollback state ($STACK_STATUS)."
  echo "Deleting stack so we can recreate it cleanly..."
  aws cloudformation delete-stack --stack-name "$STACK_NAME" --region "$REGION"

  echo "Waiting for stack deletion to complete..."
  aws cloudformation wait stack-delete-complete --stack-name "$STACK_NAME" --region "$REGION"
  echo "Stack $STACK_NAME deleted."
  echo
fi

############################################
# Deploy with SAM
############################################
echo "Deploying SAM stack: $STACK_NAME..."
echo

PARAM_OVERRIDES=(
  EnvironmentName="$ENV"
  VpcId="$VPC_ID"
  SubnetIds="${SUBNET_ONE},${SUBNET_TWO}"
  EbAppVersionBucket="$EB_BUCKET"
  EbAppVersionKey="$EB_KEY"
)

if [[ -n "$KEY_PAIR" ]]; then
  PARAM_OVERRIDES+=("KeyPairName=$KEY_PAIR")
fi

PARAM_OVERRIDES+=(
  "RdsHost=$DB_HOST"
  "RdsPort=5432"
  "RdsName=$DB_NAME"
  "RdsUsername=$DB_USER"
  "RdsPassword=$DB_PASS"
)

sam deploy \
  --template-file "$TEMPLATE_FILE" \
  --stack-name "$STACK_NAME" \
  --s3-bucket "$SAM_BUCKET" \
  --capabilities CAPABILITY_IAM CAPABILITY_NAMED_IAM \
  --region "$REGION" \
  --no-confirm-changeset \
  --parameter-overrides "${PARAM_OVERRIDES[@]}"

echo
echo "Deployment complete."
echo "Stack name: $STACK_NAME"
echo "EB bundle: s3://$EB_BUCKET/$EB_KEY"
