# Auctoritas Phase 1 - Versioning Plan

## Overview

This document defines semantic versioning milestones from **v0.1.0** (initial structure) to **v1.0.0** (production-ready Phase 1 complete). Each version represents a meaningful, demonstrable milestone.

### Versioning Strategy

```
v0.x.y - Pre-release development versions
  0.1.x - Foundation & infrastructure
  0.2.x - Core backend services
  0.3.x - Authentication features  
  0.4.x - Advanced auth (OAuth, MFA)
  0.5.x - RBAC & session management
  0.6.x - React SDK foundation
  0.7.x - SDK components complete
  0.8.x - Dashboard MVP
  0.9.x - Worker service & async processing
  
v1.0.0 - Production-ready release
```

---

## Version Breakdown

### v0.1.0 - Project Foundation (Released)
**Theme:** "The Skeleton"  
**Status:** Released  
**Demo:** Project builds, Docker Compose runs, health endpoints respond

#### Deliverables

**Monorepo Structure (Ecosystem Separation)**
```
auctoritas/
│
├── backend/                             # Java ecosystem (Maven)
│   ├── pom.xml                          # Maven parent POM
│   ├── services/
│   │   ├── gateway-service/
│   │   │   ├── pom.xml
│   │   │   └── src/main/java/.../GatewayApplication.java
│   │   ├── auth-service/
│   │   │   ├── pom.xml
│   │   │   └── src/main/java/.../AuthApplication.java
│   │   └── worker-service/
│   │       ├── pom.xml
│   │       └── src/main/java/.../WorkerApplication.java
│   └── libs/
│       └── common/
│           ├── pom.xml
│           └── src/main/java/.../
│               ├── dto/                 # Shared DTOs
│               ├── exception/           # Common exceptions
│               └── util/                # Utilities
│
├── frontend/                            # JavaScript ecosystem (pnpm)
│   ├── package.json                     # Workspace root
│   ├── pnpm-workspace.yaml
│   ├── apps/
│   │   └── dashboard/
│   │       └── package.json             # Empty React app placeholder
│   └── packages/
│       └── sdk-react/
│           └── package.json             # Empty SDK placeholder
│
├── infra/
│   ├── docker/
│   │   ├── Dockerfile.gateway
│   │   ├── Dockerfile.auth
│   │   ├── Dockerfile.worker
│   │   └── Dockerfile.dashboard
│   └── k8s/
│       └── base/                        # Empty k8s manifests
│
├── .github/workflows/
│   ├── backend-ci.yml                   # Java build & test
│   └── frontend-ci.yml                  # JS build & test
│
├── docker-compose.yml
├── .gitignore
├── LICENSE
└── README.md
```

**Technical Tasks**
- [x] Initialize monorepo with ecosystem separation structure
- [x] Setup backend/pom.xml (Maven parent) with:
  - Spring Boot 4.0.x parent
  - Java 21 configuration
  - Dependency management (Spring Cloud, etc.)
  - Plugin management (versions, builds)
- [x] Create backend/services/gateway-service module (Spring Cloud Gateway)
- [x] Create backend/services/auth-service module (Spring Boot Web + JPA)
- [x] Create backend/services/worker-service module (Spring Boot + AMQP)
- [x] Create backend/libs/common module with base exceptions & DTOs
- [x] Setup frontend/package.json and frontend/pnpm-workspace.yaml
- [x] Create frontend/apps/dashboard placeholder
- [x] Create frontend/packages/sdk-react placeholder
- [x] Setup Docker Compose with:
  - PostgreSQL 18
  - Redis 8.4
  - RabbitMQ 4.2 (with management UI)
- [x] Basic Dockerfiles for all services
- [x] Health check endpoints (`/actuator/health`)
- [x] GitHub Actions CI workflow
- [x] README with project overview

**Acceptance Criteria**
```bash
# All services start
docker-compose up -d

# Health checks pass
curl http://localhost:8080/actuator/health  # Gateway
curl http://localhost:8081/actuator/health  # Auth
curl http://localhost:8082/actuator/health  # Worker

# Maven build succeeds (from backend directory)
cd backend && mvn clean verify

# Frontend installs (from frontend directory)
cd frontend && pnpm install
```

---

