# AICTE Approval & Compliance Orchestration Platform

**Problem Statement ID**: 26262  
**Implementation Stage**: End-to-End Scrutiny & Deficiency-Rectification Journey Connected (Spring Boot 3.4.3 + PostgreSQL 18 + React 19 Frontend)  
**Reference Design**: Backend Implementation Guide, Solution Blueprint & Integration Assessment  

---

## 1. Project Overview

The **AICTE Approval & Compliance Orchestration Platform** is an enterprise-grade system designed to orchestrate technical institution approval lifecycles, deficiency rectifications, regulatory compliance monitoring, and Expert Visit Committee (EVC) operations.

The platform enforces the architectural mandate that **the backend owns business logic, state machines, and regulatory validation**:
- The React frontend is a replaceable client. Business rules, validation constraints, concurrency controls, and state transitions are enforced exclusively in server-side Spring Boot services.
- Application lifecycles maintain separate, traceable state transitions persisted directly in PostgreSQL.
- Every workflow action atomically records an immutable audit event and a workflow transition entry in the same transaction.
- Client requests are authenticated via a development Bearer token bridge derived from trusted server-side roles, never trusting client-supplied roles.

> **Important Regulatory Notice**:  
> Workflow stages, application types, and status codes currently configured are illustrative prototype values aligned with the demo journey. Authoritative AICTE Approval Process Handbook (APH) rules must be formally validated with system owners prior to production deployment.

---

## 2. Technology Stack & Versions

All versions are verified directly from repository configuration descriptors (`pom.xml`, `compose.yaml`, `package.json`):

| Component | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Language** | Java (OpenJDK) | `21` LTS | Backend runtime and compiler |
| **Framework** | Spring Boot | `3.4.3` | Application framework (Web, Data JPA, Security) |
| **Build Tool** | Apache Maven | `3.10.0` | Build automation via Maven Wrapper (`./mvnw`) |
| **Database** | PostgreSQL | `18.6` | Relational database (Docker container `aicte-postgres`) |
| **Migrations** | Flyway | `10.20.1` | Automated database schema version control (V1 + V2) |
| **ORM / JPA** | Hibernate | `6.6.8.Final` | Persistence provider with entity graphs (`ddl-auto: validate`) |
| **Connection Pool** | HikariCP | `5.1.0` | High-performance JDBC connection pooling |
| **Security** | Spring Security | `6.4.3` | Endpoint perimeter, CORS, and Bearer token authentication |
| **API Docs** | springdoc-openapi | `2.8.5` | OpenAPI 3.0 specification and Swagger UI |
| **Frontend** | React + Vite | React `19.3.1`, Vite `8.3.5` | Modern responsive web frontend interface (located in `frontend/`) |

---


---

## 2.1 Unified Project Layout

The repository consolidates both the Spring Boot 3.4.3 backend and the React 19 Vite frontend into a single shareable project:

```text
SIH2/
├── src/                               # Spring Boot backend source code & integration tests
│   ├── main/java/in/gov/aicte/platform/...
│   └── main/resources/db/migration/...# Flyway V1 & V2 PostgreSQL schema migrations
├── frontend/                          # React 19 + Vite web frontend
│   ├── src/                           # Pages, role dashboards, components, API client
│   ├── public/                        # Static assets & brand logos
│   ├── tests/                         # Automated frontend lifecycle tests
│   ├── package.json                   # Dependencies, scripts, and build targets
│   ├── package-lock.json              # Strict deterministic dependency tree
│   └── vite.config.js                 # Vite bundler configuration
├── docs/                              # Project documentation & testing guides
│   ├── SIH26262_Backend_Connected_UI_Testing_Guide.md
│   ├── SIH26262_Backend_Connected_UI_Testing_Guide.pdf
│   ├── FRONTEND_COMPLETE_DOCUMENTATION.md
│   └── references/                    # Problem statement and layout specifications
├── pom.xml                            # Backend Maven project descriptor
├── mvnw / mvnw.cmd                    # Maven wrapper scripts
├── compose.yaml                       # Docker Compose specification for PostgreSQL 18
├── .env.example                       # Safe environment variable configuration template
├── .gitignore                         # Git exclusion rules for Java, Node, & secrets
└── README.md                          # Comprehensive architecture and run guide
```

## 3. Architecture & Backend Defect Fixes

In accordance with the code review findings, the following high-priority backend defects have been remediated:

