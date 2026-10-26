import assert from 'node:assert/strict';
import {
  EVC_STATUS,
  isCompletedVisit,
  isScheduledVisit,
  isPendingScheduling,
  isUpcomingVisit,
  isReportPendingVisit,
  isOverdueVisit,
} from '../src/utils/evcLifecycle.js';

const TEST_EVC_FIXTURES = [
  {
    id: 'EVC-204',
    applicationId: 'APP-2026-0988',
    institutionId: 'INST-1044',
    institutionName: 'North Valley College of Engineering',
    region: 'Northern Region',
    status: 'Awaiting scheduling',
    coordinator: 'C. Verma (EVC Operations)',
    coordinatorId: 'USR-EVC-01',
    visitDate: '2026-10-23',
    deadline: '2026-10-23',
    urgency: 'Overdue',
    experts: [],
    inspectionType: 'Physical On-Site Verification',
    reportStatus: 'Not Applicable',
  },
  {
    id: 'EVC-201',
    applicationId: 'APP-2026-1042',
    institutionId: 'INST-1025',
    institutionName: 'ABC Institute of Technology',
    region: 'Northern Region',
    status: 'Scheduled',
    coordinator: 'C. Verma (EVC Operations)',
    coordinatorId: 'USR-EVC-01',
    visitDate: '2026-10-27',
    deadline: '2026-10-29',
    urgency: 'Due soon',
    experts: ['Prof. K. Raman (IIT Roorkee - Chair)', 'Dr. Sunita Rao (NIT Kurukshetra)'],
    inspectionType: 'Physical On-Site Verification',
    reportStatus: 'Awaiting Inspection Visit',
  },
  {
    id: 'EVC-195',
    applicationId: 'APP-2026-1033',
    institutionId: 'INST-1030',
    institutionName: 'Global Institute of Management',
    region: 'Western Region',
    status: 'Report pending',
    coordinator: 'C. Verma (EVC Operations)',
    coordinatorId: 'USR-EVC-01',
    visitDate: '2026-10-21',
    deadline: '2026-10-23',
    urgency: 'Overdue',
    experts: ['Dr. V. K. Mehta (COEP Pune - Chair)', 'Prof. Sneha Deshmukh (VJTI)'],
    inspectionType: 'Physical On-Site Verification',
    reportStatus: 'Visit Concluded · Report Draft Awaited from Committee Chair',
  },
  {
    id: 'EVC-188',
    applicationId: 'APP-2026-1011',
    institutionId: 'INST-1019',
    institutionName: 'Apex College of Pharmacy',
    region: 'Northern Region',
    status: 'Visit completed',
    coordinator: 'C. Verma (EVC Operations)',
    coordinatorId: 'USR-EVC-01',
    visitDate: '2026-10-20',
    deadline: '2026-10-22',
    urgency: 'Overdue',
    experts: ['Prof. P. C. Joshi (BITS Pilani - Chair)', 'Dr. Ritu Agarwal (DPSRU)'],
    inspectionType: 'Physical On-Site Verification',
    reportStatus: 'Visit Concluded · Report Draft Awaited from Committee Chair',
  },
  {
    id: 'EVC-177',
    applicationId: 'APP-2026-1004',
    institutionId: 'INST-1012',
    institutionName: 'Zenith Polytechnic Institute',
    region: 'Southern Region',
    status: 'Complete for stage',
    coordinator: 'C. Verma (EVC Operations)',
    coordinatorId: 'USR-EVC-01',
    visitDate: '2026-10-18',
    deadline: '2026-10-20',
    urgency: 'Completed',
    experts: ['Dr. S. Nair (NIT Trichy - Chair)', 'Prof. Anand Mohan (PSG Tech)'],
    inspectionType: 'Physical On-Site Verification',
    reportStatus: 'Official Report Accepted & Stored',
  },
];

console.log('--- RUNNING EVC LIFECYCLE TESTS ---');

// Test 1: Test Dataset Invariants
console.log('Test 1: Verifying baseline test fixtures...');
assert.equal(TEST_EVC_FIXTURES.length, 5, 'TEST_EVC_FIXTURES must retain all 5 test records');

