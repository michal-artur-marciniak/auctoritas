# Auctoritas

A modern **Authentication-as-a-Service** platform with multi-tenant organization support and three-tier identity management.

## What is Auctoritas?

Auctoritas provides flexible authentication for applications that need to manage multiple organizations, each with their own users and projects. It separates authentication into three distinct layers:

- **Platform Admins**: Internal operators with cross-tenant access for support and management
- **Organization Members**: Customer team members (OWNER/ADMIN/MEMBER roles) managing their organization
- **End Users**: Application users scoped to specific project/environment pairs via API keys

## Architecture Overview

### Three-Tier Identity Model

```
┌─────────────────────────────────────────────────────────────┐
│                    PLATFORM ADMINS                          │
│         (Cross-tenant access, internal use)                 │
│              /api/platform/admin/**                         │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ manages
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    ORGANIZATIONS                            │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              ORGANIZATION MEMBERS                      │  │
│  │    (OWNER/ADMIN/MEMBER roles)                         │  │
│  │         /api/v1/customers/orgs/**                      │  │
│  │                                                      │  │
│  │  ┌───────────────────────────────────────────────┐  │  │
│  │  │                  PROJECTS                      │  │  │
│  │  │  ┌──────────────┐    ┌──────────────┐        │  │  │
│  │  │  │   PROD       │    │     DEV      │        │  │  │
│  │  │  │ Environment  │    │ Environment  │        │  │  │
│  │  │  └──────────────┘    └──────────────┘        │  │  │
│  │  └───────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ API Keys
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    END USERS (SDK)                          │
│        (Scoped to project/environment via API keys)         │
│              /api/v1/end-users/**                           │
└─────────────────────────────────────────────────────────────┘
```

### Authentication Flows

Three isolated authentication flows ensure proper access control:

| Flow | Endpoints | Token Type | Use Case |
|------|-----------|------------|----------|
| **Platform Admin** | `/api/platform/**` | Platform JWT | Internal platform management |
| **Organization Member** | `/api/v1/customers/**` | Org JWT | Customer organization management |
| **SDK End User** | `/api/v1/end-users/**` | SDK JWT + API Key | Application end-user authentication |

**Security**: Each flow uses separate JWT types. Tokens from one flow cannot access endpoints from another.

### Technology Stack

**API** (`apps/api`): Spring Boot 4.0.2, Java 25, Spring Security 6.5, SQLite, Gradle

**Frontend** (`apps/app`): React 19.2, Vite, TypeScript

**Marketing** (`apps/www`): Astro 5.16.6, Tailwind CSS

**Architecture**: Pragmatic DDD with clean layers (Domain, Application, Infrastructure, Presentation)

## Project Structure

```
.
├── apps/
│   ├── api/          # Spring Boot API (Java 25, JWT Auth, SQLite)
│   ├── app/          # React + Vite frontend
│   └── www/          # Astro static marketing site
├── docs/
│   └── API.md        # Full API documentation
└── package.json
```

## Quick Start

```bash
# Install dependencies
pnpm install

# Run all apps in development mode
pnpm dev
```

Individual apps:
```bash
cd apps/api && ./gradlew bootRun    # API on :8080
cd apps/app && pnpm dev             # React app on :5173
cd apps/www && pnpm dev             # Marketing site on :4321
```

## Documentation

- **[API Reference](./docs/API.md)** - Complete endpoint documentation with examples

## Key Features

- **Multi-Tenancy**: Organizations with complete data isolation
- **Role-Based Access**: OWNER/ADMIN/MEMBER roles with granular permissions
- **API Key Management**: Environment-scoped keys with rotation support
- **OAuth Support**: GitHub and Google login for all user types
- **Impersonation**: Platform admins can troubleshoot as customers
- **Session Management**: Full CRUD for end-user sessions
- **Security**: httpOnly cookies, BCrypt passwords, project-level isolation

## License

MIT
