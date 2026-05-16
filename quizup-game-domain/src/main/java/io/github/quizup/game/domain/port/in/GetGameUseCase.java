package io.github.quizup.game.domain.port.in;

import io.github.quizup.game.domain.exception.GameExceptions;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.query.GameQuery;

import java.util.concurrent.CompletableFuture;

public interface GetGameUseCase {

    CompletableFuture<Game> getById(GameQuery.GetGameByIdQuery query) throws GameExceptions.GameNotFoundProblem;

    default CompletableFuture<Game> getById(String gameId) throws GameExceptions.GameNotFoundProblem {
        return getById(new GameQuery.GetGameByIdQuery(gameId));
    }
}

