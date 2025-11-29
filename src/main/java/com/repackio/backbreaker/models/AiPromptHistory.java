package com.repackio.backbreaker.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Stores archived versions of AI prompts for audit/history.
 */
@Entity
@Table(name = "ai_prompt_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiPromptHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "prompt_id", nullable = false)
    private Integer promptId;

    @Column(name = "prompt_key", nullable = false, length = 100)
    private String promptKey;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "prompt_text", nullable = false, columnDefinition = "TEXT")
    private String promptText;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "archived_at", nullable = false)
    private LocalDateTime archivedAt;
}
