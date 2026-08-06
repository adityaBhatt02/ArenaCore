# ArenaCore

**A distributed, event-driven matchmaking and lobby platform** inspired by multiplayer game backends like Valorant, CS2, and League of Legends - built to demonstrate microservices architecture, concurrency handling, and inter-service communication patterns rather than gameplay itself.

ArenaCore is backend infrastructure: authentication, MMR-based matchmaking, lobby management, and event-driven statistics — the same class of engineering problems faced by teams at Riot Games, Valve, and Discord (queueing, presence, caching, asynchronous fan-out, fault tolerance).

---

## Table of Contents

- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Services](#services)
- [Request Flow](#request-flow)
- [Key Design Decisions](#key-design-decisions)
- [Concurrency & Fault Tolerance](#concurrency--fault-tolerance)
- [Getting Started](#getting-started)
- [API Overview](#api-overview)
- [Known Limitations & Future Work](#known-limitations--future-work)

---

## Architecture

```
                              ┌─────────────┐
                              │   Client    │
                              └──────┬──────┘
                                     │ REST
                              ┌──────▼──────┐
                              │ API Gateway │ (Spring Cloud Gateway)
                              │ JWT filter  │  validates token, injects
                              └──────┬──────┘ X-Player-Id / X-Player-Username
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
              │ (players) │       │ (sorted-set │       │ + Redis    │
              └───────────┘       │   queue)    │       └──────┬─────┘
                                  └─────────────┘              │
                                                               │ Kafka
                                                        ┌──────▼──────┐
                                                        │Match History│
                                                        │  Service    │
                                                        │  (Kafka     │
                                                        │  consumer)  │
                                                        └──────┬──────┘
                                                               │
                                                         ┌─────▼─────┐
                                                         │ PostgreSQL│
                                                         └───────────┘
```

Every service is an independently deployable Spring Boot application. The **API Gateway** is the only publicly reachable component — internal services communicate exclusively via gRPC (synchronous) and Kafka (asynchronous), and are never exposed outside the Docker network.

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1.0 |
| API Gateway | Spring Cloud Gateway (WebMVC) |
| Auth | Spring Security, BCrypt, JWT (JJWT) |
| Inter-service (sync) | gRPC + Protocol Buffers |
| Inter-service (async) | Apache Kafka |
| Cache / Queue | Redis (Sorted Sets, Lua scripts) |
| Database | PostgreSQL + Spring Data JPA / Hibernate |
| Fault Tolerance | Resilience4j (Circuit Breaker, Retry, Rate Limiter, Time Limiter) |
| Service Discovery | Netflix Eureka |
| Scheduled Jobs | AWS Lambda + EventBridge |
| Containerization | Docker, Docker Compose |
| Build Tool | Maven |

---

## Services

### API Gateway (`:8080`)
Single public entry point. Routes requests by path prefix and enforces JWT validation on all non-`/auth/**` routes via a custom `OncePerRequestFilter`. On successful validation, it extracts the token's claims and injects `X-Player-Id` / `X-Player-Username` headers onto the forwarded request — downstream services never see or parse the raw JWT.

### Auth Service (`:8081` REST, `:9090` gRPC)
Handles registration and login. Passwords are hashed with BCrypt; on success, a signed JWT (HMAC-SHA) is issued containing the player's ID and MMR-relevant claims. Also exposes a gRPC endpoint (`GetPlayerMmr`) used internally by Matchmaking Service to fetch a player's current, trusted MMR — MMR is never accepted from the client directly, preventing self-reported skill manipulation.

### Matchmaking Service (`:8082`)
Maintains the matchmaking queue as a Redis **Sorted Set**, scored by MMR — giving O(log N) inserts and automatic skill-based ordering with no custom sorting logic. A scheduled job periodically scans the queue and atomically claims groups of similar-MMR players using a Lua script, eliminating race conditions where concurrent scans could assign the same player to two different matches. Matched groups are handed to Lobby Service via gRPC.

### Lobby Service
Receives matched player groups via gRPC, creates a lobby, and tracks per-player ready-check state in Redis for low-latency updates. Once all players are ready, publishes a `MatchStarted` event to Kafka.

### Match History Service
A Kafka consumer that persists completed match records to PostgreSQL, decoupled entirely from the services that produce match events — it can be scaled, restarted, or extended (e.g. with a Notification consumer) without any change to Matchmaking or Lobby.

---

## Request Flow

**1. Authentication**
`Client → Gateway → Auth Service` — register/login, BCrypt-verified, JWT issued.

**2. Joining the queue**
`Client → Gateway (JWT validated, headers injected) → Matchmaking Service`
Matchmaking Service calls Auth Service via gRPC to fetch the player's real MMR, then adds them to the Redis sorted-set queue.

**3. Matching**
A scheduled job in Matchmaking Service scans the queue, atomically claims a group of similar-MMR players via a Lua script, and calls Lobby Service via gRPC to create a lobby.

**4. Ready-check & match start**
Lobby Service tracks readiness in Redis. Once complete, it publishes a `MatchStarted` event to Kafka.

**5. Persistence**
Match History Service consumes the event independently and writes the permanent record to PostgreSQL.

---

## Key Design Decisions

**Why Redis for the queue, not PostgreSQL?**
The queue is high-churn, ephemeral data — players join and leave constantly, and matching requires frequent, fast reads across the whole set. Redis Sorted Sets provide this natively; PostgreSQL would introduce unnecessary write/lock contention for data that doesn't need to survive a restart.

**Why gRPC for Matchmaking ↔ Auth/Lobby, but REST at the Gateway?**
Internal calls are frequent, low-latency, and defined by a strict, versioned contract (`.proto` files) — gRPC's binary encoding and HTTP/2 transport suit this. The Gateway's external-facing API uses REST/JSON because it's the universal, human-readable format external and browser clients expect.

**Why does the Gateway inject headers instead of forwarding the JWT?**
Only the Gateway holds and verifies the signing secret. Downstream services trust `X-Player-Id` purely because they are not independently network-reachable — this keeps the JWT secret confined to a single service and removes JWT-parsing responsibility from every other component.

**Why is MMR fetched via gRPC instead of trusted from the client?**
Accepting a client-supplied MMR would let any player claim any skill rating, defeating the purpose of skill-based matchmaking. MMR is always read fresh from Auth Service's PostgreSQL store — the single source of truth.

---

## Concurrency & Fault Tolerance

- **Atomic queue claims** — a Lua script performs "read + remove" as a single uninterruptible Redis operation, preventing two concurrent matchmaking workers from claiming the same player for different matches.
- **Resilience4j** is applied to the Matchmaking → Lobby and Matchmaking → Auth gRPC calls:
  - **Circuit Breaker** — stops repeated calls to a failing downstream service and fails fast.
  - **Retry** — automatically retries transient failures.
  - **Time Limiter** — bounds how long a call may hang before being treated as failed.
  - **Rate Limiter** — protects the queue-join endpoint from abusive request bursts.
- **Kafka** decouples match-completion from its consumers — if Match History Service is temporarily down, events are retained and processed once it recovers, with no data loss and no blocking of Lobby Service.

---

## Getting Started

### Prerequisites
- Java 25
- Maven
- Docker & Docker Compose

### Run the infrastructure
```bash
docker-compose up -d
```
Starts PostgreSQL, Redis, Kafka, and Zookeeper.

### Run each service
```bash
cd auth-service          && ./mvnw spring-boot:run
cd matchmaking-service   && ./mvnw spring-boot:run
cd lobby-service         && ./mvnw spring-boot:run
cd match-history-service && ./mvnw spring-boot:run
cd api-gateway           && ./mvnw spring-boot:run
```

All traffic should be directed at the Gateway: `http://localhost:8080`.

---

## API Overview

| Method | Path | Description | Auth Required |
|---|---|---|---|
| POST | `/auth/register` | Create a new player account | No |
| POST | `/auth/login` | Authenticate and receive a JWT | No |
| POST | `/queue/join` | Join the matchmaking queue | Yes |
| GET | `/queue/view` | View current queue state | Yes |
| GET | `/queue/size` | Get current queue length | Yes |

---

## Known Limitations & Future Work

- Service addresses are currently hardcoded (`localhost:PORT`); Eureka integration is planned to enable dynamic service discovery and remove this coupling.
- `ddl-auto=update` is used for local development; a real deployment would use versioned migrations (Flyway/Liquibase).
- No refresh-token flow yet — tokens simply expire after a fixed TTL.
- Lobby and Match History services are still being built out; core matchmaking (queueing, MMR lookup, atomic claim) is implemented and tested end-to-end.
- A scheduled AWS Lambda (via EventBridge) to purge stale/abandoned queue entries is planned but not yet implemented.
- *WORK IN PROGRESS*
---

## Why This Project

Most portfolio backend projects are e-commerce or CRUD clones. ArenaCore was deliberately built around a domain — multiplayer matchmaking — that *requires* solving real distributed-systems problems: race conditions under concurrent load, service-to-service authentication, event-driven decoupling, and graceful degradation when a dependency fails. The goal was not to build a game, but to build the kind of backend infrastructure a game would run on.
