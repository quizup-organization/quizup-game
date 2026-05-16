package io.github.quizup.game.infrastructure.mapper;

import io.github.quizup.game.domain.model.GameQuestionChoice;
import io.github.quizup.topic.domain.model.QuestionChoice;

import java.util.LinkedHashMap;
import java.util.Map;

public final class GameQuestionChoiceMapper {

    private GameQuestionChoiceMapper() {
    }

    public static GameQuestionChoice toDomain(QuestionChoice choice) {
        return choice == null ? null : GameQuestionChoice.valueOf(choice.name());
    }

    public static QuestionChoice toTopic(GameQuestionChoice choice) {
        return choice == null ? null : QuestionChoice.valueOf(choice.name());
    }

    public static Map<GameQuestionChoice, String> toDomain(Map<QuestionChoice, String> answers) {
        if (answers == null) {
            return Map.of();
        }

        Map<GameQuestionChoice, String> converted = new LinkedHashMap<>();

        answers.forEach((choice, text) -> converted.put(toDomain(choice), text));

        return converted;
    }
}

