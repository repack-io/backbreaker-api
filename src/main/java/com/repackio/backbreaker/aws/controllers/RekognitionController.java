package com.repackio.backbreaker.aws.controllers;

import com.repackio.backbreaker.aws.services.RekognitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.rekognition.model.CompareFacesResponse;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.DetectTextResponse;

@RestController
@RequestMapping("/api/rekognition")
@RequiredArgsConstructor
public class RekognitionController {

    private final RekognitionService rekognitionService;

    /**
     * Detect labels from an uploaded image
     */
    @PostMapping("/labels")
    public DetectLabelsResponse detectLabels(@RequestParam("file") MultipartFile file) throws Exception {
        return rekognitionService.detectLabels(file.getBytes());
    }

    /**
     * OCR text detection using Rekognition (for basic OCR)
     */
    @PostMapping("/text")
    public DetectTextResponse detectText(@RequestParam("file") MultipartFile file) throws Exception {
        return rekognitionService.detectText(file.getBytes());
    }

    /**
     * Compare two images to see if they contain matching faces
     */
    @PostMapping("/compare")
    public CompareFacesResponse compareFaces(
            @RequestParam("source") MultipartFile source,
            @RequestParam("target") MultipartFile target) throws Exception {

        return rekognitionService.compareFaces(
                source.getBytes(),
                target.getBytes()
        );
    }
}
