// SIH 26262 - Reports & Analytics Intelligence Module (/reports)

import React, { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useData } from '../context/DataContext';
import { ROLES } from '../constants/roles';
import { KPICard } from '../components/common/KPICard';
import { StatusBadge } from '../components/common/StatusBadge';
import { EmptyState } from '../components/common/EmptyState';
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
import {
  BarChart3,
  Download,
  RotateCcw,
  Filter,
  Calendar,
  MapPin,
  FileText,
  Clock,
  AlertTriangle,
  CheckCircle2,
  ExternalLink,
  ShieldAlert,
  Building2,
  FileCheck,
} from 'lucide-react';

export const ReportsPage = () => {
  const { user, role } = useAuth();
  const {
    applications,
    evc,
    compliance,
    documents,
    alerts,
  } = useData();

  const isInstitutionRep = role === ROLES.INSTITUTION_REP;
  const isRegionalOfficer = role === ROLES.REGIONAL_OFFICER;

  // Filter States
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [selectedRegion, setSelectedRegion] = useState(
    isRegionalOfficer ? 'Northern Region' : 'ALL'
  );
  const [selectedType, setSelectedType] = useState('ALL');

  // Base Scoping based on Role Permissions
  const baseApplications = useMemo(() => {
    if (isInstitutionRep) {
      if (user?.institutionId) {
        return applications.filter((a) => a.institutionId === user.institutionId);
      }
      return applications;
    }
    if (isRegionalOfficer) {
      return applications.filter((a) => a.region === (user?.region || 'Northern Region'));
    }
    return applications;
  }, [applications, isInstitutionRep, isRegionalOfficer, user]);

  // Apply User Filters
  const filteredApplications = useMemo(() => {
    return baseApplications.filter((app) => {
      // 1. Date Range (using submissionDate)
      if (fromDate && app.submissionDate < fromDate) return false;
      if (toDate && app.submissionDate > toDate) return false;

      // 2. Region Filter
      if (selectedRegion !== 'ALL' && app.region !== selectedRegion) return false;

      // 3. Application Type Filter
      if (selectedType !== 'ALL' && app.applicationType !== selectedType) return false;

      return true;
    });
  }, [baseApplications, fromDate, toDate, selectedRegion, selectedType]);

  // Related filtered IDs to maintain relational consistency
  const filteredAppIds = useMemo(
    () => new Set(filteredApplications.map((a) => a.id)),
    [filteredApplications]
  );
  const filteredInstIds = useMemo(
    () => new Set(filteredApplications.map((a) => a.institutionId)),
    [filteredApplications]
  );

  // Filtered EVC records
  const filteredEVC = useMemo(() => {
    if (isInstitutionRep) {
      const instId = user?.institutionId || 'INST-1025';
      return evc.filter((e) => e.institutionId === instId);
    }
    return evc.filter(
      (e) => filteredAppIds.has(e.applicationId) || filteredInstIds.has(e.institutionId)
    );
  }, [evc, isInstitutionRep, user, filteredAppIds, filteredInstIds]);

  // Filtered Compliance records
  const filteredCompliance = useMemo(() => {
    if (isInstitutionRep) {
      const instId = user?.institutionId || 'INST-1025';
      return compliance.filter((c) => c.institutionId === instId);
    }
    return compliance.filter(
      (c) => filteredAppIds.has(c.applicationId) || filteredInstIds.has(c.institutionId)
    );
  }, [compliance, isInstitutionRep, user, filteredAppIds, filteredInstIds]);

  // Filtered Documents records
  const filteredDocuments = useMemo(() => {
    if (isInstitutionRep) {
      const instId = user?.institutionId || 'INST-1025';
      return documents.filter((d) => d.institutionId === instId);
    }
    return documents.filter((d) => filteredInstIds.has(d.institutionId));
  }, [documents, isInstitutionRep, user, filteredInstIds]);

  // Filtered Escalations records
  const filteredEscalations = useMemo(() => {
    const escList = alerts.filter(
      (a) => a.deliveryStatus === 'Escalated' || a.urgency === 'Escalated'
    );
    if (isInstitutionRep) {
      const instId = user?.institutionId || 'INST-1025';
      return escList.filter((a) => a.institutionId === instId);
    }
    return escList.filter(
      (a) => filteredInstIds.has(a.institutionId) || filteredAppIds.has(a.relatedRecordId)
    );
  }, [alerts, isInstitutionRep, user, filteredInstIds, filteredAppIds]);

  // KPI Calculations
  const kpiTotalApps = filteredApplications.length;
  const kpiPendingApps = filteredApplications.filter(
    (a) => a.currentStage !== 'Closed' && a.status !== 'Completed'
  ).length;
  const kpiOverdueApps = filteredApplications.filter(
    (a) => a.status === 'Overdue' || a.urgency === 'Overdue'
  ).length;
  const kpiEVCPending = filteredEVC.filter(
    (e) => e.status !== 'Complete for stage' && e.status !== 'Visit completed'
  ).length;
  const kpiComplianceDue = filteredCompliance.filter(
    (c) => c.status !== 'Verified compliant'
  ).length;
  const kpiEscalatedCases = filteredEscalations.length;

  // Chart 1: Stage-wise Pendency (Bar Chart)
  const stageChartData = useMemo(() => {
    const stageMap = {};
    filteredApplications.forEach((app) => {
      const st = app.currentStage || 'Scrutiny';
      stageMap[st] = (stageMap[st] || 0) + 1;
    });

    return Object.keys(stageMap).map((st) => ({
      stage: st,
      count: stageMap[st],
    }));
  }, [filteredApplications]);

  // Chart 2: Application Ageing (Bar Chart)
  const ageingChartData = useMemo(() => {
    let b0_7 = 0;
    let b8_14 = 0;
    let b15_30 = 0;
    let b30plus = 0;

    filteredApplications.forEach((app) => {
      const days = app.ageingDays || app.ageDays || 0;
      if (days <= 7) b0_7++;
      else if (days <= 14) b8_14++;
      else if (days <= 30) b15_30++;
      else b30plus++;
    });

    return [
      { band: '0 - 7 Days', count: b0_7 },
      { band: '8 - 14 Days', count: b8_14 },
      { band: '15 - 30 Days', count: b15_30 },
      { band: '30+ Days', count: b30plus },
    ];
  }, [filteredApplications]);

  // Chart 3: Region-wise Workload (Bar Chart)
  const regionChartData = useMemo(() => {
    const regionMap = {};
    filteredApplications.forEach((app) => {
      const reg = app.region || 'Northern Region';
      regionMap[reg] = (regionMap[reg] || 0) + 1;
    });

    return Object.keys(regionMap).map((reg) => ({
      region: reg.replace(' Region', ''),
      count: regionMap[reg],
    }));
  }, [filteredApplications]);

  // Chart 4: EVC Status (Bar Chart)
  const evcChartData = useMemo(() => {
    const evcMap = {};
    filteredEVC.forEach((e) => {
      const st = e.status || 'Awaiting scheduling';
      evcMap[st] = (evcMap[st] || 0) + 1;
    });

    return Object.keys(evcMap).map((st) => ({
      status: st,
      count: evcMap[st],
    }));
  }, [filteredEVC]);

  // Chart 5: Compliance Status (Pie Chart)
  const complianceChartData = useMemo(() => {
    const compMap = {};
    filteredCompliance.forEach((c) => {
      const st = c.status || 'Verification pending';
      compMap[st] = (compMap[st] || 0) + 1;
    });

    const colors = {
      'Verified compliant': '#10B981',
      'Evidence submitted': '#028090',
      'Verification pending': '#1B527A',
      'Clarification requested': '#D97706',
      'Pending': '#EF4444',
    };

    return Object.keys(compMap).map((st) => ({
      name: st,
      value: compMap[st],
      color: colors[st] || '#64748B',
    }));
  }, [filteredCompliance]);

  // Reset Filters Handler
  const handleResetFilters = () => {
    setFromDate('');
    setToDate('');
    setSelectedRegion(isRegionalOfficer ? 'Northern Region' : 'ALL');
    setSelectedType('ALL');
  };

  // CSV Export Handler
  const handleExportCSV = () => {
    if (filteredApplications.length === 0) {
      alert('No application records to export under current filters.');
      return;
    }

    const headers = [
      'Application ID',
      'Institution ID',
      'Institution Name',
      'Application Type',
      'Region',
      'Current Stage',
      'Assigned Owner',
      'Submission Date',
      'Deadline',
      'Ageing (Days)',
      'Status',
    ];

    const rows = filteredApplications.map((app) => [
      `"${app.id}"`,
      `"${app.institutionId}"`,
      `"${app.institutionName.replace(/"/g, '""')}"`,
      `"${app.applicationType.replace(/"/g, '""')}"`,
      `"${app.region}"`,
      `"${app.currentStage}"`,
      `"${app.owner || '—'}"`,
      `"${app.submissionDate || '—'}"`,
      `"${app.deadline || app.dueDate || '—'}"`,
      app.ageingDays || app.ageDays || 0,
      `"${app.status}"`,
    ]);

    const csvContent = [headers.join(','), ...rows.map((r) => r.join(','))].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute(
      'download',
      `AICTE_Approval_Analytics_Report_${new Date().toISOString().split('T')[0]}.csv`
    );
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div>
      {/* Header Bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '16px', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <div style={{ fontSize: '10px', textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--color-teal)', fontWeight: '800' }}>
            REGULATORY INTELLIGENCE · WORKLOAD &amp; COMPLIANCE ANALYTICS
          </div>
          <h1 style={{ fontSize: '22px', fontWeight: '800', color: 'var(--color-navy)', margin: '4px 0 2px' }}>
            Reports &amp; Analytics Workbench
          </h1>
          <p style={{ color: 'var(--color-text-muted)', fontSize: 'var(--font-size-sm)' }}>
            Dynamic statutory analytics derived live from connected application, inspection, compliance, and escalation registers.
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <button
            onClick={handleExportCSV}
            className="btn btn-primary"
            title="Download CSV report of currently filtered records"
          >
            <Download size={14} /> Export Filtered CSV
          </button>
        </div>
      </div>

      {/* Filter Toolbar Card */}
      <div className="panel" style={{ marginBottom: '18px', padding: '14px 16px', background: '#FCFDFE' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '12px', fontSize: 'var(--font-size-xs)', fontWeight: '700', color: 'var(--color-navy)' }}>
          <Filter size={14} style={{ color: 'var(--color-blue)' }} /> Analytics Filter Controls
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '12px' }}>
          {/* Date Range: From */}
          <div>
            <label className="form-label" style={{ fontSize: '11px' }}>From Submission Date</label>
            <input
              type="date"
              className="form-input"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
              style={{ fontSize: '11px', padding: '6px 8px' }}
            />
          </div>

          {/* Date Range: To */}
          <div>
            <label className="form-label" style={{ fontSize: '11px' }}>To Submission Date</label>
            <input
              type="date"
              className="form-input"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
              style={{ fontSize: '11px', padding: '6px 8px' }}
            />
          </div>

          {/* Region Filter */}
          <div>
            <label className="form-label" style={{ fontSize: '11px' }}>Region</label>
            <select
              className="form-select"
              value={selectedRegion}
              onChange={(e) => setSelectedRegion(e.target.value)}
              disabled={isRegionalOfficer}
              style={{ fontSize: '11px', padding: '6px 8px' }}
            >
              <option value="ALL">All Regions (National)</option>
              <option value="Northern Region">Northern Region</option>
              <option value="Southern Region">Southern Region</option>
              <option value="Eastern Region">Eastern Region</option>
              <option value="Western Region">Western Region</option>
              <option value="Central Region">Central Region</option>
            </select>
          </div>

          {/* Application Type Filter */}
          <div>
            <label className="form-label" style={{ fontSize: '11px' }}>Application Type</label>
            <select
              className="form-select"
              value={selectedType}
              onChange={(e) => setSelectedType(e.target.value)}
              style={{ fontSize: '11px', padding: '6px 8px' }}
            >
              <option value="ALL">All Application Types</option>
              <option value="Extension of Approval / Compliance Review">Extension of Approval / Compliance Review</option>
              <option value="New Technical Institution Setup">New Technical Institution Setup</option>
              <option value="Approval of New Engineering Department">Approval of New Engineering Department</option>
              <option value="Approval for Additional Division / Intake">Approval for Additional Division / Intake</option>
              <option value="Annual Mandatory Document Renewal">Annual Mandatory Document Renewal</option>
              <option value="Closure of Non-Viable Diploma Program">Closure of Non-Viable Diploma Program</option>
              <option value="Affiliation and Program Expansion">Affiliation and Program Expansion</option>
            </select>
          </div>
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '12px', borderTop: '1px solid #EDF2F7', paddingTop: '10px' }}>
          <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
            Showing <strong>{filteredApplications.length}</strong> matching application dossiers · Scope: <strong>{isInstitutionRep ? user?.institutionName : role}</strong>
          </div>

          <button
            onClick={handleResetFilters}
            className="btn btn-outline btn-sm"
            style={{ fontSize: '11px' }}
          >
            <RotateCcw size={12} /> Reset Filters
          </button>
        </div>
      </div>

      {/* 6 KPI Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: '14px', marginBottom: '20px' }}>
        <KPICard
          title="Total Applications"
          value={kpiTotalApps}
          subtitle="Matching criteria"
          iconName="FileText"
          tint="blue"
        />
        <KPICard
          title="Pending Applications"
          value={kpiPendingApps}
          subtitle="In scrutiny or committee"
          iconName="Clock"
          tint="amber"
        />
        <KPICard
          title="Overdue Applications"
          value={kpiOverdueApps}
          subtitle="SLA window breached"
          iconName="AlertTriangle"
          tint="red"
        />
        <KPICard
          title="EVC Pending"
          value={kpiEVCPending}
          subtitle="Awaiting visit / report"
          iconName="Calendar"
          tint="teal"
        />
        <KPICard
          title="Compliance Due"
          value={kpiComplianceDue}
          subtitle="Unverified undertakings"
          iconName="FileCheck"
          tint="purple"
        />
        <KPICard
          title="Escalated Cases"
          value={kpiEscalatedCases}
          subtitle="Referrals to Senior Mgmt"
          iconName="ShieldAlert"
          tint="red"
        />
      </div>

      {/* Section 1 & 2: Stage-wise Pendency & Application Ageing (Bar Charts) */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '16px', marginBottom: '18px' }}>
        {/* 1. Stage-wise Pendency */}
        <div className="panel" style={{ padding: '16px' }}>
          <div style={{ marginBottom: '12px' }}>
            <h2 style={{ fontSize: '14px', fontWeight: '800', color: 'var(--color-navy)', margin: 0 }}>
              1. Stage-wise Application Pendency
            </h2>
            <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
              Distribution of applications by active approval process workflow stage
            </div>
          </div>

          <div style={{ width: '100%', height: '240px' }}>
            {stageChartData.length === 0 ? (
              <EmptyState message="No stage data under selected filter." />
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={stageChartData} margin={{ top: 10, right: 10, left: -20, bottom: 20 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
                  <XAxis dataKey="stage" tick={{ fontSize: 10, fill: '#4A5568' }} interval={0} angle={-15} textAnchor="end" />
                  <YAxis allowDecimals={false} tick={{ fontSize: 10, fill: '#4A5568' }} />
                  <Tooltip />
                  <Bar dataKey="count" fill="#1B527A" isAnimationActive={false} name="Applications" />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        {/* 2. Application Ageing */}
        <div className="panel" style={{ padding: '16px' }}>
          <div style={{ marginBottom: '12px' }}>
            <h2 style={{ fontSize: '14px', fontWeight: '800', color: 'var(--color-navy)', margin: 0 }}>
              2. Application Ageing Profile
            </h2>
            <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
              Turnaround duration from initial submission date
            </div>
          </div>

          <div style={{ width: '100%', height: '240px' }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={ageingChartData} margin={{ top: 10, right: 10, left: -20, bottom: 20 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
                <XAxis dataKey="band" tick={{ fontSize: 10, fill: '#4A5568' }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 10, fill: '#4A5568' }} />
                <Tooltip />
                <Bar dataKey="count" fill="#028090" isAnimationActive={false} name="Dossiers" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Section 3, 4, 5: Region Workload, EVC Status & Compliance Status */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '16px', marginBottom: '20px' }}>
        {/* 3. Region-wise Workload */}
        <div className="panel" style={{ padding: '16px' }}>
          <div style={{ marginBottom: '12px' }}>
            <h2 style={{ fontSize: '14px', fontWeight: '800', color: 'var(--color-navy)', margin: 0 }}>
              3. Region-wise Workload
            </h2>
            <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
              Application load across AICTE regional directorates
            </div>
          </div>

          <div style={{ width: '100%', height: '220px' }}>
            {regionChartData.length === 0 ? (
              <EmptyState message="No regional data." />
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={regionChartData} margin={{ top: 10, right: 10, left: -20, bottom: 20 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
                  <XAxis dataKey="region" tick={{ fontSize: 10, fill: '#4A5568' }} />
                  <YAxis allowDecimals={false} tick={{ fontSize: 10, fill: '#4A5568' }} />
                  <Tooltip />
                  <Bar dataKey="count" fill="#102B46" isAnimationActive={false} name="Applications" />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        {/* 4. EVC Status */}
        <div className="panel" style={{ padding: '16px' }}>
          <div style={{ marginBottom: '12px' }}>
            <h2 style={{ fontSize: '14px', fontWeight: '800', color: 'var(--color-navy)', margin: 0 }}>
              4. EVC Inspection Status
            </h2>
            <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
              Expert committee visit pipeline
            </div>
          </div>

          <div style={{ width: '100%', height: '220px' }}>
            {evcChartData.length === 0 ? (
              <EmptyState message="No EVC cases for filtered records." />
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={evcChartData} margin={{ top: 10, right: 10, left: -20, bottom: 25 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E2E8F0" />
                  <XAxis dataKey="status" tick={{ fontSize: 9, fill: '#4A5568' }} interval={0} angle={-20} textAnchor="end" />
                  <YAxis allowDecimals={false} tick={{ fontSize: 10, fill: '#4A5568' }} />
                  <Tooltip />
                  <Bar dataKey="count" fill="#1B527A" isAnimationActive={false} name="EVC Cases" />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        {/* 5. Compliance Status */}
        <div className="panel" style={{ padding: '16px' }}>
          <div style={{ marginBottom: '12px' }}>
            <h2 style={{ fontSize: '14px', fontWeight: '800', color: 'var(--color-navy)', margin: 0 }}>
              5. Statutory Compliance Status
            </h2>
            <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
              Condition fulfillment &amp; verification breakdown
            </div>
          </div>

          <div style={{ width: '100%', height: '220px' }}>
            {complianceChartData.length === 0 ? (
              <EmptyState message="No compliance obligations for filtered records." />
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={complianceChartData}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="45%"
                    outerRadius={65}
                    innerRadius={35}
                    isAnimationActive={false}
                  >
                    {complianceChartData.map((entry, idx) => (
                      <Cell key={`cell-${idx}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip />
                  <Legend verticalAlign="bottom" wrapperStyle={{ fontSize: '10px' }} />
                </PieChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>
      </div>

      {/* Section 6: Document Expiry Summary (Table) */}
      <div className="panel" style={{ marginBottom: '20px', padding: 0 }}>
        <div style={{ padding: '14px 16px', borderBottom: '1px solid #E2E8F0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h2 style={{ fontSize: '14px', fontWeight: '800', color: 'var(--color-navy)', margin: 0 }}>
              6. Document Expiry Summary
            </h2>
            <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
              Statutory legal instruments associated with filtered institutions
            </div>
          </div>
          <Link to="/documents" className="btn btn-outline btn-sm">
            Open Registry <ExternalLink size={12} />
          </Link>
        </div>

        <div className="panel-body" style={{ padding: 0 }}>
          {filteredDocuments.length === 0 ? (
            <EmptyState message="No document instruments found under the current filter scope." />
          ) : (
            <div className="table-container" style={{ border: 'none' }}>
              <table className="gov-table">
                <thead>
                  <tr>
                    <th>Document ID</th>
                    <th>Document Type</th>
                    <th>Institution</th>
                    <th>Expiry Date</th>
                    <th>Validity Status</th>
                    <th>Verification Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredDocuments.slice(0, 6).map((doc) => (
                    <tr key={doc.id}>
                      <td>
                        <strong>{doc.id}</strong>
                      </td>
                      <td>
                        <strong>{doc.documentType || doc.category}</strong>
                      </td>
                      <td>
                        {doc.institutionName}
                      </td>
                      <td>
                        <span
                          style={{
                            fontWeight: '700',
                            color:
                              doc.validityStatus === 'Overdue' || doc.verificationStatus === 'Expired / no verified renewal'
                                ? 'var(--color-critical)'
                                : doc.validityStatus === 'Due soon'
                                ? 'var(--color-amber)'
                                : 'inherit',
                          }}
                        >
                          {doc.expiryDate}
                        </span>
                      </td>
                      <td>
                        <span
                          className={`badge ${
                            doc.validityStatus === 'Valid'
                              ? 'badge-green'
                              : doc.validityStatus === 'Due soon'
                              ? 'badge-amber'
                              : 'badge-red'
                          }`}
                        >
                          {doc.validityStatus}
                        </span>
                      </td>
                      <td>
                        <StatusBadge status={doc.verificationStatus} />
                      </td>
                      <td>
                        <Link to="/documents" className="btn btn-outline btn-sm">
                          View
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Section 7: Escalated Cases (Table) */}
      <div className="panel" style={{ padding: 0 }}>
        <div style={{ padding: '14px 16px', borderBottom: '1px solid #E2E8F0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h2 style={{ fontSize: '14px', fontWeight: '800', color: 'var(--color-navy)', margin: 0 }}>
              7. Escalated Cases &amp; Critical Exceptions
            </h2>
            <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
              Active escalations flagged for Competent Authority intervention
            </div>
          </div>
          <Link to="/alerts" className="btn btn-outline btn-sm">
            Open Alerts Center <ExternalLink size={12} />
          </Link>
        </div>

        <div className="panel-body" style={{ padding: 0 }}>
          {filteredEscalations.length === 0 ? (
            <EmptyState message="No escalated cases recorded under current filtered parameters." />
          ) : (
            <div className="table-container" style={{ border: 'none' }}>
              <table className="gov-table">
                <thead>
                  <tr>
                    <th>Alert ID</th>
                    <th>Related Record</th>
                    <th>Institution</th>
                    <th>Trigger Date</th>
                    <th>Delivery Status</th>
                    <th>Required Action</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredEscalations.map((esc) => (
                    <tr key={esc.id}>
                      <td>
                        <strong style={{ color: 'var(--color-critical)' }}>{esc.id}</strong>
                      </td>
                      <td>
                        <Link
                          to={
                            esc.relatedRecordId?.startsWith('APP-')
                              ? `/applications/${esc.relatedRecordId}`
                              : esc.relatedRecordId?.startsWith('EVC-')
                              ? '/evc'
                              : esc.relatedRecordId?.startsWith('DOC-')
                              ? '/documents'
                              : '/compliance'
                          }
                          style={{ color: 'var(--color-blue)', fontWeight: '700' }}
                        >
                          {esc.relatedRecordId}
                        </Link>
                      </td>
                      <td>
                        <strong>{esc.institutionName}</strong>
                      </td>
                      <td style={{ fontSize: '11px' }}>
                        {esc.createdAt}
                      </td>
                      <td>
                        <span className="badge badge-purple">
                          {esc.deliveryStatus}
                        </span>
                      </td>
                      <td style={{ maxWidth: '280px', whiteSpace: 'normal', fontSize: '11px' }}>
                        {esc.requiredAction || esc.message}
                      </td>
                      <td>
                        <Link to="/alerts" className="btn btn-outline btn-sm">
                          View Alert
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
