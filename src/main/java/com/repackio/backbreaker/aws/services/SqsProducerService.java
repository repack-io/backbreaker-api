package com.repackio.backbreaker.aws.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.repackio.backbreaker.aws.dto.CardProcessingMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
@RequiredArgsConstructor
public class SqsProducerService {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.queueUrl}")
    private String queueUrl;

    public void sendCardMessage(CardProcessingMessage msg) {
        try {
            String json = objectMapper.writeValueAsString(msg);

            sqsClient.sendMessage(
                    SendMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .messageBody(json)
                            .build()
            );
        } catch (Exception ex) {
            throw new RuntimeException("Failed to send SQS card message", ex);
        }
    }
}
