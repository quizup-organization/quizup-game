package io.github.quizup.game.domain.aggregate;

import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.exception.GameExceptions;
import io.github.quizup.game.domain.model.*;
import io.github.quizup.game.domain.port.out.GameEventStorePort;
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
    public GameAggregate(GameCommand.CreateGameCommand command,
                         QuestionRepositoryPort questionRepositoryPort,
                         GameEventStorePort gameEventStorePort) {
        logger.info("Creating game: gameId={}, topicId={}, player1={}, player2={}, mode={}, player2Type={}",
                command.gameId(), command.topicId(), command.player1Id(), command.player2Id(), command.mode(), command.player2Type());

        if (StringUtils.isBlank(command.topicId())) {
            throw new GameExceptions.MissingTopicProblem(command.gameId());
        }

        if (StringUtils.isBlank(command.player1Id())) {
            throw new GameExceptions.MissingPlayerProblem(command.gameId(), GamePlayer.PLAYER_1);
        }

        // Le joueur 2 est requis en duel synchrone ; en asynchrone il est absent (run solo)
        // ou représenté par le fantôme d'un run enregistré.
        if (!GameMode.ASYNC.equals(command.mode()) && StringUtils.isBlank(command.player2Id())) {
            throw new GameExceptions.MissingPlayerProblem(command.gameId(), GamePlayer.PLAYER_2);
        }

        List<GameQuestion> questions = resolveQuestions(command, questionRepositoryPort, gameEventStorePort);

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
                        command.botDifficulty(),
                        command.ghostGameId(),
                        Instant.now()
                )
        );

    }

    /**
     * Un run en replay réutilise exactement les questions du run enregistré ({@code ghostGameId})
     * pour que les deux runs soient comparables ; un run solo ou un duel synchrone tire des
     * questions aléatoires.
     */
    private List<GameQuestion> resolveQuestions(GameCommand.CreateGameCommand command,
                                                QuestionRepositoryPort questionRepositoryPort,
                                                GameEventStorePort gameEventStorePort) {
        if (StringUtils.isNotBlank(command.ghostGameId())) {
            return gameEventStorePort.findEventsByGameId(command.ghostGameId()).stream()
                    .filter(GameEvent.GameCreatedEvent.class::isInstance)
                    .map(GameEvent.GameCreatedEvent.class::cast)
                    .findFirst()
                    .map(GameEvent.GameCreatedEvent::questions)
                    .orElseThrow(() -> new GameExceptions.GhostRunNotFoundProblem(command.ghostGameId()));
        }
        return questionRepositoryPort.findRandomApprovedByTopicId(
                command.topicId(),
                GameRules.TOTAL_ROUNDS
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

        Instant now = Instant.now();

        apply(
                new GameEvent.GameStartedEvent(
                        gameId,
                        mode,
                        now,
                        now.plusMillis(GameRules.MATCH_INTRO_MS)
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

        if (roundStatus == GameRoundStatus.QUESTION_SHOWN || roundStatus == GameRoundStatus.ANSWERABLE) {
            throw new GameExceptions.RoundNotStartableProblem(
                    gameId,
                    currentRound.name(), roundStatus.name()
            );
        }

        Instant now = Instant.now();

        apply(
                new GameEvent.RoundStartedEvent(
                        gameId,
                        currentRound,
                        round.getQuestion(),
                        now,
                        now.plusMillis(GameRules.QUESTION_REVEAL_MS)
                )
        );
    }

    @CommandHandler
    public void handle(GameCommand.RevealQuestionCommand command) {
        logger.info("Revealing question: gameId={}, round={}", gameId, currentRound);

        if (status != GameStatus.IN_PROGRESS) {
            throw new GameExceptions.GameNotInProgressProblem(
                    gameId,
                    status.name()
            );
        }

        GameRoundAggregate round = rounds.get(currentRound);

        if (round.getStatus() != GameRoundStatus.QUESTION_SHOWN) {
            throw new GameExceptions.RoundNotRevealableProblem(
                    gameId,
                    currentRound.name(),
                    round.getStatus().name()
            );
        }

        Instant now = Instant.now();

        apply(
                new GameEvent.QuestionRevealedEvent(
                        gameId,
                        currentRound,
                        now,
                        now.plusSeconds(GameRules.ROUND_TIMEOUT_SECONDS)
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

        if (!round.isAnswerable()) {
            throw new GameExceptions.RoundNotRevealedProblem(
                    gameId,
                    currentRound.name(),
                    round.getStatus().name()
            );
        }

        if (round.hasPlayerAnswered(player.getPlayer())) {
            throw new GameExceptions.RoundAlreadyAnsweredProblem(
                    gameId,
                    currentRound.name(),
                    command.playerId()
            );
        }

        Duration timeToAnswer = Duration.between(round.getRevealedAt(), command.timestamp());

        boolean correct = command.choice() != null && command.choice() == round.getQuestion().correctAnswer();

        int pointsEarned = 0;

        if (correct) {
            pointsEarned = GameRules.getBasePoints(currentRound.isBonus())
                    + GameRules.calculateSpeedBonus(timeToAnswer.toSeconds(), currentRound.isBonus());
        }

        long timeMs = round.getRevealedAt() != null
                ? Math.max(0, Duration.between(round.getRevealedAt(), command.timestamp()).toMillis())
                : 0;

        apply(
                new GameEvent.QuestionAnsweredEvent(
                        gameId,
                        currentRound,
                        round.getQuestion().questionId(),
                        command.playerId(),
                        player.getPlayerType(),
                        command.choice(),
                        correct,
                        command.timestamp(),
                        pointsEarned,
                        timeMs
                )
        );
    }


    @CommandHandler
    public void handle(GameCommand.CloseRoundCommand command) {
        logger.info("Closing round: gameId={}, round={}", gameId, currentRound);

        GameRoundAggregate round = rounds.get(currentRound);

        if (round.getStatus() != GameRoundStatus.ANSWERABLE) {
            throw new GameExceptions.RoundNotRevealedProblem(
                    gameId,
                    currentRound.name(),
                    round.getStatus().name()
            );
        }

        Instant now = Instant.now();
        GameRoundType nextRound = getNextRound(currentRound);

        apply(
                new GameEvent.RoundClosedEvent(
                        gameId,
                        currentRound,
                        nextRound,
                        round.getQuestion().correctAnswer(),
                        now,
                        nextRound != null ? now.plusMillis(GameRules.ROUND_TRANSITION_MS) : null
                )
        );
    }

    @CommandHandler
    public void handle(GameCommand.EndGameCommand command) {
        logger.info("Ending game: gameId={}, forfeitBy={}", gameId, command.forfeitById());

        if (status != GameStatus.IN_PROGRESS) {
            throw new GameExceptions.GameNotInProgressProblem(gameId, status == null ? "UNKNOWN" : status.name());
        }

        GamePlayerAggregate player1 = getPlayer(GamePlayer.PLAYER_1);
        GamePlayerAggregate player2 = getPlayer(GamePlayer.PLAYER_2);

        if (command.forfeitById() != null
                && !command.forfeitById().equals(player1.getPlayerId())
                && (player2 == null || !command.forfeitById().equals(player2.getPlayerId()))) {
            throw new GameExceptions.PlayerNotInGameProblem(gameId, command.forfeitById());
        }

        String winner = resolveWinner(player1, player2, command.forfeitById());

        apply(
                new GameEvent.GameEndedEvent(
                        gameId,
                        winner,
                        player1.getPlayerId(),
                        player1.getPlayerName(),
                        player2 != null ? player2.getPlayerId() : null,
                        player2 != null ? player2.getPlayerName() : null,
                        topicId,
                        player1.getScore(),
                        player2 != null ? player2.getScore() : 0,
                        countCorrect(GamePlayer.PLAYER_1),
                        countFast(GamePlayer.PLAYER_1),
                        player2 != null ? countCorrect(GamePlayer.PLAYER_2) : 0,
                        player2 != null ? countFast(GamePlayer.PLAYER_2) : 0,
                        command.forfeitById(),
                        Instant.now()
                )
        );
    }

    /**
     * Vainqueur : l'abandon d'un joueur donne la victoire à l'adversaire ; sinon le meilleur
     * score (égalité → pas de vainqueur). Un run async solo (pas de joueur 2) n'a pas de
     * vainqueur.
     */
    private String resolveWinner(GamePlayerAggregate player1,
                                 GamePlayerAggregate player2,
                                 String forfeitById) {
        if (player2 == null) {
            return null;
        }
        if (forfeitById != null) {
            if (forfeitById.equals(player1.getPlayerId())) {
                return player2.getPlayerId();
            }
            if (forfeitById.equals(player2.getPlayerId())) {
                return player1.getPlayerId();
            }
        }
        if (player1.getScore() > player2.getScore()) {
            return player1.getPlayerId();
        }
        if (player2.getScore() > player1.getScore()) {
            return player2.getPlayerId();
        }
        return null;
    }

    /**
     * Clôt un run asynchrone solo : enregistre le run (score du joueur présent) sans vainqueur
     * ni XP. Le replay produira le {@code GameEndedEvent} autoritaire.
     */
    @CommandHandler
    public void handle(GameCommand.EndRunCommand command) {
        logger.info("Recording async run: gameId={}", gameId);

        GamePlayerAggregate player1 = getPlayer(GamePlayer.PLAYER_1);

        apply(
                new GameEvent.GameRunRecordedEvent(
                        gameId,
                        player1.getPlayerId(),
                        topicId,
                        player1.getScore(),
                        Instant.now()
                )
        );
    }

    private int countCorrect(GamePlayer player) {
        return (int) rounds.values().stream()
                .map(round -> round.getAnswer(player))
                .filter(answer -> answer != null && answer.correct())
                .count();
    }

    private int countFast(GamePlayer player) {
        return (int) rounds.values().stream()
                .filter(round -> {
                    PlayerAnswer answer = round.getAnswer(player);

                    if (answer == null || !answer.correct() || answer.answeredAt() == null || round.getRevealedAt() == null) {
                        return false;
                    }

                    return Duration.between(round.getRevealedAt(), answer.answeredAt()).toSeconds()
                            < GameRules.FAST_ANSWER_SECONDS;
                })
                .count();
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

        // En run asynchrone solo, le joueur 2 est absent : aucun sous-agrégat joueur n'est créé.
        if (StringUtils.isNotBlank(event.player2Id())) {
            players.put(GamePlayer.PLAYER_2, new GamePlayerAggregate(GamePlayer.PLAYER_2, event.player2Id(), event.player2Name(), event.player2Type()));
        }

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
        round.showQuestion(event.shownAt());
    }

    @EventSourcingHandler
    public void on(GameEvent.QuestionRevealedEvent event) {
        GameRoundAggregate round = rounds.get(event.round());
        round.reveal(event.revealedAt(), event.answerDeadlineAt());
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
                        event.pointsEarned(),
                        event.answeredAt()
                )
        );
        player.addScore(event.pointsEarned());
    }

    @EventSourcingHandler
    public void on(GameEvent.RoundClosedEvent event) {
        rounds.get(event.closedRound()).closeRound(event.closedAt());
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

    @EventSourcingHandler
    public void on(GameEvent.GameRunRecordedEvent event) {
        this.status = GameStatus.AWAITING_OPPONENT;
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
