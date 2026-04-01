package fr.nexusproject.vault_game_api.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_games", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_games_user_game", columnNames = { "user_id", "game_id" })
}, indexes = {
        @Index(name = "idx_user_games_user_id", columnList = "user_id"),
        @Index(name = "idx_user_games_game_id", columnList = "game_id")
})
public class UserGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserGameStatus status;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    public UserGame(AppUser user, Game game, UserGameStatus status) {
        this.user = user;
        this.game = game;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        if (this.addedAt == null) {
            this.addedAt = Instant.now();
        }
        if (this.status == null) {
            this.status = UserGameStatus.WISHLIST;
        }
    }
}
