package io.github.quizup.game.infrastructure.out.messaging.mapper;

import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.infrastructure.mapper.GameQuestionChoiceMapper;
import io.github.quizup.game.infrastructure.out.messaging.response.GameNotification;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.isNull;

public final class GameEventNotificationMapper {

    private GameEventNotificationMapper() {
    }

    public static Optional<GameNotification> toNotification(GameEvent event) {
        if (isNull(event)) {
            return Optional.empty();
        }

        return switch (event) {
            case GameEvent.GameCreatedEvent gameCreatedEvent -> Optional.of(
                    new GameNotification.GameCreatedNotification(
                            gameCreatedEvent.gameId(),
                            gameCreatedEvent.topicId(),
                            gameCreatedEvent.player1Id(),
                            gameCreatedEvent.player2Id()
                    )
            );

            case GameEvent.GameJoinedEvent gameJoinedEvent -> Optional.of(
                    new GameNotification.PlayerJoinedNotification(
                            gameJoinedEvent.gameId(),
                            gameJoinedEvent.playerId()
                    )
            );

            case GameEvent.GameStartedEvent gameStartedEvent -> Optional.of(
                    new GameNotification.GameStartedNotification(
                            gameStartedEvent.gameId(),
                            gameStartedEvent.mode().name()
                    )
            );

            case GameEvent.RoundStartedEvent roundStartedEvent -> {
                Map<String, String> answers = new LinkedHashMap<>();
                roundStartedEvent
                        .question()
                        .answers()
                        .forEach((choice, text) -> answers.put(choice.name(), text));
                yield Optional.of(
                        new GameNotification.RoundStartedNotification(
                                roundStartedEvent.gameId(),
                                roundStartedEvent.round().name(),
                                roundStartedEvent.question().text(),
                                answers,
                                roundStartedEvent.round().isBonus()
                        ));
            }

            case GameEvent.QuestionAnsweredEvent questionAnsweredEvent -> Optional.of(
                    new GameNotification.PlayerAnsweredNotification(
                            questionAnsweredEvent.gameId(),
                            questionAnsweredEvent.round().name(),
                            questionAnsweredEvent.playerId(),
                            GameQuestionChoiceMapper.toTopic(questionAnsweredEvent.choice()),
                            questionAnsweredEvent.correct(),
                            questionAnsweredEvent.pointsEarned(),
                            questionAnsweredEvent.answeredAt()
                    )
            );


            case GameEvent.RoundClosedEvent roundClosedEvent -> Optional.of(
                    new GameNotification.RoundClosedNotification(
                            roundClosedEvent.gameId(),
                            roundClosedEvent.closedRound().name(),
                            roundClosedEvent.nextRound() != null ? roundClosedEvent.nextRound().name() : null
                    )
            );

            case GameEvent.GameEndedEvent gameEndedEvent -> Optional.of(
                    new GameNotification.GameEndedNotification(
                            gameEndedEvent.gameId(),
                            gameEndedEvent.winnerId(),
                            gameEndedEvent.player1FinalScore(),
                            gameEndedEvent.player2FinalScore()
                    )
            );

            case GameEvent.GameCancelledEvent gameCancelledEvent -> Optional.of(
                    new GameNotification.GameCancelledNotification(
                            gameCancelledEvent.gameId(),
                            gameCancelledEvent.reason()
                    )
            );

            default -> Optional.empty();
        };
    }
}