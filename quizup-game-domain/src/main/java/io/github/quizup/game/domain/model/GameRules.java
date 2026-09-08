package io.github.quizup.game.domain.model;

/**
 * Classe utilitaire pour centraliser toutes les règles de scoring
 * Constantes et logique de calcul des points pour maintenir
 * la cohérence dans tout le système.
 */
public final class GameRules {
    private GameRules() {
        // Classe utilitaire, pas d'instanciation
    }

    public static final int TOTAL_ROUNDS = 7;

    public static final long GAME_TIMEOUT_HOURS = 24;

    /**
     * Points pour une réponse correcte normale
     */
    public static final int POINTS_NORMAL = 10;

    /**
     * Points pour une réponse correcte sur une question bonus (rounds 3 et 6)
     */
    public static final int POINTS_BONUS_QUESTION = 20;

    /**
     * Bonus maximum de vitesse (réponse en moins de 2 secondes)
     */
    public static final int MAX_SPEED_BONUS = 10;

    /**
     * Durée du timeout d'un round en secondes
     */
    public static final long ROUND_TIMEOUT_SECONDS = 10;

    /**
     * Calcule le bonus de vitesse en fonction du temps de réponse
     *
     * @param timeTakenSeconds Temps de réponse en seconde
     * @return Bonus de vitesse (0-10 points)
     */
    public static int calculateSpeedBonus(long timeTakenSeconds) {
        return (int) Math.max(0, MAX_SPEED_BONUS - timeTakenSeconds);
    }

    /**
     * Calcule les points de base pour une question
     *
     * @param isBonusQuestion true si c'est une question bonus
     * @return Points de base (10 ou 20)
     */
    public static int getBasePoints(boolean isBonusQuestion) {
        return isBonusQuestion ? POINTS_BONUS_QUESTION : POINTS_NORMAL;
    }
}
