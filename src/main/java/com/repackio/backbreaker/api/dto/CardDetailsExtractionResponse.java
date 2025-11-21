package com.repackio.backbreaker.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response after extracting and storing card details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardDetailsExtractionResponse {

    @JsonProperty("card_detail_id")
    private Long cardDetailId;

    @JsonProperty("series_card_id")
    private Long seriesCardId;

    @JsonProperty("player_id")
    private Long playerId;

    @JsonProperty("player_name")
    private String playerName;

    @JsonProperty("team_id")
    private Long teamId;

    @JsonProperty("team_name")
    private String teamName;

    @JsonProperty("parallel_type")
    private String parallelType;

    @JsonProperty("serial_number")
    private String serialNumber;

    @JsonProperty("card_status_id")
    private Integer cardStatusId;

    @JsonProperty("hit_date")
    private LocalDate hitDate;

    @JsonProperty("usd_value")
    private BigDecimal usdValue;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("extracted_data")
    private ExtractedCardData extractedData;
}
