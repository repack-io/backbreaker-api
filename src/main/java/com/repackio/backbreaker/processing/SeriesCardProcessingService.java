package com.repackio.backbreaker.processing;

import com.repackio.backbreaker.models.ProductSeries;
import com.repackio.backbreaker.models.SeriesCard;
import com.repackio.backbreaker.repositories.CardDetailRepository;
import com.repackio.backbreaker.repositories.CardFactoidRepository;
import com.repackio.backbreaker.repositories.ProductSeriesRepository;
import com.repackio.backbreaker.repositories.SeriesCardRepository;
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
    private final CardDetailRepository cardDetailRepository;
    private final CardFactoidRepository cardFactoidRepository;
    private final List<CardProcessingHandler> handlers;

    public SeriesCardProcessingService(
            ProductSeriesRepository productSeriesRepository,
            SeriesCardRepository seriesCardRepository,
            CardDetailRepository cardDetailRepository,
            CardFactoidRepository cardFactoidRepository,
            @Autowired(required = false) List<CardProcessingHandler> handlers) {
        this.productSeriesRepository = productSeriesRepository;
        this.seriesCardRepository = seriesCardRepository;
        this.cardDetailRepository = cardDetailRepository;
        this.cardFactoidRepository = cardFactoidRepository;
        this.handlers = handlers;
    }

    @Async
    public void processSeriesAsync(Long seriesId) {
        try {
            SeriesProcessingReport report = productSeriesRepository.findById(seriesId.intValue())
                    .map(this::processInternal)
                    .orElseGet(() -> {
                        log.warn("Series {} not found for processing", seriesId);
                        return null;
                    });

            if (report != null && !report.getFailures().isEmpty()) {
                log.error(report.toString());
            }
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

    /**
     * Deletes existing card_detail + card_factoid for the given series card and re-runs
     * the full processing pipeline for that single card.
     */
    @Async
    public void reprocessCardAsync(Long seriesCardId) {
        try {
            SeriesCard card = seriesCardRepository.findById(seriesCardId)
                    .orElseThrow(() -> new IllegalArgumentException("SeriesCard not found: " + seriesCardId));

            ProductSeries series = productSeriesRepository.findById(card.getSeriesId().intValue())
                    .orElseThrow(() -> new IllegalArgumentException("Series not found for card: " + seriesCardId));

            // Delete factoid first (FK references card_detail)
            cardDetailRepository.findBySeriesCardId(seriesCardId).ifPresent(cd -> {
                cardFactoidRepository.findByCardDetailId(cd.getId()).ifPresent(cardFactoidRepository::delete);
                cardDetailRepository.delete(cd);
            });
            log.info("Cleared existing detail/factoid for series_card_id={}", seriesCardId);

            CardProcessingContext context = new CardProcessingContext(series, card);
            for (CardProcessingHandler handler : handlers) {
                handler.handle(context);
            }
            seriesCardRepository.save(card);
            log.info("Reprocessed series_card_id={} successfully", seriesCardId);

        } catch (Exception ex) {
            log.error("Failed to reprocess series_card_id={}: {}", seriesCardId, ex.getMessage(), ex);
        }
    }

    public List<CardProcessingHandler> getHandlers() {
        return handlers == null ? Collections.emptyList() : Collections.unmodifiableList(handlers);
    }
}
