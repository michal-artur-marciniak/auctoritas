# OpenAgents Cloud

A modern monorepo using **pnpm workspaces** with a Spring Boot API handling authentication and a React frontend.

## Overview

OpenAgents Cloud provides secure OpenClaw hosting. The platform consists of:

- **API** (`apps/api`): Spring Boot authentication service with JWT tokens
- **App** (`apps/app`): React + Vite frontend (consumer of API)
- **WWW** (`apps/www`): Astro static marketing site

## Quick Start

```bash
# Install dependencies
pnpm install

# Run all apps in development mode
pnpm dev

# Or run individually
cd apps/api && ./gradlew bootRun   # API on :8080
cd apps/app && pnpm dev           # React app on :5173
cd apps/www && pnpm dev           # Marketing site on :4321
```

## Structure

```
.
├── apps/
│   ├── api/          # Spring Boot API (Java 25, JWT Auth, SQLite)
│   ├── app/          # React + Vite frontend
│   └── www/          # Astro + Vite marketing site
├── package.json      # Root package.json
└── pnpm-workspace.yaml
```

## Architecture

### Authentication Flow

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   React App  │────▶│  Spring API  │────▶│   SQLite DB  │
│  (apps/app)  │     │  (apps/api)  │     │  (data/app)  │
└──────────────┘     └──────────────┘     └──────────────┘
       │                    │
       │◀─── httpOnly ──────┤
       │     Cookies        │
       │                    │
       │──── Authorized ───▶│
       │    Requests        │
```

The **API** is the sole authentication authority. It issues JWT tokens in httpOnly cookies for XSS protection. The **React app** reads the cookie automatically with each request.

The **marketing site** (`www`) is fully static with no authentication dependencies.

## Technologies

### API (`apps/api`)

| Technology | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 4.0.2 | Application framework |
| Java | 25 (LTS) | Language |
| Spring Security | 6.5 | JWT authentication |
| JJWT | 0.12.6 | JWT implementation |
| SQLite | 3.51.1.0 | Database |
| Gradle | Kotlin DSL | Build tool |

**Architecture**: Pragmatic DDD with clean architecture layers (Domain, Application, Infrastructure, Presentation).

### App (`apps/app`)

| Technology | Version |
|------------|---------|
| React | 19.2.0 |
| Vite | 6.0.11 |
| TypeScript | 5.7 |

### WWW (`apps/www`)

| Technology | Version |
|------------|---------|
| Astro | 5.16.6 |
| Vite | 6.0.x |
| Tailwind CSS | 4.1.18 |

**Note**: Previously had authentication (better-auth, Stripe, Drizzle), now fully static.

## Prerequisites

- Node.js >= 22
- pnpm >= 10
- Java 25

## API Endpoints

The API runs on `http://localhost:8080` by default.

### Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | Register new user | No |
| POST | `/api/auth/login` | Login (sets httpOnly cookies) | No |
| POST | `/api/auth/refresh` | Refresh access token | No* |
| GET | `/api/auth/oauth/github` | Initiate GitHub OAuth | No |
| GET | `/api/auth/oauth/google` | Initiate Google OAuth | No |

### SDK Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/auth/register` | Register SDK end user (scoped to project/env) | API Key |
| POST | `/api/v1/auth/login` | Login SDK end user (scoped to project/env) | API Key |

SDK authentication requires the `X-API-Key` header with a valid project API key (format: `pk_prod_*` or `pk_dev_*`). The API key scopes the user to a specific project and environment. Users registered with one API key cannot authenticate with a different API key.

### SDK User

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/v1/users/me` | Get current SDK user profile | SDK JWT |

### Project-Level Isolation

End users are strictly isolated per project and environment (US-005). Key features:

- **Repository Filtering**: User queries include `projectId` and `environmentId` filters
- **Cross-Project Protection**: Accessing a user from a different project returns 404 Not Found
- **API Key Scoping**: Each SDK request includes project context from the API key
- **JWT Enforcement**: SDK endpoints validate the user belongs to the requesting project

This ensures data never leaks between tenants, even with the same email address across different projects.

### Organization

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/org/register` | Create organization + owner | No |
| POST | `/api/v1/org/auth/login` | Org member login (returns org JWT) | No |
| POST | `/api/v1/org/{orgId}/members/invite` | Invite org member | Org JWT |
| POST | `/api/v1/org/{orgId}/members/accept` | Accept invitation | No |
| PUT | `/api/v1/org/{orgId}/members/{memberId}/role` | Update member role | Org JWT (OWNER) |
| DELETE | `/api/v1/org/{orgId}/members/{memberId}` | Remove member | Org JWT (OWNER/ADMIN) |

### Projects

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/org/{orgId}/projects` | Create project with PROD/DEV environments + API keys | Org JWT (OWNER/ADMIN) |
| GET | `/api/v1/org/{orgId}/projects` | List organization projects | Org JWT |
| GET | `/api/v1/org/{orgId}/projects/{projectId}` | Get project details with environments | Org JWT |
| DELETE | `/api/v1/org/{orgId}/projects/{projectId}` | Archive project | Org JWT (OWNER/ADMIN) |

\* Can use refresh token from cookie or request body

### Sessions

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/sessions` | List active sessions | Yes |
| POST | `/api/sessions` | Create new session | Yes |
| PATCH | `/api/sessions/{id}` | Extend session expiry | Yes |
| DELETE | `/api/sessions/{id}` | Revoke session | Yes |

### Stripe

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/stripe/webhook` | Handle Stripe events | No* |

\* Uses Stripe signature verification

### User

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/user/me` | Get current user profile | Yes |
| PATCH | `/api/user/me` | Update profile (email, name) | Yes |
| GET | `/api/user/admin/check` | Admin-only endpoint | Yes (ADMIN role) |

