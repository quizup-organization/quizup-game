-- V3: snapshot Elo des joueurs à la création de la partie (résultat relatif à la partie).
ALTER TABLE game_entry
    ADD COLUMN IF NOT EXISTS player1_rating INTEGER,
    ADD COLUMN IF NOT EXISTS player2_rating INTEGER;
