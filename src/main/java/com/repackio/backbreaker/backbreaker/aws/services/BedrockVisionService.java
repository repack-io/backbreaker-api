package com.repackio.backbreaker.backbreaker.aws.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repackio.backbreaker.backbreaker.aws.dto.CardAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class BedrockVisionService {

    private final BedrockRuntimeClient bedrockClient;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${bedrock.model.id:us.anthropic.claude-3-5-sonnet-20241022-v2:0}")
    private String modelId;

    @Value("${bedrock.max.tokens:1024}")
    private int maxTokens;

    @Value("${bedrock.temperature:0.1}")
    private double temperature;

    @Value("${bedrock.prompts.path:file:prompts/}")
    private String promptsPath;

    // Cache for loaded prompts
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();

    /**
     * Analyzes a card image using Bedrock's Claude Sonnet vision model.
     * Returns bounding box coordinates and rotation information.
     */
    public CardAnalysisResult analyzeCardImage(BufferedImage image) throws IOException {
        log.info("Analyzing card image with Bedrock ({}x{})", image.getWidth(), image.getHeight());
        String prompt = loadPrompt("card_analysis_v3.txt");
        return invokeWithImage(image, prompt, CardAnalysisResult.class);
    }

    /**
     * Generic method to invoke Bedrock with an image and prompt, returning a typed response.
     *
     * @param image The image to analyze
     * @param prompt The prompt text
     * @param responseType The class type to deserialize the response into
     * @return Parsed response of type T
     */
    public <T> T invokeWithImage(BufferedImage image, String prompt, Class<T> responseType) throws IOException {
        String base64Image = encodeImageToBase64(image);
        String requestBody = buildRequestBody(base64Image, prompt);

        log.debug("Sending request to Bedrock model: {}", modelId);

        InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId(modelId)
                .body(SdkBytes.fromUtf8String(requestBody))
                .build();

        InvokeModelResponse response = bedrockClient.invokeModel(request);
        String responseBody = response.body().asUtf8String();
        log.debug("Bedrock response: {}", responseBody);

        return parseTypedResponse(responseBody, responseType);
    }

    /**
     * Generic method to invoke Bedrock with text-only prompt, returning a typed response.
     *
     * @param prompt The prompt text
     * @param responseType The class type to deserialize the response into
     * @return Parsed response of type T
     */
    public <T> T invokeWithText(String prompt, Class<T> responseType) throws IOException {
        String requestBody = buildTextOnlyRequestBody(prompt);

        log.debug("Sending text-only request to Bedrock model: {}", modelId);

        InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId(modelId)
                .body(SdkBytes.fromUtf8String(requestBody))
                .build();

        InvokeModelResponse response = bedrockClient.invokeModel(request);
        String responseBody = response.body().asUtf8String();
        log.debug("Bedrock response: {}", responseBody);

        return parseTypedResponse(responseBody, responseType);
    }

    /**
     * Loads a prompt from the prompts directory.
     * Prompts are cached after first load.
     *
     * @param promptFileName The filename of the prompt (e.g., "card-analysis.txt")
     * @return The prompt text
     */
    public String loadPrompt(String promptFileName) throws IOException {
        return promptCache.computeIfAbsent(promptFileName, fileName -> {
            try {
                String resourcePath = promptsPath + fileName;
                Resource resource = resourceLoader.getResource(resourcePath);

                if (!resource.exists()) {
                    throw new IOException("Prompt file not found: " + resourcePath);
                }

                String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                log.info("Loaded prompt from: {}", resourcePath);
                return content;
            } catch (IOException e) {
                throw new RuntimeException("Failed to load prompt: " + fileName, e);
            }
        });
    }

    private String buildRequestBody(String base64Image, String prompt) throws IOException {
        // Build the Anthropic Messages API format
        var payload = objectMapper.createObjectNode();
        payload.put("anthropic_version", "bedrock-2023-05-31");
        payload.put("max_tokens", maxTokens);
        payload.put("temperature", temperature);

        var messages = payload.putArray("messages");
        var message = messages.addObject();
        message.put("role", "user");

        var content = message.putArray("content");

        // Add image
        var imageContent = content.addObject();
        imageContent.put("type", "image");
        var source = imageContent.putObject("source");
        source.put("type", "base64");
        source.put("media_type", "image/jpeg");
        source.put("data", base64Image);

        // Add text prompt
        var textContent = content.addObject();
        textContent.put("type", "text");
        textContent.put("text", prompt);

        return objectMapper.writeValueAsString(payload);
    }

    private String buildTextOnlyRequestBody(String prompt) throws IOException {
        // Build the Anthropic Messages API format for text-only
        var payload = objectMapper.createObjectNode();
        payload.put("anthropic_version", "bedrock-2023-05-31");
        payload.put("max_tokens", maxTokens);
        payload.put("temperature", temperature);

        var messages = payload.putArray("messages");
        var message = messages.addObject();
        message.put("role", "user");

        var content = message.putArray("content");

        // Add text prompt
        var textContent = content.addObject();
        textContent.put("type", "text");
        textContent.put("text", prompt);

        return objectMapper.writeValueAsString(payload);
    }

    private <T> T parseTypedResponse(String responseBody, Class<T> responseType) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);

        // Extract the text content from Claude's response
        JsonNode contentArray = root.path("content");
        if (!contentArray.isArray() || contentArray.isEmpty()) {
            throw new IOException("Invalid Bedrock response: no content array");
        }

        String textResponse = contentArray.get(0).path("text").asText();
        log.debug("Claude response text: {}", textResponse);

        // Extract JSON from the response (it might be wrapped in markdown code blocks)
        String jsonStr = extractJson(textResponse);

        // Parse the typed response
        T result = objectMapper.readValue(jsonStr, responseType);

        // Log specific info for CardAnalysisResult
        if (result instanceof CardAnalysisResult cardResult) {
            log.info("Card analysis: bbox=[{}, {}, {}, {}], rotation={}, confidence={}",
                    cardResult.getBoundingBox().getLeft(),
                    cardResult.getBoundingBox().getTop(),
                    cardResult.getBoundingBox().getWidth(),
                    cardResult.getBoundingBox().getHeight(),
                    cardResult.getRotationDegrees(),
                    cardResult.getConfidence());
        }

        return result;
    }

    private String extractJson(String text) {
        // Remove markdown code blocks if present
        String cleaned = text.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        // Find the first '{' and last '}'
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1).trim();
        }

        return cleaned.trim();
    }

    private String encodeImageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}