### v0.1.1 - Database Foundation
**Theme:** "Data Layer Ready"  
**Duration:** ~3-4 days

#### Deliverables

**Database Schema (Initial Migration)**
```sql
-- V1__init_organizations.sql
-- Organizations & org members tables only
CREATE TABLE organizations (...);
CREATE TABLE organization_members (...);
CREATE TABLE org_member_sessions (...);

-- V2__init_projects.sql
CREATE TABLE projects (...);
CREATE TABLE project_settings (...);
CREATE TABLE api_keys (...);
```

**Technical Tasks**
- [ ] Configure Flyway migrations
- [ ] Create initial schema: organizations, org_members, projects
- [ ] Setup JPA entities with base audit fields
- [ ] Configure Argon2 password encoder
- [ ] Add structured JSON logging (correlation IDs)
- [ ] Environment-based configuration (dev/prod profiles)

**Acceptance Criteria**
- Flyway migrations run on startup
- JPA entity tests pass
- Logs output structured JSON with correlation IDs

---

### v0.2.0 - Organization Registration
**Theme:** "First User Can Sign Up"  
**Duration:** ~1 week  
**Demo:** Register organization via API, receive JWT

#### API Endpoints

```
POST /api/v1/org/register
     Body: { email, password, name, organizationName }
     Response: { organization, member, accessToken, refreshToken }

POST /api/v1/org/login
     Body: { email, password }
     Response: { member, accessToken, refreshToken }
```

#### Technical Tasks

**Auth Service**
- [ ] OrganizationService with registration logic
- [ ] OrgMemberService with login logic
- [ ] Password validation service (configurable rules)
- [ ] JWT service (RS256 key pair generation)
- [ ] Refresh token implementation with rotation
- [ ] Account lockout after failed attempts (5 in 15 min)

**Gateway Service**
- [ ] Route configuration for `/api/v1/org/**`
- [ ] Basic rate limiting with Redis (bucket4j)
- [ ] CORS configuration

**Common Library**
- [ ] JWT utilities
- [ ] Password policy DTO
- [ ] Standard API response wrapper
- [ ] Global exception handler

**Database Migration**
```sql
-- V3__add_refresh_tokens_org.sql
CREATE TABLE org_member_refresh_tokens (...);
```

**Acceptance Criteria**
```bash
# Register organization
curl -X POST http://localhost:8080/api/v1/org/register \
  -H "Content-Type: application/json" \
  -d '{"email":"owner@acme.com","password":"SecurePass123!","name":"John","organizationName":"Acme Corp"}'

# Login
curl -X POST http://localhost:8080/api/v1/org/login \
  -H "Content-Type: application/json" \
  -d '{"email":"owner@acme.com","password":"SecurePass123!"}'

# Access protected route with JWT
curl http://localhost:8080/api/v1/org/me \
  -H "Authorization: Bearer <token>"
```

---

### v0.2.1 - Project Management
**Theme:** "Create & Manage Projects"  
**Duration:** ~4-5 days

#### API Endpoints

```
GET    /api/v1/org/{orgId}/projects
POST   /api/v1/org/{orgId}/projects
       Body: { name, slug }
       Response: { project, defaultApiKey }

GET    /api/v1/org/{orgId}/projects/{projectId}
PUT    /api/v1/org/{orgId}/projects/{projectId}
DELETE /api/v1/org/{orgId}/projects/{projectId}

GET    /api/v1/org/{orgId}/projects/{projectId}/settings
PUT    /api/v1/org/{orgId}/projects/{projectId}/settings/password
PUT    /api/v1/org/{orgId}/projects/{projectId}/settings/session
```

#### Technical Tasks
- [ ] ProjectService with CRUD operations
- [ ] ProjectSettingsService (password policy, session config)
- [ ] API key generation (prefix: `pk_live_` / `pk_test_`)
- [ ] Secure API key storage (hash with SHA-256, show once)
- [ ] Organization role authorization (OWNER/ADMIN for delete)

**Acceptance Criteria**
- Create project returns API key (visible only once)
- Project settings are isolated per project
- Non-owners cannot delete projects

---

### v0.3.0 - End User Authentication
**Theme:** "SDK Users Can Sign Up"  
**Duration:** ~1 week  
**Demo:** End user registers/logs in via API key-authenticated endpoint

