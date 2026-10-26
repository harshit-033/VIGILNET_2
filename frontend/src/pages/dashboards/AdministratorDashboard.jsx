// SIH 26262 - Role 7: Administrator Dashboard (Live Backend Health & RBAC)
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useData } from '../../context/DataContext';
import { apiClient } from '../../services/apiClient';
import { KPICard } from '../../components/common/KPICard';
import { DataTable } from '../../components/common/DataTable';
import { StatusBadge } from '../../components/common/StatusBadge';
import { DashboardSection } from '../../components/common/DashboardSection';
import { RecentActivity } from '../../components/common/RecentActivity';
import { EmptyState } from '../../components/common/EmptyState';
import { Users, Server, Shield, FileCode, CheckCircle, RefreshCw, Activity, Database, AlertCircle } from 'lucide-react';

const ROLE_DEFINITIONS = [
  { id: 'ROLE-01', name: 'Institution Representative', scope: 'Assigned College / Institution', purpose: 'Dossier submission, deficiency responses, document renewal uploads' },
  { id: 'ROLE-02', name: 'Regional Officer', scope: 'Territorial Region (e.g. Northern)', purpose: 'Application intake, regional oversight, officer queue dispatch' },
  { id: 'ROLE-03', name: 'Scrutiny Officer', scope: 'Bureau Desk Queue', purpose: 'Statutory documentary scrutiny, observations, deficiency notices' },
  { id: 'ROLE-04', name: 'EVC Coordinator', scope: 'National / Multi-Region', purpose: 'Expert Visit Committee panel allocation and inspection tracking' },
  { id: 'ROLE-05', name: 'Compliance Reviewer', scope: 'National Desk', purpose: 'Post-approval statutory condition & NOC evidence verification' },
  { id: 'ROLE-06', name: 'Senior Management', scope: 'All India Executive', purpose: 'Executive intelligence, SLA pendency, approval decree issuance' },
  { id: 'ROLE-07', name: 'Administrator', scope: 'Platform Wide', purpose: 'RBAC governance, backend telemetry, system health monitoring' },
];

