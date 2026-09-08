package io.github.quizup.game.domain.aggregate;

import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.exception.GameExceptions;
import io.github.quizup.game.domain.model.*;
import io.github.quizup.game.domain.port.out.QuestionRepositoryPort;
import org.apache.commons.lang3.StringUtils;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static org.axonframework.modelling.command.AggregateLifecycle.apply;

/**
 * GameAggregate — Cycle de vie d'une partie.
 * <p>
 * Utilise {@link GamePlayerAggregate} pour encapsuler l'état de chaque joueur
 * (identité, présence, score), éliminant les maps séparées et les if/else en cascade.
 * <p>
 * Les timeouts sont orchestrés par les sagas applicatives.
 */
@Aggregate
public class GameAggregate {
    private static final Logger logger = LoggerFactory.getLogger(GameAggregate.class);

    @AggregateIdentifier
    private String gameId;
    private String topicId;
    private GameMode mode;
    private GameStatus status;
    private final Map<GamePlayer, GamePlayerAggregate> players = new EnumMap<>(GamePlayer.class);
    private final Map<GameRoundType, GameRoundAggregate> rounds = new EnumMap<>(GameRoundType.class);
    private GameRoundType currentRound;

    protected GameAggregate() {
    }

    // =============================================
    // COMMAND HANDLERS
    // =============================================

    @CommandHandler
    public GameAggregate(GameCommand.CreateGameCommand command, QuestionRepositoryPort questionRepositoryPort) {
        logger.info("Creating game: gameId={}, topicId={}, player1={}, player2={}, mode={}, player2Type={}",
                command.gameId(), command.topicId(), command.player1Id(), command.player2Id(), command.mode(), command.player2Type());

        if (StringUtils.isBlank(command.topicId())) {
            throw new GameExceptions.MissingTopicProblem(command.gameId());
        }

        if (StringUtils.isBlank(command.player1Id())) {
            throw new GameExceptions.MissingPlayerProblem(command.gameId(), GamePlayer.PLAYER_1);
        }

        if (StringUtils.isBlank(command.player2Id())) {
            throw new GameExceptions.MissingPlayerProblem(command.gameId(), GamePlayer.PLAYER_2);
        }

        List<GameQuestion> questions = questionRepositoryPort.findRandomApprovedByTopicId(
                command.topicId(),
                GameRules.TOTAL_ROUNDS
        );

        apply(
                new GameEvent.GameCreatedEvent(
                        command.gameId(),
                        command.topicId(),
                        command.player1Id(),
                        command.player1Name(),
                        command.player2Id(),
                        command.player2Name(),
                        command.player2Type(),
                        command.mode(),
                        questions,
                        Instant.now()
                )
        );

    }

    @CommandHandler
    public void handle(GameCommand.JoinGameCommand command) {
        logger.info("Joining game: gameId={}, playerId={}", gameId, command.playerId());

        if (status != GameStatus.CREATED) {
            throw new GameExceptions.GameNotJoinableProblem(gameId, status.name());
        }

        GamePlayerAggregate player = resolvePlayer(command.playerId());

        if (player.isPresent()) {
            throw new GameExceptions.PlayerAlreadyJoinedProblem(gameId, command.playerId());
        }

        apply(
                new GameEvent.GameJoinedEvent(
                        gameId,
                        command.playerId(),
                        Instant.now()
                )
        );
    }

    @CommandHandler
    public void handle(GameCommand.StartGameCommand command) {
        logger.info("Starting game: gameId={}, mode={}, status={}", gameId, mode, status);

        switch (mode) {
            case SYNC -> {
                if (status != GameStatus.READY) {
                    throw new GameExceptions.GameNotReadyProblem(
                            gameId,
                            status.name()
                    );
                }
            }
            case ASYNC -> {
                if (status != GameStatus.CREATED && status != GameStatus.READY) {
                    throw new GameExceptions.GameNotStartableProblem(
                            gameId,
                            status.name()
                    );
                }
            }
        }

        apply(
                new GameEvent.GameStartedEvent(
                        gameId,
                        mode,
                        Instant.now()
                )
        );
    }

