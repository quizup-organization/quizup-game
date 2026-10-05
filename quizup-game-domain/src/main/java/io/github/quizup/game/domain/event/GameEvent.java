package io.github.quizup.game.domain.event;

import io.github.quizup.game.domain.model.*;
import io.github.quizup.microservice.core.domain.model.i18n.Language;

import java.time.Instant;
import java.util.List;
import java.util.Set;

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
            List<GameQuestion> questions,
            BotDifficulty botDifficulty,
            Instant createdAt
    ) implements GameEvent {
    }

    record GameJoinedEvent(
            String gameId,
            String playerId,
            Instant joinedAt
    ) implements GameEvent {
    }

    /**
     * Un joueur a quitté la salle d'attente avant le démarrage ; la partie est annulée
     * ({@code GAME_CANCELLED} suit immédiatement).
     */
    record GameLeftEvent(
            String gameId,
            String playerId,
            Instant leftAt
    ) implements GameEvent {
    }

    record GameStartedEvent(
            String gameId,
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
     * Un joueur a abandonné une partie en cours. L'adversaire est vainqueur ; l'état terminal
     * est porté par le {@link GameEndedEvent} qui suit.
     */
    record GameForfeitedEvent(
            String gameId,
            String forfeiterId,
            Instant forfeitedAt
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
            Instant endedAt
    ) implements GameEvent {
    }

    /**
     * Un joueur a demandé une revanche après la fin de la partie ; la demande expire à
     * l'échéance {@link GameDeadline#REMATCH_EXPIRY} orchestrée par la saga.
     */
    record RematchRequestedEvent(
            String gameId,
            String requesterId,
            String requesterName,
            String opponentId,
            String opponentName,
            String topicId,
            Set<Language> languages,
            Instant requestedAt
    ) implements GameEvent {
    }

    /**
     * L'adversaire a accepté la revanche en attente : la saga crée la nouvelle partie.
     */
    record RematchAcceptedEvent(
            String gameId,
            String playerId,
            Instant acceptedAt
    ) implements GameEvent {
    }

    /**
     * L'adversaire a décliné la revanche en attente : la demande est close.
     */
    record RematchDeclinedEvent(
            String gameId,
            String playerId,
            Instant declinedAt
    ) implements GameEvent {
    }

    /**
     * La revanche en attente est annulée (annulation par le demandeur, sortie d'un joueur,
     * échec de création ou expiration).
     */
    record RematchCancelledEvent(
            String gameId,
            String reason,
            Instant cancelledAt
    ) implements GameEvent {
    }

    /**
     * La nouvelle partie de la revanche est créée ; {@code newGameId} porte son identifiant.
     */
    record RematchStartedEvent(
            String gameId,
            String newGameId,
            Instant startedAt
    ) implements GameEvent {
    }
}
