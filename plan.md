# Auctoritas v0.2.0 Implementation Plan

## Overview

This document outlines the step-by-step implementation plan for **v0.2.0 - Organization Registration**, following the established commit message patterns from the existing git history. The plan is divided into 29 atomic commits organized into 9 phases.

**Theme:** "First User Can Sign Up"  
**Duration:** ~2 weeks  
**Demo:** Register organization via API, receive JWT

---

## Current Status Summary

### Completed (v0.1.0 + v0.1.1)
- ✅ Project foundation with monorepo structure
- ✅ Database entities & repositories (organizations, projects, API keys)
- ✅ Flyway migrations V1-V2
- ✅ Test coverage with Testcontainers
- ✅ JWT, BouncyCastle, Argon2 dependencies
- ✅ Config classes (JwtConfig, PasswordConfig)
- ✅ Core DTOs (ApiResponse, AuthTokens, JwtClaims)
- ✅ V3 migration for refresh tokens

### Completed (v0.2.0 - Phase 1 & 2)
- ✅ **Phase 1: Foundation Infrastructure (5 commits)**
  - Commit 1: JWT, BouncyCastle, Argon2 dependencies
  - Commit 2: JWT and password policy configuration
  - Commit 3: Core DTOs (ApiResponse, ErrorDetail, AuthTokens, JwtClaims)
  - Commit 4: Utility classes (KeyPairUtils, SecureRandomUtils, ValidationUtils)
  - Commit 5: Database indexes (V4 migration)

- ✅ **Phase 2: Common Library Services (3 commits) - 98 TESTS PASSING**
  - Commit 6: JwtService for RS256 token generation/validation
  - Commit 7: PasswordValidator for password policy enforcement
  - Commit 8: GlobalExceptionHandler with custom exceptions

### In Progress (v0.2.0 branch)
- Currently at commit `2a42a4a` with Phase 2 complete
- Remaining: Auth Service Business Logic (Commits 9-12)

---

## API Endpoints (v0.2.0)

### Organization Registration (Public)
```
POST /api/v1/org/register
     Body: { organizationName, slug, email, password, name }
     Response: { organization, member, accessToken, refreshToken }

GET /api/v1/org/check-slug?slug=acme
     Response: { available: true }
```

### Organization Member Auth (Public)
```
POST /api/v1/org/auth/login
     Body: { email, password }
     Response: { member, accessToken, refreshToken }
              OR { mfaRequired: true, mfaToken }

POST /api/v1/org/auth/logout
     Headers: Authorization: Bearer <token>
     Response: { message: "Logged out" }

POST /api/v1/org/auth/refresh
     Body: { refreshToken }
     Response: { accessToken, refreshToken }

POST /api/v1/org/auth/mfa/verify
     Body: { mfaToken, code }
     Response: { member, accessToken, refreshToken }
```

### Organization Management (Protected)
```
GET /api/v1/org/{orgId}
     Response: { organization, memberCount, projectCount }

PUT /api/v1/org/{orgId}
     Body: { name }
     Response: { organization }

DELETE /api/v1/org/{orgId}
     Response: { message: "Project deleted" }
```

---

## Implementation Plan - Step-by-Step Commits

### **Phase 1: Foundation Infrastructure (5 commits)**

#### **Commit 1: feat(common): add JWT, password encoder, and security dependencies**
- Add to `backend/libs/common/pom.xml`:
  - `com.auth0:java-jwt` 4.4.0 (JWT signing/verification)
  - `org.bouncycastle:bcprov-jdk18on` 1.78.1 (RSA key generation)
  - `org.bouncycastle:bcpkix-jdk18on` 1.78.1 (PKIX utilities)
  - `de.mkammerer:argon2-jvm` 2.11 (password hashing)
- **Rationale:** Foundation for JWT service and password validation

#### **Commit 2: feat(common): add Spring configuration properties for JWT and password policy**
- Create `JwtConfig.java` with:
  - KeyConfig: key paths, key size (2048)
  - TokenConfig: access/refresh TTL, issuer
- Create `PasswordConfig.java` with:
  - Policy nested class: min/max length, character requirements
  - Failed attempt tracking, lockout duration
- **Rationale:** Externalized configuration following Spring Boot best practices

