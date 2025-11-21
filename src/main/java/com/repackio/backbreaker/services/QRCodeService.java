package com.repackio.backbreaker.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating QR codes.
 */
@Slf4j
@Service
public class QRCodeService {

    /**
     * Generate a QR code image from text data.
     *
     * @param data The text data to encode in the QR code
     * @param size The size of the QR code image (width and height in pixels)
     * @return BufferedImage containing the QR code
     * @throws WriterException if QR code generation fails
     */
    public BufferedImage generateQRCode(String data, int size) throws WriterException {
        log.debug("Generating QR code for data length: {}, size: {}", data.length(), size);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1); // Reduced margin for better space utilization

        BitMatrix bitMatrix = qrCodeWriter.encode(
            data,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        );

        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }
}
