package com.repackio.backbreaker.aws.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repackio.backbreaker.aws.dto.CardProcessingMessage;
import com.repackio.backbreaker.models.CardProcessingStatus;
import com.repackio.backbreaker.repositories.CardProcessingStatusRepository;
import com.repackio.backbreaker.repositories.SeriesCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.awt.image.BufferedImage;
import java.time.Instant;

/**
 * TODO: I THINK THIS CLASS IS UNNECESSARY
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "aws.sqs.enabled", havingValue = "true", matchIfMissing = true)
public class CardProcessingWorker {

    private final SqsClient sqsClient;
    private final S3ImageService s3ImageService;
    private final ImageCropService imageCropService;
    private final SeriesCardRepository seriesCardRepository;
    private final ObjectMapper objectMapper;
    private final CardProcessingStatusRepository statusRepo;

    @Value("${aws.s3.uploadsBucket}")
    private String uploadsBucket;

    @Value("${aws.s3.processedBucket}")
    private String processedBucket;

    @Value("${aws.sqs.queueUrl}")
    private String queueUrl;

    @Value("${aws.region}")
    private String awsRegion;

    //@Scheduled(fixedDelay = 2000)
    public void pollQueue() {
        ReceiveMessageResponse response = sqsClient.receiveMessage(
                ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(5)
                        .build());

        for (Message message : response.messages()) {
            try {
                process(message);

                sqsClient.deleteMessage(
                        DeleteMessageRequest.builder()
                                .queueUrl(queueUrl)
                                .receiptHandle(message.receiptHandle())
                                .build());
            } catch (Exception ex) {
                log.error("Error processing SQS message", ex);
            }
        }
    }

    private void process(Message message) throws Exception {
        CardProcessingMessage msg =
                objectMapper.readValue(message.body(), CardProcessingMessage.class);

        var card = seriesCardRepository.findById(msg.getCardId()).orElseThrow();

        // Update DB status
        CardProcessingStatus processingStatus = statusRepo.findByCode("processing").orElseThrow();
        card.setProcessingStatus(processingStatus); // processing
        seriesCardRepository.save(card);

        // Download originals
        BufferedImage front = s3ImageService.downloadImage(uploadsBucket, msg.getFrontKey());
        BufferedImage back  = s3ImageService.downloadImage(uploadsBucket, msg.getBackKey());

        // Process images (Rekognition handles both cropping and orientation)
        BufferedImage frontCropped = imageCropService.cropCard(front);
        BufferedImage backCropped  = imageCropService.cropCard(back);

        // Build processed S3 keys
        String frontProcessedKey = "processed/series/" + msg.getSeriesId() + "/cards/" + msg.getCardId() + "/front_cropped.jpg";
        String backProcessedKey  = "processed/series/" + msg.getSeriesId() + "/cards/" + msg.getCardId() + "/back_cropped.jpg";

        // Upload results
        s3ImageService.uploadImage(processedBucket, frontProcessedKey, frontCropped);
        s3ImageService.uploadImage(processedBucket, backProcessedKey, backCropped);

        // Build HTTPS URLs for mobile app
        String frontProcessedUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                processedBucket, awsRegion, frontProcessedKey);
        String backProcessedUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                processedBucket, awsRegion, backProcessedKey);

        // Build scan JSON
        String frontScanJson = objectMapper.createObjectNode()
                .put("processed_image_url", frontProcessedUrl)
                .toString();

        String backScanJson = objectMapper.createObjectNode()
                .put("processed_image_url", backProcessedUrl)
                .toString();

        CardProcessingStatus doneStatus = statusRepo.findByCode("done").orElseThrow();

        // Update DB
        card.setProcessedFrontImgUrl(frontProcessedUrl);
        card.setProcessedBackImgUrl(backProcessedUrl);
        card.setFrontScanResults(frontScanJson);
        card.setBackScanResults(backScanJson);
        card.setProcessedAt(Instant.now());
        card.setProcessingStatus(doneStatus); // done

        seriesCardRepository.save(card);
    }

}
