package fr.nexusproject.vault_game_api.repository;

import fr.nexusproject.vault_game_api.models.Game;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {

    Optional<Game> findByRawgId(Long rawgId);

    Page<Game> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
