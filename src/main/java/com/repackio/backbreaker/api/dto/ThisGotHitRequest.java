package com.repackio.backbreaker.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ThisGotHitRequest {

    @JsonProperty("series_card_id")
    private Long seriesCardId;

}
