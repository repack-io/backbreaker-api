package com.repackio.backbreaker.repositories;

import com.repackio.backbreaker.models.CardDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardDetailRepository extends JpaRepository<CardDetail, Long> {

    /**
     * Find card details by series card ID.
     * Used to check if details already exist for a card.
     */
    Optional<CardDetail> findBySeriesCardId(Long seriesCardId);

    /**
     * Check if card details exist for a series card.
     */
    boolean existsBySeriesCardId(Long seriesCardId);

    /**
     * Find card details by series card ID with eagerly fetched player and team.
     * Used for generating labels to avoid LazyInitializationException.
     */
    @Query("SELECT cd FROM CardDetail cd " +
           "LEFT JOIN FETCH cd.player " +
           "LEFT JOIN FETCH cd.team " +
           "WHERE cd.seriesCardId = :seriesCardId")
    Optional<CardDetail> findBySeriesCardIdWithPlayerAndTeam(@Param("seriesCardId") Long seriesCardId);

    /**
     * Find all card details for a list of series card IDs with eagerly fetched player and team.
     */
    @Query("SELECT cd FROM CardDetail cd " +
           "LEFT JOIN FETCH cd.player " +
           "LEFT JOIN FETCH cd.team " +
           "WHERE cd.seriesCardId IN :seriesCardIds")
    List<CardDetail> findBySeriesCardIdInWithPlayerAndTeam(@Param("seriesCardIds") List<Long> seriesCardIds);
}
