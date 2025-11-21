package com.repackio.backbreaker.processing.steps;

import com.repackio.backbreaker.aws.services.ImageCropService;
import com.repackio.backbreaker.processing.CardProcessingContext;
import com.repackio.backbreaker.processing.CardProcessingHandler;
import com.repackio.backbreaker.services.CardDetailsExtractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(30)
@RequiredArgsConstructor
@ConditionalOnBean(ImageCropService.class)
public class CardDetailsExtractionHandler implements CardProcessingHandler {

    private final CardDetailsExtractionService cardDetailsExtractService;

    @Override
    public void handle(CardProcessingContext context) throws Exception {
        cardDetailsExtractService.extractCardDetails(context.getCard().getId());
    }

}
