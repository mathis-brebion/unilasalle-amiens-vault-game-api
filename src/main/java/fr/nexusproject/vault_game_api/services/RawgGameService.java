package fr.nexusproject.vault_game_api.services;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

@Service
public class RawgGameService {

    private final RestClient restClient;
    private final String apiKey;

    public RawgGameService(
            @Value("${rawg.api.base-url:https://api.rawg.io/api}") String baseUrl,
            @Value("${rawg.api.key:}") String apiKey) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public List<RawgGameSummary> searchGames(String query, int size) {
        ensureApiKeyConfigured();

        try {
            JsonNode root = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/games")
                            .queryParam("key", apiKey)
                            .queryParam("search", query)
                            .queryParam("page_size", size)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null) {
                return List.of();
            }

            JsonNode results = root.path("results");
            if (!results.isArray()) {
                return List.of();
            }

            List<RawgGameSummary> summaries = new ArrayList<>();
            for (JsonNode gameNode : results) {
                Long rawgId = longValue(gameNode, "id");
                String name = textValue(gameNode, "name");
                if (rawgId == null || name == null || name.isBlank()) {
                    continue;
                }

                summaries.add(new RawgGameSummary(
                        rawgId,
                        name,
                        parseDate(textValue(gameNode, "released")),
                        textValue(gameNode, "background_image")));
            }

            return summaries;
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "RAWG API returned an error while searching games.",
                    exception);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "RAWG API is unavailable.",
                    exception);
        }
    }

    public RawgGameDetails getGameDetails(Long rawgId) {
        ensureApiKeyConfigured();

        try {
            JsonNode gameNode = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/games/{id}")
                            .queryParam("key", apiKey)
                            .build(rawgId))
                    .retrieve()
                    .body(JsonNode.class);

            if (gameNode == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "RAWG API returned an empty response.");
            }

            Long resolvedRawgId = longValue(gameNode, "id");
            String name = textValue(gameNode, "name");
            if (resolvedRawgId == null || name == null || name.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "RAWG API response is missing required fields.");
            }

            List<String> genres = extractGenres(gameNode);
            Set<String> platformNames = extractPlatformNames(gameNode);
            LocalDate releasedAt = parseDate(textValue(gameNode, "released"));

            return new RawgGameDetails(
                    resolvedRawgId,
                    name,
                    textValue(gameNode, "slug"),
                    genres.isEmpty() ? null : genres.getFirst(),
                    genres,
                    releasedAt != null ? releasedAt.getYear() : null,
                    releasedAt,
                    textValue(gameNode, "background_image"),
                    textValue(gameNode, "description_raw"),
                    platformNames);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "RAWG game not found: " + rawgId, exception);
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "RAWG API returned an error while retrieving game details.",
                    exception);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "RAWG API is unavailable.",
                    exception);
        }
    }

    private void ensureApiKeyConfigured() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "RAWG integration is not configured. Missing RAWG_API_KEY.");
        }
    }

    private static Long longValue(JsonNode node, String fieldName) {
        JsonNode valueNode = node.path(fieldName);
        if (valueNode.isMissingNode() || valueNode.isNull() || !valueNode.canConvertToLong()) {
            return null;
        }

        return valueNode.longValue();
    }

    private static String textValue(JsonNode node, String fieldName) {
        JsonNode valueNode = node.path(fieldName);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }

        return valueNode.asText();
    }

    private static LocalDate parseDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(rawDate);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static List<String> extractGenres(JsonNode gameNode) {
        JsonNode genresNode = gameNode.path("genres");
        if (!genresNode.isArray()) {
            return List.of();
        }

        List<String> genres = new ArrayList<>();
        for (JsonNode genreNode : genresNode) {
            String genreName = textValue(genreNode, "name");
            if (genreName != null && !genreName.isBlank()) {
                genres.add(genreName);
            }
        }

        return genres;
    }

    private static Set<String> extractPlatformNames(JsonNode gameNode) {
        JsonNode platformsNode = gameNode.path("platforms");
        if (!platformsNode.isArray()) {
            return Set.of();
        }

        Set<String> platformNames = new LinkedHashSet<>();
        for (JsonNode platformEntry : platformsNode) {
            String platformName = textValue(platformEntry.path("platform"), "name");
            if (platformName != null && !platformName.isBlank()) {
                platformNames.add(platformName);
            }
        }

        return Set.copyOf(platformNames);
    }

    public record RawgGameSummary(
            Long rawgId,
            String name,
            LocalDate releasedAt,
            String backgroundImageUrl) {
    }

    public record RawgGameDetails(
            Long rawgId,
            String name,
            String slug,
            String genre,
            List<String> genres,
            Integer releaseYear,
            LocalDate releasedAt,
            String backgroundImageUrl,
            String description,
            Set<String> platformNames) {
    }
}
