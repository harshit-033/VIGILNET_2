# SAGE — Local Run, Verification & Testing Guide

**System for Approval Governance and Evaluation (SAGE)**  
*AICTE Approval and Compliance Platform | SIH Problem Statement 26262*  
**Environment**: Spring Boot 3.4.3 (Java 21) + PostgreSQL 18.x + React 19 Frontend (Vite)  
**Document Version**: 2.0.0 (Hardened Milestone)  
**Date**: October 2026  

---

## 1. System Overview & Architecture

SAGE (**System for Approval Governance and Evaluation**) is an enterprise-grade platform developed for Smart India Hackathon (SIH) Problem Statement 26262. SAGE orchestrates technical education institution approval lifecycles, deficiency rectifications, regulatory compliance monitoring, and Expert Visit Committee (EVC) evaluations.

> **Regulatory & Endorsement Notice**:  
> SAGE is an academic and hackathon prototype engineered for SIH Problem Statement 26262. It is **not** an officially operated, sanctioned, or endorsed portal of the All India Council for Technical Education (AICTE). All institutional identities, approval workflows, and heuristic evaluation scores are illustrative demonstration models.

---

## 2. Prerequisites & System Requirements

Before running SAGE locally, ensure the following dependencies are available on your Linux/macOS host:

| Requirement | Minimum Version | Verified Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Java Development Kit (JDK)** | OpenJDK 21 LTS | 21.0.11 / 21.0.6 | Backend runtime and compilation |
| **Node.js** | 18.x LTS | 20.x / 22.x | Frontend build and runtime |
| **npm** | 9.x | 10.x | Frontend package manager |
| **PostgreSQL** | 14.x | 18.6 / 16.x | Persistent relational storage |
| **libpq / pg_isready** | PostgreSQL client | 18 / 16 | Database health check utility |

---

## 3. Environment Variables & Database Setup

### 3.1 Database Connection Configuration
SAGE connects to PostgreSQL using standard Spring datasource configuration. The default credentials configured in `src/main/resources/application.yml` are:

```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/aicte_platform_db"
export SPRING_DATASOURCE_USERNAME="aicte_app_user"
export SPRING_DATASOURCE_PASSWORD="aicte_secure_password_2026"
```

If your PostgreSQL client libraries (like `libpq.so.5`) reside in a user directory (such as `$HOME/.local/lib`), export `LD_LIBRARY_PATH`:
```bash
export LD_LIBRARY_PATH="$HOME/.local/lib:${LD_LIBRARY_PATH}"
```

### 3.2 Starting PostgreSQL
Ensure PostgreSQL is active on port `5432`:

**Option A: Local PostgreSQL Cluster**
```bash
pg_ctl -D "$HOME/.local/share/aicte_postgres_data" -l "$HOME/.local/share/aicte_postgres_data/logfile" start
pg_isready -h localhost -p 5432
```

**Option B: Docker Compose**
```bash
docker compose up -d
docker compose ps
```

---

## 4. One-Click Local Orchestration (`run.sh`)

The repository includes a hardened, self-contained launcher script (`run.sh`) that manages both the Spring Boot backend and React frontend.

### 4.1 Key Capabilities of `run.sh`
- **Dynamic Java 21 Discovery**: Automatically scans `$JAVA_HOME`, `$PATH`, user-local runtime directories (`~/.local/share/...`), and standard OS directories (`/usr/lib/jvm/...`) for a verified Java 21 binary.
- **Port Conflict Protection**: Uses `check_port_available` (via `ss -tulpn` / `lsof -i`) to detect if port 8080 or 5173 is already in use, printing actionable diagnostic information without terminating unrelated processes.
- **Strict Process PID Tracking**: Captures background PIDs for both backend (`$BACKEND_PID`) and frontend (`$FRONTEND_PID`).
- **Graceful Shutdown Trap**: Traps `SIGINT` (Ctrl+C), `SIGTERM`, and `EXIT` signals to cleanly terminate both child processes without leaving orphan listeners.
- **Localhost Binding**: Binds the Vite frontend server to `localhost` by default.

### 4.2 Starting the Platform
```bash
./run.sh
```

