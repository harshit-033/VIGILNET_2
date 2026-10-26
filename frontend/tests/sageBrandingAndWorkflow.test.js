// SIH 26262 - SAGE Product Branding & Frontend Contract Verification
import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const FRONTEND_ROOT = path.resolve(__dirname, '..');

console.log('--- RUNNING SAGE PRODUCT BRANDING & WORKFLOW TESTS ---');

// Test 1: Verify index.html contains exact SAGE product branding
console.log('Test 1: Verifying index.html SAGE title and metadata...');
const indexHtml = fs.readFileSync(path.join(FRONTEND_ROOT, 'index.html'), 'utf8');
assert.ok(
  indexHtml.includes('SAGE — System for Approval Governance and Evaluation | SIH 26262'),
  'index.html must declare the exact SAGE title and SIH problem statement'
);
assert.ok(
  !indexHtml.includes('AICTE Approval &amp; Compliance Orchestration Platform | SIH 26262'),
  'index.html must not contain old product title'
);
console.log('✓ Test 1 passed: index.html branding strictly verified.');

// Test 2: Verify Sidebar branding
console.log('Test 2: Verifying Sidebar branding and visual identifier...');
const sidebarJsx = fs.readFileSync(path.join(FRONTEND_ROOT, 'src/components/layout/Sidebar.jsx'), 'utf8');
assert.ok(sidebarJsx.includes('>S<') || sidebarJsx.includes('aria-label="SAGE Platform"'), 'Sidebar logo must show S / SAGE label');
assert.ok(sidebarJsx.includes('SAGE'), 'Sidebar must prominently display SAGE');
assert.ok(sidebarJsx.includes('Approval Governance &amp; Evaluation'), 'Sidebar must declare expanded governance title');
assert.ok(!sidebarJsx.includes('ORCHESTRATION PLATFORM'), 'Old orchestration platform subtitle must not be present');
console.log('✓ Test 2 passed: Sidebar branding verified.');

// Test 3: Verify Header breadcrumb
console.log('Test 3: Verifying Header breadcrumb branding...');
const headerJsx = fs.readFileSync(path.join(FRONTEND_ROOT, 'src/components/layout/Header.jsx'), 'utf8');
assert.ok(headerJsx.includes('SAGE Platform'), 'Header breadcrumbs must reference SAGE Platform');
console.log('✓ Test 3 passed: Header branding verified.');

// Test 4: Verify LoginPage branding
console.log('Test 4: Verifying LoginPage product branding...');
const loginJsx = fs.readFileSync(path.join(FRONTEND_ROOT, 'src/pages/Login/LoginPage.jsx'), 'utf8');
assert.ok(loginJsx.includes('System for Approval Governance and Evaluation'), 'LoginPage must display exact expanded product name');
assert.ok(loginJsx.includes('SAGE PLATFORM'), 'LoginPage must show SAGE PLATFORM');
assert.ok(loginJsx.includes('SIH PROBLEM STATEMENT 26262'), 'LoginPage must show SIH 26262 reference');
console.log('✓ Test 4 passed: LoginPage branding verified.');

// Test 5: Verify package.json metadata
console.log('Test 5: Verifying frontend package.json metadata...');
const pkgJson = JSON.parse(fs.readFileSync(path.join(FRONTEND_ROOT, 'package.json'), 'utf8'));
assert.strictEqual(pkgJson.name, 'sage-governance-frontend', 'package name must be sage-governance-frontend');
console.log('✓ Test 5 passed: package.json metadata verified.');

// Test 6: Verify Scrutiny checklist verification contract
console.log('Test 6: Verifying Scrutiny checklist verification contract...');
const appDetailJsx = fs.readFileSync(path.join(FRONTEND_ROOT, 'src/pages/ApplicationDetailPage.jsx'), 'utf8');
assert.ok(appDetailJsx.includes('UPDATE_CHECKLIST'), 'Frontend must support individual item checklist verification');
assert.ok(appDetailJsx.includes('COMPLETE_SCRUTINY'), 'Frontend must support explicit complete scrutiny action');
console.log('✓ Test 6 passed: Scrutiny checklist contract verified.');

console.log('\nALL 6 SAGE BRANDING & WORKFLOW CONTRACT TESTS PASSED PERFECTLY!\n');
