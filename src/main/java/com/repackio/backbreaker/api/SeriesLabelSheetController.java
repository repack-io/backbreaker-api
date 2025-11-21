package com.repackio.backbreaker.api;

import com.repackio.backbreaker.api.dto.CardLabelData;
import com.repackio.backbreaker.models.*;
import com.repackio.backbreaker.repositories.CardDetailRepository;
import com.repackio.backbreaker.repositories.ProductSeriesRepository;
import com.repackio.backbreaker.repositories.SeriesCardRepository;
import com.repackio.backbreaker.services.LabelSheetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for generating Avery 94103 label sheets with QR codes for series cards.
 */
@Slf4j
@RestController
@RequestMapping("/api/series")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SeriesLabelSheetController {

    private final ProductSeriesRepository seriesRepository;
    private final SeriesCardRepository seriesCardRepository;
    private final CardDetailRepository cardDetailRepository;
    private final LabelSheetService labelSheetService;

    /**
     * Generate a label sheet PDF for all cards in a series.
     * GET /api/series/{seriesId}/label-sheet
     *
     * @param seriesId The ID of the product series
     * @return PDF file with Avery 94103 formatted labels
     */
    @GetMapping("/{seriesId}/label-sheet")
    public ResponseEntity<?> generateLabelSheet(@PathVariable Long seriesId) {
        try {
            log.info("Generating label sheet for series_id={}", seriesId);

            // Verify series exists
            ProductSeries series = seriesRepository.findById(seriesId.intValue())
                    .orElseThrow(() -> new IllegalArgumentException("Series not found with id: " + seriesId));

            // Get all series cards for this series
            List<Long> seriesCardIds = seriesCardRepository
                    .findBySeriesIdAndFrontImgUrlIsNotNullAndBackImgUrlIsNotNull(seriesId)
                    .stream()
                    .map(SeriesCard::getId)
                    .toList();

            if (seriesCardIds.isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(Map.of("message", "No cards found for series id: " + seriesId));
            }

            // Get card details with eagerly fetched player and team to avoid LazyInitializationException
            List<CardDetail> cardDetails = cardDetailRepository.findBySeriesCardIdInWithPlayerAndTeam(seriesCardIds);
            List<CardLabelData> labelDataList = cardDetails.stream()
                    .map(this::buildLabelData)
                    .toList();

            if (labelDataList.isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(Map.of("message", "No card details found for series id: " + seriesId));
            }

            log.info("Found {} cards with details for series {}", labelDataList.size(), seriesId);

            // Generate PDF
            byte[] pdfBytes = labelSheetService.generateLabelSheet(labelDataList);

            // Return PDF as downloadable file
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", series.getProduct().getProductName() + "_series_" + seriesId + "_labels.pdf");
            headers.setContentLength(pdfBytes.length);

            log.info("Successfully generated label sheet PDF for series {} with {} labels ({} bytes)",
                    seriesId, labelDataList.size(), pdfBytes.length);

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (IllegalArgumentException e) {
            log.warn("Bad request: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", e.getMessage()));

        } catch (Exception e) {
            log.error("Error generating label sheet for series {}", seriesId, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to generate label sheet: " + e.getMessage()));
        }
    }

    /**
     * Build label data from card detail entity.
     */
    private CardLabelData buildLabelData(CardDetail cardDetail) {
        CardLabelData labelData = new CardLabelData();
        labelData.setCardDetailId(cardDetail.getId());
        labelData.setSeriesCardId(cardDetail.getSeriesCardId());

        // Player info
        Player player = cardDetail.getPlayer();
        if (player != null) {
            labelData.setPlayerName(player.getFullName());
        }

        // Team info
        Team team = cardDetail.getTeam();
        if (team != null) {
            labelData.setTeamName(team.getName());
        }

        // Card details
        labelData.setCardYear(cardDetail.getCardYear());
        labelData.setParallelType(cardDetail.getParallelType());
        labelData.setSerialNumber(cardDetail.getSerialNumber());

        // Tier info (using ID for now since we don't have a tier name table)
        if (cardDetail.getProductTierId() != null) {
            labelData.setTierName("Tier " + cardDetail.getProductTierId());
        }

        return labelData;
    }
}
