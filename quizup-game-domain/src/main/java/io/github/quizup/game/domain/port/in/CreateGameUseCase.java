package io.github.quizup.game.domain.port.in;

import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.model.GameMode;
import io.github.quizup.game.domain.model.GamePlayerType;

import java.util.concurrent.CompletableFuture;

public interface CreateGameUseCase {

    CompletableFuture<String> create(GameCommand.CreateGameCommand command);

    default CompletableFuture<String> create(String gameId,
                                           String topicId,
                                           String player1Id,
                                           String player2Id,
                                           GameMode mode,
                                           GamePlayerType player2Type) {
        return create(new GameCommand.CreateGameCommand(gameId, topicId, player1Id, player2Id, mode, player2Type));
    }
}

