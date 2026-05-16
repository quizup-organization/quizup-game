package io.github.quizup.game.domain.port.in;

import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.query.GameQuery;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface GetGameEventsUseCase {

    CompletableFuture<List<GameEvent>> getEvents(GameQuery.GetGameEventsQuery query);

    default CompletableFuture<List<GameEvent>> getEvents(String gameId) {
        return getEvents(new GameQuery.GetGameEventsQuery(gameId));
    }
}

