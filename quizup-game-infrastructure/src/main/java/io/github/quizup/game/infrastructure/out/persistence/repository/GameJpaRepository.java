package io.github.quizup.game.infrastructure.out.persistence.repository;

import io.github.quizup.game.domain.model.GameStatus;
import io.github.quizup.game.infrastructure.out.persistence.entity.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameJpaRepository extends JpaRepository<GameEntity, String>, JpaSpecificationExecutor<GameEntity> {

    List<GameEntity> findByPlayer1IdOrPlayer2Id(
            String player1Id,
            String player2Id
    );

    List<GameEntity> findByPlayer1IdAndStatusOrPlayer2IdAndStatus(
            String player1Id, GameStatus status1,
            String player2Id, GameStatus status2
    );
}

