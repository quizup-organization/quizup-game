package io.github.quizup.game.domain.model;

/**
 * Mode de jeu
 */
public enum GameMode {
    SYNC,  // Mode synchrone — les deux joueurs jouent en temps réel
    ASYNC  // Mode asynchrone — le joueur 1 joue seul, puis le joueur 2 rejoue contre le "ghost"
}
