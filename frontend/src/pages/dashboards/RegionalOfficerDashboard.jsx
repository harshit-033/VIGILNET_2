// SIH 26262 - Role 2: Regional Officer Dashboard
import React, { useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useData } from '../../context/DataContext';
import { KPICard } from '../../components/common/KPICard';
import { DataTable } from '../../components/common/DataTable';
import { StatusBadge } from '../../components/common/StatusBadge';
import { DashboardSection } from '../../components/common/DashboardSection';
import { DeadlineList } from '../../components/common/DeadlineList';
import { Users, FileText, AlertTriangle, Calendar, CheckCircle, Clock, Eye } from 'lucide-react';

export const RegionalOfficerDashboard = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const region = user?.region || 'Northern Region';
  const { applications, evc, compliance, documents } = useData();

  // Regional datasets derived from live applications
  const regApps = useMemo(
    () => applications.filter((a) => (a.region || 'Northern Region') === region),
    [applications, region]
  );
  const pendingApps = useMemo(
    () => regApps.filter((a) => a.status !== 'Completed'),
    [regApps]
  );
  const overdueApps = useMemo(
    () => regApps.filter((a) => a.status === 'Overdue'),
    [regApps]
  );
  const attentionApps = useMemo(
    () => regApps.filter((a) => a.status === 'Overdue' || a.status === 'Returned' || a.status === 'Due soon'),
    [regApps]
  );

  const regEVC = useMemo(
    () => evc.filter((e) => (e.region || 'Northern Region') === region),
    [evc, region]
  );
  const evcVisitsDue = useMemo(
    () => regEVC.filter((e) => e.status !== 'Complete for stage').length,
    [regEVC]
  );
  const regComp = useMemo(
    () => compliance.filter((c) => c.status !== 'Verified compliant').length,
    [compliance]
  );
  const regDocs = useMemo(
    () => documents.filter((d) => d.validityStatus === 'Due soon' || d.validityStatus === 'Overdue').length,
    [documents]
  );

  // Scrutiny Officer live workload tracking extracted from active applications
  const officerWorkloads = useMemo(() => {
    const map = new Map();
    regApps.forEach((app) => {
      if (app.owner && app.owner.trim() !== '') {
        const key = app.owner;
        const existing = map.get(key) || {
          id: app.ownerId || `DESK-${map.size + 1}`,
          name: app.owner,
          role: app.currentStage === 'Scrutiny' || app.currentStage === 'Re-scrutiny' ? 'Scrutiny Desk' : 'Regional Workflow Desk',
          activeQueue: 0,
          overdueCount: 0,
        };
        existing.activeQueue++;
        if (app.status === 'Overdue') existing.overdueCount++;
        map.set(key, existing);
      }
    });
    return Array.from(map.values());
  }, [regApps]);

  // Upcoming deadlines in region (only real applications with due dates)
  const regionalDeadlines = useMemo(
    () =>
      regApps
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
    [regApps]
  );

  const attentionColumns = [
    {
      header: 'App ID',
      key: 'id',
      render: (row) => <strong>{row.id}</strong>,
    },
    {
      header: 'Institution',
      key: 'institutionName',
    },
    {
      header: 'Current Stage',
      key: 'currentStage',
    },
    {
      header: 'Turnaround Limit',
      key: 'dueDate',
      render: (row) => row.dueDate || 'Not set',
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
        <Link to={`/applications/${row.id}`} className="btn btn-outline btn-sm">
          <Eye size={12} /> Inspect
        </Link>
      ),
    },
  ];

  const officerColumns = [
    {
      header: 'Desk ID',
      key: 'id',
      render: (row) => <strong>{row.id}</strong>,
    },
    {
      header: 'Official / Desk Owner',
      key: 'name',
    },
    {
      header: 'Operational Role',
      key: 'role',
    },
    {
      header: 'Active Queue',
      key: 'activeQueue',
      render: (row) => <span className="badge badge-blue">{row.activeQueue} Cases</span>,
    },
    {
      header: 'Overdue SLA',
      key: 'overdueCount',
      render: (row) =>
        row.overdueCount > 0 ? (
          <span className="badge badge-red">{row.overdueCount} Overdue</span>
        ) : (
          <span className="badge badge-green">On track</span>
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
              REGIONAL JURISDICTIONAL DESK · {region.toUpperCase()}
            </div>
            <h1 style={{ fontSize: '20px', color: 'var(--color-navy)', margin: '4px 0 2px', fontWeight: '800' }}>
              Regional Operations &amp; Scrutiny Dispatch
            </h1>
            <p style={{ color: 'var(--color-text-muted)', fontSize: 'var(--font-size-sm)' }}>
              Regional Officer: {user?.name} · Jurisdiction: {region} · Connected to Spring Boot Core API
            </p>
          </div>

          <div style={{ display: 'flex', gap: '8px' }}>
            <Link to="/applications" className="btn btn-primary btn-sm">
              <FileText size={13} /> Regional Applications ({regApps.length})
            </Link>
          </div>
        </div>
      </div>

      {/* 6 KPI Cards with Click Navigation */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '14px', marginBottom: '18px' }}>
        <KPICard
          title="Applications in Region"
          value={regApps.length}
          subtitle={`Total filed in ${region}`}
          iconName="FileText"
          tint="blue"
          targetRoute="/applications"
        />
        <KPICard
          title="Pending Applications"
          value={pendingApps.length}
          subtitle="Under active processing"
          iconName="Clock"
          tint="amber"
          targetRoute="/applications"
        />
        <KPICard
          title="Overdue Applications"
          value={overdueApps.length}
          subtitle="SLA turnaround breached"
          iconName="AlertTriangle"
          tint="red"
          targetRoute="/applications"
          filterParams={{ status: 'Overdue' }}
        />
        <KPICard
          title="EVC Visits Due"
          value={evcVisitsDue}
          subtitle="Pending inspection clearance"
          iconName="Calendar"
          tint="teal"
          targetRoute="/evc"
        />
        <KPICard
          title="Pending Compliance"
          value={regComp}
          subtitle="Post-approval statutory reviews"
          iconName="CheckCircle"
          tint="green"
          targetRoute="/compliance"
        />
        <KPICard
          title="Expiring Documents"
          value={regDocs}
          subtitle="Statutory NOC renewals due"
          iconName="Clock"
          tint="red"
          targetRoute="/documents"
        />
      </div>

      {/* Section 1: Applications Requiring Regional Attention */}
      <DashboardSection
        title="Applications Requiring Regional Attention (Live Data)"
        subtitle="Cases with overdue status, deficiency notices, or imminent turnaround milestones"
        action={
          <span className="badge badge-amber">
            {attentionApps.length} Cases
          </span>
        }
        noPadding
      >
        <DataTable
          columns={attentionColumns}
          data={attentionApps}
          emptyMessage="No applications currently require urgent attention in this region."
        />
      </DashboardSection>

      {/* Grid: Scrutiny Officers & Regional Deadlines */}
      <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '16px', marginBottom: '16px' }}>
        {/* Section 2: Officer Workload */}
        <DashboardSection
          title="Desk Queue &amp; Officer Workload"
          subtitle="Active application queue assignments across scrutiny desks"
          noPadding
        >
          <DataTable
            columns={officerColumns}
            data={officerWorkloads}
            emptyMessage="No desk officers currently assigned to live applications in this region."
          />
        </DashboardSection>

        {/* Section 3: Upcoming Regional Deadlines */}
        <DashboardSection
          title="Upcoming Regional Deadlines"
          subtitle="Strict turnaround targets for active regional cases"
          noPadding
        >
          {regionalDeadlines.length === 0 ? (
            <div style={{ padding: '24px', textAlign: 'center', color: 'var(--color-text-muted)', fontSize: '12px' }}>
              No upcoming turnaround deadlines scheduled in this region.
            </div>
          ) : (
            <DeadlineList deadlines={regionalDeadlines} />
          )}
        </DashboardSection>
      </div>
    </div>
  );
};