#### Database Migration
```sql
-- V4__init_end_users.sql
CREATE TABLE users (...);
CREATE TABLE user_profiles (...);
CREATE TABLE sessions (...);
CREATE TABLE refresh_tokens (...);
```

#### API Endpoints

```
# All endpoints require X-API-Key header

POST /api/v1/auth/register
     Headers: X-API-Key: pk_live_xxx
     Body: { email, password, name? }
     Response: { user, accessToken, refreshToken }

POST /api/v1/auth/login
     Body: { email, password }
     Response: { user, accessToken, refreshToken }

POST /api/v1/auth/logout
     Headers: Authorization: Bearer <token>

POST /api/v1/auth/refresh
     Body: { refreshToken }
     Response: { accessToken, refreshToken }
```

#### Technical Tasks

**Gateway Service**
- [ ] API key validation filter
- [ ] API key -> project_id resolution with Redis cache
- [ ] Route requests with project context header
- [ ] Separate rate limits for SDK endpoints

**Auth Service**
- [ ] EndUserService with registration/login
- [ ] Apply project-specific password policy
- [ ] Session management (device info, IP tracking)
- [ ] End user JWT with project-scoped claims
- [ ] Account lockout per user per project

**Acceptance Criteria**
```bash
# End user register (with project API key)
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "X-API-Key: pk_live_xxx" \
  -H "Content-Type: application/json" \
  -d '{"email":"user@app.com","password":"UserPass123!"}'

# End user login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "X-API-Key: pk_live_xxx" \
  -d '{"email":"user@app.com","password":"UserPass123!"}'
```

---

### v0.3.1 - Password Reset Flow
**Theme:** "Users Can Recover Access"  
**Duration:** ~3-4 days

#### API Endpoints

```
POST /api/v1/auth/password/forgot
     Body: { email }
     Response: { message: "Reset email sent" }

POST /api/v1/auth/password/reset
     Body: { token, newPassword }
     Response: { message: "Password updated" }

PUT  /api/v1/users/me/password
     Headers: Authorization: Bearer <token>
     Body: { currentPassword, newPassword }
```

#### Technical Tasks
- [ ] Password reset token generation (secure random, 1hr expiry)
- [ ] Token storage and validation
- [ ] Password history enforcement (prevent reuse of last N)
- [ ] RabbitMQ event: `user.password_reset_requested`
- [ ] Stub email sending (log to console for now)

**Database Migration**
```sql
-- V5__password_reset.sql
CREATE TABLE password_reset_tokens (...);
CREATE TABLE password_history (...);
```

---

### v0.3.2 - Email Verification
**Theme:** "Verify Email Addresses"  
**Duration:** ~2-3 days

#### API Endpoints

```
POST /api/v1/auth/register/verify-email
     Body: { code }
     Response: { verified: true }

POST /api/v1/auth/register/resend-verification
     Body: { email }
```

#### Technical Tasks
- [ ] Email verification code generation (6 digits, 24hr expiry)
- [ ] Verification status tracking
- [ ] RabbitMQ event: `user.registered` (triggers verification email)
- [ ] Optional: block login until verified (project setting)

---

### v0.4.0 - OAuth Integration
**Theme:** "Sign In With Google"  
**Duration:** ~1 week  
**Demo:** Complete OAuth flow with Google

#### Database Migration
```sql
-- V6__oauth_connections.sql
CREATE TABLE oauth_connections (...);
```

#### API Endpoints

```
GET  /api/v1/auth/oauth/{provider}/authorize
     Query: redirect_uri
     Response: Redirect to provider

GET  /api/v1/auth/oauth/{provider}/callback
     Query: code, state
     Response: { user, accessToken, refreshToken }
```

#### Technical Tasks
- [ ] OAuth provider abstraction (interface + implementations)
- [ ] Google OAuth implementation with PKCE
- [ ] State parameter handling (CSRF protection)
- [ ] Account linking (existing email matches OAuth email)
- [ ] Per-project OAuth credentials storage (encrypted)
- [ ] OAuth settings in project_settings

**Provider Settings UI (via API)**
```json
PUT /api/v1/org/{orgId}/projects/{projectId}/settings/oauth
{
  "google": {
    "enabled": true,
    "clientId": "xxx.apps.googleusercontent.com",
    "clientSecret": "GOCSPX-xxx"
  }
}
```

