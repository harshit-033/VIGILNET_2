// SIH 26262 - Centralized Data Service & KPI Calculator Engine
import { ROLES } from '../constants/roles';
import {
  isCompletedVisit,
  isScheduledVisit,
  isPendingScheduling,
  isReportPendingVisit,
  isOverdueVisit,
} from '../utils/evcLifecycle';

export const dataService = {
  // Empty baseline accessors for decoupled consumers
  getUsers: () => [],
  getInstitutions: () => [],
  getApplications: () => [],
  getEVC: () => [],
  getCompliance: () => [],
  getDocuments: () => [],
  getAlerts: () => [],
  getActivity: () => [],
  getAdminData: () => ({ workflows: [], deadlinePolicies: [], systemHealth: [] }),

  // Dynamic KPI Calculators derived from actual live datasets
  getRoleKPIs: (user, data = {}) => {
    if (!user) return {};

    const allApps = data.applications || [];
    const allEVC = data.evc || [];
    const allComp = data.compliance || [];
    const allDocs = data.documents || [];
    const allAlerts = data.alerts || [];
    const allActivity = data.activity || [];

    switch (user.role) {
      case ROLES.INSTITUTION_REP: {
        const instId = user.institutionId;
        const instApps = instId ? allApps.filter((a) => a.institutionId === instId) : allApps;
        const instComp = instId ? allComp.filter((c) => c.institutionId === instId) : allComp;
        const instDocs = instId ? allDocs.filter((d) => d.institutionId === instId) : allDocs;

        return {
          totalApplications: instApps.length,
          underReview: instApps.filter((a) => a.status === 'In progress' || a.currentStage === 'Scrutiny').length,
          correctionsRequired: instApps.filter((a) => a.status === 'Returned').length,
          pendingCompliance: instComp.filter((c) => c.status !== 'Verified compliant').length,
          expiringDocuments: instDocs.filter((d) => d.validityStatus === 'Due soon' || d.validityStatus === 'Overdue').length,
        };
      }

      case ROLES.REGIONAL_OFFICER: {
        const userRegion = user.region || 'Northern Region';
        const regApps = allApps.filter((a) => (a.region || 'Northern Region') === userRegion);
        const regEVC = allEVC.filter((e) => (e.region || 'Northern Region') === userRegion);
        const regComp = allComp.filter((c) => c.status !== 'Verified compliant');
        const regDocs = allDocs.filter((d) => d.validityStatus === 'Due soon' || d.validityStatus === 'Overdue');

        return {
          applicationsInRegion: regApps.length,
          pendingApplications: regApps.filter((a) => a.status !== 'Completed').length,
          overdueApplications: regApps.filter((a) => a.status === 'Overdue').length,
          evcVisitsDue: regEVC.filter((e) => e.status !== 'Complete for stage').length,
          pendingCompliance: regComp.length,
          expiringDocuments: regDocs.length,
        };
      }

      case ROLES.SCRUTINY_OFFICER: {
        const assigned = allApps.filter((a) => a.ownerId === user.id || a.currentStage === 'Scrutiny' || a.currentStage === 'Re-scrutiny');

        return {
          assignedApplications: assigned.length,
          pendingScrutiny: assigned.filter((a) => a.currentStage === 'Scrutiny' && (a.status === 'In progress' || a.status === 'Draft' || a.status === 'Due soon')).length,
          returnedApplications: assigned.filter((a) => a.status === 'Returned').length,
          resubmissionsReceived: assigned.filter((a) => a.currentStage === 'Re-scrutiny').length,
          overdueReviews: assigned.filter((a) => a.status === 'Overdue').length,
        };
      }

      case ROLES.EVC_COORDINATOR: {
        return {
          totalCases: allEVC.length,
          pendingScheduling: allEVC.filter(isPendingScheduling).length,
          upcomingVisits: allEVC.filter(isScheduledVisit).length,
          completedVisits: allEVC.filter(isCompletedVisit).length,
          reportsPending: allEVC.filter(isReportPendingVisit).length,
          overdueTasks: allEVC.filter(isOverdueVisit).length,
        };
      }

      case ROLES.COMPLIANCE_REVIEWER: {
        return {
          totalObligations: allComp.length,
          evidenceAwaitingReview: allComp.filter((c) => c.status === 'Evidence submitted' || c.status === 'Verification pending').length,
          dueSoon: allComp.filter((c) => c.urgency === 'Due soon').length,
          overdue: allComp.filter((c) => c.urgency === 'Overdue').length,
          clarificationRequired: allComp.filter((c) => c.status === 'Clarification requested').length,
          verifiedCompliance: allComp.filter((c) => c.status === 'Verified compliant').length,
        };
      }

      case ROLES.SENIOR_MANAGEMENT: {
        const compliantCount = allComp.filter((c) => c.status === 'Verified compliant').length;
        const complianceRate = allComp.length > 0 ? `${Math.round((compliantCount / allComp.length) * 100)}%` : '0%';

        return {
          activeApplications: allApps.filter((a) => a.status !== 'Completed').length,
          pendingApprovals: allApps.filter((a) => a.currentStage === 'Committee review' || a.currentStage === 'Decision recorded').length,
          overdueCases: allApps.filter((a) => a.status === 'Overdue').length,
          evcDelays: allEVC.filter((e) => e.urgency === 'Overdue').length,
          complianceRate,
          escalatedCases: allAlerts.filter((a) => a.urgency === 'Escalated').length,
        };
      }

      case ROLES.ADMINISTRATOR: {
        return {
          totalUsers: 0,
          activeUsers: 0,
          configuredRoles: 7,
          activeWorkflows: 1,
          notificationFailures: allAlerts.filter((a) => a.deliveryStatus === 'Delivery failed').length,
          systemEvents: allActivity.length,
        };
      }

      default:
        return {};
    }
  },
};
