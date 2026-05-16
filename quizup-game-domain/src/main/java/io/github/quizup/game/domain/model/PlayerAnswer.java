package io.github.quizup.game.domain.model;


public record PlayerAnswer(
        GamePlayer player,
        GameQuestionChoice choice,
        boolean correct,
        int pointsEarned
) {
}

