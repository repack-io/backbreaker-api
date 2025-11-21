package com.repackio.backbreaker.repositories;

import com.repackio.backbreaker.models.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    /**
     * Find a player by name and card category type.
     * Used for deduplication when creating new players.
     */
    Optional<Player> findByFirstNameAndLastNameAndCardCategoryTypeId(
        String firstName,
        String lastName,
        Integer cardCategoryTypeId
    );
}
