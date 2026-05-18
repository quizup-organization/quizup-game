package io.github.quizup.game.infrastructure.out.messaging.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.quizup.game.domain.model.GameQuestionChoice;

import java.time.Instant;
import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = GameNotification.GameCreatedNotification.class, name = "GAME_CREATED"),
        @JsonSubTypes.Type(value = GameNotification.PlayerJoinedNotification.class, name = "PLAYER_JOINED"),
        @JsonSubTypes.Type(value = GameNotification.GameStartedNotification.class, name = "GAME_STARTED"),
        @JsonSubTypes.Type(value = GameNotification.RoundStartedNotification.class, name = "ROUND_STARTED"),
        @JsonSubTypes.Type(value = GameNotification.PlayerAnsweredNotification.class, name = "PLAYER_ANSWERED"),
        @JsonSubTypes.Type(value = GameNotification.RoundClosedNotification.class, name = "ROUND_CLOSED"),
        @JsonSubTypes.Type(value = GameNotification.GameEndedNotification.class, name = "GAME_ENDED"),
        @JsonSubTypes.Type(value = GameNotification.GameCancelledNotification.class, name = "GAME_CANCELLED")
})
public interface GameNotification {

    GameNotificationType type();

    String gameId();

    enum GameNotificationType {
        GAME_CREATED,
        PLAYER_JOINED,
        GAME_STARTED,
        ROUND_STARTED,
        PLAYER_ANSWERED,
        ROUND_CLOSED,
        GAME_ENDED,
        GAME_CANCELLED
    }
    record GameCreatedNotification(
            String gameId,
            String topicId,
            String player1Id,
            String player2Id
    ) implements GameNotification {
        @Override
        public GameNotificationType type() {
            return GameNotificationType.GAME_CREATED;
        }
    }

    record PlayerJoinedNotification(
            String gameId,
            String playerId
    ) implements GameNotification {
        @Override
        public GameNotificationType type() {
            return GameNotificationType.PLAYER_JOINED;
        }
    }

    record GameStartedNotification(
            String gameId,
            String mode
    ) implements GameNotification {
        @Override
        public GameNotificationType type() {
            return GameNotificationType.GAME_STARTED;
        }
    }

    record RoundStartedNotification(
            String gameId,
            String round,
            String questionText,
            Map<String, String> answers,
            boolean bonus
    ) implements GameNotification {
        @Override
        public GameNotificationType type() {
            return GameNotificationType.ROUND_STARTED;
        }
    }

    record PlayerAnsweredNotification(
            String gameId,
            String round,
            String playerId,
            GameQuestionChoice choice,
            boolean correct,
            int pointsEarned,
            Instant answeredAt
    ) implements GameNotification {
        @Override
        public GameNotificationType type() {
            return GameNotificationType.PLAYER_ANSWERED;
        }
    }

    record RoundClosedNotification(
            String gameId,
            String closedRound,
            String nextRound
    ) implements GameNotification {
        @Override
        public GameNotificationType type() {
            return GameNotificationType.ROUND_CLOSED;
        }
    }

    record GameEndedNotification(
            String gameId,
            String winnerId,
            int player1FinalScore,
            int player2FinalScore
    ) implements GameNotification {
        @Override
        public GameNotificationType type() {
            return GameNotificationType.GAME_ENDED;
        }
    }

    record GameCancelledNotification(
            String gameId,
            String reason
    ) implements GameNotification {
        @Override
        public GameNotificationType type() {
            return GameNotificationType.GAME_CANCELLED;
        }
    }
}