1. **Jackson Serialization for Audit Logs**:
   - Replaced fragile manual JSON string concatenation in `InstitutionService` and `ApplicationService` with Jackson `ObjectMapper.writeValueAsString(...)`. All special characters (quotes, backslashes, newlines) are safely escaped.
2. **Standardized RFC 7807 Problem Detail Error Responses**:
   - Enhanced `GlobalExceptionHandler` to handle `HttpMessageNotReadableException` (400 Malformed JSON), `MethodArgumentTypeMismatchException` (400 Parameter Type Mismatch), `HttpRequestMethodNotSupportedException` (405 Method Not Allowed), `HttpMediaTypeNotSupportedException` (415 Unsupported Media Type), and `OptimisticLockingFailureException` (409 Conflict).
3. **Eliminated N+1 Query Issue**:
   - Applied `@EntityGraph(attributePaths = {"institution"})` on `ApplicationRepository` query methods (`findAll`, `findById`, `findByApplicationReference`), eliminating lazy N+1 select queries during paginated retrieval.
4. **Optimistic Locking & State Validation**:
   - All state transitions validate current stage and status prerequisites. State updates increment the `@Version` column on `applications` to prevent lost updates and race conditions.

---

## 4. Database Migrations (Flyway V1 + V2)

Database schema evolution is strictly additive and version-controlled via Flyway:

### Migration V1: `V1__initial_schema.sql`
- `institutions`: Primary directory with `institutional_identifier` (UNIQUE) and `name`.
- `applications`: Core application records with `application_reference` (UNIQUE), foreign key to `institutions`, `status`, and `version`.
- `audit_events`: Append-only audit table with `event_type`, `actor_id`, `target_entity_type`, `target_entity_id`, and `metadata_json`.

### Migration V2: `V2__workflow_and_application_extensions.sql`
- **Application extensions**: Added `current_stage` (VARCHAR 64), `owner_name` (VARCHAR 128), `owner_id` (VARCHAR 64), `due_date` (DATE), `observations` (TEXT), and `deficiencies_json` (TEXT).
- **Institution extensions**: Added `region` (VARCHAR 64) for regional routing.
- **Workflow Transitions table**: Created `workflow_transitions` capturing `application_id`, `action`, `from_stage`, `to_stage`, `from_status`, `to_status`, `actor_id`, `actor_name`, `actor_role`, `reason`, `observations`, and `deficiencies_json`.
- **Demo Seed Data**: Canonical institution `INST-1025` ("ABC Institute of Technology", "Northern Region") and application `APP-2026-1042` ("Extension of Approval", stage "Scrutiny").

---

## 5. Development Authentication Bridge & CORS

### CORS Configuration
CORS is explicitly configured for the Vite frontend origin `http://localhost:5173` (and `http://localhost:3000`):
- Allowed Methods: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`, `PATCH`.
- Allowed Headers: `Content-Type`, `Authorization`.
- Preflight cache: 3600 seconds (`maxAge`).

### Development Authentication Bridge
To allow the frontend's role switcher to function while maintaining server-side security:
- **Token Endpoint**: `POST /api/v1/auth/token`
- **Request Body**: `{"role": "Scrutiny Officer"}` (or any of the 7 supported demo roles: `Scrutiny Officer`, `Regional Officer`, `Institution Representative`, `EVC Member`, `Council Admin`, `Viewer / Auditor`, `Expert Committee Member`).
- **Response**: Returns an `access_token` (e.g. `dev-token-USR-SO-01-...`) and the verified `user` profile.
- **Verification Endpoint**: `GET /api/v1/auth/me` with `Authorization: Bearer <token>`.
- **Security Boundary**: The token is verified by `DevBearerAuthenticationFilter` on every request. Client-supplied roles in request bodies are ignored for authorization; only the authenticated server-side principal is used.

> **Development Limitation**:  
> This token bridge is strictly for local development and demonstration purposes. It does not replace production OIDC/OAuth2/SAML SSO. Passwords are not verified in this dev bridge.

---

## 6. Workflow API Endpoints

### Execute Workflow Action
- **URL**: `POST /api/v1/applications/{id}/actions`  
  *(Accepts numeric database ID or string reference e.g. `APP-2026-1042`)*
- **Supported Actions**:
  1. `RETURN_FOR_CORRECTION` (Requires stage `Scrutiny` or `Re-scrutiny` + mandatory `reason`). Transitions to stage `Re-scrutiny`, status `Returned`.
  2. `SUBMIT_CORRECTIONS` (Requires status `Returned` + mandatory `reason`). Transitions to stage `Re-scrutiny`, status `In progress`.
  3. `COMPLETE_SCRUTINY` (Requires stage `Scrutiny` or `Re-scrutiny`). Transitions to `nextStage` (default: `EVC scheduling`), status `Due soon`.

**Example Request (`RETURN_FOR_CORRECTION`)**:
```bash
curl -X POST "http://localhost:8080/api/v1/applications/APP-2026-1042/actions" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "actionType": "RETURN_FOR_CORRECTION",
    "actorName": "B. Singh",
    "actorRole": "Scrutiny Officer",
    "reason": "Fire safety NOC expired. Faculty roster missing signatures.",
    "newDeficiencies": [
      "Fire Safety NOC issued by Delhi Fire Service is expired",
      "Faculty roster missing HoD counter-signature"
    ],
    "observations": "Land verified. Fire safety deficient."
  }'