const evc204 = TEST_EVC_FIXTURES.find((e) => e.id === 'EVC-204');
const evc201 = TEST_EVC_FIXTURES.find((e) => e.id === 'EVC-201');
const evc195 = TEST_EVC_FIXTURES.find((e) => e.id === 'EVC-195');
const evc188 = TEST_EVC_FIXTURES.find((e) => e.id === 'EVC-188');
const evc177 = TEST_EVC_FIXTURES.find((e) => e.id === 'EVC-177');

assert.equal(evc204.status, 'Awaiting scheduling', 'EVC-204 must initially be awaiting scheduling');
assert.equal(evc201.status, 'Scheduled', 'EVC-201 must initially be Scheduled');
assert.equal(evc195.status, 'Report pending', 'EVC-195 must be Report pending');
assert.equal(evc188.status, 'Visit completed', 'EVC-188 must be Visit completed');
assert.equal(evc177.status, 'Complete for stage', 'EVC-177 must be Complete for stage');
console.log('✓ Test 1 passed: Baseline test records intact.');

// Test 2: Scheduling Lifecycle
console.log('\nTest 2: Verifying Scheduling behavior...');
function simulateScheduleEVC(record, payload) {
  assert.equal(isCompletedVisit(record), false, 'Cannot schedule an already completed visit');
  return {
    ...record,
    visitDate: payload.visitDate,
    coordinator: payload.coordinator || record.coordinator,
    experts: payload.experts || record.experts,
    inspectionType: payload.inspectionType || record.inspectionType,
    status: 'Scheduled',
    urgency: 'Due soon',
    reportStatus: 'Awaiting Inspection Visit',
  };
}

const scheduled204 = simulateScheduleEVC(evc204, {
  visitDate: '2026-10-31',
  coordinator: 'C. Verma (EVC Operations)',
  experts: ['Prof. K. Raman (IIT Roorkee - Chair)', 'Dr. Sunita Rao (NIT Kurukshetra)'],
  inspectionType: 'Physical On-Site Verification',
});

assert.equal(scheduled204.status, 'Scheduled', 'Scheduling MUST set status to Scheduled, never Completed');
assert.equal(isScheduledVisit(scheduled204), true, 'scheduled record must satisfy isScheduledVisit');
assert.equal(isCompletedVisit(scheduled204), false, 'scheduled record must NOT be completed');
assert.equal(isUpcomingVisit(scheduled204), true, 'scheduled record must be in Upcoming Visits');
assert.equal(isReportPendingVisit(scheduled204), false, 'scheduled record must not be in Reports Pending');
console.log('✓ Test 2 passed: Scheduling correctly sets Scheduled and excludes Completed.');

// Test 3: Completion Lifecycle
console.log('\nTest 3: Verifying Completion behavior...');
function simulateMarkCompleted(record, payload) {
  if (record.status !== 'Scheduled') {
    throw new Error(`Only scheduled visits can be marked as completed. Current status: ${record.status}`);
  }
  return {
    ...record,
    status: 'Completed',
    urgency: 'In progress',
    reportStatus: 'Visit Concluded · Report Draft Awaited from Committee Chair',
    completionNotes: payload.completionNotes || '',
  };
}

assert.throws(
  () => simulateMarkCompleted(evc204, { completionNotes: 'premature complete' }),
  /Only scheduled visits can be marked as completed/,
  'Attempting to complete an un-scheduled visit must throw an error'
);

const completed204 = simulateMarkCompleted(scheduled204, {
  completionNotes: 'Inspection successfully concluded on-site.',
});

assert.equal(completed204.status, 'Completed', 'Must have status Completed');
assert.equal(isCompletedVisit(completed204), true, 'Must satisfy isCompletedVisit');
assert.equal(isScheduledVisit(completed204), false, 'Completed record must NOT be in isScheduledVisit');
assert.equal(isUpcomingVisit(completed204), false, 'Completed record must NOT be in isUpcomingVisit');
assert.equal(isReportPendingVisit(completed204), true, 'Completed record without final report must be in Reports Pending');
console.log('✓ Test 3 passed: Completion lifecycle and guards work as expected.');

