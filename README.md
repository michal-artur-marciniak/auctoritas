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

**Note**: Previously had authentication (better-auth, Drizzle), now fully static.

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

### Platform Admin

**First Admin Creation (CLI Only):**
```bash
cd apps/api
./gradlew bootRun --args="create-admin admin@platform.com password123 'Platform Admin'"
```

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/platform/auth/login` | Platform admin login | No |
| POST | `/api/platform/auth/refresh` | Refresh platform admin token | No |
| GET | `/api/platform/admin/me` | Get current platform admin profile | Platform Admin JWT |
| PATCH | `/api/platform/admin/me` | Update platform admin profile | Platform Admin JWT |
| POST | `/api/platform/admin` | Create platform admin | Platform Admin JWT |

Platform admins are internal platform operators with cross-tenant access to all organizations, projects, and end users for support and management purposes.

**Security Note:** The first platform admin must be created via CLI when no admins exist. After the first admin is created, additional admins can only be created via the HTTP API by an existing platform admin.

**Platform Admin Authentication:**
```bash
# Login (returns access and refresh tokens)
curl -X POST http://localhost:8080/api/platform/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@platform.com","password":"password123"}'

# Refresh token
curl -X POST http://localhost:8080/api/platform/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"refresh-token-from-login"}'

# Create additional admin (requires platform admin JWT)
curl -X POST http://localhost:8080/api/platform/admin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer platform-jwt" \
  -d '{"email":"admin2@platform.com","password":"password123","name":"Second Admin"}'

# Get current admin profile (requires platform admin JWT)
curl http://localhost:8080/api/platform/admin/me \
  -H "Authorization: Bearer platform-jwt"

# Update admin profile (requires platform admin JWT)
curl -X PATCH http://localhost:8080/api/platform/admin/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer platform-jwt" \
  -d '{"name":"Updated Name","email":"new@platform.com"}'

# Change password (requires current password)
curl -X PATCH http://localhost:8080/api/platform/admin/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer platform-jwt" \
  -d '{"currentPassword":"oldpass","newPassword":"newpass123"}'
```

**Token Claims:**
- Platform admin tokens include `type: "platform"` claim
- Inactive platform admins cannot login

### SDK Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/auth/register` | Register SDK end user (scoped to project/env) | API Key |
| POST | `/api/v1/auth/login` | Login SDK end user (scoped to project/env) | API Key |
| GET | `/api/v1/auth/oauth/github` | Initiate GitHub OAuth for SDK users | API Key |
| GET | `/api/v1/auth/oauth/google` | Initiate Google OAuth for SDK users | API Key |

SDK authentication requires the `X-API-Key` header with a valid project API key (format: `pk_prod_*` or `pk_dev_*`). The API key scopes the user to a specific project and environment. Users registered with one API key cannot authenticate with a different API key.

**Banned Users:** Banned users cannot login via password or OAuth. Returns `403 Forbidden` with "User account is banned" message.

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

### Authentication Flow Isolation (US-006)

Organization member authentication and SDK end-user authentication are strictly isolated with separate filters and token types:

| Flow | Endpoints | Token Type | Filter |
|------|-----------|------------|--------|
| **Org Member** | `/api/v1/customers/**` | Org JWT (`type: "org"`) | `OrgJwtAuthenticationFilter` |
| **SDK End User** | `/api/v1/auth/**`, `/api/v1/users/**` | SDK JWT + API Key | `ApiKeyAuthenticationFilter` + `JwtAuthenticationFilter` |
| **Legacy Auth** | `/api/auth/**`, `/api/user/**` | Legacy JWT | `JwtAuthenticationFilter` |

**Isolation Guarantees:**
- Org JWTs cannot access SDK endpoints (returns 401)
- SDK JWTs cannot access org endpoints (returns 401)
- API keys are only processed on SDK routes
- Each filter only processes requests for its designated path prefix

**Org Authentication Flow:**
1. `POST /api/v1/customers/auth/login` - Authenticate with organization ID, email, password
2. Receive `accessToken` and `refreshToken` (both org-scoped)
3. Use `Authorization: Bearer {accessToken}` for org endpoints
4. `POST /api/v1/customers/auth/refresh` - Exchange refresh token for new tokens

### Organization

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/customers/orgs/register` | Create organization + owner | No |
| POST | `/api/v1/customers/auth/login` | Org member login (returns org JWT) | No |
| POST | `/api/v1/customers/auth/refresh` | Refresh org access token | No |
| POST | `/api/v1/customers/orgs/{orgId}/members/invite` | Invite org member | Org JWT |
| POST | `/api/v1/customers/orgs/{orgId}/members/accept` | Accept invitation | No |
| PUT | `/api/v1/customers/orgs/{orgId}/members/{memberId}/role` | Update member role | Org JWT (OWNER) |
| DELETE | `/api/v1/customers/orgs/{orgId}/members/{memberId}` | Remove member | Org JWT (OWNER/ADMIN) |

### Projects

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/customers/orgs/{orgId}/projects` | Create project with PROD/DEV environments + API keys | Org JWT (OWNER/ADMIN) |
| GET | `/api/v1/customers/orgs/{orgId}/projects` | List organization projects | Org JWT |
| GET | `/api/v1/customers/orgs/{orgId}/projects/{projectId}` | Get project details with environments | Org JWT |
| DELETE | `/api/v1/customers/orgs/{orgId}/projects/{projectId}` | Archive project | Org JWT (OWNER/ADMIN) |

