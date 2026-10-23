package io.github.quizup.game.domain.model;

/**
 * Snapshot de l'Elo d'un joueur au moment de la création d'une partie. Porté par l'agrégat
 * {@code game} afin que l'écran de résultat reflète l'Elo <b>à l'instant de la partie</b> — et
 * non la valeur courante du compte.
 *
 * <p>Le bot n'a pas de progression : il reçoit l'Elo de départ pour l'affichage.</p>
 */
public record PlayerProgressSnapshot(Integer rating) {

    public static PlayerProgressSnapshot forBot() {
        return new PlayerProgressSnapshot(200);
    }
}
