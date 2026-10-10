package io.github.quizup.game.domain.command;

import io.github.quizup.game.domain.model.BotDifficulty;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameQuestion;
import io.github.quizup.game.domain.model.GameQuestionChoice;
import io.github.quizup.game.domain.model.PlayerProgressSnapshot;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.time.Instant;
import java.util.List;

public interface GameCommand {
    String gameId();

    /**
     * Crée une partie avec les deux joueurs déclarés. {@code player2Id} est toujours requis :
     * un duel contre le bot utilise {@code QuizUpConstants.SYSTEM_USER_ID} avec
     * {@code player2Type = BOT}.
     *
     * <p>{@code questions} est préparé par l'appelant (salle, appariement ou façade) : la
     * sélection (sujet, langues, nombre) est un I/O inter-service qui ne doit pas s'exécuter
     * dans le handler de commande. L'agrégat ne fait que valider la complétude.</p>
     *
     * <p>{@code player1Progress}/{@code player2Progress} figent la progression des joueurs à la
     * création : l'écran de résultat l'affiche au lieu de la progression courante du compte.</p>
     */
    record CreateGameCommand(
            @TargetAggregateIdentifier String gameId,
            String topicId,
            String player1Id,
            String player1Name,
            String player2Id,
            String player2Name,
            GamePlayerType player2Type,
            BotDifficulty botDifficulty,
            PlayerProgressSnapshot player1Progress,
            PlayerProgressSnapshot player2Progress,
            List<GameQuestion> questions
    ) implements GameCommand {
    }

    /**
     * Un joueur abandonne une partie en cours : l'adversaire est déclaré vainqueur.
     */
    record ForfeitGameCommand(
            @TargetAggregateIdentifier String gameId,
            String playerId
    ) implements GameCommand {
    }

    /**
     * Clôt la partie au score (fin normale après le dernier round, ou expiration serveur).
     */
    record EndGameCommand(
            @TargetAggregateIdentifier String gameId
    ) implements GameCommand {
    }

    /**
     * Annulation système avant démarrage (expiration) — distincte de la sortie joueur.
     */
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
}
