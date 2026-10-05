package io.github.quizup.game.application.handler.query;

import io.github.quizup.microservice.core.domain.model.notification.EventEnvelope;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.game.domain.exception.GameExceptions;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.model.PlayerGamesPage;
import io.github.quizup.game.domain.model.TopicPopularity;
import io.github.quizup.game.domain.port.out.GameEventStorePort;
import io.github.quizup.game.domain.port.out.GameRepositoryPort;
import io.github.quizup.game.domain.query.GameQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * GameQueryHandler — Répond aux queries en s'appuyant sur les ports sortants.
 */
@Component
public class GameQueryHandler {

    private static final Logger logger = LoggerFactory.getLogger(GameQueryHandler.class);

    private final GameRepositoryPort gameRepositoryPort;
    private final GameEventStorePort gameEventStorePort;

    public GameQueryHandler(GameRepositoryPort gameRepositoryPort,
                            GameEventStorePort gameEventStorePort) {
        this.gameRepositoryPort = gameRepositoryPort;
        this.gameEventStorePort = gameEventStorePort;
    }

    @QueryHandler
    public Game handle(GameQuery.GetGameByIdQuery query) {
        logger.debug("Handling GetGameByIdQuery: gameId={}", query.gameId());
        return gameRepositoryPort.findById(query.gameId())
                .orElseThrow(() -> new GameExceptions.GameNotFoundProblem(query.gameId()));
    }

    @QueryHandler
    public Game handle(GameQuery.GetCurrentGameQuery query) {
        logger.debug("Handling GetCurrentGameQuery: playerId={}", query.playerId());
        return gameRepositoryPort.findCurrentGameByPlayerId(query.playerId())
                .orElseThrow(() -> new GameExceptions.NoCurrentGameProblem(query.playerId()));
    }

    @QueryHandler
    public List<EventEnvelope> handle(GameQuery.GetGameEventsQuery query) {
        logger.debug("Handling GetGameEventsQuery: gameId={}", query.gameId());
        return gameEventStorePort.findEventEnvelopesByGameId(query.gameId());
    }

    @QueryHandler
    public SearchResponse<Game> handle(GameQuery.SearchGameQuery query) {
        return gameRepositoryPort.findAll(query.request());
    }

    @QueryHandler
    public PlayerGamesPage handle(GameQuery.GetPlayerGamesQuery query) {
        logger.debug("Handling GetPlayerGamesQuery: playerId={}, topicId={}", query.playerId(), query.topicId());
        return gameRepositoryPort.findPlayerGames(
                query.playerId(), query.topicId(), query.opponentId(), query.page(), query.size());
    }

    @QueryHandler
    public List<TopicPopularity> handle(GameQuery.GetPopularTopicsQuery query) {
        logger.debug("Handling GetPopularTopicsQuery: since={}, limit={}", query.since(), query.limit());
        return gameRepositoryPort.findPopularTopics(query.since(), query.limit());
    }
}
