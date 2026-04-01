package fr.nexusproject.vault_game_api.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        @Index(name = "idx_games_slug", columnList = "slug"),
        @Index(name = "idx_games_name", columnList = "name"),
        @Index(name = "idx_games_genre", columnList = "genre"),
        @Index(name = "idx_games_release_year", columnList = "release_year")
})
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rawg_id", unique = true)
    private Long rawgId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 200)
    private String slug;

    @Column(length = 80)
    private String genre;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column(name = "released_at")
    private LocalDate releasedAt;

    @Column(name = "background_image_url", length = 500)
    private String backgroundImageUrl;

    @ManyToMany
    @JoinTable(name = "game_platforms", joinColumns = @JoinColumn(name = "game_id"), inverseJoinColumns = @JoinColumn(name = "platform_id"))
    private Set<Platform> platforms = new HashSet<>();

    @OneToMany(mappedBy = "game")
    private List<UserGame> userGames = new ArrayList<>();

    public Game(Long rawgId,
            String name,
            String slug,
            String genre,
            Integer releaseYear,
            LocalDate releasedAt,
            String backgroundImageUrl) {
        this.rawgId = rawgId;
        this.name = name;
        this.slug = slug;
        this.genre = genre;
        this.releaseYear = releaseYear;
        this.releasedAt = releasedAt;
        this.backgroundImageUrl = backgroundImageUrl;
    }
}
