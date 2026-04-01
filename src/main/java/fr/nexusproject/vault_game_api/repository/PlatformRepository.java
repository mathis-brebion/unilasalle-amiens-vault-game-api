package fr.nexusproject.vault_game_api.repository;

import fr.nexusproject.vault_game_api.models.Platform;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformRepository extends JpaRepository<Platform, Long> {

    Optional<Platform> findByNameIgnoreCase(String name);
}
