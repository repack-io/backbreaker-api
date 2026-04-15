package com.repackio.backbreaker.repositories;

import com.repackio.backbreaker.models.CardFactoid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardFactoidRepository extends JpaRepository<CardFactoid, Long> {
    Optional<CardFactoid> findByCardDetailId(Long cardDetailId);
    List<CardFactoid> findByCardDetailIdIn(List<Long> cardDetailIds);
}
