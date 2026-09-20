package io.github.quizup.game.domain.port.out;

/**
 * Port sortant des KPI métier du gameplay.
 *
 * <p>Implémenté en infrastructure avec Micrometer ({@code MeterRegistry}). Le domaine n'expose et
 * ne connaît que des types JDK : aucun import Micrometer ici (règle hexagonale).
 */
public interface GameMetricsPort {

    /**
     * Une partie a été créée.
     *
     * @param topicId      thème de la partie
     * @param mode         {@code SYNC}/{@code ASYNC}
     * @param opponentType {@code HUMAN}/{@code BOT}/{@code GHOST}
     */
    void gameCreated(String topicId, String mode, String opponentType);

    /**
     * Une partie a démarré ({@code mode} = {@code SYNC}/{@code ASYNC}).
     */
    void gameStarted(String mode);

    /**
     * Une réponse a été soumise.
     *
     * @param playerType {@code HUMAN}/{@code BOT}/{@code GHOST}
     * @param correct    réponse correcte ou non
     * @param timeMs     temps de réponse mesuré depuis la révélation
     */
    void answerSubmitted(String playerType, boolean correct, long timeMs);

    /**
     * Une partie s'est terminée.
     *
     * @param outcome     {@code WIN}/{@code DRAW}/{@code FORFEIT}
     * @param forfeited   victoire par forfait de l'adversaire
     * @param totalPoints somme des scores finaux des deux joueurs
     */
    void gameEnded(String outcome, boolean forfeited, int totalPoints);

    /**
     * Une partie a été annulée ({@code reason} = motif technique).
     */
    void gameCancelled(String reason);

    /**
     * Un run asynchrone solo a été enregistré.
     */
    void runRecorded(String topicId, int score);
}
