# Configuration Guide

This project uses Spring Boot profiles to manage different environments.

## Configuration Files

### Base Configuration
- **application.properties** - Shared settings for all environments
  - Bedrock model presets
  - Default model settings
  - Shared configuration

### Environment-Specific Configuration

#### Local Development
- **application-local.properties** (gitignored - create from example)
  - Local database credentials
  - AWS profile for local development
  - Dev/test S3 buckets
  - Model overrides for testing

To create:
```bash
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
# Edit with your local settings
```

Run with:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

#### AWS/Production
- **application-aws.properties** - AWS Elastic Beanstalk settings
  - Uses Secrets Manager for database credentials
  - Production S3 buckets
  - CloudWatch logging
  - Automatically activated when `SPRING_PROFILES_ACTIVE=aws` (set by SAM template)

## Quick Model Switching

### In Any Properties File

```properties
# Use a preset
bedrock.default-model-id=claude-sonnet

# Or use full model ID
bedrock.default-model-id=us.anthropic.claude-3-5-sonnet-20241022-v2:0
```

### Available Presets

Defined in `application.properties`:
- `claude-sonnet` - Claude 3.5 Sonnet (best quality)
- `claude-haiku` - Claude 3.5 Haiku (faster, cheaper)
- `llama3-90b` - Llama 3.2 90B
- `llama3-11b` - Llama 3.2 11B
- `llama3-vision` - Llama 3.2 90B Vision

## Use Case Specific Models

Configure different models for different handlers:

```properties
# Card analysis uses Claude
bedrock.models.card-analysis.model-id=claude-sonnet
bedrock.models.card-analysis.temperature=0.1

# Text generation uses Llama (example)
bedrock.models.text-generation.model-id=llama3-90b
bedrock.models.text-generation.temperature=0.7
```

## Examples

### Local Testing with Cheaper Model

In `application-local.properties`:
```properties
# Use faster, cheaper model for development
bedrock.default-model-id=claude-haiku
```

### Production with Best Quality

In `application-aws.properties`:
```properties
# Use best model in production
bedrock.default-model-id=claude-sonnet
bedrock.models.card-analysis.max-tokens=2048
```

### Testing Open Source Models

In `application-local.properties`:
```properties
# Try Llama instead of Claude
bedrock.default-model-id=llama3-vision
```

## Important Files

```
src/main/resources/
├── application.properties                    # Base config (committed)
├── application-local.properties.example      # Local template (committed)
├── application-local.properties              # Your local config (gitignored)
└── application-aws.properties                # AWS config (committed)
```

## See Also

- [BEDROCK_MODELS.md](BEDROCK_MODELS.md) - Detailed model configuration guide
- [BEDROCK_MULTI_IMAGE.md](BEDROCK_MULTI_IMAGE.md) - Multi-image support guide
