# Multiple Image Support for Bedrock

The Bedrock service now supports sending multiple images in a single request, which is perfect for analyzing both front and back of trading cards.

## Usage Examples

### Example 1: Analyze Front and Back of a Card

```java
@Autowired
private BedrockVisionService bedrockService;

public CardInfo analyzeCard(BufferedImage front, BufferedImage back) throws IOException {
    BufferedImage[] images = new BufferedImage[]{front, back};

    String prompt = """
        Analyze both images of this trading card (front and back).
        Extract player name, team, year, card number, and any special features.
        Return JSON with the complete card information.
        """;

    return bedrockService.invokeWithImages(
        "card-analysis",  // Use case
        images,           // Array of images
        prompt,
        CardInfo.class
    );
}
```

### Example 2: Using S3 URLs (More Efficient for Large Images)

```java
public CardInfo analyzeCardFromS3(String frontS3Url, String backS3Url) throws IOException {
    String[] s3Urls = new String[]{
        "s3://my-bucket/cards/front.jpg",
        "s3://my-bucket/cards/back.jpg"
    };

    String prompt = "Analyze both sides of this trading card...";

    // Note: S3 URLs only work with Claude models
    return bedrockService.invokeWithS3Images(
        "card-analysis",
        s3Urls,
        prompt,
        CardInfo.class
    );
}
```

### Example 3: Default Model (No Use Case)

```java
// Uses default model configuration
BufferedImage[] images = new BufferedImage[]{front, back};
CardInfo result = bedrockService.invokeWithImages(images, prompt, CardInfo.class);
```

## API Methods

### Multi-Image with BufferedImage

```java
// With use case
<T> T invokeWithImages(String useCase, BufferedImage[] images, String prompt, Class<T> responseType)

// With default model
<T> T invokeWithImages(BufferedImage[] images, String prompt, Class<T> responseType)
```

### Multi-Image with S3 URLs (Claude Only)

```java
<T> T invokeWithS3Images(String useCase, String[] s3Urls, String prompt, Class<T> responseType)
```

## Model Support

### Base64 Multi-Image Support

| Model | Supports Multiple Images | Notes |
|-------|-------------------------|-------|
| **Claude Sonnet** | ✅ Yes | Best quality, recommended |
| **Claude Haiku** | ✅ Yes | Faster, good quality |
| **Llama 3.2 Vision** | ✅ Yes | Open source option |
| **Amazon Titan** | ⚠️ Limited | Only first image used |

### S3 URL Support

| Model | Supports S3 URLs |
|-------|------------------|
| **Claude Sonnet** | ✅ Yes (via document blocks) |
| **Claude Haiku** | ✅ Yes (via document blocks) |
| **Llama 3.2** | ❌ No - base64 only |
| **Amazon Titan** | ❌ No - base64 only |

## Benefits of S3 URLs vs Base64

**Advantages of S3:**
- No base64 encoding overhead
- Faster for large images
- Lower memory usage
- Better for high-resolution images

**When to use base64:**
- Images not already in S3
- Using non-Claude models
- Small images where encoding overhead is negligible

## Request Format Examples

### Claude with Multiple Base64 Images

```json
{
  "anthropic_version": "bedrock-2023-05-31",
  "max_tokens": 1024,
  "messages": [{
    "role": "user",
    "content": [
      {"type": "image", "source": {"type": "base64", "data": "..."}},
      {"type": "image", "source": {"type": "base64", "data": "..."}},
      {"type": "text", "text": "Analyze both images..."}
    ]
  }]
}
```

### Claude with S3 URLs

```json
{
  "anthropic_version": "bedrock-2023-05-31",
  "max_tokens": 1024,
  "messages": [{
    "role": "user",
    "content": [
      {"type": "document", "source": {"type": "s3", "s3_location": "s3://..."}},
      {"type": "document", "source": {"type": "s3", "s3_location": "s3://..."}},
      {"type": "text", "text": "Analyze both images..."}
    ]
  }]
}
```

### Llama 3.2 Vision with Multiple Images

```json
{
  "prompt": "Analyze both images...",
  "max_gen_len": 1024,
  "temperature": 0.1,
  "images": ["base64_image_1", "base64_image_2"]
}
```

## Configuration

No special configuration needed! The existing model configuration works:

```properties
# Use Claude for multi-image support
bedrock.models.card-analysis.model-id=claude-sonnet

# Or use Llama 3.2 Vision
bedrock.models.card-analysis.model-id=llama3-vision
```

## Tips

1. **Image Order Matters**: Reference images by position in your prompt:
   ```
   "The first image shows the front, the second shows the back..."
   ```

2. **Size Limits**: Be aware of total payload size limits:
   - Claude: ~5 images (depending on resolution)
   - Llama: Check model-specific limits

3. **Cost**: Multiple images increase token usage. Consider:
   - Using Haiku for cost savings
   - Compressing images before sending
   - Using S3 URLs to reduce overhead

4. **Prompts**: Be explicit about what each image shows:
   ```
   "First image: card front. Second image: card back.
    Extract all text from both sides..."
   ```
