package com.repackio.backbreaker.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents a sports team extracted from card details.
 * Teams are deduplicated by name and card category type.
 */
@Entity
@Table(name = "teams", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"name", "card_category_type_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "card_category_type_id", nullable = false)
    private Integer cardCategoryTypeId;
}
