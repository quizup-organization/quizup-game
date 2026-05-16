package io.github.quizup.game.domain.port.out;

import io.github.quizup.game.domain.model.GameQuestion;

import java.util.List;

public interface QuestionPort {

    List<GameQuestion> findRandomApprovedByTopicId(String topicId, int count);
}

