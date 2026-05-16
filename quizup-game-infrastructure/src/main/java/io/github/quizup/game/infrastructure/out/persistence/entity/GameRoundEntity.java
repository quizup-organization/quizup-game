package io.github.quizup.game.infrastructure.out.persistence.entity;

import io.github.quizup.game.domain.model.GameRoundType;
import io.github.quizup.game.domain.model.GameRoundStatus;
import io.github.quizup.topic.domain.model.QuestionChoice;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entité JPA pour la projection read-only d'un round de partie.
 */
@Getter
@Setter
@Entity
@Table(name = "round_entry")
public class GameRoundEntity {

    // Getters & Setters
    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private GameEntity game;

    @Enumerated(EnumType.STRING)
    @Column(name = "round", nullable = false, length = 10)
    private GameRoundType round;

    @Column(name = "question_id", nullable = false)
    private String questionId;

    @Column(name = "question_text", nullable = false, length = 1000)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "correct_answer", nullable = false, length = 5)
    private QuestionChoice correctAnswer;

    @Enumerated(EnumType.STRING)
    @Column(name = "player1_choice", length = 5)
    private QuestionChoice player1Choice;

    @Column(name = "player1_time_ms")
    private Long player1TimeMs;

    @Column(name = "player1_points")
    private int player1Points;

    @Enumerated(EnumType.STRING)
    @Column(name = "player2_choice", length = 5)
    private QuestionChoice player2Choice;

    @Column(name = "player2_time_ms")
    private Long player2TimeMs;

    @Column(name = "player2_points")
    private int player2Points;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private GameRoundStatus status;
}
