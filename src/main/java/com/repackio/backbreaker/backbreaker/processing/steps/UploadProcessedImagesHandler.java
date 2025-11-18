package com.repackio.backbreaker.backbreaker.processing.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repackio.backbreaker.backbreaker.aws.services.S3ImageService;
import com.repackio.backbreaker.backbreaker.processing.CardProcessingContext;
import com.repackio.backbreaker.backbreaker.processing.CardProcessingHandler;
import com.repackio.backbreaker.backbreaker.processing.S3Location;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(40)
@RequiredArgsConstructor
public class UploadProcessedImagesHandler implements CardProcessingHandler {

    private final S3ImageService s3ImageService;
    private final ObjectMapper objectMapper;

    @Value("${aws.s3.processedBucket}")
    private String processedBucket;

    @Override
    public void handle(CardProcessingContext context) throws Exception {
        String baseKey = "processed/series/" + context.getSeries().getId() +
                "/cards/" + context.getCard().getId();

        S3Location frontLocation = new S3Location(processedBucket, baseKey + "/front_cropped.jpg");
        S3Location backLocation = new S3Location(processedBucket, baseKey + "/back_cropped.jpg");

        context.setFrontProcessedLocation(frontLocation);
        context.setBackProcessedLocation(backLocation);

        s3ImageService.uploadImage(frontLocation.bucket(), frontLocation.key(),
                fallbackImage(context.getFrontProcessed(), context.getFrontOriginal()));
        s3ImageService.uploadImage(backLocation.bucket(), backLocation.key(),
                fallbackImage(context.getBackProcessed(), context.getBackOriginal()));

        String frontUrl = "s3://" + frontLocation.bucket() + "/" + frontLocation.key();
        String backUrl = "s3://" + backLocation.bucket() + "/" + backLocation.key();

        context.getCard().setProcessedFrontImgUrl(frontUrl);
        context.getCard().setProcessedBackImgUrl(backUrl);
        context.getCard().setFrontScanResults(objectMapper.createObjectNode()
                .put("processed_image_url", frontUrl)
                .toString());
        context.getCard().setBackScanResults(objectMapper.createObjectNode()
                .put("processed_image_url", backUrl)
                .toString());
    }

    private java.awt.image.BufferedImage fallbackImage(java.awt.image.BufferedImage preferred,
                                                       java.awt.image.BufferedImage fallback) {
        return preferred != null ? preferred : fallback;
    }
}
