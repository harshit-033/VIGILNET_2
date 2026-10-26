// SIH 26262 - Role 4: EVC Coordinator Dashboard
import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useData } from '../../context/DataContext';
import { KPICard } from '../../components/common/KPICard';
import { DataTable } from '../../components/common/DataTable';
import { StatusBadge } from '../../components/common/StatusBadge';
import { DashboardSection } from '../../components/common/DashboardSection';
import { EmptyState } from '../../components/common/EmptyState';
import { Calendar, Clock, AlertTriangle, CheckCircle, FileText, UserPlus, Eye, Upload } from 'lucide-react';
import {
  isCompletedVisit,
  isScheduledVisit,
  isPendingScheduling,
  isReportPendingVisit,
  isOverdueVisit,
} from '../../utils/evcLifecycle';

export const EVCCoordinatorDashboard = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const { evc: evcList } = useData();

  // Categorized EVC lists derived from canonical lifecycle definitions
  const pendingScheduling = evcList.filter(isPendingScheduling);
  const scheduledVisits = evcList.filter(isScheduledVisit);
  const pendingReports = evcList.filter(isReportPendingVisit);
  const completedVisits = evcList.filter(isCompletedVisit);
  const overdueTasks = evcList.filter(isOverdueVisit);

  const evcQueueColumns = [
    {
      header: 'EVC ID',
      key: 'id',
      render: (row) => <strong>{row.id}</strong>,
    },
    {
      header: 'Application',
      key: 'applicationId',
    },
    {
      header: 'Institution',
      key: 'institutionName',
    },
    {
      header: 'Region',
      key: 'region',
    },
    {
      header: 'Inspection Type',
      key: 'inspectionType',
    },
    {
      header: 'State',
      key: 'status',
      render: (row) => <StatusBadge status={row.status} />,
    },
    {
      header: 'Action',
      key: 'action',
      render: (row) => (
        <button
          onClick={() => navigate('/evc')}
          className="btn btn-primary btn-sm"
          title="Schedule Inspection Visit"
        >
          <UserPlus size={12} /> Schedule Visit
        </button>
      ),
    },
  ];

  const reportQueueColumns = [
    {
      header: 'EVC ID',
      key: 'id',
      render: (row) => <strong>{row.id}</strong>,
    },
    {
      header: 'Institution',
      key: 'institutionName',
    },
    {
      header: 'Visit Completed Date',
      key: 'visitDate',
    },
    {
      header: 'Report Status',
      key: 'reportStatus',
      render: (row) => <span style={{ fontSize: '11px', fontWeight: '700' }}>{row.reportStatus}</span>,
    },
    {
      header: 'Action',
      key: 'action',
      render: (row) => (
        <button
          onClick={() => navigate('/evc')}
          className="btn btn-primary btn-sm"
          title="Upload or Review Visit Report"
        >
          <Upload size={12} /> Upload Report
        </button>
      ),
    },
  ];

  return (
    <div>
      {/* Banner */}
      <div className="panel" style={{ background: '#FFFFFF', borderLeft: '4px solid var(--color-warning)', marginBottom: '16px' }}>
        <div className="panel-body" style={{ padding: '16px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
          <div>
            <div style={{ fontSize: '10px', textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--color-warning)', fontWeight: '800' }}>
              EXPERT VISIT COMMITTEE (EVC) DESK
            </div>
            <h1 style={{ fontSize: '20px', color: 'var(--color-navy)', margin: '4px 0 2px', fontWeight: '800' }}>
              Physical &amp; Virtual Inspection Coordination
            </h1>
            <p style={{ color: 'var(--color-text-muted)', fontSize: 'var(--font-size-sm)' }}>
              Coordinator: {user?.name} · Allocation of expert inspection panels and verification reporting
            </p>
          </div>

          <div style={{ display: 'flex', gap: '8px' }}>
            <button onClick={() => navigate('/evc')} className="btn btn-primary btn-sm">
              <Calendar size={13} /> EVC Inspection Hub
            </button>
          </div>
        </div>
      </div>

      {/* 6 KPI Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '14px', marginBottom: '18px' }}>
        <KPICard
          title="Total EVC Cases"
          value={evcList.length}
          subtitle="Registered inspection visits"
          iconName="Calendar"
          tint="blue"
          targetRoute="/evc"
        />
        <KPICard
          title="Pending Scheduling"
          value={pendingScheduling.length}
          subtitle="Awaiting committee allocation"
          iconName="Clock"
          tint="amber"
          targetRoute="/evc"
          filterParams={{ status: 'Awaiting scheduling' }}
        />
        <KPICard
          title="Upcoming Visits"
          value={scheduledVisits.length}
          subtitle="Inspection visits on calendar"
          iconName="CheckCircle"
          tint="teal"
          targetRoute="/evc"
          filterParams={{ status: 'Scheduled' }}
        />
        <KPICard
          title="Completed Visits"
          value={completedVisits.length}
          subtitle="Inspections finished"
          iconName="CheckCircle"
          tint="green"
          targetRoute="/evc"
        />
        <KPICard
          title="Reports Pending"
          value={pendingReports.length}
          subtitle="Expert reports in drafting"
          iconName="FileText"
          tint="amber"
          targetRoute="/evc"
        />
        <KPICard
          title="Overdue Tasks"
          value={overdueTasks.length}
          subtitle="Exceeded turnaround window"
          iconName="AlertTriangle"
          tint="red"
          targetRoute="/evc"
        />
      </div>

      {/* Section 1: EVC Queue */}
      <DashboardSection
        title="EVC Inspections Awaiting Scheduling"
        subtitle="Applications cleared by Scrutiny Desk requiring Expert Visit allocation"
        action={
          <span className="badge badge-amber">
            {pendingScheduling.length} Pending Scheduling
          </span>
        }
        noPadding
      >
        <DataTable
          columns={evcQueueColumns}
          data={pendingScheduling}
          emptyMessage="No applications currently awaiting EVC scheduling."
        />
      </DashboardSection>

      {/* Section 2: Report Drafting Queue */}
      <DashboardSection
        title="Visits Completed — Awaiting Inspection Report"
        subtitle="Panels that have concluded physical/video visits with reports pending submission"
        action={
          <Link to="/evc" className="btn btn-outline btn-sm">
            EVC Workbench ↗
          </Link>
        }
        noPadding
      >
        <DataTable
          columns={reportQueueColumns}
          data={pendingReports}
          emptyMessage="No inspection reports currently pending."
        />
      </DashboardSection>
    </div>
  );
};
