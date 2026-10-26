// SIH 26262 - Application Details & Workflow Module (/applications/:id)

import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useData } from '../context/DataContext';
import { ROLES } from '../constants/roles';
import { StatusBadge } from '../components/common/StatusBadge';
import { TabsNav } from '../components/common/TabsNav';
import { Modal } from '../components/common/Modal';
import { DataTable } from '../components/common/DataTable';
import {
  ArrowLeft,
  CheckCircle2,
  AlertTriangle,
  Clock,
  FileText,
  CornerUpLeft,
  Upload,
  UserCheck,
  Send,
  Building,
  Calendar,
  Shield,
  History,
  FileCheck,
  RefreshCw,
  Cpu,
  Check,
  Zap,
} from 'lucide-react';
import { apiClient } from '../services/apiClient';

export const ApplicationDetailPage = () => {
  const { id } = useParams();
  const { user, role } = useAuth();
  const { getApplicationById, performWorkflowAction, evc, compliance, activity } = useData();

  const app = getApplicationById(id);

  // Active Tab State
  const [activeTab, setActiveTab] = useState('Overview');

  // Incremental Validation State
  const [validationReport, setValidationReport] = useState(null);
  const [validationLoading, setValidationLoading] = useState(false);

  const fetchValidation = async (modifiedFields = {}) => {
    if (!app) return;
    try {
      setValidationLoading(true);
      const targetId = app.backendId || app.applicationReference || app.id;
      const res = await apiClient(`/validation/${encodeURIComponent(targetId)}/evaluate`, {
        method: 'POST',
        body: JSON.stringify({
          modifiedFields,
          forceFullEvaluation: false,
          actorName: user?.name,
          actorRole: role,
        }),
      });
      setValidationReport(res);
    } catch (err) {
      console.error('Failed to run incremental validation:', err);
    } finally {
      setValidationLoading(false);
    }
  };

  useEffect(() => {
    if (app && activeTab === 'Scrutiny' && !validationReport) {
      fetchValidation({});
    }
  }, [app, activeTab]);

  // Interactive Scrutiny Checklist state
  const isApproved = app?.status === 'Completed' || app?.currentStage === 'Completed' || app?.currentStage === 'EVC scheduling';
  const [checklist, setChecklist] = useState([
    { id: 'CHK-LAND-01', label: 'Mandatory land title deed & non-encumbrance certificate verified', checked: isApproved },
    { id: 'CHK-FACULTY-02', label: 'Faculty cadre ratio meets APH Table 2.1 norms (1:15/1:20)', checked: isApproved && app?.status !== 'Returned' },
    { id: 'CHK-FIRE-03', label: 'Municipal building plan & fire safety NOC endorsement certified', checked: isApproved && app?.status !== 'Returned' },
    { id: 'CHK-LAB-04', label: 'Laboratory and computing facilities verified compliant with AICTE norms', checked: isApproved },
  ]);

  // Sync checklist from backend application if available
  useEffect(() => {
    if (app?.checklist && app.checklist.length > 0) {
      setChecklist(
        app.checklist.map((c) => ({
          id: c.itemCode,
          label: c.label,
          checked: Boolean(c.isVerified),
          requirementClause: c.requirementClause,
        }))
      );
    }
  }, [app?.checklist]);

  const [officerNotes, setOfficerNotes] = useState(app?.observations || '');

  // Workflow Action Modals state
  const [isReturnModalOpen, setIsReturnModalOpen] = useState(false);
  const [returnReason, setReturnReason] = useState('');
  const [returnRequirementRef, setReturnRequirementRef] = useState('APH Table 2.1 Norms');
  const [returnDeficiencies, setReturnDeficiencies] = useState('');

  const [isResubmitModalOpen, setIsResubmitModalOpen] = useState(false);
  const [resubmitNotes, setResubmitNotes] = useState('');
  const [resubmitFileName, setResubmitFileName] = useState('');

  const [isResolveModalOpen, setIsResolveModalOpen] = useState(false);
  const [selectedDeficiency, setSelectedDeficiency] = useState(null);
  const [resolveRemarks, setResolveRemarks] = useState('');

  const [isCompleteModalOpen, setIsCompleteModalOpen] = useState(false);
  const [selectedNextStage, setSelectedNextStage] = useState('EVC scheduling');
  const [completeRemarks, setCompleteRemarks] = useState('');

  const [actionFeedback, setActionFeedback] = useState(null);

  if (!app) {
    return (
      <div style={{ maxWidth: '800px', margin: '40px auto', textAlign: 'center' }}>
        <div className="panel">
          <div className="panel-body" style={{ padding: '40px' }}>
            <AlertTriangle size={32} color="var(--color-critical)" style={{ marginBottom: '12px' }} />
            <h2 style={{ fontSize: '18px', color: 'var(--color-navy)', marginBottom: '8px' }}>
              Application Dossier Not Found
            </h2>
            <p style={{ color: 'var(--color-text-muted)', marginBottom: '16px' }}>
              No application with identifier <strong>{id}</strong> could be found in the current store.
            </p>
            <Link to="/applications" className="btn btn-primary">
              <ArrowLeft size={14} /> Back to Application Explorer
            </Link>
          </div>
        </div>
      </div>
    );
  }

  // Linked EVC and Compliance records
  const linkedEVC = evc.find((e) => e.applicationId === app.id);
  const linkedCompliance = compliance.filter((c) => c.applicationId === app.id || c.institutionId === app.institutionId);
  const linkedAudit = activity.filter((a) => a.relatedRecordId === app.id);

  // Handlers for Checklist toggling
  const handleChecklistToggle = async (itemId) => {
    if (role !== ROLES.SCRUTINY_OFFICER && role !== ROLES.REGIONAL_OFFICER) return;
    const targetItem = checklist.find((i) => i.id === itemId);
    const newVerified = !targetItem?.checked;
    const updated = checklist.map((item) => (item.id === itemId ? { ...item, checked: newVerified } : item));
    setChecklist(updated);

    const result = await performWorkflowAction('UPDATE_CHECKLIST', {
      applicationId: app.id,
      actorName: user?.name,
      actorRole: role,
      checklistItemCode: itemId,
      checklistItemVerified: newVerified,
      reason: targetItem?.label,
    });

    if (!result.success) {
      alert(result.error);
    }
  };

  // Handler: Return for Correction
  const handleConfirmReturn = async (e) => {
    e.preventDefault();
    if (!returnReason.trim()) {
      alert('Please enter a mandatory return reason.');
      return;
    }

    const deficienciesList = returnDeficiencies
      .split('\n')
      .map((d) => d.trim())
      .filter((d) => d.length > 0);

    const result = await performWorkflowAction('RETURN_FOR_CORRECTION', {
      applicationId: app.id,
      actorName: user?.name,
      actorRole: role,
      reason: returnReason,
      requirementReference: returnRequirementRef,
      newDeficiencies: deficienciesList.length > 0 ? deficienciesList : [returnReason],
      observations: officerNotes,
    });

    if (result.success) {
      setIsReturnModalOpen(false);
      setReturnReason('');
      setReturnDeficiencies('');
      setActionFeedback({ type: 'warning', message: `Application ${app.id} successfully returned for correction.` });
    } else {
      alert(result.error);
    }
  };

  // Handler: Institution Submits Corrections
  const handleConfirmResubmit = async (e) => {
    e.preventDefault();
    if (!resubmitNotes.trim()) {
      alert('Please enter an explanatory note detailing the rectifications made.');
      return;
    }

    const result = await performWorkflowAction('SUBMIT_CORRECTIONS', {
      applicationId: app.id,
      actorName: user?.name,
      actorRole: role,
      reason: resubmitNotes,
    });

    if (result.success) {
      setIsResubmitModalOpen(false);
      setResubmitNotes('');
      setResubmitFileName('');
      setActionFeedback({ type: 'success', message: `Corrections for ${app.id} successfully submitted for re-scrutiny.` });
    } else {
      alert(result.error);
    }
  };

  // Handler: Scrutiny Officer Resolves Single Deficiency
  const handleConfirmResolveDeficiency = async (e) => {
    e.preventDefault();
    if (!selectedDeficiency) return;

    const result = await performWorkflowAction('RESOLVE_DEFICIENCY', {
      applicationId: app.id,
      actorName: user?.name,
      actorRole: role,
      deficiencyId: selectedDeficiency.id,
      resolutionStatus: 'RESOLVED',
      resolutionRemarks: resolveRemarks || 'Verified compliant against APH norms by scrutiny desk.',
    });

    if (result.success) {
      setIsResolveModalOpen(false);
      setSelectedDeficiency(null);
      setResolveRemarks('');
      setActionFeedback({ type: 'success', message: `Deficiency ${selectedDeficiency.deficiencyCode} resolved.` });
    } else {
      alert(result.error);
    }
  };

  // Handler: Complete Scrutiny
  const handleConfirmComplete = async (e) => {
    e.preventDefault();
    if (app.status === 'Returned') {
      alert('Cannot complete scrutiny while application has status "Returned". Rectifications must be submitted and re-scrutinized first.');
      return;
    }

    const unresolved = app.deficiencyItems?.filter((d) => d.status !== 'RESOLVED') || [];
    if (unresolved.length > 0) {
      alert(`Cannot complete scrutiny: ${unresolved.length} deficiency item(s) are still unresolved. Please verify and resolve each deficiency first.`);
      return;
    }

    const result = await performWorkflowAction('COMPLETE_SCRUTINY', {
      applicationId: app.id,
      actorName: user?.name,
      actorRole: role,
      nextStage: selectedNextStage,
      observations: completeRemarks || officerNotes,
    });

    if (result.success) {
      setIsCompleteModalOpen(false);
      setCompleteRemarks('');
      setActionFeedback({
        type: 'success',
        message: `Scrutiny cleared for ${app.id}. Application advanced to ${selectedNextStage}.`,
      });
    } else {
      alert(result.error);
    }
  };

  const tabs = [
    { key: 'Overview', label: 'Overview' },
    { key: 'Timeline', label: 'Workflow Timeline', count: app.timeline?.length || 0 },
    { key: 'Scrutiny', label: 'Scrutiny & Observations' },
    { key: 'Submissions', label: 'Submissions', count: app.submissions?.length || 0 },
    { key: 'EVC', label: 'EVC Activity', count: linkedEVC ? 1 : 0 },
    { key: 'Compliance', label: 'Compliance Obligations', count: linkedCompliance.length },
    { key: 'Audit', label: 'Audit History', count: linkedAudit.length },
  ];

  return (
    <div>
      {/* Top Back Nav & Disclaimer */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px', flexWrap: 'wrap', gap: '8px' }}>
        <Link to="/applications" className="btn btn-outline btn-sm" style={{ display: 'inline-flex' }}>
          <ArrowLeft size={14} /> Back to Application Explorer
        </Link>
        <span style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
          Configurable sample workflow (Demo configuration; does not claim to represent officially validated AICTE APH rules)
        </span>
      </div>

      {/* Action Notification Alert */}
      {actionFeedback && (
        <div
          className={`callout ${actionFeedback.type === 'success' ? 'callout-blue' : 'callout-amber'}`}
          style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}
        >
          <span>{actionFeedback.message}</span>
          <button onClick={() => setActionFeedback(null)} className="btn btn-sm" style={{ padding: '2px 6px' }}>
            Dismiss
          </button>
        </div>
      )}

      {/* Detail Header / Identity Banner */}
      <div
        style={{
          background: '#FFFFFF',
          border: '1px solid var(--color-border)',
          borderRadius: 'var(--radius-sm)',
          padding: '18px 20px',
          marginBottom: '16px',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '14px' }}>
          <div>
            <div style={{ fontSize: '10px', textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--color-teal)', fontWeight: '800' }}>
              OFFICIAL APPLICATION DOSSIER · {app.region}
            </div>
            <h1 style={{ fontSize: '22px', fontWeight: '800', color: 'var(--color-navy)', margin: '4px 0 2px' }}>
              {app.id} · {app.institutionName}
            </h1>
            <div style={{ fontSize: 'var(--font-size-sm)', color: 'var(--color-text-muted)', marginTop: '2px' }}>
              Institution ID: <strong>{app.institutionId}</strong> · Category: <strong>{app.applicationType}</strong>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
            <StatusBadge status={app.status} />

            {/* Scrutiny Officer Primary Actions */}
            {(role === ROLES.SCRUTINY_OFFICER || role === ROLES.REGIONAL_OFFICER) && (app.currentStage === 'Scrutiny' || app.currentStage === 'Re-scrutiny') && (
              <>
                <button onClick={() => setIsReturnModalOpen(true)} className="btn btn-danger btn-sm">
                  <CornerUpLeft size={13} /> Return for Correction
                </button>
                <button
                  onClick={() => {
                    if (app.status === 'Returned' || (app.deficiencies && app.deficiencies.length > 0)) {
                      alert('Cannot complete scrutiny while application has outstanding deficiencies. Rectifications must be submitted and verified first.');
                      return;
                    }
                    setIsCompleteModalOpen(true);
                  }}
                  className="btn btn-primary btn-sm"
                  disabled={app.status === 'Returned' || (app.deficiencies && app.deficiencies.length > 0)}
                  title={app.status === 'Returned' || (app.deficiencies && app.deficiencies.length > 0) ? 'Cannot complete scrutiny while deficiencies are pending' : 'Complete Scrutiny'}
                >
                  <CheckCircle2 size={13} /> Complete Scrutiny
                </button>
              </>
            )}

            {/* Institution Representative Actions */}
            {role === ROLES.INSTITUTION_REP && app.status === 'Returned' && (
              <button onClick={() => setIsResubmitModalOpen(true)} className="btn btn-primary btn-sm">
                <Upload size={13} /> Submit Corrections Now
              </button>
            )}
          </div>
        </div>

        {/* Metadata Summary Strip */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
            gap: '12px',
            marginTop: '16px',
            paddingTop: '14px',
            borderTop: '1px solid var(--color-border-subtle)',
          }}
        >
          <div>
            <span style={{ fontSize: '10px', fontWeight: '700', textTransform: 'uppercase', color: 'var(--color-text-muted)' }}>
              Current Stage
            </span>
            <div style={{ fontSize: '12px', fontWeight: '700', color: 'var(--color-navy)', marginTop: '2px' }}>
              {app.currentStage}
            </div>
          </div>

          <div>
            <span style={{ fontSize: '10px', fontWeight: '700', textTransform: 'uppercase', color: 'var(--color-text-muted)' }}>
              Assigned Desk Owner
            </span>
            <div style={{ fontSize: '12px', fontWeight: '700', color: 'var(--color-navy)', marginTop: '2px' }}>
              {app.owner}
            </div>
          </div>

          <div>
            <span style={{ fontSize: '10px', fontWeight: '700', textTransform: 'uppercase', color: 'var(--color-text-muted)' }}>
              Prescribed Deadline
            </span>
            <div style={{ fontSize: '12px', fontWeight: '700', color: 'var(--color-navy)', marginTop: '2px' }}>
              {app.dueDate}
            </div>
          </div>

          <div>
            <span style={{ fontSize: '10px', fontWeight: '700', textTransform: 'uppercase', color: 'var(--color-text-muted)' }}>
              Stage Ageing
            </span>
            <div style={{ fontSize: '12px', fontWeight: '700', color: 'var(--color-navy)', marginTop: '2px' }}>
              {app.ageDays ? `${app.ageDays} days` : '1 day'}
            </div>
          </div>

          <div>
            <span style={{ fontSize: '10px', fontWeight: '700', textTransform: 'uppercase', color: 'var(--color-text-muted)' }}>
              Workflow Engine
            </span>
            <div style={{ fontSize: '12px', fontWeight: '700', color: 'var(--color-navy)', marginTop: '2px' }}>
              {app.workflowVersion}
            </div>
          </div>
        </div>

        {/* Next Required Action Box */}
        <div
          style={{
            marginTop: '14px',
            padding: '10px 14px',
            background: 'var(--color-blue-soft)',
            border: '1px solid #C4DEF3',
            borderRadius: 'var(--radius-sm)',
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
          }}
        >
          <div style={{ fontWeight: '800', color: 'var(--color-blue)', fontSize: '15px' }}>➔</div>
          <div>
            <span style={{ fontSize: '11px', fontWeight: '700', color: '#163E63', textTransform: 'uppercase' }}>
              Next Prescribed Action:
            </span>
            <div style={{ fontSize: '12px', fontWeight: '600', color: '#102B46' }}>
              {app.nextAction || 'Continue review under Approval Process Handbook'}
            </div>
          </div>
        </div>
      </div>

      {/* Main Tabs Navigation */}
      <div className="panel" style={{ padding: 0 }}>
        <TabsNav tabs={tabs} activeTab={activeTab} onTabChange={setActiveTab} />

        <div style={{ padding: '18px' }}>
          {/* TAB 1: OVERVIEW */}
          {activeTab === 'Overview' && (
            <div>
              {/* If Returned, show prominent notice */}
              {app.status === 'Returned' && (
                <div className="callout callout-amber" style={{ marginBottom: '18px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
                    <AlertTriangle size={16} color="var(--color-warning)" />
                    <strong style={{ fontSize: '13px' }}>Scrutiny Deficiency Notice Issued</strong>
                  </div>
                  <p style={{ fontSize: '12px', marginBottom: '8px' }}>
                    This application was returned for institutional correction by the Scrutiny Desk. Please review the deficiency points and submit updated proofs.
                  </p>
                  <ul style={{ paddingLeft: '20px', fontSize: '12px' }}>
                    {app.deficiencies?.map((def, idx) => (
                      <li key={idx} style={{ marginBottom: '4px' }}>
                        <strong>Deficiency #{idx + 1}:</strong> {def}
                      </li>
                    ))}
                  </ul>
                  {role === ROLES.INSTITUTION_REP && (
                    <div style={{ marginTop: '12px' }}>
                      <button onClick={() => setIsResubmitModalOpen(true)} className="btn btn-primary btn-sm">
                        <Upload size={13} /> Open Rectification Submission Form
                      </button>
                    </div>
                  )}
                </div>
              )}

              <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '16px' }}>
                <div>
                  <div className="panel">
                    <div className="panel-header">
                      <h3 className="panel-title">Application Summary</h3>
                    </div>
                    <div className="panel-body">
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '12px' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '6px', borderBottom: '1px solid var(--color-border-subtle)' }}>
                          <span style={{ color: 'var(--color-text-muted)' }}>Official ID</span>
                          <strong>{app.id}</strong>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '6px', borderBottom: '1px solid var(--color-border-subtle)' }}>
                          <span style={{ color: 'var(--color-text-muted)' }}>Institution Name</span>
                          <strong>{app.institutionName}</strong>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '6px', borderBottom: '1px solid var(--color-border-subtle)' }}>
                          <span style={{ color: 'var(--color-text-muted)' }}>Territorial Region</span>
                          <strong>{app.region}</strong>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '6px', borderBottom: '1px solid var(--color-border-subtle)' }}>
                          <span style={{ color: 'var(--color-text-muted)' }}>Submission Date</span>
                          <strong>{app.submissionDate}</strong>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', paddingBottom: '6px', borderBottom: '1px solid var(--color-border-subtle)' }}>
                          <span style={{ color: 'var(--color-text-muted)' }}>APH Version</span>
                          <strong>{app.workflowVersion}</strong>
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                          <span style={{ color: 'var(--color-text-muted)' }}>Lifecycle Status</span>
                          <StatusBadge status={app.status} />
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div>
                  <div className="panel">
                    <div className="panel-header">
                      <h3 className="panel-title">Desk Observations</h3>
                    </div>
                    <div className="panel-body">
                      <p style={{ fontSize: '12px', color: 'var(--color-text)', lineHeight: 1.5 }}>
                        {app.observations || 'No formal observations recorded yet.'}
                      </p>
                      <div style={{ marginTop: '16px', fontSize: '11px', color: 'var(--color-text-muted)' }}>
                        Checklist Progress: <strong>{app.checklistProgress ? `${app.checklistProgress.completed} of ${app.checklistProgress.total} Items Completed` : 'In Progress'}</strong>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: WORKFLOW TIMELINE */}
          {activeTab === 'Timeline' && (
            <div>
              <div style={{ fontSize: '11px', color: 'var(--color-text-muted)', marginBottom: '14px' }}>
                Chronological stage transitions and auditable milestones recorded for <strong>{app.id}</strong>.
              </div>
              <div style={{ position: 'relative', paddingLeft: '24px', borderLeft: '2px solid var(--color-border)' }}>
                {app.timeline?.map((item, idx) => (
                  <div key={item.id || idx} style={{ position: 'relative', marginBottom: '20px' }}>
                    <div
                      style={{
                        position: 'absolute',
                        left: '-30px',
                        top: '2px',
                        width: '10px',
                        height: '10px',
                        borderRadius: '50%',
                        backgroundColor: idx === 0 ? 'var(--color-teal)' : '#FFFFFF',
                        border: '2px solid var(--color-teal)',
                      }}
                    ></div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', gap: '8px' }}>
                      <strong style={{ fontSize: '13px', color: 'var(--color-navy)' }}>
                        {item.title}
                      </strong>
                      <span style={{ fontSize: '10px', color: 'var(--color-text-light)' }}>
                        {item.timestamp}
                      </span>
                    </div>
                    <div style={{ fontSize: '11px', color: 'var(--color-text-muted)', marginTop: '2px' }}>
                      Recorded by: <strong>{item.actor}</strong>
                    </div>
                    <div style={{ fontSize: '12px', color: 'var(--color-text)', marginTop: '6px', background: '#F8FAFC', padding: '8px 12px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--color-border-subtle)' }}>
                      {item.description}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* TAB 3: SCRUTINY & OBSERVATIONS */}
          {activeTab === 'Scrutiny' && (
            <div>
              <div className="callout callout-blue" style={{ marginBottom: '16px' }}>
                <strong>Official Scrutiny Desk:</strong> Technical checklist verification in accordance with AICTE Approval Process Handbook (APH) norms.
              </div>

              {/* Interactive Checklist */}
              <div className="panel" style={{ marginBottom: '16px' }}>
                <div className="panel-header">
                  <h3 className="panel-title">Statutory Verification Checklist</h3>
                  <span className="badge badge-blue">
                    {checklist.filter((i) => i.checked).length} of {checklist.length} Verified
                  </span>
                </div>
                <div className="panel-body" style={{ padding: 0 }}>
                  {checklist.map((item) => (
                    <label
                      key={item.id}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '12px',
                        padding: '12px 16px',
                        borderBottom: '1px solid var(--color-border-subtle)',
                        cursor: role === ROLES.SCRUTINY_OFFICER || role === ROLES.REGIONAL_OFFICER ? 'pointer' : 'default',
                        backgroundColor: item.checked ? '#FFFFFF' : '#FFFDF8',
                      }}
                    >
                      <input
                        type="checkbox"
                        checked={item.checked}
                        onChange={() => handleChecklistToggle(item.id)}
                        disabled={role !== ROLES.SCRUTINY_OFFICER && role !== ROLES.REGIONAL_OFFICER}
                        style={{ width: '16px', height: '16px' }}
                      />
                      <span style={{ fontSize: '12px', color: item.checked ? 'var(--color-text)' : '#7A5408', fontWeight: item.checked ? '500' : '600' }}>
                        {item.label}
                      </span>
                    </label>
                  ))}
                </div>
              </div>

              {/* APH Incremental Rule Validation & Change-Impact Engine Panel */}
              <div className="panel" style={{ marginBottom: '16px' }}>
                <div className="panel-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <Cpu size={16} style={{ color: 'var(--color-blue)' }} />
                    <h3 className="panel-title">APH Regulatory Validation Engine (Incremental Change-Impact)</h3>
                  </div>
                  {validationReport && (
                    <div style={{ display: 'flex', gap: '6px', alignItems: 'center' }}>
                      <span className="badge badge-green" title="Unmodified rule checks skipped via cryptographic cache">
                        ✓ {validationReport.rulesSkippedCount} Skipped (Cache Hit)
                      </span>
                      <span className="badge badge-blue" title="Rules re-evaluated due to dirty checks or dependency graph invalidation">
                        ⟳ {validationReport.rulesReevaluatedCount} Re-evaluated
                      </span>
                      <span className={`badge ${validationReport.statutoryMandatoryPassed ? 'badge-green' : 'badge-red'}`}>
                        {validationReport.statutoryMandatoryPassed ? 'Mandatory Norms Met' : 'Statutory Deficiency'}
                      </span>
                    </div>
                  )}
                </div>

                <div className="panel-body">
                  {/* Explanatory Regulatory Guard Notice */}
                  <div
                    style={{
                      background: '#F0F7FD',
                      border: '1px solid #C4DFF6',
                      borderRadius: 'var(--radius-sm)',
                      padding: '10px 14px',
                      marginBottom: '14px',
                      fontSize: 'var(--font-size-xs)',
                      color: 'var(--color-navy)',
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      flexWrap: 'wrap',
                      gap: '10px',
                    }}
                  >
                    <div>
                      <strong>Handbook Source Linking &amp; Provenance:</strong> Every check is tied to a specific APH 2026-27 handbook clause. Unaffected proofs preserve their earlier cryptographic SHA-256 hashes and verification timestamps.
                    </div>
                    <div style={{ display: 'flex', gap: '6px' }}>
                      <button
                        type="button"
                        onClick={() => fetchValidation({})}
                        disabled={validationLoading}
                        className="btn btn-outline btn-sm"
                        style={{ fontSize: '11px', padding: '3px 8px' }}
                        title="Re-run incremental validation without field modifications to verify 100% cache skip rate"
                      >
                        <RefreshCw size={11} className={validationLoading ? 'animate-spin' : ''} /> Verify Cache Skips
                      </button>
                      <button
                        type="button"
                        onClick={() => fetchValidation({ faculty_roster: 'Modified faculty roster payload ' + Date.now() })}
                        disabled={validationLoading}
                        className="btn btn-outline btn-sm"
                        style={{ fontSize: '11px', padding: '3px 8px', borderColor: 'var(--color-amber)', color: '#8A5300' }}
                        title="Simulate modifying faculty roster section to verify targeted dirty re-evaluation and dependency invalidation"
                      >
                        <Zap size={11} /> Simulate Faculty Edit
                      </button>
                    </div>
                  </div>

                  {validationLoading ? (
                    <div style={{ padding: '24px', textAlign: 'center', color: 'var(--color-text-muted)', fontSize: '12px' }}>
                      <RefreshCw size={18} className="animate-spin" style={{ margin: '0 auto 8px', display: 'block' }} />
                      Traversing dependency graph &amp; evaluating cryptographic hashes...
                    </div>
                  ) : validationReport && validationReport.activeProofs ? (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                      {validationReport.activeProofs.map((proof) => {
                        const isSkipped = proof.reEvaluationReason === 'SKIPPED_UNMODIFIED_CACHE_HIT' || proof.reEvaluationReason === 'INITIAL_EVALUATION';
                        const isAi = proof.decisionType === 'AI_ASSISTED_ADVISORY';
                        return (
                          <div
                            key={proof.id || proof.ruleId}
                            style={{
                              border: '1px solid var(--color-border-subtle)',
                              borderRadius: 'var(--radius-sm)',
                              padding: '10px 14px',
                              backgroundColor: isAi ? '#FAF8FF' : proof.validationOutcome === 'PASSED' ? '#FFFFFF' : '#FFFDF9',
                            }}
                          >
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '8px', marginBottom: '4px', flexWrap: 'wrap' }}>
                              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                                <span style={{ fontWeight: '700', fontSize: '12px', color: 'var(--color-navy)', fontFamily: 'monospace' }}>
                                  {proof.ruleId}
                                </span>
                                <span className="badge badge-gray" style={{ fontSize: '10px' }}>
                                  {proof.aphClause}
                                </span>
                                <span
                                  className={`badge ${proof.ruleCategory === 'STATUTORY_MANDATORY' ? 'badge-blue' : 'badge-teal'}`}
                                  style={{ fontSize: '10px' }}
                                >
                                  {proof.ruleCategory === 'STATUTORY_MANDATORY' ? 'Mandatory Statutory' : 'Advisory Norm'}
                                </span>
                                {isAi && (
                                  <span className="badge badge-amber" style={{ fontSize: '10px' }} title="Non-binding AI algorithm suggestion; officer decision required">
                                    AI Recommendation (Non-Binding)
                                  </span>
                                )}
                              </div>

                              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <span
                                  className={`badge ${
                                    proof.reEvaluationReason === 'FIELD_DIRTY_HASH_MISMATCH'
                                      ? 'badge-amber'
                                      : proof.reEvaluationReason === 'DEPENDENCY_INVALIDATION'
                                      ? 'badge-teal'
                                      : 'badge-green'
                                  }`}
                                  style={{ fontSize: '10px' }}
                                >
                                  {proof.reEvaluationReason === 'FIELD_DIRTY_HASH_MISMATCH'
                                    ? 'Dirty Field Re-evaluated'
                                    : proof.reEvaluationReason === 'DEPENDENCY_INVALIDATION'
                                    ? 'Dependency Invalidation'
                                    : 'Cached Proof (Skipped)'}
                                </span>
                                <StatusBadge status={proof.validationOutcome === 'PASSED' ? 'Completed' : 'Overdue'} />
                              </div>
                            </div>

                            <div style={{ fontSize: '12px', fontWeight: '600', color: 'var(--color-text)', marginBottom: '4px' }}>
                              {proof.ruleName}
                            </div>

                            <div style={{ fontSize: '11px', color: 'var(--color-text-muted)', lineHeight: '1.4', marginBottom: '6px' }}>
                              {proof.proofDetails}
                            </div>

                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '10px', color: 'var(--color-text-light)', borderTop: '1px solid #F1F5F9', paddingTop: '4px' }}>
                              <span>
                                Provenance Hash: <code style={{ fontSize: '10px' }}>{proof.inputPayloadHash ? proof.inputPayloadHash.substring(0, 16) + '...' : 'Verified'}</code>
                              </span>
                              <span>
                                Evaluated: {proof.evaluatedAt ? new Date(proof.evaluatedAt).toLocaleTimeString('en-GB') : 'Just now'} ({proof.executionDurationMs || 1} ms)
                              </span>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  ) : null}
                </div>
              </div>
              {app.deficiencyItems && app.deficiencyItems.length > 0 && (
                <div className="panel" style={{ marginBottom: '16px' }}>
                  <div className="panel-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <h3 className="panel-title">Itemized Statutory Deficiencies ({app.deficiencyItems.length})</h3>
                    <span className="badge badge-amber">
                      {app.deficiencyItems.filter((d) => d.status === 'RESOLVED').length} of {app.deficiencyItems.length} Resolved
                    </span>
                  </div>
                  <div className="panel-body" style={{ padding: 0 }}>
                    {app.deficiencyItems.map((def) => {
                      const isResolved = def.status === 'RESOLVED';
                      const isRectified = def.status === 'RECTIFIED_PENDING_REVIEW';
                      return (
                        <div
                          key={def.id}
                          style={{
                            padding: '14px 16px',
                            borderBottom: '1px solid var(--color-border-subtle)',
                            backgroundColor: isResolved ? '#F6FAF7' : isRectified ? '#F8FAFD' : '#FFFDF9',
                          }}
                        >
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '8px', marginBottom: '6px' }}>
                            <div>
                              <span style={{ fontSize: '11px', fontWeight: '700', color: 'var(--color-navy)', marginRight: '8px' }}>
                                {def.deficiencyCode}
                              </span>
                              <span style={{ fontSize: '10px', color: 'var(--color-text-muted)' }}>
                                Reference: <strong>{def.requirementReference || 'APH Norms'}</strong>
                              </span>
                            </div>
                            <span
                              className={`badge ${isResolved ? 'badge-green' : isRectified ? 'badge-blue' : 'badge-amber'}`}
                              style={{ fontSize: '10px' }}
                            >
                              {isResolved ? 'Resolved' : isRectified ? 'Rectification Submitted' : 'Deficiency Open'}
                            </span>
                          </div>

                          <div style={{ fontSize: '12px', color: 'var(--color-text)', marginBottom: '8px', lineHeight: 1.4 }}>
                            {def.description}
                          </div>

                          {def.institutionResponse && (
                            <div style={{ background: '#FFFFFF', padding: '8px 10px', borderRadius: 'var(--radius-sm)', border: '1px solid #D0E1FD', fontSize: '11px', marginBottom: '6px' }}>
                              <span style={{ fontWeight: '700', color: '#1B4D89' }}>College Rectification Response:</span> {def.institutionResponse}
                            </div>
                          )}

                          {def.resolutionRemarks && (
                            <div style={{ background: '#FFFFFF', padding: '8px 10px', borderRadius: 'var(--radius-sm)', border: '1px solid #B8E4C9', fontSize: '11px', marginBottom: '6px' }}>
                              <span style={{ fontWeight: '700', color: '#1A6B3D' }}>Officer Resolution Note:</span> {def.resolutionRemarks}
                            </div>
                          )}

                          {!isResolved && (role === ROLES.SCRUTINY_OFFICER || role === ROLES.REGIONAL_OFFICER) && (
                            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '8px' }}>
                              <button
                                onClick={() => {
                                  setSelectedDeficiency(def);
                                  setResolveRemarks(`Verified rectified evidence for ${def.deficiencyCode} compliant with ${def.requirementReference || 'APH Table 2 norms'}.`);
                                  setIsResolveModalOpen(true);
                                }}
                                className="btn btn-primary btn-sm"
                                style={{ padding: '3px 8px', fontSize: '11px' }}
                              >
                                <CheckCircle2 size={12} /> Resolve Deficiency
                              </button>
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Scrutiny Officer Observation Input */}
              <div className="panel">
                <div className="panel-header">
                  <h3 className="panel-title">Desk Observation &amp; Deficiencies Log</h3>
                </div>
                <div className="panel-body">
                  <div className="form-group">
                    <label className="form-label" htmlFor="officerObservations">
                      Officer Detailed Observations
                    </label>
                    <textarea
                      id="officerObservations"
                      className="form-textarea"
                      rows={4}
                      value={officerNotes}
                      onChange={(e) => setOfficerNotes(e.target.value)}
                      disabled={role !== ROLES.SCRUTINY_OFFICER && role !== ROLES.REGIONAL_OFFICER}
                      placeholder="Enter scrutiny observations, deficiency details, and statutory references..."
                    />
                  </div>

                  {(role === ROLES.SCRUTINY_OFFICER || role === ROLES.REGIONAL_OFFICER) && (
                    <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end', marginTop: '12px' }}>
                      <button
                        onClick={() => {
                          performWorkflowAction('UPDATE_OBSERVATIONS', {
                            applicationId: app.id,
                            actorName: user?.name,
                            actorRole: role,
                            observations: officerNotes,
                          });
                          alert('Observations saved to dossier.');
                        }}
                        className="btn btn-outline btn-sm"
                      >
                        Save Observations
                      </button>
                      <button onClick={() => setIsReturnModalOpen(true)} className="btn btn-danger btn-sm">
                        <CornerUpLeft size={13} /> Return for Correction
                      </button>
                      <button
                        onClick={() => {
                          if (app.status === 'Returned' || (app.deficiencies && app.deficiencies.length > 0)) {
                            alert('Cannot complete scrutiny while application has outstanding deficiencies. Rectifications must be submitted and verified first.');
                            return;
                          }
                          setIsCompleteModalOpen(true);
                        }}
                        className="btn btn-primary btn-sm"
                        disabled={app.status === 'Returned' || (app.deficiencies && app.deficiencies.length > 0)}
                        title={app.status === 'Returned' || (app.deficiencies && app.deficiencies.length > 0) ? 'Cannot complete scrutiny while deficiencies are pending' : 'Complete Scrutiny'}
                      >
                        <CheckCircle2 size={13} /> Complete Scrutiny
                      </button>
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* TAB 4: SUBMISSIONS */}
          {activeTab === 'Submissions' && (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
                <span style={{ fontSize: '12px', color: 'var(--color-text-muted)' }}>
                  All uploaded submission versions and rectification packages for <strong>{app.id}</strong>.
                </span>
                {role === ROLES.INSTITUTION_REP && app.status === 'Returned' && (
                  <button onClick={() => setIsResubmitModalOpen(true)} className="btn btn-primary btn-sm">
                    <Upload size={13} /> Upload Resubmission Package
                  </button>
                )}
              </div>

              <DataTable
                columns={[
                  { header: 'Version ID', key: 'id', render: (r) => <strong>{r.id}</strong> },
                  { header: 'Submission Title', key: 'title' },
                  { header: 'Version', key: 'version', render: (r) => <code>{r.version}</code> },
                  { header: 'Submitted By', key: 'submittedBy' },
                  { header: 'Timestamp', key: 'submittedAt' },
                  { header: 'File Name', key: 'fileName', render: (r) => <span style={{ color: 'var(--color-blue)' }}>{r.fileName}</span> },
                  { header: 'Status', key: 'status', render: (r) => <StatusBadge status={r.status} /> },
                ]}
                data={app.submissions || []}
              />
            </div>
          )}

          {/* TAB 5: EVC ACTIVITY */}
          {activeTab === 'EVC' && (
            <div>
              {linkedEVC ? (
                <div className="panel">
                  <div className="panel-header">
                    <div>
                      <h3 className="panel-title">Linked Expert Visit Committee Inspection</h3>
                      <p className="panel-subtitle">Reference: {linkedEVC.id}</p>
                    </div>
                    <StatusBadge status={linkedEVC.status} />
                  </div>
                  <div className="panel-body">
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '14px', fontSize: '12px' }}>
                      <div>
                        <span style={{ color: 'var(--color-text-muted)', display: 'block' }}>Visit Date</span>
                        <strong>{linkedEVC.visitDate}</strong>
                      </div>
                      <div>
                        <span style={{ color: 'var(--color-text-muted)', display: 'block' }}>Coordinator</span>
                        <strong>{linkedEVC.coordinator}</strong>
                      </div>
                      <div>
                        <span style={{ color: 'var(--color-text-muted)', display: 'block' }}>Inspection Type</span>
                        <strong>{linkedEVC.inspectionType}</strong>
                      </div>
                      <div>
                        <span style={{ color: 'var(--color-text-muted)', display: 'block' }}>Committee Experts</span>
                        <strong>{linkedEVC.experts?.join('; ') || 'Pending Assignment'}</strong>
                      </div>
                    </div>
                    <div style={{ marginTop: '16px' }}>
                      <Link to="/evc" className="btn btn-outline btn-sm">
                        Open in EVC Workbench ↗
                      </Link>
                    </div>
                  </div>
                </div>
              ) : (
                <div style={{ padding: '24px', textAlign: 'center', color: 'var(--color-text-muted)' }}>
                  <Calendar size={28} style={{ margin: '0 auto 8px', color: '#9BB1C6' }} />
                  <div>No EVC inspection scheduled for this application at current stage.</div>
                </div>
              )}
            </div>
          )}

          {/* TAB 6: COMPLIANCE OBLIGATIONS */}
          {activeTab === 'Compliance' && (
            <div>
              <div style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginBottom: '14px' }}>
                Post-approval undertakings and statutory conditions linked to this application or institution.
              </div>

              <DataTable
                columns={[
                  { header: 'Obligation ID', key: 'id', render: (r) => <strong>{r.id}</strong> },
                  { header: 'Requirement', key: 'requirement' },
                  { header: 'Category', key: 'category' },
                  { header: 'Due Date', key: 'dueDate' },
                  { header: 'Reviewer', key: 'reviewer' },
                  { header: 'Status', key: 'status', render: (r) => <StatusBadge status={r.status} /> },
                ]}
                data={linkedCompliance}
                emptyMessage="No linked compliance obligations."
              />
            </div>
          )}

          {/* TAB 7: AUDIT HISTORY */}
          {activeTab === 'Audit' && (
            <div>
              <div style={{ fontSize: '12px', color: 'var(--color-text-muted)', marginBottom: '14px' }}>
                Immutable audit trail of transitions and decisions logged for <strong>{app.id}</strong>.
              </div>

              <DataTable
                columns={[
                  { header: 'Timestamp', key: 'timestamp', render: (r) => <span style={{ whiteSpace: 'nowrap' }}>{r.timestamp}</span> },
                  { header: 'Actor & Role', key: 'actor' },
                  { header: 'Action', key: 'action', render: (r) => <strong>{r.action}</strong> },
                  { header: 'Details / Reason', key: 'details' },
                  { header: 'Audit Category', key: 'category', render: (r) => <span className="badge badge-gray">{r.category}</span> },
                ]}
                data={linkedAudit}
                emptyMessage="No audit records logged."
              />
            </div>
          )}
        </div>
      </div>

      {/* MODAL 1: RETURN FOR CORRECTION */}
      <Modal
        isOpen={isReturnModalOpen}
        onClose={() => setIsReturnModalOpen(false)}
        title="Return Application for Correction"
        subtitle={`Official Deficiency Notice for ${app.id}`}
        footer={
          <>
            <button onClick={() => setIsReturnModalOpen(false)} className="btn btn-outline btn-sm">
              Cancel
            </button>
            <button onClick={handleConfirmReturn} className="btn btn-danger btn-sm">
              <CornerUpLeft size={13} /> Confirm Return to College
            </button>
          </>
        }
      >
        <form onSubmit={handleConfirmReturn}>
          <div className="callout callout-amber" style={{ fontSize: '11px', marginBottom: '12px' }}>
            Returning this application transitions status to <strong>Returned</strong> and transfers ownership to the Institution Representative.
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="returnReqRef">
              Statutory Requirement Reference (APH Handbook Clause) *
            </label>
            <input
              id="returnReqRef"
              className="form-input"
              value={returnRequirementRef}
              onChange={(e) => setReturnRequirementRef(e.target.value)}
              placeholder="e.g. APH Table 2.1 Faculty Cadre Ratio / Clause 3.4 Fire NOC"
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="returnReasonInput">
              Mandatory Return Reason *
            </label>
            <textarea
              id="returnReasonInput"
              className="form-textarea"
              rows={3}
              placeholder="State the regulatory justification under APH..."
              value={returnReason}
              onChange={(e) => setReturnReason(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="returnDeficienciesInput">
              Itemized Deficiencies (One per line)
            </label>
            <textarea
              id="returnDeficienciesInput"
              className="form-textarea"
              rows={3}
              placeholder="e.g. Faculty cadre ratio in Computer Engineering non-compliant&#10;Building safety NOC expired"
              value={returnDeficiencies}
              onChange={(e) => setReturnDeficiencies(e.target.value)}
            />
          </div>
        </form>
      </Modal>

      {/* MODAL 2: SUBMIT RECTIFICATIONS (INSTITUTION) */}
      <Modal
        isOpen={isResubmitModalOpen}
        onClose={() => setIsResubmitModalOpen(false)}
        title="Submit Rectifications / Corrections"
        subtitle={`Resubmission Dossier for ${app.id}`}
        footer={
          <>
            <button onClick={() => setIsResubmitModalOpen(false)} className="btn btn-outline btn-sm">
              Cancel
            </button>
            <button onClick={handleConfirmResubmit} className="btn btn-primary btn-sm">
              <Send size={13} /> Submit for Re-Scrutiny
            </button>
          </>
        }
      >
        <form onSubmit={handleConfirmResubmit}>
          <div className="callout callout-blue" style={{ fontSize: '11px', marginBottom: '12px' }}>
            Uploading this response will advance the dossier to <strong>Re-Scrutiny</strong> and notify Scrutiny Officer <strong>B. Singh</strong>.
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="resubmitNotesInput">
              Explanation of Rectifications Made *
            </label>
            <textarea
              id="resubmitNotesInput"
              className="form-textarea"
              rows={4}
              placeholder="Detail how each deficiency was resolved (e.g. Attached appointment letters for 3 Assistant Professors and municipal renewal receipt)..."
              value={resubmitNotes}
              onChange={(e) => setResubmitNotes(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="resubmitFileInput">
              Attach Signed Rectification Evidence Package (PDF / ZIP) *
            </label>
            <input
              id="resubmitFileInput"
              type="file"
              accept=".pdf,.zip,.png,.jpg"
              className="form-input"
              onChange={(e) => {
                if (e.target.files && e.target.files[0]) {
                  setResubmitFileName(e.target.files[0].name);
                }
              }}
            />
            {resubmitFileName && (
              <div style={{ marginTop: '6px', fontSize: '11px', color: 'var(--color-teal)' }}>
                Attached: <strong>{resubmitFileName}</strong>
              </div>
            )}
          </div>
        </form>
      </Modal>

      {/* MODAL 3: COMPLETE SCRUTINY */}
      <Modal
        isOpen={isCompleteModalOpen}
        onClose={() => setIsCompleteModalOpen(false)}
        title="Complete Scrutiny &amp; Forward Application"
        subtitle={`Clearing Scrutiny for ${app.id}`}
        footer={
          <>
            <button onClick={() => setIsCompleteModalOpen(false)} className="btn btn-outline btn-sm">
              Cancel
            </button>
            <button onClick={handleConfirmComplete} className="btn btn-primary btn-sm">
              <CheckCircle2 size={13} /> Confirm Clearance
            </button>
          </>
        }
      >
        <form onSubmit={handleConfirmComplete}>
          <div className="callout callout-blue" style={{ fontSize: '11px', marginBottom: '12px' }}>
            Confirming scrutiny clearance marks all checklist items complete and routes the application to the next prescribed APH stage.
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="nextStageSelect">
              Select Next Prescribed Stage
            </label>
            <select
              id="nextStageSelect"
              className="form-select"
              value={selectedNextStage}
              onChange={(e) => setSelectedNextStage(e.target.value)}
            >
              <option value="EVC scheduling">EVC Scheduling (Physical / Hybrid Inspection)</option>
              <option value="Committee review">Regional Committee Review (EVC-Exempt)</option>
            </select>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="completeRemarksInput">
              Officer Clearance Remarks
            </label>
            <textarea
              id="completeRemarksInput"
              className="form-textarea"
              rows={3}
              placeholder="All mandatory criteria verified compliant with APH Table 2 norms..."
              value={completeRemarks}
              onChange={(e) => setCompleteRemarks(e.target.value)}
            />
          </div>
        </form>
      </Modal>

      {/* MODAL 4: RESOLVE DEFICIENCY (RE-SCRUTINY) */}
      <Modal
        isOpen={isResolveModalOpen}
        onClose={() => setIsResolveModalOpen(false)}
        title="Resolve Statutory Deficiency"
        subtitle={`Re-Scrutiny Evaluation for ${selectedDeficiency?.deficiencyCode}`}
        footer={
          <>
            <button onClick={() => setIsResolveModalOpen(false)} className="btn btn-outline btn-sm">
              Cancel
            </button>
            <button onClick={handleConfirmResolveDeficiency} className="btn btn-primary btn-sm">
              <CheckCircle2 size={13} /> Mark Deficiency Resolved
            </button>
          </>
        }
      >
        <form onSubmit={handleConfirmResolveDeficiency}>
          <div className="callout callout-blue" style={{ fontSize: '11px', marginBottom: '12px' }}>
            Resolving this deficiency formally marks the requirement as verified under APH norms and registers the action in the immutable audit trail.
          </div>

          <div style={{ marginBottom: '12px', fontSize: '12px' }}>
            <strong>Deficiency Description:</strong>
            <p style={{ marginTop: '4px', color: 'var(--color-text-muted)' }}>{selectedDeficiency?.description}</p>
          </div>

          {selectedDeficiency?.institutionResponse && (
            <div style={{ marginBottom: '12px', fontSize: '12px', background: '#F8FAFC', padding: '8px 10px', borderRadius: 'var(--radius-sm)' }}>
              <strong>College Rectification Response:</strong>
              <p style={{ marginTop: '2px', color: 'var(--color-navy)' }}>{selectedDeficiency.institutionResponse}</p>
            </div>
          )}

          <div className="form-group">
            <label className="form-label" htmlFor="resolveRemarksInput">
              Scrutiny Officer Resolution Verification Remarks *
            </label>
            <textarea
              id="resolveRemarksInput"
              className="form-textarea"
              rows={3}
              value={resolveRemarks}
              onChange={(e) => setResolveRemarks(e.target.value)}
              placeholder="e.g. Appointment letters verified against cadre norms; NOC endorsed by municipal authority."
              required
            />
          </div>
        </form>
      </Modal>
    </div>
  );
};