Console output will display:
```text
==========================================================
 SAGE Platform Runner (SIH Problem Statement 26262)
 System for Approval Governance and Evaluation
==========================================================
[OK] Discovered Java 21 at: ...
[OK] PostgreSQL is reachable at localhost:5432
[OK] Port 8080 is available for backend
[OK] Port 5173 is available for frontend
Starting Spring Boot backend (sage-compliance-platform)...
Waiting for backend health check at http://localhost:8080/actuator/health...
[OK] Backend is healthy! (SAGE service: sage-compliance-platform)
Starting React frontend (SAGE Web Console)...
Vite development server listening at: http://localhost:5173
==========================================================
 SAGE Platform is running!
 Backend:  http://localhost:8080
 Frontend: http://localhost:5173
 API Docs: http://localhost:8080/swagger-ui.html
 Press Ctrl+C to gracefully shut down both services.
==========================================================
```

### 4.3 Stopping the Platform
Simply press `Ctrl+C` in the terminal running `./run.sh`. The trap handler will cleanly terminate both the Spring Boot JVM and Vite Node process.

---

## 5. Manual Execution Procedures

If you prefer running services in separate terminal windows:

### Terminal 1: Spring Boot Backend
```bash
export LD_LIBRARY_PATH="$HOME/.local/lib:${LD_LIBRARY_PATH}"
./mvnw spring-boot:run
```
The backend initializes Flyway schema migrations (V1 through V7) and listens on `http://localhost:8080`.

### Terminal 2: React Frontend
```bash
cd frontend
npm install
npm run dev
```
The frontend starts on `http://localhost:5173`.

---

## 6. Automated Verification & Test Suites

### 6.1 Backend Test Suite (Spring Boot & PostgreSQL Integration)
Execute all backend tests using the Maven wrapper:
```bash
./mvnw --batch-mode test
```
**Expected Result**:
- Tests run: **55**
- Failures: **0**, Errors: **0**, Skipped: **0**
- Execution time: ~10 to 15 seconds.
- Verifies: SAGE health check branding, Flyway V1-V7 schema migrations, role-based access control, scrutiny verification, EVC visit lifecycles, file storage type whitelisting, compliance obligation real file upload and byte-exact download, tenant boundary isolation, alert delivery retry engine with attempt tracking, and heuristic advisory risk score labeling.

### 6.2 Frontend Test Suite (Vitest / Component Lifecycle)
Execute frontend automated tests:
```bash
cd frontend
npm test
```
**Expected Result**:
- Test Files: **2 passed** (`evcWorkflow.test.js`, `sageBrandingAndWorkflow.test.js`)
- Tests: **12 passed**
- Verifies: EVC modal scheduling, inspection checklist submission, SAGE navigation bar, sidebar branding, login page identity header, advisory notice labeling, and tenant isolation guards.

### 6.3 Frontend Production Build & Code Quality
Verify the production bundle compiles cleanly and satisfies ESLint rules:
```bash
cd frontend
npm run build
npm run lint
```
**Expected Result**:
- `npm run build`: Generates optimized bundle in `dist/` with zero errors.
- `npm run lint`: Zero ESLint errors or warnings.

---

## 7. API Verification via cURL

### 7.1 Platform Health & SAGE Branding
```bash
curl -s http://localhost:8080/actuator/health
```
*Expected response*:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "PostgreSQL" } },
    "sageService": {
      "status": "UP",
      "details": {
        "service": "sage-compliance-platform",
        "system": "System for Approval Governance and Evaluation",
        "sihProblemStatement": "26262"
      }
    }
  }
}
```

### 7.2 Obtain Development Bearer Token
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/token \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@aicte-india.gov.in"}' | jq -r .access_token)
echo "Token: $TOKEN"
```

### 7.3 Real Compliance Evidence Upload & Download
Upload a valid PDF evidence file for a compliance obligation:
```bash
echo "Sample institutional audit evidence" > test_audit.pdf

curl -s -X POST "http://localhost:8080/api/v1/compliance/1/evidence" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@test_audit.pdf"
```

Download the stored evidence binary stream and verify byte parity:
```bash
curl -s -X GET "http://localhost:8080/api/v1/compliance/1/evidence/download" \
  -H "Authorization: Bearer $TOKEN" \
  --output downloaded_audit.pdf

cmp test_audit.pdf downloaded_audit.pdf && echo "[SUCCESS] Byte-exact parity verified!"
```

### 7.4 Alert Delivery Retry & Attempt Inquiry
Trigger delivery retry for a pending alert:
```bash
curl -s -X POST "http://localhost:8080/api/v1/alerts/1/retry" \
  -H "Authorization: Bearer $TOKEN"
```

