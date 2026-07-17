# Design Decisions

This document explains the architectural and design choices made in the **Policy Proposal Processing API**.

---

## 1. Architecture

### Layered Architecture (Controller → Service → Repository)

The application follows a classic three-tier layered architecture:

```
Client → Controller → Service → Repository → In-Memory Store
```

| Layer | Responsibility |
|-------|----------------|
| **Controller** | HTTP mapping, request/response handling, input validation (`@Valid`) |
| **Service** | Business rules, orchestration, domain validation |
| **Repository** | Data persistence abstraction using thread-safe collections |

**Why this approach?**
- Clear separation of concerns — each layer has a single responsibility
- Easy to test business logic independently
- Familiar pattern for enterprise Java/Spring interviews
- Repository layer can be swapped for JPA/database later without changing service logic

---

## 2. In-Memory Storage with Thread Safety

### Choice: `ConcurrentHashMap` + `AtomicLong`

All data is stored in memory using:
- `ConcurrentHashMap<Long, Entity>` for entity storage
- `AtomicLong` for ID generation

**Why not a database?**
- Requirement explicitly mandates in-memory storage only
- Eliminates external dependencies for demo/interview setup
- Faster startup and simpler deployment

**Thread safety considerations:**
- `ConcurrentHashMap` provides safe concurrent reads and writes
- `AtomicLong` ensures unique, collision-free ID generation under concurrent requests
- `findAll()` returns a defensive copy (`new ArrayList<>(values)`) to avoid exposing internal mutable state

### Trade-off
- Data is lost on application restart
- No ACID transactions across entities
- Not suitable for production scale or multi-instance deployment without external storage

---

## 3. Validation Strategy

Validation is split across two layers:

### Controller Layer (Structural Validation)
- Jakarta Bean Validation (`@NotBlank`, `@NotNull`, `@Min`, `@Pattern`)
- Validates required fields, formats (e.g., PAN pattern), and basic constraints
- Returns `400 Bad Request` with field-level error details

### Service Layer (Business Validation)
- Domain rules: age range, sum assured limits, nominee rules, PAN requirement
- Reference data validation (payment frequency, policy term)
- Throws `BusinessException` for rule violations

**Why split validation?**
- Controller validation catches malformed requests early
- Service validation encapsulates business logic that may depend on multiple entities (e.g., customer PAN + proposal premium)
- Keeps controllers thin and services testable

---

## 4. DTO Pattern

Separate Request/Response DTOs are used instead of exposing domain entities directly.

| DTO | Purpose |
|-----|---------|
| `CustomerRequest` | Input validation for create/update |
| `CustomerResponse` | Safe output without internal fields |
| `ProposalRequest` | Validated proposal creation input |
| `ProposalResponse` | Includes status and policy number |
| `ErrorResponse` | Consistent error structure |

**Benefits:**
- API contract is decoupled from internal model
- Validation annotations stay on request DTOs only
- Prevents accidental exposure of internal state

---

## 5. Exception Handling

A centralized `@ControllerAdvice` (`GlobalExceptionHandler`) maps exceptions to HTTP responses:

| Exception | HTTP Status | Use Case |
|-----------|-------------|----------|
| `ResourceNotFoundException` | 404 | Entity not found |
| `BusinessException` | 400 | Business rule violation |
| `MethodArgumentNotValidException` | 400 | Input validation failure |
| `Exception` | 500 | Unexpected errors |

**Design choice:** Custom runtime exceptions with a single handler class rather than checked exceptions or `ResponseEntity` in every controller method.

---

## 6. Reference Master Data

Reference data (policy terms, payment frequencies) is served via `GET /reference-master/{category}`.

**Implementation:**
- Static `Map<ReferenceCategory, List<String>>` in `ReferenceMasterService`
- Values aligned with `PolicyConstants` used in business validation

**Why static in-memory reference data?**
- Simple and fast for a demo API
- Single source of truth via `PolicyConstants`
- Category lookup is case-insensitive via `toUpperCase()`

**Trade-off:** Reference data cannot be changed at runtime without code deployment.

---

## 7. Proposal Submission Flow

```
POST /proposals/{id}/submit
    │
    ├─ Load proposal (404 if not found)
    ├─ Reject if already SUBMITTED
    ├─ Re-validate all business rules
    ├─ Generate policy number: "POL" + System.currentTimeMillis()
    ├─ Set status = SUBMITTED
    ├─ Persist proposal
    └─ Create audit record (PROPOSAL_SUBMITTED)
```

**Key decisions:**
- **Re-validation on submit:** Rules are checked at both create and submit time, guarding against stale data if customer details change
- **Policy number format:** `POL` + timestamp ensures uniqueness without a separate sequence service
- **Audit on submit only:** Audit trail captures the business-significant event, not every CRUD operation

---

## 8. Testing Approach

JUnit 5 integration tests (`@SpringBootTest`) cover:
- Customer age validation (business rules)
- Proposal validation (premium, nominee, PAN, payment frequency)
- Proposal submission (status change, policy number, audit creation)
- Re-submission rejection

**Why integration tests over pure unit tests?**
- Validates the full Spring wiring and bean collaboration
- Demonstrates real business flows end-to-end
- Simpler setup for an interview project without heavy mocking

**Trade-off:** Tests share in-memory state within the same Spring context. Audit assertions use before/after counts rather than absolute values to remain resilient.

---

## 9. Package Structure

```
com.hdfc.policy
├── controller    # REST API layer
├── service       # Business logic
├── repository    # Data access
├── model         # Entities, enums, DTOs
└── exception     # Custom exceptions & handler
```

This mirrors standard Spring Boot conventions and is easy to navigate in an interview walkthrough.

---

## 10. Summary of Trade-offs

| Decision | Benefit | Cost |
|----------|---------|------|
| In-memory storage | Zero infra, fast setup | No persistence, not scalable |
| ConcurrentHashMap | Thread-safe without locks | No relational queries |
| DTOs | Clean API contract | Extra mapping code |
| Re-validation on submit | Data integrity | Duplicate validation calls |
| Static reference data | Simple, consistent | Not runtime-configurable |
| Timestamp policy numbers | Unique without DB sequence | Not human-readable sequential |

These trade-offs are intentional for a demonstration API that prioritizes clarity, interview explainability, and production-like code structure over operational scale.
