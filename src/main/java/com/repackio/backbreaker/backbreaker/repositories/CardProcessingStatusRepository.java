package com.repackio.backbreaker.backbreaker.repositories;

import com.repackio.backbreaker.backbreaker.models.CardProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardProcessingStatusRepository extends JpaRepository<CardProcessingStatus, Long> {

    Optional<CardProcessingStatus> findByCode(String code);
}
