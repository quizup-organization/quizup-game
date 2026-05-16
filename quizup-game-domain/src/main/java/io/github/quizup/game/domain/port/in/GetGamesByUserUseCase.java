package io.github.quizup.game.domain.port.in;

import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.model.GameStatus;
import io.github.quizup.game.domain.query.GameQuery;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface GetGamesByUserUseCase {

    CompletableFuture<List<Game>> getByUser(GameQuery.GetGamesByUserIdAndStatusQuery query);

    default CompletableFuture<List<Game>> getByUser(String userId, GameStatus status) {
        return getByUser(new GameQuery.GetGamesByUserIdAndStatusQuery(userId, status));
    }
}