#### **Commit 3: feat(common): add core DTOs for standardized API responses and JWT**
- Create `ApiResponse<T>` record with factory methods (success/error)
- Create `ErrorDetail` record (code, message, optional field)
- Create `AuthTokens` record (accessToken, refreshToken, expiresIn)
- Create `JwtClaims` record (subject, orgId, role, type, issuer)
- Add unit tests for DTOs
- **Rationale:** Consistent API contract across all endpoints

#### **Commit 4: feat(common): add utility classes for JWT keys, secure random, and validation**
- Create `KeyPairUtils`:
  - Generate RSA key pair (2048-bit)
  - Save/load PEM format
  - Initialize default keys if missing
- Create `SecureRandomUtils`:
  - Generate secure random strings (URL-safe Base64)
  - Generate cryptographically secure bytes
- Create `ValidationUtils`:
  - Email validation (regex pattern)
  - Slug validation (alphanumeric, hyphens, 3-30 chars)
- **Rationale:** Reusable utilities for security operations

#### **Commit 5: feat(db): add database indexes for optimized queries**
- Create migration file `V3__add_database_indexes.sql`:
  - Index on `organization_members(email, org_id)` for login lookups
  - Index on `organizations(slug)` (verify if exists in V1)
  - Index on `api_keys(hashed_key)` for API key validation
  - Index on `org_member_refresh_tokens(token_hash)` for token lookup
- **Rationale:** Query performance optimization for auth flows

---

### **Phase 2: Common Library Services (3 commits) - COMPLETED ✅**

#### **Commit 6: feat(common): add JwtService for RS256 token generation and validation**
- Create `JwtService`:
  - `generateToken(JwtClaims claims)` → signed JWT string
  - `validateToken(String token)` → boolean with RS256 verification
  - `extractClaims(String token)` → JwtClaims object
  - Load RSA keys from configured paths
  - Handle missing keys by auto-generating on startup
- Create `JwtPrincipal` record:
  - Principal extraction from JWT claims
  - Type checking (isOrgMember, isEndUser)
  - Permission helpers (hasPermission)
- Add JwtServiceTest with 17 unit tests
- Fix KeyPairUtils for BouncyCastle PEMKeyPair compatibility
- **Rationale:** Centralized JWT logic with RS256 signing

#### **Commit 7: feat(common): add PasswordValidator for policy enforcement**
- Create `ValidationError` enum with 10 error types
- Create `ValidationResult` record with factory methods
- Create `PasswordValidator`:
  - Length validation (8-128 characters)
  - Character requirements (uppercase, lowercase, digit, special char)
  - Common password detection (blacklist of 10 common passwords)
  - Sequential character detection (4+ consecutive chars)
  - Repeated character detection (5+ consecutive chars)
  - Whitespace detection
- Add PasswordValidatorTest with 52 unit tests
- **Rationale:** Enforce password security policy consistently

#### **Commit 8: feat(common): add global exception handler and custom exceptions**
- Create exception classes:
  - `AuthException` (authentication failures with error code, details, cause)
  - `ValidationException` (validation errors with ErrorDetail list)
  - `ServiceException` (business logic errors with resource info)
- Create `GlobalExceptionHandler` with `@ControllerAdvice`:
  - Handle all custom exceptions → ApiResponse with error
  - Handle `MethodArgumentNotValidException` → field validation errors
  - Handle Spring Security exceptions → 401 responses
  - Handle generic `Exception` → 500 with error detail
- Add GlobalExceptionHandlerTest with 24 unit tests
- **Rationale:** Consistent error responses across application

---

### **Phase 3: Auth Service - Business Logic (4 commits) - NEXT PHASE 🔄**

#### **Commit 9: feat(auth): add OrganizationService with registration and CRUD**
- Create `OrganizationService`:
  - `register(OrganizationRegistrationRequest req)` → RegistrationResult
  - `findBySlug(String slug)` → Optional<Organization>
  - `findById(UUID id)` → Optional<Organization>
  - `update(UUID id, UpdateRequest req)` → Organization
  - `isSlugAvailable(String slug)` → boolean
  - Delete with authorization check
- Implement transactional registration (org + member + initial session)
- Integrate with PasswordValidator for password policy
- Use Argon2PasswordEncoder for password hashing
- Add unit tests with Testcontainers
- **Rationale:** Core business logic for organization management

