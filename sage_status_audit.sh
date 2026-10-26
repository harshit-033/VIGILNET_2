#!/usr/bin/env bash
# SAGE local status audit. Read-only against source files; writes a Markdown report and logs.
# Usage: ./scripts/sage_status_audit.sh [optional-report-path]
# Exit code: 0 = no failed checks (skips/warnings may remain), 1 = one or more failed checks.
set -uo pipefail

SCRIPT_PATH="$(readlink -f "${BASH_SOURCE[0]}" 2>/dev/null || realpath "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(cd "$(dirname "$SCRIPT_PATH")" && pwd)"
ROOT_DIR="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null || true)"
if [ -z "$ROOT_DIR" ]; then ROOT_DIR="$(git rev-parse --show-toplevel 2>/dev/null || true)"; fi
if [ -z "$ROOT_DIR" ]; then ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"; fi
STAMP="$(date '+%Y%m%d-%H%M%S')"
REPORT_PATH="${1:-${SAGE_AUDIT_REPORT:-$ROOT_DIR/docs/FINAL_STATUS_REPORT.md}}"
REPORT_DIR="$(dirname "$REPORT_PATH")"
LOG_DIR="${SAGE_AUDIT_LOG_DIR:-${TMPDIR:-/tmp}/sage-status-audit-$STAMP}"

mkdir -p "$REPORT_DIR" "$LOG_DIR" || { echo "ERROR: Cannot create report/log directory." >&2; exit 2; }
REPORT_PATH="$(cd "$REPORT_DIR" && pwd)/$(basename "$REPORT_PATH")"
if [ -f "$REPORT_PATH" ]; then cp -p "$REPORT_PATH" "$REPORT_PATH.previous-$STAMP"; fi

CHECKS=()
FAIL_COUNT=0
PASS_COUNT=0
WARN_COUNT=0
SKIP_COUNT=0

trim() { printf '%s' "$*" | tr '\n\r|' '   ' | sed 's/[[:space:]]\+/ /g; s/^ //; s/ $//'; }

add_result() {
  local id="$1" status="$2" label="$3" detail="$4" log="$5"
  detail="$(trim "$detail")"
  CHECKS+=("$id|$status|$label|$detail|$log")
  case "$status" in
    PASS) PASS_COUNT=$((PASS_COUNT + 1)) ;;
    FAIL) FAIL_COUNT=$((FAIL_COUNT + 1)) ;;
    WARN) WARN_COUNT=$((WARN_COUNT + 1)) ;;
    SKIP) SKIP_COUNT=$((SKIP_COUNT + 1)) ;;
  esac
}

pass_check() { add_result "$1" PASS "$2" "$3" ""; }
fail_check() { add_result "$1" FAIL "$2" "$3" ""; }
warn_check() { add_result "$1" WARN "$2" "$3" ""; }
skip_check() { add_result "$1" SKIP "$2" "$3" ""; }

run_logged() {
  local id="$1" label="$2"; shift 2
  local logfile="$LOG_DIR/$id.log" rc
  if command -v timeout >/dev/null 2>&1; then
    timeout "${SAGE_COMMAND_TIMEOUT_SECONDS:-600}" "$@" >"$logfile" 2>&1
    rc=$?
  else
    "$@" >"$logfile" 2>&1
    rc=$?
  fi
  if [ "$rc" -eq 0 ]; then
    add_result "$id" PASS "$label" "Command completed successfully. Log: $logfile" "$logfile"
  else
    local excerpt
    excerpt="$(tail -n 8 "$logfile" 2>/dev/null | tr '\n\r|' '   ' | sed 's/[[:space:]]\+/ /g')"
    add_result "$id" FAIL "$label" "Exit code $rc. Tail: $excerpt. Full log: $logfile" "$logfile"
  fi
  return 0
}

require_file() {
  local id="$1" file="$2" label="$3"
  if [ -f "$ROOT_DIR/$file" ]; then pass_check "$id" "$label" "Found $file"; else fail_check "$id" "$label" "Missing required file: $file"; fi
}

