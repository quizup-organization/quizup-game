package io.github.quizup.game.domain.model;

import java.util.Map;

/**
 * Snapshot immuable d'une question, embarqué dans les events pour autonomie event-sourcing.
 */
public record GameQuestion(
        String questionId,
        String text,
        Map<GameQuestionChoice, String> answers,
        GameQuestionChoice correctAnswer
) {
}

