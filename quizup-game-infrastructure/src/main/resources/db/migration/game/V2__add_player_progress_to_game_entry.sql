-- V2: Snapshot de progression des joueurs à la création de la partie.
-- Permet à l'écran de résultat d'afficher le niveau/XP « à l'instant de la partie » plutôt que
-- la progression courante du compte. Le bot porte un niveau d'affichage dérivé de sa difficulté.
ALTER TABLE game_entry
    ADD COLUMN IF NOT EXISTS player1_level    INTEGER,
    ADD COLUMN IF NOT EXISTS player1_xp_total INTEGER,
    ADD COLUMN IF NOT EXISTS player2_level    INTEGER,
    ADD COLUMN IF NOT EXISTS player2_xp_total INTEGER;
