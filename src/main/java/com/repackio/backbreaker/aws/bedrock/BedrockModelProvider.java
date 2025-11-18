package com.repackio.backbreaker.aws.bedrock;

import lombok.Getter;

/**
 * Enum representing supported Bedrock model providers and their request/response formats.
 */
@Getter
public enum BedrockModelProvider {

    ANTHROPIC("anthropic", "bedrock-2023-05-31"),
    META("meta", null),
    AMAZON("amazon", null),
    AI21("ai21", null),
    COHERE("cohere", null),
    MISTRAL("mistral", null);

    private final String providerName;
    private final String apiVersion;

    BedrockModelProvider(String providerName, String apiVersion) {
        this.providerName = providerName;
        this.apiVersion = apiVersion;
    }

    /**
     * Determines the provider from a model ID.
     * Example: "us.anthropic.claude-3-5-sonnet-20241022-v2:0" -> ANTHROPIC
     * Example: "us.meta.llama3-2-90b-instruct-v1:0" -> META
     */
    public static BedrockModelProvider fromModelId(String modelId) {
        if (modelId == null || modelId.isEmpty()) {
            throw new IllegalArgumentException("Model ID cannot be null or empty");
        }

        String lowerModelId = modelId.toLowerCase();

        if (lowerModelId.contains("anthropic") || lowerModelId.contains("claude")) {
            return ANTHROPIC;
        } else if (lowerModelId.contains("meta") || lowerModelId.contains("llama")) {
            return META;
        } else if (lowerModelId.contains("amazon") || lowerModelId.contains("titan")) {
            return AMAZON;
        } else if (lowerModelId.contains("ai21") || lowerModelId.contains("jamba")) {
            return AI21;
        } else if (lowerModelId.contains("cohere")) {
            return COHERE;
        } else if (lowerModelId.contains("mistral")) {
            return MISTRAL;
        }

        throw new IllegalArgumentException("Unknown model provider for model ID: " + modelId);
    }
}
