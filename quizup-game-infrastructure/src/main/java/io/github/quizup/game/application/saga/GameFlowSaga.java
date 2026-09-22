package io.github.quizup.game.application.saga;

import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.model.*;
import io.github.quizup.game.domain.port.out.GameEventStorePort;
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
 * Orchestration d'une partie, dans les trois saveurs du jeu :
 * <ul>
 *   <li><b>SYNC</b> : les deux joueurs rejoignent, la partie démarre à leur présence.</li>
 *   <li><b>ASYNC record</b> (joueur 2 absent) : run solo, le round se clôt dès la réponse du
 *       joueur (ou expiration) et la partie se termine par un {@code GameRunRecordedEvent} —
 *       sans vainqueur ni XP.</li>
 *   <li><b>ASYNC replay</b> (joueur 2 {@code GHOST}) : le fantôme rejoue les réponses d'un run
 *       enregistré, planifiées à leurs timings ; la partie se termine par le {@code GameEndedEvent}
 *       autoritaire (XP pour les deux joueurs).</li>
 * </ul>
 *
 * <p>Le serveur est seule source de vérité du temps : les deadlines de phase sont dérivées des
 * instants absolus portés par les événements ({@code firstRoundAt}, {@code revealAt},
 * {@code answerDeadlineAt}, {@code nextRoundAt}) — jamais des durées d'animation du client.
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

    @Autowired
    private transient GameEventStorePort gameEventStorePort;

    @Getter
    @Setter
    private String gameId;

    @Getter
    @Setter
    private GameMode mode;

    @Getter
    @Setter
    private String ghostGameId;

    /** Run asynchrone solo : un seul joueur présent, pas de fantôme. */
    @Getter
    @Setter
    private boolean singlePlayerRun;

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

    @Getter
    @Setter
    private String ghostDeadlineId;

    @StartSaga
    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.GameCreatedEvent event) {
        this.gameId = event.gameId();
        this.mode = event.mode();
        this.ghostGameId = event.ghostGameId();
        this.player1Id = event.player1Id();
        this.player2Id = event.player2Id();
        this.player2Type = event.player2Type();
        this.botDifficulty = BotDifficulty.fromOrDefault(event.botDifficulty());
        this.joinedCount = 0;

        if (GameMode.ASYNC.equals(event.mode())) {
            this.singlePlayerRun = isBlank(event.player2Id());
            commandGateway.send(new GameCommand.JoinGameCommand(gameId, player1Id));
            if (!singlePlayerRun) {
                commandGateway.send(new GameCommand.JoinGameCommand(gameId, player2Id));
            }
            commandGateway.send(new GameCommand.StartGameCommand(gameId));
            return;
        }

        commandGateway.send(new GameCommand.JoinGameCommand(gameId, player1Id));
        commandGateway.send(new GameCommand.JoinGameCommand(gameId, player2Id));
    }

    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.GameJoinedEvent event) {
        joinedCount++;

        if (GameMode.SYNC.equals(mode) && joinedCount == 2) {
            commandGateway.send(new GameCommand.StartGameCommand(gameId));
        }
    }

    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.GameStartedEvent event) {
        matchIntroDeadlineId = deadlineManager.schedule(
                durationUntil(event.firstRoundAt()),
                GameDeadline.MATCH_INTRO
        );
    }

    @DeadlineHandler(deadlineName = GameDeadline.MATCH_INTRO)
    public void onMatchIntro() {
        commandGateway.send(new GameCommand.StartRoundCommand(gameId));
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
        } else if (GamePlayerType.GHOST.equals(player2Type)) {
            scheduleGhostAnswer(event.round());
        }
    }

    @DeadlineHandler(deadlineName = GameDeadline.BOT_ANSWERS)
    public void onBotAnswers() {
        GameQuestionChoice botChoice = RANDOM.nextDouble() < botDifficulty.correctProbability()
                ? currentCorrectAnswer
                : randomWrongAnswer(currentCorrectAnswer);

        commandGateway.send(
                new GameCommand.AnswerQuestionCommand(
                        gameId,
                        player2Id,
                        botChoice,
                        Instant.now()
                )
        );
    }

    @DeadlineHandler(deadlineName = GameDeadline.GHOST_ANSWERS)
    public void onGhostAnswers() {
        GhostAnswer answer = findGhostAnswer(currentRound);
        if (answer == null || answer.choice() == null) {
            return;
        }
        commandGateway.send(
                new GameCommand.AnswerQuestionCommand(
                        gameId,
                        player2Id,
                        answer.choice(),
                        Instant.now()
                )
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

        boolean roundComplete = singlePlayerRun ? player1Answered : answersInCurrentRound >= 2;
        if (roundComplete) {
            cancelRoundDeadlines();
            commandGateway.send(new GameCommand.CloseRoundCommand(gameId));
        }
    }

    @DeadlineHandler(deadlineName = GameDeadline.ROUND_EXPIRED)
    public void onRoundExpired() {
        if (!player1Answered) {
            commandGateway.send(new GameCommand.AnswerQuestionCommand(
                    gameId, player1Id, null, Instant.now()));
        }
        if (!singlePlayerRun && !player2Answered) {
            commandGateway.send(new GameCommand.AnswerQuestionCommand(
                    gameId, player2Id, null, Instant.now()));
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
        } else if (singlePlayerRun) {
            commandGateway.send(new GameCommand.EndRunCommand(gameId));
        } else {
            commandGateway.send(new GameCommand.EndGameCommand(gameId, null));
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
    public void on(GameEvent.GameRunRecordedEvent event) {
        logger.info("Async run recorded: gameId={}, playerId={}", gameId, event.playerId());
        cancelAll();
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.GameCancelledEvent event) {
        cancelAll();
    }

    /** Rejoue la réponse enregistrée du fantôme pour le round donné, à son timing d'origine. */
    private void scheduleGhostAnswer(GameRoundType round) {
        GhostAnswer answer = findGhostAnswer(round);
        if (answer == null || answer.choice() == null) {
            return;
        }
        ghostDeadlineId = deadlineManager.schedule(
                Duration.ofMillis(Math.max(0, answer.timeMs())),
                GameDeadline.GHOST_ANSWERS
        );
    }

    private GhostAnswer findGhostAnswer(GameRoundType round) {
        if (isBlank(ghostGameId)) {
            return null;
        }
        return gameEventStorePort.findEventsByGameId(ghostGameId).stream()
                .filter(GameEvent.QuestionAnsweredEvent.class::isInstance)
                .map(GameEvent.QuestionAnsweredEvent.class::cast)
                .filter(answered -> answered.round() == round)
                .findFirst()
                .map(answered -> new GhostAnswer(answered.choice(), answered.timeMs()))
                .orElse(null);
    }

    private void cancelAll() {
        cancelMatchIntroDeadline();
        cancelQuestionRevealDeadline();
        cancelRoundDeadlines();
        cancelNextRoundDeadline();
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
        if (ghostDeadlineId != null) {
            deadlineManager.cancelSchedule(GameDeadline.GHOST_ANSWERS, ghostDeadlineId);
            ghostDeadlineId = null;
        }
    }

    private void cancelNextRoundDeadline() {
        if (nextRoundDeadlineId != null) {
            deadlineManager.cancelSchedule(GameDeadline.NEXT_ROUND_STARTS, nextRoundDeadlineId);
            nextRoundDeadlineId = null;
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Réponse enregistrée d'un run asynchrone (choix + temps depuis la révélation). */
    private record GhostAnswer(GameQuestionChoice choice, long timeMs) {
    }
}
