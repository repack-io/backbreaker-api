package com.repackio.backbreaker.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "breaker_products")
public class BreakerProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_series_seq")
    @SequenceGenerator(
            name = "breaker_product_seq",
            sequenceName = "breaker_products_id_seq2",
            allocationSize = 1
    )
    private Integer id;

    @Column(name="breaker_id")
    private Integer breakerId;

    @Column(name="product_name")
    private String productName;

}
