package com.repackio.backbreaker.backbreaker.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repackio.backbreaker.backbreaker.aws.dto.CardProcessingMessage;
import com.repackio.backbreaker.backbreaker.aws.services.SqsProducerService;
import com.repackio.backbreaker.backbreaker.models.CardProcessingStatus;
import com.repackio.backbreaker.backbreaker.models.SeriesCard;
import com.repackio.backbreaker.backbreaker.repositories.CardProcessingStatusRepository;
import com.repackio.backbreaker.backbreaker.repositories.ProductSeriesRepository;
import com.repackio.backbreaker.backbreaker.repositories.SeriesCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeriesCardService {

    private final SeriesCardRepository seriesCardRepository;
    private final CardProcessingStatusRepository statusRepository;
    private final SqsProducerService sqsProducerService;
    private final ObjectMapper objectMapper;
    private final ProductSeriesRepository repo;

    @Transactional
    public void processAllCardsInSeries(Long seriesId) {

        // Statuses
        CardProcessingStatus queuedStatus = statusRepository.findByCode("queued")
                .orElseThrow(() -> new IllegalStateException("card_processing_status 'queued' not found"));

        List<SeriesCard> cards = seriesCardRepository
                .findBySeriesIdAndFrontImgUrlIsNotNullAndBackImgUrlIsNotNull(seriesId);

        for (SeriesCard card : cards) {
            // set status to queued
            card.setProcessingStatus(queuedStatus);
            seriesCardRepository.save(card);

            CardProcessingMessage msg = buildMessageFromCard(card);
            sqsProducerService.sendCardMessage(msg);
        }

    }

    /**
     * Assumes front/back img URL currently store either:
     *  - a key like "series/12/cards/555/front.jpg"
     *  - or a full S3 URL like "s3://bucket/series/12/..."
     * <p>
     * Adjust extractKeyFromUrl(...) as needed.
     */
    private CardProcessingMessage buildMessageFromCard(SeriesCard card) {
        CardProcessingMessage msg = new CardProcessingMessage();
        msg.setSeriesId(card.getSeriesId());
        msg.setCardId(card.getId());
        msg.setFrontKey(extractKeyFromUrl(card.getFrontImgUrl()));
        msg.setBackKey(extractKeyFromUrl(card.getBackImgUrl()));
        return msg;
    }

    private String extractKeyFromUrl(String urlOrKey) {
        if (urlOrKey == null) return null;

        // If already looks like a bare key (no scheme), return as-is
        if (!urlOrKey.startsWith("s3://") && !urlOrKey.startsWith("http")) {
            return urlOrKey;
        }

        // s3://bucket/key...
        if (urlOrKey.startsWith("s3://")) {
            String withoutScheme = urlOrKey.substring("s3://".length());
            int firstSlash = withoutScheme.indexOf('/');
            if (firstSlash > 0) {
                return withoutScheme.substring(firstSlash + 1);
            }
            return withoutScheme;
        }

        // If it's an https URL from S3/CloudFront, parse and trim leading slash
        try {
            URI uri = URI.create(urlOrKey);
            String path = uri.getPath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            return path;
        } catch (Exception e) {
            // Fallback
            return urlOrKey;
        }
    }
}
