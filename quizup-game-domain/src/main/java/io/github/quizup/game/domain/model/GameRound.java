package io.github.quizup.game.domain.model;

import lombok.Builder;

import java.time.Instant;

@Builder(toBuilder = true)
public record GameRound(
        GameRoundType round,
        String questionId,
        String questionText,
        GameQuestionChoice correctAnswer,
        GameQuestionChoice player1Choice,
        int player1Points,
        Long player1TimeMs,
        GameQuestionChoice player2Choice,
        int player2Points,
        Long player2TimeMs,
        GameRoundStatus status,
        Instant revealedAt,
        Instant answerDeadlineAt
) {
}
