package io.github.quizup.game.domain.model;

/**
 * Cycle de vie d'un round, en deux phases explicites :
 * <ul>
 *   <li>{@link #CREATED} — le round n'a pas commencé.</li>
 *   <li>{@link #QUESTION_SHOWN} — la question est affichée aux joueurs, les réponses
 *       s'animent ; aucune réponse n'est acceptée et le chrono n'est pas encore armé.</li>
 *   <li>{@link #ANSWERABLE} — les réponses sont révélées : le chrono tourne jusqu'à
 *       {@code answerDeadlineAt}. C'est la seule phase où répondre est autorisé.</li>
 *   <li>{@link #CLOSED} — le round est terminé (les deux ont répondu, ou expiration).</li>
 * </ul>
 */
public enum GameRoundStatus {
    CREATED,

    QUESTION_SHOWN,

    ANSWERABLE,

    CLOSED
}
