package com.repackio.backbreaker.aws.dto;

import lombok.Data;

@Data
public class CardProcessingMessage {
    private Long seriesId;
    private Long cardId;
    private String frontKey;
    private String backKey;
}
