# AGENTS.md - Java Development Best Practices

**Use Java 25 (LTS)**

## Language Features

- **Records** - Immutable data classes with compact syntax for value objects
- **var / final var** - Type inference with `var`, immutability with `final var`, explicit types when unclear
- **Sealed Classes + Pattern Matching** - Controlled class hierarchies with exhaustive switch expressions
- **Text Blocks** - Multi-line strings with automatic indentation stripping for SQL, JSON, etc.
- **Flexible Constructor Bodies** - Constructor logic before field assignment for validation

## SOLID Principles

- **Single Responsibility Principle** - One reason to change per class
- **Open/Closed Principle** - Extend behavior via interfaces, don't modify existing code
- **Liskov Substitution Principle** - Subtypes must be substitutable for their base types
- **Interface Segregation Principle** - Small, focused interfaces rather than large monolithic ones
- **Dependency Inversion Principle** - Depend on abstractions, not concrete implementations

## DRY

- Extract validation logic into value objects
- Use template method pattern for shared workflows
- Create utility classes as `final class` with private constructor for common operations

## Best Practices

- **Immutability** - Make classes `final` with `private final` fields and no setters
- **Null Safety** - Return `Optional`, use `Objects.requireNonNull()` for validation, use `orElseThrow()` for defaults
- **Exceptions** - Create specific exception types, never swallow exceptions silently, use try-with-resources
- **Magic Values** - Replace literal numbers and strings with named constants or enums
- **Methods** - Maximum 3 parameters, keep methods small and focused, use self-documenting boolean parameters

## Concurrency (Java 21+)

- **Virtual Threads** - Lightweight threads managed by the JVM for high-throughput concurrent operations
- **Scoped Values** - Thread-local like data sharing across the call stack without thread-local overhead
