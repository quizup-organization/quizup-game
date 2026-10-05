package io.github.quizup.game.application.saga;

import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.model.*;
import lombok.Getter;
import lombok.Setter;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.deadline.DeadlineManager;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

/**
 * Orchestration du cycle de vie d'une partie :
 * <ul>
 *   <li>la partie est créée par le lobby (les deux joueurs y étaient présents) ;</li>
 *   <li>elle ne démarre qu'une fois les deux joueurs entrés dans l'arène
 *       ({@code GameJoinedEvent}) — le bot est rejoint côté serveur ;</li>
 *   <li>les deadlines de phase sont dérivées des instants absolus portés par les événements.</li>
 * </ul>
 *
 * <p>Le serveur est seule source de vérité du temps.
 */
@Saga
@ProcessingGroup("game-flow-saga")
public class GameFlowSaga {

    private static final Random RANDOM = new Random();
    private static final Logger logger = LoggerFactory.getLogger(GameFlowSaga.class);

    @Autowired
    private transient CommandGateway commandGateway;

    @Autowired
    private transient DeadlineManager deadlineManager;

    @Getter
    @Setter
    private String gameId;

    /** La partie a démarré : l'expiration la clôture au lieu de l'annuler. */
    @Getter
    @Setter
    private boolean started;

    @Getter
    @Setter
    private String expiredDeadlineId;

    @Getter
    @Setter
    private String startDeadlineId;

    @Getter
    @Setter
    private String player1Id;

    @Getter
    @Setter
    private String player2Id;

    @Getter
    @Setter
    private GamePlayerType player2Type;

    @Getter
    @Setter
    private BotDifficulty botDifficulty;

    @Getter
    @Setter
    private int joinedCount;

    @Getter
    @Setter
    private GameRoundType currentRound;

    @Getter
    @Setter
    private GameQuestionChoice currentCorrectAnswer;

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
    private String matchIntroDeadlineId;

    @Getter
    @Setter
    private String questionRevealDeadlineId;

    @Getter
    @Setter
    private String roundDeadlineId;

    @Getter
    @Setter
    private String nextRoundDeadlineId;

    @Getter
    @Setter
    private String botDeadlineId;