#### **Commit 10: feat(auth): add RefreshTokenService for token management**
- Create `RefreshTokenService`:
  - `create(UUID memberId, HttpServletRequest request)` → RefreshToken
  - `findByToken(String token)` → Optional<RefreshToken>
  - `revoke(String token)` → void
  - `revokeAllForMember(UUID memberId)` → void
  - `cleanupExpired()` → scheduled job
- Token format: `rt_<32-char base64>`
- SHA-256 hashing before storage
- Device fingerprinting (browser family, OS from User-Agent)
- Create `OrgMemberRefreshTokenRepository`
- Add unit tests
- **Rationale:** Secure refresh token management with rotation support

#### **Commit 11: feat(auth): add OrgMemberService for authentication flows**
- Create `OrgMemberService`:
  - `login(String email, String password, UUID orgId, HttpServletRequest req)` → LoginResult
  - `logout(UUID memberId, String sessionId)` → void
  - `refresh(String refreshToken, HttpServletRequest req)` → AuthTokens
  - `verifyMfa(String mfaToken, String code)` → AuthTokens
  - `lockMember(UUID memberId, Duration lockoutDuration)` → void
- Failed login tracking with Redis (5 attempts in 15 min)
- Password verification with Argon2
- Session creation with device info and IP
- JWT generation with `type: "org_member"` claims
- MFA challenge flow (return `mfaToken` if enabled)
- Add comprehensive unit tests
- **Rationale:** Authentication core for org members

#### **Commit 12: feat(auth): add request and response DTOs for org authentication**
- Create request DTOs:
  - `OrganizationRegistrationRequest` (with validation annotations)
  - `OrgLoginRequest`
  - `RefreshTokenRequest`
  - `MfaVerifyRequest`
  - `OrganizationUpdateRequest`
- Create response DTOs:
  - `OrganizationRegistrationResponse` (org, member, tokens)
  - `LoginResponse` (member, tokens, mfaRequired, mfaToken)
  - `OrganizationDetailsResponse`
- Create record for `RegistrationResult` (internal use)
- **Rationale:** Type-safe API contracts with validation

---

### **Phase 4: REST Controllers (2 commits)**

#### **Commit 13: feat(auth): add OrganizationController for registration and management**
- Create `OrganizationController`:
  - `POST /api/v1/org/register` → 201 with registration response
  - `GET /api/v1/org/check-slug` → 200 with availability
  - `GET /api/v1/org/{orgId}` → 200 with org details (auth required)
  - `PUT /api/v1/org/{orgId}` → 200 with updated org (auth required)
  - `DELETE /api/v1/org/{orgId}` → 204 (owner/admin only)
- Use `@Valid` for request validation
- Return `ApiResponse<T>` for consistency
- Add `@PreAuthorize` for protected endpoints
- Add integration tests with MockMvc
- **Rationale:** RESTful organization management API

#### **Commit 14: feat(auth): add OrgAuthController for authentication endpoints**
- Create `OrgAuthController`:
  - `POST /api/v1/org/auth/login` → 200 with tokens or MFA challenge
  - `POST /api/v1/org/auth/logout` → 200
  - `POST /api/v1/org/auth/refresh` → 200 with new tokens
  - `POST /api/v1/org/auth/mfa/verify` → 200 with tokens
- Extract `JwtPrincipal` from SecurityContext
- Validate JWT for protected endpoints
- Handle MFA required scenario
- Add integration tests
- **Rationale:** Authentication API for org members

---

### **Phase 5: Security Configuration (2 commits)**

#### **Commit 15: feat(auth): configure Spring Security with Argon2 password encoder**
- Update `SecurityConfig`:
  - Configure `PasswordEncoder` with Argon2id parameters
  - Set session policy to STATELESS
  - CSRF disabled (JWT-based auth)
  - CORS enabled (configure origins)
- Add password encoder bean:
  ```java
  Argon2PasswordEncoder(16, 32, 1, 65536, 3)
  ```
- **Rationale:** Secure password hashing with industry-standard Argon2

#### **Commit 16: feat(auth): add JWT authentication filter and security rules**
- Create `JwtAuthenticationFilter`:
  - Extract Bearer token from Authorization header
  - Validate with JwtService
  - Set Authentication in SecurityContext
  - Handle invalid tokens (401 response)
- Update `SecurityConfig`:
  - Public endpoints: `/actuator/**`, `/api/v1/org/register`, `/api/v1/org/check-slug`, `/api/v1/org/auth/login`, `/api/v1/org/auth/refresh`
  - Protected: `/api/v1/org/**` (requires valid JWT)
  - Add `@EnableMethodSecurity` for `@PreAuthorize`
