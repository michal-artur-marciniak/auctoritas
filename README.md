# Auctoritas

**Multi-tenant Authentication Platform as a Service**

Auctoritas is a comprehensive authentication and authorization platform that enables developers to integrate secure authentication into their applications via a React SDK, without building auth infrastructure from scratch. Similar to Auth0 or Clerk, it provides complete auth capabilities including email/password, OAuth, MFA, and role-based access control.

## Features

- **Email Authentication** - Registration, login, password reset, email verification
- **OAuth Integration** - Google, Apple, Facebook, Microsoft, GitHub
- **Multi-Factor Authentication** - TOTP-based 2FA with recovery codes
- **Role-Based Access Control** - Customizable roles and permissions per project
- **Multi-Tenancy** - Organizations with multiple projects, isolated user pools
- **React SDK** - Pre-built components and hooks for rapid integration
- **Admin Dashboard** - Web UI for organization and project management

## Architecture

Auctoritas follows a microservices architecture with clear separation of concerns:

```
auctoritas/
├── backend/                    # Java ecosystem (Maven)
│   ├── services/
│   │   ├── gateway-service/    # API Gateway (Spring Cloud Gateway)
│   │   ├── auth-service/       # Core auth logic (Spring Boot)
│   │   └── worker-service/     # Async processing (emails, webhooks)
│   └── libs/
│       └── common/             # Shared DTOs, exceptions, utilities
├── frontend/                   # JavaScript ecosystem (pnpm)
│   ├── apps/
│   │   └── dashboard/          # Admin dashboard (React)
│   └── packages/
│       └── sdk-react/          # React SDK (@auctoritas/react)
└── infra/                      # Infrastructure configs
    ├── docker/                 # Dockerfiles
    └── k8s/                    # Kubernetes manifests
```

### Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 21, Spring Boot 4.0+, Spring Security, Spring Cloud Gateway |
| Frontend | React 19, TypeScript, TanStack Query, Tailwind CSS |
| Database | PostgreSQL 18 |
| Cache | Redis 8.4 |
| Messaging | RabbitMQ 4.2 |
| Infrastructure | Docker, Kubernetes (k3s) |

For detailed architecture documentation, see [docs/Architecture.md](docs/Architecture.md).

## Roadmap

The project follows semantic versioning with phased releases:

| Version | Milestone | Status |
|---------|-----------|--------|
| v0.1.0 | Foundation & Infrastructure | Released |
| v0.1.1 | Database Foundation | Planned |
| v0.2.x | Organization & Project Management | Planned |
| v0.3.x | End User Authentication | Planned |
| v0.4.x | OAuth & MFA | Planned |
| v0.5.x | RBAC & Session Management | Planned |
| v0.6.x - v0.7.x | React SDK | Planned |
| v0.8.x | Dashboard MVP | Planned |
| v0.9.x | Worker Service & Testing | Planned |
| v1.0.0 | Production Release | Planned |

For detailed roadmap and version breakdown, see [docs/Roadmap.md](docs/Roadmap.md).

## Documentation

- [Architecture Guide](docs/Architecture.md) - Detailed system design and API specifications
- [Roadmap](docs/Roadmap.md) - Version milestones and implementation plan

## Project Structure

```
auctoritas/
├── backend/
│   ├── pom.xml                 # Maven parent POM
│   ├── services/
│   │   ├── gateway-service/    # API routing, rate limiting, JWT validation
│   │   ├── auth-service/       # Authentication, authorization, user management
│   │   └── worker-service/     # Email delivery, webhooks, async jobs
│   └── libs/
│       └── common/             # Shared library
├── frontend/
│   ├── package.json            # pnpm workspace root
│   ├── pnpm-workspace.yaml
│   ├── apps/
│   │   └── dashboard/          # Admin dashboard
│   └── packages/
│       └── sdk-react/          # React SDK
├── infra/
│   ├── docker/                 # Service Dockerfiles
│   └── k8s/                    # Kubernetes manifests
├── docs/
│   ├── Architecture.md         # System architecture
│   └── Roadmap.md              # Version roadmap
├── docker-compose.yml          # Local development
└── .github/
    └── workflows/              # CI/CD pipelines
```

## License

Auctoritas is licensed under the **Elastic License 2.0 (ELv2)**.

This means you can:
- Use Auctoritas for free in your own applications
- Modify and self-host for internal use
- Contribute to the project

You cannot:
- Provide Auctoritas as a managed service to third parties
- Remove or obscure licensing information

For commercial licensing inquiries: license@auctoritas.dev

See the [LICENSE](LICENSE) file for full terms and the [Elastic License 2.0](https://www.elastic.co/licensing/elastic-license) reference.