    @CommandHandler
    public void handle(GameCommand.StartRoundCommand command) {
        logger.info("Starting round: gameId={}, round={}", gameId, currentRound);

        if (status != GameStatus.IN_PROGRESS) {
            throw new GameExceptions.GameNotInProgressProblem(
                    gameId,
                    status.name()
            );
        }

        GameRoundAggregate round = rounds.get(currentRound);

        GameRoundStatus roundStatus = round.getStatus();

        if (roundStatus == GameRoundStatus.STARTED) {
            throw new GameExceptions.RoundNotStartableProblem(
                    gameId,
                    currentRound.name(), roundStatus.name()
            );
        }

        apply(
                new GameEvent.RoundStartedEvent(
                        gameId,
                        currentRound,
                        round.getQuestion(),
                        Instant.now()
                )
        );
    }

    @CommandHandler
    public void handle(GameCommand.AnswerQuestionCommand command) {
        logger.info("Answering question: gameId={}, playerId={}, choice={}", gameId, command.playerId(), command.choice());

        if (StringUtils.isBlank(command.playerId())) {
            throw new GameExceptions.MissingPlayerIdProblem(gameId);
        }

        if (isNull(command.timestamp())) {
            throw new GameExceptions.MissingTimestampProblem(gameId);
        }

        GamePlayerAggregate player = resolvePlayer(command.playerId());

        GameRoundAggregate round = rounds.get(currentRound);

        if (round.getStatus() != GameRoundStatus.STARTED) {
            throw new GameExceptions.RoundNotStartedProblem(
                    gameId,
                    currentRound.name()
            );
        }

        if (round.hasPlayerAnswered(player.getPlayer())) {
            throw new GameExceptions.RoundAlreadyAnsweredProblem(
                    gameId,
                    currentRound.name(),
                    command.playerId()
            );
        }

        Duration timeToAnswer = Duration.between(round.getStartedAt(), command.timestamp());

        boolean correct = command.choice() != null && command.choice() == round.getQuestion().correctAnswer();

        int pointsEarned = 0;

        if (correct) {
            pointsEarned = GameRules.getBasePoints(currentRound.isBonus()) + GameRules.calculateSpeedBonus(timeToAnswer.toSeconds());
        }

        apply(
                new GameEvent.QuestionAnsweredEvent(
                        gameId,
                        currentRound,
                        command.playerId(),
                        command.choice(),
                        correct,
                        command.timestamp(),
                        pointsEarned
                )
        );
    }


    @CommandHandler
    public void handle(GameCommand.CloseRoundCommand command) {
        logger.info("Closing round: gameId={}, round={}", gameId, currentRound);

        GameRoundAggregate round = rounds.get(currentRound);

        if (round.getStatus() != GameRoundStatus.STARTED) {
            throw new GameExceptions.RoundNotStartedProblem(
                    gameId,
                    currentRound.name()
            );
        }

        apply(
                new GameEvent.RoundClosedEvent(
                        gameId,
                        currentRound,
                        getNextRound(currentRound)
                )
        );
    }

    @CommandHandler
    public void handle(GameCommand.EndGameCommand command) {
        logger.info("Ending game: gameId={}", gameId);

        GamePlayerAggregate player1 = getPlayer(GamePlayer.PLAYER_1);
        GamePlayerAggregate player2 = getPlayer(GamePlayer.PLAYER_2);

        String winner = null;

        if (player1.getScore() > player2.getScore()) {
            winner = player1.getPlayerId();
        } else if (player2.getScore() > player1.getScore()) {
            winner = player2.getPlayerId();
        } // else it's a tie, winner remains null

        apply(
                new GameEvent.GameEndedEvent(
                        gameId,
                        winner,
                        player1.getPlayerId(),
                        player1.getPlayerName(),
                        player2.getPlayerId(),
                        player2.getPlayerName(),
                        topicId,
                        player1.getScore(),
                        player2.getScore(),
                        Instant.now()
                )
        );
    }

