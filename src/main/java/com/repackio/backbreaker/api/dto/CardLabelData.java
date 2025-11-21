package com.repackio.backbreaker.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data for a single card label.
 * Contains all information to be displayed on the label and encoded in the QR code.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardLabelData {
    private Long cardDetailId;
    private Long seriesCardId;
    private String playerName;
    private String teamName;
    private Integer cardYear;
    private String parallelType;
    private String serialNumber;
    private String tierName;
}
