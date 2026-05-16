package io.github.quizup.game.domain.aggregate;

import io.github.quizup.game.domain.model.*;
import lombok.Getter;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

/**
 * Sous-entité d'un round de partie, gérée par le GameAggregate parent.
 * Ne contient que l'état et les transitions simples — aucune logique d'orchestration.
 * Utilise {@link GamePlayer} pour indexer les réponses,
 * éliminant les booléens {@code isPlayer1} et les if/else en cascade.
 */
@Getter
public class GameRoundAggregate {

    private final GameRoundType roundId;

    private final GameQuestion question;

    private GameRoundStatus status;

    private Instant startedAt;

    private Instant closedAt;

    private final Map<GamePlayer, PlayerAnswer> answers = new EnumMap<>(GamePlayer.class);

    public GameRoundAggregate(GameRoundType roundId, GameQuestion question) {
        this.roundId = roundId;
        this.question = question;
        this.status = GameRoundStatus.CREATED;
    }

    public void startRound() {
        this.status = GameRoundStatus.STARTED;
        this.startedAt = Instant.now();
    }

    public void closeRound() {
        this.status = GameRoundStatus.CLOSED;
        this.closedAt = Instant.now();
    }

    public void recordAnswer(PlayerAnswer answer) {
        this.answers.put(answer.player(), answer);
    }

    public boolean hasPlayerAnswered(GamePlayer gamePlayer) {
        return answers.containsKey(gamePlayer);
    }

    public PlayerAnswer getAnswer(GamePlayer gamePlayer) {
        return answers.get(gamePlayer);
    }

    public PlayerAnswer getPlayer1Answer() {
        return getAnswer(GamePlayer.PLAYER_1);
    }

    public PlayerAnswer getPlayer2Answer() {
        return getAnswer(GamePlayer.PLAYER_2);
    }
}
