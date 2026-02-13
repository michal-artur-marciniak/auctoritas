# API Module

Spring Boot API with Java 25, JWT authentication, and SQLite. Provides authentication and user management services for the OpenAgents Cloud platform.

## Architecture

This API follows a **pragmatic Domain-Driven Design (DDD)** approach with clean separation of concerns:

```
┌─────────────────────────────────────────────────────────────────┐
│  PRESENTATION LAYER  │  AuthController, UserController          │
│  (REST Controllers)  │  GlobalExceptionHandler                  │
├─────────────────────────────────────────────────────────────────┤
│  APPLICATION LAYER   │  RegisterUseCase, LoginUseCase           │
│  (Use Cases)         │  TokenProvider (port interface)          │
├─────────────────────────────────────────────────────────────────┤
│  DOMAIN LAYER        │  User (aggregate root)                   │
│  (Business Logic)    │  Email, Password (value objects)         │
│                      │  UserRepository (port interface)         │
│                      │  PasswordEncoder (port interface)        │
├─────────────────────────────────────────────────────────────────┤
│  INFRASTRUCTURE      │  JpaUserRepositoryAdapter                │
│  (Persistence)       │  PasswordEncoderAdapter (BCrypt)         │
├─────────────────────────────────────────────────────────────────┤
│  INFRASTRUCTURE      │  JwtTokenProvider                        │
│  (Security)          │  JwtAuthenticationFilter, SecurityConfig │
└─────────────────────────────────────────────────────────────────┘
```

### Key Design Decisions

- **No Lombok**: Plain Java with explicit constructors for clarity
- **No Domain Events**: Simplified architecture (can be added later)
- **Value Objects**: `Email` and `Password` enforce invariants at creation
- **Port/Adapter Pattern**: Domain depends on abstractions, not frameworks
- **Java 25 Features**: Records for DTOs, `var` for local variables

## Tech Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| Spring Boot | 4.0.2 | Application framework |
| Java | 25 (LTS) | Language |
| Spring Security | 6.5 | Security & JWT |
| JJWT | 0.12.6 | JWT implementation |
| SQLite | 3.51.1.0 | Database |
| Gradle | Kotlin DSL | Build tool |

## API Endpoints

### Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | Register new user with email/password | No |
| POST | `/api/auth/login` | Login and receive JWT tokens | No |
| POST | `/api/auth/refresh` | Refresh access token | No |
| GET | `/api/auth/oauth/github` | OAuth login with GitHub | No |
| GET | `/api/auth/oauth/google` | OAuth login with Google | No |

### User

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/user/me` | Get current user profile | Yes |

### Sessions

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/sessions` | Create session | Yes |
| GET | `/api/sessions` | List active sessions | Yes |
| PATCH | `/api/sessions/{sessionId}` | Extend session | Yes |
| DELETE | `/api/sessions/{sessionId}` | Revoke session | Yes |

### Sessions

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/sessions` | Create session | Yes |
| GET | `/api/sessions` | List active sessions | Yes |
| PATCH | `/api/sessions/{sessionId}` | Extend session | Yes |
| DELETE | `/api/sessions/{sessionId}` | Revoke session | Yes |

### Public

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/hello` | Hello message |
| GET | `/api/health` | Health check |

## Authentication

The API uses **JWT (JSON Web Tokens)** for stateless authentication:

1. **Register** or **Login** to receive access and refresh tokens
2. Include the access token in subsequent requests: `Authorization: Bearer <token>`
3. Access tokens expire after 24 hours; refresh tokens after 7 days

### Example Requests

```bash
# Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123","name":"John Doe"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'

# Refresh access token
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refresh-token>"}'

# Get user profile (with JWT token)
curl http://localhost:8080/api/user/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

## Configuration

### Database

SQLite database is automatically created at `./data/app.db` (relative to project root) on first run.

### JWT Secret

Set the JWT signing secret in `src/main/resources/application.yml`:

```yaml
jwt:
  secret: your-super-secret-key-here-change-in-production
  expiration: 86400000  # 24 hours in milliseconds
  refresh-expiration: 604800000  # 7 days in milliseconds
```

**Important**: Change the default secret in production!

### Server Port

Default port is `8080`. Change in `application.yml`:

```yaml
server:
  port: 8080
```

## Development

### Prerequisites

- Java 25 (JDK)
- Gradle (or use `./gradlew` wrapper)

### Build & Run

```bash
# Build the project
./gradlew build

# Run in development mode
./gradlew bootRun

# Run tests
./gradlew test

