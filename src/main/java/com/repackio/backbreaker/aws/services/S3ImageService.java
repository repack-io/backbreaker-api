package com.repackio.backbreaker.aws.services;

import com.repackio.backbreaker.utils.ImageOrientationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ImageService {

    private final S3Client s3Client;

    public BufferedImage downloadImage(String bucket, String key) throws Exception {
        log.debug("Downloading image from s3://{}/{}", bucket, key);

        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        ResponseBytes<GetObjectResponse> obj = s3Client.getObjectAsBytes(req);
        byte[] imageBytes = obj.asByteArray();

        // Read the image
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));

        // Correct orientation based on EXIF data
        image = ImageOrientationUtil.correctOrientation(imageBytes, image);

        log.debug("Image downloaded and orientation corrected: {}x{}", image.getWidth(), image.getHeight());
        return image;
    }

    public void uploadImage(String bucket, String key, BufferedImage img) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", os);
        os.flush();

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType("image/jpeg")
                        .build(),
                RequestBody.fromBytes(os.toByteArray())
        );
    }
}
