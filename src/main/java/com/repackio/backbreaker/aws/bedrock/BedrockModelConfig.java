package com.repackio.backbreaker.aws.bedrock;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for Bedrock models.
 * Allows configuring different models for different use cases.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "bedrock")
public class BedrockModelConfig {

    /**
     * Default model ID to use when no specific model is configured.
     */
    private String defaultModelId = "us.anthropic.claude-3-5-sonnet-20241022-v2:0";

    /**
     * Default max tokens for model responses.
     */
    private int maxTokens = 1024;

    /**
     * Default temperature for model responses (0.0 - 1.0).
     */
    private double temperature = 0.1;

    /**
     * Path to prompt files.
     */
    private String promptsPath = "file:prompts/";

    /**
     * Model-specific configurations.
     * Key is the use case (e.g., "card-analysis", "text-generation")
     * Value is the model configuration for that use case.
     */
    private Map<String, ModelSettings> models = new HashMap<>();

    /**
     * Named model presets for easy switching.
     * Examples: "claude-sonnet", "llama3", "titan"
     */
    private Map<String, String> presets = new HashMap<>();

    /**
     * Gets the model ID for a specific use case, falling back to default.
     */
    public String getModelIdForUseCase(String useCase) {
        ModelSettings settings = models.get(useCase);
        if (settings != null && settings.getModelId() != null) {
            return settings.getModelId();
        }
        return defaultModelId;
    }

    /**
     * Gets the model settings for a specific use case, with defaults.
     */
    public ModelSettings getSettingsForUseCase(String useCase) {
        ModelSettings settings = models.get(useCase);
        if (settings == null) {
            settings = new ModelSettings();
            settings.setModelId(defaultModelId);
            settings.setMaxTokens(maxTokens);
            settings.setTemperature(temperature);
        } else {
            // Fill in defaults for missing values
            if (settings.getModelId() == null) {
                settings.setModelId(defaultModelId);
            }
            if (settings.getMaxTokens() == null) {
                settings.setMaxTokens(maxTokens);
            }
            if (settings.getTemperature() == null) {
                settings.setTemperature(temperature);
            }
        }
        return settings;
    }

    /**
     * Resolves a preset name to a model ID.
     * If the input is already a model ID (contains dots), returns it as-is.
     */
    public String resolveModelId(String modelIdOrPreset) {
        if (modelIdOrPreset == null) {
            return defaultModelId;
        }

        // If it looks like a full model ID (contains dots), use it directly
        if (modelIdOrPreset.contains(".")) {
            return modelIdOrPreset;
        }

        // Otherwise, try to resolve it as a preset
        return presets.getOrDefault(modelIdOrPreset, defaultModelId);
    }

    @Data
    public static class ModelSettings {
        private String modelId;
        private Integer maxTokens;
        private Double temperature;
    }
}
