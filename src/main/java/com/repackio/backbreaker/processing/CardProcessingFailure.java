package com.repackio.backbreaker.processing;

public record CardProcessingFailure(Long cardId, String reason) {
}
