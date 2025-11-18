# Bedrock Vision Prompts

This directory contains prompt templates for AWS Bedrock's Claude Sonnet vision model.

## Available Prompts

### card-analysis.txt
Analyzes trading card images to determine:
- Bounding box coordinates (relative 0.0-1.0)
- Precise rotation angle needed (0-360 degrees, supports decimals)
- Confidence level (0-100)
- Reasoning for the analysis

Expected JSON response format:
```json
{
  "bounding_box": {
    "left": 0.1,
    "top": 0.05,
    "width": 0.8,
    "height": 0.9
  },
  "rotation_degrees": 2.5,
  "confidence": 95,
  "reasoning": "Brief explanation"
}
```

## Usage

Prompts are automatically loaded and cached by the `BedrockVisionService`. To use a prompt:

```java
// Option 1: Use the specific method
CardAnalysisResult result = bedrockVisionService.analyzeCardImage(image);

// Option 2: Use the generic method with a custom prompt
String prompt = bedrockVisionService.loadPrompt("card-analysis.txt");
CardAnalysisResult result = bedrockVisionService.invokeWithImage(image, prompt, CardAnalysisResult.class);

// Option 3: Use with custom DTO
MyCustomResult result = bedrockVisionService.invokeWithImage(image, prompt, MyCustomResult.class);
```

## Creating New Prompts

1. Create a new `.txt` file in this directory
2. Write your prompt (can include instructions for JSON response format)
3. Create a corresponding DTO class if needed
4. Load and use via `BedrockVisionService.loadPrompt()` or `invokeWithImage()`

## Configuration

Configure the prompts directory in `application.properties`:
```properties
# Default is file:prompts/
bedrock.prompts.path=file:prompts/
```

## Notes

- Prompts are cached after first load for performance
- All prompts should request JSON responses for easy parsing
- The service automatically extracts JSON from markdown code blocks
- Supports both image + text and text-only prompts
