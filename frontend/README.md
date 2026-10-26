# SIH 26262 - Technical Institution Approval & Compliance Frontend

React 19 + Vite frontend client for the AICTE Approval & Compliance Orchestration Platform.

## Integration Overview

This frontend connects to the Spring Boot 3.4.3 + PostgreSQL 18 backend for:
- Server-side development Bearer authentication via `POST /api/v1/auth/token`.
- Application list and detail retrieval from PostgreSQL via `GET /api/v1/applications`.
- Live workflow actions persisted directly to PostgreSQL via `POST /api/v1/applications/{id}/actions`:
  - `RETURN_FOR_CORRECTION`
  - `SUBMIT_CORRECTIONS`
  - `COMPLETE_SCRUTINY`

Non-workflow modules (EVC Scheduling, Compliance Obligations, Statutory Documents, and Alerts) remain safely stored in browser `localStorage` as designed for this milestone.

## Prerequisites

- Node.js 18+ (tested on Node.js 20/22/26)
- Running backend server at `http://localhost:8080`

## Configuration

Set the API base URL in `.env` (or accept default):
```dotenv
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

## Running Locally

1. Install dependencies:
   ```bash
   npm install
   ```

2. Start the development server:
   ```bash
   npm run dev
   ```
   Open `http://localhost:5173`.

3. Build production bundle:
   ```bash
   npm run build
   ```

## Workflow Demonstration Journey

1. **Role Authentication**: Switch role in the top-right header (e.g. Scrutiny Officer). The app calls `POST /api/v1/auth/token` and stores the Bearer token.
2. **Application Dossier**: Navigate to `/applications/APP-2026-1042`. Real data is fetched from PostgreSQL.
3. **Return for Correction**: Click "Return for Correction", fill in deficiencies, and submit. The backend persists the transition in PostgreSQL.
4. **Submit Corrections**: Switch role to Institution Representative and submit rectifications.
5. **Complete Scrutiny**: Switch back to Scrutiny Officer and complete scrutiny to advance the application to `EVC scheduling`.
