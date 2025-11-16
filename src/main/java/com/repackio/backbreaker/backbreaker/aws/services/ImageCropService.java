package com.repackio.backbreaker.backbreaker.aws.services;

import lombok.RequiredArgsConstructor;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "opencv.enabled", havingValue = "true")
public class ImageCropService {

    static {
        nu.pattern.OpenCV.loadLocally();
    }

    // Convert BufferedImage → Mat
    private Mat bufferedImageToMat(BufferedImage bi) {
        int width = bi.getWidth();
        int height = bi.getHeight();
        int channels = 3; // RGB

        byte[] data = new byte[width * height * channels];

        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = bi.getRGB(x, y);
                data[index++] = (byte) ((rgb >> 16) & 0xFF);
                data[index++] = (byte) ((rgb >> 8) & 0xFF);
                data[index++] = (byte) (rgb & 0xFF);
            }
        }

        Mat mat = new Mat(height, width, CvType.CV_8UC3);
        mat.put(0, 0, data);
        return mat;
    }

    // Mat → BufferedImage
    private BufferedImage matToBufferedImage(Mat mat) {
        int width = mat.width();
        int height = mat.height();
        byte[] data = new byte[width * height * (int) mat.elemSize()];
        mat.get(0, 0, data);

        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        bi.getRaster().setDataElements(0, 0, width, height, data);
        return bi;
    }

    public BufferedImage cropCard(BufferedImage original) {

        Mat src = bufferedImageToMat(original);
        Mat gray = new Mat();
        Mat edges = new Mat();

        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);

        Imgproc.Canny(gray, edges, 50, 150);

        java.util.List<MatOfPoint> contours = new java.util.ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Find the biggest contour → assume that's the card
        double maxArea = 0;
        MatOfPoint biggest = null;

        for (MatOfPoint c : contours) {
            double area = Imgproc.contourArea(c);
            if (area > maxArea) {
                maxArea = area;
                biggest = c;
            }
        }

        if (biggest == null) {
            return original;
        }

        Rect rect = Imgproc.boundingRect(biggest);

        Mat cropped = new Mat(src, rect);
        return matToBufferedImage(cropped);
    }

    public BufferedImage rotateIfNeeded(BufferedImage img) {
        // Very simple heuristic:
        // If height < width, rotate 90 degrees.
        if (img.getHeight() < img.getWidth()) {
            return rotate(img, 90);
        }
        return img;
    }

    private BufferedImage rotate(BufferedImage img, int degrees) {
        int w = img.getWidth();
        int h = img.getHeight();

        BufferedImage rotated = new BufferedImage(h, w, img.getType());
        Graphics2D g = rotated.createGraphics();
        g.rotate(Math.toRadians(degrees), h / 2.0, h / 2.0);
        g.drawImage(img, 0, -((h - w) / 2), null);
        g.dispose();
        return rotated;
    }
}