    @CommandHandler
    public void handle(GameCommand.CancelGameCommand command) {
        logger.info("Canceling game: gameId={}, reason={}", gameId, command.reason());
        apply(
                new GameEvent.GameCancelledEvent(
                        gameId,
                        command.reason(),
                        Instant.now()
                )
        );
    }


    // =============================================
    // EVENT SOURCING HANDLERS
    // =============================================

    @EventSourcingHandler
    public void on(GameEvent.GameCreatedEvent event) {
        this.gameId = event.gameId();
        this.topicId = event.topicId();
        this.mode = event.mode();
        this.status = GameStatus.CREATED;
        this.currentRound = GameRoundType.ROUND_1;

        players.put(GamePlayer.PLAYER_1, new GamePlayerAggregate(GamePlayer.PLAYER_1, event.player1Id(), event.player1Name(), GamePlayerType.HUMAN));
        players.put(GamePlayer.PLAYER_2, new GamePlayerAggregate(GamePlayer.PLAYER_2, event.player2Id(), event.player2Name(), event.player2Type()));

        GameRoundType[] allRounds = GameRoundType.values();

        List<GameQuestion> questions = event.questions();

        for (int i = 0; i < questions.size(); i++) {
            rounds.put(allRounds[i], new GameRoundAggregate(allRounds[i], questions.get(i)));
        }
    }

    @EventSourcingHandler
    public void on(GameEvent.GameJoinedEvent event) {
        GamePlayerAggregate player = resolvePlayer(event.playerId());
        player.join();

        if (getPlayer(GamePlayer.PLAYER_1).isPresent() && getPlayer(GamePlayer.PLAYER_2).isPresent()) {
            this.status = GameStatus.READY;
        }
    }

    @EventSourcingHandler
    public void on(GameEvent.GameStartedEvent event) {
        this.status = GameStatus.IN_PROGRESS;
    }

    @EventSourcingHandler
    public void on(GameEvent.RoundStartedEvent event) {
        GameRoundAggregate round = rounds.get(event.round());
        round.startRound();
    }

    @EventSourcingHandler
    public void on(GameEvent.QuestionAnsweredEvent event) {
        GameRoundAggregate round = rounds.get(event.round());
        GamePlayerAggregate player = resolvePlayer(event.playerId());
        round.recordAnswer(
                new PlayerAnswer(
                        player.getPlayer(),
                        event.choice(),
                        event.correct(),
                        event.pointsEarned()
                )
        );
        player.addScore(event.pointsEarned());
    }

    @EventSourcingHandler
    public void on(GameEvent.RoundClosedEvent event) {
        rounds.get(event.closedRound()).closeRound();
        this.currentRound = event.nextRound();
    }

    @EventSourcingHandler
    public void on(GameEvent.GameEndedEvent event) {
        this.status = GameStatus.FINISHED;
    }

    @EventSourcingHandler
    public void on(GameEvent.GameCancelledEvent event) {
        this.status = GameStatus.CANCELED;
    }

    // =============================================
    // UTILITY
    // =============================================

    /**
     * Résout le {@link GamePlayerAggregate} à partir d'un playerId.
     *
     * @throws GameExceptions.PlayerNotInGameProblem si le joueur n'est ni player1 ni player2
     */
    private GamePlayerAggregate resolvePlayer(String playerId) {
        return players.values().stream()
                .filter(gamePlayerAggregate -> gamePlayerAggregate.matches(playerId))
                .findFirst()
                .orElseThrow(() -> new GameExceptions.PlayerNotInGameProblem(gameId, playerId));
    }

    /**
     * Accès direct à un joueur par son slot.
     */
    private GamePlayerAggregate getPlayer(GamePlayer slot) {
        return players.get(slot);
    }

    private GameRoundType getNextRound(GameRoundType current) {
        GameRoundType[] allRounds = GameRoundType.values();
        int idx = current.ordinal();
        return (idx + 1 < allRounds.length) ? allRounds[idx + 1] : null;
    }
}
