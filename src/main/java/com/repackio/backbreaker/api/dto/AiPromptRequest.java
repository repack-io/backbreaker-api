package com.repackio.backbreaker.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for creating a new AI prompt version.
 */
@Data
public class AiPromptRequest {

    @NotBlank
    private String promptKey;

    @NotBlank
    private String name;

    private String description;

    @NotBlank
    private String promptText;

    private String changeReason;
}
