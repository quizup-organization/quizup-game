package io.github.quizup.game.domain.aggregate;

import io.github.quizup.game.domain.model.GamePlayer;
import io.github.quizup.game.domain.model.GamePlayerType;
import lombok.Getter;

/**
 * Sous-entité d'un joueur dans une partie, gérée par le GameAggregate parent.
 * Encapsule l'identité, la présence et le score d'un joueur.
 */
@Getter
public class GamePlayerAggregate {

    private final GamePlayer player;

    private final String playerId;

    private final GamePlayerType playerType;

    private boolean present;

    private int score;

    public GamePlayerAggregate(GamePlayer player, String playerId, GamePlayerType playerType) {
        this.player = player;
        this.playerId = playerId;
        this.playerType = playerType;
        this.present = false;
        this.score = 0;
    }

    /**
     * Marque le joueur comme présent dans la partie.
     */
    public void join() {
        this.present = true;
    }

    /**
     * Ajoute des points au score du joueur.
     */
    public void addScore(int points) {
        this.score += points;
    }

    /**
     * Vérifie si ce slot correspond au playerId donné.
     */
    public boolean matches(String playerId) {
        return this.playerId.equals(playerId);
    }
}

