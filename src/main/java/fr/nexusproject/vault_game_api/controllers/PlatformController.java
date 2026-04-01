package fr.nexusproject.vault_game_api.controllers;

import fr.nexusproject.vault_game_api.models.Platform;
import fr.nexusproject.vault_game_api.repository.PlatformRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
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
@RequestMapping("/api/platforms")
public class PlatformController {

    private final PlatformRepository platformRepository;

    public PlatformController(PlatformRepository platformRepository) {
        this.platformRepository = platformRepository;
    }

    @GetMapping
    public Page<PlatformResponse> getAllPlatforms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return platformRepository.findAll(pageable).map(this::toResponse);
    }

    @GetMapping("/{id}")
    public PlatformResponse getPlatformById(@PathVariable Long id) {
        return toResponse(findPlatformOrThrow(id));
    }

    @PostMapping
    public ResponseEntity<PlatformResponse> createPlatform(@Valid @RequestBody PlatformRequest request) {
        Platform platform = new Platform(request.name());

        try {
            Platform savedPlatform = platformRepository.save(platform);
            return ResponseEntity
                    .created(URI.create("/api/platforms/" + savedPlatform.getId()))
                    .body(toResponse(savedPlatform));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A platform already exists with this name.");
        }
    }

    @PutMapping("/{id}")
    public PlatformResponse updatePlatform(@PathVariable Long id, @Valid @RequestBody PlatformRequest request) {
        Platform platform = findPlatformOrThrow(id);
        platform.setName(request.name());

        try {
            Platform savedPlatform = platformRepository.save(platform);
            return toResponse(savedPlatform);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A platform already exists with this name.");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlatform(@PathVariable Long id) {
        Platform platform = findPlatformOrThrow(id);
        platformRepository.delete(platform);
        return ResponseEntity.noContent().build();
    }

    private Platform findPlatformOrThrow(Long id) {
        return platformRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Platform not found: " + id));
    }

    private PlatformResponse toResponse(Platform platform) {
        return new PlatformResponse(platform.getId(), platform.getName());
    }

    public record PlatformRequest(@NotBlank @Size(max = 80) String name) {
    }

    public record PlatformResponse(Long id, String name) {
    }
}