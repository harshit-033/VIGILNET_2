// SIH 26262 - Role 5: Compliance Reviewer Dashboard
import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useData } from '../../context/DataContext';
import { KPICard } from '../../components/common/KPICard';
import { DataTable } from '../../components/common/DataTable';
import { StatusBadge } from '../../components/common/StatusBadge';
import { DashboardSection } from '../../components/common/DashboardSection';
import { CheckSquare, Clock, AlertTriangle, CheckCircle, FileText, Upload, Eye } from 'lucide-react';

export const ComplianceReviewerDashboard = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const { compliance: allObligations } = useData();

  // Categorized compliance queues
  const reviewQueue = allObligations.filter((c) => c.status === 'Evidence submitted' || c.status === 'Verification pending');
  const overdueObligations = allObligations.filter((c) => c.urgency === 'Overdue');
  const dueSoon = allObligations.filter((c) => c.urgency === 'Due soon');
  const clarificationQueue = allObligations.filter((c) => c.status === 'Clarification requested');
  const verifiedCompliance = allObligations.filter((c) => c.status === 'Verified compliant');

  const reviewColumns = [
    {
      header: 'Obligation ID',
      key: 'id',
      render: (row) => <strong>{row.id}</strong>,
    },
    {
      header: 'Institution',
      key: 'institutionName',
    },
    {
      header: 'Condition Category',
      key: 'category',
    },
    {
      header: 'Submitted File',
      key: 'evidenceFile',
      render: (row) => (
        <span style={{ fontSize: '11px', color: 'var(--color-blue)', textDecoration: 'underline' }}>
          {row.evidenceFile || 'No file attached'}
        </span>
      ),
    },
    {
      header: 'Submission Date',
      key: 'submittedDate',
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
        <button
          onClick={() => navigate('/compliance')}
          className="btn btn-primary btn-sm"
          title="Verify Institutional Evidence"
        >
          <CheckSquare size={12} /> Verify Evidence
        </button>
      ),
    },
  ];

  const clarificationColumns = [
    {
      header: 'Obligation ID',
      key: 'id',
      render: (row) => <strong>{row.id}</strong>,
    },
    {
      header: 'Institution',
      key: 'institutionName',
    },
    {
      header: 'Clarification Note',
      key: 'clarificationNote',
      render: (row) => <span style={{ fontSize: '11px', color: 'var(--color-critical)' }}>{row.clarificationNote || 'Evidence rejected; clarification awaited.'}</span>,
    },
    {
      header: 'Deadline',
      key: 'dueDate',
    },
    {
      header: 'State',
      key: 'urgency',
      render: (row) => <StatusBadge status={row.urgency} />,
    },
  ];

  return (
    <div>
      {/* Banner */}
      <div className="panel" style={{ background: '#FFFFFF', borderLeft: '4px solid var(--color-teal)', marginBottom: '16px' }}>
        <div className="panel-body" style={{ padding: '16px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
          <div>
            <div style={{ fontSize: '10px', textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--color-teal)', fontWeight: '800' }}>
              POST-APPROVAL COMPLIANCE DESK
            </div>
            <h1 style={{ fontSize: '20px', color: 'var(--color-navy)', margin: '4px 0 2px', fontWeight: '800' }}>
              Affidavit, Condition &amp; Evidence Verification
            </h1>
            <p style={{ color: 'var(--color-text-muted)', fontSize: 'var(--font-size-sm)' }}>
              Reviewer: {user?.name} · Continuous monitoring of statutory undertakings and renewal documents
            </p>
          </div>

          <div style={{ display: 'flex', gap: '8px' }}>
            <button onClick={() => navigate('/compliance')} className="btn btn-primary btn-sm">
              <CheckCircle size={13} /> Compliance Center
            </button>
          </div>
        </div>
      </div>

      {/* 6 KPI Cards with Click Navigation */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '14px', marginBottom: '18px' }}>
        <KPICard
          title="Total Obligations"
          value={allObligations.length}
          subtitle="Statutory conditions registered"
          iconName="CheckSquare"
          tint="blue"
          targetRoute="/compliance"
        />
        <KPICard
          title="Evidence Awaiting Review"
          value={reviewQueue.length}
          subtitle="Uploaded proof awaiting audit"
          iconName="Clock"
          tint="amber"
          targetRoute="/compliance"
          filterParams={{ status: 'Evidence submitted' }}
        />
        <KPICard
          title="Due Soon"
          value={dueSoon.length}
          subtitle="Upcoming 7-day deadlines"
          iconName="Clock"
          tint="amber"
          targetRoute="/compliance"
        />
        <KPICard
          title="Overdue Obligations"
          value={overdueObligations.length}
          subtitle="Conditions breached"
          iconName="AlertTriangle"
          tint="red"
          targetRoute="/compliance"
          filterParams={{ urgency: 'Overdue' }}
        />
        <KPICard
          title="Clarification Required"
          value={clarificationQueue.length}
          subtitle="Defects communicated"
          iconName="AlertTriangle"
          tint="red"
          targetRoute="/compliance"
        />
        <KPICard
          title="Verified Compliance"
          value={verifiedCompliance.length}
          subtitle="Fully compliant institutions"
          iconName="CheckCircle"
          tint="green"
          targetRoute="/compliance"
        />
      </div>

      {/* Section 1: Verification Queue */}
      <DashboardSection
        title="Compliance Evidence Awaiting Verification"
        subtitle="Statutory proof submitted by institutions for mandatory approval conditions"
        action={
          <span className="badge badge-teal">
            {reviewQueue.length} Ready for Audit
          </span>
        }
        noPadding
      >
        <DataTable
          columns={reviewColumns}
          data={reviewQueue}
          emptyMessage="No compliance evidence currently awaiting desk review. (Backend API integration pending)"
        />
      </DashboardSection>

      {/* Section 2: Clarification Requests */}
      <DashboardSection
        title="Institutional Clarifications Pending"
        subtitle="Cases where submitted proof was deficient and formal clarification has been requested"
        action={
          <Link to="/compliance" className="btn btn-outline btn-sm">
            All Obligations ↗
          </Link>
        }
        noPadding
      >
        <DataTable
          columns={clarificationColumns}
          data={clarificationQueue}
          emptyMessage="No institutional clarifications currently pending."
        />
      </DashboardSection>
    </div>
  );
};
