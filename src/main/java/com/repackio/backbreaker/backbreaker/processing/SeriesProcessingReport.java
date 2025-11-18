package com.repackio.backbreaker.backbreaker.processing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SeriesProcessingReport {

    private final Long seriesId;
    private final int totalCards;
    private int processedCards = 0;
    private final List<CardProcessingFailure> failures = new ArrayList<>();

    public SeriesProcessingReport(Long seriesId, int totalCards) {
        this.seriesId = seriesId;
        this.totalCards = totalCards;
    }

    public void markSuccess() {
        this.processedCards++;
    }

    public void markFailure(Long cardId, String reason) {
        failures.add(new CardProcessingFailure(cardId, reason));
    }

    public Long getSeriesId() {
        return seriesId;
    }

    public int getTotalCards() {
        return totalCards;
    }

    public int getProcessedCards() {
        return processedCards;
    }

    public List<CardProcessingFailure> getFailures() {
        return Collections.unmodifiableList(failures);
    }
}
