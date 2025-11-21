package com.repackio.backbreaker.repositories;

import com.repackio.backbreaker.models.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    /**
     * Find a team by name and card category type.
     * Used for deduplication when creating new teams.
     */
    Optional<Team> findByNameAndCardCategoryTypeId(
        String name,
        Integer cardCategoryTypeId
    );
}
