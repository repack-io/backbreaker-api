package com.repackio.backbreaker.utils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import lombok.extern.slf4j.Slf4j;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

/**
 * Utility class for handling image EXIF orientation.
 * Automatically rotates images based on EXIF orientation tag to display correctly.
 */
@Slf4j
public class ImageOrientationUtil {

    /**
     * Read EXIF orientation and apply the correct rotation/flip to the image.
     *
     * @param imageBytes The raw image bytes
     * @param image The BufferedImage (already read by ImageIO)
     * @return The correctly oriented image
     */
    public static BufferedImage correctOrientation(byte[] imageBytes, BufferedImage image) {
        try {
            // Extract EXIF metadata from original bytes
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(imageBytes));
            ExifIFD0Directory exifDirectory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

            if (exifDirectory == null) {
                log.debug("No EXIF data found, returning image as-is");
                return image;
            }

            if (!exifDirectory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                log.debug("No EXIF orientation tag found, returning image as-is");
                return image;
            }

            int orientation = exifDirectory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            log.info("EXIF orientation tag: {}", orientation);

            return rotateImage(image, orientation);

        } catch (Exception e) {
            log.warn("Failed to read EXIF orientation, returning image as-is: {}", e.getMessage());
            return image;
        }
    }

    /**
     * Rotate/flip image based on EXIF orientation value.
     *
     * EXIF Orientation values:
     * 1 = Normal (no rotation)
     * 2 = Flip horizontal
     * 3 = Rotate 180°
     * 4 = Flip vertical
     * 5 = Rotate 90° CW + flip horizontal
     * 6 = Rotate 90° CW
     * 7 = Rotate 90° CCW + flip horizontal
     * 8 = Rotate 90° CCW
     */
    private static BufferedImage rotateImage(BufferedImage image, int orientation) {
        AffineTransform transform = new AffineTransform();

        switch (orientation) {
            case 1:
                // Normal - no rotation needed
                log.debug("Orientation 1: No rotation needed");
                return image;

            case 2:
                // Flip horizontal
                log.info("Orientation 2: Flipping horizontal");
                transform.scale(-1.0, 1.0);
                transform.translate(-image.getWidth(), 0);
                break;

            case 3:
                // Rotate 180°
                log.info("Orientation 3: Rotating 180 degrees");
                transform.translate(image.getWidth(), image.getHeight());
                transform.rotate(Math.PI);
                break;

            case 4:
                // Flip vertical
                log.info("Orientation 4: Flipping vertical");
                transform.scale(1.0, -1.0);
                transform.translate(0, -image.getHeight());
                break;

            case 5:
                // Rotate 90° CW + flip horizontal
                log.info("Orientation 5: Rotating 90 CW + flip horizontal");
                transform.rotate(Math.PI / 2);
                transform.scale(-1.0, 1.0);
                break;

            case 6:
                // Rotate 90° CW (most common for portrait photos from phones)
                log.info("Orientation 6: Rotating 90 degrees CW (portrait mode)");
                transform.translate(image.getHeight(), 0);
                transform.rotate(Math.PI / 2);
                break;

            case 7:
                // Rotate 90° CCW + flip horizontal
                log.info("Orientation 7: Rotating 90 CCW + flip horizontal");
                transform.scale(-1.0, 1.0);
                transform.translate(-image.getHeight(), 0);
                transform.rotate(3 * Math.PI / 2);
                break;

            case 8:
                // Rotate 90° CCW
                log.info("Orientation 8: Rotating 90 degrees CCW");
                transform.translate(0, image.getWidth());
                transform.rotate(3 * Math.PI / 2);
                break;

            default:
                log.warn("Unknown EXIF orientation value: {}", orientation);
                return image;
        }

        return applyTransform(image, transform, orientation);
    }

    /**
     * Apply the affine transform to create a new rotated/flipped image.
     */
    private static BufferedImage applyTransform(BufferedImage image, AffineTransform transform, int orientation) {
        // For orientations 5-8, width and height are swapped
        int newWidth = image.getWidth();
        int newHeight = image.getHeight();

        if (orientation >= 5 && orientation <= 8) {
            newWidth = image.getHeight();
            newHeight = image.getWidth();
        }

        BufferedImage rotated = new BufferedImage(newWidth, newHeight, image.getType());
        Graphics2D g = rotated.createGraphics();
        g.setTransform(transform);
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return rotated;
    }
}
