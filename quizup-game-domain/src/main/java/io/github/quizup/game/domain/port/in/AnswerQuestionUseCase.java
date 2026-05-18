package io.github.quizup.game.domain.port.in;

import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.model.GameQuestionChoice;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public interface AnswerQuestionUseCase {

    CompletableFuture<String> answer(GameCommand.AnswerQuestionCommand command);

    default CompletableFuture<String> answer(String gameId, String playerId, GameQuestionChoice choice) {
        return answer(new GameCommand.AnswerQuestionCommand(gameId, playerId, choice, Instant.now()));
    }
}