expect_pattern() {
  local id="$1" file="$2" pattern="$3" label="$4" ok_detail="$5" bad_detail="$6"
  if [ ! -f "$ROOT_DIR/$file" ]; then
    fail_check "$id" "$label" "Cannot inspect missing file: $file"
  elif grep -Eiq -- "$pattern" "$ROOT_DIR/$file"; then
    pass_check "$id" "$label" "$ok_detail"
  else
    fail_check "$id" "$label" "$bad_detail"
  fi
}

check_no_pattern() {
  local id="$1" file="$2" pattern="$3" label="$4" fail_detail="$5"
  if [ ! -f "$ROOT_DIR/$file" ]; then
    fail_check "$id" "$label" "Cannot inspect missing file: $file"
  elif grep -Eq -- "$pattern" "$ROOT_DIR/$file"; then
    fail_check "$id" "$label" "$fail_detail"
  else
    pass_check "$id" "$label" "No matching risky pattern found in $file"
  fi
}

cd "$ROOT_DIR" || { echo "ERROR: Cannot enter repository root $ROOT_DIR" >&2; exit 2; }

# Capture current VCS facts without changing the working tree.
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  BRANCH="$(git branch --show-current 2>/dev/null || true)"
  HEAD_SHA="$(git rev-parse HEAD 2>/dev/null || echo unknown)"
  HEAD_SUBJECT="$(git log -1 --pretty=%s 2>/dev/null || echo unknown)"
  REMOTE_URL="$(git remote get-url origin 2>/dev/null || echo 'origin remote not configured')"
  REMOTE_URL="$(printf '%s' "$REMOTE_URL" | sed -E 's#(https?://)[^@]+@#\1#g')"
  GIT_STATUS="$(git status --short 2>/dev/null || true)"
  if [ -n "$GIT_STATUS" ]; then GIT_DIRTY="Yes — uncommitted files are present"; else GIT_DIRTY="No — working tree is clean"; fi
  pass_check "git_repo" "Git repository" "Root: $ROOT_DIR"
else
  BRANCH="not a git repository"; HEAD_SHA="unknown"; HEAD_SUBJECT="unknown"; REMOTE_URL="unknown"; GIT_STATUS=""; GIT_DIRTY="unknown"
  fail_check "git_repo" "Git repository" "Could not identify a Git work tree from $ROOT_DIR"
fi

# Toolchain inventory.
JAVA_VERSION="$(java -version 2>&1 | head -n 1 || echo 'java unavailable')"
NODE_VERSION="$(node --version 2>/dev/null || echo 'node unavailable')"
NPM_VERSION="$(npm --version 2>/dev/null || echo 'npm unavailable')"
if [ -z "${TESSDATA_PREFIX:-}" ] && [ -d "$HOME/.local/share/tessdata" ]; then
  export TESSDATA_PREFIX="$HOME/.local/share/tessdata"
fi
if command -v tesseract >/dev/null 2>&1; then
  TESSERACT_VERSION="$(tesseract --version 2>&1 | head -n 1 || true)"
  TESSERACT_LANGS="$(tesseract --list-langs 2>&1 | tail -n +2 | tr '\n' ',' | sed 's/,$//')"
else
  TESSERACT_VERSION="not installed or not on PATH"; TESSERACT_LANGS="unavailable"
fi

