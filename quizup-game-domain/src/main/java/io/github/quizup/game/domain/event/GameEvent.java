package io.github.quizup.game.domain.event;

import io.github.quizup.game.domain.model.*;

import java.time.Instant;
import java.util.List;

public interface GameEvent {
    String gameId();

    record GameCreatedEvent(
            String gameId,
            String topicId,
            String player1Id,
            String player2Id,
            GamePlayerType player2Type,
            GameMode mode,
            List<GameQuestion> questions,
            Instant createdAt
    ) implements GameEvent {
    }

    record GameJoinedEvent(
            String gameId,
            String playerId,
            Instant joinedAt
    ) implements GameEvent {
    }

    record GameStartedEvent(
            String gameId,
            GameMode mode,
            Instant startedAt
    ) implements GameEvent {
    }

    record GameCancelledEvent(
            String gameId,
            String reason,
            Instant cancelledAt
    ) implements GameEvent {
    }

    record RoundStartedEvent(
            String gameId,
            GameRoundType round,
            GameQuestion question,
            Instant startedAt
    ) implements GameEvent {
    }

    record QuestionAnsweredEvent(
            String gameId,
            GameRoundType round,
            String playerId,
            GameQuestionChoice choice,
            boolean correct,
            Instant answeredAt,
            int pointsEarned
    ) implements GameEvent {
    }

    record RoundClosedEvent(
            String gameId,
            GameRoundType closedRound,
            GameRoundType nextRound
    ) implements GameEvent {
    }

    record GameEndedEvent(
            String gameId,
            String winnerId,
            String player1Id,
            String player2Id,
            String topicId,
            int player1FinalScore,
            int player2FinalScore,
            Instant endedAt
    ) implements GameEvent {
    }
}
