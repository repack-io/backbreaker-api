package com.repackio.backbreaker.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "card_processing_status")
public class CardProcessingStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false, unique = true, length = 32)
    private String code; // pending, queued, processing, done, error

    public CardProcessingStatus() {
    }

    public CardProcessingStatus(String code) {
        this.code = code;
    }

}
