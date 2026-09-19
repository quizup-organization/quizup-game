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
     * Points pour une réponse correcte sur la question bonus ({@link GameRoundType#ROUND_7})
     */
    public static final int POINTS_BONUS_QUESTION = 20;

    /**
     * Bonus maximum de vitesse sur une question normale (réponse instantanée)
     */
    public static final int MAX_SPEED_BONUS = 10;

    /**
     * Bonus maximum de vitesse sur la question bonus (doublé) :
     * score maximum d'un duel = 6 × (10+10) + (20+20) = 160.
     */
    public static final int MAX_SPEED_BONUS_BONUS_QUESTION = 20;

    /**
     * Durée du timeout d'un round en secondes
     */
    public static final long ROUND_TIMEOUT_SECONDS = 10;

    /**
     * Durée (ms) pendant laquelle la question est affichée seule, avant révélation des réponses.
     * Le chrono n'est armé qu'à l'issue de cette phase : la lecture ne consomme aucun temps.
     */
    public static final long QUESTION_REVEAL_MS = 1980;

    /**
     * Délai serveur (ms) avant le premier round : écran VS + transition + intro de tour.
     * Le serveur reste propriétaire de la transition et l'expose via {@code firstRoundAt}.
     * Aligné sur la maquette : VS (2700) + swoosh (1450) + intro de tour (1900) = 6050.
     */
    public static final long MATCH_INTRO_MS = 6050;

    /**
     * Délai serveur (ms) entre la clôture d'un round et le début du suivant :
     * révélation de la bonne réponse + intro de tour.
     */
    public static final long ROUND_TRANSITION_MS = 4400;

    /**
     * Seuil (secondes) d'une réponse « éclair » (badge Éclair).
     */
    public static final long FAST_ANSWER_SECONDS = 3;

    /**
     * Calcule le bonus de vitesse en fonction du temps de réponse
     *
     * @param timeTakenSeconds Temps de réponse en seconde
     * @return Bonus de vitesse (0-10 points)
     */
    public static int calculateSpeedBonus(long timeTakenSeconds) {
        return calculateSpeedBonus(timeTakenSeconds, false);
    }

    /**
     * Calcule le bonus de vitesse, doublé sur la question bonus.
     *
     * @param timeTakenSeconds Temps de réponse en seconde
     * @param isBonusQuestion  true si c'est la question bonus (ROUND_7)
     * @return Bonus de vitesse (0-10, ou 0-20 sur la question bonus)
     */
    public static int calculateSpeedBonus(long timeTakenSeconds, boolean isBonusQuestion) {
        int maxBonus = isBonusQuestion ? MAX_SPEED_BONUS_BONUS_QUESTION : MAX_SPEED_BONUS;
        // Borné [0, maxBonus] : un temps négatif (dérive d'horloge client) ne peut pas
        // gonfler le bonus au-delà du maximum (score de duel ≤ 160).
        long elapsed = Math.max(0, Math.min(maxBonus, timeTakenSeconds));
        return (int) (maxBonus - elapsed);
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