- Create `UnauthorizedEntryPoint` for 401 responses
- **Rationale:** JWT-based stateless authentication

---

### **Phase 6: Gateway Service (4 commits)**

#### **Commit 17: feat(gateway): add dependencies for gateway routing and rate limiting**
- Add to `gateway-service/pom.xml`:
  - `spring-cloud-starter-gateway`
  - `spring-boot-starter-data-redis-reactive`
  - `bucket4j-core` 8.7.0
  - `bucket4j-jcache` 8.7.0
  - `ehcache` 3.10.8 (for bucket4j)
- **Rationale:** Required libraries for gateway features

#### **Commit 18: feat(gateway): configure route definitions for org endpoints**
- Create `GatewayConfig`:
  - Public routes: `/api/v1/org/register`, `/api/v1/org/check-slug`
  - Auth routes: `/api/v1/org/auth/**` with rate limiting
  - Protected routes: `/api/v1/org/**` with JWT validation
  - All routes forward to `lb://auth-service`
- **Rationale:** Centralized routing configuration

#### **Commit 19: feat(gateway): add rate limiting filter with Redis**
- Create `RateLimitFilter`:
  - Extract client IP from request
  - Implement token bucket algorithm
  - Store bucket state in Redis
  - Return 429 Too Many Requests when limit exceeded
  - Different limits for public (100 req/min) vs auth (50 req/min)
- Create `RedisRateLimiterConfig`:
  - Configure bucket4j with Redis backend
  - Set replenish rate and burst capacity
- Add `RedisConfiguration` bean for reactive Redis
- **Rationale:** Prevent API abuse with Redis-backed rate limiting

#### **Commit 20: feat(gateway): configure CORS for dashboard origins**
- Create `CorsConfig`:
  - Allow credentials: true
  - Origins: `http://localhost:5173`, `https://dashboard.auctoritas.dev`
  - Headers: all headers allowed
  - Methods: all methods allowed
  - Apply to `/api/**` paths
- Create `CorsWebFilter` bean
- **Rationale:** Enable cross-origin requests from dashboard

---

### **Phase 7: Configuration & Infrastructure (3 commits)**

#### **Commit 21: chore(config): add JWT and password policy configuration**
- Update `application-dev.yml`:
  - Add `app.jwt.keys` configuration
  - Add `app.jwt.token` TTL settings
  - Add `app.password.policy` rules
- Update `application-prod.yml`:
  - Use environment variables for JWT key paths
  - Production-secure defaults (shorter TTL)
  - Stronger password requirements
- Update `application-test.yml`:
  - Test configuration with in-memory keys
- **Rationale:** Environment-specific configuration

#### **Commit 22: chore(infra): add RSA key generation script and keys directory**
- Create `infra/scripts/generate-jwt-keys.sh`:
  - Generate 2048-bit RSA private key
  - Extract public key
  - Save to PEM format in `keys/` directory
  - Set proper permissions (600 for private, 644 for public)
- Create `.gitignore` entry for `keys/` directory (optional)
- Add documentation in `docs/` about key generation
- **Rationale:** Setup for JWT signing keys

#### **Commit 23: chore(docker): add Redis to docker-compose and update service configs**
- Update `docker-compose.yml`:
  - Add Redis 8.4 service with persistence
  - Update auth-service to depend on Redis
- Update auth-service Dockerfile:
  - Copy keys from build context
  - Set JWT key paths as environment variables
- Add environment variables for JWT paths in docker-compose
- **Rationale:** Redis infrastructure for rate limiting and sessions

---

### **Phase 8: Testing & Quality (4 commits)**

#### **Commit 24: test(auth): add unit tests for authentication services**
- Create `OrganizationServiceTest`:
  - Test registration success/failure
  - Test slug validation
  - Test duplicate email handling
- Create `OrgMemberServiceTest`:
  - Test login with valid credentials
  - Test login with invalid credentials
  - Test failed login lockout
  - Test token refresh
  - Test logout
- Create `PasswordValidatorTest`:
  - Test valid passwords
  - Test invalid passwords (various scenarios)
- Create `JwtServiceTest`:
  - Test token generation
  - Test token validation
  - Test claims extraction
