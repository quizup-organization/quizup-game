package io.github.quizup.game.domain.model;

import lombok.Getter;

@Getter
public enum GameRoundType {
    ROUND_1(false),
    ROUND_2(false),
    ROUND_3(false),
    ROUND_4(false),
    ROUND_5(false),
    ROUND_6(false),
    ROUND_7(true);

    private final boolean bonus;

    GameRoundType(boolean bonus) {
        this.bonus = bonus;
    }
}
