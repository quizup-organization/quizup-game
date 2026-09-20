# AGENTS.md — quizup-game

> Service de **game** : parties (duels) de quiz, rounds, scoring. **Référence implémentation des
> patterns avancés** : sous-agrégats, sagas, deadlines, event store. Architecture : Axon
> Framework (CQRS/EDA) + JPA (projections).
> Pour les règles de patterns : [
`../../best-practices/.backend/hexagonal-architecture.md`](../../best-practices/.backend/hexagonal-architecture.md).

---

## 1. Rôle

Gestion des **parties** de quiz : création (bot), participation (join), réponse aux questions,
scoring, annulation. Les questions proviennent de `quizup-theme`. Le bot est un utilisateur
spécial (`QuizUpConstants.SYSTEM_USER_ID`).

**Package** : `io.github.quizup.game`

---

## 2. Endpoints REST

### `GameController` — `/api/games`

| Méthode | Chemin                              | Handler                                         | Response                       |
|---------|-------------------------------------|-------------------------------------------------|--------------------------------|
| POST    | `/api/games/search`                 | `search(SearchRequest)`                         | `PageResponse<GameResponse>`   |
| POST    | `/api/games`                        | `createBotGame(CreateBotGameRequest)`           | `IdResponse`                   |
| POST    | `/api/games/async`                  | `createAsyncGame(CreateAsyncGameRequest)`       | `IdResponse`                   |
| GET     | `/api/games/{gameId}`               | `getGameById(String)`                           | `GameResponse`                 |
| GET     | `/api/games/{gameId}/notifications` | `getGameNotificationsById(String)`              | `Collection<NotificationEnvelope<GameNotification>>` |
| POST    | `/api/games/{gameId}/join`          | `joinGame(String, JoinGameRequest)`             | `IdResponse`                   |
| POST    | `/api/games/{gameId}/answer`        | `answerQuestion(String, AnswerQuestionRequest)` | `IdResponse`                   |
| POST    | `/api/games/{gameId}/cancel`        | `cancelGame(String)`                            | `IdResponse`                   |
| POST    | `/api/games/{gameId}/abandon`       | `abandonGame(String)`                           | `IdResponse`                   |

