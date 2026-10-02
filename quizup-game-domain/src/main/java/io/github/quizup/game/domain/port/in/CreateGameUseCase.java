package io.github.quizup.game.domain.port.in;

import io.github.quizup.game.domain.command.GameCommand;

import java.util.concurrent.CompletableFuture;

public interface CreateGameUseCase {

    CompletableFuture<String> create(GameCommand.CreateGameCommand command);
}
