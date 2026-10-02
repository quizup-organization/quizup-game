package io.github.quizup.game.domain.port.in;

import io.github.quizup.game.domain.command.GameCommand;

import java.util.concurrent.CompletableFuture;

/**
 * Port entrant — sortie d'un joueur de la salle d'attente avant le démarrage (annule la partie).
 */
public interface LeaveGameUseCase {

    CompletableFuture<String> leave(GameCommand.LeaveGameCommand command);

    default CompletableFuture<String> leave(String gameId, String playerId) {
        return leave(new GameCommand.LeaveGameCommand(gameId, playerId));
    }
}
