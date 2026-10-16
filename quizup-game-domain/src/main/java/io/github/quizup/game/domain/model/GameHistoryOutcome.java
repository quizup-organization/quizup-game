package io.github.quizup.game.domain.model;

/**
 * Filtre d'<b>issue</b> de l'historique d'un joueur ({@code GetPlayerGamesQuery}).
 *
 * <p>L'issue est <b>relative au joueur interrogé</b> : {@link #WIN} et {@link #LOSS} se lisent
 * depuis le vainqueur de la partie ({@code winnerId}), jamais depuis un côté fixe. {@link #ALL}
 * et {@code null} sont équivalents (aucun filtre).</p>
 */
public enum GameHistoryOutcome {
    /** Aucun filtre d'issue. */
    ALL,
    /** Partie terminée gagnée par le joueur interrogé. */
    WIN,
    /** Partie terminée perdue par le joueur interrogé. */
    LOSS,
    /** Partie terminée sans vainqueur (égalité). */
    DRAW,
    /** Partie en cours. */
    IN_PROGRESS,
    /** Partie annulée (expiration, sortie d'un joueur). */
    CANCELLED
}
