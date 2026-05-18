package io.github.quizup.game.infrastructure.in.api.response;

import io.github.quizup.game.domain.model.GameQuestionChoice;
import io.github.quizup.game.domain.model.GameRoundStatus;
import io.github.quizup.game.domain.model.GameRoundType;

/**
 * DTO de réponse pour un round de partie.
 */
public record GameRoundResponse(
        GameRoundType round,
        String questionText,
        GameRoundStatus status,
        GameQuestionChoice player1Choice,
        int player1Points,
        GameQuestionChoice player2Choice,
        int player2Points,
        GameQuestionChoice correctAnswer
) {
}
