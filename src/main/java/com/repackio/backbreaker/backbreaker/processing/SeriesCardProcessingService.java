package com.repackio.backbreaker.backbreaker.processing;

import com.repackio.backbreaker.backbreaker.models.ProductSeries;
import com.repackio.backbreaker.backbreaker.models.SeriesCard;
import com.repackio.backbreaker.backbreaker.repositories.ProductSeriesRepository;
import com.repackio.backbreaker.backbreaker.repositories.SeriesCardRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class SeriesCardProcessingService {

    private final ProductSeriesRepository productSeriesRepository;
    private final SeriesCardRepository seriesCardRepository;
    private final List<CardProcessingHandler> handlers;

    public SeriesCardProcessingService(
            ProductSeriesRepository productSeriesRepository,
            SeriesCardRepository seriesCardRepository,
            @Autowired(required = false) List<CardProcessingHandler> handlers) {
        this.productSeriesRepository = productSeriesRepository;
        this.seriesCardRepository = seriesCardRepository;
        this.handlers = handlers;
    }

    @Async
    public void processSeriesAsync(Long seriesId) {
        try {
            productSeriesRepository.findById(seriesId.intValue())
                    .ifPresentOrElse(this::processInternal, () ->
                            log.warn("Series {} not found for processing", seriesId));
        } catch (Exception ex) {
            log.error("Processing job for series {} failed to start", seriesId, ex);
        }
    }

    @Transactional
    protected SeriesProcessingReport processInternal(ProductSeries series) {
        List<SeriesCard> cards = seriesCardRepository
                .findBySeriesIdAndFrontImgUrlIsNotNullAndBackImgUrlIsNotNull(series.getId().longValue());

        if (handlers == null || handlers.isEmpty() || cards.isEmpty()) {
            log.info("Series {} has no cards/handlers to process", series.getId());
            return new SeriesProcessingReport(series.getId().longValue(), cards.size());
        }

        SeriesProcessingReport report = new SeriesProcessingReport(series.getId().longValue(), cards.size());

        for (SeriesCard card : cards) {
            CardProcessingContext context = new CardProcessingContext(series, card);
            try {
                for (CardProcessingHandler handler : handlers) {
                    handler.handle(context);
                }

                seriesCardRepository.save(card);
                report.markSuccess();
            } catch (Exception ex) {
                log.error("Failed to process card {} in series {}: {}", card.getId(), series.getId(), ex.getMessage(), ex);
                report.markFailure(card.getId(), ex.getMessage());
            }
        }

        log.info("Completed processing series {}: {} succeeded, {} failed",
                series.getId(), report.getProcessedCards(), report.getFailures().size());
        return report;
    }

    public List<CardProcessingHandler> getHandlers() {
        return handlers == null ? Collections.emptyList() : Collections.unmodifiableList(handlers);
    }
}
