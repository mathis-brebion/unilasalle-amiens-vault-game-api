package fr.nexusproject.vault_game_api.controllers;

import fr.nexusproject.vault_game_api.services.RawgGameService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/rawg/games")
public class RawgGameController {

    private final RawgGameService rawgGameService;

    public RawgGameController(RawgGameService rawgGameService) {
        this.rawgGameService = rawgGameService;
    }

    @GetMapping("/search")
    public List<RawgGameSearchResponse> searchGames(
            @RequestParam @NotBlank @Size(min = 2, max = 120) String query,
            @RequestParam(defaultValue = "8") @Min(1) @Max(20) int size) {
        return rawgGameService.searchGames(query, size).stream()
                .map(result -> new RawgGameSearchResponse(
                        result.rawgId(),
                        result.name(),
                        result.releasedAt(),
                        result.backgroundImageUrl()))
                .toList();
    }

    @GetMapping("/{rawgId}")
    public RawgGameDetailsResponse getGameDetails(@PathVariable @Positive Long rawgId) {
        RawgGameService.RawgGameDetails details = rawgGameService.getGameDetails(rawgId);
        return new RawgGameDetailsResponse(
                details.rawgId(),
                details.name(),
                details.slug(),
                details.genre(),
                details.genres(),
                details.releaseYear(),
                details.releasedAt(),
                details.backgroundImageUrl(),
                details.description(),
                details.platformNames());
    }

    public record RawgGameSearchResponse(
            Long rawgId,
            String name,
            LocalDate releasedAt,
            String backgroundImageUrl) {
    }

    public record RawgGameDetailsResponse(
            Long rawgId,
            String name,
            String slug,
            String genre,
            List<String> genres,
            Integer releaseYear,
            LocalDate releasedAt,
            String backgroundImageUrl,
            String description,
            Set<String> platformNames) {
    }
}
