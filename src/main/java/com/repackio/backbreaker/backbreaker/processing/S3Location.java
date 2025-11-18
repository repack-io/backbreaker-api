package com.repackio.backbreaker.backbreaker.processing;

public record S3Location(String bucket, String key) {

    public S3Location {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("S3 bucket must be provided");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("S3 key must be provided");
        }
    }
}
