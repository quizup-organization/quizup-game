package io.github.quizup.game.domain.aggregate;

import io.github.quizup.game.domain.model.*;
import lombok.Getter;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

/**
 * Sous-entité d'un round de partie, gérée par le GameAggregate parent.
 * Ne contient que l'état et les transitions simples — aucune logique d'orchestration.
 *
 * <p>Cycle en deux phases distinctes : la question est d'abord <b>affichée</b>
 * ({@link #showQuestion(Instant)}) sans chrono, puis les réponses sont <b>révélées</b>
 * ({@link #reveal(Instant, Instant)}) et le chrono démarre jusqu'à {@code answerDeadlineAt}.
 * Le temps de réponse est mesuré depuis {@code revealedAt}, jamais depuis l'affichage.
 *
 * <p>Utilise {@link GamePlayer} pour indexer les réponses, éliminant les booléens
 * {@code isPlayer1} et les if/else en cascade.
 */
@Getter
public class GameRoundAggregate {

    private final GameRoundType roundId;

    private final GameQuestion question;

    private GameRoundStatus status;

    /** Instant où la question a été envoyée aux joueurs (début de la phase de lecture). */
    private Instant shownAt;

    /** Instant où les réponses ont été révélées et le chrono armé. */
    private Instant revealedAt;

    /** Échéance absolue du chrono : après cet instant, le round est clos. */
    private Instant answerDeadlineAt;

    private Instant closedAt;

    private final Map<GamePlayer, PlayerAnswer> answers = new EnumMap<>(GamePlayer.class);

    public GameRoundAggregate(GameRoundType roundId, GameQuestion question) {
        this.roundId = roundId;
        this.question = question;
        this.status = GameRoundStatus.CREATED;
    }

    /** La question est affichée ; le chrono n'est pas encore armé. */
    public void showQuestion(Instant shownAt) {
        this.status = GameRoundStatus.QUESTION_SHOWN;
        this.shownAt = shownAt;
    }

    /** Les réponses sont révélées : le chrono démarre et court jusqu'à {@code answerDeadlineAt}. */
    public void reveal(Instant revealedAt, Instant answerDeadlineAt) {
        this.status = GameRoundStatus.ANSWERABLE;
        this.revealedAt = revealedAt;
        this.answerDeadlineAt = answerDeadlineAt;
    }

    public void closeRound(Instant closedAt) {
        this.status = GameRoundStatus.CLOSED;
        this.closedAt = closedAt;
    }

    public boolean isAnswerable() {
        return status == GameRoundStatus.ANSWERABLE;
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
