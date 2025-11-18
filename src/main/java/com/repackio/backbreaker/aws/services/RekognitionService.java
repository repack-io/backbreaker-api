package com.repackio.backbreaker.aws.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class RekognitionService {

    private final RekognitionClient rekognitionClient;

    /**
     * Detects labels in an uploaded file
     */
    public DetectLabelsResponse detectLabels(byte[] fileBytes) {
        return rekognitionClient.detectLabels(
                DetectLabelsRequest.builder()
                        .maxLabels(10)
                        .minConfidence(70F)
                        .image(Image.builder().bytes(SdkBytes.fromByteArray(fileBytes)).build())
                        .build()
        );
    }

    /**
     * Detect labels from a local file (useful for testing)
     */
    public DetectLabelsResponse detectLabelsFromFile(String filePath) throws IOException {
        byte[] bytes = Files.readAllBytes(Path.of(filePath));
        return detectLabels(bytes);
    }

    /**
     * Detects printed/handwritten text using Rekognition OCR
     */
    public DetectTextResponse detectText(byte[] bytes) {
        return rekognitionClient.detectText(
                DetectTextRequest.builder()
                        .image(Image.builder().bytes(SdkBytes.fromByteArray(bytes)).build())
                        .build()
        );
    }

    /**
     * Compares two faces — returns similarity matches
     */
    public CompareFacesResponse compareFaces(byte[] source, byte[] target) {
        return rekognitionClient.compareFaces(
                CompareFacesRequest.builder()
                        .sourceImage(Image.builder().bytes(SdkBytes.fromByteArray(source)).build())
                        .targetImage(Image.builder().bytes(SdkBytes.fromByteArray(target)).build())
                        .similarityThreshold(80F)
                        .build()
        );
    }

    public DetectLabelsResponse detectLabelsFromS3(String bucket, String key) {
        return rekognitionClient.detectLabels(
                DetectLabelsRequest.builder()
                        .image(Image.builder()
                                .s3Object(S3Object.builder().bucket(bucket).name(key).build())
                                .build()
                        )
                        .build()
        );
    }

}
