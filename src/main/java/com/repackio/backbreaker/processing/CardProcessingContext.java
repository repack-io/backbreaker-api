package com.repackio.backbreaker.processing;

import com.repackio.backbreaker.models.ProductSeries;
import com.repackio.backbreaker.models.SeriesCard;

import java.awt.image.BufferedImage;

public class CardProcessingContext {

    private final ProductSeries series;
    private final SeriesCard card;

    private S3Location frontOriginalLocation;
    private S3Location backOriginalLocation;
    private S3Location frontProcessedLocation;
    private S3Location backProcessedLocation;

    private BufferedImage frontOriginal;
    private BufferedImage backOriginal;
    private BufferedImage frontProcessed;
    private BufferedImage backProcessed;

    public CardProcessingContext(ProductSeries series, SeriesCard card) {
        this.series = series;
        this.card = card;
    }

    public ProductSeries getSeries() {
        return series;
    }

    public SeriesCard getCard() {
        return card;
    }

    public S3Location getFrontOriginalLocation() {
        return frontOriginalLocation;
    }

    public void setFrontOriginalLocation(S3Location frontOriginalLocation) {
        this.frontOriginalLocation = frontOriginalLocation;
    }

    public S3Location getBackOriginalLocation() {
        return backOriginalLocation;
    }

    public void setBackOriginalLocation(S3Location backOriginalLocation) {
        this.backOriginalLocation = backOriginalLocation;
    }

    public S3Location getFrontProcessedLocation() {
        return frontProcessedLocation;
    }

    public void setFrontProcessedLocation(S3Location frontProcessedLocation) {
        this.frontProcessedLocation = frontProcessedLocation;
    }

    public S3Location getBackProcessedLocation() {
        return backProcessedLocation;
    }

    public void setBackProcessedLocation(S3Location backProcessedLocation) {
        this.backProcessedLocation = backProcessedLocation;
    }

    public BufferedImage getFrontOriginal() {
        return frontOriginal;
    }

    public void setFrontOriginal(BufferedImage frontOriginal) {
        this.frontOriginal = frontOriginal;
    }

    public BufferedImage getBackOriginal() {
        return backOriginal;
    }

    public void setBackOriginal(BufferedImage backOriginal) {
        this.backOriginal = backOriginal;
    }

    public BufferedImage getFrontProcessed() {
        return frontProcessed;
    }

    public void setFrontProcessed(BufferedImage frontProcessed) {
        this.frontProcessed = frontProcessed;
    }

    public BufferedImage getBackProcessed() {
        return backProcessed;
    }

    public void setBackProcessed(BufferedImage backProcessed) {
        this.backProcessed = backProcessed;
    }
}
