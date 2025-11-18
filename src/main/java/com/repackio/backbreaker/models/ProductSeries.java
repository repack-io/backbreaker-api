package com.repackio.backbreaker.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_series")
public class ProductSeries {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_series_seq")
    @SequenceGenerator(
            name = "product_series_seq",
            sequenceName = "product_series_id_seq2",
            allocationSize = 1
    )
    private Integer id;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(name = "series_num", nullable = false)
    private Integer seriesNum;

    @Column(name = "pack_date", nullable = false)
    private LocalDateTime packDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_finalized", nullable = false)
    private boolean finalized;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    // ---- Constructors ----

    public ProductSeries() {}

    public ProductSeries(Integer productId, Integer seriesNum, LocalDateTime packDate) {
        this.productId = productId;
        this.seriesNum = seriesNum;
        this.packDate = packDate;
        this.finalized = false;
    }

    // ---- Getters / Setters ----

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public Integer getSeriesNum() {
        return seriesNum;
    }

    public void setSeriesNum(Integer seriesNum) {
        this.seriesNum = seriesNum;
    }

    public LocalDateTime getPackDate() {
        return packDate;
    }

    public void setPackDate(LocalDateTime packDate) {
        this.packDate = packDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isFinalized() {
        return finalized;
    }

    public void setFinalized(boolean finalized) {
        this.finalized = finalized;
    }

    public LocalDateTime getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt(LocalDateTime finalizedAt) {
        this.finalizedAt = finalizedAt;
    }
}
