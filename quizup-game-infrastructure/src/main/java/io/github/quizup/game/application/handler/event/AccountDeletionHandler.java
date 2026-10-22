package io.github.quizup.game.application.handler.event;

import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.identity.domain.event.UserEvent.UserAccountDeletedEvent;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anonymise le nom du joueur dans les parties (projection uniquement). Les payloads historiques
 * de game restent inchangés — décision produit : l'historique des adversaires est conservé.
 */
@Component
@ProcessingGroup("account-deletion-game")
public class AccountDeletionHandler {

    private final JdbcTemplate jdbcTemplate;
    private final CommandGateway commandGateway;

    public AccountDeletionHandler(JdbcTemplate jdbcTemplate,
                                  CommandGateway commandGateway) {
        this.jdbcTemplate = jdbcTemplate;
        this.commandGateway = commandGateway;
    }

    @EventHandler
    @Transactional
    public void on(UserAccountDeletedEvent event) {
        String userId = event.userId();
        java.util.List<String> activeGameIds = jdbcTemplate.queryForList("""
                SELECT game_id FROM game_entry
                WHERE status IN ('CREATED', 'READY', 'IN_PROGRESS')
                  AND (player1_id = ? OR player2_id = ?)
                """, String.class, userId, userId);
        for (String gameId : activeGameIds) {
            commandGateway.send(new GameCommand.CancelGameCommand(gameId, "ACCOUNT_DELETED"));
        }
        jdbcTemplate.update(
                "UPDATE game_entry SET status = 'CANCELED' "
                        + "WHERE status IN ('CREATED', 'READY', 'IN_PROGRESS') "
                        + "AND (player1_id = ? OR player2_id = ?)",
                userId, userId);
        jdbcTemplate.update(
                "UPDATE game_entry SET player1_name = 'Joueur supprimé' WHERE player1_id = ?",
                userId);
        jdbcTemplate.update(
                "UPDATE game_entry SET player2_name = 'Joueur supprimé' WHERE player2_id = ?",
                userId);
    }
}
