package io.github.quizup.game.application.service;

import io.github.quizup.game.domain.model.GameQuestion;
import io.github.quizup.game.domain.model.GameQuestionChoice;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.model.QuestionChoice;

import java.util.EnumMap;
import java.util.Map;

import static java.util.Objects.isNull;

/**
 * Mapping question theme → modèle local du jeu (adaptateur sortant inter-module).
 */
public final class GameQuestionMapper {

    private GameQuestionMapper() {
    }

    public static GameQuestion toGameQuestion(Question question) {
        return new GameQuestion(
                question.questionId(),
                question.text(),
                question.imageUrl(),
                question.difficulty() != null ? question.difficulty().name() : null,
                toGameQuestionChoices(question.answers()),
                toGameQuestionChoice(question.correctAnswer())
        );
    }

    public static GameQuestionChoice toGameQuestionChoice(QuestionChoice questionChoice) {
        if (isNull(questionChoice)) {
            return null;
        }

        return GameQuestionChoice.valueOf(questionChoice.name());
    }

    public static Map<GameQuestionChoice, String> toGameQuestionChoices(Map<QuestionChoice, String> questionChoices) {
        final Map<GameQuestionChoice, String> gameQuestionChoices = new EnumMap<>(GameQuestionChoice.class);

        if (isNull(questionChoices) || questionChoices.isEmpty()) {
            return gameQuestionChoices;
        }

        for (Map.Entry<QuestionChoice, String> entry : questionChoices.entrySet()) {
            final GameQuestionChoice gameQuestionChoice = toGameQuestionChoice(entry.getKey());

            if (isNull(gameQuestionChoice)) {
                continue;
            }

            gameQuestionChoices.put(gameQuestionChoice, entry.getValue());
        }

        return gameQuestionChoices;
    }
}
