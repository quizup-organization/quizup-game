package io.github.quizup.game.domain.model;

/**
 * Type du joueur 2 dans une partie.
 * <ul>
 *   <li>{@link #HUMAN} — joueur humain (duel ou salon).</li>
 *   <li>{@link #BOT} — IA (probabilité de réussite et délai pilotés par {@link BotDifficulty}).</li>
 * </ul>
 */
public enum GamePlayerType {
    BOT,
    HUMAN
}
