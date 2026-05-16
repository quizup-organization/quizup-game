package io.github.quizup.game.domain.port.out;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.domain.model.search.SearchCriteria;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.model.GameStatus;

import java.util.List;
import java.util.Optional;

public interface GameRepositoryPort {

    void save(Game game);

    Optional<Game> findById(String gameId);

    List<Game> findByUserId(String userId);

    List<Game> findByUserIdAndStatus(String userId, GameStatus status);

    PageResult<Game> findAll(SearchCriteria searchCriteria);
}

