package com.repackio.backbreaker.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
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

    @JoinColumn(name = "product_id", nullable = false)
    @ManyToOne
    private BreakerProduct product;

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

    public ProductSeries(BreakerProduct product, Integer seriesNum, LocalDateTime packDate) {
        this.product = product;
        this.seriesNum = seriesNum;
        this.packDate = packDate;
        this.finalized = false;
    }

}
