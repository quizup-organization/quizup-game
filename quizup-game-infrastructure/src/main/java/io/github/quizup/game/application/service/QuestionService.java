package io.github.quizup.game.application.service;

import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.game.domain.model.GameQuestion;
import io.github.quizup.game.domain.port.out.QuestionRepositoryPort;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.query.QuestionQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Adaptateur sortant inter-module (spec §2.7) : résout les questions via quizup-theme et ne
 * retourne que le type **local** {@link GameQuestion}. Interroge le bus Axon — la classe
 * n'implémente aucun port entrant.
 */
@Service
public class QuestionService implements QuestionRepositoryPort {

    private final QueryGateway queryGateway;

    public QuestionService(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Override
    public List<GameQuestion> findRandomApprovedByTopicId(String topicId, int count, Set<Language> languages) {
        List<Question> questions = queryGateway.query(
                new QuestionQuery.GetRandomApprovedQuestionsQuery(topicId, count, languages),
                QueryResponseTypes.multipleInstancesOf(Question.class)
        ).join();

        return questions.stream()
                .map(GameQuestionMapper::toGameQuestion)
                .toList();
    }
}