# Required project surface.
require_file "file_readme" "README.md" "Main project documentation"
require_file "file_backend_pom" "pom.xml" "Spring Boot/Maven configuration"
require_file "file_frontend_package" "frontend/package.json" "Frontend package manifest"
require_file "file_doc_page" "frontend/src/pages/DocumentsPage.jsx" "Document Registry UI"
require_file "file_api_context" "frontend/src/context/DataContext.jsx" "Frontend API/upload data context"
require_file "file_doc_controller" "src/main/java/in/gov/aicte/platform/document/DocumentController.java" "Document upload/download API"
require_file "file_doc_service" "src/main/java/in/gov/aicte/platform/document/DocumentService.java" "Document workflow service"
require_file "file_storage" "src/main/java/in/gov/aicte/platform/document/FileStorageService.java" "Durable file storage and digest"
require_file "file_ocr_service" "src/main/java/in/gov/aicte/platform/document/OcrService.java" "OCR service implementation"
if [ -f "$ROOT_DIR/src/test/java/in/gov/aicte/platform/document/OcrServiceTest.java" ]; then
  require_file "file_ocr_test" "src/test/java/in/gov/aicte/platform/document/OcrServiceTest.java" "OCR regression test"
else
  require_file "file_ocr_test" "src/test/java/in/gov/aicte/platform/OcrServiceTest.java" "OCR regression test"
fi
require_file "file_migration_v8" "src/main/resources/db/migration/V8__document_ocr_extraction.sql" "OCR database migration"
require_file "file_run_guide" "docs/LOCAL_RUN_AND_TEST_GUIDE.md" "Local run/test guide"
require_file "file_checkpoint" "docs/PROJECT_CHECKPOINT.md" "Project checkpoint"

# Functionality/configuration checks. These are source-level evidence checks, not substitutes for runtime tests.
expect_pattern "upload_api" "src/main/java/in/gov/aicte/platform/document/DocumentController.java" '@PostMapping.*"/upload"' "Multipart upload endpoint" "DocumentController declares the /upload multipart endpoint." "No /upload multipart endpoint found in DocumentController."
expect_pattern "upload_auth" "src/main/java/in/gov/aicte/platform/document/DocumentController.java" 'PreAuthorize.*INSTITUTION_REPRESENTATIVE' "Upload role protection" "Upload endpoint includes a role guard; review the complete role expression." "Could not find role protection for document upload."
expect_pattern "storage_hash" "src/main/java/in/gov/aicte/platform/document/FileStorageService.java" 'SHA-256|SHA256|MessageDigest' "File integrity hash" "Storage service includes SHA-256 digest logic." "No digest implementation found in FileStorageService."
expect_pattern "ocr_pdfbox" "pom.xml" 'org.apache.pdfbox|pdfbox' "PDF text extraction dependency" "PDFBox is declared in pom.xml." "PDFBox dependency not found in pom.xml."
expect_pattern "ocr_tesseract" "src/main/java/in/gov/aicte/platform/document/OcrService.java" 'tesseract|TESSERACT_CMD' "Tesseract OCR integration" "OCR service references Tesseract." "Tesseract integration not found in OCR service."
expect_pattern "ocr_is_called" "src/main/java/in/gov/aicte/platform/document/DocumentService.java" 'ocrService\.extract' "OCR invoked in document workflow" "Document service calls OCR extraction; verify upload and renewal paths during review." "DocumentService does not visibly call ocrService.extract."
expect_pattern "ocr_persist_status" "src/main/resources/db/migration/V8__document_ocr_extraction.sql" 'ocr_status' "Persisted OCR status schema" "Migration adds an OCR status column." "V8 migration does not declare ocr_status."
expect_pattern "ocr_persist_text" "src/main/resources/db/migration/V8__document_ocr_extraction.sql" 'extracted_text' "Persisted OCR text schema" "Migration adds extracted text columns." "V8 migration does not declare extracted_text."
expect_pattern "ocr_ui" "frontend/src/pages/DocumentsPage.jsx" 'OCR / Extracted Document Text|Extracted Document Text' "OCR results visible in UI" "Document detail UI has an OCR text panel." "OCR result panel was not found in DocumentsPage."
expect_pattern "ocr_ui_mapping" "frontend/src/context/DataContext.jsx" 'extractedText|ocrStatus' "OCR result API mapping" "DataContext maps OCR output fields." "DataContext does not visibly map OCR fields."
expect_pattern "upload_required_file" "frontend/src/pages/DocumentsPage.jsx" 'Select a real PDF|uploadFile\.size === 0|uploadFile\)' "UI requires an actual file" "Upload handler includes real-file validation." "Cannot confirm that the UI requires a selected file."
check_no_pattern "no_dummy_upload" "frontend/src/context/DataContext.jsx" 'new Blob.*textData|Statutory Certificate:|Renewed Certificate Version for:' "No dummy PDF upload fallback" "A fallback-generated Blob is still present; user uploads should never silently fabricate a document."
expect_pattern "renewal_api" "src/main/java/in/gov/aicte/platform/document/DocumentController.java" '@PostMapping.*"/\{id\}/renew"' "Renewal API exists" "Renewal endpoint found." "Renewal endpoint not found."
expect_pattern "db_migration" "src/main/resources/db/migration/V8__document_ocr_extraction.sql" 'ALTER TABLE document_registry' "OCR migration modifies document registry" "V8 migration alters document_registry." "OCR schema migration is incomplete."
expect_pattern "ocr_config" "src/main/resources/application.yml" 'max-pdf-pages|timeout-seconds|TESSERACT_CMD|app:' "OCR configuration" "Application configuration contains an OCR/configuration section; manually inspect all defaults." "OCR configuration markers not found."

