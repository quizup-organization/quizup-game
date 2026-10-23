-- V4: le niveau et l'XP globale ne sont plus des métriques joueur.
-- L'écran de résultat conserve l'Elo au moment de la partie.
ALTER TABLE game_entry
    DROP COLUMN IF EXISTS player1_level,
    DROP COLUMN IF EXISTS player1_xp_total,
    DROP COLUMN IF EXISTS player2_level,
    DROP COLUMN IF EXISTS player2_xp_total;
