package io.github.quizup.game.domain.model;

/**
 * Statut d'une partie. La partie démarre **immédiatement à la création** (la présence des
 * joueurs est garantie en amont par le salon / l'appariement côté {@code quizup-matchmaking}) :
 * il n'existe donc pas de statut d'attente.
 * <ul>
 *   <li>{@link #IN_PROGRESS} — rounds en cours.</li>
 *   <li>{@link #FINISHED} — terminée.</li>
 *   <li>{@link #CANCELED} — annulée (expiration).</li>
 * </ul>
 */
public enum GameStatus {
    IN_PROGRESS,
    FINISHED,
    CANCELED
}
