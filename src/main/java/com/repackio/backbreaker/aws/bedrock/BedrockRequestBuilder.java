package com.repackio.backbreaker.aws.bedrock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Builds request payloads for different Bedrock model providers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BedrockRequestBuilder {

    private final ObjectMapper objectMapper;

    /**
     * Builds a request body for a text-and-image prompt (single image).
     */
    public String buildImageRequest(BedrockModelProvider provider, String base64Image,
                                     String prompt, int maxTokens, double temperature) throws IOException {
        return buildImageRequest(provider, new String[]{base64Image}, prompt, maxTokens, temperature);
    }

    /**
     * Builds a request body for a text-and-image prompt (multiple images).
     * Useful for sending front/back of a card or multiple views.
     *
     * @param base64Images Array of base64-encoded images
     */
    public String buildImageRequest(BedrockModelProvider provider, String[] base64Images,
                                     String prompt, int maxTokens, double temperature) throws IOException {
        if (base64Images == null || base64Images.length == 0) {
            throw new IllegalArgumentException("At least one image is required");
        }

        return switch (provider) {
            case ANTHROPIC -> buildAnthropicImageRequest(base64Images, prompt, maxTokens, temperature);
            case META -> buildMetaImageRequest(base64Images, prompt, maxTokens, temperature);
            case AMAZON -> buildAmazonImageRequest(base64Images, prompt, maxTokens, temperature);
            default -> throw new UnsupportedOperationException(
                    "Image requests not yet supported for provider: " + provider);
        };
    }

    /**
     * Builds a request body using S3 URLs instead of base64 (Claude only).
     * This is more efficient for large images as it avoids encoding overhead.
     *
     * @param s3Urls Array of S3 URLs (s3://bucket/key format)
     */
    public String buildImageRequestWithS3(String[] s3Urls, String prompt,
                                          int maxTokens, double temperature) throws IOException {
        if (s3Urls == null || s3Urls.length == 0) {
            throw new IllegalArgumentException("At least one S3 URL is required");
        }

        // Only Claude supports S3 URLs via document blocks
        return buildAnthropicImageRequestWithS3(s3Urls, prompt, maxTokens, temperature);
    }

    /**
     * Builds a request body for a text-only prompt.
     */
    public String buildTextRequest(BedrockModelProvider provider, String prompt,
                                    int maxTokens, double temperature) throws IOException {
        return switch (provider) {
            case ANTHROPIC -> buildAnthropicTextRequest(prompt, maxTokens, temperature);
            case META -> buildMetaTextRequest(prompt, maxTokens, temperature);
            case AMAZON -> buildAmazonTextRequest(prompt, maxTokens, temperature);
            default -> throw new UnsupportedOperationException(
                    "Text requests not yet supported for provider: " + provider);
        };
    }

    // ==================== ANTHROPIC (Claude) ====================

    private String buildAnthropicImageRequest(String[] base64Images, String prompt,
                                               int maxTokens, double temperature) throws IOException {
        var payload = objectMapper.createObjectNode();
        payload.put("anthropic_version", "bedrock-2023-05-31");
        payload.put("max_tokens", maxTokens);
        payload.put("temperature", temperature);

        var messages = payload.putArray("messages");
        var message = messages.addObject();
        message.put("role", "user");

        var content = message.putArray("content");

        // Add all images first
        for (String base64Image : base64Images) {
            var imageContent = content.addObject();
            imageContent.put("type", "image");
            var source = imageContent.putObject("source");
            source.put("type", "base64");
            source.put("media_type", "image/jpeg");
            source.put("data", base64Image);
        }

        // Add text prompt after images
        var textContent = content.addObject();
        textContent.put("type", "text");
        textContent.put("text", prompt);

        return objectMapper.writeValueAsString(payload);
    }

    private String buildAnthropicImageRequestWithS3(String[] s3Urls, String prompt,
                                                     int maxTokens, double temperature) throws IOException {
        var payload = objectMapper.createObjectNode();
        payload.put("anthropic_version", "bedrock-2023-05-31");
        payload.put("max_tokens", maxTokens);
        payload.put("temperature", temperature);

        var messages = payload.putArray("messages");
        var message = messages.addObject();
        message.put("role", "user");

        var content = message.putArray("content");

        // Add all S3 document references
        for (String s3Url : s3Urls) {
            var documentContent = content.addObject();
            documentContent.put("type", "document");
            var source = documentContent.putObject("source");
            source.put("type", "s3");
            source.put("s3_location", s3Url);
        }

        // Add text prompt after documents
        var textContent = content.addObject();
        textContent.put("type", "text");
        textContent.put("text", prompt);

        return objectMapper.writeValueAsString(payload);
    }

    private String buildAnthropicTextRequest(String prompt, int maxTokens, double temperature) throws IOException {
        var payload = objectMapper.createObjectNode();
        payload.put("anthropic_version", "bedrock-2023-05-31");
        payload.put("max_tokens", maxTokens);
        payload.put("temperature", temperature);

        var messages = payload.putArray("messages");
        var message = messages.addObject();
        message.put("role", "user");

        var content = message.putArray("content");
        var textContent = content.addObject();
        textContent.put("type", "text");
        textContent.put("text", prompt);

        return objectMapper.writeValueAsString(payload);
    }

    // ==================== META (Llama) ====================

    private String buildMetaImageRequest(String[] base64Images, String prompt,
                                          int maxTokens, double temperature) throws IOException {
        // Meta Llama 3.2 Vision format
        var payload = objectMapper.createObjectNode();
        payload.put("prompt", prompt);
        payload.put("max_gen_len", maxTokens);
        payload.put("temperature", temperature);

        // Add all images to payload
        var images = payload.putArray("images");
        for (String base64Image : base64Images) {
            images.add(base64Image);
        }

        return objectMapper.writeValueAsString(payload);
    }

    private String buildMetaTextRequest(String prompt, int maxTokens, double temperature) throws IOException {
        // Meta Llama format
        var payload = objectMapper.createObjectNode();
        payload.put("prompt", prompt);
        payload.put("max_gen_len", maxTokens);
        payload.put("temperature", temperature);

        return objectMapper.writeValueAsString(payload);
    }

    // ==================== AMAZON (Titan) ====================

    private String buildAmazonImageRequest(String[] base64Images, String prompt,
                                            int maxTokens, double temperature) throws IOException {
        // Amazon Titan Multimodal format
        // Note: Titan may have limitations on multiple images - check model docs
        var payload = objectMapper.createObjectNode();

        var textGenerationConfig = payload.putObject("textGenerationConfig");
        textGenerationConfig.put("maxTokenCount", maxTokens);
        textGenerationConfig.put("temperature", temperature);

        payload.put("inputText", prompt);

        // Titan typically expects a single image, but we'll add the first one
        // For multiple images, consider using Claude instead
        if (base64Images.length > 1) {
            log.warn("Amazon Titan may not support multiple images. Only the first image will be used.");
        }
        payload.put("inputImage", base64Images[0]);

        return objectMapper.writeValueAsString(payload);
    }

    private String buildAmazonTextRequest(String prompt, int maxTokens, double temperature) throws IOException {
        // Amazon Titan Text format
        var payload = objectMapper.createObjectNode();

        payload.put("inputText", prompt);

        var textGenerationConfig = payload.putObject("textGenerationConfig");
        textGenerationConfig.put("maxTokenCount", maxTokens);
        textGenerationConfig.put("temperature", temperature);

        return objectMapper.writeValueAsString(payload);
    }
}
