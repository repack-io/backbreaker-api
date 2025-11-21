package com.repackio.backbreaker.api;

import com.repackio.backbreaker.api.dto.CardDetailsExtractionRequest;
import com.repackio.backbreaker.api.dto.CardDetailsExtractionResponse;
import com.repackio.backbreaker.services.CardDetailsExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

/**
 * REST API for extracting detailed card information using AI vision analysis.
 */
/**
 * TODO: I THINK THIS CLASS IS UNNECESSARY
 */
@Slf4j
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CardDetailsController {

    private final CardDetailsExtractionService extractionService;

    /**
     * Extract card details from front and back images using AI.
     * POST /api/cards/extract-details
     * Request body:
     * {
     *   "series_card_id": 123,
     *   "card_category_type": "baseball"  // Optional, defaults to "baseball"
     * }
     * Response:
     * {
     *   "card_detail_id": 1,
     *   "series_card_id": 123,
     *   "player_id": 5,
     *   "player_name": "Mike Trout",
     *   "team_id": 3,
     *   "team_name": "Los Angeles Angels",
     *   "parallel_type": "Refractor",
     *   "serial_number": "45/99",
     *   "card_status_id": 1,
     *   "extracted_data": {
     *     "player_first_name": "Mike",
     *     "player_last_name": "Trout",
     *     "team_name": "Los Angeles Angels",
     *     "parallel_type": "Refractor",
     *     "serial_number": "45/99",
     *     "card_year": "2023",
     *     "card_set": "Topps Chrome",
     *     "card_number": "27",
     *     "rookie_card": false,
     *     "autograph": false,
     *     "memorabilia": false,
     *     "confidence": "high"
     *   }
     * }
     */
    @PostMapping("/extract-details")
    public ResponseEntity<?> extractCardDetails(@RequestBody CardDetailsExtractionRequest request) {
        try {
            log.info("Received card details extraction request for series_card_id={}", request.getSeriesCardId());

            if (request.getSeriesCardId() == null) {
                return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "series_card_id is required"));
            }

            CardDetailsExtractionResponse response = extractionService.extractCardDetails(
                request.getSeriesCardId()
            );

            log.info("Successfully extracted card details for series_card_id={}, card_detail_id={}",
                request.getSeriesCardId(), response.getCardDetailId());

            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Bad request: {}", e.getMessage());
            return ResponseEntity
                .badRequest()
                .body(Map.of("message", e.getMessage()));

        } catch (IllegalStateException e) {
            log.warn("Conflict: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("message", e.getMessage()));

        } catch (IOException e) {
            log.error("Error extracting card details", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to analyze card images: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error extracting card details", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Internal server error: " + e.getMessage()));
        }
    }
}
