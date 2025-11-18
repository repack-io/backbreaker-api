package com.repackio.backbreaker.backbreaker.processing.steps;

import com.repackio.backbreaker.backbreaker.aws.services.ImageCropService;
import com.repackio.backbreaker.backbreaker.processing.CardProcessingContext;
import com.repackio.backbreaker.backbreaker.processing.CardProcessingHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(30)
@RequiredArgsConstructor
@ConditionalOnBean(ImageCropService.class)
public class CropCardImagesHandler implements CardProcessingHandler {

    private final ImageCropService imageCropService;

    @Override
    public void handle(CardProcessingContext context) {
        try {
            // cropCard handles both cropping and orientation detection via Bedrock Claude Sonnet
            context.setFrontProcessed(imageCropService.cropCard(context.getFrontOriginal()));
            context.setBackProcessed(imageCropService.cropCard(context.getBackOriginal()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to process card images with Bedrock", e);
        }
    }
}