**Orchestration** : `GameFlowSaga` (remplace `BotGameSaga`) conduit toutes les parties SYNC — join
auto des deux joueurs, start, rounds, réponse du bot, clôture, fin. Le service est **seule source
de vérité du temps** : chaque événement porte l'instant absolu de la phase suivante (`firstRoundAt`,
`revealAt`, `answerDeadlineAt`, `nextRoundAt`) et la saga dérive ses deadlines de ces instants (plus
aucune durée d'animation client codée en dur côté serveur).

**Durées de transition** (`GameRules`, alignées sur la maquette) : `MATCH_INTRO_MS = 6050`
(VS + transition + intro de tour avant le round 1) et `ROUND_TRANSITION_MS = 4400` (révélation du
round clos + intro du round suivant). Le client scinde la transition en révélation puis
`RoundIntro` (1900 ms) sur la base de `closedAt` / `nextRoundAt`.

**Cycle de round en deux phases** (`GameRoundStatus`) :
- `CREATED` → `QUESTION_SHOWN` (`RoundStartedEvent`) : la question est affichée, réponses en
  animation, **aucune réponse acceptée, chrono non armé** ;
- `QUESTION_SHOWN` → `ANSWERABLE` (`QuestionRevealedEvent`, émis par la saga après la deadline
  `QUESTION_REVEAL` dont la durée vaut `GameRules.QUESTION_REVEAL_MS`) : les réponses sont révélées,
  le chrono démarre jusqu'à `answerDeadlineAt` ; seule phase où `AnswerQuestionCommand` est acceptée
  (sinon `RoundNotRevealedProblem`) ;
- `ANSWERABLE` → `CLOSED` (`RoundClosedEvent`) : les deux ont répondu ou expiration (`ROUND_EXPIRED`).

Le **temps de réponse** (`timeMs`, `player*TimeMs`, bonus de vitesse, badge Éclair) est mesuré depuis
`revealedAt`, jamais depuis l'affichage de la question. Le bot répond à un délai aléatoire borné par
`BotDifficulty` (`min/maxAnswerDelayMillis`), après la révélation. Scoring max **160** (`ROUND_7`
double le bonus de vitesse). `GameProjection` cumule les scores en direct à chaque
`QuestionAnsweredEvent`. `GameEndedEvent` porte les bonnes réponses et réponses < 3 s par joueur
(badges profil). `ROUND_CLOSED` porte `correctAnswer` ; `PLAYER_ANSWERED` et `GameRoundResponse`
portent `timeMs`/`player*TimeMs`. `GameRoundResponse` expose aussi `revealedAt`/`answerDeadlineAt`
(le client repli polling peut armer son chrono sans WebSocket).

**Réponse & abandon** : `QuestionAnsweredEvent` porte `questionId` et `playerType`
(`HUMAN`/`BOT`/`GHOST`) — utilisés par `quizup-theme` pour la difficulté (seules les réponses
humaines comptent). `POST /{gameId}/abandon` déclenche `EndGameCommand(forfeitById)` : l'abandon
donne la victoire à l'adversaire, pour tout type d'adversaire (humain, bot, fantôme) comme pour un
run async solo (sans vainqueur). Gardes d'agrégat : statut `IN_PROGRESS` et `forfeitById` participant
de la partie (sinon `GameNotInProgressProblem`/`PlayerNotInGameProblem`).

### Duel asynchrone (`POST /api/games/async`)

Le fantôme n'est pas un bot : c'est le **run enregistré** d'un joueur absent. Modèle à **deux
parties** partageant les mêmes questions (le replay réutilise les questions du run via l'event
store) :
- **record** (sans `ghostGameId`, player2 absent) : run solo ; le round se clôt dès la réponse du
  joueur (ou expiration) ; à la fin `GameRunRecordedEvent` et statut `AWAITING_OPPONENT` —
  **aucun XP** à ce stade.
- **replay** (avec `ghostGameId`, `player2Type = GHOST`) : les réponses du fantôme sont rejouées
  aux timings enregistrés (`GHOST_ANSWERS`, délai = `timeMs` du run) ; la fin émet le
  `GameEndedEvent` **autoritaire** (XP attribuée aux deux joueurs une seule fois).
`GameFlowSaga` orchestre les trois saveurs (SYNC, ASYNC record, ASYNC replay).

**DTO** : `GameResponse`, `GameRoundResponse`, `GameNotification` (interface polymorphe, pattern §6).
Les notifications (historique `GET /{gameId}/notifications` **et** push `/topic/games/{gameId}`) sont
enveloppées dans `NotificationEnvelope<GameNotification>` (SDK) : `notificationId`, `aggregateId`,
`sequenceNumber`, `occurredAt`, `payload`. Le client fold l'état à partir de `payload` et déduplique
par `sequenceNumber`. `GameCreatedNotification` porte les noms/mode/type d'adversaire/difficulté ;
`RoundStartedNotification` porte `questionId`, `questionText`, `imageUrl` (illustration optionnelle)
et `difficulty` (déduite côté theme, `null` si inconnue).
`GameRoundResponse` expose `questionId` (issu de la projection) : le client charge la question
complète — dont les libellés A/B/C/D — via `quizup-theme` (`GET /api/questions/{questionId}`).

---

## 3. Use cases (ports entrants — `domain/port/in/`)

- `CreateGameUseCase` — création d'une partie
- `JoinGameUseCase` — un joueur rejoint une partie
- `AnswerQuestionUseCase` — réponse à une question d'un round
- `CancelGameUseCase` — annulation d'une partie
- `AbandonGameUseCase` — abandon/forfait d'une partie en cours (`EndGameCommand(forfeitById)`)
- `GetGameUseCase` — récupération par id
- `GetGameEventsUseCase` — lecture des événements (event store)
- `SearchGameUseCase` — recherche paginée (`POST /search` ; parties d'un joueur = filtre `player1Id`/`player2Id`)

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