---

### v0.4.1 - Additional OAuth Providers
**Theme:** "More Sign-In Options"  
**Duration:** ~4-5 days

#### Technical Tasks
- [ ] GitHub OAuth implementation
- [ ] Apple Sign-In implementation (requires additional config)
- [ ] Microsoft OAuth implementation
- [ ] Facebook OAuth implementation
- [ ] Provider factory pattern for easy extension

**Acceptance Criteria**
- All 5 providers functional
- Account linking works across providers
- Provider can be disabled per project

---

### v0.4.2 - MFA (TOTP)
**Theme:** "Two-Factor Security"  
**Duration:** ~1 week

#### Database Migration
```sql
-- V7__mfa.sql
CREATE TABLE user_mfa (...);
CREATE TABLE org_member_mfa (...);
```

#### API Endpoints

```
# End User MFA
POST /api/v1/users/me/mfa/setup
     Response: { secret, qrCode, backupCodes }

POST /api/v1/users/me/mfa/verify
     Body: { code }
     Response: { enabled: true }

DELETE /api/v1/users/me/mfa
       Body: { code }

POST /api/v1/auth/login/mfa
     Body: { mfaToken, code }
     Response: { user, accessToken, refreshToken }

# Org Member MFA (same pattern)
POST /api/v1/org/me/mfa/setup
POST /api/v1/org/me/mfa/verify
POST /api/v1/org/login/mfa
```

#### Technical Tasks
- [ ] TOTP secret generation (Google Authenticator compatible)
- [ ] QR code generation for authenticator apps
- [ ] Recovery code generation (8 codes, one-time use)
- [ ] MFA challenge flow (return mfaToken on login)
- [ ] Recovery code login flow
- [ ] MFA settings per project (required vs optional)

---

### v0.5.0 - RBAC System
**Theme:** "Roles & Permissions"  
**Duration:** ~1 week  
**Demo:** Create roles, assign to users, enforce permissions

#### Database Migration
```sql
-- V8__rbac.sql
CREATE TABLE roles (...);
CREATE TABLE permissions (...);
CREATE TABLE role_permissions (...);
CREATE TABLE user_roles (...);

-- Seed default permissions
INSERT INTO permissions (code, description, category) VALUES
  ('users:read', 'View users', 'users'),
  ('users:write', 'Create/update users', 'users'),
  ('users:delete', 'Delete users', 'users'),
  ...
```

#### API Endpoints

```
# Role Management (Dashboard)
GET    /api/v1/org/{orgId}/projects/{projectId}/roles
POST   /api/v1/org/{orgId}/projects/{projectId}/roles
PUT    /api/v1/org/{orgId}/projects/{projectId}/roles/{roleId}
DELETE /api/v1/org/{orgId}/projects/{projectId}/roles/{roleId}

# Permission Assignment
PUT    /api/v1/org/{orgId}/projects/{projectId}/roles/{roleId}/permissions
       Body: { permissions: ["users:read", "users:write"] }

# User Role Assignment
PUT    /api/v1/org/{orgId}/projects/{projectId}/users/{userId}/roles
       Body: { roles: ["admin", "moderator"] }

# Permission Check (SDK)
GET    /api/v1/users/me/permissions
       Response: { roles: [...], permissions: [...] }
```

#### Technical Tasks
- [ ] RoleService with CRUD
- [ ] PermissionService (seeded permissions)
- [ ] User role assignment
- [ ] JWT claims include roles + resolved permissions
- [ ] Spring Security method-level authorization
- [ ] Default roles created per project (admin, user)

---

### v0.5.1 - Session Management
**Theme:** "Control Active Sessions"  
**Duration:** ~4-5 days

#### API Endpoints

```
# End User Sessions
GET    /api/v1/users/me/sessions
       Response: [{ id, deviceInfo, ipAddress, lastActive, current }]

DELETE /api/v1/users/me/sessions/{sessionId}
DELETE /api/v1/users/me/sessions
       Query: ?keepCurrent=true

# Org Member Sessions (same pattern)
GET    /api/v1/org/me/sessions
DELETE /api/v1/org/me/sessions/{sessionId}
```

