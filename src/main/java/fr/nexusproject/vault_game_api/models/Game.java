package fr.nexusproject.vault_game_api.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "games", indexes = {
        @Index(name = "idx_games_rawg_id", columnList = "rawg_id"),
        @Index(name = "idx_games_slug", columnList = "slug")
})
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rawg_id", nullable = false, unique = true)
    private Long rawgId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 200)
    private String slug;

    @Column(name = "released_at")
    private LocalDate releasedAt;

    @Column(name = "background_image_url", length = 500)
    private String backgroundImageUrl;

    @OneToMany(mappedBy = "game")
    private List<UserGame> userGames = new ArrayList<>();

    public Game(Long rawgId, String name, String slug, LocalDate releasedAt, String backgroundImageUrl) {
        this.rawgId = rawgId;
        this.name = name;
        this.slug = slug;
        this.releasedAt = releasedAt;
        this.backgroundImageUrl = backgroundImageUrl;
    }
}
