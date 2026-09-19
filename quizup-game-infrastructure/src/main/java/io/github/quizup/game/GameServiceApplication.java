package io.github.quizup.game;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Game Service - Moteur de jeu central avec GameAggregate et Sagas
 */
@SpringBootApplication
public class GameServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(GameServiceApplication.class, args);
    }
}
