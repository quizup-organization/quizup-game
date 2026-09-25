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

## 2. Surface (headless)

Service **headless** : aucun contrôleur REST ni WebSocket. La surface applicative unique est le
**`quizup-bff`** (`/api/**` + `/ws`) ; il interroge ce service via le **query bus** Axon et consomme
ses événements. Les handlers de requête/commande, sagas et projections restent la seule surface
exposée par le service.
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

