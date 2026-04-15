package com.repackio.backbreaker.api;

import com.google.zxing.WriterException;
import com.repackio.backbreaker.api.dto.CardLabelData;
import com.repackio.backbreaker.models.*;
import com.repackio.backbreaker.repositories.CardDetailRepository;
import com.repackio.backbreaker.repositories.CardFactoidRepository;
import com.repackio.backbreaker.repositories.ProductSeriesRepository;
import com.repackio.backbreaker.repositories.SeriesCardRepository;
import jakarta.persistence.EntityManager;
import com.repackio.backbreaker.services.QRCodeService;
import com.repackio.backbreaker.services.SweatCardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SweatCardController {

    private final ProductSeriesRepository seriesRepository;
    private final SeriesCardRepository seriesCardRepository;
    private final CardDetailRepository cardDetailRepository;
    private final CardFactoidRepository cardFactoidRepository;
    private final SweatCardService sweatCardService;
    private final QRCodeService qrCodeService;
    private final EntityManager entityManager;

    /**
     * Generate a sweat card PDF for all cards in a series.
     * GET /api/series/{seriesId}/sweat-cards
     */
    @GetMapping("/api/series/{seriesId}/sweat-cards")
    public ResponseEntity<?> generateSweatCards(@PathVariable Long seriesId) {
        try {
            log.info("Generating sweat cards for series_id={}", seriesId);

            ProductSeries series = seriesRepository.findById(seriesId.intValue())
                    .orElseThrow(() -> new IllegalArgumentException("Series not found: " + seriesId));

            List<Long> seriesCardIds = seriesCardRepository
                    .findBySeriesIdAndFrontImgUrlIsNotNullAndBackImgUrlIsNotNull(seriesId)
                    .stream().map(SeriesCard::getId).toList();

            if (seriesCardIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "No cards found for series id: " + seriesId));
            }

            List<CardDetail> cardDetails = cardDetailRepository.findBySeriesCardIdInWithPlayerAndTeam(seriesCardIds);
            if (cardDetails.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "No card details found for series id: " + seriesId));
            }

            String seriesName = series.getProduct() != null ? series.getProduct().getProductName() : null;
            String breakerName = null;
            if (series.getProduct() != null) {
                Object result = entityManager.createNativeQuery(
                        "SELECT b.display_name FROM breakers b JOIN breaker_products bp ON bp.breaker_id = b.id WHERE bp.id = :productId")
                        .setParameter("productId", series.getProduct().getId())
                        .getResultStream().findFirst().orElse(null);
                if (result != null) breakerName = result.toString();
            }

            List<Long> cardDetailIds = cardDetails.stream().map(CardDetail::getId).toList();
            Map<Long, String> factoidMap = cardFactoidRepository.findByCardDetailIdIn(cardDetailIds)
                    .stream().collect(java.util.stream.Collectors.toMap(CardFactoid::getCardDetailId, f -> f.getFactoid() != null ? f.getFactoid() : ""));

            final String finalBreakerName = breakerName;
            final String finalSeriesName = seriesName;
            List<CardLabelData> labelData = cardDetails.stream()
                    .map(cd -> buildLabelData(cd, finalBreakerName, finalSeriesName, factoidMap.get(cd.getId())))
                    .toList();

            byte[] pdf = sweatCardService.generateSweatCards(labelData);

            String filename = (seriesName != null ? seriesName : "series_" + seriesId)
                    + "_s" + series.getSeriesNum() + "_sweat_cards.pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdf.length);

            log.info("Generated sweat card PDF for series {} ({} cards, {} bytes)", seriesId, labelData.size(), pdf.length);
            return ResponseEntity.ok().headers(headers).body(pdf);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating sweat cards for series {}", seriesId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to generate sweat cards: " + e.getMessage()));
        }
    }

    /**
     * HTML preview of a single sheet with 3 sample sweat cards.
     * GET /sample/sweat-card
     */
    @GetMapping("/sample/sweat-card")
    public ResponseEntity<String> sampleSweatCard() {
        try {
            BufferedImage qrImg = qrCodeService.generateQRCode("https://repacks.io/sample", 200);
            ByteArrayOutputStream qrBaos = new ByteArrayOutputStream();
            ImageIO.write(qrImg, "PNG", qrBaos);
            String qrDataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(qrBaos.toByteArray());

            String html = buildSampleHtml(qrDataUri);
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        } catch (WriterException | IOException e) {
            log.error("Failed to render sample sweat card", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error generating sample.");
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private CardLabelData buildLabelData(CardDetail cd, String breakerName, String seriesName, String factoid) {
        CardLabelData d = new CardLabelData();
        d.setCardDetailId(cd.getId());
        d.setSeriesCardId(cd.getSeriesCardId());
        d.setBreakerName(breakerName);
        d.setSeriesName(seriesName);
        d.setFactoid(factoid);

        if (cd.getPlayer() != null) d.setPlayerName(cd.getPlayer().getFullName());
        if (cd.getTeam()   != null) d.setTeamName(cd.getTeam().getName());

        d.setCardYear(cd.getCardYear());
        d.setParallelType(cd.getParallelType());
        d.setSerialNumber(cd.getSerialNumber());

        if (cd.getProductTierId() != null) d.setTierName("Tier " + cd.getProductTierId());

        return d;
    }

    private String buildSampleHtml(String qrDataUri) {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Sweat Card Sample</title>
<link href="https://fonts.googleapis.com/css2?family=Pacifico&family=Inter:wght@400;700;900&display=swap" rel="stylesheet">
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    background: #e8e8e8;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 32px 16px;
    font-family: 'Inter', sans-serif;
  }
  h2 {
    font-size: 14px;
    color: #666;
    margin-bottom: 20px;
    letter-spacing: 0.05em;
    text-transform: uppercase;
  }
  .sheet {
    background: white;
    width: 11in;
    height: 8.5in;
    display: flex;
    flex-direction: row;
    align-items: stretch;
    padding: 0.25in;
    gap: 0;
    box-shadow: 0 4px 24px rgba(0,0,0,0.18);
  }
  .card {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 18px 14px 14px;
    gap: 0;
  }
  .card + .card {
    border-left: 1px solid #aaa;
  }
  .logo {
    font-family: 'Pacifico', cursive;
    font-size: 30px;
    color: #111;
    margin-bottom: 8px;
    letter-spacing: 0.01em;
  }
  .rule {
    width: 100%;
    height: 1px;
    background: #bbb;
    margin-bottom: 12px;
  }
  .breaker {
    font-size: 11px;
    color: #555;
    margin-bottom: 7px;
  }
  .series {
    font-weight: 700;
    font-size: 14px;
    text-align: center;
    color: #111;
    margin-bottom: 12px;
  }
  .blurb {
    font-size: 10px;
    color: #444;
    text-align: center;
    margin-bottom: 18px;
  }
  .qr {
    margin-top: auto;
    display: flex;
    align-items: center;
    justify-content: center;
    flex: 1;
  }
  .qr img {
    max-width: 220px;
    max-height: 220px;
    width: 100%;
    height: auto;
  }
  @media (max-width: 900px) {
    .sheet { width: 100%; height: auto; flex-direction: column; }
    .card + .card { border-left: none; border-top: 1px solid #aaa; }
  }
</style>
</head>
<body>
<h2>Sweat Card — Sample Preview (3-up Landscape)</h2>
<div class="sheet">

  <div class="card">
    <div class="logo">repacks.io</div>
    <div class="rule"></div>
    <div class="breaker">waxroom</div>
    <div class="series">Bases Loaded</div>
    <div class="blurb">A 14-time All-Star and three-time American League MVP, Mike Trout is widely considered the best player of his generation, combining elite power, speed, and defense in center field.</div>
    <div class="qr"><img src="%QR%" alt="QR Code"></div>
  </div>

  <div class="card">
    <div class="logo">repacks.io</div>
    <div class="rule"></div>
    <div class="breaker">waxroom</div>
    <div class="series">Bases Loaded</div>
    <div class="blurb">Ronald Acuña Jr. became the first player in MLB history to hit 40 home runs and steal 70 bases in a single season, winning the 2023 NL MVP award unanimously.</div>
    <div class="qr"><img src="%QR%" alt="QR Code"></div>
  </div>

  <div class="card">
    <div class="logo">repacks.io</div>
    <div class="rule"></div>
    <div class="breaker">waxroom</div>
    <div class="series">Bases Loaded</div>
    <div class="blurb">Shohei Ohtani is the only player in modern MLB history to be a true two-way star, posting ace-level pitching stats while simultaneously contending for the home run title.</div>
    <div class="qr"><img src="%QR%" alt="QR Code"></div>
  </div>

</div>
</body>
</html>
""".replace("%QR%", qrDataUri);
    }
}
