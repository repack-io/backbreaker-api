package com.repackio.backbreaker.api;

import com.repackio.backbreaker.models.CardDetail;
import com.repackio.backbreaker.models.SeriesCard;
import com.repackio.backbreaker.repositories.CardDetailRepository;
import com.repackio.backbreaker.repositories.SeriesCardRepository;
import com.repackio.backbreaker.services.CardTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/thisjustgothit")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ThisGotHitController {

    private final CardDetailRepository cardDetailRepository;
    private final SeriesCardRepository seriesCardRepository;
    private final CardTokenService cardTokenService;

    @GetMapping
    public ResponseEntity<?> setCardAsHit(@RequestParam("cardid") String signedToken) {
        try {
            // Validate the signed token and extract the series_card_id
            Long seriesCardId = cardTokenService.validateAndExtract(signedToken);

            log.info("Validated token -> series_card_id: {}", seriesCardId);

            // Find CardDetail by series_card_id with player eagerly loaded
            CardDetail cardDetail = cardDetailRepository.findBySeriesCardIdWithPlayerAndTeam(seriesCardId)
                    .orElseThrow(() -> {
                        log.warn("No card detail found for series_card_id: {}", seriesCardId);
                        return new IllegalArgumentException("Card not found for series_card_id: " + seriesCardId);
                    });

            // Mark the card as hit (status 2) and set hit date
            cardDetail.setCardStatusId(2);
            cardDetail.setHitDate(LocalDate.now());
            cardDetailRepository.save(cardDetail);

            log.info("Card detail {} marked as hit for series_card_id: {}", cardDetail.getId(), seriesCardId);

            // Fetch the SeriesCard to get the images
            SeriesCard seriesCard = seriesCardRepository.findById(seriesCardId)
                    .orElseThrow(() -> new IllegalArgumentException("SeriesCard not found for id: " + seriesCardId));

            // Get player name
            String playerName = cardDetail.getPlayer() != null
                    ? cardDetail.getPlayer().getFullName()
                    : "Unknown Player";

            // Get value range
            String valueRange = cardDetail.getUsdValueRange() != null
                    ? cardDetail.getUsdValueRange()
                    : "Value not available";

            // Build and return HTML success page
            String html = buildSuccessPage(
                    playerName,
                    seriesCard.getProcessedFrontImgUrl(),
                    seriesCard.getProcessedBackImgUrl(),
                    valueRange
            );

            return ResponseEntity
                    .ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);

        } catch (IllegalArgumentException e) {
            log.error("Invalid or tampered token: {}", signedToken, e);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildErrorPage("Invalid Token", "This link appears to be invalid or has been tampered with."));
        } catch (Exception e) {
            log.error("Error processing token: {}", signedToken, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_HTML)
                    .body(buildErrorPage("Error processing request", e.getMessage()));
        }
    }

    private String buildSuccessPage(String playerName, String frontImgUrl, String backImgUrl, String valueRange) {
        try {
            String template = loadTemplate("templates/card-hit-success.html");
            return template
                    .replace("{{PLAYER_NAME}}", escapeHtml(playerName))
                    .replace("{{FRONT_IMG_URL}}", escapeHtml(frontImgUrl != null ? frontImgUrl : ""))
                    .replace("{{BACK_IMG_URL}}", escapeHtml(backImgUrl != null ? backImgUrl : ""))
                    .replace("{{VALUE_RANGE}}", escapeHtml(valueRange));
        } catch (IOException e) {
            log.error("Failed to load success template", e);
            return buildFallbackSuccessPage(playerName, frontImgUrl, backImgUrl, valueRange);
        }
    }

    private String buildFallbackSuccessPage(String playerName, String frontImgUrl, String backImgUrl, String valueRange) {
        return """
                <!DOCTYPE html>
                <html><head><meta charset="UTF-8"><title>Success</title></head>
                <body style="font-family:sans-serif;text-align:center;padding:40px;">
                    <h1>Card Hit Successfully!</h1>
                    <h2>%s</h2>
                    <p><strong>Estimated Value:</strong> %s</p>
                    <p>Front: <a href="%s">View</a></p>
                    <p>Back: <a href="%s">View</a></p>
                </body></html>
                """.formatted(escapeHtml(playerName),
                             escapeHtml(valueRange),
                             escapeHtml(frontImgUrl != null ? frontImgUrl : ""),
                             escapeHtml(backImgUrl != null ? backImgUrl : ""));
    }

    private String buildErrorPage(String title, String message) {
        try {
            String template = loadTemplate("templates/card-hit-error.html");
            return template
                    .replace("{{ERROR_TITLE}}", escapeHtml(title))
                    .replace("{{ERROR_MESSAGE}}", escapeHtml(message));
        } catch (IOException e) {
            log.error("Failed to load error template", e);
            return buildFallbackErrorPage(title, message);
        }
    }

    private String buildFallbackErrorPage(String title, String message) {
        return """
                <!DOCTYPE html>
                <html><head><meta charset="UTF-8"><title>Error</title></head>
                <body style="font-family:sans-serif;text-align:center;padding:40px;">
                    <h1 style="color:#e53e3e;">%s</h1>
                    <p>%s</p>
                </body></html>
                """.formatted(escapeHtml(title), escapeHtml(message));
    }

    private String loadTemplate(String templatePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(templatePath);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }

}
