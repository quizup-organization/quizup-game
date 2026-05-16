-- V1: Création du schéma game
-- Tables : game_entry, round_entry

CREATE TABLE game_entry (
    game_id       VARCHAR(255)  NOT NULL,
    topic_id      VARCHAR(255)  NOT NULL,
    player1_id    VARCHAR(255)  NOT NULL,
    player2_id    VARCHAR(255),
    mode          VARCHAR(10)   NOT NULL,   -- SYNC, ASYNC
    player2Type   VARCHAR(10)   NOT NULL,   -- BOT, HUMAN
    status        VARCHAR(20)   NOT NULL,   -- CREATED, READY, IN_PROGRESS, FINISHED, CANCELED
    player1_score INTEGER       DEFAULT 0,
    player2_score INTEGER       DEFAULT 0,
    winner_id     VARCHAR(255),
    created_at    TIMESTAMP     NOT NULL,
    started_at    TIMESTAMP,
    ended_at      TIMESTAMP,
    version       BIGINT,
    PRIMARY KEY (game_id)
);

CREATE TABLE round_entry (
    id              VARCHAR(255)  NOT NULL,
    game_id         VARCHAR(255)  NOT NULL,
    round           VARCHAR(10)   NOT NULL,   -- ROUND_1..ROUND_7
    question_id     VARCHAR(255)  NOT NULL,
    question_text   VARCHAR(1000) NOT NULL,
    correct_answer  VARCHAR(5)    NOT NULL,   -- A, B, C, D
    player1_choice  VARCHAR(5),
    player1_time_ms BIGINT,
    player1_points  INTEGER       DEFAULT 0,
    player2_choice  VARCHAR(5),
    player2_time_ms BIGINT,
    player2_points  INTEGER       DEFAULT 0,
    status          VARCHAR(10)   NOT NULL,   -- CREATED, STARTED, CLOSED
    PRIMARY KEY (id),
    CONSTRAINT fk_round_game FOREIGN KEY (game_id) REFERENCES game_entry(game_id) ON DELETE CASCADE
);

CREATE INDEX idx_round_game_id ON round_entry(game_id);