```

**Response (`200 OK`)**:
Returns the updated canonical `ApplicationResponse` including updated stage, owner, deficiencies list, and complete chronological `timeline`.

---

## 7. PostgreSQL Persistence vs. LocalStorage Status

To deliver the deadline goal without destabilizing the frontend, modules are segmented as follows:

| Module / Feature | Persistence Storage | Status |
| :--- | :--- | :--- |
| **Authentication & Tokens** | Server-side Dev Token Bridge | **Connected to Backend** |
| **Application List (`/applications`)** | PostgreSQL 18 via Spring Boot REST API | **Connected to Backend** |
| **Application Detail (`/applications/:id`)** | PostgreSQL 18 via Spring Boot REST API | **Connected to Backend** |
| **Return for Correction** | PostgreSQL 18 (`applications`, `workflow_transitions`, `audit_events`) | **Connected to Backend** |
| **Submit Corrections** | PostgreSQL 18 (`applications`, `workflow_transitions`, `audit_events`) | **Connected to Backend** |
| **Complete Scrutiny** | PostgreSQL 18 (`applications`, `workflow_transitions`, `audit_events`) | **Connected to Backend** |
| **Audit Log Trail** | PostgreSQL 18 (`audit_events` table) | **Connected to Backend** |
| **Expert Visit Committee (EVC)** | Browser `localStorage` | Preserved for future phase |
| **Compliance Obligations** | Browser `localStorage` | Preserved for future phase |
| **Statutory Documents** | Browser `localStorage` | Preserved for future phase |
| **Alerts & Notifications** | Browser `localStorage` | Preserved for future phase |

---

## 8. How to Run & Verify

### Option A: One-Command Startup (Recommended)
From the project root (`SIH2/`), simply execute:
```bash
./run.sh
```
This automated master script will:
1. Verify / start PostgreSQL database on port 5432 (via docker compose or system service).
2. Start the Spring Boot backend on port 8080 and await healthy status (`/api/v1/health`).
3. Start the Vite React frontend on port 5173.
4. Stream unified logs and gracefully shut down all services upon pressing `Ctrl+C`.

---

### Option B: Manual Startup (3 Separate Terminals)

#### Terminal 1: Start PostgreSQL Database
From the project root (`SIH2/`):
```bash
# Using Docker Compose (Recommended)
docker compose up -d

# Verify database connection readiness
pg_isready -h localhost -p 5432
```
*Note: If running a native PostgreSQL service, ensure a database named `aicte_platform_db` exists on port 5432 with user `aicte_app_user` (or customize via `.env`). Flyway automatically runs database migrations on backend startup.*

#### Terminal 2: Start Spring Boot Backend
From the project root (`SIH2/`):
```bash
./mvnw spring-boot:run
```
Verify health endpoint once started:
```bash
curl http://localhost:8080/api/v1/health
# Response: {"status":"UP","application":"aicte-compliance-platform",...}
```
*Backend API and OpenAPI/Swagger Docs:*
- Base API URL: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

#### Terminal 3: Start React 19 Frontend
From the project root (`SIH2/`), navigate to the frontend directory:
```bash
cd frontend
npm ci          # Or: npm install
npm run dev
```
Open **`http://localhost:5173`** in your browser.

---