if command -v tesseract >/dev/null 2>&1; then
  if printf '%s\n' "$TESSERACT_LANGS" | grep -Eq '(^|,)eng(,|$)'; then pass_check "tesseract_eng" "English OCR language data" "Tesseract is installed and eng appears in installed languages: $TESSERACT_LANGS";
  else warn_check "tesseract_eng" "English OCR language data" "Tesseract is installed but eng language data was not detected. Languages: $TESSERACT_LANGS"; fi
else
  warn_check "tesseract_installed" "Tesseract runtime dependency" "Tesseract was not found. PDF text extraction may still work; scanned PDF/image OCR will report unavailable until Tesseract and language data are installed." 
fi

# Version control/remote status.
if [ "$BRANCH" != "not a git repository" ]; then
  if [ -n "$GIT_STATUS" ]; then warn_check "git_clean" "Working tree state" "$GIT_DIRTY. Inspect git status before staging or committing."; else pass_check "git_clean" "Working tree state" "$GIT_DIRTY"; fi
  if git remote get-url origin >/dev/null 2>&1; then pass_check "git_remote" "Git remote" "origin: $REMOTE_URL"; else warn_check "git_remote" "Git remote" "No origin remote configured."; fi
  if git rev-parse --abbrev-ref '@{upstream}' >/dev/null 2>&1; then
    UPSTREAM="$(git rev-parse --abbrev-ref '@{upstream}')"
    COUNTS="$(git rev-list --left-right --count "HEAD...@{upstream}" 2>/dev/null || echo '? ?')"
    pass_check "git_upstream" "Branch upstream" "$UPSTREAM (ahead/behind counts: $COUNTS)"
  else
    warn_check "git_upstream" "Branch upstream" "Current branch has no upstream configured; remote sync status is not verified."
  fi
  if command -v gh >/dev/null 2>&1; then
    if gh auth status >/dev/null 2>&1; then
      gh pr view --json number,state,url,headRefName,baseRefName,isDraft,mergedAt,statusCheckRollup > "$LOG_DIR/github-pr.json" 2> "$LOG_DIR/github-pr.err"
      if [ "$?" -eq 0 ]; then
        pass_check "github_pr" "GitHub pull request context" "gh pr view returned PR metadata for the current branch. Details: $LOG_DIR/github-pr.json"
      else
        warn_check "github_pr" "GitHub pull request context" "gh is authenticated but no PR metadata was returned for this branch/current directory. See $LOG_DIR/github-pr.err"
      fi
    else
      warn_check "github_pr" "GitHub pull request context" "GitHub CLI is installed but not authenticated; remote PR/check status was not verified."
    fi
  else
    skip_check "github_pr" "GitHub pull request context" "GitHub CLI (gh) not installed; report cannot verify remote PR/CI status."
  fi
