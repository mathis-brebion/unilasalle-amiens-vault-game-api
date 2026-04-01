package fr.nexusproject.vault_game_api.controllers;

import fr.nexusproject.vault_game_api.models.AppUser;
import fr.nexusproject.vault_game_api.repository.AppUserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
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
@RequestMapping("/api/users")
public class AppUserController {

    private final AppUserRepository appUserRepository;

    public AppUserController(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @GetMapping
    public Page<AppUserResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return appUserRepository.findAll(pageable).map(this::toResponse);
    }

    @GetMapping("/{id}")
    public AppUserResponse getUserById(@PathVariable Long id) {
        AppUser user = findUserOrThrow(id);
        return toResponse(user);
    }

    @PostMapping
    public ResponseEntity<AppUserResponse> createUser(@Valid @RequestBody CreateAppUserRequest request) {
        AppUser user = new AppUser(request.keycloakSubject(), request.email(), request.username());

        try {
            AppUser savedUser = appUserRepository.save(user);
            return ResponseEntity
                    .created(URI.create("/api/users/" + savedUser.getId()))
                    .body(toResponse(savedUser));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A user already exists with this keycloakSubject, email, or username.");
        }
    }

    @PutMapping("/{id}")
    public AppUserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UpdateAppUserRequest request) {
        AppUser user = findUserOrThrow(id);
        user.setEmail(request.email());
        user.setUsername(request.username());

        try {
            AppUser savedUser = appUserRepository.save(user);
            return toResponse(savedUser);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A user already exists with this email or username.");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        AppUser user = findUserOrThrow(id);
        appUserRepository.delete(user);
        return ResponseEntity.noContent().build();
    }

    private AppUser findUserOrThrow(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    private AppUserResponse toResponse(AppUser user) {
        return new AppUserResponse(
                user.getId(),
                user.getKeycloakSubject(),
                user.getEmail(),
                user.getUsername(),
                user.getCreatedAt());
    }

    public record CreateAppUserRequest(
            @NotBlank @Size(max = 64) String keycloakSubject,
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Size(max = 50) String username) {
    }

    public record UpdateAppUserRequest(
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Size(max = 50) String username) {
    }

    public record AppUserResponse(
            Long id,
            String keycloakSubject,
            String email,
            String username,
            java.time.Instant createdAt) {
    }
}