package io.github.quizup.game.application.handler.event;

import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.port.out.GameRepositoryPort;
import io.github.quizup.profile.domain.event.PresenceEvent;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Forfait sur déconnexion : lorsqu'un joueur passe hors ligne (présence, service profile),
 * son duel synchrone humain en cours est clôturé à son détriment — l'adversaire gagne.
 *
 * <p>Les parties asynchrones et les duels contre bot sont ignorés : {@code findActiveSyncGameByPlayerId}
 * ne remonte que les duels {@code SYNC} opposant deux humains.
 */
@Component
public class GameForfeitHandler {

    private static final Logger logger = LoggerFactory.getLogger(GameForfeitHandler.class);

    private final GameRepositoryPort gameRepositoryPort;
    private final CommandGateway commandGateway;

    public GameForfeitHandler(GameRepositoryPort gameRepositoryPort, CommandGateway commandGateway) {
        this.gameRepositoryPort = gameRepositoryPort;
        this.commandGateway = commandGateway;
    }

    @EventHandler
    public void on(PresenceEvent.PlayerWentOfflineEvent event) {
        gameRepositoryPort.findActiveSyncGameByPlayerId(event.userId())
                .ifPresent(game -> {
                    logger.info("Forfeiting game {}: player {} went offline",
                            game.gameId(), event.userId());
                    commandGateway.send(new GameCommand.EndGameCommand(game.gameId(), event.userId()));
                });
    }
}
