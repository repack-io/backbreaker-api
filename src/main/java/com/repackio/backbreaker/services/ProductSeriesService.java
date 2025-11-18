package com.repackio.backbreaker.services;

import com.repackio.backbreaker.models.ProductSeries;
import com.repackio.backbreaker.processing.SeriesCardProcessingService;
import com.repackio.backbreaker.processing.SeriesProcessingReport;
import com.repackio.backbreaker.repositories.ProductSeriesRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductSeriesService {

    private static final Logger log = LoggerFactory.getLogger(ProductSeriesService.class);

    private final ProductSeriesRepository repo;
    private final SeriesCardProcessingService processingService;

    public ProductSeriesService(ProductSeriesRepository repo,
                                SeriesCardProcessingService processingService) {
        this.repo = repo;
        this.processingService = processingService;
    }

    @Transactional
    public SeriesFinalizeResult finalizeSeries(Long id) {
        log.debug("Finalizing series id={}", id);
        ProductSeries series = repo.findById(id.intValue())
                .orElseThrow(() -> new EntityNotFoundException("Series %d not found".formatted(id)));

        int updatedRows = repo.finalizeSeriesById(id);
        boolean finalized = updatedRows > 0;
        boolean processingStarted = false;
        SeriesProcessingReport processingReport = null;

        if (!finalized) {
            log.warn("Finalize request for series id={} did not update any rows", id);
        } else {
            log.info("Series id={} finalized", id);
            processingService.processSeriesAsync(series.getId().longValue());
            processingStarted = true;
        }

        return new SeriesFinalizeResult(series.getId().longValue(), finalized, processingStarted, processingReport);
    }
}
