package com.repackio.backbreaker.backbreaker.services;

import com.repackio.backbreaker.backbreaker.repositories.ProductSeriesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProductSeriesService {

    private static final Logger log = LoggerFactory.getLogger(ProductSeriesService.class);

    private final ProductSeriesRepository repo;

    public ProductSeriesService(ProductSeriesRepository repo) {
        this.repo = repo;
    }

    public void finalizeSeries(Long id) {
        log.debug("Finalizing series id={}", id);
        int updatedRows = repo.finalizeSeriesById(id);
        if (updatedRows == 0) {
            log.warn("Finalize request for series id={} did not update any rows", id);
        } else {
            log.info("Series id={} finalized ({} row(s) updated)", id, updatedRows);
        }
    }
}