#### Technical Tasks
- [ ] Session listing with device fingerprinting
- [ ] Individual session revocation
- [ ] Bulk session revocation ("logout everywhere")
- [ ] JWT revocation via Redis blacklist (by session_id)
- [ ] Max sessions per user (configurable per project)

---

### v0.5.2 - User Management (Dashboard)
**Theme:** "Admins Manage Users"  
**Duration:** ~3-4 days

#### API Endpoints

```
GET    /api/v1/org/{orgId}/projects/{projectId}/users
       Query: ?page=0&size=20&search=john&status=ACTIVE

GET    /api/v1/org/{orgId}/projects/{projectId}/users/{userId}
PUT    /api/v1/org/{orgId}/projects/{projectId}/users/{userId}
DELETE /api/v1/org/{orgId}/projects/{projectId}/users/{userId}
POST   /api/v1/org/{orgId}/projects/{projectId}/users/{userId}/reset-password
POST   /api/v1/org/{orgId}/projects/{projectId}/users/{userId}/lock
POST   /api/v1/org/{orgId}/projects/{projectId}/users/{userId}/unlock
```

#### Technical Tasks
- [ ] Paginated user listing with search
- [ ] User detail view (profile, sessions, roles)
- [ ] Admin password reset (generates email)
- [ ] Account lock/unlock
- [ ] User status management (ACTIVE, LOCKED, SUSPENDED)

---

### v0.6.0 - React SDK Foundation
**Theme:** "SDK Package Exists"  
**Duration:** ~1 week  
**Demo:** NPM package installs, Provider wraps app, basic auth works

#### Package Structure
```
frontend/packages/sdk-react/
├── package.json
├── tsconfig.json
├── vite.config.ts
├── src/
│   ├── index.ts                 # Public exports
│   ├── provider/
│   │   └── AuctoritasProvider.tsx
│   ├── hooks/
│   │   ├── useAuth.ts
│   │   └── useUser.ts
│   ├── api/
│   │   └── client.ts            # Axios/fetch wrapper
│   ├── store/
│   │   └── authStore.ts         # Zustand or context
│   └── types/
│       └── index.ts
└── README.md
```

#### Technical Tasks
- [ ] Initialize React package with Vite
- [ ] AuctoritasProvider with configuration
- [ ] API client with interceptors (auto-refresh)
- [ ] Token storage strategy (memory + refresh in httpOnly cookie)
- [ ] useAuth hook: `{ isLoading, isAuthenticated, login, logout, register }`
- [ ] useUser hook: `{ user, isLoading }`
- [ ] TypeScript types for all responses
- [ ] Build configuration (ESM + CJS bundles)
- [ ] Local development linking

**Usage Example**
```tsx
import { AuctoritasProvider, useAuth } from '@auctoritas/react';

function App() {
  return (
    <AuctoritasProvider 
      apiKey="pk_live_xxx"
      apiUrl="https://api.auctoritas.dev"
    >
      <MyApp />
    </AuctoritasProvider>
  );
}

function LoginButton() {
  const { login, isLoading } = useAuth();
  return <button onClick={() => login(email, password)}>Login</button>;
}
```

---

### v0.7.0 - SDK Components
**Theme:** "Pre-Built UI Components"  
**Duration:** ~1 week  
**Demo:** Drop-in SignIn component works

#### Components

```
src/components/
├── SignIn/
│   ├── SignIn.tsx
│   └── SignIn.stories.tsx
├── SignUp/
│   └── SignUp.tsx
├── UserButton/
│   └── UserButton.tsx
├── OAuthButtons/
│   └── OAuthButtons.tsx
├── MfaChallenge/
│   └── MfaChallenge.tsx
├── MfaSetup/
│   └── MfaSetup.tsx
└── ProtectedRoute/
    └── ProtectedRoute.tsx
```

#### Technical Tasks
- [ ] SignIn component (email/password + OAuth buttons)
- [ ] SignUp component with validation
- [ ] OAuthButtons component (configurable providers)
- [ ] UserButton component (avatar + dropdown)
- [ ] MfaChallenge component
- [ ] MfaSetup component (QR code display)
- [ ] ProtectedRoute wrapper component
- [ ] usePermissions hook
- [ ] Storybook setup with all components
- [ ] Tailwind-based styling (customizable via CSS vars)

