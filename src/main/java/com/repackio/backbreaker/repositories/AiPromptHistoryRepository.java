package com.repackio.backbreaker.repositories;

import com.repackio.backbreaker.models.AiPromptHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiPromptHistoryRepository extends JpaRepository<AiPromptHistory, Integer> {

    List<AiPromptHistory> findByPromptKeyOrderByVersionDesc(String promptKey);
}
