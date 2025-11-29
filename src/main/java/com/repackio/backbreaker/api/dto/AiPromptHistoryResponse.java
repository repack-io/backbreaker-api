package com.repackio.backbreaker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiPromptHistoryResponse {
    private Integer id;
    private Integer promptId;
    private String promptKey;
    private String name;
    private String description;
    private String promptText;
    private Integer version;
    private String changeReason;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime archivedAt;
}
