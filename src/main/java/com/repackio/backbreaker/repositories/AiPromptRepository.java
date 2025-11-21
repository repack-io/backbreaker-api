package com.repackio.backbreaker.repositories;

import com.repackio.backbreaker.models.AiPrompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiPromptRepository extends JpaRepository<AiPrompt, Integer> {

    /**
     * Find the active prompt by its key.
     * Returns the most recent active version of the prompt.
     */
    Optional<AiPrompt> findFirstByPromptKeyAndIsActiveTrueOrderByVersionDesc(String promptKey);
}
