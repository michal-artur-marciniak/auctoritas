# Auth Service Development Notes

## MFA Implementation Patterns

### Adding New MFA Use Cases

When implementing MFA features, follow this hexagonal architecture pattern:

1. **Domain Layer** (`domain/mfa/`)
   - Aggregates handle business logic (e.g., `EndUserMfa.create()`, `enable()`, `disable()`)
   - Value objects validate data (e.g., `TotpSecret`, `TotpCode`)
   - Domain events are registered in aggregates and published by application layer

2. **Application Layer** (`application/`)
   - Use case interfaces go in `port/in/mfa/`
   - Use case implementations are named `{Action}Service` (e.g., `SetupEndUserMfaService`)
   - DTOs go in `application/mfa/` (e.g., `SetupMfaResult`)
   - Services validate API keys, load domain objects, delegate to aggregates, persist, and publish events

3. **Adapter Layer** (`adapter/`)
   - Controllers go in `adapter/in/web/` and use `@AuthenticationPrincipal EndUserPrincipal`
   - Outbound adapters go in `adapter/out/mfa/`
   - All adapters depend on application ports, not concrete implementations

### Key Dependencies

- **Encryption**: Use `EncryptionPort` for TOTP secrets (AES-256-GCM) and recovery codes
- **QR Codes**: Use `QrCodeGeneratorPort` to generate authenticator app QR codes
- **API Key Validation**: Always validate API key via `ApiKeyService.validateActiveKey()`
- **Project Validation**: Verify principal's project matches API key's project for security
- **Domain Events**: Publish via `DomainEventPublisherPort` after persisting aggregates

### MFA Setup Flow

1. Validate API key and get project
2. Validate user belongs to project
3. Check MFA doesn't already exist
4. Generate plain TOTP secret + recovery codes
5. Encrypt secret for storage
6. Create `EndUserMfa` aggregate (disabled state)
7. Persist and publish `MfaSetupInitiatedEvent`
8. Generate QR code URL from plain secret
9. Return plain secret + QR code + recovery codes (shown once)

### Testing Requirements

- All MFA endpoints require end-user JWT + X-API-Key
- Run `./gradlew :services:auth-service:test` before committing
- Ensure type safety with no compiler warnings
