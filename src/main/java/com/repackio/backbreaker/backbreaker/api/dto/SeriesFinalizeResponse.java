package com.repackio.backbreaker.backbreaker.api.dto;

import java.util.List;

public class SeriesFinalizeResponse {

    private final Long seriesId;
    private final boolean success;
    private final String message;
    private final int totalCards;
    private final int processedCards;
    private final int failedCards;
    private final List<CardProcessingFailureResponse> failures;
    private final boolean processingStarted;

    public SeriesFinalizeResponse(Long seriesId,
                                  boolean success,
                                  String message,
                                  int totalCards,
                                  int processedCards,
                                  List<CardProcessingFailureResponse> failures,
                                  boolean processingStarted) {
        this.seriesId = seriesId;
        this.success = success;
        this.message = message;
        this.totalCards = totalCards;
        this.processedCards = processedCards;
        this.failures = failures;
        this.failedCards = failures == null ? 0 : failures.size();
        this.processingStarted = processingStarted;
    }

    public Long getSeriesId() {
        return seriesId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public int getTotalCards() {
        return totalCards;
    }

    public int getProcessedCards() {
        return processedCards;
    }

    public int getFailedCards() {
        return failedCards;
    }

    public List<CardProcessingFailureResponse> getFailures() {
        return failures;
    }

    public boolean isProcessingStarted() {
        return processingStarted;
    }
}
