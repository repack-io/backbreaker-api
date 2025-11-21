package com.repackio.backbreaker.processing;

public record S3Location(String bucket, String key) {

    public S3Location {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("S3 bucket must be provided");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("S3 key must be provided");
        }
    }

    /**
     * Convert to HTTPS URL format.
     * @param region AWS region (e.g., "us-east-2")
     * @return HTTPS URL like https://bucket.s3.region.amazonaws.com/key
     */
    public String toHttpsUrl(String region) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    /**
     * Convert to s3:// URI format.
     * @return s3:// URI like s3://bucket/key
     */
    public String toS3Uri() {
        return "s3://" + bucket + "/" + key;
    }
}
