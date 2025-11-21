package com.repackio.backbreaker.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Card data extracted from images by Bedrock AI.
 * Matches the structure returned by Claude's vision analysis.
 */
@Data
public class ExtractedCardData {

    @JsonProperty("player_first_name")
    private String playerFirstName;

    @JsonProperty("player_last_name")
    private String playerLastName;

    @JsonProperty("card_category")
    private String cardCategory;

    @JsonProperty("team_name")
    private String teamName;

    @JsonProperty("parallel_type")
    private String parallelType;

    @JsonProperty("serial_number")
    private String serialNumber;

    @JsonProperty("card_year")
    private String cardYear;

    @JsonProperty("usd_value_range")
    private String usdValueRange;

    @JsonProperty("confidence")
    private String confidence; // "high", "medium", "low"
}
