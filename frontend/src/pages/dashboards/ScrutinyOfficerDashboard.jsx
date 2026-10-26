// SIH 26262 - Role 3: Scrutiny Officer Dashboard

import React, { useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useData } from '../../context/DataContext';
import { KPICard } from '../../components/common/KPICard';
import { DataTable } from '../../components/common/DataTable';
import { StatusBadge } from '../../components/common/StatusBadge';
import { DashboardSection } from '../../components/common/DashboardSection';
import { DeadlineList } from '../../components/common/DeadlineList';
import { FileText, Clock, AlertTriangle, CheckCircle, ArrowRight, Eye, Edit3, CornerUpLeft } from 'lucide-react';

export const ScrutinyOfficerDashboard = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const { applications } = useData();

  // Scrutiny queues derived dynamically from live applications
  const myQueue = useMemo(
    () => applications.filter((a) => a.currentStage === 'Scrutiny' || a.currentStage === 'Re-scrutiny'),
    [applications]
  );
  const awaitingReview = useMemo(
    () => myQueue.filter((a) => (a.status === 'In progress' || a.status === 'Draft' || a.status === 'Due soon') && a.currentStage === 'Scrutiny'),
    [myQueue]
  );
  const resubmissions = useMemo(
    () => myQueue.filter((a) => a.currentStage === 'Re-scrutiny'),
    [myQueue]
  );
  const returnedApps = useMemo(
    () => applications.filter((a) => a.status === 'Returned'),
    [applications]
  );
  const observationsRaised = useMemo(
    () => applications.filter((a) => a.deficiencies && a.deficiencies.length > 0),
    [applications]
  );
  const overdueReviews = useMemo(
    () => myQueue.filter((a) => a.status === 'Overdue'),
    [myQueue]
  );

  // Scrutiny upcoming deadlines derived from live queue
  const scrutinyDeadlines = useMemo(
    () =>
      myQueue
        .filter((a) => a.dueDate)
        .map((a) => ({
          id: a.id,
          title: `${a.institutionName} - ${a.currentStage}`,
          dueDate: a.dueDate,
          status: a.status,
          referenceId: a.id,
          linkTo: `/applications/${a.id}`,
        }))
        .sort((a, b) => new Date(a.dueDate) - new Date(b.dueDate)),
    [myQueue]
  );

  const queueColumns = [
    {
      header: 'Application ID',
      key: 'id',
      render: (row) => <strong>{row.id}</strong>,
    },
    {
      header: 'Institution',
      key: 'institutionName',
    },
    {
      header: 'Type',
      key: 'applicationType',
    },
    {
      header: 'Checklist Progress',
      key: 'checklistProgress',
      render: (row) => (
        <span style={{ fontSize: '11px', fontWeight: '700' }}>
          {row.checklistProgress ? `${row.checklistProgress.completed}/${row.checklistProgress.total} Items` : '0/4'}
        </span>
      ),
    },
    {
      header: 'Due Date',
      key: 'dueDate',
    },
    {
      header: 'Stage Status',
      key: 'status',
      render: (row) => <StatusBadge status={row.status} />,
    },
    {
      header: 'Workflow Actions',
      key: 'actions',
      render: (row) => (
        <div style={{ display: 'flex', gap: '6px' }}>
          <Link to={`/applications/${row.id}`} className="btn btn-primary btn-sm" title="Review Dossier">
            <Eye size={12} /> Review
          </Link>
          <Link to={`/applications/${row.id}`} className="btn btn-outline btn-sm" title="Add Observation Note">
            <Edit3 size={12} />
          </Link>
          <Link to={`/applications/${row.id}`} className="btn btn-danger btn-sm" title="Return with Deficiencies">
            <CornerUpLeft size={12} />
          </Link>
        </div>
      ),
    },
  ];

  return (
    <div>
      {/* Banner */}
      <div className="panel" style={{ background: '#FFFFFF', borderLeft: '4px solid var(--color-blue)', marginBottom: '16px' }}>
        <div className="panel-body" style={{ padding: '16px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
          <div>
            <div style={{ fontSize: '10px', textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--color-blue)', fontWeight: '800' }}>
              TECHNICAL SCRUTINY &amp; DEFICIENCY DESK
            </div>
            <h1 style={{ fontSize: '20px', color: 'var(--color-navy)', margin: '4px 0 2px', fontWeight: '800' }}>
              Official Scrutiny Operations Workspace
            </h1>
            <p style={{ color: 'var(--color-text-muted)', fontSize: 'var(--font-size-sm)' }}>
              Assigned Officer: {user?.name} · Active technical scrutiny against live Spring Boot application repository
            </p>
          </div>

          <div style={{ display: 'flex', gap: '8px' }}>
            <Link to="/applications" className="btn btn-primary btn-sm">
              <FileText size={13} /> All Applications Explorer
            </Link>
          </div>
        </div>
      </div>

      {/* 5 KPI Cards with Click Navigation */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))', gap: '14px', marginBottom: '18px' }}>
        <KPICard
          title="Assigned Applications"
          value={myQueue.length}
          subtitle="Live dossiers in scrutiny queue"
          iconName="FileText"
          tint="blue"
          targetRoute="/applications"
        />
        <KPICard
          title="Pending Scrutiny"
          value={awaitingReview.length}
          subtitle="Live dossiers awaiting initial review"
          iconName="Clock"
          tint="amber"
          targetRoute="/applications"
          filterParams={{ stage: 'Scrutiny' }}
        />
        <KPICard
          title="Returned Applications"
          value={returnedApps.length}
          subtitle="Live deficiency notices sent"
          iconName="AlertTriangle"
          tint="red"
          targetRoute="/applications"
          filterParams={{ status: 'Returned' }}
        />
        <KPICard
          title="Resubmissions Received"
          value={resubmissions.length}
          subtitle="Live institution responses"
          iconName="CheckCircle"
          tint="teal"
          targetRoute="/applications"
          filterParams={{ stage: 'Re-scrutiny' }}
        />
        <KPICard
          title="Overdue Reviews"
          value={overdueReviews.length}
          subtitle="Live handbook SLA breaches"
          iconName="Clock"
          tint="red"
          targetRoute="/applications"
          filterParams={{ status: 'Overdue' }}
        />
      </div>

      {/* Section 1: My Scrutiny Queue */}
      <DashboardSection
        title="My Scrutiny Queue (Live Data)"
        subtitle="Active applications requiring technical checklist examination or deficiency clearance"
        action={
          <span className="badge badge-blue">
            {myQueue.length} Active Dossiers
          </span>
        }
        noPadding
      >
        <DataTable columns={queueColumns} data={myQueue} emptyMessage="No dossiers currently in your scrutiny queue." />
      </DashboardSection>

      {/* Grid: Resubmitted Applications & Observations Raised */}
      <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '16px', marginBottom: '16px' }}>
        {/* Section 2: Resubmitted Applications */}
        <DashboardSection
          title="Resubmitted Applications (Live Re-Scrutiny)"
          subtitle="Institutions that have provided rectifications to previously raised deficiencies"
          noPadding
        >
          <DataTable
            columns={[
              { header: 'App ID', key: 'id', render: (r) => <strong>{r.id}</strong> },
              { header: 'Institution', key: 'institutionName' },
              { header: 'Turnaround Age', key: 'ageDays', render: (r) => `${r.ageDays} days` },
              { header: 'Status', key: 'status', render: (r) => <StatusBadge status={r.status} /> },
              {
                header: 'Action',
                key: 'action',
                render: (r) => (
                  <Link to={`/applications/${r.id}`} className="btn btn-primary btn-sm">
                    Re-examine <ArrowRight size={11} />
                  </Link>
                ),
              },
            ]}
            data={resubmissions}
            emptyMessage="No resubmissions currently pending."
          />
        </DashboardSection>

        {/* Section 3: Upcoming Deadlines */}
        <DashboardSection
          title="Upcoming Scrutiny Deadlines (Live Queue)"
          subtitle="Strict turnaround targets for active scrutiny cases"
          noPadding
        >
          <DeadlineList deadlines={scrutinyDeadlines} />
        </DashboardSection>
      </div>

      {/* Section 4: Observations Raised */}
      <DashboardSection
        title="Observations &amp; Deficiencies Raised Across Applications (Live Data)"
        subtitle="Summary of deficiencies currently pending correction by applicant institutions"
      >
        {observationsRaised.length === 0 ? (
          <div style={{ padding: '20px', textAlign: 'center', color: 'var(--color-text-muted)', fontSize: 'var(--font-size-sm)' }}>
            No active deficiency notices recorded across current live applications.
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {observationsRaised.map((app) => (
              <div
                key={app.id}
                style={{
                  padding: '12px 14px',
                  background: '#FDFEFE',
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-sm)',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                  <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: '700', color: 'var(--color-navy)' }}>
                    {app.id} · {app.institutionName}
                  </span>
                  <StatusBadge status={app.status} />
                </div>
                <ul style={{ margin: 0, paddingLeft: '18px', fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
                  {app.deficiencies?.map((def, idx) => (
                    <li key={idx} style={{ marginBottom: '2px' }}>
                      {def}
                    </li>
                  ))}
                </ul>
                <div style={{ marginTop: '8px', textAlign: 'right' }}>
                  <Link to={`/applications/${app.id}`} className="btn btn-outline btn-sm">
                    View Observations Dossier ↗
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </DashboardSection>
    </div>
  );
};
