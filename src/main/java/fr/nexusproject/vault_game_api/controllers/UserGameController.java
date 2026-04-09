package fr.nexusproject.vault_game_api.controllers;

import fr.nexusproject.vault_game_api.models.AppUser;
import fr.nexusproject.vault_game_api.models.Game;
import fr.nexusproject.vault_game_api.models.Platform;
import fr.nexusproject.vault_game_api.models.UserGame;
import fr.nexusproject.vault_game_api.models.UserGameStatus;
import fr.nexusproject.vault_game_api.repository.AppUserRepository;
import fr.nexusproject.vault_game_api.repository.GameRepository;
import fr.nexusproject.vault_game_api.repository.PlatformRepository;
import fr.nexusproject.vault_game_api.repository.UserGameRepository;
import fr.nexusproject.vault_game_api.services.RawgGameService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/users/{userId}/games")
public class UserGameController {

    private final UserGameRepository userGameRepository;
    private final AppUserRepository appUserRepository;
    private final GameRepository gameRepository;
    private final PlatformRepository platformRepository;
    private final RawgGameService rawgGameService;

    public UserGameController(UserGameRepository userGameRepository,
            AppUserRepository appUserRepository,
            GameRepository gameRepository,
            PlatformRepository platformRepository,
            RawgGameService rawgGameService) {
        this.userGameRepository = userGameRepository;
        this.appUserRepository = appUserRepository;
        this.gameRepository = gameRepository;
        this.platformRepository = platformRepository;
        this.rawgGameService = rawgGameService;
    }

    @GetMapping
    public Page<UserGameResponse> getUserLibrary(
            @PathVariable Long userId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ensureUserExists(userId);
        Pageable pageable = PageRequest.of(page, size);

        Page<UserGame> results = (search == null || search.isBlank())
                ? userGameRepository.findByUserId(userId, pageable)
                : userGameRepository.searchByUserIdAndGameTitle(userId, search, pageable);

        return results.map(this::toResponse);
    }

    @PostMapping
    public ResponseEntity<UserGameResponse> addGameToUserLibrary(
            @PathVariable Long userId,
            @Valid @RequestBody AddUserGameRequest request) {
        AppUser user = findUserOrThrow(userId);
        Game game = findOrCreateGameByRawgId(request.rawgId());
        UserGameStatus status = request.status() != null ? request.status() : UserGameStatus.WISHLIST;

        UserGame userGame = userGameRepository.findByUserIdAndGameId(userId, game.getId())
                .map(existing -> {
                    existing.setStatus(status);
                    return existing;
                })
                .orElseGet(() -> new UserGame(user, game, status));

        UserGame saved = userGameRepository.save(userGame);
        return ResponseEntity
                .created(URI.create("/api/users/" + userId + "/games/" + game.getId()))
                .body(toResponse(saved));
    }

    @PatchMapping("/{gameId}/status")
    public UserGameResponse updateUserGameStatus(
            @PathVariable Long userId,
            @PathVariable Long gameId,
            @Valid @RequestBody UpdateUserGameStatusRequest request) {
        ensureUserExists(userId);

        UserGame userGame = userGameRepository.findByUserIdAndGameId(userId, gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No library entry found for user " + userId + " and game " + gameId));

        userGame.setStatus(request.status());
        UserGame saved = userGameRepository.save(userGame);
        return toResponse(saved);
    }

    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> removeGameFromUserLibrary(@PathVariable Long userId, @PathVariable Long gameId) {
        ensureUserExists(userId);

        if (!userGameRepository.existsByUserIdAndGameId(userId, gameId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No library entry found for user " + userId + " and game " + gameId);
        }

        userGameRepository.deleteByUserIdAndGameId(userId, gameId);
        return ResponseEntity.noContent().build();
    }

    private void ensureUserExists(Long userId) {
        if (!appUserRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId);
        }
    }

    private AppUser findUserOrThrow(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    private Game findGameOrThrow(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found: " + id));
    }

    private Game findOrCreateGameByRawgId(Long rawgId) {
        return gameRepository.findByRawgId(rawgId).orElseGet(() -> createGameFromRawg(rawgId));
    }

    private Game createGameFromRawg(Long rawgId) {
        RawgGameService.RawgGameDetails rawgGame = rawgGameService.getGameDetails(rawgId);

        Game game = new Game(
                rawgGame.rawgId(),
                rawgGame.name(),
                rawgGame.slug(),
                rawgGame.genre(),
                rawgGame.releaseYear(),
                rawgGame.releasedAt(),
                rawgGame.backgroundImageUrl());
        game.setPlatforms(resolvePlatforms(rawgGame.platformNames()));

        try {
            return gameRepository.save(game);
        } catch (DataIntegrityViolationException exception) {
            return gameRepository.findByRawgId(rawgId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "A game already exists with this rawgId.",
                            exception));
        }
    }

    private Set<Platform> resolvePlatforms(Set<String> platformNames) {
        if (platformNames == null || platformNames.isEmpty()) {
            return Set.of();
        }

        return platformNames.stream()
                .map(platformRepository::findByNameIgnoreCase)
                .flatMap(java.util.Optional::stream)
                .collect(Collectors.toSet());
    }

    private UserGameResponse toResponse(UserGame userGame) {
        return new UserGameResponse(
                userGame.getId(),
                userGame.getUser().getId(),
                userGame.getGame().getId(),
                userGame.getGame().getName(),
                userGame.getStatus(),
                userGame.getAddedAt());
    }

    public record AddUserGameRequest(
            @NotNull Long rawgId,
            UserGameStatus status) {
    }

    public record UpdateUserGameStatusRequest(@NotNull UserGameStatus status) {
    }

    public record UserGameResponse(
            Long id,
            Long userId,
            Long gameId,
            String gameName,
            UserGameStatus status,
            java.time.Instant addedAt) {
    }
}
