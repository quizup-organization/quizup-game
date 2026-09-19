package io.github.quizup.game.domain.event;

import io.github.quizup.game.domain.model.*;

import java.time.Instant;
import java.util.List;

/**
 * Événements du domaine Game.
 *
 * <p>Convention de temps : chaque événement de phase porte l'instant absolu de la phase
 * <b>suivante</b> ({@code firstRoundAt}, {@code revealAt}, {@code nextRoundAt}). Le client
 * n'anime qu'en direction de ces instants — il ne duplique aucune durée d'animation.
 */
public interface GameEvent {
    String gameId();

    record GameCreatedEvent(
            String gameId,
            String topicId,
            String player1Id,
            String player1Name,
            String player2Id,
            String player2Name,
            GamePlayerType player2Type,
            GameMode mode,
            List<GameQuestion> questions,
            BotDifficulty botDifficulty,
            String ghostGameId,
            Instant createdAt
    ) implements GameEvent {
    }

    record GameJoinedEvent(
            String gameId,
            String playerId,
            Instant joinedAt
    ) implements GameEvent {
    }

    record GameLeftEvent(
            String gameId,
            String playerId,
            String reason,
            Instant leftAt
    ) implements GameEvent {
    }

    record GameStartedEvent(
            String gameId,
            GameMode mode,
            Instant startedAt,
            Instant firstRoundAt
    ) implements GameEvent {
    }

    record GameCancelledEvent(
            String gameId,
            String reason,
            Instant cancelledAt
    ) implements GameEvent {
    }

    /**
     * La question est affichée. {@code revealAt} est l'instant absolu où les réponses
     * seront révélées et le chrono armé.
     */
    record RoundStartedEvent(
            String gameId,
            GameRoundType round,
            GameQuestion question,
            Instant shownAt,
            Instant revealAt
    ) implements GameEvent {
    }

    /**
     * Les réponses sont révélées et le chrono démarre ; il expire à {@code answerDeadlineAt}.
     * C'est le « timer started » du round, et le seul moment où répondre devient possible.
     */
    record QuestionRevealedEvent(
            String gameId,
            GameRoundType round,
            Instant revealedAt,
            Instant answerDeadlineAt
    ) implements GameEvent {
    }

    record QuestionAnsweredEvent(
            String gameId,
            GameRoundType round,
            String questionId,
            String playerId,
            GamePlayerType playerType,
            GameQuestionChoice choice,
            boolean correct,
            Instant answeredAt,
            int pointsEarned,
            long timeMs
    ) implements GameEvent {
    }

    /**
     * Le round est clos ; {@code nextRoundAt} est l'instant absolu de début du round suivant
     * (null si c'était le dernier).
     */
    record RoundClosedEvent(
            String gameId,
            GameRoundType closedRound,
            GameRoundType nextRound,
            GameQuestionChoice correctAnswer,
            Instant closedAt,
            Instant nextRoundAt
    ) implements GameEvent {
    }

    record GameEndedEvent(
            String gameId,
            String winnerId,
            String player1Id,
            String player1Name,
            String player2Id,
            String player2Name,
            String topicId,
            int player1FinalScore,
            int player2FinalScore,
            int player1CorrectAnswers,
            int player1FastAnswers,
            int player2CorrectAnswers,
            int player2FastAnswers,
            String forfeitById,
            Instant endedAt
    ) implements GameEvent {
    }

    /**
     * Fin d'un run asynchrone solo (perspective « record ») : le score est enregistré pour être
     * rejoué plus tard. Aucun XP n'est attribué à ce stade — c'est le replay qui fait foi.
     */
    record GameRunRecordedEvent(
            String gameId,
            String playerId,
            String topicId,
            int score,
            Instant recordedAt
    ) implements GameEvent {
    }
}
