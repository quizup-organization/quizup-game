package io.github.quizup.game.domain.port.in;

import io.github.quizup.game.domain.command.GameCommand;

import java.util.concurrent.CompletableFuture;

public interface AbandonGameUseCase {

    CompletableFuture<String> abandon(GameCommand.EndGameCommand command);

    default CompletableFuture<String> abandon(String gameId, String playerId) {
        return abandon(new GameCommand.EndGameCommand(gameId, playerId));
    }
}
