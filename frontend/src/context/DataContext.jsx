// SIH 26262 - DataContext: Live API Integration & Workflow Engine
import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { ROLES } from '../constants/roles';
import { apiClient } from '../services/apiClient';

const DataContext = createContext(null);

const mapBackendStatus = (status) => {
  if (!status) return 'In progress';
  const s = String(status).toUpperCase();
  if (s === 'IN_PROGRESS' || s === 'IN PROGRESS') return 'In progress';
  if (s === 'DUE_SOON' || s === 'DUE SOON') return 'Due soon';
  if (s === 'OVERDUE') return 'Overdue';
  if (s === 'RETURNED') return 'Returned';
  if (s === 'COMPLETED' || s === 'APPROVED') return 'Completed';
  if (s === 'DRAFT') return 'Draft';
  return status;
};

const mapBackendApplicationToFrontend = (apiApp) => {
  const appId = apiApp.applicationReference || (apiApp.id != null ? `APP-${apiApp.id}` : 'APP-UNKNOWN');
  const checklist = Array.isArray(apiApp.checklist) ? apiApp.checklist : [];
  const completedChecklist = checklist.filter((c) => c.isVerified).length;
  const totalChecklist = checklist.length > 0 ? checklist.length : 4;

  const submissions = Array.isArray(apiApp.submissions) ? apiApp.submissions.map((s) => ({
    id: s.submissionReference || `SUB-${s.id}`,
    title: s.submissionType === 'RECTIFICATION_PACKAGE' ? `Rectification Package (v${s.versionNumber})` : `Initial Statutory Dossier (v${s.versionNumber})`,
    version: `v${s.versionNumber}`,
    submittedBy: `${s.submittedByName || 'Representative'} (${s.submittedById || 'USR-INST'})`,
    submittedAt: s.createdAt ? new Date(s.createdAt).toLocaleString('en-GB', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '',
    fileName: `${appId}_submission_v${s.versionNumber}_signed.pdf`,
    status: s.status || 'Verified',
    notes: s.notes || '',
  })) : [];

  const deficiencyItems = Array.isArray(apiApp.deficiencyItems) ? apiApp.deficiencyItems : [];

  return {
    id: appId,
    backendId: apiApp.id,
    applicationReference: apiApp.applicationReference || appId,
    institutionId: apiApp.institution?.institutionalIdentifier || apiApp.institution?.institutionCode || (apiApp.institution?.id ? `INST-${apiApp.institution.id}` : ''),
    institutionName: apiApp.institution?.name || apiApp.institution?.legalName || 'Unassigned Institution',
    applicationType: apiApp.applicationType || 'Extension of Approval (EoA)',
    academicYear: apiApp.academicYear || '2026-2027',
    region: apiApp.institution?.region || 'Northern Region',
    currentStage: apiApp.currentStage || 'Scrutiny',
    owner: apiApp.ownerName || 'Unassigned Desk',
    ownerId: apiApp.ownerId || '',
    dueDate: apiApp.dueDate ? String(apiApp.dueDate) : null,
    submissionDate: apiApp.createdAt ? String(apiApp.createdAt).substring(0, 10) : '',
    status: mapBackendStatus(apiApp.status),
    ageDays: apiApp.ageDays != null ? apiApp.ageDays : (apiApp.createdAt ? Math.max(0, Math.floor((Date.now() - new Date(apiApp.createdAt).getTime()) / (1000 * 60 * 60 * 24))) : 0),
    workflowVersion: apiApp.version != null ? `APH 2026-27 v${apiApp.version}` : 'APH 2026-27 v1',
    deficiencies: Array.isArray(apiApp.deficiencies) ? apiApp.deficiencies : [],
    deficiencyItems,
    checklist,
    observations: apiApp.observations || '',
    checklistProgress: { completed: completedChecklist, total: totalChecklist },
    timeline: Array.isArray(apiApp.timeline) && apiApp.timeline.length > 0
      ? apiApp.timeline.map((t, idx) => ({
          id: `TL-${idx}-${t.timestamp || Date.now()}`,
          title: t.title || t.action || 'Workflow Event',
          actor: `${t.actorName || 'Official'} (${t.actorRole || 'System'})`,
          timestamp: t.timestamp ? new Date(t.timestamp).toLocaleString('en-GB', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '',
          description: t.description || t.notes || ''
        }))
      : (apiApp.createdAt ? [{
          id: `TL-REG-${apiApp.id}`,
          title: 'Application Dossier Submitted & Registered',
          actor: 'Institution Authorized Representative',
          timestamp: new Date(apiApp.createdAt).toLocaleString('en-GB', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }),
          description: `Official application registered under APH workflow. Assigned tracking reference ${appId}.`
        }] : []),
    submissions
  };
};

const mapBackendEvcToFrontend = (apiEvc) => {
  const evcId = apiEvc.id || apiEvc.evcReference || (apiEvc.backendId != null ? `EVC-${apiEvc.backendId}` : 'EVC-UNKNOWN');
  return {
    id: evcId,
    evcReference: apiEvc.evcReference || evcId,
    backendId: apiEvc.backendId,
    applicationId: apiEvc.applicationId || (apiEvc.applicationBackendId ? `APP-${apiEvc.applicationBackendId}` : ''),
    applicationBackendId: apiEvc.applicationBackendId,
    institutionId: apiEvc.institutionId || '',
    institutionName: apiEvc.institutionName || 'Unassigned Institution',
    region: apiEvc.region || 'Northern Region',
    inspectionType: apiEvc.inspectionType || 'Physical On-Site Verification',
    status: apiEvc.status || 'Awaiting scheduling',
    urgency: apiEvc.urgency || 'In progress',
    visitDate: apiEvc.visitDate || null,
    coordinator: apiEvc.coordinator || 'C. Verma (EVC Operations)',
    coordinatorId: apiEvc.coordinatorId || 'USR-EVC-01',
    deadline: apiEvc.deadline || null,
    experts: Array.isArray(apiEvc.experts) ? apiEvc.experts : [],
    reportStatus: apiEvc.reportStatus || 'Not Applicable',
    reportOutcome: apiEvc.reportOutcome || null,
    reportNotes: apiEvc.reportNotes || '',
    completionNotes: apiEvc.completionNotes || '',
    version: apiEvc.version || 0,
    createdAt: apiEvc.createdAt,
    updatedAt: apiEvc.updatedAt,
  };
};

const mapBackendComplianceToFrontend = (apiItem) => {
  const ref = apiItem.obligationReference || apiItem.id || (apiItem.backendId != null ? `OBL-${apiItem.backendId}` : 'OBL-UNKNOWN');
  return {
    id: ref,
    backendId: apiItem.backendId,
    obligationReference: apiItem.obligationReference || ref,
    institutionId: apiItem.institutionalIdentifier || (apiItem.institutionId ? `INST-${apiItem.institutionId}` : ''),
    institutionName: apiItem.institutionName || 'Unassigned Institution',
    category: apiItem.category || 'General Statutory Condition',
    requirement: apiItem.requirement || '',
    aphClause: apiItem.aphClause || 'APH 2026-27 Norms',
    dueDate: apiItem.dueDate ? String(apiItem.dueDate) : null,
    status: apiItem.status || 'Pending submission',
    urgency: apiItem.urgency || 'In progress',
    evidenceFile: apiItem.evidenceFile || null,
    evidenceHash: apiItem.evidenceHash || null,
    notes: apiItem.evidenceNotes || '',
    evidenceNotes: apiItem.evidenceNotes || '',
    submittedDate: apiItem.submittedDate ? String(apiItem.submittedDate) : null,
    submittedBy: apiItem.submittedBy || null,
    reviewer: apiItem.reviewer || 'Assigned Compliance Reviewer',
    verificationOutcome: apiItem.verificationOutcome || null,
    verificationRemarks: apiItem.verificationRemarks || '',
    verifiedDate: apiItem.verifiedDate ? String(apiItem.verifiedDate) : null,
    verifiedBy: apiItem.verifiedBy || null,
  };
};

const mapBackendDocumentToFrontend = (apiDoc) => {
  const ref = apiDoc.documentReference || apiDoc.id || (apiDoc.backendId != null ? `DOC-${apiDoc.backendId}` : 'DOC-UNKNOWN');
  const versions = Array.isArray(apiDoc.versions) ? apiDoc.versions.map((v) => ({
    version: v.versionNumber != null ? `v${v.versionNumber}.0` : 'v1.0',
    versionNumber: v.versionNumber,
    fileName: v.fileName,
    fileSize: v.fileSize || '0 B',
    fileSizeBytes: v.fileSizeBytes,
    sha256Hash: v.sha256Hash,
    issueDate: v.issueDate ? String(v.issueDate) : null,
    expiryDate: v.expiryDate ? String(v.expiryDate) : null,
    uploadedBy: v.uploadedBy,
    uploadedAt: v.createdAt ? new Date(v.createdAt).toLocaleDateString('en-GB') : '',
    notes: v.notes || '',
    ocrStatus: v.ocrStatus || 'PENDING',
    ocrMessage: v.ocrMessage || '',
    extractedText: v.extractedText || ''
  })) : [];

  return {
    id: ref,
    backendId: apiDoc.backendId,
    documentReference: apiDoc.documentReference || ref,
    institutionId: apiDoc.institutionalIdentifier || (apiDoc.institutionId ? `INST-${apiDoc.institutionId}` : ''),
    institutionName: apiDoc.institutionName || 'Unassigned Institution',
    applicationId: apiDoc.applicationReference || (apiDoc.applicationId ? `APP-${apiDoc.applicationId}` : null),
    documentType: apiDoc.documentType || 'Statutory Instrument',
    category: apiDoc.category || apiDoc.documentType || 'Statutory Compliance',
    fileName: apiDoc.fileName || `${ref}.pdf`,
    fileSizeBytes: apiDoc.fileSizeBytes || 0,
    fileSize: apiDoc.fileSize || '0 B',
    mimeType: apiDoc.mimeType || 'application/pdf',
    sha256Hash: apiDoc.fileHash || '',
    ocrStatus: apiDoc.ocrStatus || 'PENDING',
    ocrMessage: apiDoc.ocrMessage || '',
    extractedText: apiDoc.extractedText || '',
    issueDate: apiDoc.issueDate ? String(apiDoc.issueDate) : '',
    expiryDate: apiDoc.expiryDate ? String(apiDoc.expiryDate) : '',
    validityStatus: apiDoc.validityStatus || 'Valid',
    verificationStatus: apiDoc.verificationStatus || 'Verified',
    reviewer: apiDoc.reviewer || 'Compliance Reviewer',
    notes: apiDoc.notes || '',
    currentVersion: apiDoc.versionNumber != null ? `v${apiDoc.versionNumber}.0` : 'v1.0',
    versionNumber: apiDoc.versionNumber || 1,
    uploadedBy: apiDoc.uploadedBy || 'Institutional Representative',
    createdAt: apiDoc.createdAt,
    renewalStatus: apiDoc.verificationStatus === 'Renewal submitted'
      ? 'Renewal submitted'
      : apiDoc.validityStatus === 'Overdue'
      ? 'Overdue for Renewal'
      : apiDoc.validityStatus === 'Due soon'
      ? 'Renewal Due'
      : 'Renewed & Verified',
    versions: versions.length > 0 ? versions : [{
      version: apiDoc.versionNumber != null ? `v${apiDoc.versionNumber}.0` : 'v1.0',
      versionNumber: apiDoc.versionNumber || 1,
      fileName: apiDoc.fileName || `${ref}.pdf`,
      fileSize: apiDoc.fileSize || '0 B',
      sha256Hash: apiDoc.fileHash || '',
      issueDate: apiDoc.issueDate ? String(apiDoc.issueDate) : '',
      expiryDate: apiDoc.expiryDate ? String(apiDoc.expiryDate) : '',
      uploadedBy: apiDoc.uploadedBy || 'Representative',
      uploadedAt: apiDoc.createdAt ? new Date(apiDoc.createdAt).toLocaleDateString('en-GB') : '',
      notes: apiDoc.notes || 'Initial registered filing.'
    }]
  };
};

const mapBackendAlertToFrontend = (apiAlert) => {
  const ref = apiAlert.alertReference || apiAlert.id || (apiAlert.backendId != null ? `ALT-${apiAlert.backendId}` : 'ALT-UNKNOWN');
  return {
    id: ref,
    backendId: apiAlert.backendId,
    alertReference: apiAlert.alertReference || ref,
    institutionId: apiAlert.institutionalIdentifier || (apiAlert.institutionId ? `INST-${apiAlert.institutionId}` : ''),
    institutionName: apiAlert.institutionName || 'Unassigned Institution',
    type: apiAlert.type || 'SLA Warning',
    category: apiAlert.category || 'Upcoming Deadlines',
    severity: apiAlert.severity || 'Medium',
    urgency: apiAlert.urgency || 'Due soon',
    message: apiAlert.message || '',
    requiredAction: apiAlert.requiredAction || '',
    relatedRecordId: apiAlert.relatedRecordId || '',
    recipient: apiAlert.recipient || 'Officer',
    channel: apiAlert.channel || 'IN_APP',
    deliveryStatus: apiAlert.deliveryStatus || 'Delivered',
    acknowledged: Boolean(apiAlert.acknowledged),
    acknowledgedBy: apiAlert.acknowledgedBy || null,
    acknowledgedAt: apiAlert.acknowledgedAt,
    escalated: Boolean(apiAlert.escalated),
    escalatedTo: apiAlert.escalatedTo || null,
    escalationReason: apiAlert.escalationReason || null,
    dueDate: apiAlert.dueDate ? String(apiAlert.dueDate) : null,
    createdAt: apiAlert.createdAt ? String(apiAlert.createdAt) : null,
    read: Boolean(apiAlert.acknowledged),
  };
};

export const DataProvider = ({ children }) => {
  const [applications, setApplications] = useState([]);
  const [institutions, setInstitutions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Live EVC, Compliance, Document, and Alert state populated from backend
  const [evc, setEvc] = useState([]);
  const [compliance, setCompliance] = useState([]);
  const [documents, setDocuments] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [activity, setActivity] = useState([]);

  // Fetch applications from Spring Boot / PostgreSQL backend
  const fetchApplicationsFromBackend = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await apiClient('/applications?size=100');
      const backendItems = data?.content || (Array.isArray(data) ? data : (data?.items || []));
      const mapped = backendItems.map(mapBackendApplicationToFrontend);
      setApplications(mapped);
      return mapped;
    } catch (err) {
      console.error('Failed to fetch applications from backend API:', err);
      setError(err.message || 'Unable to load applications from server.');
      setApplications([]);
      return [];
    } finally {
      setLoading(false);
    }
  }, []);

  // Fetch institutions from backend
  const fetchInstitutionsFromBackend = useCallback(async () => {
    try {
      const data = await apiClient('/institutions?size=100');
      const items = data?.content || (Array.isArray(data) ? data : []);
      setInstitutions(items);
      return items;
    } catch (err) {
      console.error('Failed to load institutions from backend:', err);
      setInstitutions([]);
      return [];
    }
  }, []);

  // Fetch EVC visits from backend
  const fetchEVCFromBackend = useCallback(async () => {
    try {
      const data = await apiClient('/evc');
      const items = Array.isArray(data) ? data : (data?.content || data?.items || []);
      const mapped = items.map(mapBackendEvcToFrontend);
      setEvc(mapped);
      return mapped;
    } catch (err) {
      console.error('Failed to load EVC visits from backend:', err);
      setEvc([]);
      return [];
    }
  }, []);

  // Fetch Compliance Obligations from backend
  const fetchComplianceFromBackend = useCallback(async () => {
    try {
      const data = await apiClient('/compliance?size=100');
      const items = Array.isArray(data) ? data : (data?.content || data?.items || []);
      const mapped = items.map(mapBackendComplianceToFrontend);
      setCompliance(mapped);
      return mapped;
    } catch (err) {
      console.error('Failed to load compliance obligations from backend:', err);
      setCompliance([]);
      return [];
    }
  }, []);

  // Fetch Documents from backend
  const fetchDocumentsFromBackend = useCallback(async () => {
    try {
      const data = await apiClient('/documents?size=100');
      const items = Array.isArray(data) ? data : (data?.content || data?.items || []);
      const mapped = items.map(mapBackendDocumentToFrontend);
      setDocuments(mapped);
      return mapped;
    } catch (err) {
      console.error('Failed to load documents from backend:', err);
      setDocuments([]);
      return [];
    }
  }, []);

  // Fetch Alerts from backend
  const fetchAlertsFromBackend = useCallback(async () => {
    try {
      const data = await apiClient('/alerts?size=100');
      const items = Array.isArray(data) ? data : (data?.content || data?.items || []);
      const mapped = items.map(mapBackendAlertToFrontend);
      setAlerts(mapped);
      return mapped;
    } catch (err) {
      console.error('Failed to load alerts from backend:', err);
      setAlerts([]);
      return [];
    }
  }, []);

  useEffect(() => {
    fetchApplicationsFromBackend();
    fetchInstitutionsFromBackend();
    fetchEVCFromBackend();
    fetchComplianceFromBackend();
    fetchDocumentsFromBackend();
    fetchAlertsFromBackend();
  }, [
    fetchApplicationsFromBackend,
    fetchInstitutionsFromBackend,
    fetchEVCFromBackend,
    fetchComplianceFromBackend,
    fetchDocumentsFromBackend,
    fetchAlertsFromBackend,
  ]);

  const getApplicationById = (id) => {
    if (!id) return null;
    return applications.find((a) => a.id === id || a.applicationReference === id || String(a.backendId) === String(id)) || null;
  };

  /**
   * Core Functional Workflow Transition Engine
   * Connected directly to POST /api/v1/applications/{id}/actions
   */
  const performWorkflowAction = async (actionType, payload) => {
    const {
      applicationId,
      actorName,
      actorRole,
      reason,
      observations,
      newDeficiencies,
      requirementReference,
      nextStage,
      deficiencyId,
      resolutionStatus,
      resolutionRemarks,
      checklistItemCode,
      checklistItemVerified,
    } = payload;
    const targetApp = applications.find((a) => a.id === applicationId || a.applicationReference === applicationId || String(a.backendId) === String(applicationId));

    if (!targetApp) {
      return { success: false, error: 'Application not found in current store.' };
    }

    const idOrRef = targetApp.backendId || targetApp.applicationReference || targetApp.id;

    try {
      const apiResponse = await apiClient(`/applications/${idOrRef}/actions`, {
        method: 'POST',
        body: JSON.stringify({
          action: actionType,
          actorName: actorName || 'Authorized Officer',
          actorRole: actorRole || 'Scrutiny Officer',
          reason: reason || observations || 'Workflow state transition',
          observations: observations || '',
          deficiencies: newDeficiencies || targetApp.deficiencies || [],
          requirementReference,
          nextStage: nextStage || targetApp.currentStage,
          deficiencyId,
          resolutionStatus,
          resolutionRemarks,
          checklistItemCode,
          checklistItemVerified,
        }),
      });

      if (apiResponse && (apiResponse.id != null || apiResponse.applicationReference)) {
        const updatedMapped = mapBackendApplicationToFrontend(apiResponse);
        setApplications((prev) =>
          prev.map((app) =>
            app.id === targetApp.id || app.applicationReference === targetApp.applicationReference
              ? { ...app, ...updatedMapped }
              : app
          )
        );

        // Record to real activity audit trail
        const now = new Date();
        const timestampStr = `${now.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })} ${now.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })}`;
        setActivity((prev) => [
          {
            id: `ACT-${Date.now().toString().slice(-4)}`,
            timestamp: timestampStr,
            actor: `${actorName} (${actorRole})`,
            action: actionType,
            details: reason || observations || `Transitioned to ${nextStage || updatedMapped.currentStage}`,
            relatedRecordId: targetApp.id,
            institutionId: targetApp.institutionId,
            category: 'Workflow',
          },
          ...prev,
        ]);

        if (actionType === 'COMPLETE_SCRUTINY') {
          await fetchEVCFromBackend();
        }

        return { success: true, application: updatedMapped };
      }

      // If backend succeeded but returned without body, reload
      await Promise.all([fetchApplicationsFromBackend(), fetchEVCFromBackend()]);
      return { success: true };
    } catch (err) {
      console.error(`Workflow action ${actionType} failed on backend:`, err);
      return {
        success: false,
        error: err.data?.detail || err.message || 'Workflow transition failed on backend server.',
      };
    }
  };

  // EVC actions connected to Spring Boot REST endpoints
  const scheduleEVC = async (arg1, arg2) => {
    const payload = typeof arg1 === 'object' && arg1 !== null && !arg2 ? arg1 : { ...(arg2 || {}), evcId: arg1 };
    const evcId = payload.evcId || payload.id;
    if (!evcId) {
      return { success: false, error: 'EVC identifier is required for scheduling.' };
    }

    try {
      const response = await apiClient(`/evc/${encodeURIComponent(evcId)}/schedule`, {
        method: 'POST',
        body: JSON.stringify({
          visitDate: payload.visitDate,
          coordinator: payload.coordinator,
          inspectionType: payload.inspectionType,
          experts: Array.isArray(payload.experts) ? payload.experts : [],
          actorName: payload.actorName || 'C. Verma',
          actorRole: payload.actorRole || 'EVC Coordinator',
        }),
      });

      const updated = mapBackendEvcToFrontend(response);
      setEvc((prev) => prev.map((item) => (item.id === updated.id || item.evcReference === updated.evcReference ? updated : item)));

      // Refresh applications list
      fetchApplicationsFromBackend();

      const now = new Date();
      const timestampStr = `${now.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })} ${now.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })}`;
      setActivity((prev) => [
        {
          id: `ACT-${Date.now().toString().slice(-4)}`,
          timestamp: timestampStr,
          actor: `${payload.actorName || 'C. Verma'} (${payload.actorRole || 'EVC Coordinator'})`,
          action: 'EVC_SCHEDULED',
          details: `Inspection visit for ${updated.institutionName} scheduled for ${updated.visitDate}.`,
          relatedRecordId: updated.id,
          category: 'EVC Operations',
        },
        ...prev,
      ]);

      return { success: true, visit: updated };
    } catch (err) {
      console.error('Failed to schedule EVC visit:', err);
      return {
        success: false,
        error: err.data?.detail || err.message || 'Failed to schedule EVC visit on backend server.',
      };
    }
  };

  const markEVCCompleted = async (arg1, arg2) => {
    const payload = typeof arg1 === 'object' && arg1 !== null && !arg2 ? arg1 : { ...(arg2 || {}), evcId: arg1 };
    const evcId = payload.evcId || payload.id;
    if (!evcId) {
      return { success: false, error: 'EVC identifier is required to complete visit.' };
    }

    try {
      const response = await apiClient(`/evc/${encodeURIComponent(evcId)}/complete`, {
        method: 'POST',
        body: JSON.stringify({
          completionNotes: payload.completionNotes || 'Inspection concluded successfully.',
          actorName: payload.actorName || 'C. Verma',
          actorRole: payload.actorRole || 'EVC Coordinator',
        }),
      });

      const updated = mapBackendEvcToFrontend(response);
      setEvc((prev) => prev.map((item) => (item.id === updated.id || item.evcReference === updated.evcReference ? updated : item)));

      const now = new Date();
      const timestampStr = `${now.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })} ${now.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })}`;
      setActivity((prev) => [
        {
          id: `ACT-${Date.now().toString().slice(-4)}`,
          timestamp: timestampStr,
          actor: `${payload.actorName || 'C. Verma'} (${payload.actorRole || 'EVC Coordinator'})`,
          action: 'EVC_VISIT_COMPLETED',
          details: `Inspection visit concluded for ${updated.institutionName}. Inspection report pending.`,
          relatedRecordId: updated.id,
          category: 'EVC Operations',
        },
        ...prev,
      ]);

      return { success: true, visit: updated };
    } catch (err) {
      console.error('Failed to mark EVC visit completed:', err);
      return {
        success: false,
        error: err.data?.detail || err.message || 'Failed to record visit completion on backend.',
      };
    }
  };

  const submitEVCReport = async (arg1, arg2) => {
    const payload = typeof arg1 === 'object' && arg1 !== null && !arg2 ? arg1 : { ...(arg2 || {}), evcId: arg1 };
    const evcId = payload.evcId || payload.id;
    if (!evcId) {
      return { success: false, error: 'EVC identifier is required to submit report.' };
    }

    try {
      const response = await apiClient(`/evc/${encodeURIComponent(evcId)}/report`, {
        method: 'POST',
        body: JSON.stringify({
          reportOutcome: payload.reportOutcome || 'Recommended Approval',
          reportNotes: payload.reportNotes || '',
          actorName: payload.actorName || 'C. Verma',
          actorRole: payload.actorRole || 'EVC Coordinator',
        }),
      });

      const updated = mapBackendEvcToFrontend(response);
      setEvc((prev) => prev.map((item) => (item.id === updated.id || item.evcReference === updated.evcReference ? updated : item)));

      // Re-fetch applications because linked application advanced to 'Committee review'
      await fetchApplicationsFromBackend();

      const now = new Date();
      const timestampStr = `${now.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })} ${now.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })}`;
      setActivity((prev) => [
        {
          id: `ACT-${Date.now().toString().slice(-4)}`,
          timestamp: timestampStr,
          actor: `${payload.actorName || 'C. Verma'} (${payload.actorRole || 'EVC Coordinator'})`,
          action: 'EVC_REPORT_SUBMITTED',
          details: `EVC report submitted for ${updated.institutionName} (${updated.reportOutcome}). Application advanced to Committee Review.`,
          relatedRecordId: updated.id,
          category: 'EVC Operations',
        },
        ...prev,
      ]);

      return { success: true, visit: updated };
    } catch (err) {
      console.error('Failed to submit EVC report:', err);
      return {
        success: false,
        error: err.data?.detail || err.message || 'Failed to submit inspection report on backend.',
      };
    }
  };

  const rescheduleEVC = async (arg1, arg2) => {
    const payload = typeof arg1 === 'object' && arg1 !== null && !arg2 ? arg1 : { ...(arg2 || {}), evcId: arg1 };
    const evcId = payload.evcId || payload.id;
    if (!evcId) {
      return { success: false, error: 'EVC identifier is required to reschedule.' };
    }

    try {
      const response = await apiClient(`/evc/${encodeURIComponent(evcId)}/reschedule`, {
        method: 'POST',
        body: JSON.stringify({
          newVisitDate: payload.visitDate || payload.newVisitDate || null,
          actorName: payload.actorName || 'C. Verma',
          actorRole: payload.actorRole || 'EVC Coordinator',
        }),
      });

      const updated = mapBackendEvcToFrontend(response);
      setEvc((prev) => prev.map((item) => (item.id === updated.id || item.evcReference === updated.evcReference ? updated : item)));

      return { success: true, visit: updated };
    } catch (err) {
      console.error('Failed to reschedule EVC:', err);
      return {
        success: false,
        error: err.data?.detail || err.message || 'Failed to reschedule EVC visit on backend.',
      };
    }
  };

  // Compliance actions connected to Spring Boot REST endpoints
  const submitComplianceEvidence = async (arg1, arg2) => {
    const payload = typeof arg1 === 'object' && arg1 !== null && !arg2 ? arg1 : { ...(arg2 || {}), obligationId: arg1 };
    const obligationId = payload.obligationId || payload.id;
    if (!obligationId) {
      return { success: false, error: 'Compliance obligation identifier is required.' };
    }
    try {
      const response = await apiClient(`/compliance/${encodeURIComponent(obligationId)}/submit`, {
        method: 'POST',
        body: JSON.stringify({
          evidenceFile: payload.evidenceFile || payload.fileName || `${obligationId}_evidence.pdf`,
          notes: payload.notes || payload.evidenceNotes || '',
          actorName: payload.actorName || 'Authorized Representative',
          actorRole: payload.actorRole || 'Institution Representative',
        }),
      });
      const updated = mapBackendComplianceToFrontend(response);
      setCompliance((prev) => prev.map((item) => (item.id === updated.id || item.obligationReference === updated.obligationReference ? updated : item)));
      return { success: true, compliance: updated };
    } catch (err) {
      console.error('Failed to submit compliance evidence:', err);
      return {
        success: false,
        error: err.data?.detail || err.message || 'Failed to submit compliance evidence on backend.',
      };
    }
  };

  const markComplianceUnderVerification = async (arg1, arg2) => {
    const payload = typeof arg1 === 'object' && arg1 !== null && !arg2 ? arg1 : { ...(arg2 || {}), obligationId: arg1 };
    const obligationId = payload.obligationId || payload.id;
    if (!obligationId) {
      return { success: false, error: 'Compliance obligation identifier is required.' };
    }
    try {
      const response = await apiClient(`/compliance/${encodeURIComponent(obligationId)}/under-verification`, {
        method: 'POST',
      });
      const updated = mapBackendComplianceToFrontend(response);
      setCompliance((prev) => prev.map((item) => (item.id === updated.id || item.obligationReference === updated.obligationReference ? updated : item)));
      return { success: true, compliance: updated };
    } catch (err) {
      console.error('Failed to mark compliance under verification:', err);
      return {
        success: false,
        error: err.data?.detail || err.message || 'Failed to transition compliance on backend.',
      };
    }
  };

  const verifyComplianceEvidence = async (arg1, arg2) => {
    const payload = typeof arg1 === 'object' && arg1 !== null && !arg2 ? arg1 : { ...(arg2 || {}), obligationId: arg1 };
    const obligationId = payload.obligationId || payload.id;
    if (!obligationId) {
      return { success: false, error: 'Compliance obligation identifier is required.' };
    }
    try {
      const response = await apiClient(`/compliance/${encodeURIComponent(obligationId)}/verify`, {
        method: 'POST',
        body: JSON.stringify({
          outcome: payload.outcome || payload.verificationOutcome || 'Verified compliant',
          remarks: payload.remarks || payload.verificationRemarks || '',
          actorName: payload.actorName || 'Compliance Reviewer',
          actorRole: payload.actorRole || 'Compliance Reviewer',
        }),
      });
      const updated = mapBackendComplianceToFrontend(response);
      setCompliance((prev) => prev.map((item) => (item.id === updated.id || item.obligationReference === updated.obligationReference ? updated : item)));
      return { success: true, compliance: updated };
    } catch (err) {
      console.error('Failed to record compliance verification:', err);
      return {
        success: false,
        error: err.data?.detail || err.message || 'Failed to record compliance decision on backend.',
      };
    }
  };

  // Document actions connected to Spring Boot REST endpoints & durable storage
  const uploadDocument = async (payload) => {
    try {
      if (!payload.file || !(payload.file instanceof Blob) || payload.file.size === 0) {
        return { success: false, error: 'Choose a real, non-empty PDF or image file before uploading.' };
      }
      const formData = new FormData();
      formData.append('file', payload.file, payload.fileName || payload.file.name || 'statutory_document.pdf');
      formData.append('documentType', payload.documentType || payload.category || 'Statutory Instrument');
      formData.append('category', payload.category || payload.documentType || 'Statutory Instrument');
      formData.append('issueDate', payload.issueDate);
      formData.append('expiryDate', payload.expiryDate);
      if (payload.notes) formData.append('notes', payload.notes);

      const suppliedInstitutionId = payload.institutionId == null ? '' : String(payload.institutionId).trim();
      // Only numeric database IDs belong here; INST-xxxx is resolved using the authenticated user.
      if (/^\d+$/.test(suppliedInstitutionId)) {
        formData.append('institutionId', suppliedInstitutionId);
      }

      const response = await apiClient('/documents/upload', {
        method: 'POST',
        body: formData,
      });

      const updated = mapBackendDocumentToFrontend(response);
      setDocuments((prev) => [updated, ...prev]);
      return { success: true, document: updated };
    } catch (err) {
      console.error('Failed to upload statutory document:', err);
      return {
        success: false,
        error: err.data?.detail || err.message || 'Failed to upload document to backend storage.',
      };
    }
  };

  const submitDocumentRenewal = async (arg1, arg2) => {
    const payload = typeof arg1 === 'object' && arg1 !== null && !arg2 ? arg1 : { ...(arg2 || {}), documentId: arg1 };
    const docId = payload.documentId || payload.id;
    if (!docId) {
      return { success: false, error: 'Document identifier is required for renewal.' };
    }
    try {
      if (!payload.file || !(payload.file instanceof Blob) || payload.file.size === 0) {
        return { success: false, error: 'Choose a real, non-empty PDF or image file before submitting a renewal.' };
      }
      const formData = new FormData();
      formData.append('file', payload.file, payload.fileName || payload.file.name || (docId + '_renewal.pdf'));
      // Match the backend controller parameter names.
      formData.append('issueDate', payload.newIssueDate);
      formData.append('expiryDate', payload.newExpiryDate);
      if (payload.notes) formData.append('notes', payload.notes);

      const response = await apiClient(`/documents/${encodeURIComponent(docId)}/renew`, {
        method: 'POST',
        body: formData,
      });

      const updated = mapBackendDocumentToFrontend(response);
      setDocuments((prev) => prev.map((item) => (item.id === updated.id || item.documentReference === updated.documentReference ? updated : item)));
      return { success: true, document: updated };
    } catch (err) {
      console.error('Failed to submit document renewal:', err);
      return {
        success: false,
        error: err.data?.detail || err.message || 'Failed to submit renewal to backend storage.',
      };
    }
  };

  const updateDocumentVerification = async (arg1, arg2) => {
    const payload = typeof arg1 === 'object' && arg1 !== null && !arg2 ? arg1 : { ...(arg2 || {}), documentId: arg1 };
    const docId = payload.documentId || payload.id;
    if (!docId) {
      return { success: false, error: 'Document identifier is required for verification.' };
    }
    try {
      const response = await apiClient(`/documents/${encodeURIComponent(docId)}/verify`, {
        method: 'POST',
        body: JSON.stringify({
          verificationStatus: payload.verificationStatus || 'Verified',
          remarks: payload.remarks || '',
          actorName: payload.actorName || 'Compliance Reviewer',
          actorRole: payload.actorRole || 'Compliance Reviewer',
        }),
      });

      const updated = mapBackendDocumentToFrontend(response);
      setDocuments((prev) => prev.map((item) => (item.id === updated.id || item.documentReference === updated.documentReference ? updated : item)));
      return { success: true, document: updated };
    } catch (err) {
      console.error('Failed to update document verification:', err);
      return {
        success: false,
        error: err.data?.detail || err.message || 'Failed to update document verification on backend.',
      };
    }
  };

  const downloadDocumentFile = async (docId, fileName) => {
    try {
      const token = localStorage.getItem('aicte_auth_token');
      const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';
      const url = `${API_BASE_URL}/documents/${encodeURIComponent(docId)}/download`;
      const res = await fetch(url, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (!res.ok) {
        throw new Error(`Download failed with status ${res.status}`);
      }
      const blob = await res.blob();
      const blobUrl = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = blobUrl;
      a.download = fileName || `${docId}.pdf`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(blobUrl);
      return { success: true };
    } catch (err) {
      console.error('Failed to download document:', err);
      return { success: false, error: err.message || 'Failed to download document from server.' };
    }
  };

  // Alert actions connected to Spring Boot REST endpoints
  const acknowledgeAlert = async (arg1, arg2) => {
    const payload = typeof arg1 === 'object' && arg1 !== null && !arg2 ? arg1 : { ...(arg2 || {}), alertId: arg1 };
    const alertId = payload.alertId || payload.id;
    if (!alertId) {
      return { success: false, error: 'Alert identifier is required.' };
    }
    try {
      const response = await apiClient(`/alerts/${encodeURIComponent(alertId)}/acknowledge`, {
        method: 'POST',
      });
      const updated = mapBackendAlertToFrontend(response);
      setAlerts((prev) => prev.map((item) => (item.id === updated.id || item.alertReference === updated.alertReference ? updated : item)));
      return { success: true, alert: updated };
    } catch (err) {
      console.error('Failed to acknowledge alert:', err);
      return {
        success: false,
        error: err.data?.detail || err.message || 'Failed to acknowledge alert on backend.',
      };
    }
  };

  const simulateEscalation = async (arg1, arg2) => {
    const payload = typeof arg1 === 'object' && arg1 !== null && !arg2 ? arg1 : { ...(arg2 || {}), alertId: arg1 };
    const alertId = payload.alertId || payload.id;
    if (!alertId) {
      return { success: false, error: 'Alert identifier is required for escalation.' };
    }
    try {
      const response = await apiClient(`/alerts/${encodeURIComponent(alertId)}/escalate`, {
        method: 'POST',
        body: JSON.stringify({
          reason: payload.reason || 'Handbook turnaround SLA breached beyond allowable buffer.',
          actorName: payload.actorName || 'Regional Officer',
          actorRole: payload.actorRole || 'Regional Officer',
        }),
      });
      const updated = mapBackendAlertToFrontend(response);
      setAlerts((prev) => prev.map((item) => (item.id === updated.id || item.alertReference === updated.alertReference ? updated : item)));
      return { success: true, alert: updated };
    } catch (err) {
      console.error('Failed to escalate alert:', err);
      return {
        success: false,
        error: err.data?.detail || err.message || 'Failed to escalate alert on backend.',
      };
    }
  };

  const markAlertRead = (arg1) => {
    const alertId = typeof arg1 === 'object' && arg1 !== null ? (arg1.alertId || arg1.id) : arg1;
    setAlerts((prev) =>
      prev.map((alt) => (alt.id === alertId ? { ...alt, read: true } : alt))
    );
  };

  const retryNotification = async (arg1) => {
    const alertId = typeof arg1 === 'object' && arg1 !== null ? (arg1.alertId || arg1.id) : arg1;
    if (!alertId) return { success: false, error: 'Alert identifier is required.' };
    try {
      const response = await apiClient(`/alerts/${encodeURIComponent(alertId)}/retry`, {
        method: 'POST',
      });
      const updated = mapBackendAlertToFrontend(response);
      setAlerts((prev) => prev.map((item) => (item.id === updated.id || item.alertReference === updated.alertReference ? updated : item)));
      return { success: true, alert: updated };
    } catch (err) {
      console.warn('Backend retry failed, applying optimistic update:', err);
      setAlerts((prev) =>
        prev.map((alt) => (alt.id === alertId ? { ...alt, deliveryStatus: 'Delivered' } : alt))
      );
      return { success: false, error: err.message };
    }
  };

  const generateSystemAlerts = async () => {
    const refreshed = await fetchAlertsFromBackend();
    return { success: true, count: refreshed.length };
  };

  const refreshAllData = async () => {
    await Promise.all([
      fetchApplicationsFromBackend(),
      fetchInstitutionsFromBackend(),
      fetchEVCFromBackend(),
      fetchComplianceFromBackend(),
      fetchDocumentsFromBackend(),
      fetchAlertsFromBackend(),
    ]);
  };

  return (
    <DataContext.Provider
      value={{
        applications,
        institutions,
        loading,
        error,
        evc,
        compliance,
        documents,
        alerts,
        activity,
        fetchApplicationsFromBackend,
        fetchInstitutionsFromBackend,
        fetchEVCFromBackend,
        fetchComplianceFromBackend,
        fetchDocumentsFromBackend,
        fetchAlertsFromBackend,
        refreshAllData,
        getApplicationById,
        performWorkflowAction,
        scheduleEVC,
        markEVCCompleted,
        submitEVCReport,
        rescheduleEVC,
        submitComplianceEvidence,
        markComplianceUnderVerification,
        verifyComplianceEvidence,
        uploadDocument,
        submitDocumentRenewal,
        updateDocumentVerification,
        downloadDocumentFile,
        markAlertRead,
        acknowledgeAlert,
        simulateEscalation,
        retryNotification,
        generateSystemAlerts,
      }}
    >
      {children}
    </DataContext.Provider>
  );
};

export const useData = () => {
  const context = useContext(DataContext);
  if (!context) {
    throw new Error('useData must be used within a DataProvider');
  }
  return context;
};
