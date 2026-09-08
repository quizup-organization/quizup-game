package io.github.quizup.game.application.handler.query;

import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.game.domain.exception.GameExceptions;
import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.port.out.GameEventStorePort;
import io.github.quizup.game.domain.port.out.GameRepositoryPort;
import io.github.quizup.game.domain.query.GameQuery;
import org.axonframework.queryhandling.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

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
    public List<Game> handle(GameQuery.GetGamesByUserIdQuery query) {
        logger.debug("Handling GetGamesByUserIdQuery: userId={}", query.userId());
        return gameRepositoryPort.findByUserId(query.userId());
    }

    @QueryHandler
    public List<Game> handle(GameQuery.GetGamesByUserIdAndStatusQuery query) {
        logger.debug("Handling GetGamesByUserIdAndStatusQuery: userId={}, status={}", query.userId(), query.status());
        return gameRepositoryPort.findByUserIdAndStatus(query.userId(), query.status());
    }

    @QueryHandler
    public List<GameEvent> handle(GameQuery.GetGameEventsQuery query) {
        logger.debug("Handling GetGameEventsQuery: gameId={}", query.gameId());
        return gameEventStorePort.findEventsByGameId(query.gameId());
    }

    @QueryHandler
    public PageResult<Game> handle(GameQuery.SearchGameQuery query) {
        return gameRepositoryPort.findAll(query);
    }
}
