package io.github.quizup.game.domain.port.in;

import io.github.quizup.microservice.core.domain.model.search.FilterCriteria;
import io.github.quizup.microservice.core.domain.model.search.PageCriteria;
import io.github.quizup.microservice.core.domain.model.search.PageResult;
import io.github.quizup.microservice.core.domain.model.search.SortCriteria;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.query.GameQuery;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SearchGameUseCase {

    CompletableFuture<PageResult<Game>> search(GameQuery.SearchGameQuery query);

    default CompletableFuture<PageResult<Game>> search(List<FilterCriteria> filters,
                                                       List<SortCriteria> sorts,
                                                       PageCriteria page) {
        return search(
                new GameQuery.SearchGameQuery(
                        filters,
                        sorts,
                        page
                )
        );
    }
}

