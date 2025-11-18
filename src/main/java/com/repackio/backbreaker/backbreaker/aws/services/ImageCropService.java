package com.repackio.backbreaker.backbreaker.aws.services;

import com.repackio.backbreaker.backbreaker.aws.dto.CardAnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageCropService {

    private final BedrockVisionService bedrockVisionService;

    @Value("${bedrock.confidence.threshold:70.0}")
    private double confidenceThreshold;

    @Value("${bedrock.crop.padding:10}")
    private int cropPaddingPercent;

    private static final double CARD_ASPECT_RATIO = 3.5 / 2.5; // Standard trading card aspect ratio
    private static final int OUTPUT_WIDTH = 500;
    private static final int OUTPUT_HEIGHT = (int) (OUTPUT_WIDTH * CARD_ASPECT_RATIO);

    /**
     * Main method to crop and process a card image.
     * Uses AWS Bedrock's Claude Sonnet vision model to detect the card and determine orientation.
     *
     * @param original The original BufferedImage
     * @return The cropped and oriented BufferedImage
     */
    public BufferedImage cropCard(BufferedImage original) throws IOException {
        log.info("Processing image with Bedrock: {}x{}", original.getWidth(), original.getHeight());

        try {
            // Use Bedrock to analyze the card
            CardAnalysisResult analysis = bedrockVisionService.analyzeCardImage(original);

            // Check confidence threshold
            if (analysis.getConfidence() < confidenceThreshold) {
                log.warn("Bedrock confidence {}% below threshold {}%, applying fallback crop",
                        analysis.getConfidence(), confidenceThreshold);
                return processFallback(original);
            }

            log.info("Bedrock analysis successful: confidence={}%, rotation={} degrees",
                    analysis.getConfidence(), analysis.getRotationDegrees());
            log.info("Reasoning: {}", analysis.getReasoning());

            // First, rotate the image to correct orientation
            BufferedImage rotated = rotateImage(original, analysis.getRotationDegrees());

            // Then crop based on the bounding box
            BufferedImage cropped = cropWithBoundingBox(rotated, analysis.getBoundingBox());

            // Resize to standard dimensions
            return resizeToStandard(cropped);

        } catch (Exception e) {
            log.error("Bedrock analysis failed: {}", e.getMessage(), e);
            log.warn("Falling back to simple crop");
            return processFallback(original);
        }
    }

    /**
     * Fallback processing when Bedrock fails or confidence is low.
     */
    private BufferedImage processFallback(BufferedImage original) {
        BufferedImage cropped = applyCenterCrop(original);
        BufferedImage rotated = rotateIfNeeded(cropped);
        return resizeToStandard(rotated);
    }

    /**
     * Crops the image based on the bounding box from Bedrock analysis.
     * Bounding box coordinates are relative (0-1), so we convert to pixels.
     */
    private BufferedImage cropWithBoundingBox(BufferedImage image, CardAnalysisResult.BoundingBoxDto box) {
        int imgWidth = image.getWidth();
        int imgHeight = image.getHeight();

        // Convert relative coordinates to pixel coordinates
        int x = (int) (box.getLeft() * imgWidth);
        int y = (int) (box.getTop() * imgHeight);
        int width = (int) (box.getWidth() * imgWidth);
        int height = (int) (box.getHeight() * imgHeight);

        // Add padding around the detected object to avoid over-cropping
        int hPadding = (int) (width * cropPaddingPercent / 100.0);
        int vPadding = (int) (height * cropPaddingPercent / 100.0);

        x = Math.max(0, x - hPadding);
        y = Math.max(0, y - vPadding);
        width = Math.min(imgWidth - x, width + 2 * hPadding);
        height = Math.min(imgHeight - y, height + 2 * vPadding);

        log.info("Cropping to: x={}, y={}, width={}, height={} (with {}% padding)",
                x, y, width, height, cropPaddingPercent);

        return image.getSubimage(x, y, width, height);
    }

    /**
     * Applies a simple center crop when Bedrock analysis fails.
     */
    private BufferedImage applyCenterCrop(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();

        // Crop 5% from each edge (very conservative)
        int cropPercent = 5;
        int cropX = (int) (width * cropPercent / 100.0);
        int cropY = (int) (height * cropPercent / 100.0);
        int cropWidth = width - 2 * cropX;
        int cropHeight = height - 2 * cropY;

        if (cropWidth > 0 && cropHeight > 0) {
            log.info("Applying fallback center crop: {}x{} ({}% from edges)", cropWidth, cropHeight, cropPercent);
            return img.getSubimage(cropX, cropY, cropWidth, cropHeight);
        }

        return img;
    }

    /**
     * Simple rotation check based on aspect ratio (fallback only).
     * Rotates landscape images to portrait since most cards are portrait.
     */
    private BufferedImage rotateIfNeeded(BufferedImage img) {
        if (img.getWidth() > img.getHeight()) {
            log.info("Fallback: Rotating landscape image to portrait (width={} > height={})",
                    img.getWidth(), img.getHeight());
            return rotateImage(img, 90);
        }
        return img;
    }

    /**
     * Rotates an image by the specified degrees (any angle 0-360).
     * For angles close to 90-degree multiples, uses optimized rotation.
     * For arbitrary angles, uses general affine transform with proper bounds calculation.
     */
    private BufferedImage rotateImage(BufferedImage image, double degrees) {
        if (Math.abs(degrees) < 0.1 || Math.abs(degrees - 360) < 0.1) {
            log.info("No rotation needed ({}°)", degrees);
            return image;
        }

        // Normalize degrees to 0-360 range
        degrees = degrees % 360;
        if (degrees < 0) {
            degrees += 360;
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // Check if it's close to a 90-degree multiple for optimized rotation
        if (isCloseToAngle(degrees, 90)) {
            return rotateBy90Degrees(image, 90);
        } else if (isCloseToAngle(degrees, 180)) {
            return rotateBy90Degrees(image, 180);
        } else if (isCloseToAngle(degrees, 270)) {
            return rotateBy90Degrees(image, 270);
        }

        // General rotation for arbitrary angles
        log.info("Rotating image by {} degrees (arbitrary angle)", degrees);
        return rotateByArbitraryAngle(image, degrees);
    }

    /**
     * Checks if an angle is within 1 degree of a target angle.
     */
    private boolean isCloseToAngle(double angle, double target) {
        return Math.abs(angle - target) < 1.0;
    }

    /**
     * Optimized rotation for exact 90-degree multiples.
     */
    private BufferedImage rotateBy90Degrees(BufferedImage image, int degrees) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage rotated;
        AffineTransform transform;

        if (degrees == 90) {
            rotated = new BufferedImage(height, width, image.getType());
            transform = new AffineTransform();
            transform.translate(height / 2.0, width / 2.0);
            transform.rotate(Math.toRadians(90));
            transform.translate(-width / 2.0, -height / 2.0);
        } else if (degrees == 180) {
            rotated = new BufferedImage(width, height, image.getType());
            transform = new AffineTransform();
            transform.translate(width / 2.0, height / 2.0);
            transform.rotate(Math.toRadians(180));
            transform.translate(-width / 2.0, -height / 2.0);
        } else if (degrees == 270) {
            rotated = new BufferedImage(height, width, image.getType());
            transform = new AffineTransform();
            transform.translate(height / 2.0, width / 2.0);
            transform.rotate(Math.toRadians(270));
            transform.translate(-width / 2.0, -height / 2.0);
        } else {
            log.warn("Invalid 90-degree multiple: {}. Returning original image.", degrees);
            return image;
        }

        Graphics2D g2d = rotated.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setTransform(transform);
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        log.info("Rotated image {} degrees (optimized)", degrees);
        return rotated;
    }

    /**
     * Rotates an image by an arbitrary angle.
     * Calculates new bounds to fit the rotated image without cropping.
     */
    private BufferedImage rotateByArbitraryAngle(BufferedImage image, double degrees) {
        int width = image.getWidth();
        int height = image.getHeight();
        double radians = Math.toRadians(degrees);

        // Calculate new bounds after rotation
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));
        int newWidth = (int) Math.ceil(width * cos + height * sin);
        int newHeight = (int) Math.ceil(height * cos + width * sin);

        BufferedImage rotated = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = rotated.createGraphics();

        // Set high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Create transform: translate to center, rotate, translate back
        AffineTransform transform = new AffineTransform();
        transform.translate(newWidth / 2.0, newHeight / 2.0);
        transform.rotate(radians);
        transform.translate(-width / 2.0, -height / 2.0);

        g2d.setTransform(transform);
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        log.info("Rotated image by {} degrees", degrees);
        return rotated;
    }

    /**
     * Resizes the image to standard card dimensions.
     */
    private BufferedImage resizeToStandard(BufferedImage image) {
        // Determine if the card should be portrait or landscape
        boolean shouldBePortrait = image.getHeight() >= image.getWidth();

        int targetWidth, targetHeight;
        if (shouldBePortrait) {
            targetWidth = OUTPUT_WIDTH;
            targetHeight = OUTPUT_HEIGHT;
        } else {
            // Landscape card
            targetWidth = OUTPUT_HEIGHT;
            targetHeight = OUTPUT_WIDTH;
        }

        log.info("Resizing image to standard dimensions: {}x{}", targetWidth, targetHeight);

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();

        // Use high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return resized;
    }
}
