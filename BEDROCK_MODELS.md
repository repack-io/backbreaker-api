# Bedrock Model Configuration Guide

The Backbreaker API supports flexible configuration of AWS Bedrock models for different use cases. You can easily switch between different AI models (Claude, Llama, Titan, etc.) via configuration.

## Quick Start

### Local Development: application-local.properties

Create `src/main/resources/application-local.properties` for local development:

```properties
# Switch between models using presets
bedrock.default-model-id=claude-sonnet  # or llama3-90b, claude-haiku, etc.

# Or use a full model ID
bedrock.default-model-id=us.anthropic.claude-3-5-sonnet-20241022-v2:0
```

Then run with: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`

### AWS Deployment: application-aws.properties

For AWS/EB deployment, settings are in `src/main/resources/application-aws.properties`.
The SAM template automatically sets `SPRING_PROFILES_ACTIVE=aws`.

## Available Model Presets

The following presets are configured out-of-the-box:

| Preset | Model | Provider | Best For |
|--------|-------|----------|----------|
| `claude-sonnet` | Claude 3.5 Sonnet | Anthropic | **Default** - Highest quality, vision capable |
| `claude-haiku` | Claude 3.5 Haiku | Anthropic | Faster, more cost-effective |
| `llama3-90b` | Llama 3.2 90B | Meta | Open source, high quality text |
| `llama3-11b` | Llama 3.2 11B | Meta | Open source, faster, lower cost |
| `llama3-vision` | Llama 3.2 90B Vision | Meta | Open source, vision capable |

## Use Case Specific Models

You can configure different models for different use cases:

```properties
# Use Claude Sonnet for card analysis (vision task)
bedrock.models.card-analysis.model-id=claude-sonnet

# Use Llama 3 for text generation (if you add this feature)
bedrock.models.text-generation.model-id=llama3-90b
bedrock.models.text-generation.max-tokens=2048
bedrock.models.text-generation.temperature=0.7
```

Or via environment variables:

```bash
BEDROCK_MODELS_CARD_ANALYSIS_MODEL_ID=claude-sonnet
BEDROCK_MODELS_TEXT_GENERATION_MODEL_ID=llama3-90b
BEDROCK_MODELS_TEXT_GENERATION_TEMPERATURE=0.7
```

## Configuration Options

### Global Defaults

These apply to all models unless overridden:

```properties
bedrock.default-model-id=claude-sonnet  # Default model
bedrock.max-tokens=1024                 # Max response tokens
bedrock.temperature=0.1                 # Randomness (0.0-1.0)
bedrock.prompts-path=file:prompts/     # Prompt files location
```

### Per-Use-Case Settings

Each use case can override any setting:

```properties
bedrock.models.{use-case}.model-id=     # Which model to use
bedrock.models.{use-case}.max-tokens=   # Max tokens for this use case
bedrock.models.{use-case}.temperature=  # Temperature for this use case
```

## Using Full Model IDs

Instead of presets, you can use full model IDs:

```properties
bedrock.default-model-id=us.anthropic.claude-3-5-sonnet-20241022-v2:0
```

Or add your own presets:

```properties
bedrock.presets.my-model=us.amazon.titan-text-express-v1
bedrock.default-model-id=my-model
```

## Supported Providers

The system automatically detects the provider from the model ID:

- **Anthropic**: Claude models (best for vision + structured output)
- **Meta**: Llama models (open source, cost-effective)
- **Amazon**: Titan models
- **AI21**: Jamba models
- **Cohere**: Command models
- **Mistral**: Mistral models

## Adding New Use Cases in Code

To use a specific model for a new feature:

```java
// Use default model
CardAnalysisResult result = bedrockVisionService.invokeWithImage(
    image, prompt, CardAnalysisResult.class
);

// Use specific use case configuration
MyResponse response = bedrockVisionService.invokeWithText(
    "my-use-case",  // References bedrock.models.my-use-case.*
    prompt,
    MyResponse.class
);
```

## Examples

### Example 1: Switch to Llama for Cost Savings

In `application-local.properties`:
```properties
bedrock.default-model-id=llama3-90b
```

### Example 2: Use Different Models for Different Tasks

In `application.properties`:
```properties
# Use Claude for vision tasks
bedrock.models.card-analysis.model-id=claude-sonnet

# Use Llama for text generation (if you add this feature)
bedrock.models.text-generation.model-id=llama3-90b
```

### Example 3: Test with Faster Model in Development

In `application-local.properties`:
```properties
# Faster, cheaper for testing
bedrock.default-model-id=claude-haiku
```

### Example 4: Production Settings

In `application-aws.properties`:
```properties
# Use best quality model in production
bedrock.default-model-id=claude-sonnet
bedrock.models.card-analysis.max-tokens=2048
bedrock.models.card-analysis.temperature=0.05
```
