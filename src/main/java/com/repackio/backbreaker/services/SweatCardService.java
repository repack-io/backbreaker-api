package com.repackio.backbreaker.services;

import com.google.zxing.WriterException;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.repackio.backbreaker.api.dto.CardLabelData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates "sweat card" PDF sheets — landscape 8.5x11 with 3 cards per page.
 * Each card contains the repax.io brand header, product/tier info, player name,
 * a detail blurb, and a QR code.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SweatCardService {

    private final QRCodeService qrCodeService;
    private final CardTokenService cardTokenService;
    private final Environment environment;

    // Landscape Letter: 792 x 612 points (1 point = 1/72 inch)
    private static final PageSize PAGE_SIZE = PageSize.LETTER.rotate();
    private static final float PAGE_W = 792f;
    private static final float PAGE_H = 612f;
    private static final float MARGIN = 18f;       // 0.25 inch outer margin
    private static final int CARDS_PER_PAGE = 3;
    private static final float CARD_W = (PAGE_W - 2 * MARGIN) / CARDS_PER_PAGE;  // ~252 pt
    private static final float CARD_H = PAGE_H - 2 * MARGIN;                      // 576 pt
    private static final float INNER_PAD = 14f;
    private static final int QR_PX = 200;  // QR source pixel size

    public byte[] generateSweatCards(List<CardLabelData> cards) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(writer)) {

            PdfFont logoFont    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont boldFont    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            int idx = 0;
            while (idx < cards.size()) {
                PdfPage page = pdf.addNewPage(PAGE_SIZE);
                PdfCanvas canvas = new PdfCanvas(page);

                // Vertical divider lines between cards
                canvas.setLineWidth(0.75f).setStrokeColor(new DeviceGray(0.55f));
                for (int i = 1; i < CARDS_PER_PAGE; i++) {
                    float lx = MARGIN + i * CARD_W;
                    canvas.moveTo(lx, MARGIN).lineTo(lx, PAGE_H - MARGIN).stroke();
                }

                for (int slot = 0; slot < CARDS_PER_PAGE && idx < cards.size(); slot++, idx++) {
                    float cardLeft = MARGIN + slot * CARD_W;
                    renderCard(canvas, logoFont, boldFont, regularFont, cards.get(idx), cardLeft);
                }

                canvas.release();
            }
        }

        return baos.toByteArray();
    }

    private void renderCard(PdfCanvas canvas, PdfFont logoFont, PdfFont boldFont, PdfFont regularFont,
                            CardLabelData card, float cardLeft) throws IOException {
        float cx = cardLeft + CARD_W / 2;
        // y tracks the current top position (iText origin is bottom-left)
        float y = MARGIN + CARD_H - INNER_PAD;

        // ── repax.io logo ──────────────────────────────────────────────
        float logoSize = 26f;
        String logoText = "repacks.io";
        float logoW = logoFont.getWidth(logoText, logoSize);
        y -= logoSize;
        canvas.beginText()
              .setFontAndSize(logoFont, logoSize)
              .moveText(cx - logoW / 2, y)
              .showText(logoText)
              .endText();
        y -= 7;

        // Thin rule under logo
        canvas.setLineWidth(0.5f).setStrokeColor(new DeviceGray(0.7f))
              .moveTo(cardLeft + INNER_PAD, y)
              .lineTo(cardLeft + CARD_W - INNER_PAD, y)
              .stroke();
        y -= 13;

        // ── Breaker name ───────────────────────────────────────────────
        if (card.getBreakerName() != null && !card.getBreakerName().isBlank()) {
            float size = 11f;
            String text = card.getBreakerName();
            float tw = regularFont.getWidth(text, size);
            y -= size;
            canvas.beginText()
                  .setFontAndSize(regularFont, size)
                  .moveText(cx - tw / 2, y)
                  .showText(text)
                  .endText();
            y -= 7;
        }

        // ── Series name ────────────────────────────────────────────────
        if (card.getSeriesName() != null && !card.getSeriesName().isBlank()) {
            float size = 14f;
            String text = card.getSeriesName();
            float tw = boldFont.getWidth(text, size);
            y -= size;
            canvas.beginText()
                  .setFontAndSize(boldFont, size)
                  .moveText(cx - tw / 2, y)
                  .showText(text)
                  .endText();
            y -= 12;
        }

        // ── Factoid paragraph (word-wrapped) ───────────────────────────
        if (card.getFactoid() != null && !card.getFactoid().isBlank()) {
            float size = 7.5f;
            float maxW = CARD_W - 2 * INNER_PAD;
            List<String> lines = wordWrap(card.getFactoid(), regularFont, size, maxW);
            for (String line : lines) {
                float tw = regularFont.getWidth(line, size);
                y -= size;
                canvas.beginText()
                      .setFontAndSize(regularFont, size)
                      .moveText(cx - tw / 2, y)
                      .showText(line)
                      .endText();
                y -= 3;
            }
            y -= 6;
        }

        // ── QR code ────────────────────────────────────────────────────
        try {
            String qrUrl = buildQrUrl(card);
            BufferedImage qrImg = qrCodeService.generateQRCode(qrUrl, QR_PX);
            ByteArrayOutputStream qrBaos = new ByteArrayOutputStream();
            ImageIO.write(qrImg, "PNG", qrBaos);
            ImageData imageData = ImageDataFactory.create(qrBaos.toByteArray());

            float availH   = y - MARGIN - INNER_PAD;
            float availW   = CARD_W - 2 * INNER_PAD;
            float qrSize   = Math.min(availH, availW);
            float qrX      = cx - qrSize / 2;
            float qrY      = y - qrSize - (availH - qrSize) / 2;

            // addImageWithTransformationMatrix(data, scaleX, skewY, skewX, scaleY, x, y)
            canvas.addImageWithTransformationMatrix(imageData, qrSize, 0, 0, qrSize, qrX, qrY);
        } catch (WriterException e) {
            log.error("Failed to generate QR code for card {}", card.getCardDetailId(), e);
        }
    }

    private String buildMetaLine(CardLabelData card) {
        StringBuilder sb = new StringBuilder();
        if (card.getCardYear() != null) sb.append(card.getCardYear());
        if (card.getParallelType() != null && !card.getParallelType().isBlank()) {
            if (!sb.isEmpty()) sb.append("  |  ");
            sb.append(card.getParallelType());
        }
        if (card.getSerialNumber() != null && !card.getSerialNumber().isBlank()) {
            if (!sb.isEmpty()) sb.append("  |  ");
            sb.append("#").append(card.getSerialNumber());
        }
        return sb.toString();
    }

    private List<String> wordWrap(String text, PdfFont font, float fontSize, float maxWidth) {
        List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (font.getWidth(candidate, fontSize) <= maxWidth) {
                current = new StringBuilder(candidate);
            } else {
                if (!current.isEmpty()) lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    private String buildQrUrl(CardLabelData card) {
        String signedToken = cardTokenService.generateToken(card.getSeriesCardId());
        return environment.getProperty("url.labelsheet", "https://repacks.io") + "/thisjustgothit?cardid=" + signedToken;
    }
}
