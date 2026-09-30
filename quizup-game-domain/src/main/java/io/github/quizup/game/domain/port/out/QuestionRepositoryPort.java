package io.github.quizup.game.domain.port.out;

import io.github.quizup.game.domain.model.GameQuestion;
import io.github.quizup.microservice.core.domain.model.i18n.Language;

import java.util.List;
import java.util.Set;

public interface QuestionRepositoryPort {

    /**
     * Questions approuvées aléatoires du thème, disponibles dans **toutes** les langues demandées.
     */
    List<GameQuestion> findRandomApprovedByTopicId(String topicId, int count, Set<Language> languages);
}
