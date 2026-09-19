package io.github.quizup.game.domain.query;

import io.github.quizup.microservice.core.domain.model.search.FilterCriteria;
import io.github.quizup.microservice.core.domain.model.search.PageCriteria;
import io.github.quizup.microservice.core.domain.model.search.SortCriteria;
import io.github.quizup.microservice.core.domain.query.SearchQuery;

import java.util.List;

public interface GameQuery {
    record SearchGameQuery(
            List<FilterCriteria> filters,
            List<SortCriteria> sorts,
            PageCriteria page
    ) implements GameQuery, SearchQuery {

    }

    record GetGameByIdQuery(String gameId) implements GameQuery {
    }

    record GetGameEventsQuery(String gameId) implements GameQuery {
    }
}
