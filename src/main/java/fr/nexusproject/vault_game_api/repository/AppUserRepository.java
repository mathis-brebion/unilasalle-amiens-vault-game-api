package fr.nexusproject.vault_game_api.repository;

import fr.nexusproject.vault_game_api.models.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByKeycloakSubject(String keycloakSubject);

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByUsername(String username);
}
