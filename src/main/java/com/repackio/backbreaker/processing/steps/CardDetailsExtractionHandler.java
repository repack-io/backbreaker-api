package com.repackio.backbreaker.processing.steps;

import com.repackio.backbreaker.api.dto.CardDetailsExtractionResponse;
import com.repackio.backbreaker.api.dto.ExtractedCardData;
import com.repackio.backbreaker.aws.services.ImageCropService;
import com.repackio.backbreaker.processing.CardProcessingContext;
import com.repackio.backbreaker.processing.CardProcessingHandler;
import com.repackio.backbreaker.services.CardDetailsExtractionService;
import com.repackio.backbreaker.services.CardFactoidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(30)
@RequiredArgsConstructor
@ConditionalOnBean(ImageCropService.class)
public class CardDetailsExtractionHandler implements CardProcessingHandler {

    private final CardDetailsExtractionService cardDetailsExtractService;
    private final CardFactoidService cardFactoidService;

    @Override
    public void handle(CardProcessingContext context) throws Exception {
        CardDetailsExtractionResponse response = cardDetailsExtractService.extractCardDetails(context.getCard().getId());
        ExtractedCardData extracted = response.getExtractedData();
        try {
            cardFactoidService.generateAndSave(
                    response.getCardDetailId(),
                    extracted.getPlayerFirstName(),
                    extracted.getPlayerLastName(),
                    extracted.getCardCategory()
            );
        } catch (Exception e) {
            log.warn("Factoid generation failed for cardDetailId={}: {}", response.getCardDetailId(), e.getMessage());
        }
    }

}
