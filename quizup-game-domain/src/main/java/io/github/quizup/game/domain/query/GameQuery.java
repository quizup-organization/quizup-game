package io.github.quizup.game.domain.query;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;

public interface GameQuery {
    record SearchGameQuery(SearchRequest request) implements GameQuery {
    }

    record GetGameByIdQuery(String gameId) implements GameQuery {
    }

    record GetGameEventsQuery(String gameId) implements GameQuery {
    }
}
