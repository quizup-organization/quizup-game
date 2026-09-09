# AGENTS.md — quizup-game

> Service de **game** : parties (duels) de quiz, rounds, scoring. **Référence implémentation des
> patterns avancés** : sous-agrégats, sagas, deadlines, event store. Architecture : Axon
> Framework (CQRS/EDA) + JPA (projections).
> Pour les règles de patterns : [
`../../best-practices/hexagonal-architecture.md`](../../best-practices/hexagonal-architecture.md).

---

## 1. Rôle

Gestion des **parties** de quiz : création (bot), participation (join), réponse aux questions,
scoring, annulation. Les questions proviennent de `quizup-theme`. Le bot est un utilisateur
spécial (`QuizUpConstants.BOT_USER_ID`).

**Package** : `io.github.quizup.game`

---

## 2. Endpoints REST

### `GameController` — `/api/games`

| Méthode | Chemin                              | Handler                                         | Response                       |
|---------|-------------------------------------|-------------------------------------------------|--------------------------------|
| POST    | `/api/games/search`                 | `search(SearchRequest)`                         | `PageResponse<GameResponse>`   |
| POST    | `/api/games`                        | `createBotGame(CreateBotGameRequest)`           | `IdResponse`                   |
| GET     | `/api/games/{gameId}`               | `getGameById(String)`                           | `GameResponse`                 |
| GET     | `/api/games/{gameId}/notifications` | `getGameNotificationsById(String)`              | `Collection<GameNotification>` |
| POST    | `/api/games/{gameId}/join`          | `joinGame(String, JoinGameRequest)`             | `IdResponse`                   |
| POST    | `/api/games/{gameId}/answer`        | `answerQuestion(String, AnswerQuestionRequest)` | `IdResponse`                   |

**DTO** : `GameResponse`, `GameRoundResponse`, `GameNotification` (interface polymorphe, pattern §6).

---

## 3. Use cases (ports entrants — `domain/port/in/`)

- `CreateGameUseCase` — création d'une partie
- `JoinGameUseCase` — un joueur rejoint une partie
- `AnswerQuestionUseCase` — réponse à une question d'un round
- `CancelGameUseCase` — annulation d'une partie
- `GetGameUseCase` — récupération par id
- `GetGameEventsUseCase` — lecture des événements (event store)
- `GetGamesByUserUseCase` — parties d'un utilisateur
- `SearchGameUseCase` — recherche paginée

---

## 4. Dépendances inter-services

| Port out                 | Service cible  | Query Axon envoyée (QueryGateway)               |
|--------------------------|----------------|-------------------------------------------------|
| `QuestionRepositoryPort` | `quizup-theme` | `QuestionQuery.GetRandomApprovedQuestionsQuery` |

Implémentation : `infrastructure/out/question/adapter/QuestionRepositoryAdapter` (→ theme, mappé
via `GameQuestionMapper.toGameQuestion`).

**Ports sortants locaux** : `GameRepositoryPort`, `GameEventStorePort`.

---

## 5. Contrats cassés / TODO

- **Placement du port inter-service** : `QuestionRepositoryAdapter` (package
  `infrastructure/out/question/adapter/`) implémente `QuestionRepositoryPort` (→ quizup-theme)
  mais importe des types `io.github.quizup.theme.domain.*` (`Question`, `QuestionQuery`).
  La spec §2.7 exige que l'implémentation d'un port sortant inter-modules soit dans
  `application/service/` et ne retourne que des types **locaux** (`GameQuestion`). → **À corriger** :
  déplacer vers `application/service/QuestionService`.

