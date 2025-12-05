# Deployment Guide

## Environment Profiles

The application supports multiple environment profiles:

### Supported Environments
- `dev` - Development environment
- `qa` - QA/Testing environment
- `prd` - Production environment
- `demo` or `demo-*` - Demo environments
- `test` or `test-*` - Test environments

### Profile Structure

Each environment has two profile types:

1. **AWS Profiles** (`application-aws-{env}.properties`)
   - Used when deployed to Elastic Beanstalk
   - Loads database credentials from AWS Secrets Manager
   - Environment-specific URLs and configurations

2. **Local Profiles** (`application-local-{env}.properties`)
   - Used for local development
   - Contains direct database credentials
   - **NOT committed to git** (contains sensitive data)

## Building for Deployment

### Build for Specific Environment

```bash
./build-eb-zip.sh <environment>
```

Examples:
```bash
./build-eb-zip.sh dev    # Build for DEV
./build-eb-zip.sh qa     # Build for QA
./build-eb-zip.sh prd    # Build for PRODUCTION
```

### What the Build Script Does

1. Validates the environment name
2. Builds the Spring Boot JAR
3. Creates an Elastic Beanstalk deployment package
4. Sets the appropriate Spring profile in the Procfile: `aws-{env}`

### Default Environment

If no environment is specified, it defaults to `qa`:
```bash
./build-eb-zip.sh  # Builds for QA
```

## Local Development

### Running Locally Against Different Environments

```bash
# Connect to DEV database
./mvnw spring-boot:run -Dspring-boot.run.profiles=local-dev

# Connect to QA database
./mvnw spring-boot:run -Dspring-boot.run.profiles=local-qa

# Connect to TEST database
./mvnw spring-boot:run -Dspring-boot.run.profiles=local-test
```

### Creating Your Local Configuration

Copy one of the example files and modify for your setup:

```bash
cp src/main/resources/application-local-dev.properties src/main/resources/application-local-myenv.properties
# Edit the file with your database credentials
```

## Environment-Specific Configuration

### Database Secrets

Each environment in AWS should have a corresponding secret:
- DEV: `backbreaker/dev/database/app`
- QA: `backbreaker/qa/database/app`
- PRD: `backbreaker/prd/database/app`

Set the `DB_SECRET_NAME` environment variable in Elastic Beanstalk configuration.

### Key Configuration Differences

| Configuration | DEV | QA | PRD |
|--------------|-----|-----|-----|
| Label Sheet URL | backbreaker-dev... | backbreaker-qa... | www.repacks.io |
| Connection Pool | 2-5 connections | 2-5 connections | 5-20 connections |
| Lazy Init | true | true | false |
| Logging Level | DEBUG | DEBUG | INFO |

## Deployment Workflow

### 1. Build for Target Environment
```bash
./build-eb-zip.sh qa
```

### 2. Deploy to Elastic Beanstalk
```bash
# Upload to EB (example)
aws elasticbeanstalk create-application-version \
  --application-name backbreaker \
  --version-label "v1.0.0-qa" \
  --source-bundle S3Bucket="my-bucket",S3Key="backbreaker-latest.zip"
```

### 3. Verify Profile is Active
Check the logs to confirm the correct profile loaded:
```
Activating profile: aws-qa
```

## Troubleshooting

### Wrong URL Showing Up
- Verify the build was done with correct environment: `./build-eb-zip.sh <env>`
- Check the Procfile in the deployment package
- Verify `SPRING_PROFILES_ACTIVE` environment variable in EB

### Database Connection Issues
- Verify the secret name matches the environment
- Check secret exists in Secrets Manager: `backbreaker/{env}/database/app`
- Confirm database user has correct permissions

### Profile Not Loading
- Check Procfile has: `--spring.profiles.active=aws-{env}`
- Verify property file exists: `application-aws-{env}.properties`
- Check application logs for profile activation messages
