# Content Organizer

A Spring Boot REST API for organizing and managing content such as articles, videos, courses, and conference talks. Each piece of content supports multiple sources, enabling well-documented and reference-rich entries.

Based on the project by [Dan Vega](https://www.danvega.dev/).

## Tech Stack

- Java 21
- Spring Boot 4.1.0
- Spring Data JPA + PostgreSQL
- Spring Boot Starter Validation
- Spring Security (JWT) — role-based authorization (`MEMBER`, `WRITER`, `ADMIN`)
- Jackson 3 (`tools.jackson`) — type-safe JSON serialization
- JUnit 5 + MockMvc + Spring Security Test — clean, isolated integration tests

## Getting Started

### Prerequisites

- Java 21+
- PostgreSQL running locally on port `5432`
- A database named `postgres` with user `admin`

### Run

The API signs JWTs with a key read from the `JWT_SECRET_KEY` environment variable
(`app.jwt.secret-key=${JWT_SECRET_KEY}` in `application.properties`).

1. Generate a key:
   ```bash
   openssl rand -base64 64
   ```
2. Export it and start:
   ```bash
   export JWT_SECRET_KEY='<paste-the-generated-key>'
   ./mvnw spring-boot:run
   ```

The API starts at `http://localhost:8080`.

### Run with Docker

The project ships with a `Dockerfile` (multi-stage build: Maven compiles, a slim JRE image runs) and a `docker-compose.yml` that runs the API together with its PostgreSQL database.

#### Prerequisites

- A running Docker engine — e.g. [colima](https://github.com/abiosoft/colima) (`colima start`) or Docker Desktop
- The Docker Compose plugin (`docker compose`)

#### Option A — provide the JWT secret via a `.env` file (recommended)

The `app` service reads `JWT_SECRET_KEY` from a `.env` file at the project root. This file is gitignored: create it once, never commit it.

```bash
printf 'JWT_SECRET_KEY=%s\n' "$(openssl rand -base64 32 | tr -d '\n')" > .env
```

If no `.env` is present, the application fails to start with a "could not resolve placeholder" error — the secret is intentionally required (fail fast), since an unguessable signing key is what protects the JWTs.

#### Option B — remove the `POSTGRES_HOST_AUTH_METHOD: trust` line

`docker-compose.yml` uses `POSTGRES_HOST_AUTH_METHOD: trust` so PostgreSQL accepts the empty password from `application.properties`. This is fine for local development, but it is **not** a production setup.

To use a real database password instead, the following conditions must be met:

1. In `docker-compose.yml`, set a non-empty `POSTGRES_PASSWORD` and delete the `POSTGRES_HOST_AUTH_METHOD: trust` line
2. Set the matching `SPRING_DATASOURCE_PASSWORD` on the `app` service
3. In `.github/workflows/ci.yml`, the `POSTGRES_HOST_AUTH_METHOD: trust` line exists so the CI tests can connect without a password — adjust it to match your CI database settings

#### Start

```bash
docker compose up --build
```

The API is available at `http://localhost:8080`; PostgreSQL listens on port `5432`. Stop everything with `docker compose down`.

## Content Model

| Field        | Type                                          | Description                                     |
|--------------|-----------------------------------------------|-------------------------------------------------|
| id           | Integer                                       | Auto-generated primary key                      |
| title        | String                                        | Required, cannot be blank                       |
| description  | String                                        | Optional                                        |
| status       | `IDEA`, `IN_PROGRESS`, `COMPLETED`, `PUBLISHED` | Current stage of the content               |
| type         | `ARTICLE`, `VIDEO`, `COURSE`, `CONFERENCE_TALK` | The format of the content                   |
| sources      | List\<String\>                                | Multiple reference URLs supporting the content (persisted in `content_sources` table) |
| authors      | List\<String\>                                | Usernames authorized to update the content (persisted in `content_authors` table) |
| dateCreated  | LocalDateTime                                 | Set automatically via `@PrePersist`             |
| dateUpdated  | LocalDateTime                                 | Set automatically on update                     |

## API Endpoints

| Method   | Endpoint                        | Description                    |
|----------|---------------------------------|--------------------------------|
| `GET`    | `/api/contents`                 | List all content               |
| `GET`    | `/api/contents/{id}`            | Get content by ID              |
| `POST`   | `/api/contents`                 | Create new content             |
| `PUT`    | `/api/contents/{id}`            | Update existing content        |
| `DELETE` | `/api/contents/{id}`            | Delete content by ID           |
| `GET`    | `/api/contents/filter/{keyword}`| Search content by title        |
| `GET`    | `/api/contents/filter/status/{status}` | Filter content by status |

### Create Content

```json
POST /api/contents
{
  "title": "Spring Security Guide",
  "description": "A comprehensive guide to securing Spring apps",
  "status": "PUBLISHED",
  "type": "ARTICLE",
  "sources": [
    "https://example.com/part-1",
    "https://example.com/part-2"
  ]
}
```

## Security & Admin bootstrap

The application creates no default account at startup.

For **testing purposes only**, promote a registered user to `ADMIN`:

```bash
# 1. Register a user
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"your-name","password":"your-password"}'

# 2. Promote the user
psql -d postgres -U admin \
  -c "UPDATE users SET role = 'ADMIN' WHERE username = 'your-name';"
```

In production, create the first admin through a one-time SQL migration executed by
an operator. Do not create accounts from application code.

## Testing

`ContentControllerTest` is a JUnit 5 + MockMvc test class covering the content API authorization matrix.

| Test | Role | HTTP semantics asserted |
|------|------|------------------------|
| `shouldRejectUnauthenticatedUsers` | anonymous | `GET /api/contents` → 401 |
| `usersCanGetContents` | MEMBER | `GET /api/contents` → 200 |
| `guestsCannotCreateContents` | anonymous | `POST /api/contents` → 401 |
| `membersCannotCreateContent` | MEMBER | `POST /api/contents` → 403 |
| `membersCannotDeleteContent` | MEMBER | `DELETE /api/contents/{id}` → 403 |
| `adminCanCreateContent` | ADMIN | `POST /api/contents` → 201 |
| `adminCanDeleteContent` | ADMIN | `DELETE /api/contents/{id}` → 204 |

Key practices:

- **AAA pattern** on `adminCanDeleteContent`: full Arrange-Act-Assert, building its own database fixture via `ContentRepository` before acting — no dependency on pre-existing data
- **Transactional isolation** (`@Transactional`): each test runs inside a rolled-back transaction, keeping the database clean between runs
- **Type-safe JSON** (Jackson 3 `tools.jackson` `ObjectMapper`): payloads serialized from a typed `ContentRequest` DTO instead of hand-written JSON strings
- **Role-based matrix** (`@WithMockUser`): exact HTTP semantics 401 / 403 / 201 / 204 asserted against the real `SecurityFilterChain` rules

Run the suite:

```bash
./mvnw test
```

## Scripts

`script.sh` exercises the API against a running instance: register, login, content
CRUD, filters, and error cases. `register` creates users with the default `MEMBER`
role, so promote `admin1` first (see Security & Admin bootstrap) before the admin
steps, which otherwise return 403.

```bash
./script.sh
```

## Migration v1 → v2

After pulling this version, run the following statements **once** via `psql` :

```sql
ALTER TABLE Content DROP COLUMN IF EXISTS sources;
DROP TABLE IF EXISTS content_sources;
DROP TABLE IF EXISTS content_authors;
```

The `schema.sql` file will recreate the `content_sources` and `content_authors` tables on the next application startup.

## License

This project is based on work by Dan Vega. All rights to the original base project belong to him.
