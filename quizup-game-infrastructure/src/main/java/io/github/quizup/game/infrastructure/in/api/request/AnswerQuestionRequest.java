package io.github.quizup.game.infrastructure.in.api.request;

import io.github.quizup.topic.domain.model.QuestionChoice;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de requête pour répondre à une question.
 */
public record AnswerQuestionRequest(
        @NotNull QuestionChoice choice
) {
}

