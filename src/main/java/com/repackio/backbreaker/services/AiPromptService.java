package com.repackio.backbreaker.services;

import com.repackio.backbreaker.models.AiPrompt;
import com.repackio.backbreaker.repositories.AiPromptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for loading and caching AI prompts from the database.
 * Prompts are cached after first load for performance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiPromptService {

    private final AiPromptRepository aiPromptRepository;

    // Cache for loaded prompts (prompt_key -> prompt_text)
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();

    /**
     * Load a prompt by its key from the database.
     * Returns the active version of the prompt.
     * Prompts are cached after first load.
     *
     * @param promptKey The unique key for the prompt (e.g., "card_crop", "card_details_extraction")
     * @return The prompt text
     * @throws IllegalArgumentException if no active prompt found for the key
     */
    public String loadPrompt(String promptKey) {
        return promptCache.computeIfAbsent(promptKey, key -> {
            log.info("Loading prompt from database: {}", key);
            AiPrompt prompt = aiPromptRepository.findFirstByPromptKeyAndIsActiveTrueOrderByVersionDesc(key)
                    .orElseThrow(() -> new IllegalArgumentException("No active prompt found for key: " + key));

            log.info("Loaded prompt '{}' (version {})", prompt.getName(), prompt.getVersion());
            return prompt.getPromptText();
        });
    }

    /**
     * Clear the cache for a specific prompt key.
     * Useful after updating a prompt in the database.
     *
     * @param promptKey The prompt key to clear from cache
     */
    public void clearCache(String promptKey) {
        promptCache.remove(promptKey);
        log.info("Cleared cache for prompt: {}", promptKey);
    }

    /**
     * Clear all cached prompts.
     * Useful for reloading all prompts after bulk updates.
     */
    public void clearAllCache() {
        promptCache.clear();
        log.info("Cleared all prompt cache");
    }
}
