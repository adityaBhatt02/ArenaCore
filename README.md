# ArenaCore

**A microservices-based matchmaking and lobby backend** inspired by multiplayer game infrastructure like Valorant, CS2, and League of Legends — built to demonstrate service-to-service communication, event-driven design, and relational data modeling, not gameplay itself.

ArenaCore is backend infrastructure only: authentication, MMR-based matchmaking, lobby/ready-check management, and event-driven match history — the kind of plumbing that sits behind a real game client, not a playable game.

---

## Table of Contents

- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Services](#services)
- [Request Flow](#request-flow)
- [Key Design Decisions](#key-design-decisions)
- [Getting Started](#getting-started)
- [API Overview](#api-overview)
- [Known Limitations & Roadmap](#known-limitations--roadmap)

---

## Architecture

```
                              ┌─────────────┐
                              │   Client    │
                              └──────┬──────┘
                                     │ REST
                              ┌──────▼──────┐
                              │ API Gateway │ (Spring Cloud Gateway, WebMVC)
                              │ JWT filter  │  validates token, injects
                              └──────┬──────┘  X-Player-Id header
                    ┌────────────────┼─────────────────────────┐
                    │                │                         │
             ┌──────▼─────┐       ┌──────▼──────┐       ┌──────▼──────┐
             │    Auth    │       │ Matchmaking │       │    Lobby    │
             │   Service  │◄──────┤   Service   ├──────►│   Service   │
             │ (REST+gRPC)│  gRPC │  (Redis Q)  │  gRPC │ (Ready-chk) │
             └──────┬─────┘       └──────┬──────┘       └──────┬──────┘
                    │                    │                     │
              ┌─────▼─────┐       ┌──────▼──────┐       ┌──────▼─────┐
              │ PostgreSQL│       │    Redis    │       │ PostgreSQL │
              │ (players) │       │  (queue)    │       │  (lobbies, │
              └───────────┘       └─────────────┘       │lobby_players)
                                                          └──────┬─────┘
                                                                 │ Kafka
                                                          (match-started
                                                            topic)
```

Every service is an independently runnable Spring Boot application. The **API Gateway** is the only externally reachable component — internal services communicate via gRPC (synchronous, for calls that need an immediate response) and Kafka (asynchronous, fire-and-forget events).

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1.0 |
| API Gateway | Spring Cloud Gateway (WebMVC) |
| Auth | Spring Security, BCrypt, JWT |
| Inter-service (sync) | gRPC + Protocol Buffers |
| Inter-service (async) | Apache Kafka (Zookeeper-based) |
| Cache / Queue | Redis |
| Database | PostgreSQL + Spring Data JPA / Hibernate |
| Containerization | Docker, Docker Compose |
| Build Tool | Maven |

---

## Services

### API Gateway (`:8080`)
Single public entry point. Routes requests by path prefix (`/auth/**`, `/queue/**`, `/lobby/**`) and validates JWTs on protected routes. On success, it injects an `X-Player-Id` header onto the forwarded request — downstream services trust this header and never parse the JWT themselves.

### Auth Service (`:8081` REST, gRPC)
Handles registration and login. Passwords are hashed with BCrypt; on success, a signed JWT is issued. Also exposes a gRPC endpoint used internally by Matchmaking Service to fetch a player's current MMR — MMR is read from Auth Service's own database, never trusted from the client.

### Matchmaking Service (`:8082`)
Maintains the matchmaking queue in Redis. A `@Scheduled` job runs every 5 seconds, attempts to claim 10 players within an MMR range, balances them into two teams, and calls Lobby Service via gRPC to create the lobby.

### Lobby Service (`:8083` REST, gRPC `:9091`)
Receives matched player groups via gRPC (`CreateLobby`), persists a `Lobby` with its `LobbyPlayer` rows in PostgreSQL (via JPA cascade), and exposes REST endpoints for the ready-check flow (`POST /lobby/{id}/ready`, `GET /lobby/{id}`). Once every player in a lobby is ready, the lobby's status flips from `WAITING_FOR_READY` to `IN_PROGRESS` and a `MatchStartedEvent` is published to the `match-started` Kafka topic.

---

## Request Flow

**1. Authentication**
`Client → Gateway → Auth Service` — register/login, BCrypt-verified, JWT issued.

**2. Joining the queue**
`Client → Gateway (JWT validated, X-Player-Id injected) → Matchmaking Service`
Matchmaking Service calls Auth Service via gRPC to fetch the player's MMR, then adds them to the Redis queue.

**3. Matching**
A scheduled job in Matchmaking Service claims a group of similarly-rated players from the queue, balances two teams, and calls Lobby Service via gRPC to create a lobby (persisted across `lobbies` and `lobby_players` tables via a single cascaded save).

**4. Ready-check & match start**
Each player calls `POST /lobby/{lobbyId}/ready` through the Gateway. Once all players in the lobby are ready, Lobby Service flips the lobby's status to `IN_PROGRESS` and publishes a `MatchStartedEvent` (lobby ID, both team rosters, timestamp) to Kafka.

---

## Key Design Decisions

**Why Redis for the queue, not PostgreSQL?**
The queue is high-churn, ephemeral data — players join and leave constantly, and the matching job needs fast, frequent reads. Redis suits this access pattern natively; using a relational database here would add unnecessary transactional overhead for data with no long-term value.

**Why gRPC for Matchmaking ↔ Auth/Lobby, but REST at the Gateway?**
Internal calls are frequent and defined by a strict, versioned `.proto` contract — both services get compiler-checked, auto-generated types from the same schema, and gRPC's binary encoding over HTTP/2 keeps these calls fast. The Gateway's external API uses REST/JSON because it's the public-facing boundary, and REST/JSON is what a client (or Postman, for testing) expects.

**Why does the Gateway inject a header instead of forwarding the raw JWT?**
Only the Gateway holds and verifies the signing secret. Downstream services trust `X-Player-Id` because they're not independently reachable from outside the Docker network — this keeps JWT verification logic in exactly one place.

**Why is MMR fetched via gRPC instead of trusted from the client?**
Accepting a client-supplied MMR would let any player claim any skill rating. MMR is always read fresh from Auth Service's own database — the single source of truth for that data, owned by exactly one service.

**Why `EnumType.STRING` for status enums, not the JPA default?**
The default (`ORDINAL`) stores an enum as its declaration-order integer, which silently corrupts existing data if the enum is ever reordered or a value is inserted in the middle. Storing the name as text avoids this fragility entirely.

**Why is `LobbyPlayer.playerId` a plain `Long`, not a JPA relationship to a `Player` entity?**
`Player` lives in Auth Service's own database — a genuine foreign key across two services' databases isn't possible, and enforcing one would violate each service owning its own data. `playerId` is a logical reference, populated from the gRPC payload, validated at the application level.

**Why Kafka for `MatchStarted`, instead of a direct call to whatever consumes it?**
A direct (e.g. gRPC) call would mean Lobby Service has to know about and wait on every consumer of "a match started" — and a slow or down consumer would risk blocking the ready-check flow itself. Publishing to Kafka lets Lobby Service fire the event and move on immediately; any number of independent consumers (a future Match History Service, notifications, etc.) can read it whenever they're ready, with no risk to the core flow if they're temporarily unavailable.

---

## Getting Started

### Prerequisites
- Java 25
- Maven (or the bundled `mvnw` wrapper)
- Docker & Docker Compose

### Run the infrastructure
```bash
docker-compose up -d
```
Starts PostgreSQL, Redis, Zookeeper, and Kafka.

### Run each service
```bash
cd auth-service        && ./mvnw spring-boot:run
cd matchmaking-service  && ./mvnw spring-boot:run
cd lobby-service        && ./mvnw spring-boot:run
cd api-gateway           && ./mvnw spring-boot:run
```

All client traffic goes through the Gateway: `http://localhost:8080`.

### Seeding a test match
A PowerShell script (`seed_players.ps1`) registers 10 test players with varied MMR, joins them to the queue, polls for the lobby the scheduler creates, and marks all 10 ready — useful for exercising the full flow without doing it manually in Postman.

---

## API Overview

| Method | Path | Description | Auth Required |
|---|---|---|---|
| POST | `/auth/register` | Create a new player account | No |
| POST | `/auth/login` | Authenticate and receive a JWT | No |
| POST | `/queue/join` | Join the matchmaking queue | Yes |
| GET | `/queue/view` | View current queue state | Yes |
| GET | `/queue/size` | Get current queue length | Yes |
| POST | `/lobby/{lobbyId}/ready` | Mark the calling player as ready | Yes |
| GET | `/lobby/{lobbyId}` | Get current lobby status and player ready states | Yes |

---

## Known Limitations & Roadmap

- **Match History Service** — not yet built. Planned as a Kafka consumer of `match-started` (and a future `match-completed` event) to build a permanent, queryable match record in its own PostgreSQL database, fully decoupled from Lobby Service.
- **Match completion** — Lobby Service currently only models `WAITING_FOR_READY` and `IN_PROGRESS`; there's no endpoint yet to mark a match `COMPLETED` with an outcome (winner, per-player stats). This requires a source of truth for match results, which doesn't exist since ArenaCore doesn't host actual gameplay.
- **`CANCELLED` lobbies** — the status exists in the enum but nothing currently sets it. A scheduled cleanup (sweeping lobbies stuck in `WAITING_FOR_READY` past some age) is planned.
- **Stale queue entries** — a scheduled job (planned as an AWS Lambda on an EventBridge schedule) to purge abandoned Redis queue entries is designed but not yet implemented.
- **Service discovery** — service addresses are currently hardcoded (`localhost:PORT`); this works for local development but wouldn't survive multi-host deployment. Eureka (or similar) is a natural next addition, not yet integrated.
- **Fault tolerance** — the gRPC calls between services (Matchmaking → Auth, Matchmaking → Lobby) have no circuit breaker, retry, or timeout policy yet. A downstream failure currently surfaces as a bare exception rather than degrading gracefully. Resilience4j is a planned addition here.
- **Schema migrations** — `ddl-auto=update` is used for local development convenience; a real deployment would use versioned migrations (Flyway/Liquibase) instead.
- **No refresh-token flow** — JWTs simply expire after a fixed TTL.

---

## Why This Project

Most portfolio backend projects are e-commerce or CRUD clones. ArenaCore was built around a domain — multiplayer matchmaking — that requires engaging with real distributed-systems concerns: service-to-service auth, data ownership boundaries in a microservices architecture, and decoupling via events rather than direct calls. The goal isn't to build a game — it's to build the backend a game would run on top of, and to be able to explain, precisely, why each piece is built the way it is.
