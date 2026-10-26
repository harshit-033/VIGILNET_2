// SIH 26262 - EVC Workbench Module (/evc)

import React, { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useData } from '../context/DataContext';
import { ROLES } from '../constants/roles';
import { KPICard } from '../components/common/KPICard';
import { TabsNav } from '../components/common/TabsNav';
import { StatusBadge } from '../components/common/StatusBadge';
import { EmptyState } from '../components/common/EmptyState';
import {
  isCompletedVisit,
  isScheduledVisit,
  isPendingScheduling,
  isUpcomingVisit,
  isReportPendingVisit,
  isOverdueVisit,
} from '../utils/evcLifecycle';
import { Modal } from '../components/common/Modal';
import {
  Calendar,
  Clock,
  CheckCircle2,
  AlertTriangle,
  FileText,
  UserPlus,
  Eye,
  Upload,
  Check,
  Search,
} from 'lucide-react';

export const EVCPage = () => {
  const { user, role } = useAuth();
  const { evc, scheduleEVC, markEVCCompleted, submitEVCReport, rescheduleEVC } = useData();

  const isAuthorizedToModify = [
    ROLES.EVC_COORDINATOR,
    ROLES.REGIONAL_OFFICER,
    ROLES.SENIOR_MANAGEMENT,
    ROLES.ADMINISTRATOR,
  ].includes(role);

  // Tab State
  const [activeTab, setActiveTab] = useState('EVC Queue');
  const [searchQuery, setSearchQuery] = useState('');

  // Modals state
  const [selectedEVC, setSelectedEVC] = useState(null);
  const [isScheduleModalOpen, setIsScheduleModalOpen] = useState(false);
  const [scheduleDate, setScheduleDate] = useState('');
  const [scheduleCoordinator, setScheduleCoordinator] = useState('C. Verma (EVC Operations)');
  const [scheduleType, setScheduleType] = useState('Physical On-Site Verification');
  const [scheduleExperts, setScheduleExperts] = useState('');

  const [isReportModalOpen, setIsReportModalOpen] = useState(false);
  const [reportOutcome, setReportOutcome] = useState('Recommended Approval');
  const [reportNotes, setReportNotes] = useState('');

  const [isDetailsModalOpen, setIsDetailsModalOpen] = useState(false);

  // Filtered by Search
  const searchedEVC = useMemo(() => {
    if (!searchQuery.trim()) return evc;
    const q = searchQuery.toLowerCase();
    return evc.filter(
      (e) =>
        e.id.toLowerCase().includes(q) ||
        e.applicationId.toLowerCase().includes(q) ||
        e.institutionName.toLowerCase().includes(q) ||
        e.coordinator.toLowerCase().includes(q)
    );
  }, [evc, searchQuery]);

  // Tab Filter
  const tabFilteredEVC = useMemo(() => {
    switch (activeTab) {
      case 'Scheduled Visits':
        return searchedEVC.filter(isScheduledVisit);
      case 'Upcoming Visits':
        return searchedEVC.filter(isUpcomingVisit);
      case 'Completed Visits':
        return searchedEVC.filter(isCompletedVisit);
      case 'Reports Pending':
        return searchedEVC.filter(isReportPendingVisit);
      case 'EVC Queue':
      default:
        return searchedEVC;
    }
  }, [searchedEVC, activeTab]);

  // KPI Calculations derived from canonical lifecycle definitions
  const kpiTotal = evc.length;
  const kpiPendingScheduling = evc.filter(isPendingScheduling).length;
  const kpiScheduledVisits = evc.filter(isScheduledVisit).length;
  const kpiUpcomingVisits = evc.filter(isUpcomingVisit).length;
  const kpiCompletedVisits = evc.filter(isCompletedVisit).length;
  const kpiReportsPending = evc.filter(isReportPendingVisit).length;

  const tabs = [
    { key: 'EVC Queue', label: 'EVC Queue', count: evc.length },
    { key: 'Scheduled Visits', label: 'Scheduled Visits', count: kpiScheduledVisits },
    { key: 'Upcoming Visits', label: 'Upcoming Visits', count: kpiUpcomingVisits },
    { key: 'Completed Visits', label: 'Completed Visits', count: kpiCompletedVisits },
    { key: 'Reports Pending', label: 'Reports Pending', count: kpiReportsPending },
  ];

  // Actions
  const handleOpenScheduleModal = (record) => {
    setSelectedEVC(record);
    setScheduleDate(record.visitDate || '');
    setScheduleCoordinator(record.coordinator || user?.name || '');
    setScheduleType(record.inspectionType || 'Physical On-Site Verification');
    setScheduleExperts(record.experts && record.experts.length > 0 ? record.experts.join('\n') : '');
    setIsScheduleModalOpen(true);
  };

  const handleSaveSchedule = async (e) => {
    e.preventDefault();
    if (!scheduleDate) {
      alert('Please select a confirmed visit date.');
      return;
    }

    const expertList = scheduleExperts
      .split('\n')
      .map((x) => x.trim())
      .filter((x) => x.length > 0);

    const res = await scheduleEVC({
      evcId: selectedEVC.id,
      visitDate: scheduleDate,
      coordinator: scheduleCoordinator,
      experts: expertList,
      inspectionType: scheduleType,
      actorName: user?.name,
      actorRole: role,
    });

    if (res.success) {
      setIsScheduleModalOpen(false);
      alert(`Visit for ${selectedEVC.id} successfully scheduled on ${scheduleDate}.`);
    } else {
      alert(res.error);
    }
  };

  const handleMarkVisitCompleted = async (record) => {
    if (!window.confirm(`Confirm that on-site/virtual inspection for ${record.id} (${record.institutionName}) has concluded?`)) {
      return;
    }

    const res = await markEVCCompleted({
      evcId: record.id,
      completionNotes: 'Inspection concluded successfully. Committee drafting report.',
      actorName: user?.name,
      actorRole: role,
    });

    if (res.success) {
      alert(`Visit marked completed for ${record.id}. Report submission clock started.`);
    } else {
      alert(res.error);
    }
  };

  const handleRevertToScheduled = async (record) => {
    if (!window.confirm(`Revert visit ${record.id} status back to Scheduled? Use this if it was marked completed prematurely.`)) {
      return;
    }

    const res = await rescheduleEVC({
      evcId: record.id,
      actorName: user?.name,
      actorRole: role,
    });

    if (res.success) {
      alert(`Visit ${record.id} successfully reverted to Scheduled.`);
      setIsDetailsModalOpen(false);
    } else {
      alert(res.error);
    }
  };

  const handleOpenReportModal = (record) => {
    setSelectedEVC(record);
    setReportNotes(record.reportNotes || '');
    setReportOutcome(record.reportOutcome || 'Recommended Approval');
    setIsReportModalOpen(true);
  };

  const handleSaveReport = async (e) => {
    e.preventDefault();
    const res = await submitEVCReport({
      evcId: selectedEVC.id,
      reportNotes,
      reportOutcome,
      actorName: user?.name,
      actorRole: role,
    });

    if (res.success) {
      setIsReportModalOpen(false);
      alert(`Official report for ${selectedEVC.id} successfully accepted & archived.`);
    } else {
      alert(res.error);
    }
  };

  const handleViewDetails = (record) => {
    setSelectedEVC(record);
    setIsDetailsModalOpen(true);
  };

  return (
    <div>
      {/* Page Heading */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '16px', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <div style={{ fontSize: '10px', textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--color-teal)', fontWeight: '800' }}>
            OPERATIONS WORKBENCH · INSPECTIONS &amp; VISITS
          </div>
          <h1 style={{ fontSize: '22px', fontWeight: '800', color: 'var(--color-navy)', margin: '4px 0 2px' }}>
            EVC Operations Workbench
          </h1>
          <p style={{ color: 'var(--color-text-muted)', fontSize: 'var(--font-size-sm)' }}>
            Orchestrate Expert Visit Committee (EVC) scheduling, panel composition, inspection tracking, and formal report filings.
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span className="badge badge-blue">
            Role: {role}
          </span>
        </div>
      </div>

      {/* 5 KPI Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '14px', marginBottom: '18px' }}>
        <KPICard
          title="Total EVC Cases"
          value={kpiTotal}
          subtitle="All inspection records"
          iconName="FileText"
          tint="blue"
          onClick={() => setActiveTab('EVC Queue')}
        />
        <KPICard
          title="Pending Scheduling"
          value={kpiPendingScheduling}
          subtitle="Awaiting date & roster"
          iconName="Clock"
          tint="amber"
          onClick={() => setActiveTab('EVC Queue')}
        />
        <KPICard
          title="Upcoming Visits"
          value={kpiUpcomingVisits}
          subtitle="Dates confirmed with colleges"
          iconName="Calendar"
          tint="teal"
          onClick={() => setActiveTab('Scheduled Visits')}
        />
        <KPICard
          title="Completed Visits"
          value={kpiCompletedVisits}
          subtitle="Physical audit concluded"
          iconName="CheckCircle2"
          tint="green"
          onClick={() => setActiveTab('Completed Visits')}
        />
        <KPICard
          title="Reports Pending"
          value={kpiReportsPending}
          subtitle="Awaiting committee filing"
          iconName="AlertTriangle"
          tint="red"
          onClick={() => setActiveTab('Reports Pending')}
        />
      </div>

      {/* Toolbar & Search */}
      <div className="panel" style={{ marginBottom: '16px', padding: '12px 16px', background: '#FCFDFE', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
        <div style={{ position: 'relative', width: '320px' }}>
          <input
            type="text"
            className="form-input"
            placeholder="Search by EVC ID, App ID, College..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ paddingLeft: '32px' }}
          />
          <Search size={15} style={{ position: 'absolute', left: '10px', top: '10px', color: '#899BAA' }} />
        </div>

        <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
          {isAuthorizedToModify ? (
            <span style={{ color: 'var(--color-success)', fontWeight: '700' }}>
              ✓ Authorized to Schedule &amp; Update EVC Records
            </span>
          ) : (
            <span>Read-Only Scope · Institutional Review Mode</span>
          )}
        </div>
      </div>

      {/* Main Table Panel with Tabs */}
      <div className="panel" style={{ padding: 0 }}>
        <TabsNav tabs={tabs} activeTab={activeTab} onTabChange={setActiveTab} />

        <div className="panel-body" style={{ padding: 0 }}>
          {tabFilteredEVC.length === 0 ? (
            <EmptyState message="No EVC cases match the active tab filter." />
          ) : (
            <div className="table-container" style={{ border: 'none' }}>
              <table className="gov-table">
                <thead>
                  <tr>
                    <th>EVC ID</th>
                    <th>Application ID</th>
                    <th>Institution</th>
                    <th>Visit Date</th>
                    <th>Coordinator</th>
                    <th>Status</th>
                    <th>Deadline</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {tabFilteredEVC.map((row) => (
                    <tr key={row.id}>
                      <td>
                        <strong>{row.id}</strong>
                      </td>
                      <td>
                        <Link to={`/applications/${row.applicationId}`} style={{ color: 'var(--color-blue)', fontWeight: '600' }}>
                          {row.applicationId}
                        </Link>
                      </td>
                      <td>
                        <strong>{row.institutionName}</strong>
                        <div style={{ fontSize: '10px', color: 'var(--color-text-muted)' }}>
                          {row.inspectionType}
                        </div>
                      </td>
                      <td>
                        <span style={{ fontWeight: row.visitDate ? '700' : 'normal' }}>
                          {row.visitDate || 'Not Scheduled'}
                        </span>
                      </td>
                      <td style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
                        {row.coordinator}
                      </td>
                      <td>
                        <StatusBadge status={row.status} />
                      </td>
                      <td>
                        <span style={{ fontWeight: '600', color: row.urgency === 'Overdue' ? 'var(--color-critical)' : 'inherit' }}>
                          {row.deadline}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: '6px' }}>
                          <button
                            onClick={() => handleViewDetails(row)}
                            className="btn btn-outline btn-sm"
                            title="View Full EVC Dossier"
                          >
                            <Eye size={12} /> View
                          </button>

                          {isAuthorizedToModify && isPendingScheduling(row) && (
                            <button
                              onClick={() => handleOpenScheduleModal(row)}
                              className="btn btn-primary btn-sm"
                              title="Schedule Date and Panel"
                            >
                              <UserPlus size={12} /> Schedule
                            </button>
                          )}

                          {isAuthorizedToModify && isScheduledVisit(row) && (
                            <button
                              onClick={() => handleMarkVisitCompleted(row)}
                              className="btn btn-outline btn-sm"
                              style={{ borderColor: 'var(--color-success)', color: 'var(--color-success)' }}
                              title="Mark Visit Concluded"
                            >
                              <Check size={12} /> Mark Completed
                            </button>
                          )}

                          {isAuthorizedToModify && isReportPendingVisit(row) && (
                            <button
                              onClick={() => handleOpenReportModal(row)}
                              className="btn btn-primary btn-sm"
                              title="Submit Inspection Report"
                            >
                              <Upload size={12} /> Submit Report
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* MODAL 1: SCHEDULE VISIT & ASSIGN COORDINATOR */}
      <Modal
        isOpen={isScheduleModalOpen}
        onClose={() => setIsScheduleModalOpen(false)}
        title="Schedule Inspection Visit &amp; Assign Panel"
        subtitle={selectedEVC ? `${selectedEVC.id} · ${selectedEVC.institutionName}` : ''}
        footer={
          <>
            <button onClick={() => setIsScheduleModalOpen(false)} className="btn btn-outline btn-sm">
              Cancel
            </button>
            <button onClick={handleSaveSchedule} className="btn btn-primary btn-sm">
              <Calendar size={13} /> Confirm Schedule &amp; Assign
            </button>
          </>
        }
      >
        <form onSubmit={handleSaveSchedule}>
          <div className="callout callout-blue" style={{ fontSize: '11px', marginBottom: '12px' }}>
            Confirmed schedules are communicated officially to the institution and empaneled experts under APH visit procedures.
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="evcDateInput">
              Confirmed Inspection Date *
            </label>
            <input
              id="evcDateInput"
              type="date"
              className="form-input"
              value={scheduleDate}
              onChange={(e) => setScheduleDate(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="evcCoordInput">
              Assigned Operations Coordinator
            </label>
            <input
              id="evcCoordInput"
              type="text"
              className="form-input"
              value={scheduleCoordinator}
              onChange={(e) => setScheduleCoordinator(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="evcTypeSelect">
              Inspection Format
            </label>
            <select
              id="evcTypeSelect"
              className="form-select"
              value={scheduleType}
              onChange={(e) => setScheduleType(e.target.value)}
            >
              <option value="Physical On-Site Verification">Physical On-Site Verification</option>
              <option value="Hybrid Video & Physical Audit">Hybrid Video &amp; Physical Audit</option>
              <option value="Comprehensive APH Assessment">Comprehensive APH Assessment</option>
            </select>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="evcExpertsInput">
              Empaneled Expert Committee Members (One per line)
            </label>
            <textarea
              id="evcExpertsInput"
              className="form-textarea"
              rows={3}
              value={scheduleExperts}
              onChange={(e) => setScheduleExperts(e.target.value)}
              placeholder="e.g. Prof. K. Raman (IIT Roorkee - Chair)&#10;Dr. Sunita Rao (NIT Kurukshetra)"
            />
          </div>
        </form>
      </Modal>

      {/* MODAL 2: SUBMIT INSPECTION REPORT */}
      <Modal
        isOpen={isReportModalOpen}
        onClose={() => setIsReportModalOpen(false)}
        title="Submit Official EVC Inspection Report"
        subtitle={selectedEVC ? `${selectedEVC.id} · ${selectedEVC.institutionName}` : ''}
        footer={
          <>
            <button onClick={() => setIsReportModalOpen(false)} className="btn btn-outline btn-sm">
              Cancel
            </button>
            <button onClick={handleSaveReport} className="btn btn-primary btn-sm">
              <Upload size={13} /> Accept &amp; File Report
            </button>
          </>
        }
      >
        <form onSubmit={handleSaveReport}>
          <div className="callout callout-blue" style={{ fontSize: '11px', marginBottom: '12px' }}>
            Uploading the official report transitions the EVC stage to <strong>Complete for stage</strong> and forwards findings to the Regional Committee Desk.
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="reportOutcomeSelect">
              Committee Recommendation Outcome *
            </label>
            <select
              id="reportOutcomeSelect"
              className="form-select"
              value={reportOutcome}
              onChange={(e) => setReportOutcome(e.target.value)}
            >
              <option value="Recommended Approval">Recommended Approval (Meets APH Criteria)</option>
              <option value="Recommended with Conditions">Recommended with Conditions / Undertakings</option>
              <option value="Not Recommended">Not Recommended (Critical Deficiencies Noted)</option>
            </select>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="reportNotesInput">
              Committee Findings &amp; Summary Notes *
            </label>
            <textarea
              id="reportNotesInput"
              className="form-textarea"
              rows={4}
              value={reportNotes}
              onChange={(e) => setReportNotes(e.target.value)}
              placeholder="Summary of on-site verification, physical infrastructure audit, and committee remarks..."
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Attached Report File</label>
            <div style={{ padding: '8px 12px', background: '#F8FAFC', border: '1px dashed var(--color-border)', borderRadius: 'var(--radius-sm)', fontSize: '11px' }}>
              📄 {selectedEVC?.id}_official_signed_inspection_report.pdf (Verified digital signature)
            </div>
          </div>
        </form>
      </Modal>

      {/* MODAL 3: VIEW EVC DETAILS */}
      <Modal
        isOpen={isDetailsModalOpen}
        onClose={() => setIsDetailsModalOpen(false)}
        title={`EVC Inspection Record · ${selectedEVC?.id}`}
        subtitle={selectedEVC?.institutionName}
        footer={
          <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%', alignItems: 'center' }}>
            {isAuthorizedToModify && isCompletedVisit(selectedEVC) && (
              <button
                onClick={() => handleRevertToScheduled(selectedEVC)}
                className="btn btn-outline btn-sm"
                style={{ borderColor: 'var(--color-warning)', color: 'var(--color-warning)' }}
                title="Revert status to Scheduled if visit was marked completed prematurely"
              >
                Revert to Scheduled (Correction)
              </button>
            )}
            <button onClick={() => setIsDetailsModalOpen(false)} className="btn btn-primary btn-sm" style={{ marginLeft: 'auto' }}>
              Close
            </button>
          </div>
        }
      >
        {selectedEVC && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', fontSize: '12px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '6px', borderBottom: '1px solid var(--color-border-subtle)' }}>
              <span style={{ color: 'var(--color-text-muted)' }}>Application Reference</span>
              <Link to={`/applications/${selectedEVC.applicationId}`} style={{ color: 'var(--color-blue)', fontWeight: '700' }}>
                {selectedEVC.applicationId} ↗
              </Link>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '6px', borderBottom: '1px solid var(--color-border-subtle)' }}>
              <span style={{ color: 'var(--color-text-muted)' }}>Current EVC Status</span>
              <StatusBadge status={selectedEVC.status} />
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '6px', borderBottom: '1px solid var(--color-border-subtle)' }}>
              <span style={{ color: 'var(--color-text-muted)' }}>Territorial Region</span>
              <strong>{selectedEVC.region}</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '6px', borderBottom: '1px solid var(--color-border-subtle)' }}>
              <span style={{ color: 'var(--color-text-muted)' }}>Inspection Format</span>
              <strong>{selectedEVC.inspectionType}</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '6px', borderBottom: '1px solid var(--color-border-subtle)' }}>
              <span style={{ color: 'var(--color-text-muted)' }}>Confirmed Visit Date</span>
              <strong>{selectedEVC.visitDate || 'Pending scheduling'}</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '6px', borderBottom: '1px solid var(--color-border-subtle)' }}>
              <span style={{ color: 'var(--color-text-muted)' }}>Operations Coordinator</span>
              <strong>{selectedEVC.coordinator}</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '6px', borderBottom: '1px solid var(--color-border-subtle)' }}>
              <span style={{ color: 'var(--color-text-muted)' }}>Report Filing Status</span>
              <strong>{selectedEVC.reportStatus || 'N/A'}</strong>
            </div>

            <div style={{ marginTop: '8px' }}>
              <span style={{ fontSize: '11px', fontWeight: '700', textTransform: 'uppercase', color: 'var(--color-text-muted)', display: 'block', marginBottom: '4px' }}>
                Empaneled Expert Committee:
              </span>
              <ul style={{ margin: 0, paddingLeft: '18px', color: 'var(--color-text)', lineHeight: 1.5 }}>
                {selectedEVC.experts && selectedEVC.experts.length > 0 ? (
                  selectedEVC.experts.map((exp, idx) => <li key={idx}>{exp}</li>)
                ) : (
                  <li>No experts assigned yet.</li>
                )}
              </ul>
            </div>

            {selectedEVC.reportNotes && (
              <div style={{ marginTop: '8px', padding: '10px 12px', background: '#F8FAFC', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-sm)' }}>
                <span style={{ fontSize: '11px', fontWeight: '700', color: 'var(--color-navy)', display: 'block', marginBottom: '2px' }}>
                  Archived Report Notes:
                </span>
                <p style={{ margin: 0, fontSize: '11px', color: 'var(--color-text)' }}>
                  {selectedEVC.reportNotes}
                </p>
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
};
