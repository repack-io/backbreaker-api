package com.repackio.backbreaker.api;

import com.repackio.backbreaker.models.CardDetail;
import com.repackio.backbreaker.repositories.CardDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@RestController
@RequestMapping("/thisjustgothit")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ThisGotHitController {

    private final CardDetailRepository cardDetailRepository;

    @GetMapping
    public ResponseEntity<?> setCardAsHit(@RequestParam("cardid") String encodedCardId) {
        try {
            // Decode the Base64 encoded series_card_id
            String decodedCardId = new String(Base64.getUrlDecoder().decode(encodedCardId), StandardCharsets.UTF_8);
            Long seriesCardId = Long.parseLong(decodedCardId);

            log.info("Decoded cardid: {} -> series_card_id: {}", encodedCardId, seriesCardId);

            // Find CardDetail by series_card_id
            CardDetail cardDetail = cardDetailRepository.findBySeriesCardId(seriesCardId)
                    .orElseThrow(() -> {
                        log.warn("No card detail found for series_card_id: {}", seriesCardId);
                        return new IllegalArgumentException("Card not found for series_card_id: " + seriesCardId);
                    });

            // Mark the card as hit (status 2)
            cardDetail.setCardStatusId(2);
            cardDetailRepository.save(cardDetail);

            log.info("Card detail {} marked as hit for series_card_id: {}", cardDetail.getId(), seriesCardId);

            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {
            log.error("Invalid encoded cardid: {}", encodedCardId, e);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Invalid card ID format");
        } catch (Exception e) {
            log.error("Error processing cardid: {}", encodedCardId, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing request");
        }
    }

}