### API Key Management

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/v1/customers/orgs/{orgId}/projects/{projectId}/keys` | List API keys for project (redacted) | Org JWT |
| POST | `/api/v1/customers/orgs/{orgId}/projects/{projectId}/keys` | Rotate API key for environment | Org JWT (OWNER/ADMIN) |

**API Key Rotation:**
- POST body: `{"environmentId": "PROD"}` or `{"environmentId": "DEV"}`
- Returns new raw key once (cannot be retrieved again)
- Old key is immediately revoked
- New key has prefix `pk_prod_*` or `pk_dev_*`

* Can use refresh token from cookie or request body

### Sessions

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/sessions` | List active sessions | Yes |
| POST | `/api/sessions` | Create new session | Yes |
| PATCH | `/api/sessions/{id}` | Extend session expiry | Yes |
| DELETE | `/api/sessions/{id}` | Revoke session | Yes |

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
curl -X POST http://localhost:8080/api/v1/customers/orgs/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Acme Inc","slug":"acme","ownerEmail":"owner@acme.com","ownerPassword":"password123","ownerName":"Owner"}'

# Org member login
curl -X POST http://localhost:8080/api/v1/customers/auth/login \
  -H "Content-Type: application/json" \
  -d '{"organizationId":"org-id","email":"owner@acme.com","password":"password123"}'

# Refresh org token
curl -X POST http://localhost:8080/api/v1/customers/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"refresh-token-from-login"}'

# Invite org member
curl -X POST http://localhost:8080/api/v1/customers/orgs/org-id/members/invite \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer org-jwt" \
  -d '{"email":"member@acme.com","role":"ADMIN"}'

# Accept invitation
curl -X POST http://localhost:8080/api/v1/customers/orgs/org-id/members/accept \
  -H "Content-Type: application/json" \
  -d '{"token":"invitation-token","name":"Member Name","password":"password123"}'

# Update member role
curl -X PUT http://localhost:8080/api/v1/customers/orgs/org-id/members/member-id/role \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer org-jwt" \
  -d '{"role":"MEMBER"}'

# Remove member
curl -X DELETE http://localhost:8080/api/v1/customers/orgs/org-id/members/member-id \
  -H "Authorization: Bearer org-jwt"

# Create project with PROD/DEV environments (returns API keys once)
curl -X POST http://localhost:8080/api/v1/customers/orgs/org-id/projects \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer org-jwt" \
  -d '{"name":"My App","slug":"my-app","description":"Production application"}'

# List projects
curl http://localhost:8080/api/v1/customers/orgs/org-id/projects \
  -H "Authorization: Bearer org-jwt"

# Get project details
curl http://localhost:8080/api/v1/customers/orgs/org-id/projects/project-id \
  -H "Authorization: Bearer org-jwt"

# Archive project
curl -X DELETE http://localhost:8080/api/v1/customers/orgs/org-id/projects/project-id \
  -H "Authorization: Bearer org-jwt"

# List API keys for project (returns metadata, no raw keys)
curl http://localhost:8080/api/v1/customers/orgs/org-id/projects/project-id/keys \
  -H "Authorization: Bearer org-jwt"

# Rotate API key (returns new raw key once)
curl -X POST http://localhost:8080/api/v1/customers/orgs/org-id/projects/project-id/keys \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer org-jwt" \
  -d '{"environmentId":"PROD"}'

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

# SDK end user OAuth via GitHub (requires API key, redirects to GitHub)
curl -L http://localhost:8080/api/v1/auth/oauth/github \
  -H "X-API-Key: pk_prod_xxxxx"

# SDK end user OAuth via Google (requires API key, redirects to Google)
curl -L http://localhost:8080/api/v1/auth/oauth/google \
  -H "X-API-Key: pk_prod_xxxxx"
```

### Platform Admin Creation

**First admin via CLI (when no admins exist):**
```bash
cd apps/api
./gradlew bootRun --args="create-admin admin@platform.com password123 'Platform Admin'"
```

**Additional admins via API (requires platform admin JWT - implemented in US-PA-002):**
```bash
curl -X POST http://localhost:8080/api/platform/admin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer platform-jwt" \
  -d '{"email":"admin2@platform.com","password":"password123","name":"Second Admin"}'
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
| Tokens in localStorage | httpOnly cookies |

**Key Features**:
- OAuth login (GitHub, Google) with email fallback
- Session management (create, extend, revoke, list)
- Profile updates with email uniqueness checks
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
- No secrets in repositories (use environment variables)

## License

MIT
