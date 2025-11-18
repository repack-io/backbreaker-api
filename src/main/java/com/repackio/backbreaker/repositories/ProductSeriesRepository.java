package com.repackio.backbreaker.repositories;

import com.repackio.backbreaker.models.ProductSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ProductSeriesRepository extends JpaRepository<ProductSeries, Integer> {

    /**
     * Marks a series as finalized.
     * Sets is_finalized = true and finalized_at = NOW().
     */
    @Modifying
    @Transactional
    @Query(
            value = "UPDATE product_series " +
                    "SET is_finalized = TRUE, finalized_at = NOW() " +
                    "WHERE id = :id",
            nativeQuery = true
    )
    int finalizeSeriesById(Long id);
}
