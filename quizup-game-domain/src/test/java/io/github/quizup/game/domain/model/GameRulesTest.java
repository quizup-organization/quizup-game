package io.github.quizup.game.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Bornes du bonus de vitesse : un temps négatif (dérive d'horloge client) ne doit pas
 * dépasser le bonus maximum, pour garantir un score de duel ≤ 160.
 */
class GameRulesTest {

    @Test
    void normalSpeedBonus_isWithinBounds() {
        assertEquals(GameRules.MAX_SPEED_BONUS, GameRules.calculateSpeedBonus(0, false));
        assertEquals(0, GameRules.calculateSpeedBonus(GameRules.MAX_SPEED_BONUS, false));
        assertEquals(0, GameRules.calculateSpeedBonus(999, false));
        assertEquals(GameRules.MAX_SPEED_BONUS, GameRules.calculateSpeedBonus(-5, false));
    }

    @Test
    void bonusSpeedBonus_isWithinBounds() {
        assertEquals(GameRules.MAX_SPEED_BONUS_BONUS_QUESTION, GameRules.calculateSpeedBonus(0, true));
        assertEquals(0, GameRules.calculateSpeedBonus(GameRules.MAX_SPEED_BONUS_BONUS_QUESTION, true));
        assertEquals(GameRules.MAX_SPEED_BONUS_BONUS_QUESTION, GameRules.calculateSpeedBonus(-3, true));
    }

    @Test
    void maxDuelScore_isOneHundredSixty() {
        int normal = 6 * (GameRules.POINTS_NORMAL + GameRules.MAX_SPEED_BONUS);
        int bonus = GameRules.POINTS_BONUS_QUESTION + GameRules.MAX_SPEED_BONUS_BONUS_QUESTION;
        assertEquals(160, normal + bonus);
    }
}
