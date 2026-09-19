package io.github.quizup.game.domain.model;

/**
 * Statut d'une partie.
 * <ul>
 *   <li>{@link #CREATED} — créée, en attente de la présence des joueurs.</li>
 *   <li>{@link #READY} — les deux joueurs sont présents.</li>
 *   <li>{@link #IN_PROGRESS} — rounds en cours.</li>
 *   <li>{@link #AWAITING_OPPONENT} — run asynchrone enregistré, en attente du replay adverse.</li>
 *   <li>{@link #FINISHED} — terminée.</li>
 *   <li>{@link #CANCELED} — annulée.</li>
 * </ul>
 */
public enum GameStatus {
    CREATED,
    READY,
    IN_PROGRESS,
    AWAITING_OPPONENT,
    FINISHED,
    CANCELED
}
