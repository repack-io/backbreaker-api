package com.repackio.backbreaker.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents a card category type (baseball, football, basketball, hockey, etc.).
 */
@Entity
@Table(name = "card_category_types", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"category"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "category", nullable = false, length = 50)
    private String category;
}
