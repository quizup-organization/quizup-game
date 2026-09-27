package io.github.quizup.game.domain.port.in;

import io.github.quizup.game.domain.query.GameQuery;
import io.github.quizup.microservice.core.domain.model.notification.EventEnvelope;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface GetGameEventsUseCase {

    CompletableFuture<List<EventEnvelope>> getEvents(GameQuery.GetGameEventsQuery query);

    default CompletableFuture<List<EventEnvelope>> getEvents(String gameId) {
        return getEvents(new GameQuery.GetGameEventsQuery(gameId));
    }
}
