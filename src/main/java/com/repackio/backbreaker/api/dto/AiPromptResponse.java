package com.repackio.backbreaker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiPromptResponse {
    private Integer id;
    private String promptKey;
    private String name;
    private String description;
    private String promptText;
    private Integer version;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