    @StartSaga
    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.GameCreatedEvent event) {
        this.gameId = event.gameId();
        this.player1Id = event.player1Id();
        this.player2Id = event.player2Id();
        this.player2Type = event.player2Type();
        this.botDifficulty = BotDifficulty.fromOrDefault(event.botDifficulty());
        this.joinedCount = 0;
        this.expiredDeadlineId = deadlineManager.schedule(
                GameDeadline.GAME_EXPIRED_TIMEOUT,
                GameDeadline.GAME_EXPIRED
        );
        this.startDeadlineId = deadlineManager.schedule(
                GameDeadline.START_TIMEOUT_DURATION,
                GameDeadline.START_TIMEOUT
        );

        // Seul le bot est rejoint côté serveur : il n'a pas de client pour entrer dans l'arène.
        if (GamePlayerType.BOT.equals(player2Type)) {
            commandGateway.send(new GameCommand.JoinGameCommand(gameId, player2Id));
        }
    }

    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.GameJoinedEvent event) {
        joinedCount++;

        if (joinedCount == 2) {
            commandGateway.send(new GameCommand.StartGameCommand(gameId));
        }
    }

    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.GameStartedEvent event) {
        this.started = true;
        cancelStartDeadline();
        matchIntroDeadlineId = deadlineManager.schedule(
                durationUntil(event.firstRoundAt()),
                GameDeadline.MATCH_INTRO
        );
    }

    /**
     * Filet de sécurité : les deux joueurs n'ont pas rejoint l'arène dans la fenêtre — la partie
     * est annulée (plus de salle fantôme jusqu'à l'expiration de 24 h).
     */
    @DeadlineHandler(deadlineName = GameDeadline.START_TIMEOUT)
    public void onStartTimeout() {
        if (started) {
            return;
        }
        logger.warn("Partie non démarrée après {}s: gameId={}",
                GameDeadline.START_TIMEOUT_DURATION.toSeconds(), gameId);
        commandGateway.send(new GameCommand.CancelGameCommand(gameId, "NO_SHOW_START"));
    }

    @DeadlineHandler(deadlineName = GameDeadline.MATCH_INTRO)
    public void onMatchIntro() {
        commandGateway.send(new GameCommand.StartRoundCommand(gameId));
    }

    /**
     * Filet de sécurité : une partie ni terminée ni annulée sous {@code GAME_EXPIRED_TIMEOUT}
     * est close d'office (annulation avant démarrage, clôture au score en cours de jeu).
     */
    @DeadlineHandler(deadlineName = GameDeadline.GAME_EXPIRED)
    public void onGameExpired() {
        logger.warn("Partie expirée après {}h: gameId={}", GameRules.GAME_TIMEOUT_HOURS, gameId);
        if (started) {
            commandGateway.send(new GameCommand.EndGameCommand(gameId));
            return;
        }
        commandGateway.send(new GameCommand.CancelGameCommand(gameId, "GAME_EXPIRED"));
    }

    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.RoundStartedEvent event) {
        this.currentRound = event.round();
        this.currentCorrectAnswer = event.question().correctAnswer();

        questionRevealDeadlineId = deadlineManager.schedule(
                durationUntil(event.revealAt()),
                GameDeadline.QUESTION_REVEAL
        );
    }

    @DeadlineHandler(deadlineName = GameDeadline.QUESTION_REVEAL)
    public void onQuestionReveal() {
        commandGateway.send(new GameCommand.RevealQuestionCommand(gameId));
    }

    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.QuestionRevealedEvent event) {
        this.answersInCurrentRound = 0;
        this.player1Answered = false;
        this.player2Answered = false;

        roundDeadlineId = deadlineManager.schedule(
                durationUntil(event.answerDeadlineAt()),
                GameDeadline.ROUND_EXPIRED
        );

        if (GamePlayerType.BOT.equals(player2Type)) {
            botDeadlineId = deadlineManager.schedule(
                    randomBotAnswerDelay(),
                    GameDeadline.BOT_ANSWERS
            );
        }
    }

    @DeadlineHandler(deadlineName = GameDeadline.BOT_ANSWERS)
    public void onBotAnswers() {
        GameQuestionChoice botChoice = RANDOM.nextDouble() < botDifficulty.correctProbability()
                ? currentCorrectAnswer
                : randomWrongAnswer(currentCorrectAnswer);

        commandGateway.send(
                new GameCommand.AnswerQuestionCommand(gameId, player2Id, botChoice, Instant.now())
        );
    }

    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.QuestionAnsweredEvent event) {
        answersInCurrentRound++;
        if (event.playerId().equals(player1Id)) {
            player1Answered = true;
        } else {
            player2Answered = true;
        }

        if (answersInCurrentRound >= 2) {
            cancelRoundDeadlines();
            commandGateway.send(new GameCommand.CloseRoundCommand(gameId));
        }
    }

    @DeadlineHandler(deadlineName = GameDeadline.ROUND_EXPIRED)
    public void onRoundExpired() {
        if (!player1Answered) {
            commandGateway.send(new GameCommand.AnswerQuestionCommand(gameId, player1Id, null, Instant.now()));
        }
        if (!player2Answered) {
            commandGateway.send(new GameCommand.AnswerQuestionCommand(gameId, player2Id, null, Instant.now()));
        }
    }

    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.RoundClosedEvent event) {
        cancelQuestionRevealDeadline();

        if (event.nextRound() != null) {
            nextRoundDeadlineId = deadlineManager.schedule(
                    durationUntil(event.nextRoundAt()),
                    GameDeadline.NEXT_ROUND_STARTS
            );
        } else {
            commandGateway.send(new GameCommand.EndGameCommand(gameId));
        }
    }

    @DeadlineHandler(deadlineName = GameDeadline.NEXT_ROUND_STARTS)
    public void onNextRoundStarts() {
        commandGateway.send(new GameCommand.StartRoundCommand(gameId));
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.GameEndedEvent event) {
        logger.info("Game ended: gameId={}, winnerId={}", gameId, event.winnerId());
        cancelAll();
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.GameCancelledEvent event) {
        cancelAll();
    }

    private void cancelAll() {
        cancelMatchIntroDeadline();
        cancelQuestionRevealDeadline();
        cancelRoundDeadlines();
        cancelNextRoundDeadline();
        cancelExpiredDeadline();
        cancelStartDeadline();
    }

    private void cancelStartDeadline() {
        if (startDeadlineId != null) {
            deadlineManager.cancelSchedule(GameDeadline.START_TIMEOUT, startDeadlineId);
            startDeadlineId = null;
        }
    }

    private void cancelMatchIntroDeadline() {
        if (matchIntroDeadlineId != null) {
            deadlineManager.cancelSchedule(GameDeadline.MATCH_INTRO, matchIntroDeadlineId);
            matchIntroDeadlineId = null;
        }
    }

    private void cancelQuestionRevealDeadline() {
        if (questionRevealDeadlineId != null) {
            deadlineManager.cancelSchedule(GameDeadline.QUESTION_REVEAL, questionRevealDeadlineId);
            questionRevealDeadlineId = null;
        }
    }

    private void cancelRoundDeadlines() {
        if (roundDeadlineId != null) {
            deadlineManager.cancelSchedule(GameDeadline.ROUND_EXPIRED, roundDeadlineId);
            roundDeadlineId = null;
        }
        if (botDeadlineId != null) {
            deadlineManager.cancelSchedule(GameDeadline.BOT_ANSWERS, botDeadlineId);
            botDeadlineId = null;
        }
    }

    private void cancelNextRoundDeadline() {
        if (nextRoundDeadlineId != null) {
            deadlineManager.cancelSchedule(GameDeadline.NEXT_ROUND_STARTS, nextRoundDeadlineId);
            nextRoundDeadlineId = null;
        }
    }

    private void cancelExpiredDeadline() {
        if (expiredDeadlineId != null) {
            deadlineManager.cancelSchedule(GameDeadline.GAME_EXPIRED, expiredDeadlineId);
            expiredDeadlineId = null;
        }
    }

    private Duration durationUntil(Instant target) {
        Duration delay = Duration.between(Instant.now(), target);
        return delay.isNegative() ? Duration.ZERO : delay;
    }

    private Duration randomBotAnswerDelay() {
        long min = botDifficulty.minAnswerDelayMillis();
        long max = botDifficulty.maxAnswerDelayMillis();
        long delay = min + (long) (RANDOM.nextDouble() * (max - min + 1));
        return Duration.ofMillis(delay);
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