export const AdministratorDashboard = () => {
  const { user, availableRoles } = useAuth();
  const navigate = useNavigate();
  const { applications, institutions, activity, fetchApplicationsFromBackend, fetchInstitutionsFromBackend } = useData();

  const [healthStatus, setHealthStatus] = useState(null);
  const [healthLoading, setHealthLoading] = useState(true);
  const [healthError, setHealthError] = useState(null);

  const fetchHealth = async () => {
    setHealthLoading(true);
    setHealthError(null);
    try {
      const data = await apiClient('/health');
      setHealthStatus(data);
    } catch (err) {
      console.error('Health fetch failed:', err);
      setHealthError(err.message || 'Unable to reach backend health endpoint.');
      setHealthStatus(null);
    } finally {
      setHealthLoading(false);
    }
  };

  useEffect(() => {
    fetchHealth();
  }, []);

  const roleColumns = [
    {
      header: 'Role Code',
      key: 'id',
      render: (row) => <strong>{row.id}</strong>,
    },
    {
      header: 'Official Role Title',
      key: 'name',
      render: (row) => <StatusBadge status={row.name} variant="blue" />,
    },
    {
      header: 'Jurisdictional Scope',
      key: 'scope',
    },
    {
      header: 'Operational Governance Function',
      key: 'purpose',
    },
    {
      header: 'Status',
      key: 'status',
      render: () => <StatusBadge status="Active" />,
    },
  ];

  return (
    <div>
      {/* Banner */}
      <div className="panel" style={{ background: '#FFFFFF', borderLeft: '4px solid #1A2C3D', marginBottom: '16px' }}>
        <div className="panel-body" style={{ padding: '16px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
          <div>
            <div style={{ fontSize: '10px', textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--color-navy)', fontWeight: '800' }}>
              SYSTEM CONFIGURATION &amp; PLATFORM ADMINISTRATION
            </div>
            <h1 style={{ fontSize: '20px', color: 'var(--color-navy)', margin: '4px 0 2px', fontWeight: '800' }}>
              AICTE Core Platform Administration
            </h1>
            <p style={{ color: 'var(--color-text-muted)', fontSize: 'var(--font-size-sm)' }}>
              Administrator: {user?.name || 'System Administrator'} · Connected to Spring Boot Core API (Port 8080) &amp; PostgreSQL
            </p>
          </div>

          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              onClick={() => {
                fetchHealth();
                fetchApplicationsFromBackend();
                fetchInstitutionsFromBackend();
              }}
              className="btn btn-outline btn-sm"
              title="Refresh telemetry and live records"
            >
              <RefreshCw size={13} /> Refresh Telemetry
            </button>
            <button onClick={() => navigate('/applications')} className="btn btn-primary btn-sm">
              <FileCode size={13} /> Live Applications ({applications.length})
            </button>
          </div>
        </div>
      </div>

      {/* 6 KPI Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '14px', marginBottom: '18px' }}>
        <KPICard
          title="Backend Service"
          value={healthStatus?.status || (healthLoading ? 'Checking...' : 'DOWN')}
          subtitle={healthStatus?.application || 'aicte-compliance-platform'}
          iconName="Server"
          tint={healthStatus?.status === 'UP' ? 'green' : 'red'}
        />
        <KPICard
          title="Live Applications"
          value={applications.length}
          subtitle="PostgreSQL database records"
          iconName="FileCode"
          tint="blue"
          targetRoute="/applications"
        />
        <KPICard
          title="Canonical Institutions"
          value={institutions.length}
          subtitle="Registered institutional entities"
          iconName="Database"
          tint="teal"
          targetRoute="/applications"
        />
        <KPICard
          title="Configured Roles"
          value={availableRoles?.length || 7}
          subtitle="All 7 AICTE operational roles"
          iconName="Shield"
          tint="purple"
        />
        <KPICard
          title="Audit Events Logged"
          value={activity.length}
          subtitle="Session workflow transitions"
          iconName="Activity"
          tint="blue"
        />
        <KPICard
          title="Active Workflow Version"
          value="APH 2026-27"
          subtitle="Deterministic rule progression"
          iconName="CheckCircle"
          tint="green"
        />
      </div>

      {/* Backend Health Telemetry Box */}
      <div className="panel" style={{ marginBottom: '18px' }}>
        <div className="panel-header" style={{ padding: '12px 18px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h3 className="panel-title" style={{ fontSize: '14px', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Server size={15} color="var(--color-navy)" /> Core Backend API Telemetry
            </h3>
            <p className="panel-subtitle" style={{ fontSize: '11px' }}>
              Live connection status for Spring Boot RESTful microservice &amp; PostgreSQL persistence
            </p>
          </div>
          {healthStatus?.status === 'UP' ? (
            <span className="badge badge-green">Service Operational</span>
          ) : (
            <span className="badge badge-red">{healthError || 'Service Unreachable'}</span>
          )}
        </div>
        <div className="panel-body" style={{ padding: '14px 18px' }}>
          {healthLoading ? (
            <div style={{ color: 'var(--color-text-muted)', fontSize: '12px' }}>Pinging backend API /health endpoint...</div>
          ) : healthError ? (
            <div style={{ color: 'var(--color-critical)', fontSize: '12px', display: 'flex', alignItems: 'center', gap: '6px' }}>
              <AlertCircle size={15} /> Backend API unreachable: {healthError}
            </div>
          ) : (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '14px', fontSize: '12px' }}>
              <div>
                <span style={{ color: 'var(--color-text-muted)', display: 'block', fontSize: '11px' }}>Service Name</span>
                <strong>{healthStatus.application || 'aicte-compliance-platform'}</strong>
              </div>
              <div>
                <span style={{ color: 'var(--color-text-muted)', display: 'block', fontSize: '11px' }}>Service Health</span>
                <strong style={{ color: 'var(--color-teal)' }}>{healthStatus.status}</strong>
              </div>
              <div>
                <span style={{ color: 'var(--color-text-muted)', display: 'block', fontSize: '11px' }}>Timestamp</span>
                <span>{healthStatus.timestamp ? new Date(healthStatus.timestamp).toLocaleString('en-GB') : 'Live'}</span>
              </div>
              <div>
                <span style={{ color: 'var(--color-text-muted)', display: 'block', fontSize: '11px' }}>Database Table /applications</span>
                <strong>{applications.length} Dossiers Loaded</strong>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Section 1: Official Role Governance Matrix */}
      <DashboardSection
        title="Role-Based Access Control (RBAC) Governance Matrix"
        subtitle="Configured operational roles, organizational scopes, and workflow authorizations"
        action={
          <span className="badge badge-blue">
            {availableRoles?.length || 7} Platform Roles
          </span>
        }
        noPadding
      >
        <DataTable columns={roleColumns} data={ROLE_DEFINITIONS} />
      </DashboardSection>

      {/* Section 2: System Audit Activity */}
      <DashboardSection
        title="Platform Audit Trail &amp; Activity Log"
        subtitle="Recent administrative, workflow, and scrutiny system events logged from active sessions"
        noPadding
      >
        {activity.length === 0 ? (
          <EmptyState
            title="No Platform Audit Activities"
            description="Workflow actions and desk determinations performed during this operational session will appear here."
            icon="Activity"
          />
        ) : (
          <RecentActivity activities={activity} />
        )}
      </DashboardSection>
    </div>
  );
};
