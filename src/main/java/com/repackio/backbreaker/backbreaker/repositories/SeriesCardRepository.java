package com.repackio.backbreaker.backbreaker.repositories;

import com.repackio.backbreaker.backbreaker.models.SeriesCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeriesCardRepository extends JpaRepository<SeriesCard, Long> {

    List<SeriesCard> findBySeriesIdAndFrontImgUrlIsNotNullAndBackImgUrlIsNotNull(Long seriesId);
}
