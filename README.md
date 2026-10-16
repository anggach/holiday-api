# Holidays API

Production-style Spring Boot API that answers the three Nager.Date holiday use cases, with caching, resilience, API-key auth, rate limiting, observability, Docker packaging, and an optional multilingual Ollama tool-calling agent.

Upstream data: [Nager.Date API](https://date.nager.at/Api)

## Requirements

- Java 17+
- Gradle Wrapper (`./gradlew`) — no system Gradle required
- Optional: [Ollama](https://ollama.com) with model `llama3.1` for `/api/v1/chat`

## Quick start

```bash
cd {project_dir}
./gradlew bootRun
```

Default API key: `dev-api-key` (override with `APP_API_KEY`).

Swagger UI: http://localhost:8080/swagger-ui.html  
Health: http://localhost:8080/actuator/health  
Ollama component: http://localhost:8080/actuator/health/ollamaAi  

## API examples

All `/api/v1/**` routes require header `X-API-Key`.

### 1) Last celebrated holidays

```bash
curl -s -H "X-API-Key: dev-api-key" \
  "http://localhost:8080/api/v1/holidays/NL/recent?limit=3" | jq
```

Returns public holidays with `date <= today` (spills into the previous year if needed).

### 2) Weekday public holiday counts

```bash
curl -s -H "X-API-Key: dev-api-key" \
  "http://localhost:8080/api/v1/holidays/weekday-counts?year=2026&countries=NL,DE,FR" | jq
```

Counts holidays whose Nager `types` include `Public` and fall on Mon–Fri. Sorted descending by count.

### 3) Shared holiday dates

```bash
curl -s -H "X-API-Key: dev-api-key" \
  "http://localhost:8080/api/v1/holidays/shared?year=2026&countryA=NL&countryB=DE" | jq
```

Intersection by date using a `HashSet` — **O(Hₐ + Hᵦ)**.

### 4) Natural language chat (optional AI)

```bash
# Setup Ollama first
ollama serve
ollama pull llama3.1
curl -s http://localhost:8080/actuator/health/ollamaAi | jq

curl -s -H "X-API-Key: dev-api-key" -H "Content-Type: application/json" \
  -d '{"message":"3 libur terakhir di Belanda"}' \
  http://localhost:8080/api/v1/chat | jq
```

If Ollama is down or the model is missing, chat returns **503** with setup steps. The three REST endpoints keep working.

The agent replies in the **same language as the user**. It only calls tools for the three capabilities above; out-of-scope questions are refused without forcing a tool call.

Chat latency is dominated by two local generations (tool choice, then the reply). The app pins a 4k context, caps each generation at 384 tokens, keeps the model loaded for 30 minutes, and warms it up in the background after startup.

## Complexity notes

| Use case | Approach | Complexity |
|----------|----------|------------|
| Recent holidays | Filter + sort desc + limit | O(H log H) |
| Weekday counts | Parallel fetch per country, filter weekdays, sort countries | O(N·H + N log N) wall-clock ≈ max latency with parallelism |
| Shared dates | Index country A dates in HashSet, scan B | O(Hₐ + Hᵦ) |

`H` (holidays/country/year) is typically &lt; 30.

## Caching & resilience

- Caffeine cache keys: `holidays:{year}:{country}` and available countries, TTL 24h
- RestClient connect/read timeouts
- Spring Retry on idempotent upstream GETs
- Upstream failures mapped to RFC 7807 Problem Details (`502`)
- Request ID via `X-Request-Id` / MDC
- Micrometer metrics + Prometheus scrape at `/actuator/prometheus` (dev profile)
- Access log per API request: method, path, status, duration, `X-Request-Id`
- Rate limit per API key (Bucket4j), default 60 req/min

## Docker

```bash
docker compose up --build
```

Grafana: http://localhost:3000 (anonymous admin). The Holidays API dashboard is provisioned on startup. Prometheus scrapes `/actuator/prometheus` on the Docker network.

The dashboard splits chat latency into Ollama generation, the full chat client call, and tool execution. A point on those panels links to the Tempo trace. The **Chat traces** link opens the waterfall for one request: HTTP, chat client, each model pass, then the tool. Tracing is on inside Compose (`TRACING_ENABLED=true`). A local `./gradlew bootRun` leaves it off unless `TRACING_ENABLED=true` and Tempo is listening on `localhost:4318`.

Ollama is expected on the host (`host.docker.internal:11434` from the container). Override `OLLAMA_BASE_URL` if needed.

## Profiles

`SPRING_PROFILES_ACTIVE` selects the environment. `.env` is loaded by Docker Compose (`env_file`). `./gradlew bootRun` does not read `.env`; export the variable or pass the profile:

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

| | dev | prod |
|---|---|---|
| Error body | includes the exception message | `Unexpected error` (stack trace stays in the server log) |
| Health details | always | hidden |
| Swagger | on | off |
| Metrics / Prometheus | exposed | health only |
| API key | `APP_API_KEY` or `dev-api-key` | `APP_API_KEY` required |

## Tests

```bash
./gradlew test
```

## Configuration

See [`.env.example`](.env.example) and `src/main/resources/application.yml`.

| Variable | Default | Meaning |
|----------|---------|---------|
| `SPRING_PROFILES_ACTIVE` | unset (closed error details) | `dev` or `prod` |
| `APP_API_KEY` | `dev-api-key` | API key for `/api/v1/**` |
| `NAGER_BASE_URL` | `https://date.nager.at/api/v3` | Upstream base URL |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Local LLM |
| `OLLAMA_MODEL` | `llama3.1` | Expected chat model |
| `OLLAMA_NUM_CTX` | `4096` | Context window. `llama3.1` otherwise allocates 128k |
| `OLLAMA_NUM_PREDICT` | `384` | Max tokens generated per model pass |
| `OLLAMA_KEEP_ALIVE` | `30m` | How long Ollama keeps the model loaded |
| `OLLAMA_WARMUP` | `true` | Background load after startup so the first chat skips cold start |

## Project layout

```
api/       REST, auth, rate limit, Problem Details
domain/    HolidayService, filters, country normalize/emoji
client/    Nager.Date RestClient
ai/        Tools, ChatService, Ollama health gate
config/    Cache, OpenAPI, RestClient, properties
```

REST and the agent share the same `HolidayService` so numbers stay deterministic.
