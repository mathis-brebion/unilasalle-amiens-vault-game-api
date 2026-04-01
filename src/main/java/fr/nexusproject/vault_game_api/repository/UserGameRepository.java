package fr.nexusproject.vault_game_api.repository;

import fr.nexusproject.vault_game_api.models.UserGame;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserGameRepository extends JpaRepository<UserGame, Long> {

    Page<UserGame> findByUserId(Long userId, Pageable pageable);

    @Query("""
            SELECT ug
            FROM UserGame ug
            WHERE ug.user.id = :userId
            AND LOWER(ug.game.name) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<UserGame> searchByUserIdAndGameTitle(@Param("userId") Long userId,
            @Param("search") String search,
            Pageable pageable);

    boolean existsByUserIdAndGameId(Long userId, Long gameId);

    Optional<UserGame> findByUserIdAndGameId(Long userId, Long gameId);

    void deleteByUserIdAndGameId(Long userId, Long gameId);
}
