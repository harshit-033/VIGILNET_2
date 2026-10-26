# AICTE Approval & Compliance Orchestration Platform (SIH 26262)
## Complete REST API Specification & Developer Reference

---

**Specification Standard**: OpenAPI 3.0 / REST / JSON  
**Base URL (Local)**: `http://localhost:8080/api/v1`  
**Swagger UI**: `http://localhost:8080/swagger-ui.html`  
**OpenAPI JSON**: `http://localhost:8080/v3/api-docs`  
**Protocol**: HTTP/1.1 (HTTPS in Production)  
**Authentication Scheme**: HTTP Bearer Token (`Authorization: Bearer <TOKEN>`)  
**Error Standard**: RFC 7807 Problem Details (`application/problem+json` & `application/json`)  
**Backend Framework**: Spring Boot 3.4.3 + Spring Security 6.4.3 + PostgreSQL 18.6  
**Document Generation Date**: October 23, 2026  

---

## Table of Contents

1. [Architectural Principles & Design Standards](#1-architectural-principles--design-standards)
2. [Authentication & Development Token Bridge](#2-authentication--development-token-bridge)
3. [Error Response Format (RFC 7807)](#3-error-response-format-rfc-7807)
4. [Pagination, Sorting & Filtering Protocol](#4-pagination-sorting--filtering-protocol)
5. [Complete Endpoint Reference](#5-complete-endpoint-reference)
   - 5.1 System Health (`GET /api/v1/health`)
   - 5.2 Issue Development Token (`POST /api/v1/auth/token`)
   - 5.3 Get Current User Profile (`GET /api/v1/auth/me`)
   - 5.4 List Institutions (`GET /api/v1/institutions`)
   - 5.5 Register Institution (`POST /api/v1/institutions`)
   - 5.6 Get Institution by ID (`GET /api/v1/institutions/{id}`)
   - 5.7 List Applications (`GET /api/v1/applications`)
   - 5.8 Register Application (`POST /api/v1/applications`)
   - 5.9 Get Application Details (`GET /api/v1/applications/{id}`)
   - 5.10 Execute Workflow Action (`POST /api/v1/applications/{id}/actions`)
6. [Workflow State Machine Action Payloads](#6-workflow-state-machine-action-payloads)
7. [Automated Test Evidence Matrix](#7-automated-test-evidence-matrix)

---

## 1. Architectural Principles & Design Standards

The platform's API surface adheres to strict enterprise REST guidelines:
- **Stateless Communication**: No server-side HTTP session state. Every request carries an `Authorization: Bearer` header.
- **Resource-Oriented URIs**: Plural nouns for collections (`/institutions`, `/applications`) and subordinate actions for operations (`/applications/{id}/actions`).
- **Standard HTTP Verbs**:
  - `GET`: Safe, idempotent read operations.
  - `POST`: Non-idempotent creation or consequential state transitions.
- **HTTP Status Codes**:
  - `200 OK`: Successful synchronous retrieval or state mutation.
  - `201 Created`: Successful creation of a persistent entity, accompanied by a `Location` header pointing to the canonical resource URI.
  - `400 Bad Request`: Validation failure, parameter type mismatch, or invalid state machine precondition.
  - `401 Unauthorized`: Missing, unparseable, or invalid Bearer token.
  - `404 Not Found`: Referenced entity ID or application code does not exist.
  - `405 Method Not Allowed`: HTTP method unsupported for target URI.
  - `409 Conflict`: Unique constraint violation or JPA optimistic locking collision.
  - `415 Unsupported Media Type`: Request `Content-Type` is not `application/json`.
  - `500 Internal Server Error`: Unhandled server exception.

---

## 2. Authentication & Development Token Bridge

### Overview
To facilitate rapid multi-role testing during hackathon evaluations without external identity provider dependencies, the backend implements a server-managed development authentication bridge in `DevTokenService.java` and `DevBearerAuthenticationFilter.java`.

### Supported Demonstration Roles
The bridge recognizes the following canonical roles:
- `Scrutiny Officer` (Principal: `USR-SO-01`, B. Singh)
- `Institution Representative` (Principal: `USR-INST-01`, Dr. Ramesh)
- `Regional Officer` (Principal: `USR-RO-01`, Dr. A. Sharma)
- `EVC Coordinator` (Principal: `USR-EVC-01`, C. Verma)
- `Compliance Reviewer` (Principal: `USR-COMP-01`, P. Mehta)
- `Senior Management` (Principal: `USR-SM-01`, Prof. M. K. Sridhar)
- `Administrator` (Principal: `USR-ADM-01`, System Administrator)

### Token Issuance Flow
1. Client sends `POST /api/v1/auth/token` with `{"role": "Scrutiny Officer"}`.
2. Server validates the role, resolves the user profile, issues a signed token `dev-token-USR-SO-01-<uuid>`, and caches it in memory.
3. Client stores this token in browser `localStorage` under `aicte_auth_token`.
4. Client passes `Authorization: Bearer <TOKEN>` in subsequent requests.
5. `DevBearerAuthenticationFilter` intercepts the request, verifies the token against the registry, and populates the `SecurityContextHolder` with `ROLE_SCRUTINY_OFFICER`.

---

## 3. Error Response Format (RFC 7807)

All non-2xx responses conform to the **RFC 7807 Problem Details for HTTP APIs** specification:

```json
{
  "type": "https://api.aicte.gov.in/errors/validation-failed",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Request validation failed. Inspect field errors.",
  "instance": "/api/v1/applications",
  "timestamp": "2026-10-23T23:08:50.123Z",
  "invalidFields": {
    "applicationReference": "must not be blank",
    "institutionId": "must not be null"
  }
}
```

---

## 4. Pagination, Sorting & Filtering Protocol

Endpoints returning collections (`GET /api/v1/institutions`, `GET /api/v1/applications`) support standard Spring Data pagination parameters:

### Query Parameters
- `page` (integer, default `0`): 0-indexed page number.
- `size` (integer, default `20`, maximum `100`): Maximum items returned per page.

### Envelope Structure (`PageResponse<T>`)
```json
{
  "items": [ ... ],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 42,
  "totalPages": 3,
  "isFirst": true,
  "isLast": false,
  "hasNext": true,
  "hasPrevious": false
}
```

---

## 5. Complete Endpoint Reference

### 5.1 System Health
`GET /api/v1/health`
- **Purpose**: System liveness and diagnostic probe.
- **Authentication**: None (Public).
- **Responses**:
  - `200 OK`: System healthy.
- **Example cURL**:
```bash
curl -s http://localhost:8080/api/v1/health
```
- **Example Response**:
```json
{
  "status": "UP",
  "application": "aicte-compliance-platform",
  "timestamp": "2026-10-23T23:08:50.000Z"
}
```

### 5.2 Issue Development Token
`POST /api/v1/auth/token`
- **Purpose**: Issue an authentic Bearer token for a demo role.
- **Authentication**: None (Public Dev Bridge).
- **Headers**: `Content-Type: application/json`
- **Request Body**:
```json
{
  "role": "Scrutiny Officer"
}
```
- **Responses**:
  - `200 OK`: Token generated.
- **Example cURL**:
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/token   -H "Content-Type: application/json"   -d '{"role":"Scrutiny Officer"}'
```
- **Example Response**:
```json
{
  "accessToken": "dev-token-USR-SO-01-f5479a8a",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "USR-SO-01",
    "name": "B. Singh",
    "email": "scrutiny.singh@aicte-india.gov.in",
    "role": "Scrutiny Officer",
    "region": "Northern Region",
    "designation": "Senior Scrutiny Officer"
  }
}
```

### 5.3 Get Current User Profile
`GET /api/v1/auth/me`
- **Purpose**: Verify token validity and fetch caller profile.
- **Authentication**: `Authorization: Bearer <TOKEN>`
- **Responses**:
  - `200 OK`: Authenticated profile returned.
  - `401 Unauthorized`: Token missing or invalid.
- **Example cURL**:
```bash
curl -s http://localhost:8080/api/v1/auth/me   -H "Authorization: Bearer dev-token-USR-SO-01-f5479a8a"
```

### 5.4 List Institutions
`GET /api/v1/institutions`
- **Purpose**: Retrieve paginated list of canonical institutions.
- **Authentication**: `Authorization: Bearer <TOKEN>`
- **Query Parameters**: `page` (int, default 0), `size` (int, default 20)
- **Responses**:
  - `200 OK`: `PageResponse<InstitutionResponse>`
  - `401 Unauthorized`: Missing token.
- **Example cURL**:
```bash
curl -s "http://localhost:8080/api/v1/institutions?page=0&size=10"   -H "Authorization: Bearer $TOKEN"
```

### 5.5 Register Institution
`POST /api/v1/institutions`
- **Purpose**: Register a new canonical institution in the directory.
- **Authentication**: `Authorization: Bearer <TOKEN>`
- **Headers**: `Content-Type: application/json`
- **Request Body**:
```json
{
  "institutionalIdentifier": "INST-1099",
  "name": "National Technical Institute",
  "region": "Northern Region"
}
```
- **Responses**:
  - `201 Created`: Institution created (Returns `Location` header).
  - `400 Bad Request`: Field validation failure.
  - `409 Conflict`: Duplicate `institutionalIdentifier`.
- **Example cURL**:
```bash
curl -s -i -X POST http://localhost:8080/api/v1/institutions   -H "Authorization: Bearer $TOKEN"   -H "Content-Type: application/json"   -d '{"institutionalIdentifier":"INST-1099","name":"National Technical Institute","region":"Northern Region"}'
```

### 5.6 Get Institution by ID
`GET /api/v1/institutions/{id}`
- **Purpose**: Retrieve institution details by numeric primary key.
- **Authentication**: `Authorization: Bearer <TOKEN>`
- **Path Variable**: `id` (Long primary key)
- **Responses**:
  - `200 OK`: `InstitutionResponse`
  - `404 Not Found`: Institution not found.
- **Example cURL**:
```bash
curl -s http://localhost:8080/api/v1/institutions/1   -H "Authorization: Bearer $TOKEN"
```

### 5.7 List Applications (With Search & Filtering)
`GET /api/v1/applications`
- **Purpose**: Retrieve paginated list of applications with relational filtering.
- **Authentication**: `Authorization: Bearer <TOKEN>`
- **Query Parameters**:
  - `institutionId` (Long): Filter by institution primary key.
  - `status` (String): Filter by application status (`In progress`, `Returned`, `Due soon`).
  - `academicYear` (String): Filter by academic cycle (`2026-2027`).
  - `region` (String): Filter by institution region (`Northern Region`).
  - `stage` (String): Filter by stage (`Scrutiny`, `Re-scrutiny`, `EVC scheduling`).
  - `q` (String): Multi-field case-insensitive search.
  - `page` (Integer): Page number (default 0).
  - `size` (Integer): Page size (default 20).
- **Responses**:
  - `200 OK`: `PageResponse<ApplicationResponse>`
- **Example cURL**:
```bash
curl -s "http://localhost:8080/api/v1/applications?region=Northern+Region&stage=Scrutiny"   -H "Authorization: Bearer $TOKEN"
```

### 5.8 Register Application
`POST /api/v1/applications`
- **Purpose**: Register a new application linked to a canonical institution.
- **Authentication**: `Authorization: Bearer <TOKEN>`
- **Request Body**:
```json
{
  "institutionId": 1,
  "applicationReference": "APP-2026-9001",
  "applicationType": "Extension of Approval / Compliance Review",
  "academicYear": "2026-2027"
}
```
- **Responses**:
  - `201 Created`: Application registered with status `In progress`, stage `Scrutiny`.
  - `400 Bad Request`: Field validation failure.
  - `404 Not Found`: Referenced `institutionId` does not exist.
  - `409 Conflict`: Duplicate `applicationReference`.
- **Example cURL**:
```bash
curl -s -i -X POST http://localhost:8080/api/v1/applications   -H "Authorization: Bearer $TOKEN"   -H "Content-Type: application/json"   -d '{"institutionId":1,"applicationReference":"APP-2026-9001","applicationType":"Extension of Approval / Compliance Review","academicYear":"2026-2027"}'
```

### 5.9 Get Application Details & Timeline
`GET /api/v1/applications/{id}`
- **Purpose**: Fetch application dossier including linked institution and chronological workflow transitions.
- **Authentication**: `Authorization: Bearer <TOKEN>`
- **Path Variable**: `id` (Supports numeric ID `13` or reference string `APP-2026-1042`).
- **Responses**:
  - `200 OK`: Full `ApplicationResponse` with `timeline`.
  - `404 Not Found`: Application reference not found.
- **Example cURL**:
```bash
curl -s http://localhost:8080/api/v1/applications/APP-2026-1042   -H "Authorization: Bearer $TOKEN"
```

### 5.10 Execute Workflow Action
`POST /api/v1/applications/{id}/actions`
- **Purpose**: Execute a state transition (`RETURN_FOR_CORRECTION`, `SUBMIT_CORRECTIONS`, `COMPLETE_SCRUTINY`).
- **Authentication**: `Authorization: Bearer <TOKEN>`
- **Path Variable**: `id` (Supports numeric ID or reference string).
- **Headers**: `Content-Type: application/json`
- **Request Body (`WorkflowActionRequest`)**:
```json
{
  "actionType": "RETURN_FOR_CORRECTION",
  "actorName": "B. Singh",
  "actorRole": "Scrutiny Officer",
  "reason": "Fire safety certificate expired.",
  "newDeficiencies": [
    "Fire Safety NOC issued by Delhi Fire Service expired on 2026-09-30"
  ],
  "observations": "Land and building verified. Safety compliance non-compliant."
}
```
- **Responses**:
  - `200 OK`: Action executed atomically; returns updated `ApplicationResponse`.
  - `400 Bad Request`: Missing reason, invalid action, or precondition violation.
  - `404 Not Found`: Application not found.
  - `409 Conflict`: JPA optimistic locking collision.
- **Example cURL**:
```bash
curl -s -X POST http://localhost:8080/api/v1/applications/APP-2026-1042/actions   -H "Authorization: Bearer $TOKEN"   -H "Content-Type: application/json"   -d '{"actionType":"RETURN_FOR_CORRECTION","actorName":"B. Singh","actorRole":"Scrutiny Officer","reason":"Fire safety NOC expired.","newDeficiencies":["Fire safety NOC expired"]}'
```

---

## 6. Workflow State Machine Action Payloads

### Action 1: `RETURN_FOR_CORRECTION`
- **Prerequisites**: Application stage must be `Scrutiny` or `Re-scrutiny`.
- **Target State**: Stage becomes `Re-scrutiny`, status becomes `Returned`, owner becomes `Institution Representative`.
- **Payload Schema**:
```json
{
  "actionType": "RETURN_FOR_CORRECTION",
  "actorName": "string (optional, defaults to authenticated user)",
  "actorRole": "string (optional)",
  "reason": "string (MANDATORY, non-blank)",
  "newDeficiencies": ["string array of specific objections"],
  "observations": "string (optional review remarks)"
}
```

### Action 2: `SUBMIT_CORRECTIONS`
- **Prerequisites**: Application status must be `Returned`.
- **Target State**: Stage remains `Re-scrutiny`, status becomes `In progress`, owner becomes `Scrutiny Officer`.
- **Payload Schema**:
```json
{
  "actionType": "SUBMIT_CORRECTIONS",
  "actorName": "string (optional, e.g. Dr. Ramesh)",
  "actorRole": "Institution Representative",
  "reason": "string (MANDATORY, explanation of rectifications made)"
}
```

### Action 3: `COMPLETE_SCRUTINY`
- **Prerequisites**: Application stage must be `Scrutiny` or `Re-scrutiny`. Open deficiencies must be resolved.
- **Target State**: Stage advances to `nextStage` (default: `EVC scheduling`), status becomes `Due soon`, owner becomes `EVC Coordinator`.
- **Payload Schema**:
```json
{
  "actionType": "COMPLETE_SCRUTINY",
  "actorName": "B. Singh",
  "actorRole": "Scrutiny Officer",
  "nextStage": "EVC scheduling",
  "observations": "string (optional final endorsement)"
}
```

---

## 7. Automated Test Evidence Matrix

| Test Method | HTTP Endpoint Tested | Scenario Verified | Status |
| :--- | :--- | :--- | :--- |
| `HealthEndpointTest.shouldReturnHealthStatusOk` | `GET /api/v1/health` | Public liveness probe returning UP | **PASSED** |
| `HealthEndpointTest.shouldRejectUnauthenticatedAccess` | `GET /api/v1/applications` | Protected endpoint returning 401 without Bearer | **PASSED** |
| `WorkflowAndSecurityIntegrationTest.shouldAuthenticateWithDevBearerToken` | `POST /api/v1/auth/token` + `GET /api/v1/auth/me` | Dev Bearer token bridge issuance & resolution | **PASSED** |
| `WorkflowAndSecurityIntegrationTest.shouldReturn400ForMalformedJson` | `POST /api/v1/applications/APP-WF-xxx/actions` | RFC 7807 problem details on syntax errors | **PASSED** |
| `WorkflowAndSecurityIntegrationTest.shouldReturn400ForParameterTypeMismatch` | `GET /api/v1/applications?page=not-a-number` | Parameter type conversion error handling | **PASSED** |
| `WorkflowAndSecurityIntegrationTest.shouldReturn405ForUnsupportedMethod` | `DELETE /api/v1/institutions` | Method Not Allowed RFC 7807 response | **PASSED** |
| `WorkflowAndSecurityIntegrationTest.shouldReturn415ForUnsupportedMediaType` | `POST /api/v1/applications` with `text/plain` | Unsupported Media Type RFC 7807 response | **PASSED** |
| `WorkflowAndSecurityIntegrationTest.shouldHandleSpecialCharactersInAuditMetadataSafely` | `POST /api/v1/institutions` | Jackson serialization with quotes and backslashes | **PASSED** |
| `WorkflowAndSecurityIntegrationTest.shouldExecuteFullScrutinyLifecycleTransitions` | `POST /api/v1/applications/{id}/actions` | Full 3-step state machine (`RETURN` -> `SUBMIT` -> `COMPLETE`) | **PASSED** |
| `InstitutionAndApplicationApiIntegrationTest.shouldCreateAndRetrieveInstitution` | `POST /api/v1/institutions` | Institution registration and retrieval | **PASSED** |
| `InstitutionAndApplicationApiIntegrationTest.shouldRejectDuplicateInstitutionalIdentifier` | `POST /api/v1/institutions` | 409 Conflict rejection on duplicate code | **PASSED** |
| `InstitutionAndApplicationApiIntegrationTest.shouldCreateAndRetrieveApplicationLinkedToInstitution` | `POST /api/v1/applications` | Application registration linked to FK institution | **PASSED** |
| `InstitutionAndApplicationApiIntegrationTest.shouldRejectDuplicateApplicationReference` | `POST /api/v1/applications` | 409 Conflict rejection on duplicate reference | **PASSED** |
| `InstitutionAndApplicationApiIntegrationTest.shouldPaginateAndFilterApplications` | `GET /api/v1/applications` | Multivariable pagination and filtering | **PASSED** |

---
*End of SIH 26262 API Documentation.*
