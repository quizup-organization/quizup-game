package io.github.quizup.game.domain.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record GameRound(
        GameRoundType round,
        String questionId,
        String questionText,
        GameQuestionChoice correctAnswer,
        GameQuestionChoice player1Choice,
        int player1Points,
        GameQuestionChoice player2Choice,
        int player2Points,
        GameRoundStatus status
) {
}

