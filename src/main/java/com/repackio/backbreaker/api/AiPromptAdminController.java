package com.repackio.backbreaker.api;

import com.repackio.backbreaker.api.dto.AiPromptHistoryResponse;
import com.repackio.backbreaker.api.dto.AiPromptRequest;
import com.repackio.backbreaker.api.dto.AiPromptResponse;
import com.repackio.backbreaker.models.AiPrompt;
import com.repackio.backbreaker.models.AiPromptHistory;
import com.repackio.backbreaker.services.AiPromptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/prompts")
@RequiredArgsConstructor
@Slf4j
public class AiPromptAdminController {

    private final AiPromptService aiPromptService;

    @GetMapping
    public List<AiPromptResponse> listPrompts() {
        return aiPromptService.listPrompts()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{promptKey}/history")
    public List<AiPromptHistoryResponse> history(@PathVariable String promptKey) {
        log.info("Fetching prompt history for key={}", promptKey);
        return aiPromptService.getPromptHistory(promptKey)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    @PostMapping
    public ResponseEntity<AiPromptResponse> createPrompt(@Valid @RequestBody AiPromptRequest request) {
        log.info("Creating new prompt version for key={}", request.getPromptKey());
        AiPrompt prompt = aiPromptService.createPromptVersion(
                request.getPromptKey(),
                request.getName(),
                request.getDescription(),
                request.getPromptText(),
                request.getChangeReason()
        );
        return ResponseEntity.ok(toResponse(prompt));
    }

    private AiPromptResponse toResponse(AiPrompt prompt) {
        return new AiPromptResponse(
                prompt.getId(),
                prompt.getPromptKey(),
                prompt.getName(),
                prompt.getDescription(),
                prompt.getPromptText(),
                prompt.getVersion(),
                prompt.getIsActive(),
                prompt.getCreatedAt(),
                prompt.getUpdatedAt()
        );
    }

    private AiPromptHistoryResponse toHistoryResponse(AiPromptHistory history) {
        return new AiPromptHistoryResponse(
                history.getId(),
                history.getPromptId(),
                history.getPromptKey(),
                history.getName(),
                history.getDescription(),
                history.getPromptText(),
                history.getVersion(),
                history.getChangeReason(),
                history.getIsActive(),
                history.getCreatedAt(),
                history.getUpdatedAt(),
                history.getArchivedAt()
        );
    }
}