- **Rationale:** Comprehensive unit test coverage

#### **Commit 25: test(auth): add integration tests for authentication flows**
- Create `OrganizationRegistrationIT`:
  - Test full registration flow
  - Verify database state
  - Verify JWT token generation
- Create `OrgLoginIT`:
  - Test login flow
  - Test session creation
  - Test refresh token flow
- Create `AuthFlowE2ETest`:
  - Register org
  - Login as owner
  - Refresh token
  - Logout
- Use `@SpringBootTest` with Testcontainers PostgreSQL
- **Rationale:** End-to-end flow testing

#### **Commit 26: test(auth): add API endpoint tests**
- Create `OrganizationControllerTest`:
  - Test registration endpoint
  - Test slug check endpoint
  - Test protected endpoints with/without auth
- Create `OrgAuthControllerTest`:
  - Test login endpoint
  - Test logout endpoint
  - Test refresh endpoint
  - Test MFA verify endpoint
- Use `MockMvc` for HTTP layer testing
- **Rationale:** API contract validation

#### **Commit 27: chore(ci): add code quality checks**
- Add Checkstyle configuration
- Add SpotBugs plugin to parent POM
- Configure JaCoCo for code coverage (target 80%)
- Add Maven build phases for quality checks
- Update GitHub Actions CI to run quality checks
- **Rationale:** Maintain code quality standards

---

### **Phase 9: Documentation & Release (2 commits)**

#### **Commit 28: docs: update documentation for v0.2.0 features**
- Update `docs/Roadmap.md`:
  - Mark v0.2.0 as complete
  - Update completion status
- Update `README.md`:
  - Add v0.2.0 API endpoints section
  - Add curl examples for registration and login
  - Update getting started guide
- Add `docs/API.md`:
  - Document all `/api/v1/org` endpoints
  - Include request/response examples
  - Document authentication flow
- Add `docs/ARCHITECTURE.md` updates:
  - Document JWT token structure
  - Document refresh token flow
  - Document rate limiting strategy
- **Rationale:** Complete documentation for v0.2.0

#### **Commit 29: chore(release): prepare v0.2.0 release**
- Update version in `backend/pom.xml` to `0.2.0`
- Create `CHANGELOG.md`:
  - Add v0.2.0 section with features
  - List breaking changes (none for v0.2)
  - List migration notes
- Create `RELEASE_NOTES.md`:
  - Summary of v0.2.0 features
  - Installation/upgrade instructions
  - Known issues
- **Rationale:** Release preparation

---

## Summary

### Total: 29 atomic commits organized into 9 phases

| Phase | Commits | Status | Focus |
|-------|---------|--------|-------|
| 1. Foundation Infrastructure | 5 | ✅ Complete | Dependencies, config, DTOs, utils, DB indexes |
| 2. Common Library Services | 3 | ✅ Complete | JwtService, PasswordValidator, ExceptionHandler |
| 3. Auth Service Business Logic | 4 | 🔄 In Progress | OrgService, RefreshTokenService, OrgMemberService, DTOs |
| 4. REST Controllers | 2 | ⏳ Pending | OrganizationController, OrgAuthController |
| 5. Security Configuration | 2 | ⏳ Pending | Password encoder, JWT filter |
| 6. Gateway Service | 4 | ⏳ Pending | Dependencies, routing, rate limiting, CORS |
| 7. Configuration & Infrastructure | 3 | ⏳ Pending | Config files, key generation, Docker |
| 8. Testing & Quality | 4 | ⏳ Pending | Unit tests, integration tests, API tests, CI checks |
| 9. Documentation & Release | 2 | ⏳ Pending | Docs, changelog, release notes |

### Current Progress
- **Total Commits:** 8 of 29 (27%)
- **Tests Passing:** 98 (all modules)

### Commit Message Format

All commits follow this format (as established in git history):

```
<scope>: <short description>

Detailed explanation of changes
- Specific item 1
- Specific item 2
```

**Scope prefixes:**
- `feat` - New features
- `fix` - Bug fixes
- `chore` - Maintenance tasks
- `docs` - Documentation
- `test` - Tests
- `refactor` - Code refactoring
- `eat` - Existing code transformation

### Key Principles

