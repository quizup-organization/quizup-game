package io.github.quizup.game.infrastructure.in.api.request;

import io.github.quizup.game.domain.model.GameQuestionChoice;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de requête pour répondre à une question.
 */
public record AnswerQuestionRequest(
        @NotNull GameQuestionChoice choice,

        @NotBlank String playerId
) {
}