**Usage Example**
```tsx
import { SignIn, ProtectedRoute } from '@auctoritas/react';

<Route path="/login" element={<SignIn />} />
<Route 
  path="/dashboard" 
  element={
    <ProtectedRoute fallback={<Navigate to="/login" />}>
      <Dashboard />
    </ProtectedRoute>
  } 
/>
```

---

### v0.7.1 - SDK Advanced Features
**Theme:** "Real-Time & Polish"  
**Duration:** ~4-5 days

#### Technical Tasks
- [ ] WebSocket connection for real-time session updates
- [ ] Session invalidation notification (logout on other device)
- [ ] Token refresh optimization (refresh before expiry)
- [ ] Error boundary integration
- [ ] Loading states and skeletons
- [ ] Accessibility improvements (ARIA, keyboard nav)
- [ ] SDK documentation (README, API reference)

---

### v0.8.0 - Dashboard MVP
**Theme:** "Admin Can Manage"  
**Duration:** ~1.5 weeks  
**Demo:** Login to dashboard, switch org/project, view users

#### Application Structure
```
frontend/apps/dashboard/
├── package.json
├── vite.config.ts
├── src/
│   ├── main.tsx
│   ├── App.tsx
│   ├── routes/
│   │   ├── auth/
│   │   │   ├── login.tsx
│   │   │   └── mfa.tsx
│   │   ├── org/
│   │   │   ├── [orgId]/
│   │   │   │   ├── overview.tsx
│   │   │   │   ├── members.tsx
│   │   │   │   └── projects/
│   │   │   │       └── [projectId]/
│   │   │   │           ├── overview.tsx
│   │   │   │           ├── users.tsx
│   │   │   │           ├── roles.tsx
│   │   │   │           └── settings.tsx
│   ├── components/
│   │   ├── layout/
│   │   ├── org-selector/
│   │   └── project-selector/
│   └── lib/
│       └── api.ts
```

#### Pages (MVP Scope)
- [ ] Login page
- [ ] MFA challenge page
- [ ] Org selector (after login)
- [ ] Project selector
- [ ] Project overview (stats cards)
- [ ] Users list (paginated table)
- [ ] User detail page
- [ ] Roles list
- [ ] Role detail (permission assignment)
- [ ] Project settings (basic)

#### Technical Tasks
- [ ] Initialize Vite + React + TypeScript
- [ ] TanStack Router setup
- [ ] TanStack Query setup
- [ ] Tailwind CSS + shadcn/ui components
- [ ] Dashboard consumes @auctoritas/react SDK
- [ ] Organization/Project context management
- [ ] Protected routes with permission checks
- [ ] Responsive layout (sidebar + content)

---

### v0.8.1 - Dashboard Complete
**Theme:** "Full Admin Experience"  
**Duration:** ~1 week

#### Additional Pages
- [ ] API Keys management page
- [ ] Webhooks management page
- [ ] Audit logs viewer
- [ ] Organization settings
- [ ] Team members (invite, remove)
- [ ] Profile settings
- [ ] Security settings (change password, MFA)

#### Technical Tasks
- [ ] Invite org member flow
- [ ] API key creation (copy-once UI)
- [ ] Webhook test delivery
- [ ] Audit log filtering
- [ ] Real-time session list updates

---

### v0.9.0 - Worker Service
**Theme:** "Async Processing Works"  
**Duration:** ~1 week  
**Demo:** Registration triggers welcome email

#### Worker Handlers
```java
@RabbitListener(queues = "email.queue")
public class EmailHandler {
    void handleUserRegistered(UserRegisteredEvent event);
    void handlePasswordResetRequested(PasswordResetEvent event);
    void handleNewDeviceLogin(NewDeviceLoginEvent event);
}

@RabbitListener(queues = "webhook.queue")  
public class WebhookHandler {
    void handleEvent(AuthEvent event);
}
```

#### Technical Tasks
- [ ] RabbitMQ consumer configuration
- [ ] Resend SDK integration
- [ ] Email templates (HTML + plain text):
  - Welcome email
  - Email verification
  - Password reset
  - New device login alert
  - MFA enabled notification
- [ ] Webhook delivery system
- [ ] Webhook retry logic (exponential backoff)
- [ ] Webhook signature generation (HMAC-SHA256)
- [ ] Dead letter queue handling
- [ ] Audit log event handler

