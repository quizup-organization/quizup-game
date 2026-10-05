package io.github.quizup.game.application.saga;

import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.model.GameDeadline;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
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

import java.util.Set;
import java.util.UUID;

/**
 * Orchestration de la revanche : chaque demande en attente après la fin d'une partie est
 * associée à une saga. À l'acceptation, la saga crée la nouvelle partie (mêmes joueurs,
 * mêmes langues) puis confirme la revanche sur l'ancienne partie ; la demande expire après
 * {@link GameDeadline#REMATCH_EXPIRY_DURATION}.
 */
@Saga
@ProcessingGroup("game-rematch-saga")
public class RematchSaga {

    private static final Logger logger = LoggerFactory.getLogger(RematchSaga.class);

    @Autowired
    private transient CommandGateway commandGateway;

    @Autowired
    private transient DeadlineManager deadlineManager;

    @Getter
    @Setter
    private String gameId;

    @Getter
    @Setter
    private String rematchRequesterId;

    @Getter
    @Setter
    private String requesterName;

    @Getter
    @Setter
    private String opponentId;

    @Getter
    @Setter
    private String opponentName;

    @Getter
    @Setter
    private String topicId;

    @Getter
    @Setter
    private Set<Language> languages;

    @Getter
    @Setter
    private String expiryDeadlineId;

    @Getter
    @Setter
    private boolean accepted;

    @StartSaga
    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.RematchRequestedEvent event) {
        this.gameId = event.gameId();
        this.rematchRequesterId = event.requesterId();
        this.requesterName = event.requesterName();
        this.opponentId = event.opponentId();
        this.opponentName = event.opponentName();
        this.topicId = event.topicId();
        this.languages = event.languages();
        this.accepted = false;
        this.expiryDeadlineId = deadlineManager.schedule(
                GameDeadline.REMATCH_EXPIRY_DURATION,
                GameDeadline.REMATCH_EXPIRY
        );
    }

    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.RematchAcceptedEvent event) {
        if (accepted) {
            return;
        }
        accepted = true;
        cancelExpiry();

        String newGameId = UUID.randomUUID().toString();

        try {
            commandGateway.sendAndWait(
                    new GameCommand.CreateGameCommand(
                            newGameId,
                            topicId,
                            rematchRequesterId,
                            requesterName,
                            opponentId,
                            opponentName,
                            languages,
                            GamePlayerType.HUMAN,
                            null
                    )
            );
            commandGateway.send(new GameCommand.ConfirmRematchCommand(gameId, newGameId));
        } catch (Exception e) {
            logger.warn("Échec de création de la revanche: gameId={}, newGameId={}", gameId, newGameId, e);
            commandGateway.send(new GameCommand.AbortRematchCommand(gameId, "CREATE_FAILED"));
        }
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.RematchDeclinedEvent event) {
        cancelExpiry();
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.RematchCancelledEvent event) {
        cancelExpiry();
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "gameId")
    public void on(GameEvent.RematchStartedEvent event) {
        cancelExpiry();
    }

    @DeadlineHandler(deadlineName = GameDeadline.REMATCH_EXPIRY)
    public void onExpiry() {
        logger.info("Demande de revanche expirée: gameId={}", gameId);
        commandGateway.send(new GameCommand.AbortRematchCommand(gameId, "EXPIRED"));
    }

    private void cancelExpiry() {
        if (expiryDeadlineId != null) {
            deadlineManager.cancelSchedule(GameDeadline.REMATCH_EXPIRY, expiryDeadlineId);
            expiryDeadlineId = null;
        }
    }
}
