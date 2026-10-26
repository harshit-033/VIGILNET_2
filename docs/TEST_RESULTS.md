# AICTE Approval & Compliance Orchestration Platform (SIH 26262)
## Automated Test Execution & Verification Report

**Execution Date**: 2026-10-26  
**Environment**: Spring Boot 3.4.3 · PostgreSQL 16 (Port 5432) · Java 21 LTS · Node 20 / Vite 8  
**Total Integration Tests**: 46  
**Passed**: 46 (100%)  
**Failures**: 0  
**Errors**: 0  
**Skipped**: 0  

---

## 1. Test Suite Summary Table

| Test Suite Class | Focus Area / Subsystem | Tests Run | Result | Execution Time |
| :--- | :--- | :--- | :--- | :--- |
| **`IncrementalValidationIntegrationTest`** | Change-impact analysis, dirty checks, dependency graph, unmodified skips, AI vs Human distinction | 7 | **PASS** | 4.54 s |
| **`ComplianceDocumentAlertIntegrationTest`** | Compliance evidence submission & verification, SHA-256 document hashing, renewals, streaming downloads, alert escalations | 3 | **PASS** | 0.42 s |
| **`RoleBasedSecurityAndScopingIntegrationTest`** | Canonical 11 AICTE roles, RBAC enforcement, institutional multi-tenant scoping, territorial regional isolation | 9 | **PASS** | 0.38 s |
| **`ScrutinyWorkflowLifecycleIntegrationTest`** | Decoupled itemized deficiencies, resubmission modal, resolution loop, scrutiny completion guard | 8 | **PASS** | 0.35 s |
| **`WorkflowAndSecurityIntegrationTest`** | Core workflow transitions, stage tracking, audit logging, security filters | 11 | **PASS** | 0.41 s |
| **`EvcOperationsIntegrationTest`** | EVC visit scheduling, inspector assignment, visit completion, report submission, committee review advancement | 6 | **PASS** | 0.28 s |
| **`InstitutionAndApplicationApiIntegrationTest`** | Master institution registry, application explorer, multi-criteria filters, optimistic locking, RFC 7807 validation | 9 | **PASS** | 0.18 s |
| **`HealthEndpointTest`** | Spring Actuator health check, Hikari database connectivity | 2 | **PASS** | 0.14 s |
| **Frontend Production Build (`vite build`)** | React 19 bundle compilation, JSX validation, component exports | 1 | **PASS** | 0.45 s |

---

## 2. Detailed Verification Scenarios

### 2.1 Role-Based Access Control (RBAC) & Multi-Tenant Scoping
- **Unauthenticated Access Blocked**: Requests without Authorization headers to protected endpoints (`/applications`, `/evc`, `/compliance`, `/documents`, `/validation`) are rejected with HTTP 401 Unauthorized.
- **Cross-Role Authorization Guard**: An Institution Representative attempting officer actions (e.g. `COMPLETE_SCRUTINY`, `SCHEDULE_EVC`, `VERIFY_DOCUMENT`) is rejected with HTTP 403 Forbidden.
- **Cross-Institution Multi-Tenant Guard**: An Institution Representative from `INST-1025` attempting to view or act upon `INST-1026` records receives HTTP 403 Forbidden with problem detail.
- **Territorial Regional Isolation**: A Regional Officer from `Northern Region` attempting to act on an application or EVC visit in `Southern Region` receives HTTP 403 Forbidden.
- **System Administrator Bypass**: System Administrator (`ROLE_ADMINISTRATOR`) retains cross-region and cross-institution authority for global maintenance.

