package io.github.quizup.game.domain.port.in;

import io.github.quizup.microservice.core.infrastructure.in.api.request.SearchRequest;
import io.github.quizup.microservice.core.infrastructure.in.api.response.SearchResponse;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.query.GameQuery;

import java.util.concurrent.CompletableFuture;

public interface SearchGameUseCase {

    CompletableFuture<SearchResponse<Game>> search(GameQuery.SearchGameQuery query);

    default CompletableFuture<SearchResponse<Game>> search(SearchRequest request) {
        return search(new GameQuery.SearchGameQuery(request));
    }
}
