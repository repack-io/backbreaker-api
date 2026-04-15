package com.repackio.backbreaker.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Projection interface for card editor search results.
 * Maps native SQL query columns to typed getters.
 */
public interface CardEditorRow {
    Long getId();
    Long getSeriesCardId();
    String getParallelType();
    String getSerialNumber();
    Integer getCardCategoryTypeId();
    Integer getCardStatusId();
    Long getProductTierId();
    LocalDate getHitDate();
    BigDecimal getUsdValue();
    Integer getCardYear();
    String getUsdValueRange();
    String getConfidence();
    Long getPlayerId();
    String getFirstName();
    String getLastName();
    Long getTeamId();
    String getTeamName();
    String getProductName();
    Integer getSeriesNum();
    Integer getBreakerId();
    String getFrontImgUrl();
    String getBackImgUrl();
    String getProcessedFrontImgUrl();
    String getProcessedBackImgUrl();
    Long getFactoidId();
    String getFactoid();
}
