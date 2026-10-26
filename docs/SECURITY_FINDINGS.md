# AICTE Approval & Compliance Orchestration Platform (SIH 26262)
## Security Findings & Vulnerability Assessment Report (Phase 1)

**Inspection Date**: 2026-10-26  
**Auditor**: Automated Security & Baseline Inspector  
**Classification**: CONFIDENTIAL / SECURITY REMEDIATION PLAN  
**Status**: ACTIVE FINDINGS & IMMEDIATE MITIGATIONS  

---

## 1. Executive Summary

A comprehensive security audit of the repository, environment configurations, credential handling, Spring Security architecture, and API endpoints was conducted in accordance with Phase 1 requirements.

### Key Takeaways:
- **Zero Real Credentials Committed**: No production passwords, live tokens, private keys, or actual AICTE credentials are committed to version control.
- **Gitignore Protection Active**: `.env`, `.env.*`, `target/`, and `node_modules/` are strictly ignored.
- **High-Risk Architectural Gaps Identified**:
  1. Default fallback password in `application.yml` for database connectivity.
  2. Unrestricted dev-token authentication filter active in all application configurations.
  3. Missing method-level authorization checks (`@PreAuthorize`) on sensitive REST endpoints.
  4. Missing institutional tenant scoping and regional scope isolation.

---

## 2. Detailed Security Findings

### SEC-01: Insecure Default Password Fallback in Configuration
- **Severity**: HIGH (P1)
- **Component**: `src/main/resources/application.yml`
- **Finding**:
  ```yaml
  password: ${SPRING_DATASOURCE_PASSWORD:${DB_PASSWORD:aicte_123}}
  ```
  If started in production without setting `SPRING_DATASOURCE_PASSWORD` or `DB_PASSWORD`, the backend falls back to `aicte_123`.
- **Impact**: Risk of unauthorized database access if PostgreSQL is exposed or accessed across internal subnets.
- **Remediation**:
  Introduce explicit Spring Profile separation (`dev` vs. `prod`). In `prod` profile, configuration must fail fast on startup if `DB_PASSWORD` is absent, removing hardcoded fallback values.

---

### SEC-02: Dev Token Bridge Bypasses Cryptographic Signature Verification
- **Severity**: HIGH (P1)
- **Component**: `DevTokenService.java`, `DevBearerAuthenticationFilter.java`
- **Finding**:
  Tokens are constructed using simple string prefixes (`dev-token-USR-SO-01-...`) and cached in memory. The filter accepts any token matching a user pattern without cryptographic signature (HMAC/RSA) verification or expiration enforcement.
- **Impact**: In development/testing, this enables rapid prototyping without external OIDC infrastructure. However, if deployed in production, any user could fabricate valid Bearer tokens for arbitrary roles.
- **Remediation**:
  1. Guard the Dev Token filter with a strict configuration property: `app.security.dev-tokens-enabled: false` by default in production.
  2. Clearly mark all dev tokens and demo login buttons in the UI with a `[DEVELOPMENT ONLY]` badge.
  3. Ensure production profile requires standard signed JWT or OIDC Bearer tokens.

---

### SEC-03: Missing Role-Based Endpoint Authorization
- **Severity**: HIGH (P1)
- **Component**: `src/main/java/in/gov/aicte/platform/config/SecurityConfig.java`
- **Finding**:
  `SecurityConfig` configures:
  ```java
  .authorizeHttpRequests(auth -> auth
      .requestMatchers("/api/v1/health").permitAll()
      .requestMatchers("/api/v1/auth/**").permitAll()
      .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
      .anyRequest().authenticated()
  )
  ```
  While all other endpoints require *authentication*, they do NOT enforce *role-based authorization*. An authenticated Institution Representative could call `/api/v1/applications/{id}/actions` with action `COMPLETE_SCRUTINY`, or schedule EVC visits via `/api/v1/evc/{id}/schedule`.
- **Impact**: Privilege escalation across administrative roles.
- **Remediation**:
  1. Enable `@EnableMethodSecurity(prePostEnabled = true)`.
  2. Add `@PreAuthorize` guards on all controller methods (e.g., `hasAnyRole('SCRUTINY_OFFICER', 'REGIONAL_OFFICER')`).
  3. Validate caller role inside `WorkflowService` and `EvcService` domain logic.

---

### SEC-04: Lack of Institutional Tenant and Regional Isolation
- **Severity**: CRITICAL (P0)
- **Component**: `ApplicationService.java`, `EvcService.java`
- **Finding**:
  Endpoints accept an application or EVC ID and perform queries without verifying whether the authenticated user is authorized for that specific institution or region.
- **Impact**:
  - An Institution Representative from College A can query or modify dossiers belonging to College B.
  - A Regional Officer assigned to Northern Region could access and manipulate cases belonging to Western Region.
- **Remediation**:
  1. Retrieve `DevUserDto` / principal details in the service layer.
  2. If the user possesses role `ROLE_INSTITUTION_REPRESENTATIVE`, enforce `application.institution.institutionalIdentifier == user.institutionId`.
  3. If the user possesses role `ROLE_REGIONAL_OFFICER`, enforce `application.institution.region == user.region` (unless multi-region access is explicitly granted).

---

### SEC-05: Deficiencies Cleared Prior to Re-Scrutiny Verification
- **Severity**: MEDIUM (P2)
- **Component**: `WorkflowService.java` (Line 116)
- **Finding**:
  In action `SUBMIT_CORRECTIONS`:
  ```java
  application.setDeficienciesJson(null); // Cleared upon resubmission
  ```
  The application automatically clears deficiency records as soon as the institution submits corrections.
- **Impact**: Violates regulatory audit rules: deficiencies must remain preserved until an authorized Scrutiny Officer explicitly resolves them during re-scrutiny.
- **Remediation**:
  Maintain persistent deficiency entities with status (`OPEN`, `RECTIFIED_PENDING_REVIEW`, `RESOLVED`, `RE_ISSUED`).

---

### SEC-06: Unauthenticated File Operations Risk
- **Severity**: HIGH (P1)
- **Component**: Proposed Documents Module
- **Finding**:
  Currently, files are simulated in the UI. When implementing durable storage (Phase 4.3), files must not be stored with original user-supplied filenames, nor served via public static endpoints.
- **Remediation**:
  1. Store files using UUID-based storage keys.
  2. Calculate SHA-256 hashes on upload.
  3. Enforce authenticated, authorized file streaming through `/api/v1/documents/{id}/download`.

---

## 3. Prioritized Remediation Roadmap

| Priority | Defect ID | Description | Remediation Phase |
| :--- | :--- | :--- | :--- |
| **P0** | **SEC-04** | Enforce Institutional Tenant & Regional Scoping | Phase 2 & Phase 3 |
| **P1** | **SEC-01** | Remove default DB password fallback in production profile | Phase 1 (Immediate) |
| **P1** | **SEC-02** | Add production guard on Dev Token Filter | Phase 1 & Phase 3 |
| **P1** | **SEC-03** | Implement `@PreAuthorize` role matrix on all endpoints | Phase 3 |
| **P1** | **SEC-06** | Secure durable document storage with SHA-256 validation | Phase 4.3 |
| **P2** | **SEC-05** | Decouple deficiency entities from state machine clearing | Phase 2 |

---

## 4. Verification Check

All immediate Phase 1 security improvements will be verified with backend tests ensuring existing development workflows continue functioning smoothly while eliminating hardcoded production vulnerabilities.
