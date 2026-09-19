package io.github.quizup.game.domain.port.in;

import io.github.quizup.game.domain.command.GameCommand;

import java.util.concurrent.CompletableFuture;

public interface JoinGameUseCase {

    CompletableFuture<String> join(GameCommand.JoinGameCommand command);

    default CompletableFuture<String> join(String gameId, String playerId) {
        return join(new GameCommand.JoinGameCommand(gameId, playerId));
    }
}

