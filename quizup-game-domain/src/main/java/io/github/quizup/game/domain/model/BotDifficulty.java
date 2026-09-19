package io.github.quizup.game.domain.model;

/**
 * Niveau de difficulté d'un bot (features.md §1 : « niveau de difficulté ajustable »).
 * La probabilité de réponse correcte et la fenêtre de temps de réponse pilotent le
 * comportement de la saga de jeu : plus le bot est difficile, plus il répond juste et vite.
 */
public enum BotDifficulty {

    EASY(0.40, 3000, 6400),
    NORMAL(0.60, 2000, 5200),
    HARD(0.85, 1300, 3800);

    private final double correctProbability;
    private final long minAnswerDelayMillis;
    private final long maxAnswerDelayMillis;

    BotDifficulty(double correctProbability, long minAnswerDelayMillis, long maxAnswerDelayMillis) {
        this.correctProbability = correctProbability;
        this.minAnswerDelayMillis = minAnswerDelayMillis;
        this.maxAnswerDelayMillis = maxAnswerDelayMillis;
    }

    public double correctProbability() {
        return correctProbability;
    }

    /** Borne basse du délai de réponse du bot, mesuré depuis la révélation des réponses. */
    public long minAnswerDelayMillis() {
        return minAnswerDelayMillis;
    }

    /** Borne haute du délai de réponse du bot, mesuré depuis la révélation des réponses. */
    public long maxAnswerDelayMillis() {
        return maxAnswerDelayMillis;
    }

    public static BotDifficulty fromOrDefault(BotDifficulty difficulty) {
        return difficulty != null ? difficulty : NORMAL;
    }
}
