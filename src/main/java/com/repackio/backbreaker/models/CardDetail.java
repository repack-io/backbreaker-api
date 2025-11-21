package com.repackio.backbreaker.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents detailed information about a trading card extracted via AI.
 * Links a series card to its player, team, and other metadata.
 */
@Entity
@Table(name = "card_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "series_card_id", nullable = false, unique = true)
    private Long seriesCardId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "parallel_type", length = 100)
    private String parallelType;

    @Column(name = "serial_number", length = 50)
    private String serialNumber;

    @Column(name = "card_category_type_id", nullable = false)
    private Integer cardCategoryTypeId;

    @Column(name = "card_status_id", nullable = false)
    private Integer cardStatusId;

    @Column(name = "product_tier_id")
    private Long productTierId;

    @Column(name = "hit_date")
    private LocalDate hitDate;

    @Column(name = "usd_value", precision = 10, scale = 2)
    private BigDecimal usdValue;

    @Column(name = "card_year")
    private Integer cardYear;

    @Column(name = "usd_value_range")
    private String usdValueRange;

    @Column(name = "confidence")
    private String confidence;
}