// Test 4: Tab Filter Categorization
console.log('\nTest 4: Verifying Tab Filter Rules...');
const allTestRecords = [
  evc204,
  scheduled204,
  completed204,
  evc195,
  evc188,
  evc177,
];

const scheduledTab = allTestRecords.filter(isScheduledVisit);
const upcomingTab = allTestRecords.filter(isUpcomingVisit);
const completedTab = allTestRecords.filter(isCompletedVisit);
const reportsPendingTab = allTestRecords.filter(isReportPendingVisit);

assert.equal(scheduledTab.length, 1, 'Scheduled tab should contain 1 scheduled visit');
assert.equal(scheduledTab[0].status, 'Scheduled');
assert.ok(!scheduledTab.some(isCompletedVisit), 'Scheduled tab must never include completed visits');

assert.equal(upcomingTab.length, 2, 'Upcoming tab should contain 2 upcoming visits (Awaiting scheduling + Scheduled)');
assert.ok(!upcomingTab.some(isCompletedVisit), 'Upcoming tab must never include completed visits');

assert.equal(completedTab.length, 3, 'Completed tab should contain 3 completed visits');
assert.ok(completedTab.includes(completed204), 'Completed tab must include newly Completed record');
assert.ok(completedTab.includes(evc188), 'Completed tab must include Visit completed record');
assert.ok(completedTab.includes(evc177), 'Completed tab must include Complete for stage record');

assert.equal(reportsPendingTab.length, 3, 'Reports pending should have 3 records (EVC-195, completed204, EVC-188)');
assert.ok(reportsPendingTab.includes(evc195), 'Must include EVC-195');
assert.ok(reportsPendingTab.includes(completed204), 'Must include completed204 before report submission');
assert.ok(reportsPendingTab.includes(evc188), 'Must include EVC-188 drafting report');
assert.ok(!reportsPendingTab.includes(evc177), 'Must NOT include EVC-177 because report was filed and stage complete');
console.log('✓ Test 4 passed: Tab filters accurately partition records according to lifecycle rules.');

// Test 5: Report Submission & Final Stage Closure
console.log('\nTest 5: Verifying Report Submission and Closure...');
function simulateSubmitEVCReport(record, payload) {
  return {
    ...record,
    status: 'Complete for stage',
    urgency: 'Completed',
    reportStatus: 'Official Report Accepted & Stored',
    reportNotes: payload.reportNotes,
    reportOutcome: payload.reportOutcome || 'Recommended',
  };
}

const closed204 = simulateSubmitEVCReport(completed204, {
  reportNotes: 'Detailed assessment uploaded. All laboratory requirements met.',
  reportOutcome: 'Recommended Approval',
});

assert.equal(closed204.status, 'Complete for stage');
assert.equal(isCompletedVisit(closed204), true, 'Still completed');
assert.equal(isReportPendingVisit(closed204), false, 'Report is now filed, must NOT be pending');
console.log('✓ Test 5 passed: Report submission transitions visit out of Reports Pending.');

// Test 6: Safe Manual Correction / Reversal
console.log('\nTest 6: Verifying Safe Manual Correction Path...');
function simulateRescheduleEVC(record, payload) {
  return {
    ...record,
    status: 'Scheduled',
    urgency: 'Due soon',
    visitDate: payload.visitDate || record.visitDate || '2026-10-30',
    reportStatus: 'Awaiting Inspection Visit',
    reportNotes: '',
  };
}

const reverted204 = simulateRescheduleEVC(completed204, { visitDate: '2026-11-05' });
assert.equal(reverted204.status, 'Scheduled');
assert.equal(isCompletedVisit(reverted204), false, 'Reverted record is no longer completed');
assert.equal(isScheduledVisit(reverted204), true, 'Reverted record is scheduled');
assert.equal(isUpcomingVisit(reverted204), true, 'Reverted record is in upcoming');
assert.equal(isReportPendingVisit(reverted204), false, 'Reverted record is no longer pending report');
console.log('✓ Test 6 passed: Safe manual correction path successfully reverts accidental completion.');

console.log('\nALL 6 EVC LIFECYCLE TESTS PASSED PERFECTLY!\n');
