package com.repackio.backbreaker.processing.steps;

import com.repackio.backbreaker.models.CardProcessingStatus;
import com.repackio.backbreaker.processing.CardProcessingContext;
import com.repackio.backbreaker.processing.CardProcessingHandler;
import com.repackio.backbreaker.repositories.CardProcessingStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
@RequiredArgsConstructor
public class MarkProcessingStatusHandler implements CardProcessingHandler {

    private final CardProcessingStatusRepository statusRepository;

    @Override
    public void handle(CardProcessingContext context) {
        CardProcessingStatus status = statusRepository.findByCode("processing")
                .orElseThrow(() -> new IllegalStateException("Processing status not configured"));
        context.getCard().setProcessingStatus(status);
    }
}
