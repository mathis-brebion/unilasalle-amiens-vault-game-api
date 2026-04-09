package fr.nexusproject.vault_game_api.controllers;

import fr.nexusproject.vault_game_api.models.Game;
import fr.nexusproject.vault_game_api.models.Platform;
import fr.nexusproject.vault_game_api.repository.GameRepository;
import fr.nexusproject.vault_game_api.repository.PlatformRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameRepository gameRepository;
    private final PlatformRepository platformRepository;

    public GameController(GameRepository gameRepository, PlatformRepository platformRepository) {
        this.gameRepository = gameRepository;
        this.platformRepository = platformRepository;
    }

    @GetMapping
    public Page<GameResponse> getAllGames(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Game> games = (search == null || search.isBlank())
                ? gameRepository.findAll(pageable)
                : gameRepository.findByNameContainingIgnoreCase(search, pageable);

        return games.map(this::toResponse);
    }

    @GetMapping("/{id}")
    public GameResponse getGameById(@PathVariable Long id) {
        return toResponse(findGameOrThrow(id));
    }

    @PostMapping
    public ResponseEntity<GameResponse> createGame(@Valid @RequestBody GameRequest request) {
        Game game = new Game(
                request.rawgId(),
                request.name(),
                request.slug(),
                request.genre(),
                request.releaseYear(),
                request.releasedAt(),
                request.backgroundImageUrl());

        if (request.platformIds() != null && !request.platformIds().isEmpty()) {
            game.setPlatforms(resolvePlatforms(request.platformIds()));
        }

        try {
            Game savedGame = gameRepository.save(game);
            return ResponseEntity
                    .created(URI.create("/api/games/" + savedGame.getId()))
                    .body(toResponse(savedGame));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A game already exists with this rawgId.");
        }
    }

    @PutMapping("/{id}")
    public GameResponse updateGame(@PathVariable Long id, @Valid @RequestBody GameRequest request) {
        Game game = findGameOrThrow(id);

        game.setRawgId(request.rawgId());
        game.setName(request.name());
        game.setSlug(request.slug());
        game.setGenre(request.genre());
        game.setReleaseYear(request.releaseYear());
        game.setReleasedAt(request.releasedAt());
        game.setBackgroundImageUrl(request.backgroundImageUrl());
        game.setPlatforms(resolvePlatforms(request.platformIds()));

        try {
            Game savedGame = gameRepository.save(game);
            return toResponse(savedGame);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A game already exists with this rawgId.");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable Long id) {
        Game game = findGameOrThrow(id);
        gameRepository.delete(game);
        return ResponseEntity.noContent().build();
    }

    private Game findGameOrThrow(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found: " + id));
    }

    private Set<Platform> resolvePlatforms(Set<Long> platformIds) {
        if (platformIds == null || platformIds.isEmpty()) {
            return new HashSet<>();
        }

        List<Platform> platforms = platformRepository.findAllById(platformIds);
        if (platforms.size() != platformIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "One or more platformIds do not exist.");
        }

        return new HashSet<>(platforms);
    }

    private GameResponse toResponse(Game game) {
        Set<Long> platformIds = game.getPlatforms().stream()
                .map(Platform::getId)
                .collect(Collectors.toSet());

        return new GameResponse(
                game.getId(),
                game.getRawgId(),
                game.getName(),
                game.getSlug(),
                game.getGenre(),
                game.getReleaseYear(),
                game.getReleasedAt(),
                game.getBackgroundImageUrl(),
                platformIds);
    }

    public record GameRequest(
            Long rawgId,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 200) String slug,
            @Size(max = 80) String genre,
            Integer releaseYear,
            LocalDate releasedAt,
            @Size(max = 500) String backgroundImageUrl,
            Set<Long> platformIds) {
    }

    public record GameResponse(
            Long id,
            Long rawgId,
            String name,
            String slug,
            String genre,
            Integer releaseYear,
            LocalDate releasedAt,
            String backgroundImageUrl,
            Set<Long> platformIds) {
    }
}
