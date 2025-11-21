package com.repackio.backbreaker.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents a sports player extracted from card details.
 * Players are deduplicated by name and card category type.
 */
@Entity
@Table(name = "players", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"first_name", "last_name", "card_category_type_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 255)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 255)
    private String lastName;

    @Column(name = "card_category_type_id", nullable = false)
    private Integer cardCategoryTypeId;

    /**
     * Get the full name of the player.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
