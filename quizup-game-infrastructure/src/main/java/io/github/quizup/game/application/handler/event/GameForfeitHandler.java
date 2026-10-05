package io.github.quizup.game.application.handler.event;

import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.port.out.GameRepositoryPort;
import io.github.quizup.profile.domain.event.PresenceEvent;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Forfait sur déconnexion : lorsqu'un joueur passe hors ligne (présence, service profile),
 * son duel synchrone humain en cours est clôturé à son détriment — l'adversaire gagne. Sa
 * présence sur l'écran de résultat de sa dernière partie terminée est également libérée afin
 * qu'une revanche en attente soit annulée ({@code PLAYER_LEFT}).
 *
 * <p>Les duels contre bot sont ignorés : {@code findActiveGameByPlayerId} ne remonte que les
 * parties en cours opposant deux humains.
 */
@Component
@ProcessingGroup("game-forfeit-handler")
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
        gameRepositoryPort.findActiveGameByPlayerId(event.userId())
                .ifPresent(game -> {
                    logger.info("Forfeiting game {}: player {} went offline",
                            game.gameId(), event.userId());
                    commandGateway.send(new GameCommand.ForfeitGameCommand(game.gameId(), event.userId()));
                });

        gameRepositoryPort.findLatestFinishedGameByPlayerId(event.userId())
                .ifPresent(game -> {
                    logger.debug("Releasing result-screen presence of game {}: player {} went offline",
                            game.gameId(), event.userId());
                    commandGateway.send(new GameCommand.LeaveGameCommand(game.gameId(), event.userId()));
                });
    }
}
