package io.github.quizup.game.domain.model;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder(toBuilder = true)
public record Game(
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
        Instant startedAt,
        Instant endedAt,
        List<GameRound> rounds
) {
}

