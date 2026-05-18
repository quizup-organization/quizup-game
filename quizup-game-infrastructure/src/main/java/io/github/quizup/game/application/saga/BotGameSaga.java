package io.github.quizup.game.application.saga;

import io.github.quizup.common.domain.constant.QuizUpConstants;
import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.model.*;
import io.github.quizup.theme.domain.model.QuestionChoice;
import lombok.Getter;
import lombok.Setter;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Random;

@Saga
public class BotGameSaga {
    private static final double BOT_CORRECT_PROBABILITY = 0.6;
    private static final Random RANDOM = new Random();
    private static final Logger logger = LoggerFactory.getLogger(BotGameSaga.class);

    @Autowired
    private transient CommandGateway commandGateway;

    @Autowired
    private transient DeadlineManager deadlineManager;

    @Getter
    @Setter
    private String gameId;

    @Getter
    @Setter
    private String player1Id;

    @Getter
    @Setter
    private String player2Id;

    @Getter
    @Setter
    private int joinedCount;

    @Getter
    @Setter
    private GameRoundType currentRound;

    @Getter
    @Setter
    private int answersInCurrentRound;

    @Getter
    @Setter
    private boolean player1Answered;

    @Getter
    @Setter
    private boolean player2Answered;

    @Getter
    @Setter
    private String roundDeadlineId;

    @Getter
    @Setter
    private String nextRoundDeadlineId;

    @StartSaga
    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.GameCreatedEvent event) {
        if (GameMode.ASYNC.equals(event.mode()) || GamePlayerType.HUMAN.equals(event.player2Type())) {
            SagaLifecycle.end();
            return;
        }

        this.gameId = event.gameId();
        this.player1Id = event.player1Id();
        this.player2Id = event.player2Id();
        this.joinedCount = 0;

        commandGateway.send(
                new GameCommand.JoinGameCommand(
                        gameId,
                        QuizUpConstants.BOT_USER_ID
                )
        );
    }

    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.GameJoinedEvent event) {
        joinedCount++;

        if (joinedCount == 2) {
            commandGateway.send(
                    new GameCommand.StartGameCommand(
                            gameId
                    )
            );
        }
    }

    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.GameStartedEvent event) {
        commandGateway.send(
                new GameCommand.StartRoundCommand(
                        gameId
                )
        );
    }

    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.RoundStartedEvent event) {
        this.currentRound = event.round();
        this.answersInCurrentRound = 0;
        this.player1Answered = false;
        this.player2Answered = false;

        roundDeadlineId = deadlineManager.schedule(
                GameDeadline.ROUND_EXPIRED_TIMEOUT,
                GameDeadline.ROUND_EXPIRED
        );

        GameQuestionChoice botChoice = RANDOM.nextDouble() < BOT_CORRECT_PROBABILITY
                ? event.question().correctAnswer()
                : randomWrongAnswer(event.question().correctAnswer());

        commandGateway.send(
                new GameCommand.AnswerQuestionCommand(
                        gameId,
                        QuizUpConstants.BOT_USER_ID,
                        botChoice,
                        Instant.now()
                )
        );
    }

    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.QuestionAnsweredEvent event) {
        answersInCurrentRound++;
        if (event.playerId().equals(player1Id)) player1Answered = true;
        else player2Answered = true;

        if (answersInCurrentRound >= 2) {
            cancelRoundDeadline();
            commandGateway.send(
                    new GameCommand.CloseRoundCommand(
                            gameId
                    )
            );
        }
    }

    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.RoundClosedEvent event) {
        if (event.nextRound() != null) {
            nextRoundDeadlineId = deadlineManager.schedule(
                    GameDeadline.NEXT_ROUND_STARTS_TIMEOUT,
                    GameDeadline.NEXT_ROUND_STARTS);
        } else {
            commandGateway.send(
                    new GameCommand.EndGameCommand(
                            gameId
                    )
            );
        }
    }

    @DeadlineHandler(deadlineName = GameDeadline.ROUND_EXPIRED)
    public void onRoundExpired() {
        if (!player1Answered) {
            commandGateway.send(
                    new GameCommand.AnswerQuestionCommand(
                            gameId,
                            player1Id,
                            null,
                            Instant.now()
                    )
            );
        }
    }

    @DeadlineHandler(deadlineName = GameDeadline.NEXT_ROUND_STARTS)
    public void onNextRoundStarts() {
        commandGateway.send(
                new GameCommand.StartRoundCommand(
                        gameId
                )
        );
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.GameEndedEvent event) {
        logger.info("[SyncBotGameFlowSaga] Game ended: gameId={}, winnerId={}", gameId, event.winnerId());
        cancelAll();
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.GameCancelledEvent event) {
        cancelAll();
    }

    private void cancelAll() {
        cancelRoundDeadline();
        cancelNextRoundDeadline();
    }

    private void cancelRoundDeadline() {
        if (roundDeadlineId != null) {
            deadlineManager.cancelSchedule(GameDeadline.ROUND_EXPIRED, roundDeadlineId);
            roundDeadlineId = null;
        }
    }

    private void cancelNextRoundDeadline() {
        if (nextRoundDeadlineId != null) {
            deadlineManager.cancelSchedule(GameDeadline.NEXT_ROUND_STARTS, nextRoundDeadlineId);
            nextRoundDeadlineId = null;
        }
    }

    private GameQuestionChoice randomWrongAnswer(GameQuestionChoice correct) {
        GameQuestionChoice[] all = GameQuestionChoice.values();
        GameQuestionChoice wrong;
        do {
            wrong = all[RANDOM.nextInt(all.length)];
        } while (wrong == correct);
        return wrong;
    }
}