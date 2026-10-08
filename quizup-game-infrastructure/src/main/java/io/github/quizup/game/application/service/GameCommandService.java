package io.github.quizup.game.application.service;

import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.port.in.AnswerQuestionUseCase;
import io.github.quizup.game.domain.port.in.CancelGameUseCase;
import io.github.quizup.game.domain.port.in.CreateGameUseCase;
import io.github.quizup.game.domain.port.in.ForfeitGameUseCase;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class GameCommandService implements CreateGameUseCase,
        ForfeitGameUseCase, AnswerQuestionUseCase, CancelGameUseCase {

    private final CommandGateway commandGateway;

    public GameCommandService(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @Override
    public CompletableFuture<String> create(GameCommand.CreateGameCommand command) {
        return commandGateway.send(command);
    }

    @Override
    public CompletableFuture<String> forfeit(GameCommand.ForfeitGameCommand command) {
        return commandGateway.send(command);
    }

    @Override
    public CompletableFuture<String> answer(GameCommand.AnswerQuestionCommand command) {
        return commandGateway.send(command);
    }

    @Override
    public CompletableFuture<String> cancel(GameCommand.CancelGameCommand command) {
        return commandGateway.send(command);
    }
}
