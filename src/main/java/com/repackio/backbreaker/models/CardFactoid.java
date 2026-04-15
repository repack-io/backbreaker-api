package com.repackio.backbreaker.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "card_factoids")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardFactoid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_detail_id", nullable = false, unique = true)
    private Long cardDetailId;

    @Column(name = "factoid", columnDefinition = "TEXT")
    private String factoid;
}
