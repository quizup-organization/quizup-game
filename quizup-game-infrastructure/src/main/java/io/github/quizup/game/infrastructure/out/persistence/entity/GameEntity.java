package io.github.quizup.game.infrastructure.out.persistence.entity;

import io.github.quizup.microservice.core.domain.model.search.FieldType;
import io.github.quizup.microservice.core.domain.model.search.Searchable;
import io.github.quizup.game.domain.model.GameMode;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité JPA pour la projection read-only de l'état global d'une partie.
 */
@Setter
@Getter
@Entity
@Table(name = "game_entry")
public class GameEntity {

    @Id
    @Searchable(type = FieldType.STRING)
    @Column(name = "game_id", nullable = false)
    private String gameId;

    @Searchable(type = FieldType.STRING)
    @Column(name = "topic_id", nullable = false)
    private String topicId;

    @Searchable(type = FieldType.STRING)
    @Column(name = "player1_id", nullable = false)
    private String player1Id;

    @Searchable(type = FieldType.STRING)
    @Column(name = "player1_name", nullable = false)
    private String player1Name;

    @Searchable(type = FieldType.STRING)
    @Column(name = "player2_id")
    private String player2Id;

    @Searchable(type = FieldType.STRING)
    @Column(name = "player2_name")
    private String player2Name;

    @Searchable(type = FieldType.STRING)
    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 10)
    private GameMode mode;

    @Searchable(type = FieldType.STRING)
    @Enumerated(EnumType.STRING)
    @Column(name = "player2Type", nullable = false, length = 10)
    private GamePlayerType opponent;

    @Searchable(type = FieldType.STRING)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GameStatus status;

    @Searchable(type = FieldType.NUMBER)
    @Column(name = "player1_score")
    private int player1Score;

    @Searchable(type = FieldType.NUMBER)
    @Column(name = "player2_score")
    private int player2Score;

    @Searchable(type = FieldType.STRING)
    @Column(name = "winner_id")
    private String winnerId;

    @Searchable(type = FieldType.DATE)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Searchable(type = FieldType.DATE)
    @Column(name = "started_at")
    private Instant startedAt;

    @Searchable(type = FieldType.DATE)
    @Column(name = "ended_at")
    private Instant endedAt;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("round ASC")
    private List<GameRoundEntity> rounds = new ArrayList<>();

}
