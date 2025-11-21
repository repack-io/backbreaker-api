package com.repackio.backbreaker.services;

import com.repackio.backbreaker.api.dto.CardDetailsExtractionResponse;
import com.repackio.backbreaker.api.dto.ExtractedCardData;
import com.repackio.backbreaker.aws.services.BedrockVisionService;
import com.repackio.backbreaker.aws.services.S3ImageService;
import com.repackio.backbreaker.models.CardDetail;
import com.repackio.backbreaker.models.Player;
import com.repackio.backbreaker.models.SeriesCard;
import com.repackio.backbreaker.models.Team;
import com.repackio.backbreaker.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;

/**
 * Service for extracting detailed card information using AI vision analysis.
 * Analyzes both front and back card images to extract player, team, and card metadata.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardDetailsExtractionService {

    private final SeriesCardRepository seriesCardRepository;
    private final CardDetailRepository cardDetailRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final BedrockVisionService bedrockService;
    private final S3ImageService s3Service;
    private final CardCategoryRepository cardCategoryRepository;
    private final AiPromptService aiPromptService;

    /**
     * Extract card details from a series card using Bedrock AI vision.
     *
     * @param seriesCardId The ID of the series card to analyze
     * @return Response containing the extracted card details
     * @throws IllegalArgumentException if card not found or invalid category
     * @throws IllegalStateException if card details already exist
     * @throws IOException if image download or Bedrock call fails
     */
    @Transactional
    public CardDetailsExtractionResponse extractCardDetails(Long seriesCardId) throws Exception {
        log.info("Extracting card details for series_card_id={}", seriesCardId);

        // Get the series card
        SeriesCard seriesCard = seriesCardRepository.findById(seriesCardId)
            .orElseThrow(() -> new IllegalArgumentException("Card not found with id: " + seriesCardId));

        // Check if details already exist
        if (cardDetailRepository.existsBySeriesCardId(seriesCardId)) {
            throw new IllegalStateException("Card details already exist for series_card_id: " + seriesCardId);
        }

        // Download card images
        BufferedImage frontImage = downloadImage(seriesCard.getFrontImgUrl());
        BufferedImage backImage = downloadImage(seriesCard.getBackImgUrl());

        // Analyze with Bedrock
        log.info("Analyzing card images with Bedrock AI...");
        ExtractedCardData extractedData = analyzeCardImages(frontImage, backImage);
        log.info("Bedrock analysis complete: player={} {}, team={}, confidence={}",
            extractedData.getPlayerFirstName(), extractedData.getPlayerLastName(),
            extractedData.getTeamName(), extractedData.getConfidence());

        // Validate extracted data
        if (extractedData.getPlayerFirstName() == null
                || extractedData.getPlayerLastName() == null
                || extractedData.getCardCategory() == null) {
            throw new IllegalStateException("Could not extract player name or sport from card images");
        }

        // get the card category id (ai returns a string description, we need numeric id)
        int DEFAULT_CATEGORY_ID = 6;
        Integer cardCategoryId = cardCategoryRepository.findIdByCategory(
                extractedData.getCardCategory()
        ).orElse(DEFAULT_CATEGORY_ID);

        // Get or create player
        Player player = getOrCreatePlayer(
            extractedData.getPlayerFirstName(),
            extractedData.getPlayerLastName(),
                cardCategoryId
        );

        // Get or create team (if team name was extracted)
        Team team = null;
        if (extractedData.getTeamName() != null && !extractedData.getTeamName().isBlank()) {
            team = getOrCreateTeam(extractedData.getTeamName(), cardCategoryId);
        }

        // Create card detail record
        CardDetail cardDetail = new CardDetail();
        cardDetail.setSeriesCardId(seriesCardId);
        cardDetail.setPlayer(player);
        cardDetail.setTeam(team);
        cardDetail.setParallelType(extractedData.getParallelType());
        cardDetail.setSerialNumber(extractedData.getSerialNumber());
        cardDetail.setCardCategoryTypeId(cardCategoryId);
        cardDetail.setCardStatusId(1); // Default: Available
        cardDetail.setProductTierId(seriesCard.getProductTierId());
        cardDetail.setCardYear(Integer.valueOf(extractedData.getCardYear()));
        cardDetail.setUsdValueRange(extractedData.getUsdValueRange());
        cardDetail.setConfidence(extractedData.getConfidence());

        cardDetail = cardDetailRepository.save(cardDetail);
        log.info("Card detail created with id={}", cardDetail.getId());

        // Build response
        return buildResponse(cardDetail, player, team, extractedData);
    }

    /**
     * Analyze card images using Bedrock vision model.
     */
    private ExtractedCardData analyzeCardImages(BufferedImage frontImage, BufferedImage backImage) {
        String prompt = buildAnalysisPrompt();

        BufferedImage[] images = {frontImage, backImage};

        return bedrockService.invokeWithImages(
            "card-details-extraction",  // Use case for model selection
            images,
            prompt,
            ExtractedCardData.class
        );
    }

    /**
     * Build the prompt for Bedrock card analysis by loading from database.
     */
    private String buildAnalysisPrompt() {
        return aiPromptService.loadPrompt("card_details_extraction");
    }

    /**
     * Download an image from a URL.
     */
    private BufferedImage downloadImage(String imageUrl) throws Exception {
        log.debug("Downloading image from: {}", imageUrl);

        // TODO: this will ALWAYS be on S3
        // Check if it's an S3 URL and use S3Service if available
        if (imageUrl.startsWith("s3://")) {
            // Parse S3 URL and download via S3Service
            String[] parts = imageUrl.substring(5).split("/", 2);
            String bucket = parts[0];
            String key = parts[1];
            return s3Service.downloadImage(bucket, key);
        } else {
            // Download via HTTP/HTTPS
            try (InputStream inputStream = new URL(imageUrl).openStream()) {
                return ImageIO.read(inputStream);
            }
        }
    }

    /**
     * Get existing player or create a new one.
     */
    private Player getOrCreatePlayer(String firstName, String lastName, Integer cardCategoryTypeId) {
        return playerRepository.findByFirstNameAndLastNameAndCardCategoryTypeId(firstName, lastName, cardCategoryTypeId)
            .orElseGet(() -> {
                log.info("Creating new player: {} {}", firstName, lastName);
                Player player = new Player();
                player.setFirstName(firstName);
                player.setLastName(lastName);
                player.setCardCategoryTypeId(cardCategoryTypeId);
                return playerRepository.save(player);
            });
    }

    /**
     * Get existing team or create a new one.
     */
    private Team getOrCreateTeam(String teamName, Integer cardCategoryTypeId) {
        return teamRepository.findByNameAndCardCategoryTypeId(teamName, cardCategoryTypeId)
            .orElseGet(() -> {
                log.info("Creating new team: {}", teamName);
                Team team = new Team();
                team.setName(teamName);
                team.setCardCategoryTypeId(cardCategoryTypeId);
                return teamRepository.save(team);
            });
    }

    /**
     * Build the response DTO.
     */
    private CardDetailsExtractionResponse buildResponse(CardDetail cardDetail, Player player, Team team, ExtractedCardData extractedData) {
        CardDetailsExtractionResponse response = new CardDetailsExtractionResponse();
        response.setCardDetailId(cardDetail.getId());
        response.setSeriesCardId(cardDetail.getSeriesCardId());
        response.setPlayerId(player.getId());
        response.setPlayerName(player.getFullName());

        if (team != null) {
            response.setTeamId(team.getId());
            response.setTeamName(team.getName());
        }

        response.setParallelType(cardDetail.getParallelType());
        response.setSerialNumber(cardDetail.getSerialNumber());
        response.setCardStatusId(cardDetail.getCardStatusId());
        response.setHitDate(cardDetail.getHitDate());
        response.setUsdValue(cardDetail.getUsdValue());
        response.setExtractedData(extractedData);

        return response;
    }
}
