package com.repackio.backbreaker.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Entity
@Table(name = "series_cards")
public class SeriesCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(name = "series_id", nullable = false)
    private Long seriesId;

    @Setter
    @Column(name = "front_img_url")
    private String frontImgUrl;

    @Setter
    @Column(name = "back_img_url")
    private String backImgUrl;

    @Setter
    @Column(name = "processed_front_img_url")
    private String processedFrontImgUrl;

    @Setter
    @Column(name = "processed_back_img_url")
    private String processedBackImgUrl;

    @Setter
    @Column(name = "front_scan_results", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String frontScanResults; // store JSON as String

    @Setter
    @Column(name = "back_scan_results", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String backScanResults; // store JSON as String

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processing_status")
    private CardProcessingStatus processingStatus;

    @Setter
    @Column(name = "processed_at")
    private Instant processedAt;

    @Setter
    @Column(name = "product_tier_id", nullable = false)
    private Long productTierId;

    public SeriesCard() {
    }

    // --- getters & setters ---

}
