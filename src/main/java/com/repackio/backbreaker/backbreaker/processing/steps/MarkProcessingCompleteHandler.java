package com.repackio.backbreaker.backbreaker.processing.steps;

import com.repackio.backbreaker.backbreaker.models.CardProcessingStatus;
import com.repackio.backbreaker.backbreaker.processing.CardProcessingContext;
import com.repackio.backbreaker.backbreaker.processing.CardProcessingHandler;
import com.repackio.backbreaker.backbreaker.repositories.CardProcessingStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Order(50)
@RequiredArgsConstructor
public class MarkProcessingCompleteHandler implements CardProcessingHandler {

    private final CardProcessingStatusRepository statusRepository;

    @Override
    public void handle(CardProcessingContext context) {
        CardProcessingStatus doneStatus = statusRepository.findByCode("done")
                .orElseThrow(() -> new IllegalStateException("Done status not configured"));

        context.getCard().setProcessingStatus(doneStatus);
        context.getCard().setProcessedAt(Instant.now());
    }
}
