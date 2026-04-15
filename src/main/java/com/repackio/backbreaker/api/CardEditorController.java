package com.repackio.backbreaker.api;

import com.repackio.backbreaker.api.dto.CardEditorRow;
import com.repackio.backbreaker.api.dto.CardEditorUpdateRequest;
import com.repackio.backbreaker.models.CardDetail;
import com.repackio.backbreaker.models.Player;
import com.repackio.backbreaker.models.Team;
import com.repackio.backbreaker.repositories.CardDetailRepository;
import com.repackio.backbreaker.repositories.PlayerRepository;
import com.repackio.backbreaker.repositories.TeamRepository;
import com.repackio.backbreaker.services.CardFactoidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/card-editor")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CardEditorController {

    private final CardDetailRepository cardDetailRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final CardFactoidService cardFactoidService;

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> editorPage() throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/card-editor.html");
        String html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    @GetMapping("/search")
    public List<CardEditorRow> search(
            @RequestParam(required = false) String player,
            @RequestParam(required = false) String product,
            @RequestParam(required = false) Integer breakerId,
            @RequestParam(required = false) Integer seriesNum
    ) {
        log.info("Card editor search: player={}, product={}, breakerId={}, seriesNum={}", player, product, breakerId, seriesNum);
        return cardDetailRepository.searchCards(player, product, breakerId, seriesNum);
    }

    @PutMapping("/series-card/{seriesCardId}")
    public ResponseEntity<Void> updateBySeriesCard(
            @PathVariable Long seriesCardId,
            @RequestBody CardEditorUpdateRequest request
    ) {
        CardDetail cardDetail = cardDetailRepository.findBySeriesCardId(seriesCardId)
                .orElseGet(() -> {
                    CardDetail cd = new CardDetail();
                    cd.setSeriesCardId(seriesCardId);
                    return cd;
                });
        applyUpdate(cardDetail, request);
        CardDetail saved = cardDetailRepository.save(cardDetail);
        log.info("Upserted CardDetail id={} for seriesCardId={}", saved.getId(), seriesCardId);
        if (request.getFactoid() != null) {
            cardFactoidService.upsert(saved.getId(), request.getFactoid());
        }
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            @PathVariable Long id,
            @RequestBody CardEditorUpdateRequest request
    ) {
        CardDetail cardDetail = cardDetailRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("CardDetail not found: " + id));
        applyUpdate(cardDetail, request);
        cardDetailRepository.save(cardDetail);
        log.info("Updated CardDetail id={}", id);
        if (request.getFactoid() != null) {
            cardFactoidService.upsert(id, request.getFactoid());
        }
        return ResponseEntity.ok().build();
    }

    private void applyUpdate(CardDetail cardDetail, CardEditorUpdateRequest request) {
        // Resolve player
        if (hasText(request.getPlayerFirstName()) && hasText(request.getPlayerLastName())) {
            Integer categoryId = request.getCardCategoryTypeId() != null
                    ? request.getCardCategoryTypeId()
                    : cardDetail.getCardCategoryTypeId();
            Player player = playerRepository
                    .findByFirstNameAndLastNameAndCardCategoryTypeId(
                            request.getPlayerFirstName(), request.getPlayerLastName(), categoryId)
                    .orElseGet(() -> playerRepository.save(
                            new Player(null, request.getPlayerFirstName(), request.getPlayerLastName(), categoryId)));
            cardDetail.setPlayer(player);
        } else if (request.getPlayerFirstName() != null && request.getPlayerFirstName().isBlank()
                && request.getPlayerLastName() != null && request.getPlayerLastName().isBlank()) {
            cardDetail.setPlayer(null);
        }

        // Resolve team
        if (hasText(request.getTeamName())) {
            Integer categoryId = request.getCardCategoryTypeId() != null
                    ? request.getCardCategoryTypeId()
                    : cardDetail.getCardCategoryTypeId();
            Team team = teamRepository
                    .findByNameAndCardCategoryTypeId(request.getTeamName(), categoryId)
                    .orElseGet(() -> teamRepository.save(
                            new Team(null, request.getTeamName(), categoryId)));
            cardDetail.setTeam(team);
        } else if (request.getTeamName() != null && request.getTeamName().isBlank()) {
            cardDetail.setTeam(null);
        }

        // Update scalar fields
        if (request.getParallelType() != null) cardDetail.setParallelType(request.getParallelType());
        if (request.getSerialNumber() != null) cardDetail.setSerialNumber(request.getSerialNumber());
        if (request.getCardCategoryTypeId() != null) cardDetail.setCardCategoryTypeId(request.getCardCategoryTypeId());
        if (request.getCardStatusId() != null) cardDetail.setCardStatusId(request.getCardStatusId());
        if (request.getProductTierId() != null) cardDetail.setProductTierId(request.getProductTierId());
        if (request.getHitDate() != null) cardDetail.setHitDate(request.getHitDate());
        if (request.getUsdValue() != null) cardDetail.setUsdValue(request.getUsdValue());
        if (request.getCardYear() != null) cardDetail.setCardYear(request.getCardYear());
        if (request.getUsdValueRange() != null) cardDetail.setUsdValueRange(request.getUsdValueRange());
        if (request.getConfidence() != null) cardDetail.setConfidence(request.getConfidence());
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
