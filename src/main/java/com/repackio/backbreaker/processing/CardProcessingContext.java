package com.repackio.backbreaker.processing;

import com.repackio.backbreaker.models.ProductSeries;
import com.repackio.backbreaker.models.SeriesCard;
import lombok.Getter;
import lombok.Setter;

import java.awt.image.BufferedImage;

@Getter
public class CardProcessingContext {

    private final ProductSeries series;
    private final SeriesCard card;

    @Setter
    private S3Location frontOriginalLocation;
    @Setter
    private S3Location backOriginalLocation;
    @Setter
    private S3Location frontProcessedLocation;
    @Setter
    private S3Location backProcessedLocation;

    @Setter
    private BufferedImage frontOriginal;
    @Setter
    private BufferedImage backOriginal;
    @Setter
    private BufferedImage frontProcessed;
    @Setter
    private BufferedImage backProcessed;

    public CardProcessingContext(ProductSeries series, SeriesCard card) {
        this.series = series;
        this.card = card;
    }

}
