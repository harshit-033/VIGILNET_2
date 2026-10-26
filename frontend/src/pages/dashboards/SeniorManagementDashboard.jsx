// SIH 26262 - Role 6: Senior Management Dashboard (Executive Intelligence)
import React, { useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useData } from '../../context/DataContext';
import { KPICard } from '../../components/common/KPICard';
import { DataTable } from '../../components/common/DataTable';
import { StatusBadge } from '../../components/common/StatusBadge';
import { DashboardSection } from '../../components/common/DashboardSection';
import { EmptyState } from '../../components/common/EmptyState';
import { Building, Clock, AlertTriangle, CheckCircle, TrendingUp, ShieldAlert, Eye } from 'lucide-react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend,
} from 'recharts';

export const SeniorManagementDashboard = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const { applications, alerts, evc, compliance } = useData();

  // Live Application Metrics derived from Spring Boot backend
  const activeApplications = useMemo(
    () => applications.filter((a) => a.status !== 'Completed'),
    [applications]
  );
  const pendingApprovals = useMemo(
    () => applications.filter((a) => a.currentStage === 'Committee review' || a.currentStage === 'Decision recorded'),
    [applications]
  );
  const overdueCases = useMemo(
    () => applications.filter((a) => a.status === 'Overdue'),
    [applications]
  );

  const evcDelays = useMemo(
    () => evc.filter((e) => e.urgency === 'Overdue' || e.status === 'Report pending'),
    [evc]
  );
  const compliantCount = useMemo(
    () => compliance.filter((c) => c.status === 'Verified compliant').length,
    [compliance]
  );
  const complianceRateStr = compliance.length > 0 ? `${Math.round((compliantCount / compliance.length) * 100)}%` : '0%';
  const escalations = useMemo(
    () => alerts.filter((a) => a.urgency === 'Escalated' || a.urgency === 'Overdue'),
    [alerts]
  );

  // Chart 1: Stage-Wise Pendency (Dynamically calculated from live applications)
  const stageData = useMemo(() => {
    if (applications.length === 0) return [];
    const counts = {
      Scrutiny: 0,
      'Re-Scrutiny': 0,
      'EVC Scheduling': 0,
      Committee: 0,
      'Decision Desk': 0,
    };

    applications.forEach((app) => {
      const stage = app.currentStage;
      if (stage === 'Scrutiny') counts.Scrutiny++;
      else if (stage === 'Re-scrutiny' || stage === 'Re-Scrutiny') counts['Re-Scrutiny']++;
      else if (stage === 'EVC scheduling' || stage === 'EVC visit') counts['EVC Scheduling']++;
      else if (stage === 'Committee review') counts.Committee++;
      else if (stage === 'Decision recorded' || stage === 'Completed') counts['Decision Desk']++;
      else counts.Scrutiny++;
    });

    return [
      { name: 'Scrutiny', count: counts.Scrutiny, color: '#1B527A' },
      { name: 'Re-Scrutiny', count: counts['Re-Scrutiny'], color: '#087E8B' },
      { name: 'EVC Visit', count: counts['EVC Scheduling'], color: '#B87917' },
      { name: 'Committee', count: counts.Committee, color: '#6B47A5' },
      { name: 'Decision Desk', count: counts['Decision Desk'], color: '#AC3E3E' },
    ];
  }, [applications]);

  // Chart 2: Region-Wise Performance (Dynamically calculated from live applications)
  const regionData = useMemo(() => {
    if (applications.length === 0) return [];
    const counts = {
      'Northern Region': 0,
      'Western Region': 0,
      'Southern Region': 0,
      'Eastern Region': 0,
    };

    applications.forEach((app) => {
      const r = app.region || 'Northern Region';
      if (counts[r] !== undefined) {
        counts[r]++;
      } else {
        counts['Northern Region']++;
      }
    });

    const colors = {
      'Northern Region': '#102B46',
      'Western Region': '#1B527A',
      'Southern Region': '#087E8B',
      'Eastern Region': '#4B7B9E',
    };

    return Object.entries(counts).map(([name, value]) => ({
      name,
      value,
      color: colors[name],
    }));
  }, [applications]);

  // Chart 3: Application Ageing Bands (Dynamically calculated from live applications)
  const ageingData = useMemo(() => {
    if (applications.length === 0) return [];
    let b0_7 = 0;
    let b8_14 = 0;
    let b15_30 = 0;
    let b30_plus = 0;

    applications.forEach((app) => {
      const age = app.ageDays != null ? Number(app.ageDays) : 0;
      if (age <= 7) b0_7++;
      else if (age <= 14) b8_14++;
      else if (age <= 30) b15_30++;
      else b30_plus++;
    });

    return [
      { band: '0–7 Days', count: b0_7 },
      { band: '8–14 Days', count: b8_14 },
      { band: '15–30 Days', count: b15_30 },
      { band: '30+ Days (Overdue)', count: b30_plus },
    ];
  }, [applications]);

  const escalationColumns = [
    {
      header: 'Alert ID',
      key: 'id',
      render: (row) => <strong>{row.id}</strong>,
    },
    {
      header: 'Subject & Reason',
      key: 'message',
    },
    {
      header: 'Institution',
      key: 'institutionName',
    },
    {
      header: 'Assigned Desk',
      key: 'recipient',
    },
    {
      header: 'Severity',
      key: 'urgency',
      render: (row) => <StatusBadge status={row.urgency} />,
    },
    {
      header: 'Action',
      key: 'action',
      render: (row) => (
        <button
          onClick={() => navigate('/alerts')}
          className="btn btn-primary btn-sm"
          title="Review Executive Escalation"
        >
          <Eye size={12} /> Intervene
        </button>
      ),
    },
  ];

  return (
    <div>
      {/* Banner */}
      <div className="panel" style={{ background: '#FFFFFF', borderLeft: '4px solid #6B47A5', marginBottom: '16px' }}>
        <div className="panel-body" style={{ padding: '16px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
          <div>
            <div style={{ fontSize: '10px', textTransform: 'uppercase', letterSpacing: '0.08em', color: '#6B47A5', fontWeight: '800' }}>
              EXECUTIVE OVERSIGHT &amp; COMPETENT AUTHORITY DESK
            </div>
            <h1 style={{ fontSize: '20px', color: 'var(--color-navy)', margin: '4px 0 2px', fontWeight: '800' }}>
              National Approval &amp; Pendency Intelligence
            </h1>
            <p style={{ color: 'var(--color-text-muted)', fontSize: 'var(--font-size-sm)' }}>
              Authority: {user?.name} · Live backend PostgreSQL data analytics and approval turnaround telemetry
            </p>
          </div>

          <div style={{ display: 'flex', gap: '8px' }}>
            <button onClick={() => navigate('/reports')} className="btn btn-primary btn-sm">
              <TrendingUp size={13} /> Full Analytics &amp; Reports
            </button>
          </div>
        </div>
      </div>

      {/* 6 KPI Cards with Click Navigation */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '14px', marginBottom: '18px' }}>
        <KPICard
          title="Active Applications"
          value={activeApplications.length}
          subtitle="Live PostgreSQL applications"
          iconName="Building"
          tint="blue"
          targetRoute="/applications"
        />
        <KPICard
          title="Pending Approvals"
          value={pendingApprovals.length}
          subtitle="Committee / decision stage"
          iconName="CheckCircle"
          tint="amber"
          targetRoute="/applications"
          filterParams={{ stage: 'Committee review' }}
        />
        <KPICard
          title="Overdue Cases"
          value={overdueCases.length}
          subtitle="Handbook turnaround SLA breached"
          iconName="Clock"
          tint="red"
          targetRoute="/applications"
          filterParams={{ status: 'Overdue' }}
        />
        <KPICard
          title="EVC Delays"
          value={evcDelays.length}
          subtitle="Scheduled inspection delays"
          iconName="AlertTriangle"
          tint="red"
          targetRoute="/evc"
        />
        <KPICard
          title="Compliance Rate"
          value={complianceRateStr}
          subtitle="Statutory conditions verified"
          iconName="CheckCircle"
          tint="green"
          targetRoute="/compliance"
        />
        <KPICard
          title="Escalated Cases"
          value={escalations.length}
          subtitle="Executive interventions required"
          iconName="ShieldAlert"
          tint="purple"
          targetRoute="/alerts"
          filterParams={{ severity: 'Escalated' }}
        />
      </div>

      {/* Recharts Row: Stage-wise Pendency & Regional Performance */}
      <div style={{ display: 'grid', gridTemplateColumns: '1.3fr 1fr', gap: '16px', marginBottom: '16px' }}>
        <DashboardSection
          title="Stage-Wise Application Pendency (Live Data)"
          subtitle={`Live volume (${applications.length} total) distributed across workflow milestones`}
        >
          {applications.length === 0 ? (
            <div style={{ padding: '30px', textAlign: 'center', color: 'var(--color-text-muted)', fontSize: '12px' }}>
              No application records in database to plot stage pendency.
            </div>
          ) : (
            <div style={{ width: '100%', height: '230px' }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={stageData} margin={{ top: 10, right: 20, left: -10, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E5EDF4" />
                  <XAxis dataKey="name" tick={{ fontSize: 11, fill: '#5E6E80' }} />
                  <YAxis allowDecimals={false} tick={{ fontSize: 11, fill: '#5E6E80' }} />
                  <Tooltip
                    contentStyle={{ backgroundColor: '#FFFFFF', borderColor: '#D5E0E8', fontSize: '12px' }}
                    formatter={(val) => [`${val} Applications`, 'Volume']}
                  />
                  <Bar dataKey="count" radius={[4, 4, 0, 0]} fill="#1B527A" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </DashboardSection>

        <DashboardSection
          title="Region-Wise Workload Share (Live Data)"
          subtitle={`Territorial distribution for current live applications (${applications.length} total)`}
        >
          {applications.length === 0 ? (
            <div style={{ padding: '30px', textAlign: 'center', color: 'var(--color-text-muted)', fontSize: '12px' }}>
              No applications available to display territorial share.
            </div>
          ) : (
            <div style={{ width: '100%', height: '230px' }}>
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={regionData}
                    cx="50%"
                    cy="50%"
                    innerRadius={50}
                    outerRadius={75}
                    paddingAngle={3}
                    dataKey="value"
                  >
                    {regionData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{ backgroundColor: '#FFFFFF', borderColor: '#D5E0E8', fontSize: '12px' }}
                    formatter={(val) => [`${val} Applications`, 'Count']}
                  />
                  <Legend
                    formatter={(value) => <span style={{ fontSize: '11px', color: '#1A2C3D' }}>{value}</span>}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
        </DashboardSection>
      </div>

      {/* Grid: Application Ageing & High-Priority Escalations */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.3fr', gap: '16px', marginBottom: '16px' }}>
        <DashboardSection
          title="Application Ageing Analysis (Live Data)"
          subtitle="Elapsed turnaround days computed for active database records"
        >
          {applications.length === 0 ? (
            <div style={{ padding: '30px', textAlign: 'center', color: 'var(--color-text-muted)', fontSize: '12px' }}>
              No application records in database to compute ageing.
            </div>
          ) : (
            <div style={{ width: '100%', height: '200px' }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={ageingData} layout="vertical" margin={{ top: 5, right: 30, left: 35, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#E5EDF4" />
                  <XAxis type="number" allowDecimals={false} tick={{ fontSize: 11, fill: '#5E6E80' }} />
                  <YAxis dataKey="band" type="category" tick={{ fontSize: 10, fill: '#1A2C3D' }} />
                  <Tooltip
                    contentStyle={{ backgroundColor: '#FFFFFF', borderColor: '#D5E0E8', fontSize: '12px' }}
                  />
                  <Bar dataKey="count" fill="#B87917" radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </DashboardSection>

        <DashboardSection
          title="Escalations Requiring Competent Authority Review"
          subtitle="Alerts escalated past regional desks requiring executive direction"
          action={
            <Link to="/alerts" className="btn btn-outline btn-sm">
              All Alerts ↗
            </Link>
          }
          noPadding
        >
          <DataTable columns={escalationColumns} data={escalations} emptyMessage="No active escalations." />
        </DashboardSection>
      </div>
    </div>
  );
};
