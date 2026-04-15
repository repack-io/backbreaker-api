package com.repackio.backbreaker.repositories;

import com.repackio.backbreaker.models.CardDetail;
import com.repackio.backbreaker.api.dto.CardEditorRow;
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

    /**
     * Search card details across player name, product name, breaker ID, and series number.
     */
    @Query(value = """
            SELECT cd.id,
                   sc.id AS seriesCardId,
                   cd.parallel_type AS parallelType,
                   cd.serial_number AS serialNumber,
                   cd.card_category_type_id AS cardCategoryTypeId,
                   cd.card_status_id AS cardStatusId,
                   cd.product_tier_id AS productTierId,
                   cd.hit_date AS hitDate,
                   cd.usd_value AS usdValue,
                   cd.card_year AS cardYear,
                   cd.usd_value_range AS usdValueRange,
                   cd.confidence,
                   p.id AS playerId,
                   p.first_name AS firstName,
                   p.last_name AS lastName,
                   t.id AS teamId,
                   t.name AS teamName,
                   bp.product_name AS productName,
                   ps.series_num AS seriesNum,
                   bp.breaker_id AS breakerId,
                   sc.front_img_url AS frontImgUrl,
                   sc.back_img_url AS backImgUrl,
                   sc.processed_front_img_url AS processedFrontImgUrl,
                   sc.processed_back_img_url AS processedBackImgUrl,
                   cf.id AS factoidId,
                   cf.factoid AS factoid
            FROM series_cards sc
            JOIN product_series ps ON ps.id = sc.series_id
            JOIN breaker_products bp ON bp.id = ps.product_id
            LEFT JOIN card_details cd ON cd.series_card_id = sc.id
            LEFT JOIN players p ON p.id = cd.player_id
            LEFT JOIN teams t ON t.id = cd.team_id
            LEFT JOIN card_factoids cf ON cf.card_detail_id = cd.id
            WHERE (:playerName IS NULL OR :playerName = '' OR LOWER(CONCAT(p.first_name, ' ', p.last_name)) LIKE LOWER(CONCAT('%', :playerName, '%')))
              AND (:productName IS NULL OR :productName = '' OR LOWER(bp.product_name) LIKE LOWER(CONCAT('%', :productName, '%')))
              AND (:breakerId IS NULL OR bp.breaker_id = :breakerId)
              AND (:seriesNum IS NULL OR ps.series_num = :seriesNum)
            ORDER BY sc.id DESC
            LIMIT 100
            """, nativeQuery = true)
    List<CardEditorRow> searchCards(
            @Param("playerName") String playerName,
            @Param("productName") String productName,
            @Param("breakerId") Integer breakerId,
            @Param("seriesNum") Integer seriesNum
    );
}