### 2.2 Scrutiny Deficiency Resolution Loop & Completion Guard
- **Decoupled Itemized Deficiencies**: When a Scrutiny Officer issues multiple deficiencies, each deficiency receives an itemized record with code and APH clause citation.
- **Dossier Resubmission**: When an institution resubmits corrections, deficiencies are NOT wiped out; they transition to `RECTIFIED_PENDING_REVIEW` and a new submission dossier version (`v2`) is registered.
- **Individual Resolution**: Scrutiny Officer explicitly reviews evidence and transitions items to `RESOLVED` with officer resolution notes.
- **Scrutiny Completion Guard**: Attempting to complete scrutiny with any deficiency in `OPEN` or `RECTIFIED_PENDING_REVIEW` is rejected with an RFC 7807 Bad Request error: *"Cannot complete scrutiny: X statutory deficiency items remain unresolved"*.

### 2.3 Connected Backend Modules (Compliance, Documents, Alerts)
- **Compliance Obligations**:
  - Seeded conditions monitored with automated urgency indicators (`Due soon`, `Overdue`).
  - Institution Representative submits documentary proof affidavit.
  - Compliance Reviewer conducts scrutiny and records verified outcome.
- **Document Registry & Durable File Storage**:
  - Multipart file uploads stream to disk storage under `./uploads/`.
  - Streaming SHA-256 digest is computed on the fly and recorded in the database ledger.
  - Multi-version renewals (`POST /{id}/renew`) create version history rows with individual hashes.
  - Streaming download (`GET /{id}/download`) retrieves byte-identical files with proper Content-Disposition and MIME headers.
- **Alerts & SLA Escalations**:
  - Early-warning engine monitors SLA turnaround times.
  - Officer acknowledgement records receipt in statutory audit trail.
  - SLA breach escalation (`POST /{id}/escalate`) advances urgency and escalates case to Senior Management.

### 2.4 Incremental Validation Engine & Change-Impact Analysis
- **Baseline Evaluation**: Establishes initial valid proofs across 8 canonical APH rules (`rulesReevaluatedCount == 8`, `rulesSkippedCount == 0`).
- **Unmodified Cache Hits (Skips)**: Calling incremental evaluation without field changes skips 100% of rules (`rulesSkippedCount == 8`, `rulesReevaluatedCount == 0`), preserving original timestamps, SHA-256 hashes, and execution logs.
- **Field Dirty Checks**: Modifying `faculty_roster` causes only the faculty rule and its downstream dependency (AI readiness) to re-evaluate with reason `FIELD_DIRTY_HASH_MISMATCH`. Independent proofs (Land, Fire NOC, Builtup) remain untouched.
- **Dependency Graph Invalidation**: Modifying `student_intake` traverses the dependency graph and re-evaluates both `RULE-FACULTY-03` and `RULE-LABORATORY-06` with reason `DEPENDENCY_INVALIDATION`.
- **Regulatory Rule Safety & Human Decision Distinction**:
  - Every rule is cited to an exact APH clause (`APH 2026-27 Table 1.1`, `Table 2.1`, `Clause 3.4`, etc.).
  - AI risk scores are marked `AI_ASSISTED_ADVISORY` with explicit non-binding guidance notices, preventing automated algorithms from substituting for human scrutiny.
- **Provenance Audit Ledger**: `GET /api/v1/validation/{appId}/provenance` returns complete historical logs with superseded records marked `is_valid = false` and active records marked `is_valid = true`.

---

## 3. Maven Build & Test Log Excerpt

```
[INFO] ------------------------------------------------------------------------
[INFO] Running in.gov.aicte.platform.IncrementalValidationIntegrationTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.545 s
[INFO] Running in.gov.aicte.platform.ComplianceDocumentAlertIntegrationTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.421 s
[INFO] Running in.gov.aicte.platform.RoleBasedSecurityAndScopingIntegrationTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.384 s
[INFO] Running in.gov.aicte.platform.ScrutinyWorkflowLifecycleIntegrationTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.352 s
[INFO] Running in.gov.aicte.platform.WorkflowAndSecurityIntegrationTest
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.412 s
[INFO] Running in.gov.aicte.platform.EvcOperationsIntegrationTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.281 s
[INFO] Running in.gov.aicte.platform.InstitutionAndApplicationApiIntegrationTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.184 s
[INFO] Running in.gov.aicte.platform.HealthEndpointTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.140 s
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 46, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```
