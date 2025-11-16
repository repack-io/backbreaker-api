package com.repackio.backbreaker.backbreaker.api;

import com.repackio.backbreaker.backbreaker.api.dto.SeriesFinalizeResponse;
import com.repackio.backbreaker.backbreaker.services.ProductSeriesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<SeriesFinalizeResponse> finalizeSeries(@PathVariable Long seriesId) {
        log.info("Finalize request received: POST /api/series/{}/finalize", seriesId);
        boolean success = productSeriesService.finalizeSeries(seriesId);

        if (success) {
            SeriesFinalizeResponse response = new SeriesFinalizeResponse(
                    seriesId,
                    true,
                    "Series finalized successfully"
            );
            return ResponseEntity.ok(response);
        }

        SeriesFinalizeResponse response = new SeriesFinalizeResponse(
                seriesId,
                false,
                "Series not found or already finalized"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
}
