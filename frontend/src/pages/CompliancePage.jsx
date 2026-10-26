// SIH 26262 - Compliance & Evidence Review Module (/compliance)

import React, { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useData } from '../context/DataContext';
import { ROLES } from '../constants/roles';
import { KPICard } from '../components/common/KPICard';
import { TabsNav } from '../components/common/TabsNav';
import { StatusBadge } from '../components/common/StatusBadge';
import { EmptyState } from '../components/common/EmptyState';
import { Modal } from '../components/common/Modal';
import {
  CheckSquare,
  Clock,
  AlertTriangle,
  CheckCircle2,
  FileText,
  Upload,
  Eye,
  Search,
  MessageSquare,
  FileCheck,
} from 'lucide-react';

export const CompliancePage = () => {
  const { user, role } = useAuth();
  const {
    compliance,
    submitComplianceEvidence,
    markComplianceUnderVerification,
    verifyComplianceEvidence,
  } = useData();

  const isInstitutionRep = role === ROLES.INSTITUTION_REP;
  const isComplianceReviewer = [
    ROLES.COMPLIANCE_REVIEWER,
    ROLES.REGIONAL_OFFICER,
    ROLES.SENIOR_MANAGEMENT,
    ROLES.ADMINISTRATOR,
  ].includes(role);

  // Tabs state
  const [activeTab, setActiveTab] = useState('All');
  const [searchQuery, setSearchQuery] = useState('');

  // Modals state
  const [selectedObligation, setSelectedObligation] = useState(null);

  // Submit Evidence Modal (Institution Rep)
  const [isSubmitModalOpen, setIsSubmitModalOpen] = useState(false);
  const [evidenceFileName, setEvidenceFileName] = useState('');
  const [evidenceNotes, setEvidenceNotes] = useState('');

  // Review & Verify Modal (Compliance Reviewer)
  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
  const [verificationOutcome, setVerificationOutcome] = useState('Verified compliant');
  const [verificationRemarks, setVerificationRemarks] = useState('');

  // 1. Role Scoping
  const baseCompliance = useMemo(() => {
    if (isInstitutionRep) {
      if (user?.institutionId) {
        return compliance.filter((c) => c.institutionId === user.institutionId);
      }
      return compliance;
    }
    return compliance;
  }, [compliance, isInstitutionRep, user]);

  // 2. Search filtering
  const searchedCompliance = useMemo(() => {
    if (!searchQuery.trim()) return baseCompliance;
    const q = searchQuery.toLowerCase();
    return baseCompliance.filter(
      (c) =>
        c.id.toLowerCase().includes(q) ||
        c.institutionName.toLowerCase().includes(q) ||
        c.requirement.toLowerCase().includes(q) ||
        c.category.toLowerCase().includes(q)
    );
  }, [baseCompliance, searchQuery]);

  // 3. Tab Filtering
  const tabFilteredCompliance = useMemo(() => {
    switch (activeTab) {
      case 'Due Soon':
        return searchedCompliance.filter((c) => c.urgency === 'Due soon');
      case 'Overdue':
        return searchedCompliance.filter((c) => c.urgency === 'Overdue');
      case 'Submitted':
        return searchedCompliance.filter((c) => c.status === 'Evidence submitted');
      case 'Under Verification':
        return searchedCompliance.filter((c) => c.status === 'Verification pending');
      case 'Clarification Required':
        return searchedCompliance.filter((c) => c.status === 'Clarification requested');
      case 'Completed':
        return searchedCompliance.filter((c) => c.status === 'Verified compliant');
      case 'All':
      default:
        return searchedCompliance;
    }
  }, [searchedCompliance, activeTab]);

  // KPI Calculations
  const kpiTotal = baseCompliance.length;
  const kpiDueSoon = baseCompliance.filter((c) => c.urgency === 'Due soon').length;
  const kpiOverdue = baseCompliance.filter((c) => c.urgency === 'Overdue').length;
  const kpiSubmitted = baseCompliance.filter((c) => c.status === 'Evidence submitted').length;
  const kpiUnderVerification = baseCompliance.filter((c) => c.status === 'Verification pending').length;
  const kpiCompleted = baseCompliance.filter((c) => c.status === 'Verified compliant').length;

  const tabs = [
    { key: 'All', label: 'All Obligations', count: kpiTotal },
    { key: 'Due Soon', label: 'Due Soon', count: kpiDueSoon },
    { key: 'Overdue', label: 'Overdue', count: kpiOverdue },
    { key: 'Submitted', label: 'Submitted', count: kpiSubmitted },
    { key: 'Under Verification', label: 'Under Verification', count: kpiUnderVerification },
    { key: 'Clarification Required', label: 'Clarification Required', count: baseCompliance.filter((c) => c.status === 'Clarification requested').length },
    { key: 'Completed', label: 'Completed', count: kpiCompleted },
  ];

  // Action: Open Submit Evidence Modal
  const handleOpenSubmitModal = (obligation) => {
    setSelectedObligation(obligation);
    setEvidenceFileName(`${obligation.id}_affidavit_compliance.pdf`);
    setEvidenceNotes('');
    setIsSubmitModalOpen(true);
  };

  // Action: Confirm Submit Evidence
  const handleConfirmSubmitEvidence = async (e) => {
    e.preventDefault();
    if (!evidenceNotes.trim()) {
      alert('Please provide an explanatory remark describing the uploaded proof.');
      return;
    }

    const res = await submitComplianceEvidence({
      obligationId: selectedObligation.id,
      evidenceFile: evidenceFileName,
      notes: evidenceNotes,
      actorName: user?.name,
      actorRole: role,
    });

    if (res.success) {
      setIsSubmitModalOpen(false);
      alert(
        `Evidence for condition ${selectedObligation.id} successfully submitted!\n\nNote: The condition is now marked "Evidence submitted" and will be evaluated separately by the assigned Compliance Reviewer.`
      );
    } else {
      alert(res.error);
    }
  };

  // Action: Open Review & Verify Modal
  const handleOpenReviewModal = (obligation) => {
    setSelectedObligation(obligation);
    setVerificationOutcome('Verified compliant');
    setVerificationRemarks('');
    setIsReviewModalOpen(true);
  };

  // Action: Initiate Verification
  const handleMarkUnderVerification = async (obligation) => {
    const res = await markComplianceUnderVerification({
      obligationId: obligation.id,
      actorName: user?.name,
      actorRole: role,
    });

    if (res.success) {
      alert(`Obligation ${obligation.id} marked as "Under Verification". Active documentary scrutiny initiated.`);
    } else {
      alert(res.error);
    }
  };

  // Action: Confirm Outcome
  const handleConfirmVerification = async (e) => {
    e.preventDefault();
    const res = await verifyComplianceEvidence({
      obligationId: selectedObligation.id,
      outcome: verificationOutcome,
      remarks: verificationRemarks,
      actorName: user?.name,
      actorRole: role,
    });

    if (res.success) {
      setIsReviewModalOpen(false);
      alert(`Official verification recorded as "${verificationOutcome}" for condition ${selectedObligation.id}.`);
    } else {
      alert(res.error);
    }
  };

  return (
    <div>
      {/* Page Heading */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '16px', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <div style={{ fontSize: '10px', textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--color-teal)', fontWeight: '800' }}>
            STATUTORY MONITORING · POST-APPROVAL UNDERTAKINGS
          </div>
          <h1 style={{ fontSize: '22px', fontWeight: '800', color: 'var(--color-navy)', margin: '4px 0 2px' }}>
            Compliance &amp; Evidence Review Workbench
          </h1>
          <p style={{ color: 'var(--color-text-muted)', fontSize: 'var(--font-size-sm)' }}>
            Track approval conditions, institutional undertakings, documentary proof submissions, and auditable verification decisions.
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span className="badge badge-blue">
            Scope: {isInstitutionRep ? user?.institutionName : role}
          </span>
        </div>
      </div>

      {/* 6 KPI Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: '14px', marginBottom: '18px' }}>
        <KPICard
          title="Total Obligations"
          value={kpiTotal}
          subtitle="Monitored conditions"
          iconName="FileText"
          tint="blue"
          onClick={() => setActiveTab('All')}
        />
        <KPICard
          title="Due Soon"
          value={kpiDueSoon}
          subtitle="Approaching deadline"
          iconName="Clock"
          tint="amber"
          onClick={() => setActiveTab('Due Soon')}
        />
        <KPICard
          title="Overdue"
          value={kpiOverdue}
          subtitle="SLA breached"
          iconName="AlertTriangle"
          tint="red"
          onClick={() => setActiveTab('Overdue')}
        />
        <KPICard
          title="Evidence Submitted"
          value={kpiSubmitted}
          subtitle="Awaiting desk check"
          iconName="Upload"
          tint="teal"
          onClick={() => setActiveTab('Submitted')}
        />
        <KPICard
          title="Under Verification"
          value={kpiUnderVerification}
          subtitle="Active scrutiny"
          iconName="Eye"
          tint="purple"
          onClick={() => setActiveTab('Under Verification')}
        />
        <KPICard
          title="Completed"
          value={kpiCompleted}
          subtitle="Verified compliant"
          iconName="CheckCircle2"
          tint="green"
          onClick={() => setActiveTab('Completed')}
        />
      </div>

      {/* Search & Principle Banner */}
      <div className="panel" style={{ marginBottom: '16px', padding: '12px 16px', background: '#FCFDFE', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
        <div style={{ position: 'relative', width: '320px' }}>
          <input
            type="text"
            className="form-input"
            placeholder="Search by ID, college, condition..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ paddingLeft: '32px' }}
          />
          <Search size={15} style={{ position: 'absolute', left: '10px', top: '10px', color: '#899BAA' }} />
        </div>

        <div style={{ fontSize: '11px', color: 'var(--color-navy)', background: 'var(--color-blue-soft)', padding: '6px 12px', borderRadius: 'var(--radius-sm)', border: '1px solid #C5DCF2' }}>
          <strong>Statutory Separation:</strong> Evidence Submission ≠ Compliance. Submissions must undergo verified human scrutiny.
        </div>
      </div>

      {/* Main Table with Tabs */}
      <div className="panel" style={{ padding: 0 }}>
        <TabsNav tabs={tabs} activeTab={activeTab} onTabChange={setActiveTab} />

        <div className="panel-body" style={{ padding: 0 }}>
          {tabFilteredCompliance.length === 0 ? (
            <EmptyState message="No compliance obligations match the active tab filter." />
          ) : (
            <div className="table-container" style={{ border: 'none' }}>
              <table className="gov-table">
                <thead>
                  <tr>
                    <th>Obligation ID</th>
                    <th>Institution</th>
                    <th>Compliance Condition</th>
                    <th>Due Date</th>
                    <th>Evidence Status</th>
                    <th>Assigned Reviewer</th>
                    <th>Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {tabFilteredCompliance.map((row) => (
                    <tr key={row.id}>
                      <td>
                        <strong>{row.id}</strong>
                      </td>
                      <td>
                        <strong>{row.institutionName}</strong>
                        <div style={{ fontSize: '10px', color: 'var(--color-text-muted)' }}>
                          Category: {row.category}
                        </div>
                      </td>
                      <td style={{ maxWidth: '240px', whiteSpace: 'normal', fontSize: '11px', lineHeight: 1.4 }}>
                        {row.requirement}
                      </td>
                      <td>
                        <span style={{ fontWeight: '700', color: row.urgency === 'Overdue' ? 'var(--color-critical)' : 'inherit' }}>
                          {row.dueDate}
                        </span>
                      </td>
                      <td>
                        {row.evidenceFile ? (
                          <div>
                            <span style={{ color: 'var(--color-blue)', fontSize: '11px', textDecoration: 'underline' }}>
                              {row.evidenceFile}
                            </span>
                            <div style={{ fontSize: '10px', color: 'var(--color-text-muted)' }}>
                              Uploaded: {row.submittedDate || 'Recent'}
                            </div>
                          </div>
                        ) : (
                          <span style={{ color: 'var(--color-text-muted)', fontSize: '11px' }}>
                            Not submitted
                          </span>
                        )}
                      </td>
                      <td style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
                        {row.reviewer}
                      </td>
                      <td>
                        <StatusBadge status={row.status} />
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: '6px' }}>
                          {/* Institution Actions: Submit / Resubmit */}
                          {isInstitutionRep && (
                            <button
                              onClick={() => handleOpenSubmitModal(row)}
                              className="btn btn-primary btn-sm"
                              title="Upload Evidence Document"
                            >
                              <Upload size={12} /> {row.evidenceFile ? 'Resubmit Proof' : 'Submit Evidence'}
                            </button>
                          )}

                          {/* Reviewer Actions: Mark Under Verification / Verify */}
                          {isComplianceReviewer && row.status === 'Evidence submitted' && (
                            <button
                              onClick={() => handleMarkUnderVerification(row)}
                              className="btn btn-outline btn-sm"
                              title="Start Verification Review"
                            >
                              <Eye size={12} /> Initiate Review
                            </button>
                          )}

                          {isComplianceReviewer && (row.status === 'Verification pending' || row.status === 'Evidence submitted') && (
                            <button
                              onClick={() => handleOpenReviewModal(row)}
                              className="btn btn-primary btn-sm"
                              title="Evaluate and Record Verification Decision"
                            >
                              <CheckCircle2 size={12} /> Evaluate Evidence
                            </button>
                          )}

                          {isComplianceReviewer && row.status === 'Clarification requested' && (
                            <button
                              onClick={() => handleOpenReviewModal(row)}
                              className="btn btn-outline btn-sm"
                              title="Update Review Remarks"
                            >
                              <MessageSquare size={12} /> Update Query
                            </button>
                          )}

                          {row.status === 'Verified compliant' && (
                            <span style={{ fontSize: '11px', color: 'var(--color-success)', fontWeight: '700' }}>
                              ✓ Verified
                            </span>
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

      {/* MODAL 1: SUBMIT / RESUBMIT EVIDENCE (INSTITUTION) */}
      <Modal
        isOpen={isSubmitModalOpen}
        onClose={() => setIsSubmitModalOpen(false)}
        title="Submit Compliance Evidence Dossier"
        subtitle={selectedObligation ? `${selectedObligation.id} · ${selectedObligation.institutionName}` : ''}
        footer={
          <>
            <button onClick={() => setIsSubmitModalOpen(false)} className="btn btn-outline btn-sm">
              Cancel
            </button>
            <button onClick={handleConfirmSubmitEvidence} className="btn btn-primary btn-sm">
              <Upload size={13} /> Submit Proof to Bureau
            </button>
          </>
        }
      >
        <form onSubmit={handleConfirmSubmitEvidence}>
          <div className="callout callout-amber" style={{ fontSize: '11px', marginBottom: '12px' }}>
            <strong>Note on Regulatory Procedure:</strong> Submitting evidence records your document as <em>Evidence submitted</em>. Formal compliance is only achieved once the Compliance Reviewer officially verifies the proof against APH criteria.
          </div>

          {selectedObligation && (
            <div style={{ marginBottom: '14px', padding: '10px 12px', background: '#F8FAFC', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-sm)', fontSize: '12px' }}>
              <div style={{ fontWeight: '700', color: 'var(--color-navy)', marginBottom: '2px' }}>
                Condition Requirement:
              </div>
              <p style={{ margin: 0, color: 'var(--color-text)', lineHeight: 1.4 }}>
                {selectedObligation.requirement}
              </p>
            </div>
          )}

          <div className="form-group">
            <label className="form-label" htmlFor="evidenceFileInput">
              Document Attachment (Simulated PDF)
            </label>
            <input
              id="evidenceFileInput"
              type="text"
              className="form-input"
              value={evidenceFileName}
              onChange={(e) => setEvidenceFileName(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="evidenceNotesInput">
              Institutional Remarks &amp; Certification Statement *
            </label>
            <textarea
              id="evidenceNotesInput"
              className="form-textarea"
              rows={4}
              placeholder="State the specifics of the evidence provided (e.g. Attached certified bills, engineer inspection sign-off, or bank salary statement)..."
              value={evidenceNotes}
              onChange={(e) => setEvidenceNotes(e.target.value)}
              required
            />
          </div>
        </form>
      </Modal>

      {/* MODAL 2: EVALUATE & VERIFY EVIDENCE (REVIEWER) */}
      <Modal
        isOpen={isReviewModalOpen}
        onClose={() => setIsReviewModalOpen(false)}
        title="Official Compliance Verification Decision"
        subtitle={selectedObligation ? `${selectedObligation.id} · ${selectedObligation.institutionName}` : ''}
        footer={
          <>
            <button onClick={() => setIsReviewModalOpen(false)} className="btn btn-outline btn-sm">
              Cancel
            </button>
            <button onClick={handleConfirmVerification} className="btn btn-primary btn-sm">
              <CheckCircle2 size={13} /> Record Decision
            </button>
          </>
        }
      >
        <form onSubmit={handleConfirmVerification}>
          <div className="callout callout-blue" style={{ fontSize: '11px', marginBottom: '12px' }}>
            Review the submitted proof against the stipulated approval condition. Only an authorized Compliance Reviewer may record this decision.
          </div>

          {selectedObligation && (
            <div style={{ marginBottom: '14px', padding: '10px 12px', background: '#F8FAFC', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-sm)', fontSize: '12px' }}>
              <div style={{ fontWeight: '700', color: 'var(--color-navy)', marginBottom: '2px' }}>
                Requirement to Satisfy:
              </div>
              <p style={{ margin: 0, color: 'var(--color-text)', lineHeight: 1.4 }}>
                {selectedObligation.requirement}
              </p>
              <div style={{ marginTop: '8px', fontSize: '11px' }}>
                Attached File: <strong style={{ color: 'var(--color-blue)' }}>{selectedObligation.evidenceFile || 'No file uploaded'}</strong>
              </div>
              {selectedObligation.remarks && (
                <div style={{ marginTop: '6px', fontSize: '11px', color: 'var(--color-text-muted)' }}>
                  College Notes: {selectedObligation.remarks}
                </div>
              )}
            </div>
          )}

          <div className="form-group">
            <label className="form-label" htmlFor="verificationOutcomeSelect">
              Official Verification Outcome *
            </label>
            <select
              id="verificationOutcomeSelect"
              className="form-select"
              value={verificationOutcome}
              onChange={(e) => setVerificationOutcome(e.target.value)}
            >
              <option value="Verified compliant">Verified Compliant (Condition Fulfilled)</option>
              <option value="Clarification requested">Clarification Requested (Additional Information Needed)</option>
              <option value="Rejected">Rejected (Inadequate or Non-Compliant Proof)</option>
            </select>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="verificationRemarksInput">
              Reviewer Official Remarks &amp; Statutory Justification *
            </label>
            <textarea
              id="verificationRemarksInput"
              className="form-textarea"
              rows={4}
              placeholder="State the verification findings, regulatory references, or specific deficiency clarification required..."
              value={verificationRemarks}
              onChange={(e) => setVerificationRemarks(e.target.value)}
              required
            />
          </div>
        </form>
      </Modal>
    </div>
  );
};
