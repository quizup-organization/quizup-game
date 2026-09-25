package io.github.quizup.game.application.service;

import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.microservice.core.domain.model.notification.NotificationEnvelope;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.exception.GameExceptions;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.port.in.GetGameEventsUseCase;
import io.github.quizup.game.domain.port.in.GetGameUseCase;
import io.github.quizup.game.domain.port.in.SearchGameUseCase;
import io.github.quizup.game.domain.query.GameQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class GameQueryService implements GetGameUseCase, GetGameEventsUseCase, SearchGameUseCase {

    private final QueryGateway queryGateway;

    public GameQueryService(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Override
    public CompletableFuture<Game> getById(GameQuery.GetGameByIdQuery query) throws GameExceptions.GameNotFoundProblem {
        return queryGateway.query(query, QueryResponseTypes.instanceOf(Game.class));
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<List<NotificationEnvelope<GameEvent>>> getEvents(GameQuery.GetGameEventsQuery query) {
        return queryGateway
                .query(query, QueryResponseTypes.multipleInstancesOf(NotificationEnvelope.class))
                .thenApply(result -> (List<NotificationEnvelope<GameEvent>>) (List<?>) result);
    }

    @Override
    public CompletableFuture<SearchResponse<Game>> search(GameQuery.SearchGameQuery query) {
        return queryGateway.query(query, QueryResponseTypes.searchResponseOf(Game.class));
    }
}