1. **Atomic Commits:** Each commit leaves codebase in a compilable state
2. **Clear Scope:** Focused, single-purpose changes
3. **Testable:** Changes can be tested independently
4. **Documented:** Configuration and utilities are well-documented
5. **Best Practices:** Follows Spring Boot 4.0+ and Java 21 standards

---

## Industry Best Practices Applied

1. **Layered Architecture:** Controller → Service → Repository separation
2. **DTO Pattern:** Separate request/response DTOs from entities
3. **Immutability:** Record-based DTOs (Java 16+)
4. **Dependency Injection:** Constructor injection with `@RequiredArgsConstructor`
5. **Transaction Management:** `@Transactional` on service methods
6. **Security:** Argon2id password hashing, RS256 JWT, stateless sessions
7. **Error Handling:** Global exception handler with standardized responses
8. **Validation:** Bean Validation (`@Valid`, `@NotBlank`, etc.)
9. **Rate Limiting:** Token bucket algorithm with Redis
10. **Testing:** Unit + integration + API tests with Testcontainers
11. **Configuration:** Externalized via `@ConfigurationProperties`
12. **Logging:** Structured JSON logging with correlation IDs
13. **Database:** Flyway migrations, proper indexes, foreign keys
14. **API Design:** RESTful, proper HTTP status codes, consistent response format

---

## Acceptance Criteria

```bash
# Register organization
curl -X POST http://localhost:8080/api/v1/org/register \
  -H "Content-Type: application/json" \
  -d '{"organizationName":"Acme Corp","slug":"acme","email":"owner@acme.com","password":"SecurePass123!","name":"John"}'

# Login
curl -X POST http://localhost:8080/api/v1/org/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"owner@acme.com","password":"SecurePass123!"}'

# Access protected route with JWT
curl http://localhost:8080/api/v1/org/me \
  -H "Authorization: Bearer <token>"
```

---

## Dependencies

### Internal
- v0.1.0 - Project Foundation
- v0.1.1 - Database Foundation

### External
- Spring Boot 4.0.1
- Spring Security 4.0
- Spring Cloud Gateway 2025.0.1
- PostgreSQL 18
- Redis 8.4
- RabbitMQ 4.2
- Java 21

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Key generation failures | Auto-generate keys on startup if missing |
| Database performance | Proper indexing on frequently queried columns |
| Rate limit bypass | Use Redis for distributed rate limiting |
| Token theft | Short-lived access tokens (30 min), refresh token rotation |
| Password breaches | Argon2id with strong parameters, password policy |
| Session hijacking | Device fingerprinting, IP tracking |

---

## Next Steps (v0.2.1)

After v0.2.0 is complete:
- v0.2.1 - Project Management (project CRUD, API keys)
- v0.3.0 - End User Authentication (SDK authentication)

---

*Last Updated: January 2026*  
*Version: 1.1 - Phase 2 Complete*

---

## Phase 2 Completion Details

### Files Created (Phase 2)

**Commit 6: JwtService**
```
backend/libs/common/src/main/java/dev/auctoritas/common/service/JwtService.java
backend/libs/common/src/main/java/dev/auctoritas/common/security/JwtPrincipal.java
backend/libs/common/src/test/java/dev/auctoritas/common/service/JwtServiceTest.java
```

**Commit 7: PasswordValidator**
```
backend/libs/common/src/main/java/dev/auctoritas/common/validation/ValidationError.java
backend/libs/common/src/main/java/dev/auctoritas/common/validation/ValidationResult.java
backend/libs/common/src/main/java/dev/auctoritas/common/validation/PasswordValidator.java
backend/libs/common/src/test/java/dev/auctoritas/common/validation/PasswordValidatorTest.java
```

**Commit 8: GlobalExceptionHandler**
```
backend/libs/common/src/main/java/dev/auctoritas/common/exception/AuthException.java
backend/libs/common/src/main/java/dev/auctoritas/common/exception/ValidationException.java
backend/libs/common/src/main/java/dev/auctoritas/common/exception/ServiceException.java
backend/libs/common/src/main/java/dev/auctoritas/common/exception/GlobalExceptionHandler.java
backend/libs/common/src/test/java/dev/auctoritas/common/exception/GlobalExceptionHandlerTest.java
```

### Files Modified (Phase 2)
```
backend/libs/common/src/main/java/dev/auctoritas/common/util/KeyPairUtils.java
backend/libs/common/pom.xml (added web, security, mockito dependencies)
```

### Test Results
```
Tests run: 98, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
