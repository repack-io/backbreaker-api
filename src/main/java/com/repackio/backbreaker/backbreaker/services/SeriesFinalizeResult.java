package com.repackio.backbreaker.backbreaker.services;

import com.repackio.backbreaker.backbreaker.processing.SeriesProcessingReport;
import lombok.Getter;

@Getter
public class SeriesFinalizeResult {

    private final Long seriesId;
    private final boolean finalized;
    private final boolean processingStarted;
    private final SeriesProcessingReport processingReport;

    public SeriesFinalizeResult(Long seriesId,
                                boolean finalized,
                                boolean processingStarted,
                                SeriesProcessingReport processingReport) {
        this.seriesId = seriesId;
        this.finalized = finalized;
        this.processingStarted = processingStarted;
        this.processingReport = processingReport;
    }

}
