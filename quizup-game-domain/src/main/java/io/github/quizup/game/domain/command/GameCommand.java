package io.github.quizup.game.domain.command;

import io.github.quizup.game.domain.model.BotDifficulty;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameQuestionChoice;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.time.Instant;
import java.util.Set;

public interface GameCommand {
    String gameId();

    /**
     * Crée une partie avec les deux joueurs déclarés. {@code player2Id} est toujours requis :
     * un duel contre le bot utilise {@code QuizUpConstants.SYSTEM_USER_ID} avec
     * {@code player2Type = BOT}. {@code languages} = langues requises (union des langues des
     * joueurs) : seules les questions disponibles dans **toutes** ces langues sont tirées
     * (sélection stricte).
     */
    record CreateGameCommand(
            @TargetAggregateIdentifier String gameId,
            String topicId,
            String player1Id,
            String player1Name,
            String player2Id,
            String player2Name,
            Set<Language> languages,
            GamePlayerType player2Type,
            BotDifficulty botDifficulty
    ) implements GameCommand {
    }

    /**
     * Un joueur confirme sa présence dans la salle d'attente. Idempotent : un joueur déjà présent
     * ne provoque aucun nouvel événement. Quand les deux sont présents → statut {@code READY}.
     */
    record JoinGameCommand(
            @TargetAggregateIdentifier String gameId,
            String playerId
    ) implements GameCommand {
    }

    /**
     * Un joueur quitte la salle d'attente avant le démarrage. La partie est annulée
     * (aucun round n'a été joué). En cours, utiliser {@link ForfeitGameCommand}.
     */
    record LeaveGameCommand(
            @TargetAggregateIdentifier String gameId,
            String playerId
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

    /**
     * Démarre la partie : les deux joueurs sont présents ({@code READY}).
     */
    record StartGameCommand(
            @TargetAggregateIdentifier String gameId
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
     * Un joueur demande une revanche après la fin de la partie. Si l'adversaire avait déjà
     * demandé, cette commande vaut acceptation (double-clic simultané). {@code languages} =
     * langues requises pour la nouvelle partie (non vide).
     */
    record RequestRematchCommand(
            @TargetAggregateIdentifier String gameId,
            String playerId,
            Set<Language> languages
    ) implements GameCommand {
    }

    /**
     * L'adversaire accepte la demande de revanche en attente.
     */
    record AcceptRematchCommand(
            @TargetAggregateIdentifier String gameId,
            String playerId
    ) implements GameCommand {
    }

    /**
     * L'adversaire décline la demande de revanche en attente.
     */
    record DeclineRematchCommand(
            @TargetAggregateIdentifier String gameId,
            String playerId
    ) implements GameCommand {
    }

    /**
     * Le demandeur annule sa demande de revanche en attente. Seul le demandeur peut l'annuler.
     */
    record CancelRematchCommand(
            @TargetAggregateIdentifier String gameId,
            String playerId
    ) implements GameCommand {
    }

    /**
     * La saga confirme que la nouvelle partie a bien été créée : l'état de revanche est clos sur
     * l'ancienne partie.
     */
    record ConfirmRematchCommand(
            @TargetAggregateIdentifier String gameId,
            String newGameId
    ) implements GameCommand {
    }

    /**
     * La saga avorte la revanche en attente (échec de création ou expiration).
     */
    record AbortRematchCommand(
            @TargetAggregateIdentifier String gameId,
            String reason
    ) implements GameCommand {
    }
}
