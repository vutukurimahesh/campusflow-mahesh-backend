# CampusFlow Backend

Spring Boot 3.3 · Java 21 · PostgreSQL · JWT · Flyway. REST API under `/api/v1`.

## Environment variables

| Variable | Required | Meaning |
|---|---|---|
| `DB_URL` | yes | JDBC URL, `jdbc:postgresql://<host>:5432/<database>` |
| `DB_USERNAME` | yes | Database user |
| `DB_PASSWORD` | yes | Database password |
| `JWT_SECRET` | **yes** (no default) | Random string, 32+ characters. App refuses to start without it |
| `FRONTEND_ORIGIN` | yes | Exact frontend origin, e.g. `https://my-app.onrender.com` (no trailing slash). Comma-separate for several |
| `PORT` | set by Render | Defaults to 8080 locally. Server binds to `0.0.0.0` |
| `JWT_EXPIRATION_MINUTES` | no | Default 720 |
| `APP_TIMEZONE` | no | Default `UTC`; used for "today", overdue and daily charts (e.g. `Asia/Kolkata`) |

Database tables are created by Flyway (`src/main/resources/db/migration`) on startup. Existing data is never dropped.

## Run locally

```bash
cp .env.example .env            # edit DB_PASSWORD and JWT_SECRET
docker compose up -d            # local PostgreSQL (or use your own and edit DB_URL)
set -a; source .env; set +a     # load variables (bash/zsh)
mvn spring-boot:run
```

Check: `curl http://localhost:8080/api/v1/health`

PowerShell: load values with `Get-Content .env | % { if ($_ -match '^\s*([^#=]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim()) } }` and then run `mvn spring-boot:run`.

Run tests: `mvn test` (the included test needs no database).

## Quick API smoke test

```bash
B=http://localhost:8080/api/v1
TOKEN=$(curl -s -X POST $B/auth/register -H 'Content-Type: application/json' \
  -d '{"name":"Test User","email":"test@example.com","password":"password123"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')
curl -s -X POST $B/projects -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Demo","status":"ACTIVE"}'
curl -s $B/dashboard -H "Authorization: Bearer $TOKEN"
```

## Endpoints (all need `Authorization: Bearer <jwt>` except auth and health)

- `GET /health`, `GET /api/v1/health`
- `POST /api/v1/auth/register`, `POST /api/v1/auth/login`
- `GET|PUT /api/v1/users/me`, `GET /api/v1/users/search?q=`
- `GET|POST /api/v1/projects`, `GET|PUT|DELETE /api/v1/projects/{id}`
- `GET|POST /api/v1/projects/{id}/members`, `PUT|DELETE /api/v1/projects/{id}/members/{memberId}`
- `GET|POST /api/v1/projects/{id}/tasks`
- `GET /api/v1/tasks/mine`, `GET|PUT|DELETE /api/v1/tasks/{id}`, `PATCH /api/v1/tasks/{id}/status`
- `GET /api/v1/dashboard`, `GET /api/v1/analytics`, `GET /api/v1/team`

Access rules: only project members can read a project or its tasks (non-members get 404); only the owner can edit/delete the project and manage members; tasks can only be assigned to members.

## Deploy on Render (Docker)

1. **Database**: Render dashboard → New → PostgreSQL → create. Open it and note *Hostname*, *Database*, *Username*, *Password* (use the **internal** hostname if the backend is in the same region).
2. **Web service**: New → Web Service → connect this repo.
   - Language/Runtime: **Docker** (Render uses the `Dockerfile`)
   - Health Check Path: `/api/v1/health`
   - Environment variables:
     - `DB_URL` = `jdbc:postgresql://<Hostname>:5432/<Database>`
     - `DB_USERNAME` = the Username, `DB_PASSWORD` = the Password
     - `JWT_SECRET` = output of `openssl rand -base64 48`
     - `FRONTEND_ORIGIN` = your frontend URL (put a temporary value first if the frontend does not exist yet, then update it)
     - `APP_TIMEZONE` = e.g. `Asia/Kolkata`
   - Do not set `PORT`; Render provides it.
3. After the frontend is live, set `FRONTEND_ORIGIN` to its exact URL and let the backend redeploy.
4. Open `https://<your-backend>.onrender.com/api/v1/health`; it should return `{"status":"UP",...}`.

`render.yaml` is an optional Blueprint with the same settings.