---

### v0.9.1 - Testing & Quality
**Theme:** "Confidence to Ship"  
**Duration:** ~1 week

#### Testing Scope

**Backend Tests**
- [ ] Unit tests for all services (80%+ coverage)
- [ ] Integration tests for auth flows
- [ ] API contract tests
- [ ] Security tests (OWASP basics)

**Frontend Tests**
- [ ] SDK unit tests (hooks, components)
- [ ] Dashboard component tests
- [ ] Playwright E2E tests:
  - User registration flow
  - Login + MFA flow
  - OAuth login flow
  - Role assignment flow
  - Session management flow

**Technical Tasks**
- [ ] Playwright setup in CI
- [ ] Test data factories
- [ ] Auth fixtures (pre-authenticated state)
- [ ] Load testing (basic, k6)
- [ ] Fix bugs from testing

---

### v0.10.0 - Deployment Preparation
**Theme:** "Ready for Production"  
**Duration:** ~3-4 days

#### Infrastructure

```
infra/
├── k8s/
│   ├── base/
│   │   ├── namespace.yaml
│   │   ├── gateway-deployment.yaml
│   │   ├── auth-deployment.yaml
│   │   ├── worker-deployment.yaml
│   │   ├── postgres-statefulset.yaml
│   │   ├── redis-statefulset.yaml
│   │   ├── rabbitmq-statefulset.yaml
│   │   └── ingress.yaml
│   └── overlays/
│       ├── dev/
│       └── prod/
└── scripts/
    ├── setup-k3s.sh
    └── deploy.sh
```

#### Technical Tasks
- [ ] Kubernetes manifests for all services
- [ ] Secrets management (k8s secrets)
- [ ] ConfigMaps for environment config
- [ ] Resource limits and requests
- [ ] Liveness/readiness probes
- [ ] Horizontal Pod Autoscaler configs
- [ ] Ingress with TLS (cert-manager)
- [ ] GitHub Actions: build & push images

---

### v0.10.1 - Production Deployment
**Theme:** "Live on Internet"  
**Duration:** ~3-4 days

#### Technical Tasks
- [ ] Provision Hetzner server (CX22)
- [ ] Install k3s
- [ ] Configure DNS records:
  - api.auctoritas.dev
  - dashboard.auctoritas.dev
- [ ] Deploy cert-manager + Let's Encrypt
- [ ] Deploy infrastructure (Postgres, Redis, RabbitMQ)
- [ ] Deploy application services
- [ ] Deploy dashboard (static files via Traefik)
- [ ] Verify all endpoints work
- [ ] Monitor logs and health

---

## v1.0.0 - Production Release 🎉
**Theme:** "Phase 1 Complete"  
**Duration:** ~1 week (polish + docs)

### Final Checklist

**Functionality**
- [ ] Organization registration & management
- [ ] Project creation & configuration
- [ ] End user authentication (email/password)
- [ ] OAuth (Google, GitHub, Apple, Microsoft, Facebook)
- [ ] MFA (TOTP) for org members and end users
- [ ] RBAC with custom roles and permissions
- [ ] Session management
- [ ] Password reset flow
- [ ] Email verification
- [ ] Webhooks

**SDK**
- [ ] Published to npm: `@auctoritas/react`
- [ ] All components documented in Storybook
- [ ] TypeScript types exported
- [ ] README with examples

**Dashboard**
- [ ] All pages functional
- [ ] Responsive design
- [ ] Accessible (keyboard + screen reader)

**Infrastructure**
- [ ] Production deployed on Hetzner/k3s
- [ ] TLS everywhere
- [ ] Monitoring (structured logs)
- [ ] Backups configured

**Documentation**
- [ ] OpenAPI spec published
- [ ] SDK documentation
- [ ] Self-hosting guide
- [ ] Architecture documentation
- [ ] README with quick start

**Quality**
- [ ] >80% test coverage
- [ ] E2E tests passing
- [ ] No critical security issues
- [ ] Performance acceptable (<500ms p95)

---

## Release Timeline Summary

