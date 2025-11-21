package com.repackio.backbreaker.processing;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SeriesProcessingReport {

    @Getter
    private final Long seriesId;
    @Getter
    private final int totalCards;
    @Getter
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

    public List<CardProcessingFailure> getFailures() {
        return Collections.unmodifiableList(failures);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SeriesProcessingReport {\n");
        sb.append("  seriesId: ").append(seriesId).append(",\n");
        sb.append("  totalCards: ").append(totalCards).append(",\n");
        sb.append("  processedCards: ").append(processedCards).append(",\n");

        sb.append("  failures: [");
        if (failures.isEmpty()) {
            sb.append("]\n");
        } else {
            sb.append("\n");
            for (CardProcessingFailure f : failures) {
                sb.append("    { cardId: ").append(f.cardId())
                        .append(", reason: \"").append(f.reason()).append("\" }\n");
            }
            sb.append("  ]\n");
        }

        sb.append("}");
        return sb.toString();
    }
}
