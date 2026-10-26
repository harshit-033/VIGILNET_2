// SIH 26262 - Canonical EVC Lifecycle Definitions and Filters

/**
 * Visit Status Canonical Constants
 */
export const EVC_STATUS = {
  AWAITING_SCHEDULING: 'Awaiting scheduling',
  SCHEDULED: 'Scheduled',
  COMPLETED: 'Completed',
  VISIT_COMPLETED: 'Visit completed', // Historical / legacy status in demo dataset
  REPORT_PENDING: 'Report pending',
  COMPLETE_FOR_STAGE: 'Complete for stage',
};

/**
 * Checks if an EVC visit has concluded its physical or virtual inspection.
 * Includes 'Completed', legacy 'Visit completed', and final 'Complete for stage'.
 */
export const isCompletedVisit = (record) => {
  if (!record || !record.status) return false;
  const s = String(record.status).trim().toLowerCase();
  return s === 'completed' || s === 'visit completed' || s === 'complete for stage';
};

/**
 * Checks if an EVC visit is scheduled and awaiting inspection.
 * Must NOT be already completed.
 */
export const isScheduledVisit = (record) => {
  if (!record || !record.status) return false;
  return record.status === 'Scheduled' && !isCompletedVisit(record);
};

/**
 * Checks if an EVC visit is awaiting scheduling.
 */
export const isPendingScheduling = (record) => {
  if (!record || !record.status) return false;
  return record.status === 'Awaiting scheduling' && !isCompletedVisit(record);
};

/**
 * Checks if an EVC visit is upcoming (scheduled or pending scheduling, NOT completed).
 */
export const isUpcomingVisit = (record) => {
  if (!record || !record.status) return false;
  if (isCompletedVisit(record)) return false;
  return record.status === 'Scheduled' || record.status === 'Awaiting scheduling';
};

/**
 * Checks if an EVC visit is awaiting report submission.
 * This includes:
 * 1) Explicit 'Report pending' status.
 * 2) Visits that have completed inspection ('Completed' or 'Visit completed') whose official
 *    report dossier has not yet been accepted/filed (i.e. status is not 'Complete for stage'
 *    and reportNotes is empty or reportStatus indicates drafting/awaited).
 */
export const isReportPendingVisit = (record) => {
  if (!record || !record.status) return false;
  const s = String(record.status).trim().toLowerCase();
  if (s === 'report pending') return true;
  if (isCompletedVisit(record) && s !== 'complete for stage') {
    const hasNotes = Boolean(record.reportNotes && record.reportNotes.trim().length > 0);
    const reportStatus = String(record.reportStatus || '').toLowerCase();
    const isAwaiting = reportStatus.includes('draft') || reportStatus.includes('awaited') || reportStatus.includes('pending') || !hasNotes;
    return isAwaiting;
  }
  return false;
};

/**
 * Checks if an EVC visit has overdue urgency.
 */
export const isOverdueVisit = (record) => {
  if (!record) return false;
  return record.urgency === 'Overdue';
};
