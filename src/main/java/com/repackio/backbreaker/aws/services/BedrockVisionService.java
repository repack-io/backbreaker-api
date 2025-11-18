package com.repackio.backbreaker.aws.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repackio.backbreaker.aws.bedrock.BedrockModelConfig;
import com.repackio.backbreaker.aws.bedrock.BedrockModelProvider;
import com.repackio.backbreaker.aws.bedrock.BedrockRequestBuilder;
import com.repackio.backbreaker.aws.bedrock.BedrockResponseParser;
import com.repackio.backbreaker.aws.dto.CardAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final BedrockModelConfig modelConfig;
    private final BedrockRequestBuilder requestBuilder;
    private final BedrockResponseParser responseParser;

    // Cache for loaded prompts
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();

    /**
     * Analyzes a card image using Bedrock vision model.
     * Returns bounding box coordinates and rotation information.
     * Uses the "card-analysis" use case configuration.
     */
    public CardAnalysisResult analyzeCardImage(BufferedImage image) throws IOException {
        log.info("Analyzing card image with Bedrock ({}x{})", image.getWidth(), image.getHeight());
        String prompt = loadPrompt("card_analysis_v3.txt");
        return invokeWithImage("card-analysis", image, prompt, CardAnalysisResult.class);
    }

    /**
     * Generic method to invoke Bedrock with an image and prompt, returning a typed response.
     * Uses the default model configuration.
     *
     * @param image The image to analyze
     * @param prompt The prompt text
     * @param responseType The class type to deserialize the response into
     * @return Parsed response of type T
     */
    public <T> T invokeWithImage(BufferedImage image, String prompt, Class<T> responseType) throws IOException {
        return invokeWithImage(null, image, prompt, responseType);
    }

    /**
     * Generic method to invoke Bedrock with multiple images (e.g., front/back of a card).
     * Uses the default model configuration.
     *
     * @param images Array of images to analyze (e.g., [front, back])
     * @param prompt The prompt text
     * @param responseType The class type to deserialize the response into
     * @return Parsed response of type T
     */
    public <T> T invokeWithImages(BufferedImage[] images, String prompt, Class<T> responseType) throws IOException {
        return invokeWithImages(null, images, prompt, responseType);
    }

    /**
     * Generic method to invoke Bedrock with an image and prompt, returning a typed response.
     *
     * @param useCase The use case (e.g., "card-analysis") to determine which model to use
     * @param image The image to analyze
     * @param prompt The prompt text
     * @param responseType The class type to deserialize the response into
     * @return Parsed response of type T
     */
    public <T> T invokeWithImage(String useCase, BufferedImage image, String prompt, Class<T> responseType) throws IOException {
        return invokeWithImages(useCase, new BufferedImage[]{image}, prompt, responseType);
    }

    /**
     * Generic method to invoke Bedrock with multiple images and prompt, returning a typed response.
     *
     * @param useCase The use case (e.g., "card-analysis") to determine which model to use
     * @param images Array of images to analyze (e.g., front and back of a card)
     * @param prompt The prompt text
     * @param responseType The class type to deserialize the response into
     * @return Parsed response of type T
     */
    public <T> T invokeWithImages(String useCase, BufferedImage[] images, String prompt, Class<T> responseType) throws IOException {
        if (images == null || images.length == 0) {
            throw new IllegalArgumentException("At least one image is required");
        }

        BedrockModelConfig.ModelSettings settings = modelConfig.getSettingsForUseCase(useCase);
        String modelId = settings.getModelId();
        BedrockModelProvider provider = BedrockModelProvider.fromModelId(modelId);

        // Encode all images to base64
        String[] base64Images = new String[images.length];
        for (int i = 0; i < images.length; i++) {
            base64Images[i] = encodeImageToBase64(images[i]);
        }

        String requestBody = requestBuilder.buildImageRequest(
                provider, base64Images, prompt,
                settings.getMaxTokens(), settings.getTemperature());

        log.info("Invoking Bedrock model: {} (provider: {}, use case: {}, image count: {})",
                modelId, provider, useCase, images.length);

        InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId(modelId)
                .body(SdkBytes.fromUtf8String(requestBody))
                .build();

        InvokeModelResponse response = bedrockClient.invokeModel(request);
        String responseBody = response.body().asUtf8String();
        log.debug("Bedrock response: {}", responseBody);

        return responseParser.parseTypedResponse(provider, responseBody, responseType);
    }

    /**
     * Generic method to invoke Bedrock with S3 image URLs (Claude only).
     * More efficient than base64 for large images.
     *
     * @param useCase The use case to determine model settings
     * @param s3Urls Array of S3 URLs (s3://bucket/key format)
     * @param prompt The prompt text
     * @param responseType The class type to deserialize the response into
     * @return Parsed response of type T
     */
    public <T> T invokeWithS3Images(String useCase, String[] s3Urls, String prompt, Class<T> responseType) throws IOException {
        if (s3Urls == null || s3Urls.length == 0) {
            throw new IllegalArgumentException("At least one S3 URL is required");
        }

        BedrockModelConfig.ModelSettings settings = modelConfig.getSettingsForUseCase(useCase);
        String modelId = settings.getModelId();
        BedrockModelProvider provider = BedrockModelProvider.fromModelId(modelId);

        if (provider != BedrockModelProvider.ANTHROPIC) {
            throw new UnsupportedOperationException("S3 URLs are only supported for Claude models");
        }

        String requestBody = requestBuilder.buildImageRequestWithS3(
                s3Urls, prompt,
                settings.getMaxTokens(), settings.getTemperature());

        log.info("Invoking Bedrock model: {} with S3 images (use case: {}, image count: {})",
                modelId, useCase, s3Urls.length);

        InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId(modelId)
                .body(SdkBytes.fromUtf8String(requestBody))
                .build();

        InvokeModelResponse response = bedrockClient.invokeModel(request);
        String responseBody = response.body().asUtf8String();
        log.debug("Bedrock response: {}", responseBody);

        return responseParser.parseTypedResponse(provider, responseBody, responseType);
    }

    /**
     * Generic method to invoke Bedrock with text-only prompt, returning a typed response.
     * Uses the default model configuration.
     *
     * @param prompt The prompt text
     * @param responseType The class type to deserialize the response into
     * @return Parsed response of type T
     */
    public <T> T invokeWithText(String prompt, Class<T> responseType) throws IOException {
        return invokeWithText(null, prompt, responseType);
    }

    /**
     * Generic method to invoke Bedrock with text-only prompt, returning a typed response.
     *
     * @param useCase The use case (e.g., "text-generation") to determine which model to use
     * @param prompt The prompt text
     * @param responseType The class type to deserialize the response into
     * @return Parsed response of type T
     */
    public <T> T invokeWithText(String useCase, String prompt, Class<T> responseType) throws IOException {
        BedrockModelConfig.ModelSettings settings = modelConfig.getSettingsForUseCase(useCase);
        String modelId = settings.getModelId();
        BedrockModelProvider provider = BedrockModelProvider.fromModelId(modelId);

        String requestBody = requestBuilder.buildTextRequest(
                provider, prompt,
                settings.getMaxTokens(), settings.getTemperature());

        log.info("Invoking Bedrock model: {} (provider: {}, use case: {})", modelId, provider, useCase);

        InvokeModelRequest request = InvokeModelRequest.builder()
                .modelId(modelId)
                .body(SdkBytes.fromUtf8String(requestBody))
                .build();

        InvokeModelResponse response = bedrockClient.invokeModel(request);
        String responseBody = response.body().asUtf8String();
        log.debug("Bedrock response: {}", responseBody);

        return responseParser.parseTypedResponse(provider, responseBody, responseType);
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
                String resourcePath = modelConfig.getPromptsPath() + fileName;
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

    private String encodeImageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}
