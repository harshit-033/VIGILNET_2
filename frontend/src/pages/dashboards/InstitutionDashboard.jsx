// SIH 26262 - Role 1: Institution Representative Dashboard
import React, { useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useData } from '../../context/DataContext';
import { KPICard } from '../../components/common/KPICard';
import { DataTable } from '../../components/common/DataTable';
import { StatusBadge } from '../../components/common/StatusBadge';
import { DashboardSection } from '../../components/common/DashboardSection';
import { DeadlineList } from '../../components/common/DeadlineList';
import { EmptyState } from '../../components/common/EmptyState';
import { FileText, AlertTriangle, CheckCircle, Clock, Upload, ArrowRight, Eye, Plus } from 'lucide-react';

export const InstitutionDashboard = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const institutionId = user?.institutionId;
  const { applications, compliance, documents, activity } = useData();

  // Filtered institution records from live applications
  const myApps = useMemo(() => {
    if (institutionId) {
      return applications.filter(
        (a) => a.institutionId === institutionId || a.institutionName === user?.institutionName
      );
    }
    if (user?.institutionName) {
      return applications.filter((a) => a.institutionName === user.institutionName);
    }
    return applications;
  }, [applications, institutionId, user]);

  const myUnderReview = useMemo(
    () =>
      myApps.filter(
        (a) =>
          a.status === 'In progress' ||
          a.status === 'Draft' ||
          a.status === 'Due soon' ||
          a.currentStage === 'Scrutiny' ||
          a.currentStage === 'EVC scheduling'
      ).length,
    [myApps]
  );

  const returnedApps = useMemo(
    () => myApps.filter((a) => a.status === 'Returned' || (a.deficiencies && a.deficiencies.length > 0)),
    [myApps]
  );

  const myCompliance = useMemo(
    () => (institutionId ? compliance.filter((c) => c.institutionId === institutionId) : compliance),
    [compliance, institutionId]
  );
  const myDocs = useMemo(
    () => (institutionId ? documents.filter((d) => d.institutionId === institutionId) : documents),
    [documents, institutionId]
  );

  const pendingComplianceCount = useMemo(
    () => myCompliance.filter((c) => c.status !== 'Verified compliant').length,
    [myCompliance]
  );
  const expiringDocsCount = useMemo(
    () => myDocs.filter((d) => d.validityStatus === 'Due soon' || d.validityStatus === 'Overdue').length,
    [myDocs]
  );

  // Deadlines derived strictly from real applications
  const upcomingDeadlines = useMemo(
    () =>
      myApps
        .filter((a) => a.dueDate)
        .map((a) => ({
          id: a.id,
          title: `Application ${a.id} - ${a.currentStage}`,
          dueDate: a.dueDate,
          status: a.status,
          referenceId: a.id,
          linkTo: `/applications/${a.id}`,
        }))
        .sort((a, b) => new Date(a.dueDate) - new Date(b.dueDate)),
    [myApps]
  );

  const appColumns = [
    {
      header: 'Application Ref',
      key: 'id',
      render: (row) => <strong>{row.id}</strong>,
    },
    {
      header: 'Application Type',
      key: 'applicationType',
    },
    {
      header: 'Current Stage',
      key: 'currentStage',
    },
    {
      header: 'Turnaround Target',
      key: 'dueDate',
      render: (row) => row.dueDate || 'Standard SLA',
    },
    {
      header: 'Status',
      key: 'status',
      render: (row) => <StatusBadge status={row.status} />,
    },
    {
      header: 'Action',
      key: 'action',
      render: (row) => (
        <Link to={`/applications/${row.id}`} className="btn btn-outline btn-sm" title="View Application Dossier">
          <Eye size={12} /> View
        </Link>
      ),
    },
  ];

  return (
    <div>
      {/* Banner */}
      <div className="panel" style={{ background: '#FFFFFF', borderLeft: '4px solid var(--color-teal)', marginBottom: '16px' }}>
        <div className="panel-body" style={{ padding: '16px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
          <div>
            <div style={{ fontSize: '10px', textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--color-teal)', fontWeight: '800' }}>
              AUTHORIZED INSTITUTION DESK · {user?.institutionName || 'TECHNICAL INSTITUTION'}
            </div>
            <h1 style={{ fontSize: '20px', color: 'var(--color-navy)', margin: '4px 0 2px', fontWeight: '800' }}>
              Approval &amp; Compliance Portal
            </h1>
            <p style={{ color: 'var(--color-text-muted)', fontSize: 'var(--font-size-sm)' }}>
              Representative: {user?.name} {institutionId ? `· Institution Code: ${institutionId}` : ''}
            </p>
          </div>

          <div style={{ display: 'flex', gap: '8px' }}>
            <Link to="/applications" className="btn btn-primary btn-sm">
              <Plus size={13} /> View Applications ({myApps.length})
            </Link>
          </div>
        </div>
      </div>

      {/* 5 KPI Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '14px', marginBottom: '18px' }}>
        <KPICard
          title="Total Applications"
          value={myApps.length}
          subtitle="Dossiers submitted to AICTE"
          iconName="FileText"
          tint="blue"
          targetRoute="/applications"
        />
        <KPICard
          title="Under Review"
          value={myUnderReview}
          subtitle="Dossiers with regional desk"
          iconName="Clock"
          tint="amber"
          targetRoute="/applications"
        />
        <KPICard
          title="Corrections Required"
          value={returnedApps.length}
          subtitle="Deficiency notices requiring action"
          iconName="AlertTriangle"
          tint="red"
          targetRoute="/applications"
          filterParams={{ status: 'Returned' }}
        />
        <KPICard
          title="Pending Compliance"
          value={pendingComplianceCount}
          subtitle="Statutory conditions registered"
          iconName="CheckCircle"
          tint="teal"
          targetRoute="/compliance"
        />
        <KPICard
          title="Expiring Documents"
          value={expiringDocsCount}
          subtitle="Renewals due within 30 days"
          iconName="Clock"
          tint="red"
          targetRoute="/documents"
        />
      </div>

      {/* Returned / Deficiency Warning Banner */}
      {returnedApps.length > 0 && (
        <div
          style={{
            background: 'var(--color-critical-bg)',
            border: '1px solid var(--color-critical-border)',
            borderRadius: 'var(--radius-sm)',
            padding: '14px 18px',
            marginBottom: '16px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '12px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <AlertTriangle size={20} color="var(--color-critical)" />
            <div>
              <strong style={{ color: 'var(--color-critical)', fontSize: '13px', display: 'block' }}>
                Action Required: {returnedApps.length} Application(s) Returned with Deficiency Notice
              </strong>
              <span style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
                Official observations have been recorded. Please rectify requested deficiencies and submit corrected documents.
              </span>
            </div>
          </div>
          <Link to={`/applications/${returnedApps[0].id}`} className="btn btn-danger btn-sm">
            Review Observations &amp; Rectify <ArrowRight size={13} />
          </Link>
        </div>
      )}

      {/* Section 1: My Applications */}
      <DashboardSection
        title="Institutional Approval Applications (Live Data)"
        subtitle="Current approval cycle dossiers tracked in the AICTE platform"
        action={
          <span className="badge badge-blue">
            {myApps.length} Active Filings
          </span>
        }
        noPadding
      >
        <DataTable
          columns={appColumns}
          data={myApps}
          emptyMessage="No applications currently filed. Use the Application Explorer to register a new submission."
        />
      </DashboardSection>

      {/* Grid: Upcoming Deadlines & Post-Approval Compliance */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px' }}>
        {/* Section 2: Upcoming Deadlines */}
        <DashboardSection
          title="Upcoming Turnaround Deadlines"
          subtitle="Pending milestones requiring institutional response"
          noPadding
        >
          {upcomingDeadlines.length === 0 ? (
            <div style={{ padding: '24px', textAlign: 'center', color: 'var(--color-text-muted)', fontSize: '12px' }}>
              No upcoming deadlines pending for this institution.
            </div>
          ) : (
            <DeadlineList deadlines={upcomingDeadlines} />
          )}
        </DashboardSection>

        {/* Section 3: Statutory Compliance Obligations */}
        <DashboardSection
          title="Post-Approval Compliance Obligations"
          subtitle="Statutory conditions and undertakings"
          noPadding
        >
          <div style={{ padding: '24px', textAlign: 'center', color: 'var(--color-text-muted)', fontSize: '12px' }}>
            No post-approval conditions registered. Continuous compliance tracking will be configured upon approval issuance.
          </div>
        </DashboardSection>
      </div>
    </div>
  );
};
