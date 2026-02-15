# Auctoritas API Documentation

Base URL: `http://localhost:8080`

## Table of Contents

- [Platform Admin](#platform-admin)
- [Organization](#organization)
- [Projects](#projects)
- [API Keys](#api-keys)
- [SDK Authentication](#sdk-authentication)
- [SDK End Users](#sdk-end-users)
- [Legacy Endpoints](#legacy-endpoints)
- [Configuration](#configuration)

---

## Platform Admin

Internal platform operators with cross-tenant access.

### First Admin Creation (CLI Only)

```bash
cd apps/api
./gradlew bootRun --args="create-admin admin@platform.com password123 'Platform Admin'"
```

### Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/platform/auth/login` | Platform admin login | No |
| POST | `/api/platform/auth/refresh` | Refresh platform admin token | No |

### Platform Admin Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/platform/admin/me` | Get current admin profile | Platform JWT |
| PATCH | `/api/platform/admin/me` | Update admin profile | Platform JWT |
| POST | `/api/platform/admin` | Create new platform admin | Platform JWT |
| DELETE | `/api/platform/admin/{adminId}` | Deactivate platform admin | Platform JWT |
| GET | `/api/platform/admin/organizations` | List all organizations | Platform JWT |
| GET | `/api/platform/admin/organizations/{orgId}` | Get organization details | Platform JWT |
| POST | `/api/platform/admin/organizations/{orgId}/impersonate` | Impersonate organization | Platform JWT |
| GET | `/api/platform/admin/end-users` | List all end users | Platform JWT |

### Examples

**Login:**
```bash
curl -X POST http://localhost:8080/api/platform/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@platform.com","password":"password123"}'
```

**Create additional admin:**
```bash
curl -X POST http://localhost:8080/api/platform/admin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer platform-jwt" \
  -d '{"email":"admin2@platform.com","password":"password123","name":"Second Admin"}'
```

**List all organizations:**
```bash
curl http://localhost:8080/api/platform/admin/organizations \
  -H "Authorization: Bearer platform-jwt"
```

**Impersonate organization:**
```bash
curl -X POST http://localhost:8080/api/platform/admin/organizations/org-id/impersonate \
  -H "Authorization: Bearer platform-jwt"
```

**Search end users:**
```bash
# List all end users
curl http://localhost:8080/api/platform/admin/end-users \
  -H "Authorization: Bearer platform-jwt"

# Search by email (partial match)
curl "http://localhost:8080/api/platform/admin/end-users?email=user@example.com" \
  -H "Authorization: Bearer platform-jwt"

# Filter by project ID
curl "http://localhost:8080/api/platform/admin/end-users?projectId=project-id" \
  -H "Authorization: Bearer platform-jwt"
```

---

## Organization

Organization management and member operations.

### Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/customers/orgs/register` | Create organization + owner | No |
| POST | `/api/v1/customers/auth/login` | Org member login | No |
| POST | `/api/v1/customers/auth/refresh` | Refresh org access token | No |
| GET | `/api/v1/customers/orgs/{orgId}` | Get organization | Org JWT |
| PUT | `/api/v1/customers/orgs/{orgId}` | Update organization | Org JWT (OWNER/ADMIN) |
| POST | `/api/v1/customers/orgs/{orgId}/members/invite` | Invite member | Org JWT (OWNER/ADMIN) |
| POST | `/api/v1/customers/orgs/{orgId}/members/accept` | Accept invitation | No |
| PUT | `/api/v1/customers/orgs/{orgId}/members/{memberId}/role` | Update member role | Org JWT (OWNER) |
| DELETE | `/api/v1/customers/orgs/{orgId}/members/{memberId}` | Remove member | Org JWT (OWNER/ADMIN) |

### Examples

**Create organization:**
```bash
curl -X POST http://localhost:8080/api/v1/customers/orgs/register \
  -H "Content-Type: application/json" \
  -d '{
    "name":"Acme Inc",
    "slug":"acme",
    "ownerEmail":"owner@acme.com",
    "ownerPassword":"password123",
    "ownerName":"Owner"
  }'
```

**Org member login:**
```bash
curl -X POST http://localhost:8080/api/v1/customers/auth/login \
  -H "Content-Type: application/json" \
  -d '{"organizationId":"org-id","email":"owner@acme.com","password":"password123"}'
```

**Invite member:**
```bash
curl -X POST http://localhost:8080/api/v1/customers/orgs/org-id/members/invite \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer org-jwt" \
  -d '{"email":"member@acme.com","role":"ADMIN"}'
```

**Accept invitation:**
```bash
curl -X POST http://localhost:8080/api/v1/customers/orgs/org-id/members/accept \
  -H "Content-Type: application/json" \
  -d '{
    "token":"invitation-token",
    "name":"Member Name",
    "password":"password123"
  }'
```

**Update member role:**
```bash
curl -X PUT http://localhost:8080/api/v1/customers/orgs/org-id/members/member-id/role \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer org-jwt" \
  -d '{"role":"MEMBER"}'
```

**Remove member:**
```bash
curl -X DELETE http://localhost:8080/api/v1/customers/orgs/org-id/members/member-id \
  -H "Authorization: Bearer org-jwt"
```

---

## Projects

Project management within organizations.

### Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/customers/orgs/{orgId}/projects` | Create project | Org JWT (OWNER/ADMIN) |
| GET | `/api/v1/customers/orgs/{orgId}/projects` | List projects | Org JWT |
| GET | `/api/v1/customers/orgs/{orgId}/projects/{projectId}` | Get project | Org JWT |
| DELETE | `/api/v1/customers/orgs/{orgId}/projects/{projectId}` | Archive project | Org JWT (OWNER/ADMIN) |

### Examples

**Create project:**
```bash
curl -X POST http://localhost:8080/api/v1/customers/orgs/org-id/projects \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer org-jwt" \
  -d '{
    "name":"My App",
    "slug":"my-app",
    "description":"Production application"
  }'
```

**List projects:**
```bash
curl http://localhost:8080/api/v1/customers/orgs/org-id/projects \
  -H "Authorization: Bearer org-jwt"
```

**Get project details:**
```bash
curl http://localhost:8080/api/v1/customers/orgs/org-id/projects/project-id \
  -H "Authorization: Bearer org-jwt"
```

**Archive project:**
```bash
curl -X DELETE http://localhost:8080/api/v1/customers/orgs/org-id/projects/project-id \
  -H "Authorization: Bearer org-jwt"
```

---

## API Keys

API key management for SDK authentication.

### Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/v1/customers/orgs/{orgId}/projects/{projectId}/keys` | List API keys | Org JWT |
| POST | `/api/v1/customers/orgs/{orgId}/projects/{projectId}/keys` | Rotate API key | Org JWT (OWNER/ADMIN) |

### Examples

**List API keys:**
```bash
curl http://localhost:8080/api/v1/customers/orgs/org-id/projects/project-id/keys \
  -H "Authorization: Bearer org-jwt"
```

**Rotate API key:**
```bash
curl -X POST http://localhost:8080/api/v1/customers/orgs/org-id/projects/project-id/keys \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer org-jwt" \
  -d '{"environmentId":"PROD"}'
```

---

## SDK Authentication

Authentication for end users of customer applications.

### Requirements
- `X-API-Key` header with valid API key (format: `pk_prod_*` or `pk_dev_*`)
- API key scopes user to specific project and environment

### Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/end-users/auth/register` | Register SDK end user | API Key |
| POST | `/api/v1/end-users/auth/login` | Login SDK end user | API Key |
| GET | `/api/v1/end-users/auth/oauth/github` | GitHub OAuth | API Key |
| GET | `/api/v1/end-users/auth/oauth/google` | Google OAuth | API Key |

### Examples

**Register end user:**
```bash
curl -X POST http://localhost:8080/api/v1/end-users/auth/register \
  -H "Content-Type: application/json" \
  -H "X-API-Key: pk_prod_xxxxx" \
  -d '{
    "email":"user@app.com",
    "password":"password123",
    "name":"App User"
  }'
```

**Login end user:**
```bash
curl -X POST http://localhost:8080/api/v1/end-users/auth/login \
  -H "Content-Type: application/json" \
  -H "X-API-Key: pk_prod_xxxxx" \
  -d '{"email":"user@app.com","password":"password123"}'
```

**OAuth via GitHub:**
```bash
curl -L http://localhost:8080/api/v1/end-users/auth/oauth/github \
  -H "X-API-Key: pk_prod_xxxxx"
```

---

## SDK End Users

End user profile and session management.

### Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/v1/end-users/me` | Get current user profile | SDK JWT + API Key |
| PATCH | `/api/v1/end-users/me` | Update user profile | SDK JWT + API Key |
| GET | `/api/v1/end-users/sessions` | List active sessions | SDK JWT |
| POST | `/api/v1/end-users/sessions` | Create new session | SDK JWT |
| PATCH | `/api/v1/end-users/sessions/{sessionId}` | Extend session | SDK JWT |
| DELETE | `/api/v1/end-users/sessions/{sessionId}` | Revoke session | SDK JWT |

### Examples

**Get profile:**
```bash
curl http://localhost:8080/api/v1/end-users/me \
  -H "Authorization: Bearer sdk-jwt" \
  -H "X-API-Key: pk_prod_xxxxx"
```

**Update profile:**
```bash
curl -X PATCH http://localhost:8080/api/v1/end-users/me \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer sdk-jwt" \
  -H "X-API-Key: pk_prod_xxxxx" \
  -d '{"email":"new@app.com","name":"New Name"}'
```

**List sessions:**
```bash
curl http://localhost:8080/api/v1/end-users/sessions \
  -H "Authorization: Bearer sdk-jwt"
```

**Create session:**
```bash
curl -X POST http://localhost:8080/api/v1/end-users/sessions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer sdk-jwt" \
  -d '{"expiresAt":"2026-12-31T23:59:59Z"}'
```

**Extend session:**
```bash
curl -X PATCH http://localhost:8080/api/v1/end-users/sessions/session-id \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer sdk-jwt" \
  -d '{"expiresAt":"2026-12-31T23:59:59Z"}'
```

**Revoke session:**
```bash
curl -X DELETE http://localhost:8080/api/v1/end-users/sessions/session-id \
  -H "Authorization: Bearer sdk-jwt"
```

---

## Legacy Endpoints

Original authentication endpoints (for backward compatibility).

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | Register new user | No |
| POST | `/api/auth/login` | Login | No |
| POST | `/api/auth/refresh` | Refresh access token | No |
| GET | `/api/auth/oauth/github` | GitHub OAuth | No |
| GET | `/api/auth/oauth/google` | Google OAuth | No |
| GET | `/api/user/me` | Get current user | JWT |
| PATCH | `/api/user/me` | Update profile | JWT |
| GET | `/api/sessions` | List sessions | JWT |
| POST | `/api/sessions` | Create session | JWT |
| PATCH | `/api/sessions/{id}` | Extend session | JWT |
| DELETE | `/api/sessions/{id}` | Revoke session | JWT |

---

## Configuration

### API Configuration

Edit `apps/api/src/main/resources/application.yml`:

```yaml
jwt:
  secret: your-super-secret-key-min-32-bytes
  expiration: 86400000
  refresh-expiration: 604800000

server:
  port: 8080

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
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}

app:
  frontend:
    redirect-url: http://localhost:5173/oauth/callback
    allowed-origins: http://localhost:5173,http://localhost:4321
```

### Security Features

- JWT tokens in httpOnly cookies (XSS protection)
- Access tokens: 24h expiry, Refresh tokens: 7d expiry
- BCrypt password hashing
- API key rotation with immediate revocation
- Project-level data isolation
- Banned user enforcement
