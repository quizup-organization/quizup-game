package io.github.quizup.game.domain.command;

import io.github.quizup.game.domain.model.BotDifficulty;
import io.github.quizup.game.domain.model.GameMode;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameQuestionChoice;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.time.Instant;

public interface GameCommand {
    String gameId();

    /**
     * Crée une partie avec les deux joueurs déclarés.
     * player2Id peut être null en mode ASYNC (run solo : le deuxième joueur est absent).
     * player2Id = "BOT" si player2Type=BOT.
     * ghostGameId référence un run asynchrone enregistré dont les questions sont réutilisées
     * (mode replay) ; null pour un tirage aléatoire ou un run enregistré.
     */
    record CreateGameCommand(
            @TargetAggregateIdentifier String gameId,
            String topicId,
            String player1Id,
            String player1Name,
            String player2Id,
            String player2Name,
            GameMode mode,
            GamePlayerType player2Type,
            BotDifficulty botDifficulty,
            String ghostGameId
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

    /**
     * Révèle les réponses du round courant et arme le chrono. Émise par la saga après
     * la phase de lecture — jamais par le client, afin de rester autoritaire sur le temps.
     */
    record RevealQuestionCommand(
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

    /**
     * Termine la partie. {@code forfeitById} (optionnel) désigne le joueur qui abandonne :
     * l'adversaire est alors déclaré vainqueur, quel que soit le score.
     */
    record EndGameCommand(
            @TargetAggregateIdentifier String gameId,
            String forfeitById
    ) implements GameCommand {
    }

    /**
     * Clôt un run asynchrone solo (perspective « record ») : enregistre le run sans attribuer
     * de vainqueur ni d'XP. Le replay ultérieur produira le {@code GameEndedEvent} autoritaire.
     */
    record EndRunCommand(
            @TargetAggregateIdentifier String gameId
    ) implements GameCommand {
    }
}