| Version | Theme | Duration | Cumulative |
|---------|-------|----------|------------|
| v0.1.0 | Project Foundation | 1 week | Week 1 |
| v0.1.1 | Database Foundation | 3-4 days | Week 1.5 |
| v0.2.0 | Organization Registration | 1 week | Week 2.5 |
| v0.2.1 | Project Management | 4-5 days | Week 3 |
| v0.3.0 | End User Authentication | 1 week | Week 4 |
| v0.3.1 | Password Reset | 3-4 days | Week 4.5 |
| v0.3.2 | Email Verification | 2-3 days | Week 5 |
| v0.4.0 | OAuth (Google) | 1 week | Week 6 |
| v0.4.1 | Additional OAuth | 4-5 days | Week 6.5 |
| v0.4.2 | MFA (TOTP) | 1 week | Week 7.5 |
| v0.5.0 | RBAC System | 1 week | Week 8.5 |
| v0.5.1 | Session Management | 4-5 days | Week 9 |
| v0.5.2 | User Management | 3-4 days | Week 9.5 |
| v0.6.0 | SDK Foundation | 1 week | Week 10.5 |
| v0.7.0 | SDK Components | 1 week | Week 11.5 |
| v0.7.1 | SDK Advanced | 4-5 days | Week 12 |
| v0.8.0 | Dashboard MVP | 1.5 weeks | Week 13.5 |
| v0.8.1 | Dashboard Complete | 1 week | Week 14.5 |
| v0.9.0 | Worker Service | 1 week | Week 15.5 |
| v0.9.1 | Testing & Quality | 1 week | Week 16.5 |
| v0.10.0 | Deployment Prep | 3-4 days | Week 17 |
| v0.10.1 | Production Deploy | 3-4 days | Week 17.5 |
| v1.0.0 | Polish & Release | 1 week | Week 18.5 |

**Total: ~18-19 weeks** (with buffer for issues)

---

## Git Tagging Strategy

```bash
# Feature development
git checkout -b feature/v0.2.0-org-registration
# ... work ...
git checkout main
git merge feature/v0.2.0-org-registration

# Tag release
git tag -a v0.2.0 -m "Organization registration"
git push origin v0.2.0

# Patch for bugs in release
git checkout -b hotfix/v0.2.1-login-bug
# ... fix ...
git tag -a v0.2.1 -m "Fix login redirect issue"
```

---

## Version Dependencies

```
v0.1.0 ─┬─> v0.1.1 (DB)
        │
        └─> v0.2.0 (Org Auth) ──> v0.2.1 (Projects)
                                      │
                                      v
                               v0.3.0 (End User Auth) ─┬─> v0.3.1 (Reset)
                                                       └─> v0.3.2 (Verify)
                                                              │
                                                              v
                                                       v0.4.0 (OAuth) ──> v0.4.1 (More OAuth)
                                                                               │
                                                                               v
                                                                        v0.4.2 (MFA)
                                                                               │
                                                                               v
                                                                        v0.5.0 (RBAC) ─┬─> v0.5.1 (Sessions)
                                                                                        └─> v0.5.2 (User Mgmt)
                                                                                               │
                                      ┌────────────────────────────────────────────────────────┘
                                      v
                               v0.6.0 (SDK Foundation) ──> v0.7.0 (Components) ──> v0.7.1 (Advanced)
                                                                                          │
                                                                                          v
                                                                                   v0.8.0 (Dashboard) ──> v0.8.1
                                                                                                              │
                                                                                                              v
                                                                                                       v0.9.0 (Worker)
                                                                                                              │
                                                                                                              v
                                                                                                       v0.9.1 (Testing)
                                                                                                              │
                                                                                                              v
                                                                                                       v0.10.0 (Infra)
                                                                                                              │
                                                                                                              v
                                                                                                       v0.10.1 (Deploy)
                                                                                                              │
                                                                                                              v
                                                                                                       v1.0.0 🎉
```

---

## Notes

1. **Flexibility**: Versions can be combined if progress is faster than expected (e.g., v0.3.0 + v0.3.1 in one sprint)

2. **MVP First**: Each version should be deployable and demonstrable, even if features are limited

3. **Testing Throughout**: Unit tests should be written alongside features, not only in v0.9.1

4. **Documentation**: Update README and API docs with each version, not just at the end

5. **Demo Videos**: Consider recording short demo videos at major milestones (v0.3.0, v0.5.0, v0.7.0, v0.8.0, v1.0.0)
