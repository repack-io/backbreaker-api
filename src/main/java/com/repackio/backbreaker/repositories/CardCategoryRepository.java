package com.repackio.backbreaker.repositories;

import com.repackio.backbreaker.models.CardCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardCategoryRepository extends JpaRepository<CardCategory, Integer> {

    /**
     * Find a card category by its category name (case-insensitive).
     * The category will be converted to lowercase before querying.
     *
     * @param category The category name (e.g., "baseball", "football")
     * @return Optional containing the CardCategory if found
     */
    @Query("SELECT c FROM CardCategory c WHERE LOWER(c.category) = LOWER(:category)")
    Optional<CardCategory> findByCategory(@Param("category") String category);

    /**
     * Get the ID of a card category by its name (case-insensitive).
     * Returns the serial4 ID if found.
     *
     * @param category The category name (e.g., "baseball", "football")
     * @return Optional containing the category ID if found
     */
    @Query("SELECT c.id FROM CardCategory c WHERE LOWER(c.category) = LOWER(:category)")
    Optional<Integer> findIdByCategory(@Param("category") String category);
}
