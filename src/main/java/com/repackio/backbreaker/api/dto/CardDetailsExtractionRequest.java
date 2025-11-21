package com.repackio.backbreaker.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Request to extract card details from card images using AI.
 */
@Data
public class CardDetailsExtractionRequest {

    @JsonProperty("series_card_id")
    private Long seriesCardId;

    @JsonProperty("card_category_type")
    private String cardCategoryType = "baseball"; // Default to baseball
}
