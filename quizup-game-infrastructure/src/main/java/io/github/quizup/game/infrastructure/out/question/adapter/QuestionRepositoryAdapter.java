package io.github.quizup.game.infrastructure.out.question.adapter;

import io.github.quizup.game.domain.model.GameQuestion;
import io.github.quizup.game.domain.port.out.QuestionRepositoryPort;
import io.github.quizup.game.infrastructure.out.question.mapper.GameQuestionMapper;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.query.QuestionQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionRepositoryAdapter implements QuestionRepositoryPort {

    private final QueryGateway queryGateway;

    public QuestionRepositoryAdapter(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Override
    public List<GameQuestion> findRandomApprovedByTopicId(String topicId, int count) {
        List<Question> questions = queryGateway.query(
                new QuestionQuery.GetRandomApprovedQuestionsQuery(topicId, count),
                ResponseTypes.multipleInstancesOf(Question.class)
        ).join();

        return questions.stream()
                .map(GameQuestionMapper::toGameQuestion)
                .toList();
    }
}

