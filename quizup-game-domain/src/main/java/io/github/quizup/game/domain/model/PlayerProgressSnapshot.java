package io.github.quizup.game.domain.model;

/**
 * Snapshot de la progression d'un joueur (niveau + XP totale) au moment de la création d'une
 * partie. Porté par l'agrégat {@code game} afin que l'écran de résultat reflète la progression
 * <b>à l'instant de la partie</b> — et non la progression courante du compte.
 *
 * <p>Le bot n'a pas de progression : l'appelant fournit un niveau d'affichage dérivé de sa
 * difficulté.</p>
 */
public record PlayerProgressSnapshot(int level, int xpTotal) {

    /**
     * Niveau/XP d'<b>affichage</b> du bot selon sa difficulté : le bot n'a pas de progression
     * réelle, on lui prête un palier pour l'écran de résultat.
     */
    public static PlayerProgressSnapshot forBot(BotDifficulty difficulty) {
        int level = switch (BotDifficulty.fromOrDefault(difficulty)) {
            case EASY -> 1;
            case NORMAL -> 3;
            case HARD -> 5;
        };
        return new PlayerProgressSnapshot(level, 100 * (level - 1) * (level - 1));
    }
}
