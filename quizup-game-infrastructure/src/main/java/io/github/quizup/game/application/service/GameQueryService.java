package io.github.quizup.game.application.service;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.infrastructure.axon.PageResponseTypes;
import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.exception.GameExceptions;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.port.in.GetGameEventsUseCase;
import io.github.quizup.game.domain.port.in.GetGameUseCase;
import io.github.quizup.game.domain.port.in.GetGamesByUserUseCase;
import io.github.quizup.game.domain.port.in.SearchGameUseCase;
import io.github.quizup.game.domain.query.GameQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class GameQueryService implements GetGameUseCase, GetGamesByUserUseCase, GetGameEventsUseCase, SearchGameUseCase {

    private final QueryGateway queryGateway;

    public GameQueryService(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Override
    public CompletableFuture<Game> getById(GameQuery.GetGameByIdQuery query) throws GameExceptions.GameNotFoundProblem {
        return queryGateway.query(query, ResponseTypes.instanceOf(Game.class));
    }

    @Override
    public CompletableFuture<List<Game>> getByUser(GameQuery.GetGamesByUserIdAndStatusQuery query) {
        return queryGateway.query(query, ResponseTypes.multipleInstancesOf(Game.class));
    }

    @Override
    public CompletableFuture<List<GameEvent>> getEvents(GameQuery.GetGameEventsQuery query) {
        return queryGateway.query(query, ResponseTypes.multipleInstancesOf(GameEvent.class));
    }

    @Override
    public CompletableFuture<PageResult<Game>> search(GameQuery.SearchGameQuery query) {
        return queryGateway.query(query, PageResponseTypes.pageResultOf(Game.class));
    }
}

