package com.repackio.backbreaker.backbreaker.processing;

public record CardProcessingFailure(Long cardId, String reason) {
}