Inspect delivery attempt audit trail:
```bash
curl -s -X GET "http://localhost:8080/api/v1/alerts/1/attempts" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

## 8. Database Migrations (Flyway V1 – V7)

The database schema is managed via automated Flyway migrations:

| Version | Migration Script | Scope & Changes |
| :--- | :--- | :--- |
| **V1** | `V1__init_schema.sql` | Core domain: `institutions`, `applications`, `workflow_transitions`, `audit_events`. |
| **V2** | `V2__scrutiny_and_deficiencies.sql` | Scrutiny review tables, deficiency records, rectification submissions. |
| **V3** | `V3__incremental_validation.sql` | Validation rulesets, heuristic advisory scores, real-time feedback logging. |
| **V4** | `V4__document_integrity.sql` | SHA-256 document hashing, version history, tampering verification records. |
| **V5** | `V5__evc_inspection_checklist.sql` | EVC visits, inspection checklist items, geotagged evidence metadata. |
| **V6** | `V6__scrutiny_checklist_verification.sql` | Scrutiny checklist verification tables, reviewer signoffs. |
| **V7** | `V7__compliance_evidence_and_alert_delivery.sql` | Compliance obligation evidence file storage metadata (`storage_path`, `mime_type`, `file_size_bytes`), alert delivery retry engine (`delivery_attempts`, `alert_delivery_attempts` table). |

---

## 9. Development vs. Production Considerations

1. **Authentication (`DevTokenService`)**:
   - The development environment issues mock JWT Bearer tokens for pre-seeded user accounts (`admin@aicte-india.gov.in`, `scrutiny@aicte-india.gov.in`, `evc@aicte-india.gov.in`, `principal@abctech.edu.in`).
   - Production must integrate with an OIDC / OAuth2 identity provider (e.g. Keycloak or National Single Sign-On / MeriPehchaan).

2. **Notification Delivery Adapter (`DevMockNotificationDeliveryAdapter`)**:
   - In development and automated test runs, notification delivery is simulated in-memory via `DevMockNotificationDeliveryAdapter` with configurable transient and permanent failure triggers.
   - Production requires binding an enterprise SMS/SMTP gateway adapter (e.g. NIC SMS Gateway, AWS SES/SNS) implementing `NotificationDeliveryAdapter`.

3. **Storage Engine (`FileStorageService`)**:
   - Uploaded compliance evidence and institutional documents are written to the local filesystem (`target/storage` or configured disk path).
   - In multi-instance or cloud production environments, configure an S3-compliant object store (AWS S3, MinIO) with server-side encryption.


## Document upload and OCR

Open **Document Registry** at `/documents`, choose **Upload Document & Extract Text**, select a real PDF, PNG, or JPEG file, and submit it. The upload does not create a placeholder file when no file is selected. The backend stores the original file in its configured storage directory, computes SHA-256, and records OCR output and status against the document and version.

- Text-based PDF files: extract selectable text with PDFBox.
- Scanned PDF files: render up to the configured page limit and run Tesseract OCR on each rendered page.
- PNG/JPEG files: run Tesseract OCR directly.
- Output: open **View** on the document to see **OCR / Extracted Document Text**, status and diagnostic message.

### OCR prerequisites

Tesseract must be installed on the backend host for scanned PDF/image OCR. On Arch Linux, install `tesseract` and the language data package, for example `sudo pacman -S tesseract tesseract-data-eng`. On Debian/Ubuntu, run `sudo apt install tesseract-ocr tesseract-ocr-eng`. Validate with `tesseract --version`. If Tesseract is missing, upload/storage can still succeed but OCR is marked `OCR_UNAVAILABLE`; it is not reported as successful extraction.

Configuration environment variables (also listed in `.env.example`):

| Variable | Default | Purpose |
|---|---|---|
| `MAX_UPLOAD_FILE_SIZE` | `25MB` | Max size of each uploaded file |
| `MAX_UPLOAD_REQUEST_SIZE` | `26MB` | Multipart request size limit |
| `TESSERACT_CMD` | `tesseract` | Tesseract executable path |
| `OCR_LANGUAGE` | `eng` | Installed Tesseract language code(s), e.g. `eng` or `eng+hin` |
| `OCR_MAX_PDF_PAGES` | `10` | Maximum scanned PDF pages processed per upload |
| `OCR_TIMEOUT_SECONDS` | `45` | Maximum runtime per Tesseract invocation |

The OCR pipeline is an extraction aid only; it does not certify a document's authenticity or make an approval decision. Read the stored document carefully and have the authorized reviewer verify it.
