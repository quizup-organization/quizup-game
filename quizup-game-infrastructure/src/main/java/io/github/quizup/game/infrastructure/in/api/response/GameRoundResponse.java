package io.github.quizup.game.infrastructure.in.api.response;

import io.github.quizup.game.domain.model.GameRoundType;
import io.github.quizup.game.domain.model.GameRoundStatus;
import io.github.quizup.topic.domain.model.QuestionChoice;

/**
 * DTO de réponse pour un round de partie.
 */
public record GameRoundResponse(
        GameRoundType round,
        String questionText,
        GameRoundStatus status,
        QuestionChoice player1Choice,
        int player1Points,
        QuestionChoice player2Choice,
        int player2Points,
        QuestionChoice correctAnswer
) {
}