### Public

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/hello` | Hello message |
| GET | `/api/health` | Health check |

### Example API Usage

```bash
# Register (sets httpOnly cookies automatically)
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123","name":"John"}' \
  -c cookies.txt

# Login (sets httpOnly cookies)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}' \
  -c cookies.txt

# Get profile (cookies sent automatically)
curl http://localhost:8080/api/user/me \
  -b cookies.txt

# Update profile
curl -X PATCH http://localhost:8080/api/user/me \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{"email":"new@example.com","name":"New Name"}'

# List sessions
curl http://localhost:8080/api/sessions \
  -b cookies.txt

# OAuth (redirects to provider)
curl -L http://localhost:8080/api/auth/oauth/github

# Create organization with owner
curl -X POST http://localhost:8080/api/v1/org/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Acme Inc","slug":"acme","ownerEmail":"owner@acme.com","ownerPassword":"password123","ownerName":"Owner"}'

# Org member login
curl -X POST http://localhost:8080/api/v1/org/auth/login \
  -H "Content-Type: application/json" \
  -d '{"organizationId":"org-id","email":"owner@acme.com","password":"password123"}'

# Invite org member
curl -X POST http://localhost:8080/api/v1/org/org-id/members/invite \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer org-jwt" \
  -d '{"email":"member@acme.com","role":"ADMIN"}'

# Accept invitation
curl -X POST http://localhost:8080/api/v1/org/org-id/members/accept \
  -H "Content-Type: application/json" \
  -d '{"token":"invitation-token","name":"Member Name","password":"password123"}'

# Update member role
curl -X PUT http://localhost:8080/api/v1/org/org-id/members/member-id/role \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer org-jwt" \
  -d '{"role":"MEMBER"}'

# Remove member
curl -X DELETE http://localhost:8080/api/v1/org/org-id/members/member-id \
  -H "Authorization: Bearer org-jwt"

# Create project with PROD/DEV environments (returns API keys once)
curl -X POST http://localhost:8080/api/v1/org/org-id/projects \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer org-jwt" \
  -d '{"name":"My App","slug":"my-app","description":"Production application"}'

# List projects
curl http://localhost:8080/api/v1/org/org-id/projects \
  -H "Authorization: Bearer org-jwt"

# Get project details
curl http://localhost:8080/api/v1/org/org-id/projects/project-id \
  -H "Authorization: Bearer org-jwt"

# Archive project
curl -X DELETE http://localhost:8080/api/v1/org/org-id/projects/project-id \
  -H "Authorization: Bearer org-jwt"

# SDK end user registration (requires API key from project creation)
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -H "X-API-Key: pk_prod_xxxxx" \
  -d '{"email":"user@app.com","password":"password123","name":"App User"}'

# SDK end user login (requires API key)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-API-Key: pk_prod_xxxxx" \
  -d '{"email":"user@app.com","password":"password123"}'
```

## Development

### Running the API

```bash
cd apps/api

# Build
./gradlew build

# Run
./gradlew bootRun

# Test
./gradlew test
```

### Running the React App

```bash
cd apps/app
pnpm install
pnpm dev
```

The React dev server proxies API requests to `http://localhost:8080` automatically.

### Running the Marketing Site

```bash
cd apps/www
pnpm install
pnpm dev
```

## Configuration

### API Configuration

Edit `apps/api/src/main/resources/application.yml`:

```yaml
# JWT Secret (CHANGE IN PRODUCTION!)
jwt:
  secret: your-super-secret-key-min-32-bytes
  expiration: 86400000       # 24 hours (access token)
  refresh-expiration: 604800000  # 7 days (refresh token)

# Server port
server:
  port: 8080

# Database (SQLite)
spring:
  datasource:
    url: jdbc:sqlite:./data/app.db
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope: user:email
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: email,profile

# Frontend integration
app:
  frontend:
    redirect-url: http://localhost:5173/oauth/callback
    allowed-origins: http://localhost:5173,http://localhost:4321
  auth:
    cookies:
      secure: true
      same-site: Lax

# Stripe integration
stripe:
  secret-key: ${STRIPE_SECRET_KEY}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET}
```

### React App Configuration

The React app uses Vite's proxy configuration in `apps/app/vite.config.ts`:

```typescript
server: {
  proxy: {
    '/api': 'http://localhost:8080'
  }
}
```

## Project Migration History

### Auth Migration (Completed)

Previously, authentication was handled by `apps/www` using better-auth with SQLite. This was migrated to a dedicated Spring Boot API:

| Before | After |
|--------|-------|
| `apps/www` had auth pages (signin, signup, dashboard, admin) | `apps/www` is now fully static |
| better-auth library in Node.js | Spring Security + JWT in Java |
| Database: better-sqlite3 | Database: SQLite + JPA |
| Auth logic in Astro pages | Auth logic in API with pragmatic DDD |
| Stripe subscriptions in www | Stripe webhooks in API |
| Tokens in localStorage | httpOnly cookies |

**Key Features**:
- OAuth login (GitHub, Google) with email fallback
- Session management (create, extend, revoke, list)
- Profile updates with email uniqueness checks
- Stripe customer mapping and subscription sync
- httpOnly cookie-based token storage for XSS protection

## Scripts

Root `package.json` scripts:

```bash
pnpm dev      # Run all apps in dev mode
pnpm build    # Build all apps
pnpm test     # Run all tests
```

## Security

- JWT tokens stored in httpOnly cookies (XSS protection)
- Access tokens expire after 24 hours, refresh tokens after 7 days
- Passwords hashed with BCrypt
- CORS configured for localhost development
- OAuth2 state parameter protection
- Stripe webhook signature verification
- No secrets in repositories (use environment variables)

## License

MIT
