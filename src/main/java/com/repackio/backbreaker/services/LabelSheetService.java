package com.repackio.backbreaker.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.WriterException;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.repackio.backbreaker.api.dto.CardLabelData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generating Avery 94103 label sheets as PDF files.
 *
 * Avery 94103 Specifications:
 * - Label size: 1" x 1" (72 points x 72 points in PDF)
 * - 48 labels per sheet (6 columns x 8 rows)
 * - Sheet size: 8.5" x 11" (Letter)
 * - Margins: 0.625" top, bottom, left, right (45 points)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LabelSheetService {

    private final QRCodeService qrCodeService;
    private final ObjectMapper objectMapper;

    // Avery 94103 specifications (in points: 1 inch = 72 points)
    private static final float LABEL_WIDTH = 72f;  // 1 inch
    private static final float LABEL_HEIGHT = 72f; // 1 inch
    private static final int LABELS_PER_ROW = 6;
    private static final int LABELS_PER_COLUMN = 8;
    private static final int LABELS_PER_SHEET = 48;
    private static final float MARGIN_TOP = 45f;    // 0.625 inch
    private static final float MARGIN_BOTTOM = 45f;
    private static final float MARGIN_LEFT = 45f;
    private static final float MARGIN_RIGHT = 45f;
    private static final int QR_CODE_SIZE = 60; // pixels for QR code generation
    private static final float FONT_SIZE = 5f;  // Small font for 1" labels

    /**
     * Generate a PDF label sheet for the given card data.
     *
     * @param cards List of card label data
     * @return PDF file as byte array
     * @throws IOException if PDF generation fails
     */
    public byte[] generateLabelSheet(List<CardLabelData> cards) throws IOException {
        log.info("Generating label sheet for {} cards", cards.size());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf, PageSize.LETTER)) {

            // Set margins
            document.setMargins(MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOTTOM, MARGIN_LEFT);

            int cardIndex = 0;
            int totalCards = cards.size();

            // Generate sheets (each sheet has 48 labels)
            while (cardIndex < totalCards) {
                if (cardIndex > 0) {
                    document.add(new com.itextpdf.layout.element.AreaBreak());
                }

                Table table = createLabelTable();

                // Fill the table with labels (6 columns x 8 rows = 48 labels)
                for (int row = 0; row < LABELS_PER_COLUMN; row++) {
                    for (int col = 0; col < LABELS_PER_ROW; col++) {
                        if (cardIndex < totalCards) {
                            table.addCell(createLabelCell(cards.get(cardIndex)));
                            cardIndex++;
                        } else {
                            // Empty cell for remaining slots
                            table.addCell(createEmptyCell());
                        }
                    }
                }

                document.add(table);
            }

            log.info("Successfully generated label sheet with {} labels across {} pages",
                    totalCards, (totalCards + LABELS_PER_SHEET - 1) / LABELS_PER_SHEET);
        }

        return baos.toByteArray();
    }

    /**
     * Create the label table with proper dimensions.
     */
    private Table createLabelTable() {
        float[] columnWidths = new float[LABELS_PER_ROW];
        for (int i = 0; i < LABELS_PER_ROW; i++) {
            columnWidths[i] = LABEL_WIDTH;
        }

        Table table = new Table(UnitValue.createPointArray(columnWidths));
        table.setWidth(UnitValue.createPointValue(LABEL_WIDTH * LABELS_PER_ROW));
        table.setHorizontalAlignment(HorizontalAlignment.CENTER);

        return table;
    }

    /**
     * Create a label cell with QR code and card information.
     */
    private com.itextpdf.layout.element.Cell createLabelCell(CardLabelData card) throws IOException {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell();
        cell.setWidth(UnitValue.createPointValue(LABEL_WIDTH));
        cell.setHeight(UnitValue.createPointValue(LABEL_HEIGHT));
        cell.setPadding(2);
        cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        cell.setHorizontalAlignment(HorizontalAlignment.CENTER);

        try {
            // Generate QR code data (JSON format, excluding usd_value_range)
            String qrData = generateQRData(card);
            BufferedImage qrImage = qrCodeService.generateQRCode(qrData, QR_CODE_SIZE);

            // Convert BufferedImage to iText Image
            ByteArrayOutputStream qrBaos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", qrBaos);
            Image qrPdfImage = new Image(ImageDataFactory.create(qrBaos.toByteArray()));
            qrPdfImage.setWidth(50); // Slightly smaller to fit text
            qrPdfImage.setHorizontalAlignment(HorizontalAlignment.CENTER);

            cell.add(qrPdfImage);

            // Add player name (bold, slightly larger)
            if (card.getPlayerName() != null) {
                Paragraph playerPara = new Paragraph(card.getPlayerName())
                        .setFontSize(FONT_SIZE + 1)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMargin(0)
                        .setPadding(0);
                cell.add(playerPara);
            }

            // Add year and parallel on same line if available
            StringBuilder details = new StringBuilder();
            if (card.getCardYear() != null) {
                details.append(card.getCardYear());
            }
            if (card.getParallelType() != null && !card.getParallelType().equalsIgnoreCase("Base")) {
                if (details.length() > 0) {
                    details.append(" ");
                }
                details.append(card.getParallelType());
            }

            if (details.length() > 0) {
                Paragraph detailsPara = new Paragraph(details.toString())
                        .setFontSize(FONT_SIZE)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMargin(0)
                        .setPadding(0);
                cell.add(detailsPara);
            }

        } catch (WriterException e) {
            log.error("Failed to generate QR code for card {}", card.getCardDetailId(), e);
            // Add error text instead
            cell.add(new Paragraph("QR Error")
                    .setFontSize(FONT_SIZE)
                    .setTextAlignment(TextAlignment.CENTER));
        }

        return cell;
    }

    /**
     * Create an empty label cell.
     */
    private com.itextpdf.layout.element.Cell createEmptyCell() {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell();
        cell.setWidth(UnitValue.createPointValue(LABEL_WIDTH));
        cell.setHeight(UnitValue.createPointValue(LABEL_HEIGHT));
        return cell;
    }

    /**
     * Generate QR code data as JSON (excluding usd_value_range).
     */
    private String generateQRData(CardLabelData card) throws IOException {
        Map<String, Object> qrData = new HashMap<>();
        qrData.put("card_detail_id", card.getCardDetailId());
        qrData.put("series_card_id", card.getSeriesCardId());
        qrData.put("player_name", card.getPlayerName());
        qrData.put("team_name", card.getTeamName());
        qrData.put("card_year", card.getCardYear());
        qrData.put("parallel_type", card.getParallelType());
        qrData.put("serial_number", card.getSerialNumber());
        qrData.put("tier_name", card.getTierName());

        return objectMapper.writeValueAsString(qrData);
    }
}
