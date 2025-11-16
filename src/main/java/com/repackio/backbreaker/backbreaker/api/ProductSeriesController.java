package com.repackio.backbreaker.backbreaker.api;

import com.repackio.backbreaker.backbreaker.services.ProductSeriesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/series")
@RequiredArgsConstructor
@Slf4j
public class ProductSeriesController {

    private final ProductSeriesService productSeriesService;

    @PostMapping("/{seriesId}/finalize")
    public ResponseEntity<Void> finalizeSeries(@PathVariable Long seriesId) {
        log.info("Finalize request received: POST /api/series/{}/finalize", seriesId);
        productSeriesService.finalizeSeries(seriesId);
        return ResponseEntity.accepted().build();
    }
}
