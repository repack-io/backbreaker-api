package com.repackio.backbreaker.backbreaker.api.dto;

public class SeriesFinalizeResponse {

    private final Long seriesId;
    private final boolean success;
    private final String message;

    public SeriesFinalizeResponse(Long seriesId, boolean success, String message) {
        this.seriesId = seriesId;
        this.success = success;
        this.message = message;
    }

    public Long getSeriesId() {
        return seriesId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
