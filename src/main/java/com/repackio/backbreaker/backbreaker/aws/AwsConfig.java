package com.repackio.backbreaker.backbreaker.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AwsConfig {

    private static final Logger log = LoggerFactory.getLogger(AwsConfig.class);

    @Value("${aws.region}")
    private String region;

    @Value("${aws.profile:}")
    private String profile;

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        if (StringUtils.hasText(profile)) {
            try {
                log.info("Using AWS profile '{}'", profile);
                return ProfileCredentialsProvider.create(profile);
            } catch (RuntimeException ex) {
                log.warn("Failed to load AWS profile '{}', falling back to instance credentials: {}", profile, ex.getMessage());
            }
        }
        log.info("Using default AWS credentials provider chain");
        return DefaultCredentialsProvider.create();
    }

    @Bean
    public RekognitionClient rekognitionClient(AwsCredentialsProvider provider) {
        return RekognitionClient.builder()
                .region(Region.of(region))
                .credentialsProvider(provider)
                .build();
    }

    @Bean
    public S3Client s3Client(AwsCredentialsProvider provider) {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(provider)
                .build();
    }

    @Bean
    public SqsClient sqsClient(AwsCredentialsProvider provider) {
        return SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(provider)
                .build();
    }
}
