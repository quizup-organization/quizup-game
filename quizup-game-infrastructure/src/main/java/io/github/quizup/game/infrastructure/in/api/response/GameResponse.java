package io.github.quizup.game.infrastructure.in.api.response;

import io.github.quizup.game.domain.model.GameMode;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameStatus;

import java.time.Instant;
import java.util.List;

/**
 * DTO de réponse pour une partie.
 */
public record GameResponse(
        String gameId,
        String topicId,
        String player1Id,
        String player1Name,
        String player2Id,
        String player2Name,
        GameMode mode,
        GamePlayerType opponent,
        GameStatus status,
        int player1Score,
        int player2Score,
        String winnerId,
        Instant createdAt,
        List<GameRoundResponse> rounds
) {
}