# Create executable JAR
./gradlew bootJar
# Output: build/libs/api-0.0.1-SNAPSHOT.jar
```

### Project Structure

```
src/main/java/com/example/api/
├── ApiApplication.java              # Application entry point
├── application/                     # Application layer (use cases)
│   ├── auth/
│   │   ├── RegisterUseCase.java
│   │   ├── LoginUseCase.java
│   │   ├── RefreshTokenUseCase.java
│   │   ├── OAuthLoginUseCase.java
│   │   ├── TokenProvider.java       # Port interface
│   │   └── dto/
│   │       ├── RegisterRequest.java
│   │       ├── LoginRequest.java
│   │       ├── RefreshTokenRequest.java
│   │       ├── AuthResponse.java
│   │       └── UserDto.java
│   ├── session/
│   │   ├── CreateSessionUseCase.java
│   │   ├── ExtendSessionUseCase.java
│   │   ├── RevokeSessionUseCase.java
│   │   ├── ListActiveSessionsUseCase.java
│   │   └── dto/
│   │       ├── CreateSessionRequest.java
│   │       ├── ExtendSessionRequest.java
│   │       ├── RevokeSessionRequest.java
│   │       └── SessionDto.java
│   └── user/
│       └── GetUserUseCase.java
├── domain/                          # Domain layer (business logic)
│   └── user/
│       ├── User.java                # Aggregate root
│       ├── UserId.java              # Value object
│       ├── Email.java               # Value object
│       ├── Password.java            # Value object
│       ├── Role.java                # Enum
│       ├── UserRepository.java      # Port interface
│       ├── PasswordEncoder.java     # Port interface
│       └── exception/               # Domain exceptions
├── domain/                          # Domain layer (business logic)
│   ├── session/
│   │   ├── Session.java
│   │   ├── SessionId.java
│   │   ├── SessionRepository.java
│   │   └── exception/
│   │       └── SessionNotFoundException.java
│   └── subscription/
│       ├── Subscription.java
│       ├── SubscriptionId.java
│       ├── SubscriptionPlan.java
│       ├── SubscriptionStatus.java
│       └── SubscriptionRepository.java
├── infrastructure/                  # Infrastructure layer
│   ├── persistence/                 # Database adapters
│   │   ├── UserJpaEntity.java
│   │   ├── UserJpaRepository.java
│   │   ├── UserDomainMapper.java
│   │   └── JpaUserRepositoryAdapter.java
│   │   ├── SessionJpaEntity.java
│   │   ├── SessionJpaRepository.java
│   │   ├── SessionDomainMapper.java
│   │   └── JpaSessionRepositoryAdapter.java
│   │   ├── SubscriptionJpaEntity.java
│   │   ├── SubscriptionJpaRepository.java
│   │   ├── SubscriptionDomainMapper.java
│   │   └── JpaSubscriptionRepositoryAdapter.java
│   └── security/                    # Security adapters
│       ├── JwtTokenProvider.java
│       ├── JwtAuthenticationFilter.java
│       ├── PasswordEncoderAdapter.java
│       ├── OAuth2UserInfo.java
│       ├── OAuth2UserInfoMapper.java
│       ├── OAuth2AuthenticationSuccessHandler.java
│       ├── OAuth2AuthenticationFailureHandler.java
│       ├── HttpCookieOAuth2AuthorizationRequestRepository.java
│       ├── FrontendCorsProperties.java
│       └── SecurityConfig.java
└── presentation/                    # Presentation layer
    ├── auth/
    │   ├── AuthController.java
    │   └── dto/
    │       ├── RegisterRequestDto.java
    │       ├── LoginRequestDto.java
    │       └── RefreshTokenRequestDto.java
    ├── session/
    │   ├── SessionController.java
    │   └── dto/
    │       ├── CreateSessionRequestDto.java
    │       ├── ExtendSessionRequestDto.java
    │       └── SessionResponseDto.java
    ├── user/
    │   └── UserController.java
    └── exception/
        ├── GlobalExceptionHandler.java
        └── ApiError.java
```

## Domain Model

### User Aggregate

```java
public class User {
    private final UserId id;           // UUID
    private final Email email;         // Validated email
    private final String passwordHash; // BCrypt hash
    private final String name;
    private final Role role;           // USER or ADMIN
    private final Instant createdAt;
    private final boolean banned;
}
```

### Value Objects

- **Email**: Validates format at construction
- **Password**: Enforces minimum 8 characters, creates hash
- **UserId**: UUID wrapper

## Error Handling

The API returns structured error responses:

```json
{
  "timestamp": "2026-02-07T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Email already exists",
  "path": "/api/auth/register"
}
```

### Common HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request (validation error) |
| 401 | Unauthorized (invalid/missing token) |
| 403 | Forbidden (banned user) |
| 404 | Not Found |
| 409 | Conflict (email already exists) |
| 500 | Internal Server Error |

## Security Considerations

- Passwords are hashed with **BCrypt** (strength 10)
- JWT tokens are signed with HMAC-SHA256
- CORS is configured for frontend integration
- SQL injection prevented by JPA/Hibernate
- Input validation using Jakarta Bean Validation

## Future Enhancements

- [x] OAuth2 integration (GitHub, Google)
- [x] Refresh tokens
- [ ] Rate limiting
- [ ] Email verification
- [ ] Password reset
- [ ] Admin endpoints for user management
