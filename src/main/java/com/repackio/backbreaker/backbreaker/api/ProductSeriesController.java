package com.repackio.backbreaker.backbreaker.api;

import com.repackio.backbreaker.backbreaker.api.dto.CardProcessingFailureResponse;
import com.repackio.backbreaker.backbreaker.api.dto.SeriesFinalizeResponse;
import com.repackio.backbreaker.backbreaker.processing.CardProcessingFailure;
import com.repackio.backbreaker.backbreaker.processing.SeriesProcessingReport;
import com.repackio.backbreaker.backbreaker.services.ProductSeriesService;
import com.repackio.backbreaker.backbreaker.services.SeriesFinalizeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/series")
@RequiredArgsConstructor
@Slf4j
public class ProductSeriesController {

    private final ProductSeriesService productSeriesService;

    @PostMapping("/{seriesId}/finalize")
    public ResponseEntity<SeriesFinalizeResponse> finalizeSeries(@PathVariable Long seriesId) throws ExecutionException, InterruptedException {
        log.info("Finalize request received: POST /api/series/{}/finalize", seriesId);
        SeriesFinalizeResult result = productSeriesService.finalizeSeries(seriesId);
        SeriesFinalizeResponse response = mapResponse(result);

        return result.isFinalized()
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    private SeriesFinalizeResponse mapResponse(SeriesFinalizeResult result) {
        SeriesProcessingReport report = result.getProcessingReport();
        var failures = report == null ? List.<CardProcessingFailureResponse>of()
                : report.getFailures().stream().map(this::mapFailure).toList();

        int totalCards = report != null ? report.getTotalCards() : 0;
        int processedCards = report != null ? report.getProcessedCards() : 0;

        String message;
        if (!result.isFinalized()) {
            message = "Series not found or already finalized";
        } else if (result.isProcessingStarted()) {
            message = "Series finalized and card processing started asynchronously";
        } else {
            message = "Series finalized successfully";
        }

        return new SeriesFinalizeResponse(
                result.getSeriesId(),
                result.isFinalized(),
                message,
                totalCards,
                processedCards,
                failures,
                result.isProcessingStarted()
        );
    }

    private CardProcessingFailureResponse mapFailure(CardProcessingFailure failure) {
        return new CardProcessingFailureResponse(failure.cardId(), failure.reason());
    }
}
