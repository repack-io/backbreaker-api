package com.repackio.backbreaker.aws.services;

import com.repackio.backbreaker.aws.dto.CardAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageCropService {

    private final BedrockVisionService bedrockVisionService;

    @Value("${bedrock.confidence.threshold:70.0}")
    private double confidenceThreshold;

    @Value("${bedrock.crop.padding:5}")
    private int cropPaddingPercent;

    // Target size for output (width for portrait cards)
    private static final int TARGET_SIZE = 500;

    /**
     * Main method to crop a card image (NO ROTATION).
     *
     * @param original The original BufferedImage
     * @return The cropped and resized BufferedImage (SAME ORIENTATION as input)
     */
    public BufferedImage cropCard(BufferedImage original) throws IOException {
        log.info("Processing image with Bedrock: {}x{}", original.getWidth(), original.getHeight());

        try {
            // Use Bedrock to analyze the card
            CardAnalysisResult analysis = bedrockVisionService.analyzeCardImage(original);

            // Log what we got back
            log.info("Bedrock returned - confidence: {}%, reasoning: {}",
                    analysis.getConfidence(),
                    analysis.getReasoning());

            CardAnalysisResult.BoundingBoxDto box = analysis.getBoundingBox();
            log.info("Bounding box: left={}, top={}, width={}, height={}",
                    box.getLeft(), box.getTop(), box.getWidth(), box.getHeight());

            // Calculate actual margins for debugging
            double rightMargin = 1.0 - (box.getLeft() + box.getWidth());
            double bottomMargin = 1.0 - (box.getTop() + box.getHeight());
            log.info("Margins - left: {:.1f}%, right: {:.1f}%, top: {:.1f}%, bottom: {:.1f}%",
                    box.getLeft() * 100, rightMargin * 100,
                    box.getTop() * 100, bottomMargin * 100);

            log.info("Reasoning: {}", analysis.getReasoning());

            // Check confidence threshold
            if (analysis.getConfidence() < confidenceThreshold) {
                log.warn("Bedrock confidence {}% below threshold {}%, applying fallback crop",
                        analysis.getConfidence(), confidenceThreshold);
                return processFallback(original);
            }

            // Validate bounding box
            if (!isValidBoundingBox(box)) {
                log.warn("Invalid bounding box detected, using fallback");
                return processFallback(original);
            }

            // Crop based on the bounding box
            BufferedImage cropped = cropWithBoundingBox(original, box);

            log.info("Cropped result: {}x{} (original was {}x{})",
                    cropped.getWidth(), cropped.getHeight(),
                    original.getWidth(), original.getHeight());

            // Resize maintaining aspect ratio AND ORIENTATION
            BufferedImage resized = resizePreservingAspectRatio(cropped);

            log.info("Final output: {}x{}", resized.getWidth(), resized.getHeight());

            return resized;

        } catch (Exception e) {
            log.error("Bedrock analysis failed: {}", e.getMessage(), e);
            log.warn("Falling back to simple crop");
            return processFallback(original);
        }
    }

    /**
     * Validates that bounding box coordinates are reasonable.
     */
    private boolean isValidBoundingBox(CardAnalysisResult.BoundingBoxDto box) {
        // Check that values are in valid range
        if (box.getLeft() < 0 || box.getLeft() > 0.5) {
            log.warn("Invalid left value: {}", box.getLeft());
            return false;
        }
        if (box.getTop() < 0 || box.getTop() > 0.5) {
            log.warn("Invalid top value: {}", box.getTop());
            return false;
        }
        if (box.getWidth() < 0.5 || box.getWidth() > 1.0) {
            log.warn("Invalid width value: {}", box.getWidth());
            return false;
        }
        if (box.getHeight() < 0.5 || box.getHeight() > 1.0) {
            log.warn("Invalid height value: {}", box.getHeight());
            return false;
        }

        // Check that box doesn't exceed image bounds
        if (box.getLeft() + box.getWidth() > 1.0) {
            log.warn("Bounding box exceeds right edge: left={}, width={}", box.getLeft(), box.getWidth());
            return false;
        }
        if (box.getTop() + box.getHeight() > 1.0) {
            log.warn("Bounding box exceeds bottom edge: top={}, height={}", box.getTop(), box.getHeight());
            return false;
        }

        return true;
    }

    /**
     * Fallback processing when Bedrock fails or confidence is low.
     */
    private BufferedImage processFallback(BufferedImage original) {
        log.info("Using fallback crop method");
        BufferedImage cropped = applyCenterCrop(original);
        return resizePreservingAspectRatio(cropped);
    }

    /**
     * Crops the image based on the bounding box from Bedrock analysis.
     */
    private BufferedImage cropWithBoundingBox(BufferedImage image, CardAnalysisResult.BoundingBoxDto box) {
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();

        // Convert relative coordinates to pixel coordinates
        int x = (int) Math.round(box.getLeft() * imgWidth);
        int y = (int) Math.round(box.getTop() * imgHeight);
        int width = (int) Math.round(box.getWidth() * imgWidth);
        int height = (int) Math.round(box.getHeight() * imgHeight);

        log.info("Pixel coordinates before padding - x={}, y={}, width={}, height={}", x, y, width, height);

        // Add padding (can be 0 to disable)
        if (cropPaddingPercent > 0) {
            int hPadding = (int) Math.round(width * cropPaddingPercent / 100.0);
            int vPadding = (int) Math.round(height * cropPaddingPercent / 100.0);

            x = Math.max(0, x - hPadding);
            y = Math.max(0, y - vPadding);
            width = Math.min(imgWidth - x, width + 2 * hPadding);
            height = Math.min(imgHeight - y, height + 2 * vPadding);

            log.info("After {}% padding - x={}, y={}, width={}, height={}",
                    cropPaddingPercent, x, y, width, height);
        }

        // Validate crop dimensions
        if (x < 0 || y < 0 || width <= 0 || height <= 0 ||
                x + width > imgWidth || y + height > imgHeight) {
            log.error("Invalid crop dimensions, using fallback. x={}, y={}, w={}, h={}, imgW={}, imgH={}",
                    x, y, width, height, imgWidth, imgHeight);
            return applyCenterCrop(image);
        }

        double cropPercentage = (width * height * 100.0) / (imgWidth * imgHeight);
        log.info("Cropping to: x={}, y={}, width={}, height={} ({:.1f}% of original)",
                x, y, width, height, cropPercentage);

        return image.getSubimage(x, y, width, height);
    }

    /**
     * Applies a simple center crop when Bedrock analysis fails.
     */
    private BufferedImage applyCenterCrop(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();

        // Crop 10% from each edge
        int cropPercent = 10;
        int cropX = (int) (width * cropPercent / 100.0);
        int cropY = (int) (height * cropPercent / 100.0);
        int cropWidth = width - 2 * cropX;
        int cropHeight = height - 2 * cropY;

        if (cropWidth > 0 && cropHeight > 0) {
            log.info("Fallback center crop: {}x{} ({}% from edges)",
                    cropWidth, cropHeight, cropPercent);
            return img.getSubimage(cropX, cropY, cropWidth, cropHeight);
        }

        log.warn("Fallback crop failed, returning original");
        return img;
    }

    /**
     * Resizes image to target size while preserving aspect ratio.
     * The longest dimension will be TARGET_SIZE pixels.
     */
    private BufferedImage resizePreservingAspectRatio(BufferedImage image) {
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();

        log.info("Resizing from {}x{}", originalWidth, originalHeight);

        // Calculate dimensions to fit within TARGET_SIZE while preserving aspect ratio
        int targetWidth, targetHeight;

        if (originalWidth > originalHeight) {
            // Landscape - constrain width
            targetWidth = TARGET_SIZE;
            targetHeight = (int) Math.round((double) TARGET_SIZE * originalHeight / originalWidth);
        } else {
            // Portrait or square - constrain height
            targetHeight = TARGET_SIZE;
            targetWidth = (int) Math.round((double) TARGET_SIZE * originalWidth / originalHeight);
        }

        log.info("Target dimensions: {}x{} (aspect ratio preserved)", targetWidth, targetHeight);

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();

        // High-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return resized;
    }
}