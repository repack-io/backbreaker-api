package com.repackio.backbreaker.backbreaker.processing.steps;

import com.repackio.backbreaker.backbreaker.aws.services.S3ImageService;
import com.repackio.backbreaker.backbreaker.processing.CardProcessingContext;
import com.repackio.backbreaker.backbreaker.processing.CardProcessingHandler;
import com.repackio.backbreaker.backbreaker.processing.S3Location;
import com.repackio.backbreaker.backbreaker.processing.S3LocationResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
@RequiredArgsConstructor
public class DownloadOriginalImagesHandler implements CardProcessingHandler {

    private final S3ImageService s3ImageService;
    private final S3LocationResolver locationResolver;

    @Value("${aws.s3.uploadsBucket}")
    private String uploadsBucket;

    @Override
    public void handle(CardProcessingContext context) throws Exception {
        S3Location frontLocation = locationResolver.resolve(
                context.getCard().getFrontImgUrl(), uploadsBucket);
        S3Location backLocation = locationResolver.resolve(
                context.getCard().getBackImgUrl(), uploadsBucket);

        context.setFrontOriginalLocation(frontLocation);
        context.setBackOriginalLocation(backLocation);
        context.setFrontOriginal(s3ImageService.downloadImage(frontLocation.bucket(), frontLocation.key()));
        context.setBackOriginal(s3ImageService.downloadImage(backLocation.bucket(), backLocation.key()));
    }
}
