package com.repackio.backbreaker.api.dto;

public record CardProcessingFailureResponse(Long cardId, String reason) {
}
