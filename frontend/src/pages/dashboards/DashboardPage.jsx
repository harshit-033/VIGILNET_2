// SIH 26262 - Master Dashboard Page (Role Dispatcher under single /dashboard route)

import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { ROLES } from '../../constants/roles';

// Role-Specific Dashboards
import { InstitutionDashboard } from './InstitutionDashboard';
import { RegionalOfficerDashboard } from './RegionalOfficerDashboard';
import { ScrutinyOfficerDashboard } from './ScrutinyOfficerDashboard';
import { EVCCoordinatorDashboard } from './EVCCoordinatorDashboard';
import { ComplianceReviewerDashboard } from './ComplianceReviewerDashboard';
import { SeniorManagementDashboard } from './SeniorManagementDashboard';
import { AdministratorDashboard } from './AdministratorDashboard';

export const DashboardPage = () => {
  const { role } = useAuth();

  switch (role) {
    case ROLES.INSTITUTION_REP:
      return <InstitutionDashboard />;

    case ROLES.REGIONAL_OFFICER:
      return <RegionalOfficerDashboard />;

    case ROLES.SCRUTINY_OFFICER:
      return <ScrutinyOfficerDashboard />;

    case ROLES.EVC_COORDINATOR:
      return <EVCCoordinatorDashboard />;

    case ROLES.COMPLIANCE_REVIEWER:
      return <ComplianceReviewerDashboard />;

    case ROLES.SENIOR_MANAGEMENT:
      return <SeniorManagementDashboard />;

    case ROLES.ADMINISTRATOR:
      return <AdministratorDashboard />;

    default:
      return <RegionalOfficerDashboard />;
  }
};
