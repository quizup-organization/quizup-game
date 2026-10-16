package io.github.quizup.game.domain.model;

/**
 * Ordre d'affichage de l'historique d'un joueur ({@code GetPlayerGamesQuery}).
 *
 * <p>{@link #SCORE} compare le <b>score du joueur interrogé</b> (le côté du joueur dépend de la
 * partie), jamais un côté fixe.</p>
 */
public enum GameHistorySort {
    /** Plus récentes d'abord (défaut). */
    RECENT,
    /** Plus anciennes d'abord. */
    OLDEST,
    /** Meilleur score du joueur interrogé d'abord. */
    SCORE
}
