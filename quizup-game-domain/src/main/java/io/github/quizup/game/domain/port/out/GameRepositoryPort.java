package io.github.quizup.game.domain.port.out;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.model.PlayerGamesPage;
import io.github.quizup.game.domain.model.TopicPopularity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GameRepositoryPort {

    void save(Game game);

    Optional<Game> findById(String gameId);

    /**
     * Partie active la plus récente à laquelle participe le joueur (pour le forfait
     * lorsqu'il passe hors ligne). Vide s'il n'a aucune partie active.
     */
    Optional<Game> findActiveGameByPlayerId(String playerId);

    /**
     * Partie en attente ou en cours la plus récente du joueur (`CREATED/READY/IN_PROGRESS`),
     * pour la reprise depuis l'application.
     */
    Optional<Game> findCurrentGameByPlayerId(String playerId);

    SearchResponse<Game> findAll(SearchRequest request);

    /**
     * Historique paginé des parties d'un joueur, plus récentes d'abord.
     * {@code topicId} / {@code opponentId} optionnels ({@code null} = pas de filtre).
     */
    PlayerGamesPage findPlayerGames(String playerId, String topicId, String opponentId, int page, int size);

    /**
     * Thèmes les plus joués (parties créées) depuis {@code since}, ordre décroissant.
     */
    List<TopicPopularity> findPopularTopics(Instant since, int limit);
}
