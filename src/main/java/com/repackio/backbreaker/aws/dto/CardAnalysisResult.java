package com.repackio.backbreaker.aws.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Response from Bedrock vision model analyzing a card image.
 */
@Data
public class CardAnalysisResult {
    @JsonProperty("bounding_box")
    private BoundingBoxDto boundingBox;

    @JsonProperty("rotation_degrees")
    private double rotationDegrees;

    @JsonProperty("confidence")
    private double confidence;

    @JsonProperty("reasoning")
    private String reasoning;

    @Data
    public static class BoundingBoxDto {
        @JsonProperty("left")
        private double left;

        @JsonProperty("top")
        private double top;

        @JsonProperty("width")
        private double width;

        @JsonProperty("height")
        private double height;
    }
}
