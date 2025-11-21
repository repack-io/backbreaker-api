package com.repackio.backbreaker.api;


import com.repackio.backbreaker.models.SeriesCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * TODO: I THINK THIS CLASS IS UNNECESSARY
 */
@RestController
@RequestMapping("/api/cards")
public class SeriesCardImageController {

    private static final Logger log = LoggerFactory.getLogger(SeriesCardImageController.class);

    /**
     * Endpoint to receive card image update events from AWS Lambda.
     *
     * Expected JSON:
     * {
     *   "id": 1,
     *   "front_img_url": "string",
     *   "back_img_url": "string"
     * }
     */
    @PostMapping("/card_upload")
    public ResponseEntity<String> handleImageUpdate(@RequestBody SeriesCard request) {

        log.info("Received card image update: id={}, front={}, back={}",
                request.getId(), request.getFrontImgUrl(), request.getBackImgUrl());

        return ResponseEntity.ok("Image update processed");
    }

}
