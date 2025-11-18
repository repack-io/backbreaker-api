package com.repackio.backbreaker.aws.bedrock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Parses responses from different Bedrock model providers.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BedrockResponseParser {

    private final ObjectMapper objectMapper;

    /**
     * Extracts the text content from a Bedrock response.
     * Handles different response formats based on the provider.
     */
    public String extractText(BedrockModelProvider provider, String responseBody) throws IOException {
        return switch (provider) {
            case ANTHROPIC -> extractAnthropicText(responseBody);
            case META -> extractMetaText(responseBody);
            case AMAZON -> extractAmazonText(responseBody);
            default -> throw new UnsupportedOperationException(
                    "Response parsing not yet supported for provider: " + provider);
        };
    }

    /**
     * Parses the text response into a typed object.
     */
    public <T> T parseTypedResponse(BedrockModelProvider provider, String responseBody,
                                     Class<T> responseType) throws IOException {
        String textResponse = extractText(provider, responseBody);
        log.debug("Model response text: {}", textResponse);

        // Extract JSON from the response (it might be wrapped in markdown code blocks)
        String jsonStr = extractJson(textResponse);

        // Parse the typed response
        return objectMapper.readValue(jsonStr, responseType);
    }

    // ==================== ANTHROPIC (Claude) ====================

    private String extractAnthropicText(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode contentArray = root.path("content");

        if (!contentArray.isArray() || contentArray.isEmpty()) {
            throw new IOException("Invalid Anthropic response: no content array");
        }

        return contentArray.get(0).path("text").asText();
    }

    // ==================== META (Llama) ====================

    private String extractMetaText(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);

        // Llama 3 format: { "generation": "..." }
        JsonNode generation = root.path("generation");
        if (!generation.isMissingNode()) {
            return generation.asText();
        }

        // Alternative format: { "outputs": [{ "text": "..." }] }
        JsonNode outputs = root.path("outputs");
        if (outputs.isArray() && !outputs.isEmpty()) {
            return outputs.get(0).path("text").asText();
        }

        throw new IOException("Invalid Meta/Llama response format");
    }

    // ==================== AMAZON (Titan) ====================

    private String extractAmazonText(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);

        // Titan format: { "results": [{ "outputText": "..." }] }
        JsonNode results = root.path("results");
        if (results.isArray() && !results.isEmpty()) {
            return results.get(0).path("outputText").asText();
        }

        throw new IOException("Invalid Amazon Titan response format");
    }

    // ==================== UTILITIES ====================

    /**
     * Extracts JSON from text that might be wrapped in markdown code blocks.
     */
    private String extractJson(String text) {
        String cleaned = text.trim();

        // Remove markdown code blocks if present
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
}
