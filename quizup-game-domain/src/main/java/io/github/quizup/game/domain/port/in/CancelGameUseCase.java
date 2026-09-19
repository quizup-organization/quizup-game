package io.github.quizup.game.domain.port.in;

import io.github.quizup.game.domain.command.GameCommand;

import java.util.concurrent.CompletableFuture;

public interface CancelGameUseCase {

    CompletableFuture<String> cancel(GameCommand.CancelGameCommand command);

    default CompletableFuture<String> cancel(String gameId, String reason) {
        return cancel(new GameCommand.CancelGameCommand(gameId, reason));
    }
}

