package io.github.quizup.game.domain.model;

/**
 * Type du joueur 2 dans une partie.
 * <ul>
 *   <li>{@link #HUMAN} — joueur humain (duel synchrone ou défi).</li>
 *   <li>{@link #BOT} — IA (probabilité de réussite et délai pilotés par {@link BotDifficulty}).</li>
 *   <li>{@link #GHOST} — run enregistré d'un joueur absent (duel asynchrone) : ses réponses
 *       sont rejouées aux timings enregistrés.</li>
 * </ul>
 */
public enum GamePlayerType {
    BOT,
    HUMAN,
    GHOST
}
