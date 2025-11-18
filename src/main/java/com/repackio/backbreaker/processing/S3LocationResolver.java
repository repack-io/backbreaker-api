package com.repackio.backbreaker.processing;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;

@Component
public class S3LocationResolver {

    public S3Location resolve(String value, String defaultBucket) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("S3 location is not provided");
        }
        if (!value.contains("://")) {
            return new S3Location(defaultBucket, normalizeKey(value));
        }

        URI uri = URI.create(value);
        if ("s3".equalsIgnoreCase(uri.getScheme())) {
            return new S3Location(uri.getHost(), normalizeKey(uri.getPath()));
        }

        if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null) {
            String host = uri.getHost();
            int bucketEnd = host.indexOf(".s3");
            if (bucketEnd > 0) {
                String bucket = host.substring(0, bucketEnd);
                return new S3Location(bucket, normalizeKey(uri.getPath()));
            }
        }

        throw new IllegalArgumentException("Unsupported S3 URL format: " + value);
    }

    private String normalizeKey(String rawPath) {
        if (!StringUtils.hasText(rawPath)) {
            return "";
        }
        return rawPath.startsWith("/") ? rawPath.substring(1) : rawPath;
    }
}
