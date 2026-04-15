package com.repackio.backbreaker.services;

import com.repackio.backbreaker.aws.services.BedrockVisionService;
import com.repackio.backbreaker.models.CardFactoid;
import com.repackio.backbreaker.repositories.CardFactoidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardFactoidService {

    private final BedrockVisionService bedrockService;
    private final CardFactoidRepository cardFactoidRepository;
    private final AiPromptService aiPromptService;

    /**
     * Generate a factoid blurb for a player via Bedrock and save it.
     * Called after card detail extraction completes (and its transaction commits).
     */
    @Transactional
    public CardFactoid generateAndSave(Long cardDetailId, String firstName, String lastName, String sport) throws IOException {
        log.info("Generating factoid for cardDetailId={}, player={} {} ({})", cardDetailId, firstName, lastName, sport);

        String promptTemplate = aiPromptService.loadPrompt("card_factoid_generation");
        String fullPrompt = promptTemplate + "\n\n" + firstName + " " + lastName + " (" + sport + ")";

        String factoidText = bedrockService.invokeWithText("card-factoid-generation", fullPrompt, String.class);

        CardFactoid factoid = new CardFactoid();
        factoid.setCardDetailId(cardDetailId);
        factoid.setFactoid(factoidText);

        CardFactoid saved = cardFactoidRepository.save(factoid);
        log.info("Factoid saved with id={}", saved.getId());
        return saved;
    }

    /**
     * Create or update a factoid for a card detail (used by the card editor).
     * Only updates fields that are non-null in the request.
     */
    @Transactional
    public CardFactoid upsert(Long cardDetailId, String factoidText) {
        CardFactoid factoid = cardFactoidRepository.findByCardDetailId(cardDetailId)
                .orElseGet(() -> {
                    CardFactoid f = new CardFactoid();
                    f.setCardDetailId(cardDetailId);
                    return f;
                });

        if (factoidText != null) factoid.setFactoid(factoidText);

        return cardFactoidRepository.save(factoid);
    }
}
