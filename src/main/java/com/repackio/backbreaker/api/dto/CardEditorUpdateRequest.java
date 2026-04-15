package com.repackio.backbreaker.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CardEditorUpdateRequest {
    private String playerFirstName;
    private String playerLastName;
    private String teamName;
    private String parallelType;
    private String serialNumber;
    private Integer cardCategoryTypeId;
    private Integer cardStatusId;
    private Long productTierId;
    private LocalDate hitDate;
    private BigDecimal usdValue;
    private Integer cardYear;
    private String usdValueRange;
    private String confidence;
    private String factoid;
}