## 8.1 Setup Instructions for Evaluators / Teammates Unpacking Archive

If you received `SIH26262_Complete_Project.zip`:

1. **Extract the ZIP file**:
   ```bash
   unzip SIH26262_Complete_Project.zip
   cd SIH2
   ```
2. **Environment Configuration**:
   - The default configuration in `compose.yaml` and `src/main/resources/application.yml` runs out-of-the-box with default development values (`aicte_platform_db`, `aicte_app_user`, `aicte_123`).
   - If you need custom ports or credentials, copy the template:
     ```bash
     cp .env.example .env
     ```
3. **Start Services in 3 Terminals**:
   - Terminal 1: `docker compose up -d`
   - Terminal 2: `./mvnw spring-boot:run`
   - Terminal 3: `cd frontend && npm ci && npm run dev`
4. **Access the Application**:
   - Open browser at `http://localhost:5173`
   - Switch roles using the top-right header role dropdown to explore all 7 AICTE stakeholder journeys.

---

## 9. Automated Testing & Verification

### Backend Automated Tests (20 Tests, 100% Passing)
Run all backend unit, regression, security, and persistence tests:
```bash
./mvnw test
```

### Frontend Automated Tests & Build Verification
From the `frontend/` directory:
```bash
cd frontend

# Run frontend EVC lifecycle & tab filtering tests
npm test

# Verify production Vite build bundle
npm run build
```

### Test Suite Summary (20 Tests, 100% Passing)
- **`HealthEndpointTest`** (2 tests): Public health endpoint access and security boundary verification.
- **`PostgresPersistenceIntegrationTest`** (2 tests): Flyway schema validation and JPA entity relationships.
- **`InstitutionAndApplicationApiIntegrationTest`** (9 tests): CRUD endpoints, duplicate rejection, pagination, and query filtering.
- **`WorkflowAndSecurityIntegrationTest`** (7 tests):
  1. Development authentication token issuance and Bearer access verification.
  2. Malformed JSON request body rejection (400 Bad Request Problem Detail).
  3. Query parameter type mismatch rejection (400 Bad Request Problem Detail).
  4. Unsupported HTTP method handling (405 Method Not Allowed Problem Detail).
  5. Unsupported media type handling (415 Unsupported Media Type Problem Detail).
  6. Jackson audit log serialization verifying safe JSON escaping with quotes and backslashes.
  7. Full 3-step workflow state transitions (`RETURN_FOR_CORRECTION` -> `SUBMIT_CORRECTIONS` -> `COMPLETE_SCRUTINY`) verifying database integrity and optimistic locking.

---

## 10. Rehearsing the End-to-End Scrutiny Journey

1. **Log in / Switch Role**: Open `http://localhost:5173`. Switch role to **Scrutiny Officer** (B. Singh). The frontend requests a real Bearer token from `POST /api/v1/auth/token`.
2. **Open Application**: Navigate to Application Explorer. Click on `APP-2026-1042` ("ABC Institute of Technology"). The dossier loads from PostgreSQL.
3. **Return for Correction**: Click "Return for Correction". Provide reason: *"Fire safety NOC expired. Faculty roster missing signatures."* and click Submit.
   - The backend validates the transition from `Scrutiny` to `Re-scrutiny`.
   - Atomically updates application status to `Returned`, records the deficiency items, inserts a row into `workflow_transitions`, and appends an audit record to `audit_events`.
4. **Switch to Institution Representative**: Switch role to **Institution Representative** (Dr. Ramesh). Notice the status is `Returned`.
5. **Submit Corrections**: Click "Submit Deficiency Rectification". Enter rectification notes and submit.
   - Backend transitions status to `In progress`, reassigns ownership to the Scrutiny Officer, and appends transition and audit history.
6. **Switch back to Scrutiny Officer**: View the updated dossier. Click "Complete Scrutiny".
   - Backend advances application stage to `EVC scheduling` (or selected stage) with status `Due soon`.
7. **Verify Database Records**:
```bash
PGPASSWORD=aicte_123 psql -h localhost -U aicte_app_user -d aicte_platform_db -c "
SELECT application_reference, current_stage, status, owner_name, version FROM applications WHERE application_reference = 'APP-2026-1042';
SELECT action, from_stage, to_stage, from_status, to_status, actor_name, created_at FROM workflow_transitions WHERE application_id = 13;
"
```
All records are verified directly in PostgreSQL!
