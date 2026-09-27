package io.github.quizup.game.domain.query;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;

import java.time.Instant;

public interface GameQuery {
    record SearchGameQuery(SearchRequest request) implements GameQuery {
    }

    record GetGameByIdQuery(String gameId) implements GameQuery {
    }

    record GetGameEventsQuery(String gameId) implements GameQuery {
    }

    /**
     * Informations minimales d'une partie (validation d'un run asynchrone par un autre service).
     */
    record GetGameRunInfoQuery(String gameId) implements GameQuery {
    }

    /**
     * Historique paginé des parties d'un joueur.
     * {@code topicId} et {@code opponentId} sont optionnels ({@code null} = pas de filtre).
     */
    record GetPlayerGamesQuery(
            String playerId,
            String topicId,
            String opponentId,
            int page,
            int size
    ) implements GameQuery {
    }

    /**
     * Thèmes les plus joués depuis {@code since} (comptage des parties créées).
     */
    record GetPopularTopicsQuery(Instant since, int limit) implements GameQuery {
    }
}
