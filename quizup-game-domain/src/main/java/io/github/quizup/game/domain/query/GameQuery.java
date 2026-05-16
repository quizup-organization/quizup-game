package io.github.quizup.game.domain.query;

import io.github.quizup.common.domain.model.search.FilterCriteria;
import io.github.quizup.common.domain.model.search.PageCriteria;
import io.github.quizup.common.domain.model.search.SortCriteria;
import io.github.quizup.common.domain.query.SearchQuery;
import io.github.quizup.game.domain.model.GameStatus;

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

    record GetGamesByUserIdQuery(String userId) implements GameQuery {
    }

    record GetGamesByUserIdAndStatusQuery(String userId, GameStatus status) implements GameQuery {
    }

    record GetGameEventsQuery(String gameId) implements GameQuery {
    }
}

