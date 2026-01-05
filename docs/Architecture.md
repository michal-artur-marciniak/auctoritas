# Auctoritas Auth Platform

## Phase 1: Architecture & Implementation Guide

**Project:** Auctoritas (auctoritas.dev)  
**Version:** 2.0  
**Phase:** 1 - Auth Platform (No Web3)  
**Last Updated:** December 2024  
**Document Type:** Learning & Reference Guide  

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Architecture Overview](#2-architecture-overview)
3. [Service Decomposition](#3-service-decomposition)
4. [API Specification](#4-api-specification)
5. [Data Architecture](#5-data-architecture)
6. [Authentication Flows](#6-authentication-flows)
7. [RBAC Model](#7-rbac-model)
8. [Organization & Project Configuration](#8-organization--project-configuration)
9. [Multi-Tenancy](#9-multi-tenancy)
10. [Event System](#10-event-system)
11. [React SDK](#11-react-sdk)
12. [Dashboard Application](#12-dashboard-application)
13. [Security Architecture](#13-security-architecture)
14. [Infrastructure & Deployment](#14-infrastructure--deployment)
15. [Monorepo Structure](#15-monorepo-structure)
16. [Implementation Roadmap](#16-implementation-roadmap)
17. [Appendix](#17-appendix)

---

## 1. Executive Summary

### 1.1 Project Vision

Auctoritas Phase 1 is a multi-tenant authentication platform providing Auth-as-a-Service capabilities similar to Auth0 or Clerk. It enables developers to integrate secure authentication into their applications via a React SDK, without building auth infrastructure from scratch.

Phase 2 will extend this foundation with Web3 capabilities (blockchain wallets, transaction signing, SIWE).

### 1.2 Core Capabilities

| Capability | Description |
|------------|-------------|
| **Email Auth** | Registration, login, password reset, email verification |
| **OAuth** | Google, Apple, Facebook, Microsoft, GitHub |
| **MFA** | TOTP-based two-factor authentication with recovery codes |
| **RBAC** | Roles, permissions, fine-grained access control (per project) |
| **Organizations** | Multi-project management, team collaboration, org-level roles |
| **Projects** | Isolated environments with independent configs, users, and API keys |
| **React SDK** | Pre-built components and hooks for rapid integration |
| **Dashboard** | Admin UI for organization/project management |

### 1.3 Architecture Summary

| Aspect | Choice |
|--------|--------|
| **Services** | 3 microservices (Gateway, Auth, Worker) |
| **Messaging** | RabbitMQ (event-driven) |
| **Database** | PostgreSQL (organization -> project hierarchy) |
| **Cache** | Redis (sessions, rate limiting) |
| **Email** | Resend |
| **Orchestration** | k3s (lightweight Kubernetes) |
| **Frontend** | React + TypeScript + Tailwind + shadcn/ui |
| **Hierarchy** | Organization -> Projects -> End Users |

### 1.4 Tech Stack Overview

```
+-----------------------------------------------------------------------------+
|                              TECH STACK                                      |
+-----------------------------------------------------------------------------+
|                                                                              |
|  BACKEND                           FRONTEND                                  |
|  -----------------------------     -----------------------------             |
|  * Java 21                         * React 19                                |
|  * Spring Boot 4.0+                * TypeScript                              |
|  * Spring Security                 * TanStack Query (React Query)            |
|  * Spring AMQP                     * Tailwind CSS + shadcn/ui                |
|  * Spring WebSocket                * Storybook (component docs)              |
|  * Maven (multi-module)            * Playwright (E2E testing)                |
|                                                                              |
|  INFRASTRUCTURE                    DATA                                      |
|  -----------------------------     -----------------------------             |
|  * k3s (Kubernetes)                * PostgreSQL 18                           |
|  * Docker                          * Redis 8.4                               |
|  * Traefik Ingress                 * RabbitMQ 4.2                            |
|  * cert-manager                                                              |
|  * Hetzner Cloud                                                             |
|                                                                              |
+-----------------------------------------------------------------------------+
```

### 1.5 Organization -> Projects Model

This architecture uses a **three-level hierarchy** that aligns with industry standards (Auth0, Clerk, Firebase):

```
+-----------------------------------------------------------------------------+
|                     ORGANIZATION -> PROJECTS MODEL                           |
+-----------------------------------------------------------------------------+
|                                                                              |
|  Organization (billing entity, dashboard access)                             |
|    |                                                                         |
|    +-- Organization Members (developers who manage projects)                 |
|    |     +-- owner@company.com (OWNER)                                       |
|    |     +-- lead@company.com (ADMIN)                                        |
|    |     +-- dev@company.com (MEMBER)                                        |
|    |                                                                         |
|    +-- Project: "Production Web App"                                         |
|    |     +-- project_settings (password policy, OAuth config, MFA)           |
|    |     +-- end_users (customers of the app)                                |
|    |     +-- roles & permissions (for end_users)                             |
|    |     +-- api_keys                                                        |
|    |     +-- webhooks                                                        |
|    |                                                                         |
|    +-- Project: "Staging Environment"                                        |
|    |     +-- ... (isolated configuration)                                    |
|    |                                                                         |
|    +-- Project: "Mobile App"                                                 |
|          +-- ... (different rules, same or different user pool)              |
|                                                                              |
+-----------------------------------------------------------------------------+

TWO TYPES OF USERS:
===================

1. ORGANIZATION MEMBERS (Platform Users)
   * Developers/admins who use the Auctoritas dashboard
   * Authenticate to dashboard.auctoritas.dev
   * Manage projects, view analytics, configure settings
   * Have org-level roles: OWNER, ADMIN, MEMBER

2. END USERS (Project Users)  
   * Customers of apps built by organization members
   * Authenticate via SDK/API to specific projects
   * Subject to project-specific rules (password policy, MFA, OAuth)
   * Have project-level RBAC roles (custom per project)
```

---

## 2. Architecture Overview

### 2.1 High-Level Architecture

```
+-----------------------------------------------------------------------------+
|                                 CLIENTS                                      |
|                                                                              |
|    +-----------------+    +-----------------+    +-----------------+         |
|    |    Dashboard    |    |   React SDK     |    |   Direct API    |         |
|    |  (Org Members)  |    | @auctoritas/react    |    Consumers    |         |
|    |                 |    |  (End Users)    |    |   (End Users)   |         |
|    +--------+--------+    +--------+--------+    +--------+--------+         |
|             |                      |                      |                  |
+-------------+----------------------+----------------------+------------------+
              |                      |                      |
              +----------------------+----------------------+
                                     |
                                     v
+-----------------------------------------------------------------------------+
|                            INGRESS (Traefik)                                 |
|                      TLS Termination * Path Routing                          |
|                                                                              |
|    api.auctoritas.dev ------> API Gateway                                    |
|    dashboard.auctoritas.dev > Dashboard (Static)                             |
+-----------------------------------------------------------------------------+
                                     |
                                     v
+-----------------------------------------------------------------------------+
|                              API GATEWAY                                     |
|                                                                              |
|    +-------------+ +-------------+ +-------------+ +-------------+           |
|    |   Routing   | | JWT Verify  | | Rate Limit  | |    CORS     |           |
|    +-------------+ +-------------+ +-------------+ +-------------+           |
|                                                                              |
|    Routes:                                                                   |
|    * /api/v1/org/**      -> Org member operations (dashboard)                |
|    * /api/v1/auth/**     -> End user auth (SDK)                              |
|    * /api/v1/users/**    -> End user management                              |
|    * /api/v1/projects/** -> Project config (dashboard)                       |
+-----------------------------------------------------------------------------+
                                     |
                                     v
+-----------------------------------------------------------------------------+
|                             AUTH SERVICE                                     |
|                                                                              |
|    +--------------------------------------------------------------------+    |
|    |                                                                    |    |
|    |  ORGANIZATION DOMAIN              PROJECT DOMAIN                   |    |
|    |  ---------------------            ----------------------           |    |
|    |  * Org registration               * End user registration          |    |
|    |  * Org member login               * End user login                 |    |
|    |  * Org member MFA                 * End user OAuth                 |    |
|    |  * Project management             * End user MFA                   |    |
|    |  * API key management             * End user RBAC                  |    |
|    |  * Org-level RBAC                 * Sessions                       |    |
|    |                                   * Profiles                       |    |
|    |                                                                    |    |
|    +--------------------------------------------------------------------+    |
|                                                                              |
+-----------------------+---------------------------------+--------------------+
                        |                                 |
                        v                                 v
              +-----------------+            +-----------------+
              |   PostgreSQL    |            |    RabbitMQ     |
              |    auth_db      |            |                 |
              +-----------------+            +--------+--------+
                                                      |
              +-----------------+                      |
              |     Redis       |                      |
              |                 |                      v
              | * Sessions      |            +-----------------+
              | * Rate limits   |            |  WORKER SERVICE |
              +-----------------+            |                 |
                                             | * Emails        |
                                             | * Webhooks      |
                                             | * Cleanup jobs  |
                                             +-----------------+
```

### 2.2 Request Flow

```
+-----------------------------------------------------------------------------+
|                           REQUEST FLOW                                       |
+-----------------------------------------------------------------------------+
|                                                                              |
|  1. End User SDK Request (with API Key)                                      |
|  ----------------------------------------                                    |
|                                                                              |
|  [SDK] ---> [Traefik] ---> [Gateway] ---> [Auth Service] ---> [PostgreSQL]   |
|    |           |              |                |                             |
|    |     TLS termination  Validate API     Process request                   |
|    |                      Key + JWT        Emit events                       |
|    |                      Rate limit                |                        |
|    |                                                v                        |
|    |                                          [RabbitMQ]                     |
|    |                                                |                        |
|    |                                                v                        |
|    |                                          [Worker]                       |
|    |                                          Send email                     |
|    |                                          Deliver webhook                |
|    <------ JWT Response --------<                                            |
|                                                                              |
|                                                                              |
|  2. Org Member Dashboard Request                                             |
|  ----------------------------------------                                    |
|                                                                              |
|  [Dashboard] ---> [Traefik] ---> [Gateway] ---> [Auth Service]               |
|       |               |              |                |                      |
|       |         TLS termination  Validate org     Process request            |
|       |                          member JWT       Return data                |
|       |                                                                      |
|       <------ Response --------<                                             |
|                                                                              |
+-----------------------------------------------------------------------------+
```

---

## 3. Service Decomposition

### 3.1 Services Overview

```
+-----------------------------------------------------------------------------+
|                          SERVICES OVERVIEW                                   |
+-----------------------------------------------------------------------------+
|                                                                              |
|  +------------------+  +------------------+  +------------------+             |
|  |   API Gateway    |  |   Auth Service   |  |  Worker Service  |            |
|  |                  |  |                  |  |                  |            |
|  | Spring Cloud GW  |  |   Spring Boot    |  |   Spring Boot    |            |
|  | Non-blocking     |  |   JPA/Hibernate  |  |   AMQP Consumer  |            |
|  |                  |  |   Spring Sec     |  |   Resend SDK     |            |
|  +------------------+  +------------------+  +------------------+             |
|         |                     |                      |                       |
|         v                     v                      v                       |
|    Route requests       Business logic         Async processing              |
|    JWT validation       Data access            Email delivery                |
|    Rate limiting        Event publishing       Webhook delivery              |
|    CORS handling        Session mgmt           Scheduled cleanup             |
|                                                                              |
+-----------------------------------------------------------------------------+
```

### 3.2 API Gateway

**Purpose:** Single entry point for all API requests

**Responsibilities:**
- Route requests to appropriate services
- Validate JWT tokens (both org member and end user)
- Validate API keys for SDK requests
- Apply rate limiting (Redis-based)
- Handle CORS
- Add correlation IDs for tracing

**Technology:** Spring Cloud Gateway (reactive, non-blocking)

**Configuration:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        # Org member routes (dashboard)
        - id: org-auth
          uri: lb://auth-service
          predicates:
            - Path=/api/v1/org/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100
                redis-rate-limiter.burstCapacity: 200
                
        # End user routes (SDK)
        - id: end-user-auth
          uri: lb://auth-service
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 50
                redis-rate-limiter.burstCapacity: 100
```

### 3.3 Auth Service

**Purpose:** Core authentication and authorization logic

**Responsibilities:**
- Organization registration and management
- Organization member authentication
- End user registration, login, password management
- OAuth provider integration
- MFA (TOTP) management
- Session management (for both user types)
- RBAC (roles and permissions)
- Project settings management
- API key management
- Emit events for async processing

**Technology:** Spring Boot + Spring Security + Spring Data JPA

**Internal Modules:**
```
auth-service/
  +-- organization/       # Org registration, member management
  +-- project/            # Project CRUD, settings, API keys
  +-- enduser/            # End user auth, registration, profiles
  +-- oauth/              # OAuth provider integration
  +-- mfa/                # TOTP setup, verification, recovery
  +-- rbac/               # Roles, permissions, assignments
  +-- session/            # Session management for both user types
  +-- common/             # Shared utilities, exceptions, events
```

### 3.4 Worker Service

**Purpose:** Asynchronous processing of background tasks

**Responsibilities:**
- Email delivery (via Resend)
- Webhook delivery with retries
- Scheduled cleanup (expired sessions, tokens)
- Event consumption from RabbitMQ

**Technology:** Spring Boot + Spring AMQP + Resend SDK

**Event Handlers:**
```
worker-service/
  +-- email/
  |     +-- WelcomeEmailHandler
  |     +-- VerificationEmailHandler
  |     +-- PasswordResetEmailHandler
  |     +-- NewDeviceAlertHandler
  +-- webhook/
  |     +-- WebhookDeliveryHandler
  |     +-- WebhookRetryHandler
  +-- cleanup/
        +-- ExpiredSessionCleanup
        +-- ExpiredTokenCleanup
```

---

## 4. API Specification

### 4.1 Organization Endpoints

```
+-----------------------------------------------------------------------------+
| ORGANIZATION REGISTRATION                                          [Public] |
+-----------------------------------------------------------------------------+
|                                                                              |
| POST   /api/v1/org/register                                                  |
|        Create new organization with owner account                            |
|        Body: { orgName, slug, ownerEmail, ownerPassword, ownerName }         |
|        Response: {                                                           |
|          organization: { id, name, slug },                                   |
|          member: { id, email, role: "OWNER" },                               |
|          accessToken, refreshToken                                           |
|        }                                                                     |
|                                                                              |
| GET    /api/v1/org/check-slug?slug=acme                                      |
|        Check if organization slug is available                               |
|        Response: { available: true }                                         |
|                                                                              |
+-----------------------------------------------------------------------------+

+-----------------------------------------------------------------------------+
| ORGANIZATION MEMBER AUTH                                           [Public] |
+-----------------------------------------------------------------------------+
|                                                                              |
| POST   /api/v1/org/auth/login                                                |
|        Organization member login (dashboard access)                          |
|        Body: { email, password }                                             |
|        Response: { accessToken, refreshToken, member, organizations[] }      |
|                  OR { mfaRequired: true, mfaToken }                          |
|                                                                              |
| POST   /api/v1/org/auth/logout                                               |
|        Invalidate org member session                                         |
|        Response: { message: "Logged out" }                                   |
|                                                                              |
| POST   /api/v1/org/auth/refresh                                              |
|        Refresh org member tokens                                             |
|        Body: { refreshToken }                                                |
|        Response: { accessToken, refreshToken }                               |
|                                                                              |
| POST   /api/v1/org/auth/mfa/verify                                           |
|        Complete MFA for org member login                                     |
|        Body: { mfaToken, code }                                              |
|        Response: { accessToken, refreshToken, member }                       |
|                                                                              |
+-----------------------------------------------------------------------------+

+-----------------------------------------------------------------------------+
| ORGANIZATION MANAGEMENT                                  [Org Member Auth]  |
+-----------------------------------------------------------------------------+
|                                                                              |
| GET    /api/v1/org/{orgId}                                                   |
|        Get organization details                                              |
|        Response: { organization, memberCount, projectCount }                 |
|                                                                              |
| PUT    /api/v1/org/{orgId}                                                   |
|        Update organization (OWNER/ADMIN only)                                |
|        Body: { name }                                                        |
|        Response: { organization }                                            |
|                                                                              |
| GET    /api/v1/org/{orgId}/members                                           |
|        List organization members                                             |
|        Response: [{ id, email, name, role, status }]                         |
|                                                                              |
| POST   /api/v1/org/{orgId}/members/invite                                    |
|        Invite new member (OWNER/ADMIN only)                                  |
|        Body: { email, role }                                                 |
|        Response: { invitation }                                              |
|                                                                              |
| PUT    /api/v1/org/{orgId}/members/{memberId}/role                           |
|        Change member role (OWNER only)                                       |
|        Body: { role }                                                        |
|        Response: { member }                                                  |
|                                                                              |
| DELETE /api/v1/org/{orgId}/members/{memberId}                                |
|        Remove member (OWNER/ADMIN only)                                      |
|        Response: { message: "Member removed" }                               |
|                                                                              |
+-----------------------------------------------------------------------------+
```

### 4.2 Project Endpoints

```
+-----------------------------------------------------------------------------+
| PROJECT MANAGEMENT                                       [Org Member Auth]  |
+-----------------------------------------------------------------------------+
|                                                                              |
| GET    /api/v1/org/{orgId}/projects                                          |
|        List all projects in organization                                     |
|        Response: [{ id, name, slug, status, userCount }]                     |
|                                                                              |
| POST   /api/v1/org/{orgId}/projects                                          |
|        Create new project                                                    |
|        Body: { name, slug }                                                  |
|        Response: { project, defaultApiKey }                                  |
|                                                                              |
| GET    /api/v1/org/{orgId}/projects/{projectId}                              |
|        Get project details                                                   |
|        Response: { project, settings, stats }                                |
|                                                                              |
| PUT    /api/v1/org/{orgId}/projects/{projectId}                              |
|        Update project                                                        |
|        Body: { name }                                                        |
|        Response: { project }                                                 |
|                                                                              |
| DELETE /api/v1/org/{orgId}/projects/{projectId}                              |
|        Delete project (OWNER/ADMIN only)                                     |
|        Response: { message: "Project deleted" }                              |
|                                                                              |
+-----------------------------------------------------------------------------+

+-----------------------------------------------------------------------------+
| PROJECT SETTINGS                                         [Org Member Auth]  |
+-----------------------------------------------------------------------------+
|                                                                              |
| GET    /api/v1/org/{orgId}/projects/{projectId}/settings                     |
|        Get all project settings                                              |
|        Response: { passwordPolicy, mfaSettings, oauthProviders, ... }        |
|                                                                              |
| PUT    /api/v1/org/{orgId}/projects/{projectId}/settings/password            |
|        Update password policy                                                |
|        Body: { minLength, requireUppercase, requireNumbers, ... }            |
|        Response: { passwordPolicy }                                          |
|                                                                              |
| PUT    /api/v1/org/{orgId}/projects/{projectId}/settings/mfa                 |
|        Update MFA settings                                                   |
|        Body: { enabled, required, allowedMethods }                           |
|        Response: { mfaSettings }                                             |
|                                                                              |
| PUT    /api/v1/org/{orgId}/projects/{projectId}/settings/oauth               |
|        Update OAuth providers                                                |
|        Body: { google: { enabled, clientId, clientSecret }, ... }            |
|        Response: { oauthProviders }                                          |
|                                                                              |
| PUT    /api/v1/org/{orgId}/projects/{projectId}/settings/session             |
|        Update session settings                                               |
|        Body: { accessTokenTtl, refreshTokenTtl, maxSessions }                |
|        Response: { sessionSettings }                                         |
|                                                                              |
+-----------------------------------------------------------------------------+

+-----------------------------------------------------------------------------+
| API KEYS                                                 [Org Member Auth]  |
+-----------------------------------------------------------------------------+
|                                                                              |
| GET    /api/v1/org/{orgId}/projects/{projectId}/api-keys                     |
|        List API keys                                                         |
|        Response: [{ id, name, prefix, lastUsedAt, createdAt }]               |
|                                                                              |
| POST   /api/v1/org/{orgId}/projects/{projectId}/api-keys                     |
|        Create API key (key shown only once!)                                 |
|        Body: { name }                                                        |
|        Response: { apiKey: { id, name, key } }                               |
|                                                                              |
| DELETE /api/v1/org/{orgId}/projects/{projectId}/api-keys/{keyId}             |
|        Revoke API key                                                        |
|        Response: { message: "API key revoked" }                              |
|                                                                              |
+-----------------------------------------------------------------------------+
```

### 4.3 End User Auth Endpoints (SDK/API)

```
+-----------------------------------------------------------------------------+
| END USER REGISTRATION                        [API Key Required - X-API-Key] |
+-----------------------------------------------------------------------------+
|                                                                              |
| POST   /api/v1/auth/register                                                 |
|        Register new end user                                                 |
|        Headers: X-API-Key: pk_live_xxx                                       |
|        Body: { email, password, name? }                                      |
|        Response: { user, accessToken, refreshToken }                         |
|                                                                              |
| POST   /api/v1/auth/register/verify-email                                    |
|        Verify email with code                                                |
|        Headers: X-API-Key: pk_live_xxx                                       |
|        Body: { token, code }                                                 |
|        Response: { verified: true }                                          |
|                                                                              |
| POST   /api/v1/auth/register/resend-verification                             |
|        Resend verification email                                             |
|        Headers: X-API-Key: pk_live_xxx                                       |
|        Body: { email }                                                       |
|        Response: { message: "Verification email sent" }                      |
|                                                                              |
+-----------------------------------------------------------------------------+

+-----------------------------------------------------------------------------+
| END USER LOGIN                                           [API Key Required] |
+-----------------------------------------------------------------------------+
|                                                                              |
| POST   /api/v1/auth/login                                                    |
|        Login with email/password                                             |
|        Headers: X-API-Key: pk_live_xxx                                       |
|        Body: { email, password }                                             |
|        Response: { user, accessToken, refreshToken }                         |
|                  OR { mfaRequired: true, mfaToken }                          |
|                                                                              |
| POST   /api/v1/auth/login/mfa                                                |
|        Complete MFA challenge                                                |
|        Headers: X-API-Key: pk_live_xxx                                       |
|        Body: { mfaToken, code }                                              |
|        Response: { user, accessToken, refreshToken }                         |
|                                                                              |
| POST   /api/v1/auth/logout                                                   |
|        Logout (invalidate session)                                           |
|        Headers: X-API-Key + Authorization: Bearer <token>                    |
|        Response: { message: "Logged out" }                                   |
|                                                                              |
| POST   /api/v1/auth/refresh                                                  |
|        Refresh tokens                                                        |
|        Headers: X-API-Key: pk_live_xxx                                       |
|        Body: { refreshToken }                                                |
|        Response: { accessToken, refreshToken }                               |
|                                                                              |
+-----------------------------------------------------------------------------+

+-----------------------------------------------------------------------------+
| END USER OAUTH                                           [API Key Required] |
+-----------------------------------------------------------------------------+
|                                                                              |
| GET    /api/v1/auth/oauth/{provider}                                         |
|        Initiate OAuth flow (redirect to provider)                            |
|        Headers: X-API-Key: pk_live_xxx                                       |
|        Query: redirect_uri                                                   |
|        Response: Redirect to OAuth provider                                  |
|                                                                              |
| GET    /api/v1/auth/oauth/{provider}/callback                                |
|        OAuth callback (internal, handles provider response)                  |
|        Query: code, state                                                    |
|        Response: Redirect to app with tokens                                 |
|                                                                              |
+-----------------------------------------------------------------------------+

+-----------------------------------------------------------------------------+
| END USER PASSWORD                                        [API Key Required] |
+-----------------------------------------------------------------------------+
|                                                                              |
| POST   /api/v1/auth/password/forgot                                          |
|        Request password reset                                                |
|        Headers: X-API-Key: pk_live_xxx                                       |
|        Body: { email }                                                       |
|        Response: { message: "Reset email sent" }                             |
|                                                                              |
| POST   /api/v1/auth/password/reset                                           |
|        Reset password with token                                             |
|        Headers: X-API-Key: pk_live_xxx                                       |
|        Body: { token, newPassword }                                          |
|        Response: { message: "Password updated" }                             |
|                                                                              |
+-----------------------------------------------------------------------------+

+-----------------------------------------------------------------------------+
| END USER PROFILE                              [API Key + End User JWT Auth] |
+-----------------------------------------------------------------------------+
|                                                                              |
| GET    /api/v1/users/me                                                      |
|        Get current user profile                                              |
|        Response: { user, profile, roles, permissions }                       |
|                                                                              |
| PUT    /api/v1/users/me                                                      |
|        Update profile                                                        |
|        Body: { name, avatarUrl, metadata }                                   |
|        Response: { user, profile }                                           |
|                                                                              |
| PUT    /api/v1/users/me/password                                             |
|        Change password (requires current password)                           |
|        Body: { currentPassword, newPassword }                                |
|        Response: { message: "Password updated" }                             |
|                                                                              |
| GET    /api/v1/users/me/sessions                                             |
|        List active sessions                                                  |
|        Response: [{ id, deviceInfo, ipAddress, createdAt, current }]         |
|                                                                              |
| DELETE /api/v1/users/me/sessions/{sessionId}                                 |
|        Revoke specific session                                               |
|        Response: { message: "Session revoked" }                              |
|                                                                              |
+-----------------------------------------------------------------------------+

+-----------------------------------------------------------------------------+
| END USER MFA                                  [API Key + End User JWT Auth] |
+-----------------------------------------------------------------------------+
|                                                                              |
| POST   /api/v1/users/me/mfa/setup                                            |
|        Start MFA setup (returns QR code)                                     |
|        Response: { secret, qrCodeUrl, backupCodes }                          |
|                                                                              |
| POST   /api/v1/users/me/mfa/verify                                           |
|        Complete MFA setup with verification code                             |
|        Body: { code }                                                        |
|        Response: { enabled: true }                                           |
|                                                                              |
| DELETE /api/v1/users/me/mfa                                                  |
|        Disable MFA                                                           |
|        Body: { code } (current MFA code)                                     |
|        Response: { message: "MFA disabled" }                                 |
|                                                                              |
| POST   /api/v1/users/me/mfa/recovery                                         |
|        Regenerate recovery codes                                             |
|        Body: { code }                                                        |
|        Response: { backupCodes }                                             |
|                                                                              |
+-----------------------------------------------------------------------------+
```

### 4.4 End User Admin Endpoints (Dashboard)

```
+-----------------------------------------------------------------------------+
| END USER ADMIN (Dashboard)                     [Org Member Auth + Project]  |
+-----------------------------------------------------------------------------+
|                                                                              |
| GET    /api/v1/org/{orgId}/projects/{projectId}/users                        |
|        List end users in project                                             |
|        Query: page, size, search, status, role                               |
|        Response: { users[], total, page, size }                              |
|                                                                              |
| GET    /api/v1/org/{orgId}/projects/{projectId}/users/{userId}               |
|        Get end user details                                                  |
|        Response: { user, profile, roles, sessions, oauthConnections }        |
|                                                                              |
| PUT    /api/v1/org/{orgId}/projects/{projectId}/users/{userId}/status        |
|        Update end user status                                                |
|        Body: { status: "ACTIVE" | "SUSPENDED" | "DELETED" }                  |
|        Response: { user }                                                    |
|                                                                              |
| PUT    /api/v1/org/{orgId}/projects/{projectId}/users/{userId}/roles         |
|        Assign roles to end user                                              |
|        Body: { roleIds: [] }                                                 |
|        Response: { user, roles }                                             |
|                                                                              |
| DELETE /api/v1/org/{orgId}/projects/{projectId}/users/{userId}/mfa           |
|        Force disable end user's MFA                                          |
|        Response: { message: "MFA disabled" }                                 |
|                                                                              |
| POST   /api/v1/org/{orgId}/projects/{projectId}/users/{userId}/sessions/revoke-all
|        Revoke all end user sessions                                          |
|        Response: { message: "All sessions revoked" }                         |
|                                                                              |
+-----------------------------------------------------------------------------+
```

### 4.5 RBAC Endpoints (Per Project)

```
+-----------------------------------------------------------------------------+
| ROLES & PERMISSIONS (Per Project)              [Org Member Auth + Project]  |
+-----------------------------------------------------------------------------+
|                                                                              |
| GET    /api/v1/org/{orgId}/projects/{projectId}/roles                        |
|        List all roles in project                                             |
|        Response: [{ id, name, description, isSystem, permissionCount }]      |
|                                                                              |
| POST   /api/v1/org/{orgId}/projects/{projectId}/roles                        |
|        Create new role                                                       |
|        Body: { name, description, permissionIds }                            |
|        Response: { role }                                                    |
|                                                                              |
| GET    /api/v1/org/{orgId}/projects/{projectId}/roles/{roleId}               |
|        Get role details                                                      |
|        Response: { role, permissions }                                       |
|                                                                              |
| PUT    /api/v1/org/{orgId}/projects/{projectId}/roles/{roleId}               |
|        Update role                                                           |
|        Body: { name, description }                                           |
|        Response: { role }                                                    |
|                                                                              |
| DELETE /api/v1/org/{orgId}/projects/{projectId}/roles/{roleId}               |
|        Delete role (not system roles)                                        |
|        Response: { message: "Role deleted" }                                 |
|                                                                              |
| PUT    /api/v1/org/{orgId}/projects/{projectId}/roles/{roleId}/permissions   |
|        Update role permissions                                               |
|        Body: { permissionIds: [] }                                           |
|        Response: { role, permissions }                                       |
|                                                                              |
| GET    /api/v1/permissions                                                   |
|        List all available permissions (global, not per-project)              |
|        Response: [{ id, code, description, category }]                       |
|                                                                              |
+-----------------------------------------------------------------------------+
```

### 4.6 Webhook Endpoints

```
+-----------------------------------------------------------------------------+
| WEBHOOKS (Per Project)                         [Org Member Auth + Project]  |
+-----------------------------------------------------------------------------+
|                                                                              |
| GET    /api/v1/org/{orgId}/projects/{projectId}/webhooks                     |
|        List configured webhooks                                              |
|        Response: [{ id, url, events, enabled }]                              |
|                                                                              |
| POST   /api/v1/org/{orgId}/projects/{projectId}/webhooks                     |
|        Create webhook endpoint                                               |
|        Body: { url, events, secret }                                         |
|        Response: { webhook }                                                 |
|                                                                              |
| PUT    /api/v1/org/{orgId}/projects/{projectId}/webhooks/{webhookId}         |
|        Update webhook                                                        |
|        Body: { url, events, enabled }                                        |
|        Response: { webhook }                                                 |
|                                                                              |
| DELETE /api/v1/org/{orgId}/projects/{projectId}/webhooks/{webhookId}         |
|        Delete webhook                                                        |
|        Response: { message: "Webhook deleted" }                              |
|                                                                              |
+-----------------------------------------------------------------------------+
```

---

## 5. Data Architecture

### 5.1 Database Overview

```
+-----------------------------------------------------------------------------+
|                         DATABASE: auth_db                                    |
+-----------------------------------------------------------------------------+
|                                                                              |
|  +-----------------------------------------------------------------------+   |
|  | ORGANIZATION TABLES (Platform Level)                                  |   |
|  |                                                                       |   |
|  |  organizations                                                        |   |
|  |  organization_members                                                 |   |
|  |  organization_invitations                                             |   |
|  |  org_member_mfa                                                       |   |
|  |  org_member_sessions                                                  |   |
|  +-----------------------------------------------------------------------+   |
|                                                                              |
|  +-----------------------------------------------------------------------+   |
|  | PROJECT TABLES                                                        |   |
|  |                                                                       |   |
|  |  projects                                                             |   |
|  |  project_settings                                                     |   |
|  |  api_keys                                                             |   |
|  +-----------------------------------------------------------------------+   |
|                                                                              |
|  +-----------------------------------------------------------------------+   |
|  | END USER TABLES (Per Project)                                         |   |
|  |                                                                       |   |
|  |  users                                                                |   |
|  |  user_profiles                                                        |   |
|  |  user_settings                                                        |   |
|  |  oauth_connections                                                    |   |
|  |  user_mfa                                                             |   |
|  |  password_history                                                     |   |
|  +-----------------------------------------------------------------------+   |
|                                                                              |
|  +-----------------------------------------------------------------------+   |
|  | END USER SESSION TABLES                                               |   |
|  |                                                                       |   |
|  |  sessions                                                             |   |
|  |  refresh_tokens                                                       |   |
|  +-----------------------------------------------------------------------+   |
|                                                                              |
|  +-----------------------------------------------------------------------+   |
|  | RBAC TABLES (Per Project)                                             |   |
|  |                                                                       |   |
|  |  roles                                                                |   |
|  |  permissions (global)                                                 |   |
|  |  role_permissions                                                     |   |
|  |  user_roles                                                           |   |
|  +-----------------------------------------------------------------------+   |
|                                                                              |
|  +-----------------------------------------------------------------------+   |
|  | WEBHOOK TABLES (Per Project)                                          |   |
|  |                                                                       |   |
|  |  webhook_endpoints                                                    |   |
|  |  webhook_deliveries                                                   |   |
|  +-----------------------------------------------------------------------+   |
|                                                                              |
|  +-----------------------------------------------------------------------+   |
|  | AUDIT TABLES                                                          |   |
|  |                                                                       |   |
|  |  audit_logs                                                           |   |
|  +-----------------------------------------------------------------------+   |
|                                                                              |
+-----------------------------------------------------------------------------+
```

### 5.2 Schema Definitions

```sql
-- ============================================================================
-- ORGANIZATION TABLES (Platform Level)
-- ============================================================================

CREATE TABLE organizations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(50) UNIQUE NOT NULL,
    status          VARCHAR(20) DEFAULT 'ACTIVE',  -- ACTIVE, SUSPENDED, DELETED
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_organizations_slug ON organizations(slug);

-- Organization members (developers who manage projects via dashboard)
CREATE TABLE organization_members (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    name            VARCHAR(100),
    avatar_url      VARCHAR(500),
    role            VARCHAR(20) NOT NULL,  -- OWNER, ADMIN, MEMBER
    email_verified  BOOLEAN DEFAULT FALSE,
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT uq_org_member_email UNIQUE(organization_id, email)
);

CREATE INDEX idx_org_members_org ON organization_members(organization_id);
CREATE INDEX idx_org_members_email ON organization_members(email);

-- Invitations for new org members
CREATE TABLE organization_invitations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    email           VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL,
    token           VARCHAR(255) UNIQUE NOT NULL,
    invited_by      UUID REFERENCES organization_members(id),
    expires_at      TIMESTAMP NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW()
);

-- MFA for org members
CREATE TABLE org_member_mfa (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id       UUID UNIQUE NOT NULL REFERENCES organization_members(id) ON DELETE CASCADE,
    secret          VARCHAR(255) NOT NULL,  -- Encrypted TOTP secret
    recovery_codes  TEXT[],                 -- Encrypted recovery codes
    enabled         BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT NOW()
);

-- Sessions for org members (dashboard sessions)
CREATE TABLE org_member_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id       UUID NOT NULL REFERENCES organization_members(id) ON DELETE CASCADE,
    device_info     JSONB,
    ip_address      INET,
    user_agent      TEXT,
    expires_at      TIMESTAMP NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_org_member_sessions_member ON org_member_sessions(member_id);
CREATE INDEX idx_org_member_sessions_expires ON org_member_sessions(expires_at);

-- ============================================================================
-- PROJECT TABLES
-- ============================================================================

CREATE TABLE projects (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(50) NOT NULL,
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT uq_project_org_slug UNIQUE(organization_id, slug)
);

CREATE INDEX idx_projects_org ON projects(organization_id);

CREATE TABLE project_settings (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id              UUID UNIQUE NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    
    -- Password policy
    password_min_length     INT DEFAULT 8,
    password_require_upper  BOOLEAN DEFAULT TRUE,
    password_require_lower  BOOLEAN DEFAULT TRUE,
    password_require_number BOOLEAN DEFAULT TRUE,
    password_require_special BOOLEAN DEFAULT FALSE,
    password_history_count  INT DEFAULT 0,
    
    -- MFA settings
    mfa_enabled             BOOLEAN DEFAULT TRUE,
    mfa_required            BOOLEAN DEFAULT FALSE,
    
    -- OAuth providers (JSONB for flexibility)
    oauth_google            JSONB,    -- { enabled, clientId, clientSecret }
    oauth_github            JSONB,
    oauth_microsoft         JSONB,
    oauth_apple             JSONB,
    oauth_facebook          JSONB,
    
    -- Session settings
    access_token_ttl        INT DEFAULT 900,       -- 15 minutes
    refresh_token_ttl       INT DEFAULT 604800,    -- 7 days
    max_sessions_per_user   INT DEFAULT 5,
    
    -- General settings
    allow_registration      BOOLEAN DEFAULT TRUE,
    require_email_verify    BOOLEAN DEFAULT TRUE,
    
    created_at              TIMESTAMP DEFAULT NOW(),
    updated_at              TIMESTAMP DEFAULT NOW()
);

CREATE TABLE api_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    key_hash        VARCHAR(255) UNIQUE NOT NULL,
    key_prefix      VARCHAR(20) NOT NULL,       -- e.g., "pk_live_abc"
    last_used_at    TIMESTAMP,
    expires_at      TIMESTAMP,                  -- NULL = never expires
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_api_keys_project ON api_keys(project_id);
CREATE INDEX idx_api_keys_prefix ON api_keys(key_prefix);

-- ============================================================================
-- END USER TABLES (Per Project)
-- ============================================================================

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255),               -- NULL if OAuth-only
    email_verified  BOOLEAN DEFAULT FALSE,
    status          VARCHAR(20) DEFAULT 'ACTIVE',
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT uq_users_project_email UNIQUE(project_id, email)
);

CREATE INDEX idx_users_project ON users(project_id);
CREATE INDEX idx_users_email ON users(project_id, email);

CREATE TABLE user_profiles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id         UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(100),
    avatar_url      VARCHAR(500),
    metadata        JSONB DEFAULT '{}',         -- Custom fields per project
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE user_settings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id         UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    preferences     JSONB DEFAULT '{}',
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE oauth_connections (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider        VARCHAR(50) NOT NULL,       -- google, github, etc.
    provider_id     VARCHAR(255) NOT NULL,      -- ID from provider
    access_token    TEXT,                       -- Encrypted
    refresh_token   TEXT,                       -- Encrypted
    token_expires   TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT uq_oauth_project_user_provider UNIQUE(project_id, user_id, provider),
    CONSTRAINT uq_oauth_project_provider_id UNIQUE(project_id, provider, provider_id)
);

CREATE INDEX idx_oauth_connections_user ON oauth_connections(project_id, user_id);

CREATE TABLE user_mfa (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id         UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    secret          VARCHAR(255) NOT NULL,      -- Encrypted TOTP secret
    recovery_codes  TEXT[],                     -- Encrypted
    enabled         BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE TABLE password_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    password_hash   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_password_history_user ON password_history(project_id, user_id);

-- ============================================================================
-- END USER SESSION TABLES
-- ============================================================================

CREATE TABLE sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_info     JSONB,
    ip_address      INET,
    user_agent      TEXT,
    expires_at      TIMESTAMP NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_sessions_user ON sessions(project_id, user_id);
CREATE INDEX idx_sessions_expires ON sessions(expires_at);

CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_id      UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) UNIQUE NOT NULL,
    expires_at      TIMESTAMP NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_session ON refresh_tokens(session_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);

-- ============================================================================
-- RBAC TABLES (Per Project for End Users)
-- ============================================================================

CREATE TABLE roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name            VARCHAR(50) NOT NULL,
    description     TEXT,
    is_system       BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT uq_roles_project_name UNIQUE(project_id, name)
);

-- Permissions are global (not per-project)
CREATE TABLE permissions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(100) UNIQUE NOT NULL,
    description     TEXT,
    category        VARCHAR(50)
);

CREATE TABLE role_permissions (
    role_id         UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id   UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_roles (
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id         UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at     TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user ON user_roles(project_id, user_id);

-- ============================================================================
-- WEBHOOK TABLES (Per Project)
-- ============================================================================

CREATE TABLE webhook_endpoints (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    url             VARCHAR(500) NOT NULL,
    secret          VARCHAR(255) NOT NULL,
    events          TEXT[] NOT NULL,
    enabled         BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_webhook_endpoints_project ON webhook_endpoints(project_id);

CREATE TABLE webhook_deliveries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    endpoint_id     UUID NOT NULL REFERENCES webhook_endpoints(id) ON DELETE CASCADE,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    status          VARCHAR(20) DEFAULT 'PENDING',
    attempts        INT DEFAULT 0,
    last_attempt_at TIMESTAMP,
    response_code   INT,
    response_body   TEXT,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_webhook_deliveries_endpoint ON webhook_deliveries(endpoint_id);
CREATE INDEX idx_webhook_deliveries_status ON webhook_deliveries(status);

-- ============================================================================
-- AUDIT TABLES
-- ============================================================================

CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID REFERENCES projects(id) ON DELETE CASCADE,
    organization_id UUID REFERENCES organizations(id) ON DELETE CASCADE,
    actor_type      VARCHAR(20) NOT NULL,       -- ORG_MEMBER, END_USER, SYSTEM
    actor_id        UUID,
    action          VARCHAR(100) NOT NULL,
    resource_type   VARCHAR(50),
    resource_id     UUID,
    ip_address      INET,
    user_agent      TEXT,
    metadata        JSONB,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_project ON audit_logs(project_id);
CREATE INDEX idx_audit_logs_org ON audit_logs(organization_id);
CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_type, actor_id);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at);
```

---

## 6. Authentication Flows

### 6.1 Org Member Login Flow

```
+-----------------------------------------------------------------------------+
|                    ORG MEMBER LOGIN FLOW (Dashboard)                         |
+-----------------------------------------------------------------------------+
|                                                                              |
|  [Dashboard]                    [Gateway]              [Auth Service]        |
|       |                            |                         |               |
|       | POST /org/auth/login       |                         |               |
|       | { email, password }        |                         |               |
|       |--------------------------->|                         |               |
|       |                            | Forward                 |               |
|       |                            |------------------------>|               |
|       |                            |                         |               |
|       |                            |                  Validate credentials   |
|       |                            |                  Check org membership   |
|       |                            |                  Check MFA status       |
|       |                            |                         |               |
|       |                            |     IF MFA enabled:     |               |
|       |                            |<------------------------|               |
|       |<---------------------------|     { mfaRequired,      |               |
|       |                            |       mfaToken }        |               |
|       |                            |                         |               |
|       | POST /org/auth/mfa/verify  |                         |               |
|       | { mfaToken, code }         |                         |               |
|       |--------------------------->|                         |               |
|       |                            |------------------------>|               |
|       |                            |                         |               |
|       |                            |                  Verify TOTP code       |
|       |                            |                  Create session         |
|       |                            |                  Generate tokens        |
|       |                            |                  Emit login event       |
|       |                            |                         |               |
|       |                            |<------------------------|               |
|       |<---------------------------|  { accessToken,         |               |
|       |                            |    refreshToken,        |               |
|       |                            |    member,              |               |
|       |                            |    organizations[] }    |               |
|       |                            |                         |               |
+-----------------------------------------------------------------------------+
```

### 6.2 End User Registration Flow

```
+-----------------------------------------------------------------------------+
|                    END USER REGISTRATION FLOW (SDK)                          |
+-----------------------------------------------------------------------------+
|                                                                              |
|  [SDK App]                      [Gateway]              [Auth Service]        |
|       |                            |                         |               |
|       | POST /auth/register        |                         |               |
|       | X-API-Key: pk_live_xxx     |                         |               |
|       | { email, password, name }  |                         |               |
|       |--------------------------->|                         |               |
|       |                            |  Validate API key       |               |
|       |                            |  Extract project_id     |               |
|       |                            |  Apply rate limit       |               |
|       |                            |------------------------>|               |
|       |                            |                         |               |
|       |                            |            Load project settings        |
|       |                            |            Validate password policy     |
|       |                            |            Check if email exists        |
|       |                            |            Hash password (Argon2id)     |
|       |                            |            Create user record           |
|       |                            |            Create profile               |
|       |                            |            Assign default role          |
|       |                            |            Generate tokens              |
|       |                            |            Emit user.registered         |
|       |                            |                         |               |
|       |                            |<------------------------|               |
|       |<---------------------------|  { user, accessToken,   |               |
|       |                            |    refreshToken }       |               |
|       |                            |                         |               |
|       |                            |                         |               |
|       |                     [Worker Service]                 |               |
|       |                            |                         |               |
|       |                     Consumes user.registered         |               |
|       |                     Sends welcome email              |               |
|       |                     Sends verification email         |               |
|       |                            |                         |               |
+-----------------------------------------------------------------------------+
```

### 6.3 End User Login Flow

```
+-----------------------------------------------------------------------------+
|                       END USER LOGIN FLOW (SDK)                              |
+-----------------------------------------------------------------------------+
|                                                                              |
|  [SDK App]                      [Gateway]              [Auth Service]        |
|       |                            |                         |               |
|       | POST /auth/login           |                         |               |
|       | X-API-Key: pk_live_xxx     |                         |               |
|       | { email, password }        |                         |               |
|       |--------------------------->|                         |               |
|       |                            |  Validate API key       |               |
|       |                            |------------------------>|               |
|       |                            |                         |               |
|       |                            |            Load project settings        |
|       |                            |            Find user by email           |
|       |                            |            Verify password              |
|       |                            |            Check user status            |
|       |                            |            Check MFA status             |
|       |                            |                         |               |
|       |              IF password correct and no MFA:         |               |
|       |                            |            Create session               |
|       |                            |            Generate tokens              |
|       |                            |            Update last_login_at         |
|       |                            |            Emit user.logged_in          |
|       |                            |<------------------------|               |
|       |<---------------------------|  { user, accessToken,   |               |
|       |                            |    refreshToken }       |               |
|       |                            |                         |               |
|       |              IF MFA required:                        |               |
|       |                            |<------------------------|               |
|       |<---------------------------|  { mfaRequired: true,   |               |
|       |                            |    mfaToken }           |               |
|       |                            |                         |               |
|       | POST /auth/login/mfa       |                         |               |
|       | { mfaToken, code }         |                         |               |
|       |--------------------------->|                         |               |
|       |                            |------------------------>|               |
|       |                            |            Verify TOTP  |               |
|       |                            |            Create session               |
|       |                            |            Generate tokens              |
|       |                            |<------------------------|               |
|       |<---------------------------|  { user, accessToken,   |               |
|       |                            |    refreshToken }       |               |
|       |                            |                         |               |
+-----------------------------------------------------------------------------+
```

### 6.4 OAuth Flow

```
+-----------------------------------------------------------------------------+
|                         END USER OAUTH FLOW                                  |
+-----------------------------------------------------------------------------+
|                                                                              |
|  [SDK App]         [Gateway]     [Auth Service]     [OAuth Provider]         |
|       |               |               |                    |                 |
|       | GET /auth/oauth/google        |                    |                 |
|       | X-API-Key: pk_live_xxx        |                    |                 |
|       | ?redirect_uri=...             |                    |                 |
|       |-------------->|               |                    |                 |
|       |               |-------------->|                    |                 |
|       |               |               |                    |                 |
|       |               |         Load project OAuth config  |                 |
|       |               |         Generate state, PKCE       |                 |
|       |               |         Store in Redis             |                 |
|       |               |         Build OAuth URL            |                 |
|       |               |               |                    |                 |
|       |               |<--------------|                    |                 |
|       |<--------------|  Redirect to  |                    |                 |
|       |               |  Google       |                    |                 |
|       |                                                    |                 |
|       |  User authenticates with Google                    |                 |
|       |                                                    |                 |
|       |<---------------------------------------------------|                 |
|       |  Redirect to callback with code                    |                 |
|       |                                                    |                 |
|       | GET /auth/oauth/google/callback                    |                 |
|       | ?code=xxx&state=xxx           |                    |                 |
|       |-------------->|               |                    |                 |
|       |               |-------------->|                    |                 |
|       |               |               |                    |                 |
|       |               |         Validate state             |                 |
|       |               |         Exchange code for tokens   |                 |
|       |               |               |------------------->|                 |
|       |               |               |<-------------------|                 |
|       |               |               |    { access_token, |                 |
|       |               |               |      user_info }   |                 |
|       |               |               |                    |                 |
|       |               |         Find or create user        |                 |
|       |               |         Link OAuth connection      |                 |
|       |               |         Create session             |                 |
|       |               |         Generate tokens            |                 |
|       |               |               |                    |                 |
|       |               |<--------------|                    |                 |
|       |<--------------|  Redirect to app with tokens       |                 |
|       |               |                                    |                 |
+-----------------------------------------------------------------------------+
```

### 6.5 Token Refresh Flow

```
+-----------------------------------------------------------------------------+
|                         TOKEN REFRESH FLOW                                   |
+-----------------------------------------------------------------------------+
|                                                                              |
|  [SDK App]                      [Gateway]              [Auth Service]        |
|       |                            |                         |               |
|       | POST /auth/refresh         |                         |               |
|       | X-API-Key: pk_live_xxx     |                         |               |
|       | { refreshToken }           |                         |               |
|       |--------------------------->|                         |               |
|       |                            |  Validate API key       |               |
|       |                            |------------------------>|               |
|       |                            |                         |               |
|       |                            |            Validate refresh token       |
|       |                            |            Check if revoked             |
|       |                            |            Check if expired             |
|       |                            |            Load user & session          |
|       |                            |                         |               |
|       |                            |            Revoke old refresh token     |
|       |                            |            Generate new token pair      |
|       |                            |            (Token Rotation)             |
|       |                            |                         |               |
|       |                            |<------------------------|               |
|       |<---------------------------|  { accessToken,         |               |
|       |                            |    refreshToken }       |               |
|       |                            |                         |               |
+-----------------------------------------------------------------------------+
```

---

## 7. RBAC Model

### 7.1 Two-Level RBAC

```
+-----------------------------------------------------------------------------+
|                          TWO-LEVEL RBAC MODEL                                |
+-----------------------------------------------------------------------------+
|                                                                              |
|  ORGANIZATION LEVEL (Fixed Roles)                                            |
|  ================================                                            |
|                                                                              |
|  +----------+    +-----------+    +----------+                               |
|  |  OWNER   |    |   ADMIN   |    |  MEMBER  |                               |
|  +----------+    +-----------+    +----------+                               |
|       |               |                |                                     |
|       v               v                v                                     |
|  * All permissions  * Most perms    * View-only                              |
|  * Transfer owner   * Manage users  * No admin                               |
|  * Delete org       * Manage projects                                        |
|  * Billing          * No billing                                             |
|                                                                              |
|                                                                              |
|  PROJECT LEVEL (Customizable Roles)                                          |
|  ==================================                                          |
|                                                                              |
|  +----------+    +-----------+    +----------+    +----------+               |
|  |  admin   |    |   editor  |    |  viewer  |    |  custom  |               |
|  | (system) |    | (system)  |    | (system) |    |  roles   |               |
|  +----------+    +-----------+    +----------+    +----------+               |
|       |               |                |                |                    |
|       v               v                v                v                    |
|  [permissions]   [permissions]   [permissions]   [permissions]               |
|                                                                              |
|  Per-project roles are independent:                                          |
|  * Production project may have different roles than Staging                  |
|  * End users can have different roles in different projects                  |
|                                                                              |
+-----------------------------------------------------------------------------+
```

### 7.2 Org-Level Permissions

| Permission | OWNER | ADMIN | MEMBER |
|------------|:-----:|:-----:|:------:|
| View organization | Y | Y | Y |
| Update organization | Y | Y | - |
| Delete organization | Y | - | - |
| Transfer ownership | Y | - | - |
| Invite members | Y | Y | - |
| Remove members | Y | Y | - |
| Change member roles | Y | - | - |
| Create projects | Y | Y | - |
| Delete projects | Y | Y | - |
| Manage billing | Y | - | - |

### 7.3 Project-Level Default Roles

**System Roles (per project, cannot be deleted):**

| Role | Description |
|------|-------------|
| admin | Full access to all project resources |
| editor | Can modify data but not settings |
| viewer | Read-only access |

**Default Permissions:**

```sql
INSERT INTO permissions (code, description, category) VALUES
-- User permissions
('users:read', 'View user profiles', 'users'),
('users:write', 'Create and update users', 'users'),
('users:delete', 'Delete users', 'users'),
-- Content permissions
('content:read', 'View content', 'content'),
('content:write', 'Create and update content', 'content'),
('content:delete', 'Delete content', 'content'),
-- Settings permissions
('settings:read', 'View settings', 'settings'),
('settings:write', 'Modify settings', 'settings');
```

### 7.4 Permission Check Flow

```
+-----------------------------------------------------------------------------+
|                       PERMISSION CHECK FLOW                                  |
+-----------------------------------------------------------------------------+
|                                                                              |
|  [SDK Request]                                                               |
|       |                                                                      |
|       v                                                                      |
|  +-------------------+                                                       |
|  |  API Gateway      |                                                       |
|  +-------------------+                                                       |
|       |                                                                      |
|       | 1. Extract JWT claims                                                |
|       | 2. Get project_id from API key                                       |
|       |                                                                      |
|       v                                                                      |
|  +-------------------+                                                       |
|  |  JWT Claims       |                                                       |
|  +-------------------+                                                       |
|  | sub: user_id      |                                                       |
|  | project: proj_id  |                                                       |
|  | roles: [admin]    |                                                       |
|  | perms: [...]      |----+                                                  |
|  +-------------------+    |                                                  |
|                           |                                                  |
|       +-------------------+                                                  |
|       |                                                                      |
|       v                                                                      |
|  +-------------------+                                                       |
|  |  @PreAuthorize    |                                                       |
|  |  hasPermission()  |                                                       |
|  +-------------------+                                                       |
|       |                                                                      |
|       | Check if required permission                                         |
|       | exists in JWT claims                                                 |
|       |                                                                      |
|       v                                                                      |
|  +-------------------+                                                       |
|  |  Allow / Deny     |                                                       |
|  +-------------------+                                                       |
|                                                                              |
+-----------------------------------------------------------------------------+
```

---

## 8. Organization & Project Configuration

### 8.1 Project Settings Structure

```
+-----------------------------------------------------------------------------+
|                    PROJECT SETTINGS STRUCTURE                                |
+-----------------------------------------------------------------------------+
|                                                                              |
|  project_settings: {                                                         |
|                                                                              |
|    // Password Policy                                                        |
|    passwordPolicy: {                                                         |
|      minLength: 8,                                                           |
|      requireUppercase: true,                                                 |
|      requireLowercase: true,                                                 |
|      requireNumbers: true,                                                   |
|      requireSpecialChars: false,                                             |
|      historyCount: 3          // Prevent reuse of last N passwords           |
|    },                                                                        |
|                                                                              |
|    // MFA Settings                                                           |
|    mfa: {                                                                    |
|      enabled: true,           // Allow MFA setup                             |
|      required: false,         // Force all users to enable MFA               |
|      methods: ["totp"]        // Supported methods                           |
|    },                                                                        |
|                                                                              |
|    // OAuth Providers                                                        |
|    oauth: {                                                                  |
|      google: {                                                               |
|        enabled: true,                                                        |
|        clientId: "xxx",                                                      |
|        clientSecret: "xxx"    // Encrypted                                   |
|      },                                                                      |
|      github: {                                                               |
|        enabled: false,                                                       |
|        clientId: null,                                                       |
|        clientSecret: null                                                    |
|      },                                                                      |
|      // ... other providers                                                  |
|    },                                                                        |
|                                                                              |
|    // Session Settings                                                       |
|    session: {                                                                |
|      accessTokenTtl: 900,     // 15 minutes                                  |
|      refreshTokenTtl: 604800, // 7 days                                      |
|      maxSessionsPerUser: 5,                                                  |
|      revokeOnPasswordChange: true                                            |
|    },                                                                        |
|                                                                              |
|    // General                                                                |
|    general: {                                                                |
|      allowRegistration: true,                                                |
|      requireEmailVerification: true,                                         |
|      allowedDomains: null,    // null = any, ["company.com"] = restricted    |
|      blockedDomains: []                                                      |
|    }                                                                         |
|  }                                                                           |
|                                                                              |
+-----------------------------------------------------------------------------+
```

### 8.2 API Key Format

```
+-----------------------------------------------------------------------------+
|                          API KEY FORMAT                                      |
+-----------------------------------------------------------------------------+
|                                                                              |
|  Format: pk_<environment>_<random>                                           |
|                                                                              |
|  Examples:                                                                   |
|  * pk_live_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6                                  |
|  * pk_test_x9y8z7w6v5u4t3s2r1q0p9o8n7m6l5k4                                  |
|                                                                              |
|  Storage:                                                                    |
|  +------------------+                                                        |
|  | id: uuid         |                                                        |
|  | project_id: uuid |                                                        |
|  | name: "Prod Key" |                                                        |
|  | key_hash: sha256 |   <-- Only hash stored, not the key                    |
|  | key_prefix: "pk_live_a1b2" <-- For identification                         |
|  | last_used_at     |                                                        |
|  | expires_at       |                                                        |
|  | status: ACTIVE   |                                                        |
|  +------------------+                                                        |
|                                                                              |
|  The full key is shown ONCE at creation and never again.                     |
|                                                                              |
+-----------------------------------------------------------------------------+
```

---

## 9. Multi-Tenancy

### 9.1 Isolation Model

```
+-----------------------------------------------------------------------------+
|                         ISOLATION MODEL                                      |
+-----------------------------------------------------------------------------+
|                                                                              |
|  +-------------------------------------------------------------------+       |
|  |                      Organization: Acme Corp                      |       |
|  +-------------------------------------------------------------------+       |
|  |                                                                   |       |
|  |  +---------------------------+  +---------------------------+     |       |
|  |  | Project: Production       |  | Project: Staging          |     |       |
|  |  +---------------------------+  +---------------------------+     |       |
|  |  |                           |  |                           |     |       |
|  |  | Users: 10,000             |  | Users: 500                |     |       |
|  |  | Roles: admin, user, guest |  | Roles: admin, tester      |     |       |
|  |  | MFA: Required             |  | MFA: Optional             |     |       |
|  |  | OAuth: Google, GitHub     |  | OAuth: Google only        |     |       |
|  |  |                           |  |                           |     |       |
|  |  | API Keys:                 |  | API Keys:                 |     |       |
|  |  | * pk_live_xxx (web)       |  | * pk_test_yyy (dev)       |     |       |
|  |  | * pk_live_zzz (mobile)    |  |                           |     |       |
|  |  |                           |  |                           |     |       |
|  |  +---------------------------+  +---------------------------+     |       |
|  |                                                                   |       |
|  +-------------------------------------------------------------------+       |
|                                                                              |
|  DATA ISOLATION:                                                             |
|  * Users in Production cannot access Staging data                            |
|  * API keys are project-scoped                                               |
|  * Settings are independent per project                                      |
|  * RBAC roles are defined per project                                        |
|                                                                              |
+-----------------------------------------------------------------------------+
```

### 9.2 API Key Resolution

```
+-----------------------------------------------------------------------------+
|                       API KEY RESOLUTION FLOW                                |
+-----------------------------------------------------------------------------+
|                                                                              |
|  [Request with X-API-Key header]                                             |
|       |                                                                      |
|       v                                                                      |
|  +-------------------+                                                       |
|  |  API Gateway      |                                                       |
|  +-------------------+                                                       |
|       |                                                                      |
|       | 1. Extract API key from header                                       |
|       | 2. Check Redis cache                                                 |
|       |                                                                      |
|       +--------> [Cache Hit] -------> Return cached project_id               |
|       |                                                                      |
|       | 3. [Cache Miss] Query database                                       |
|       |                                                                      |
|       v                                                                      |
|  +-------------------+                                                       |
|  |  SELECT           |                                                       |
|  |  project_id       |                                                       |
|  |  FROM api_keys    |                                                       |
|  |  WHERE key_hash   |                                                       |
|  |  = sha256(key)    |                                                       |
|  |  AND status =     |                                                       |
|  |  'ACTIVE'         |                                                       |
|  +-------------------+                                                       |
|       |                                                                      |
|       | 4. Cache result in Redis (TTL: 5 min)                                |
|       | 5. Update last_used_at (async)                                       |
|       |                                                                      |
|       v                                                                      |
|  [Forward request with project_id in context]                                |
|                                                                              |
+-----------------------------------------------------------------------------+
```

---

## 10. Event System

### 10.1 Event Architecture

```
+-----------------------------------------------------------------------------+
|                         EVENT ARCHITECTURE                                   |
+-----------------------------------------------------------------------------+
|                                                                              |
|  +-------------------+                                                       |
|  |   Auth Service    |                                                       |
|  +-------------------+                                                       |
|          |                                                                   |
|          | Publish events                                                    |
|          |                                                                   |
|          v                                                                   |
|  +-------------------+                                                       |
|  |    RabbitMQ       |                                                       |
|  +-------------------+                                                       |
|  |                   |                                                       |
|  | Exchange: auth.events (topic)                                             |
|  |     |                                                                     |
|  |     +---> Queue: email.queue ------> [Worker: EmailHandler]               |
|  |     |         routing: user.#, org.*                                      |
|  |     |                                                                     |
|  |     +---> Queue: webhook.queue ---> [Worker: WebhookHandler]              |
|  |     |         routing: *.#                                                |
|  |     |                                                                     |
|  |     +---> Queue: audit.queue -----> [Worker: AuditHandler]                |
|  |               routing: *.#                                                |
|  |                                                                           |
|  +-------------------+                                                       |
|                                                                              |
+-----------------------------------------------------------------------------+
```

### 10.2 Event Types

```
+-----------------------------------------------------------------------------+
|                           EVENT TYPES                                        |
+-----------------------------------------------------------------------------+
|                                                                              |
|  ORGANIZATION EVENTS (org.*)                                                 |
|  ---------------------------                                                 |
|  org.created           - New organization registered                         |
|  org.updated           - Organization details updated                        |
|  org.member.invited    - Member invitation sent                              |
|  org.member.joined     - Member accepted invitation                          |
|  org.member.removed    - Member removed from org                             |
|  org.member.role_changed - Member role updated                               |
|                                                                              |
|  END USER EVENTS (user.*)                                                    |
|  -------------------------                                                   |
|  user.registered       - New end user registered                             |
|  user.email_verified   - Email verified                                      |
|  user.logged_in        - Successful login                                    |
|  user.logged_out       - User logged out                                     |
|  user.password_changed - Password updated                                    |
|  user.password_reset   - Password reset requested                            |
|  user.mfa_enabled      - MFA enabled                                         |
|  user.mfa_disabled     - MFA disabled                                        |
|  user.session_revoked  - Session revoked                                     |
|  user.updated          - Profile updated                                     |
|  user.deleted          - User deleted                                        |
|                                                                              |
|  SECURITY EVENTS (security.*)                                                |
|  -----------------------------                                               |
|  security.login_failed      - Failed login attempt                           |
|  security.suspicious_activity - Unusual behavior detected                    |
|  security.new_device        - Login from new device                          |
|                                                                              |
+-----------------------------------------------------------------------------+
```

### 10.3 Event Payload Structure

```json
{
  "id": "evt_abc123",
  "type": "user.registered",
  "timestamp": "2024-12-15T10:30:00Z",
  "projectId": "proj_xyz789",
  "organizationId": "org_def456",
  "actor": {
    "type": "END_USER",
    "id": "user_123"
  },
  "data": {
    "userId": "user_123",
    "email": "user@example.com",
    "registrationMethod": "email"
  },
  "metadata": {
    "ipAddress": "192.168.1.1",
    "userAgent": "Mozilla/5.0...",
    "correlationId": "req_abc123"
  }
}
```

---

## 11. React SDK

### 11.1 SDK Overview

```
+-----------------------------------------------------------------------------+
|                          REACT SDK OVERVIEW                                  |
+-----------------------------------------------------------------------------+
|                                                                              |
|  Package: @auctoritas/react                                                  |
|                                                                              |
|  +-------------------+     +-------------------+     +-------------------+   |
|  |    Provider       |     |    Components     |     |      Hooks        |   |
|  +-------------------+     +-------------------+     +-------------------+   |
|  |                   |     |                   |     |                   |   |
|  | AuctoritasProvider|     | SignIn            |     | useAuth           |   |
|  |                   |     | SignUp            |     | useUser           |   |
|  |                   |     | SignOut           |     | useSession        |   |
|  |                   |     | UserButton        |     | usePermissions    |   |
|  |                   |     | MfaSetup          |     | useOrganization   |   |
|  |                   |     | ProtectedRoute    |     |                   |   |
|  |                   |     | OAuthButton       |     |                   |   |
|  +-------------------+     +-------------------+     +-------------------+   |
|                                                                              |
+-----------------------------------------------------------------------------+
```

### 11.2 Basic Usage

```tsx
// App.tsx
import { AuctoritasProvider } from '@auctoritas/react';

function App() {
  return (
    <AuctoritasProvider 
      apiKey="pk_live_xxx"
      apiUrl="https://api.auctoritas.dev"
    >
      <Router>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route 
            path="/dashboard" 
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            } 
          />
        </Routes>
      </Router>
    </AuctoritasProvider>
  );
}

// LoginPage.tsx
import { SignIn, OAuthButton } from '@auctoritas/react';

function LoginPage() {
  return (
    <div>
      <SignIn 
        onSuccess={() => navigate('/dashboard')}
        onError={(error) => console.error(error)}
      />
      <div>
        <OAuthButton provider="google" />
        <OAuthButton provider="github" />
      </div>
    </div>
  );
}

// Dashboard.tsx
import { useAuth, useUser, UserButton } from '@auctoritas/react';

function Dashboard() {
  const { isAuthenticated, logout } = useAuth();
  const { user, isLoading } = useUser();
  
  if (isLoading) return <Spinner />;
  
  return (
    <div>
      <header>
        <h1>Welcome, {user.name}</h1>
        <UserButton />
      </header>
      {/* Dashboard content */}
    </div>
  );
}
```

### 11.3 Permission-Based Access

```tsx
import { usePermissions, ProtectedRoute } from '@auctoritas/react';

// Using hook
function AdminPanel() {
  const { hasPermission, hasRole } = usePermissions();
  
  if (!hasRole('admin')) {
    return <AccessDenied />;
  }
  
  return (
    <div>
      {hasPermission('users:write') && (
        <button>Create User</button>
      )}
      {hasPermission('settings:write') && (
        <button>Edit Settings</button>
      )}
    </div>
  );
}

// Using component
function App() {
  return (
    <Routes>
      <Route 
        path="/admin" 
        element={
          <ProtectedRoute 
            requiredPermissions={['admin:access']}
            fallback={<AccessDenied />}
          >
            <AdminPanel />
          </ProtectedRoute>
        } 
      />
    </Routes>
  );
}
```

---

## 12. Dashboard Application

### 12.1 Dashboard Architecture

```
+-----------------------------------------------------------------------------+
|                      DASHBOARD ARCHITECTURE                                  |
+-----------------------------------------------------------------------------+
|                                                                              |
|  Technology Stack:                                                           |
|  * React 19 + TypeScript                                                     |
|  * TanStack Query (React Query) for data fetching                            |
|  * TanStack Router for routing                                               |
|  * Tailwind CSS + shadcn/ui for UI                                           |
|  * WebSocket for real-time updates                                           |
|                                                                              |
|  +-------------------------------------------------------------------+       |
|  |  Static Build (Vite)                                              |       |
|  |  Served via Traefik at dashboard.auctoritas.dev                   |       |
|  +-------------------------------------------------------------------+       |
|       |                                                                      |
|       | API calls via @auctoritas/react SDK                                  |
|       |                                                                      |
|       v                                                                      |
|  +-------------------------------------------------------------------+       |
|  |  api.auctoritas.dev                                               |       |
|  +-------------------------------------------------------------------+       |
|                                                                              |
+-----------------------------------------------------------------------------+
```

### 12.2 Dashboard Pages

```
+-----------------------------------------------------------------------------+
|                        DASHBOARD NAVIGATION                                  |
+-----------------------------------------------------------------------------+
|                                                                              |
|  ORGANIZATION LEVEL                                                          |
|  ------------------                                                          |
|  /org/[orgId]/overview        - Organization dashboard                       |
|  /org/[orgId]/members         - Team members                                 |
|  /org/[orgId]/settings        - Organization settings                        |
|  /org/[orgId]/billing         - Billing & plans (future)                     |
|                                                                              |
|  PROJECT LEVEL                                                               |
|  -------------                                                               |
|  /org/[orgId]/projects/[projectId]/overview    - Project dashboard           |
|  /org/[orgId]/projects/[projectId]/users       - End user management         |
|  /org/[orgId]/projects/[projectId]/roles       - RBAC management             |
|  /org/[orgId]/projects/[projectId]/settings    - Project settings            |
|  /org/[orgId]/projects/[projectId]/api-keys    - API key management          |
|  /org/[orgId]/projects/[projectId]/webhooks    - Webhook configuration       |
|  /org/[orgId]/projects/[projectId]/logs        - Audit logs                  |
|                                                                              |
+-----------------------------------------------------------------------------+
```

### 12.3 Dashboard UI Structure

```
+-----------------------------------------------------------------------------+
|  +--------+                                              [User v] [Logout]   |
|  | LOGO   |   Acme Corp v  >  Production v                                   |
+--+--------+--------------------------------------------------------------+---+
|  | Overview | Users | Roles | Settings | API Keys | Webhooks |               |
+-----------------------------------------------------------------------------+
|                                                                              |
|  Production Overview                                                         |
|                                                                              |
|  +--------------+  +--------------+  +--------------+  +--------------+      |
|  |    10,243    |  |     156      |  |     89%      |  |    2,341     |      |
|  |  Total Users |  |  New Today   |  | MFA Enabled  |  | Active Today |      |
|  +--------------+  +--------------+  +--------------+  +--------------+      |
|                                                                              |
|  Quick Actions                                                               |
|  +--------------------------------------------------------------------+      |
|  |  [View API Keys]   [Manage Users]   [Edit Settings]   [View Logs]  |      |
|  +--------------------------------------------------------------------+      |
|                                                                              |
|  Recent Activity                                                             |
|  +--------------------------------------------------------------------+      |
|  |  * user@example.com logged in                         2 min ago    |      |
|  |  * new@user.com registered                            5 min ago    |      |
|  |  * admin@test.com enabled MFA                        10 min ago    |      |
|  +--------------------------------------------------------------------+      |
|                                                                              |
+-----------------------------------------------------------------------------+
```

---

## 13. Security Architecture

### 13.1 Security Overview

```
+-----------------------------------------------------------------------------+
|                       SECURITY ARCHITECTURE                                  |
+-----------------------------------------------------------------------------+
|                                                                              |
|  +-------------------------+                                                 |
|  | TRANSPORT SECURITY      |                                                 |
|  +-------------------------+                                                 |
|  | * TLS 1.3 everywhere    |                                                 |
|  | * HSTS enabled          |                                                 |
|  | * Certificate auto-renew|                                                 |
|  +-------------------------+                                                 |
|                                                                              |
|  +-------------------------+   +-------------------------+                   |
|  | PASSWORD SECURITY       |   | TOKEN SECURITY          |                   |
|  +-------------------------+   +-------------------------+                   |
|  | * Argon2id hashing      |   | * JWT with RS256        |                   |
|  | * Configurable policy   |   | * Short-lived access    |                   |
|  | * History enforcement   |   | * Refresh token rotation|                   |
|  +-------------------------+   | * Secure storage guide  |                   |
|                                +-------------------------+                   |
|                                                                              |
|  +-------------------------+   +-------------------------+                   |
|  | API SECURITY            |   | DATA SECURITY           |                   |
|  +-------------------------+   +-------------------------+                   |
|  | * API key authentication|   | * Encryption at rest    |                   |
|  | * Rate limiting         |   | * Project isolation     |                   |
|  | * CORS configuration    |   | * Audit logging         |                   |
|  | * Input validation      |   | * GDPR compliance       |                   |
|  +-------------------------+   +-------------------------+                   |
|                                                                              |
+-----------------------------------------------------------------------------+
```

### 13.2 JWT Structure

```
+-----------------------------------------------------------------------------+
|                          JWT STRUCTURE                                       |
+-----------------------------------------------------------------------------+
|                                                                              |
|  END USER JWT:                                                               |
|  {                                                                           |
|    "header": {                                                               |
|      "alg": "RS256",                                                         |
|      "typ": "JWT"                                                            |
|    },                                                                        |
|    "payload": {                                                              |
|      "sub": "user_abc123",           // User ID                              |
|      "iss": "https://api.auctoritas.dev",                                    |
|      "aud": "proj_xyz789",           // Project ID                           |
|      "iat": 1702634400,              // Issued at                            |
|      "exp": 1702635300,              // Expires (15 min)                     |
|      "type": "end_user",             // Token type                           |
|      "sid": "ses_def456",            // Session ID                           |
|      "roles": ["admin", "user"],     // Project roles                        |
|      "permissions": [                // Resolved permissions                 |
|        "users:read",                                                         |
|        "users:write",                                                        |
|        "content:*"                                                           |
|      ]                                                                       |
|    }                                                                         |
|  }                                                                           |
|                                                                              |
|  ORG MEMBER JWT:                                                             |
|  {                                                                           |
|    "payload": {                                                              |
|      "sub": "mem_abc123",            // Member ID                            |
|      "iss": "https://api.auctoritas.dev",                                    |
|      "type": "org_member",           // Token type                           |
|      "org_id": "org_xyz789",         // Organization ID                      |
|      "org_role": "ADMIN",            // Org-level role                       |
|      "sid": "ses_def456"             // Session ID                           |
|    }                                                                         |
|  }                                                                           |
|                                                                              |
+-----------------------------------------------------------------------------+
```

### 13.3 Rate Limiting

| Endpoint Category | Limit | Window |
|-------------------|-------|--------|
| Login attempts | 5 | 15 min |
| Registration | 10 | 1 hour |
| Password reset | 3 | 1 hour |
| API (authenticated) | 1000 | 1 hour |
| API (unauthenticated) | 100 | 1 hour |

---

## 14. Infrastructure & Deployment

### 14.1 k3s Cluster Architecture

```
+-----------------------------------------------------------------------------+
|                      K3S CLUSTER ARCHITECTURE                                |
+-----------------------------------------------------------------------------+
|                                                                              |
|  +-------------------------------------------------------------------+       |
|  |  Hetzner Cloud Server (CX21 or CX31)                              |       |
|  |  Ubuntu 24.04 LTS                                                 |       |
|  +-------------------------------------------------------------------+       |
|       |                                                                      |
|       v                                                                      |
|  +-------------------------------------------------------------------+       |
|  |  k3s (Single Node)                                                |       |
|  +-------------------------------------------------------------------+       |
|  |                                                                   |       |
|  |  Traefik Ingress (built-in)                                       |       |
|  |       |                                                           |       |
|  |       +----> api.auctoritas.dev ----> gateway-service             |       |
|  |       |                                                           |       |
|  |       +----> dashboard.auctoritas.dev ----> dashboard (static)    |       |
|  |                                                                   |       |
|  |  +-------------+  +-------------+  +-------------+                |       |
|  |  |   Gateway   |  |    Auth     |  |   Worker    |                |       |
|  |  |   Service   |  |   Service   |  |   Service   |                |       |
|  |  +-------------+  +-------------+  +-------------+                |       |
|  |                                                                   |       |
|  |  +-------------+  +-------------+  +-------------+                |       |
|  |  | PostgreSQL  |  |    Redis    |  |  RabbitMQ   |                |       |
|  |  | (StatefulSet|  | (StatefulSet|  | (StatefulSet|                |       |
|  |  +-------------+  +-------------+  +-------------+                |       |
|  |                                                                   |       |
|  +-------------------------------------------------------------------+       |
|                                                                              |
+-----------------------------------------------------------------------------+
```

### 14.2 Deployment Manifests

```yaml
# Example: auth-service deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
  namespace: auctoritas
spec:
  replicas: 2
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      containers:
        - name: auth-service
          image: ghcr.io/auctoritas/auth-service:latest
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            - name: DATABASE_URL
              valueFrom:
                secretKeyRef:
                  name: db-credentials
                  key: url
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
```

### 14.3 Cost Estimate

| Resource | Spec | Monthly Cost |
|----------|------|--------------|
| Hetzner CX21 | 2 vCPU, 4GB RAM | ~â‚¬5.49 |
| Additional storage | 20GB | ~â‚¬1.00 |
| **Total** | | **~â‚¬6.49/month** |

---

## 15. Monorepo Structure

The monorepo uses **ecosystem separation** to cleanly divide Java (Maven) and JavaScript (pnpm) toolchains:

```
auctoritas/
│
├── backend/                          # Java ecosystem (Maven)
│   ├── pom.xml                       # Maven parent POM
│   ├── services/
│   │   ├── gateway-service/          # API Gateway (Spring Cloud Gateway)
│   │   │   ├── pom.xml
│   │   │   └── src/
│   │   ├── auth-service/             # Core Auth Service (Spring Boot)
│   │   │   ├── pom.xml
│   │   │   └── src/
│   │   └── worker-service/           # Background Worker (Spring AMQP)
│   │       ├── pom.xml
│   │       └── src/
│   └── libs/
│       └── common/                   # Shared Java utilities
│           ├── pom.xml
│           └── src/
│
├── frontend/                         # JavaScript ecosystem (pnpm)
│   ├── package.json                  # Workspace root
│   ├── pnpm-workspace.yaml           # Workspace configuration
│   ├── apps/
│   │   ├── dashboard/                # React Dashboard (Vite)
│   │   │   ├── package.json
│   │   │   └── src/
│   │   └── docs/                     # Documentation site (optional)
│   │       ├── package.json
│   │       └── src/
│   └── packages/
│       └── sdk-react/                # React SDK (@auctoritas/react)
│           ├── package.json
│           └── src/
│
├── infra/
│   ├── docker/                       # Dockerfiles
│   │   ├── Dockerfile.gateway
│   │   ├── Dockerfile.auth
│   │   ├── Dockerfile.worker
│   │   └── Dockerfile.dashboard
│   ├── k8s/                          # Kubernetes manifests
│   │   ├── base/
│   │   └── overlays/
│   │       ├── dev/
│   │       └── prod/
│   └── scripts/                      # Deployment scripts
│
├── .github/
│   └── workflows/
│       ├── backend-ci.yml            # Java build & test
│       ├── frontend-ci.yml           # JS build & test
│       └── deploy.yml                # Production deployment
│
├── docker-compose.yml                # Local development
├── .gitignore
├── LICENSE
└── README.md
```

### 15.1 Backend Configuration

**Maven Parent POM** (`backend/pom.xml`):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>dev.auctoritas</groupId>
    <artifactId>auctoritas-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    
    <modules>
        <module>libs/common</module>
        <module>services/gateway-service</module>
        <module>services/auth-service</module>
        <module>services/worker-service</module>
    </modules>
    
    <properties>
        <java.version>21</java.version>
        <spring-boot.version>3.3.0</spring-boot.version>
        <spring-cloud.version>2023.0.2</spring-cloud.version>
    </properties>
    
    <!-- dependency management, plugins, etc. -->
</project>
```

### 15.2 Frontend Configuration

**pnpm Workspace** (`frontend/pnpm-workspace.yaml`):
```yaml
packages:
  - 'apps/*'
  - 'packages/*'
```

**Workspace Root** (`frontend/package.json`):
```json
{
  "name": "auctoritas-frontend",
  "private": true,
  "scripts": {
    "dev:dashboard": "pnpm --filter dashboard dev",
    "build:sdk": "pnpm --filter @auctoritas/sdk-react build",
    "build:dashboard": "pnpm --filter dashboard build",
    "build:all": "pnpm -r build",
    "test": "pnpm -r test",
    "lint": "pnpm -r lint",
    "storybook": "pnpm --filter @auctoritas/sdk-react storybook"
  },
  "devDependencies": {
    "typescript": "^5.4.0"
  }
}
```

### 15.3 Why Ecosystem Separation?

| Benefit | Description |
|---------|-------------|
| **Clear boundaries** | Java developers work in `backend/`, JS developers in `frontend/` |
| **IDE support** | IntelliJ opens `backend/` as Maven project; VS Code opens `frontend/` as JS workspace |
| **Independent CI/CD** | Java builds don't block JS builds |
| **Standard conventions** | Each ecosystem follows its own best practices |
| **Easier onboarding** | New developers understand structure immediately |

---

## 16. Implementation Roadmap

### 16.1 Phase Overview

```
+-----------------------------------------------------------------------------+
|                        16-WEEK IMPLEMENTATION PLAN                           |
+-----------------------------------------------------------------------------+
|                                                                              |
|  PHASE 1: Foundation (Weeks 1-4)                                             |
|  --------------------------------                                            |
|  Week 1:  Project setup, monorepo, dev environment                           |
|  Week 2:  Auth service core + organization registration                      |
|  Week 3:  End user auth + password policies                                  |
|  Week 4:  API Gateway + JWT validation                                       |
|                                                                              |
|  PHASE 2: Core Features (Weeks 5-8)                                          |
|  ----------------------------------                                          |
|  Week 5:  OAuth providers (per project)                                      |
|  Week 6:  MFA (org members + end users)                                      |
|  Week 7:  RBAC (two-level: org + project)                                    |
|  Week 8:  User & session management                                          |
|                                                                              |
|  PHASE 3: Frontend (Weeks 9-12)                                              |
|  ------------------------------                                              |
|  Week 9:  React SDK foundation                                               |
|  Week 10: SDK components + hooks                                             |
|  Week 11: Dashboard - auth + org/project selection                           |
|  Week 12: Dashboard - admin pages + E2E setup                                |
|                                                                              |
|  PHASE 4: Production (Weeks 13-16)                                           |
|  ---------------------------------                                           |
|  Week 13: Worker service (emails, webhooks)                                  |
|  Week 14: Integration & E2E testing                                          |
|  Week 15: Multi-tenancy + Hetzner deployment                                 |
|  Week 16: Deploy + polish + documentation                                    |
|                                                                              |
+-----------------------------------------------------------------------------+
```

### 16.2 Week-by-Week Checklist

#### Phase 1: Foundation (Weeks 1-4)

**Week 1: Project Setup**
- [ ] Initialize monorepo with ecosystem separation (backend/ + frontend/)
- [ ] Setup Maven parent POM in backend/
- [ ] Create service modules (backend/services/gateway, auth, worker)
- [ ] Create shared backend/libs/common module
- [ ] Setup pnpm workspace in frontend/
- [ ] Create frontend/apps/dashboard and frontend/packages/sdk-react placeholders
- [ ] Setup Docker Compose for local development
- [ ] Configure PostgreSQL, Redis, RabbitMQ containers
- [ ] Setup GitHub repository and branch protection
- [ ] Configure CI pipelines (backend-ci.yml, frontend-ci.yml)
- [ ] Create initial README and documentation structure

**Week 2: Auth Service Core + Organizations**
- [ ] Create backend/services/auth-service module
- [ ] Database schema (Flyway migrations) with org/project tables
- [ ] Organization registration endpoint
- [ ] Organization member auth (login, logout, refresh)
- [ ] Default project creation on org registration
- [ ] API key generation
- [ ] JWT generation for org members
- [ ] Password hashing with Argon2id

**Week 3: End User Auth**
- [ ] End user registration (via API key)
- [ ] End user login (via API key)
- [ ] Password policy validation (per project)
- [ ] Refresh token rotation
- [ ] Email verification flow
- [ ] Forgot/reset password flow
- [ ] Redis integration for sessions

**Week 4: API Gateway**
- [ ] Create backend/services/gateway-service module
- [ ] Route configuration
- [ ] JWT verification filter (both user types)
- [ ] API key validation filter
- [ ] Rate limiting with Redis
- [ ] CORS configuration
- [ ] Correlation ID propagation
- [ ] Error handling

#### Phase 2: Core Features (Weeks 5-8)

**Week 5: OAuth Providers**
- [ ] OAuth configuration per project
- [ ] Google OAuth integration
- [ ] GitHub OAuth integration
- [ ] OAuth state management (Redis)
- [ ] PKCE implementation
- [ ] Link OAuth to existing users
- [ ] OAuth enable/disable in project settings

**Week 6: MFA**
- [ ] MFA for organization members
- [ ] MFA for end users (per project settings)
- [ ] TOTP setup endpoint
- [ ] TOTP verification
- [ ] Recovery codes generation
- [ ] Recovery code usage
- [ ] MFA challenge during login

**Week 7: RBAC**
- [ ] Organization-level roles (OWNER, ADMIN, MEMBER)
- [ ] Project-level roles (customizable per project)
- [ ] Permissions table and seeding
- [ ] Role-permission assignment
- [ ] User-role assignment
- [ ] Permission check in Gateway
- [ ] Default project roles seeding

**Week 8: User & Session Management**
- [ ] Organization member management (invite, remove, roles)
- [ ] End user management (per project)
- [ ] Session listing endpoint
- [ ] Session revocation
- [ ] "Revoke all sessions" functionality
- [ ] User profile management
- [ ] User settings management

#### Phase 3: Frontend (Weeks 9-12)

**Week 9: React SDK Foundation**
- [ ] Initialize frontend/packages/sdk-react package
- [ ] AuctoritasProvider component
- [ ] Auth context and state management
- [ ] API client with interceptors
- [ ] Token storage (memory + refresh)
- [ ] Build and bundle configuration
- [ ] Package publishing setup

**Week 10: SDK Components**
- [ ] SignIn component
- [ ] SignUp component
- [ ] OAuthButton component
- [ ] UserButton component
- [ ] MfaSetup component
- [ ] useAuth hook
- [ ] useUser hook
- [ ] usePermissions hook
- [ ] ProtectedRoute component

**Week 11: Dashboard Auth + Navigation**
- [ ] Initialize frontend/apps/dashboard app
- [ ] Login page
- [ ] Organization selector
- [ ] Project selector
- [ ] Main layout with navigation
- [ ] MFA challenge page
- [ ] Profile page
- [ ] Security page (password, MFA, sessions)
- [ ] Real-time session notifications

**Week 12: Dashboard Admin + E2E Setup**
- [ ] Users list page (with search, filter, pagination)
- [ ] User detail page
- [ ] Role assignment UI
- [ ] Roles list page
- [ ] Role detail page (permission assignment)
- [ ] Create role page
- [ ] Settings page (project config)
- [ ] Webhooks management page
- [ ] Navigation guards (permission-based)
- [ ] Playwright setup + auth fixtures
- [ ] Basic E2E tests (login, logout)

#### Phase 4: Production (Weeks 13-16)

**Week 13: Worker Service**
- [ ] Create backend/services/worker-service module
- [ ] RabbitMQ consumer setup
- [ ] Resend SDK integration
- [ ] Email templates (welcome, verify, reset, alert)
- [ ] Handle user.registered event
- [ ] Handle email verification event
- [ ] Handle password reset event
- [ ] Handle new device login event
- [ ] Webhook delivery system
- [ ] Webhook retry logic

**Week 14: Integration & E2E Testing**
- [ ] E2E tests: user management flows
- [ ] E2E tests: role assignment flows
- [ ] E2E tests: MFA setup flow
- [ ] E2E tests: OAuth login flows
- [ ] E2E tests: session management
- [ ] Load testing (basic)
- [ ] Security testing (OWASP basics)
- [ ] Fix bugs from testing
- [ ] Audit logging implementation
- [ ] Project webhook delivery tests
- [ ] CI pipeline for E2E tests

**Week 15: Multi-Tenancy + Deployment**
- [ ] Verify org/project isolation
- [ ] API key scoping validation
- [ ] Dashboard: API Keys page
- [ ] Setup Hetzner server
- [ ] Install k3s
- [ ] Configure DNS (api.auctoritas.dev, dashboard.auctoritas.dev)
- [ ] Setup cert-manager + Let's Encrypt
- [ ] Deploy PostgreSQL, Redis, RabbitMQ

**Week 16: Deploy + Polish**
- [ ] Deploy all services
- [ ] Deploy dashboard (static)
- [ ] Verify structured logging works in production
- [ ] Test correlation ID tracing across services
- [ ] Verify end-to-end flow in production
- [ ] API documentation (OpenAPI spec)
- [ ] SDK documentation (README, examples)
- [ ] README and setup guide
- [ ] Final testing
- [ ] Publish SDK to npm
- [ ] Demo video/screenshots

---

## 17. Appendix

### 17.1 Glossary

| Term | Definition |
|------|------------|
| **Organization** | Top-level entity representing a company/team using Auctoritas |
| **Org Member** | Developer/admin who manages projects via the dashboard |
| **Project** | Isolated environment within an organization (e.g., Production, Staging) |
| **End User** | Customer of an app built using Auctoritas (authenticates via SDK) |
| **API Key** | Secret key identifying a project, used by SDK for authentication |
| **RBAC** | Role-Based Access Control |
| **TOTP** | Time-based One-Time Password (MFA method) |
| **PKCE** | Proof Key for Code Exchange (OAuth security) |
| **JWT** | JSON Web Token |
| **k3s** | Lightweight Kubernetes distribution |

### 17.2 Key Changes from Original Architecture

| Aspect | Before | After |
|--------|--------|-------|
| **Top-level entity** | Tenant | Organization |
| **User isolation** | Per tenant | Per project |
| **Dashboard users** | Same as end users | Separate: org members |
| **API key scope** | Per tenant | Per project |
| **Settings scope** | Per tenant | Per project |
| **RBAC** | Single level | Two levels: org + project |
| **JWT types** | One (end user) | Two: org member + end user |
| **URL structure** | `/tenants/current/*` | `/org/{orgId}/projects/{projectId}/*` |

### 17.3 References

- [Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security](https://docs.spring.io/spring-security/reference/)
- [Spring Cloud Gateway](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)
- [Spring AMQP](https://docs.spring.io/spring-amqp/docs/current/reference/html/)
- [OAuth 2.0](https://oauth.net/2/)
- [TOTP RFC 6238](https://datatracker.ietf.org/doc/html/rfc6238)
- [Argon2](https://github.com/P-H-C/phc-winner-argon2)
- [Resend](https://resend.com/docs)
- [k3s](https://docs.k3s.io/)
- [shadcn/ui](https://ui.shadcn.com/)

### 17.4 License

Auctoritas is licensed under the **Elastic License 2.0** (ELv2).

**You CAN:**
- View, read, and learn from the source code
- Self-host for your own organization
- Use in your commercial applications and products
- Modify, fork, and create derivative works
- Contribute back to the project

**You CANNOT:**
- Offer Auctoritas as a hosted/managed authentication service
- Resell access to Auctoritas functionality as a service
- Remove or obscure licensing, copyright, or other notices

**Why this license?**

Auctoritas is "source-available" rather than "open source" (by OSI definition). This protects the project from cloud providers or competitors offering it as a competing SaaS without contributing back, while still allowing:
- Developers to learn from and use the code
- Companies to self-host for internal use
- Integration into commercial products

**Commercial licensing:**

Want to offer authentication services using Auctoritas? Contact for a commercial license discussion: license@auctoritas.dev

**Full license text:** See `LICENSE` file in repository root.

**Reference:** [Elastic License 2.0](https://www.elastic.co/licensing/elastic-license)

### 17.5 Document History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | Dec 2024 | Initial Phase 1 architecture |
| 1.1 | Dec 2024 | Added Multi-Tenancy section |
| 1.2 | Dec 2024 | Simplified observability |
| 1.3 | Dec 2024 | Enhanced frontend: React Query, WebSocket, Storybook, Playwright |
| 1.4 | Dec 2024 | Added licensing section (Elastic License 2.0) |
| 2.0 | Dec 2024 | **Major update: Organization -> Projects model**, merged architecture documents, fixed ASCII diagrams |

---

**End of Document**
