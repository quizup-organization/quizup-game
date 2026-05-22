package io.github.quizup.game.domain.command;

import io.github.quizup.game.domain.model.GameMode;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameQuestionChoice;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.time.Instant;

public interface GameCommand {
    String gameId();

    /**
     * Crée une partie avec les deux joueurs déclarés.
     * player2Id peut être null en mode ASYNC (le deuxième joueur rejoint plus tard).
     * player2Id = "BOT" si player2Type=BOT.
     */
    record CreateGameCommand(
            @TargetAggregateIdentifier String gameId,
            String topicId,
            String player1Id,
            String player2Id,
            GameMode mode,
            GamePlayerType player2Type
    ) implements GameCommand {
    }

    /**
     * Un joueur confirme sa présence dans la partie.
     * Quand les deux sont présents → status passe à READY.
     */
    record JoinGameCommand(
            @TargetAggregateIdentifier String gameId,
            String playerId
    ) implements GameCommand {
    }

    record LeaveGameCommand(
            @TargetAggregateIdentifier String gameId,
            String playerId,
            String reason
    ) implements GameCommand {
    }

    /**
     * Démarre la partie (READY → IN_PROGRESS, ou CREATED → IN_PROGRESS en mode ASYNC).
     */
    record StartGameCommand(
            @TargetAggregateIdentifier String gameId
    ) implements GameCommand {
    }

    record CancelGameCommand(
            @TargetAggregateIdentifier String gameId,
            String reason
    ) implements GameCommand {
    }

    record StartRoundCommand(
            @TargetAggregateIdentifier String gameId
    ) implements GameCommand {
    }

    record AnswerQuestionCommand(
            @TargetAggregateIdentifier String gameId,
            String playerId,
            GameQuestionChoice choice,
            Instant timestamp
    ) implements GameCommand {
    }

    record CloseRoundCommand(
            @TargetAggregateIdentifier String gameId
    ) implements GameCommand {
    }

    record EndGameCommand(
            @TargetAggregateIdentifier String gameId
    ) implements GameCommand {
    }
}