fi

# Runtime test/build steps. Missing prerequisites/dependencies are marked SKIP rather than fabricated as passes.
if [ -x "$ROOT_DIR/mvnw" ] && command -v java >/dev/null 2>&1; then
  if [ "${SAGE_SKIP_TESTS:-0}" = "1" ]; then skip_check "backend_tests" "Backend Maven tests" "SAGE_SKIP_TESTS=1; tests were explicitly skipped.";
  else run_logged "backend_tests" "Backend Maven tests" ./mvnw --batch-mode test; fi
else
  skip_check "backend_tests" "Backend Maven tests" "Executable ./mvnw and Java are both required. Detected: $JAVA_VERSION"
fi

if [ -f "$ROOT_DIR/frontend/package.json" ] && command -v npm >/dev/null 2>&1; then
  if [ ! -d "$ROOT_DIR/frontend/node_modules" ]; then
    skip_check "frontend_tests" "Frontend tests" "frontend/node_modules is missing. Run npm ci in frontend/ and rerun the audit."
    skip_check "frontend_build" "Frontend production build" "frontend/node_modules is missing. Run npm ci in frontend/ and rerun the audit."
    skip_check "frontend_lint" "Frontend lint" "frontend/node_modules is missing. Run npm ci in frontend/ and rerun the audit."
  else
    if node -e "process.exit(require('./frontend/package.json').scripts?.test ? 0 : 1)" >/dev/null 2>&1; then run_logged "frontend_tests" "Frontend tests" npm --prefix frontend test; else skip_check "frontend_tests" "Frontend tests" "No test script defined in frontend/package.json."; fi
    if node -e "process.exit(require('./frontend/package.json').scripts?.build ? 0 : 1)" >/dev/null 2>&1; then run_logged "frontend_build" "Frontend production build" npm --prefix frontend run build; else skip_check "frontend_build" "Frontend production build" "No build script defined in frontend/package.json."; fi
    if node -e "process.exit(require('./frontend/package.json').scripts?.lint ? 0 : 1)" >/dev/null 2>&1; then run_logged "frontend_lint" "Frontend lint" npm --prefix frontend run lint; else skip_check "frontend_lint" "Frontend lint" "No lint script defined in frontend/package.json."; fi
  fi
else
  skip_check "frontend_tests" "Frontend tests" "npm or frontend/package.json unavailable."
  skip_check "frontend_build" "Frontend production build" "npm or frontend/package.json unavailable."
  skip_check "frontend_lint" "Frontend lint" "npm or frontend/package.json unavailable."
fi

# Gather key source file references and hashes for a reproducible report.
FILE_SHA_ROWS=""
for file in \
  "frontend/src/pages/DocumentsPage.jsx" \
  "frontend/src/context/DataContext.jsx" \
  "src/main/java/in/gov/aicte/platform/document/DocumentController.java" \
  "src/main/java/in/gov/aicte/platform/document/DocumentService.java" \
  "src/main/java/in/gov/aicte/platform/document/OcrService.java" \
  "src/main/resources/db/migration/V8__document_ocr_extraction.sql" \
  "src/test/java/in/gov/aicte/platform/OcrServiceTest.java" \
  "src/test/java/in/gov/aicte/platform/document/OcrServiceTest.java"; do
  if [ -f "$file" ]; then
    hash="$(sha256sum "$file" 2>/dev/null | awk '{print $1}')"
    FILE_SHA_ROWS+="| \`$file\` | \`$hash\` |\n"
  fi
done

