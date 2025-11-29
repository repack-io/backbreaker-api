package com.repackio.backbreaker.services;

import com.repackio.backbreaker.models.AiPrompt;
import com.repackio.backbreaker.models.AiPromptHistory;
import com.repackio.backbreaker.repositories.AiPromptRepository;
import com.repackio.backbreaker.repositories.AiPromptHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for loading and caching AI prompts from the database.
 * Prompts are cached after first load for performance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiPromptService {

    private final AiPromptRepository aiPromptRepository;
    private final AiPromptHistoryRepository aiPromptHistoryRepository;

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

    /**
    * Retrieve all prompts ordered by key then version desc.
    */
    public List<AiPrompt> listPrompts() {
        return aiPromptRepository.findAll(
                Sort.by(Sort.Order.asc("promptKey"), Sort.Order.desc("version"))
        );
    }

    /**
     * Create a new prompt version. Archives the previous version into ai_prompt_history
     * and deactivates it before saving the new active version.
     */
    @Transactional
    public AiPrompt createPromptVersion(String promptKey, String name, String description, String promptText, String changeReason) {
        LocalDateTime now = LocalDateTime.now();
        AiPrompt previous = aiPromptRepository.findFirstByPromptKeyOrderByVersionDesc(promptKey).orElse(null);

        if (previous != null) {
            archivePrompt(previous, now, changeReason);
            previous.setIsActive(false);
            previous.setUpdatedAt(now);
            aiPromptRepository.save(previous);
        }

        int nextVersion = previous == null ? 1 : previous.getVersion() + 1;

        AiPrompt newPrompt = new AiPrompt();
        newPrompt.setPromptKey(promptKey);
        newPrompt.setName(name);
        newPrompt.setDescription(description);
        newPrompt.setPromptText(promptText);
        newPrompt.setVersion(nextVersion);
        newPrompt.setIsActive(true);
        newPrompt.setCreatedAt(now);
        newPrompt.setUpdatedAt(now);

        AiPrompt saved = aiPromptRepository.save(newPrompt);
        clearCache(promptKey);
        return saved;
    }

    public List<AiPromptHistory> getPromptHistory(String promptKey) {
        return aiPromptHistoryRepository.findByPromptKeyOrderByVersionDesc(promptKey);
    }

    private void archivePrompt(AiPrompt prompt, LocalDateTime archivedAt, String changeReason) {
        AiPromptHistory history = new AiPromptHistory();
        history.setPromptId(prompt.getId());
        history.setPromptKey(prompt.getPromptKey());
        history.setName(prompt.getName());
        history.setDescription(prompt.getDescription());
        history.setPromptText(prompt.getPromptText());
        history.setVersion(prompt.getVersion());
        history.setChangeReason(changeReason);
        history.setIsActive(prompt.getIsActive());
        history.setCreatedAt(prompt.getCreatedAt());
        history.setUpdatedAt(prompt.getUpdatedAt());
        history.setArchivedAt(archivedAt);

        aiPromptHistoryRepository.save(history);
    }
}
