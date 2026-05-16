package io.github.quizup.game.application.service;

import io.github.quizup.game.domain.model.GameQuestion;
import io.github.quizup.game.domain.port.out.QuestionPort;
import io.github.quizup.game.infrastructure.mapper.GameQuestionChoiceMapper;
import io.github.quizup.topic.domain.model.Question;
import io.github.quizup.topic.domain.query.QuestionQuery;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService implements QuestionPort {

    private final QueryGateway queryGateway;

    public QuestionService(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Override
    public List<GameQuestion> findRandomApprovedByTopicId(String topicId, int count) {
        List<Question> questions = queryGateway.query(
                new QuestionQuery.GetRandomApprovedQuestionsQuery(topicId, count),
                ResponseTypes.multipleInstancesOf(Question.class)
        ).join();

        return questions.stream()
                .map(question -> new GameQuestion(
                        question.questionId(),
                        question.text(),
                        GameQuestionChoiceMapper.toDomain(question.answers()),
                        GameQuestionChoiceMapper.toDomain(question.correctAnswer())
                ))
                .toList();
    }
}