# Report creation (the report is the intended output of this script).
{
  echo "# SAGE — Final Status Audit Report"
  echo
  echo "- **Generated:** $(date '+%Y-%m-%d %H:%M:%S %Z')"
  echo "- **Repository root:** \`$ROOT_DIR\`"
  echo "- **Branch:** \`$BRANCH\`"
  echo "- **HEAD:** \`$HEAD_SHA\` — $HEAD_SUBJECT"
  echo "- **Origin:** $REMOTE_URL"
  echo "- **Working tree:** $GIT_DIRTY"
  echo "- **Audit logs:** \`$LOG_DIR\`"
  echo
  echo "> **Scope note:** This is an automated source/environment/test audit. A passing grep or build does not prove business-rule correctness, production security, OCR accuracy on real certificates, or successful end-to-end user acceptance. The manual review section must be completed by the agent/operator." 
  echo
  echo "## Executive summary"
  echo
  echo "| Result | Count |"
  echo "|---|---:|"
  echo "| PASS | $PASS_COUNT |"
  echo "| FAIL | $FAIL_COUNT |"
  echo "| WARN | $WARN_COUNT |"
  echo "| SKIP | $SKIP_COUNT |"
  if [ "$FAIL_COUNT" -eq 0 ] && [ "$SKIP_COUNT" -eq 0 ]; then
    echo
    echo "**Automated result: PASS.** No automated checks failed or were skipped. Manual review is still required."
  elif [ "$FAIL_COUNT" -eq 0 ]; then
    echo
    echo "**Automated result: PARTIAL.** No automated checks failed, but one or more checks were skipped. Do not interpret skips as passes."
  else
    echo
    echo "**Automated result: NEEDS ATTENTION.** At least one automated check failed. See findings and command logs below."
  fi
  echo
  echo "## Automated check results"
  echo
  echo "| ID | Status | Check | Result / evidence |"
  echo "|---|---|---|---|"
  for row in "${CHECKS[@]}"; do
    IFS='|' read -r id status label detail log <<< "$row"
    echo "| \`$id\` | **$status** | $label | $detail |"
  done
  echo
  echo "## Git and version-control state"
  echo
  echo "- **Branch:** \`$BRANCH\`"
  echo "- **Commit:** \`$HEAD_SHA\`"
  echo "- **Commit subject:** $HEAD_SUBJECT"
  echo "- **Origin:** $REMOTE_URL"
  echo "- **Working tree:** $GIT_DIRTY"
  echo
  if [ -n "$GIT_STATUS" ]; then
    echo "Current changes (not modified by this audit script):"
    echo '```text'
    printf '%s\n' "$GIT_STATUS"
    echo '```'
  else
    echo "No uncommitted changes were present when the audit began."
  fi
  echo
  echo "## Runtime toolchain"
  echo
  echo "- **Java:** $JAVA_VERSION"
  echo "- **Node.js:** $NODE_VERSION"
  echo "- **npm:** $NPM_VERSION"
  echo "- **Tesseract:** $TESSERACT_VERSION"
  echo "- **Tesseract languages:** $TESSERACT_LANGS"
  echo
  echo "## Required manual review checklist"
  echo
  echo "Mark each item PASS / PARTIAL / FAIL / NOT VERIFIED, and cite a file, test name, log or screenshot. The automation cannot truthfully determine all of these from source patterns alone."
  echo
  echo "| Area | Required verification | Status | Evidence / findings |"
  echo "|---|---|---|---|"
  echo "| Document upload | Real PDF/PNG/JPEG upload succeeds; missing file rejected; empty/oversize/unsupported files rejected; stored bytes match original | NOT VERIFIED | Add manual/API test evidence |"
  echo "| Document access control | User cannot list, view, download, renew or upload documents outside the permitted institution/region; reviewers' intended roles are enforced server-side | NOT VERIFIED | Add role and tenant-isolation test evidence |"
  echo "| OCR | Text PDF extraction, scanned PDF OCR, image OCR, no-text files, Tesseract missing, timeout, multi-page limit and non-English language configuration are tested | NOT VERIFIED | Add sample files, expected text and test/log references |"
  echo "| OCR persistence | Extracted text and status survive a backend restart and appear in the document details/version history | NOT VERIFIED | Add API/database/UI evidence |"
  echo "| Document versioning | Renewal uploads create a new version and keep historical file/hash/text; no old file is silently overwritten | NOT VERIFIED | Add renewal/version test evidence |"
  echo "| File integrity and safety | SHA-256 validated; extension, MIME/signature, size and malformed file handling reviewed; filenames and storage paths cannot escape storage root | NOT VERIFIED | Digest is not proof of authenticity |"
  echo "| Compliance/evidence | Upload/download, institution isolation, hash metadata and review workflow tested end-to-end | NOT VERIFIED | Separate compliance-evidence flow from document registry flow |"
  echo "| AI/rule checks | Identify rule engine, AI model/API, prompts, deterministic checks, outputs, uncertainty and human approval; clearly state what is mock/demo/not implemented | NOT VERIFIED | Do not call OCR an AI compliance decision |"
  echo "| Alerts/retries | Retry limits, persisted attempts, failure/retry path, real vs mock provider and scheduler/manual trigger verified | NOT VERIFIED | Confirm delivery is simulated or real |"
  echo "| Frontend | Correct role navigation, upload UI, loading/error states, download, renewal and OCR details tested against the live backend | NOT VERIFIED | Distinguish mock data from API-backed data |"
  echo "| Database | All Flyway migrations apply to a clean DB and an upgraded existing DB; schema validates against JPA; seeded file paths are not presented as real uploads | NOT VERIFIED | Include migration logs |"
  echo "| API documentation | Endpoints, auth roles, multipart fields, response schema, error codes and limits match implementation | NOT VERIFIED | Update OpenAPI/manual docs if mismatched |"
  echo "| Deployment | .env.example, secrets, CORS/base URL, database setup, OCR installation, upload directory permissions and persistence configured | NOT VERIFIED | Test clean-machine setup |"
  echo "| Version control | Correct feature branch/PR, clean diff understood, no secrets or generated uploads committed, current CI status verified remotely | NOT VERIFIED | This script does not push, merge or commit |"
  echo
  echo "## Source file fingerprints"
  echo
  echo "| File | SHA-256 |"
  echo "|---|---|"
  printf '%b' "$FILE_SHA_ROWS"
  echo
  echo "## Known caveats"
  echo
  echo "1. Tesseract must be installed on the backend host for scanned-document OCR; local availability does not prove deployment availability."
  echo "2. OCR output can be inaccurate. Reviewers must inspect the original file; OCR does not establish authenticity, compliance or approval."
  echo "3. A source-level check is not a substitute for a runtime/API/UI test. Skipped tests must remain explicitly skipped."
  echo "4. Remote PR/CI status is only checked if GitHub CLI is installed, authenticated and able to query the current branch's PR."
  echo
  echo "## Findings requiring action"
  echo
  if [ "$FAIL_COUNT" -eq 0 ]; then
    echo "- No automated check is marked FAIL by this script. Review all WARN/SKIP items and complete the manual checklist before calling the project release-ready."
  else
    echo "- Resolve each **FAIL** row in the automated check table. Use the linked full logs where a command failed."
  fi
  echo "- Antigravity/operator should append concise findings, exact code/test references, prioritised defects (P0/P1/P2/P3), recommended fixes and a final READY / READY WITH LIMITATIONS / NOT READY decision after manual review."
  echo
  echo "## Detailed command logs"
  echo
  echo "Logs are stored at: \`$LOG_DIR\`."
} > "$REPORT_PATH"

printf '\nSAGE audit complete.\nReport: %s\nLogs:   %s\nPASS=%s FAIL=%s WARN=%s SKIP=%s\n' "$REPORT_PATH" "$LOG_DIR" "$PASS_COUNT" "$FAIL_COUNT" "$WARN_COUNT" "$SKIP_COUNT"
if [ "$FAIL_COUNT" -gt 0 ]; then exit 1; fi
exit 0
