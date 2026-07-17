# Policy Proposal Processing API

A Spring Boot 3.x REST API for managing insurance policy proposals with in-memory, thread-safe storage. Built with Java 17 and Maven for interview-ready demonstration of layered architecture, business validation, and clean exception handling.

## Project Overview

This API supports the end-to-end lifecycle of policy proposals:

- Register and manage customers
- Create policy proposals in `DRAFT` status
- Submit proposals with full business rule validation
- Generate unique policy numbers on submission
- Maintain an audit trail of key actions
- Expose reference master data for policy terms and payment frequencies

**Tech Stack:** Java 17, Spring Boot 3.2.5, Maven, JUnit 5, Jakarta Validation

## Prerequisites

- Java 17 or higher
- Maven 3.8+

## Setup & Run

```bash
cd policy-proposal-api
mvnw.cmd clean install    # Windows (no global Maven required)
./mvnw clean install      # Linux/macOS
mvnw.cmd spring-boot:run
```

If Maven is installed globally, you can use `mvn` instead of `mvnw.cmd`.

The application starts on `http://localhost:8081`.

## Run Tests

```bash
mvnw.cmd test    # Windows
./mvnw test      # Linux/macOS
```

## API List

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/reference-master/{category}` | Get reference data (`POLICY_TERM`, `PAYMENT_FREQUENCY`) |
| POST | `/customers` | Create a customer |
| GET | `/customers` | List all customers |
| GET | `/customers/{id}` | Get customer by ID |
| PUT | `/customers/{id}` | Update customer |
| POST | `/proposals` | Create a proposal (DRAFT) |
| GET | `/proposals/{id}` | Get proposal by ID |
| POST | `/proposals/{id}/submit` | Submit proposal and generate policy number |
| GET | `/audits` | List all audit records |

## Business Rules

| Rule | Validation |
|------|------------|
| Customer age | 18 – 65 |
| Policy term | 10, 15, 20, 25, 30 years |
| Sum assured | ₹1,00,000 – ₹5,00,00,000 |
| Minimum premium | ₹5,000 |
| PAN required | When premium > ₹50,000 |
| Nominee | Mandatory; cannot match customer name |
| Payment frequency | MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY |

## Sample Requests & Responses

### 1. Get Reference Master

**Request:**
```http
GET /reference-master/PAYMENT_FREQUENCY
```

**Response (200 OK):**
```json
["MONTHLY", "QUARTERLY", "HALF_YEARLY", "YEARLY"]
```

---

### 2. Create Customer

**Request:**
```http
POST /customers
Content-Type: application/json

{
  "name": "Ravi Kumar",
  "age": 32,
  "pan": "ABCDE1234F"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "name": "Ravi Kumar",
  "age": 32,
  "pan": "ABCDE1234F"
}
```

---

### 3. Create Proposal

**Request:**
```http
POST /proposals
Content-Type: application/json

{
  "customerId": 1,
  "sumAssured": 1000000,
  "term": 20,
  "premium": 25000,
  "nominee": "Priya Kumar",
  "paymentFrequency": "YEARLY"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "customerId": 1,
  "sumAssured": 1000000,
  "term": 20,
  "premium": 25000,
  "nominee": "Priya Kumar",
  "paymentFrequency": "YEARLY",
  "status": "DRAFT",
  "policyNumber": null
}
```

---

### 4. Submit Proposal

**Request:**
```http
POST /proposals/1/submit
```

**Response (200 OK):**
```json
{
  "id": 1,
  "customerId": 1,
  "sumAssured": 1000000,
  "term": 20,
  "premium": 25000,
  "nominee": "Priya Kumar",
  "paymentFrequency": "YEARLY",
  "status": "SUBMITTED",
  "policyNumber": "POL1720954321000"
}
```

---

### 5. Get Audits

**Request:**
```http
GET /audits
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "action": "PROPOSAL_SUBMITTED",
    "message": "Proposal 1 submitted with policy number POL1720954321000",
    "timestamp": "2026-07-14T19:05:21"
  }
]
```

---

### 6. Validation Error Example

**Request:**
```http
POST /proposals
Content-Type: application/json

{
  "customerId": 1,
  "sumAssured": 1000000,
  "term": 20,
  "premium": 3000,
  "nominee": "Priya Kumar",
  "paymentFrequency": "YEARLY"
}
```

**Response (400 Bad Request):**
```json
{
  "timestamp": "2026-07-14T19:06:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Minimum premium is 5000"
}
```

## Project Structure

```
src/main/java/com/hdfc/policy/
├── PolicyProposalApplication.java
├── controller/       # REST endpoints
├── service/          # Business logic & validation
├── repository/       # In-memory thread-safe storage
├── model/            # Domain entities & DTOs
└── exception/        # Global error handling
```

## License

This project is for educational and interview demonstration purposes.
