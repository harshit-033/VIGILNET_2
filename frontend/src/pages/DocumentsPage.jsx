// SIH 26262 - Document Registry & Renewal Module (/documents)

import React, { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useData } from '../context/DataContext';
import { ROLES } from '../constants/roles';
import { KPICard } from '../components/common/KPICard';
import { StatusBadge } from '../components/common/StatusBadge';
import { EmptyState } from '../components/common/EmptyState';
import { Modal } from '../components/common/Modal';
import {
  FolderArchive,
  FileText,
  CheckCircle2,
  Clock,
  AlertTriangle,
  Upload,
  Eye,
  RefreshCw,
  Search,
  FileCheck,
  ShieldCheck,
  History,
  Calendar,
  Building2,
  Info,
  ExternalLink,
  Check,
} from 'lucide-react';

export const DocumentsPage = () => {
  const { user, role } = useAuth();
  const {
    documents,
    uploadDocument,
    submitDocumentRenewal,
    updateDocumentVerification,
    downloadDocumentFile,
  } = useData();

  const isInstitutionRep = role === ROLES.INSTITUTION_REP;
  const isAuthorizedReviewer = [
    ROLES.COMPLIANCE_REVIEWER,
    ROLES.REGIONAL_OFFICER,
    ROLES.SENIOR_MANAGEMENT,
    ROLES.ADMINISTRATOR,
  ].includes(role);

  // Filter & Search states
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Modals state
  const [selectedDoc, setSelectedDoc] = useState(null);

  // 1. Upload Document Modal
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [uploadCategory, setUploadCategory] = useState('State Fire Service Safety NOC');
  const [uploadIssueDate, setUploadIssueDate] = useState('2026-10-01');
  const [uploadExpiryDate, setUploadExpiryDate] = useState('2028-09-30');
  const [uploadNotes, setUploadNotes] = useState('');
  const [uploadFileName, setUploadFileName] = useState('');
  const [uploadFileBlobUrl, setUploadFileBlobUrl] = useState(null);
  const [uploadFileType, setUploadFileType] = useState('');
  const [uploadFile, setUploadFile] = useState(null);
  const [isUploading, setIsUploading] = useState(false);

  // 2. Submit Renewal Modal
  const [isRenewalModalOpen, setIsRenewalModalOpen] = useState(false);
  const [renewalIssueDate, setRenewalIssueDate] = useState('2026-10-23');
  const [renewalExpiryDate, setRenewalExpiryDate] = useState('2029-10-22');
  const [renewalNotes, setRenewalNotes] = useState('');
  const [renewalFileName, setRenewalFileName] = useState('');
  const [renewalFileBlobUrl, setRenewalFileBlobUrl] = useState(null);
  const [renewalFile, setRenewalFile] = useState(null);

  // 3. Document Details & Preview Modal
  const [isDetailsModalOpen, setIsDetailsModalOpen] = useState(false);
  const [detailsTab, setDetailsTab] = useState('preview'); // 'preview' | 'versions'

  // 4. Update Verification Modal (Reviewer)
  const [isVerifyModalOpen, setIsVerifyModalOpen] = useState(false);
  const [verifyStatus, setVerifyStatus] = useState('Verified');
  const [verifyRemarks, setVerifyRemarks] = useState('');

  // Base Scoping: If Institution Rep, show only their college's records
  const baseDocuments = useMemo(() => {
    if (isInstitutionRep) {
      if (user?.institutionId) {
        return documents.filter((d) => d.institutionId === user.institutionId);
      }
      return documents;
    }
    return documents;
  }, [documents, isInstitutionRep, user]);

  // Search & Filter
  const filteredDocuments = useMemo(() => {
    return baseDocuments.filter((doc) => {
      // 1. Search Query
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchesQ =
          doc.id.toLowerCase().includes(q) ||
          (doc.documentType && doc.documentType.toLowerCase().includes(q)) ||
          (doc.category && doc.category.toLowerCase().includes(q)) ||
          doc.institutionName.toLowerCase().includes(q) ||
          (doc.reviewer && doc.reviewer.toLowerCase().includes(q)) ||
          (doc.fileName && doc.fileName.toLowerCase().includes(q));
        if (!matchesQ) return false;
      }

      // 2. Status Filter
      if (statusFilter !== 'ALL') {
        if (statusFilter === 'VERIFIED' && doc.verificationStatus !== 'Verified') return false;
        if (statusFilter === 'EXPIRING_SOON' && doc.verificationStatus !== 'Expiring soon' && doc.validityStatus !== 'Due soon') return false;
        if (statusFilter === 'EXPIRED' && doc.verificationStatus !== 'Expired / no verified renewal' && doc.validityStatus !== 'Overdue') return false;
        if (statusFilter === 'PENDING' && doc.verificationStatus !== 'Verification pending' && doc.verificationStatus !== 'Renewal submitted') return false;
        if (statusFilter === 'RENEWAL_SUBMITTED' && doc.verificationStatus !== 'Renewal submitted') return false;
      }

      return true;
    });
  }, [baseDocuments, searchQuery, statusFilter]);

  // KPI calculations
  const kpiTotal = baseDocuments.length;
  const kpiVerified = baseDocuments.filter((d) => d.verificationStatus === 'Verified').length;
  const kpiExpiringSoon = baseDocuments.filter(
    (d) => d.verificationStatus === 'Expiring soon' || d.validityStatus === 'Due soon'
  ).length;
  const kpiExpired = baseDocuments.filter(
    (d) => d.verificationStatus === 'Expired / no verified renewal' || d.validityStatus === 'Overdue'
  ).length;
  const kpiPendingVerification = baseDocuments.filter(
    (d) => d.verificationStatus === 'Verification pending' || d.verificationStatus === 'Renewal submitted'
  ).length;

  // Handlers for File Input with Temporary Browser Memory Preview & Backend Durable Upload
  const handleFileUploadChange = (e, isRenewal = false) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const fileUrl = URL.createObjectURL(file);
    if (isRenewal) {
      setRenewalFileName(file.name);
      setRenewalFileBlobUrl(fileUrl);
      setRenewalFile(file);
    } else {
      setUploadFileName(file.name);
      setUploadFileBlobUrl(fileUrl);
      setUploadFileType(file.type);
      setUploadFile(file);
    }
  };

  // Open Upload Modal
  const handleOpenUploadModal = () => {
    setUploadCategory('State Fire Service Safety NOC');
    setUploadIssueDate('2026-10-01');
    setUploadExpiryDate('2028-09-30');
    setUploadNotes('');
    setUploadFileName('');
    setUploadFileBlobUrl(null);
    setUploadFileType('');
    setUploadFile(null);
    setIsUploadModalOpen(true);
  };

  // Confirm Upload: never fabricate a placeholder document; store the selected file.
  const handleConfirmUpload = async (e) => {
    e.preventDefault();
    if (!uploadFile || uploadFile.size === 0) { alert('Select a real PDF, PNG, or JPEG file before uploading.'); return; }
    if (uploadFile.size > 25 * 1024 * 1024) { alert('The selected document exceeds the 25 MB upload limit.'); return; }
    if (!uploadCategory) { alert('Please select a valid statutory document type.'); return; }
    if (!uploadIssueDate || !uploadExpiryDate) { alert('Both issue date and expiry date are mandatory.'); return; }
    if (new Date(uploadIssueDate) > new Date(uploadExpiryDate)) { alert('Issue Date cannot be later than Expiry Date.'); return; }
    setIsUploading(true);
    try {
      const res = await uploadDocument({
        institutionId: isInstitutionRep ? (user?.institutionId || '') : '',
        institutionName: isInstitutionRep ? (user?.institutionName || '') : '',
        category: uploadCategory, documentType: uploadCategory,
        issueDate: uploadIssueDate, expiryDate: uploadExpiryDate,
        fileName: uploadFile.name, fileBlobUrl: uploadFileBlobUrl, file: uploadFile,
        notes: uploadNotes, actorName: user?.name, actorRole: role,
      });
      if (res.success) { setIsUploadModalOpen(false); alert(`Document ${res.document.id} uploaded. OCR status: ${res.document.ocrStatus || 'PENDING'}.`); }
      else { alert(res.error); }
    } finally { setIsUploading(false); }
  };

  // Open Renewal Modal
  const handleOpenRenewalModal = (doc) => {
    setSelectedDoc(doc);
    setRenewalIssueDate('2026-10-23');
    // Default to +3 years
    setRenewalExpiryDate('2029-10-22');
    setRenewalNotes('');
    setRenewalFileName('');
    setRenewalFileBlobUrl(null);
    setRenewalFile(null);
    setIsRenewalModalOpen(true);
  };

  // Confirm Renewal
  const handleConfirmRenewal = async (e) => {
    e.preventDefault();
    if (!renewalIssueDate || !renewalExpiryDate) {
      alert('Both issue date and renewal expiry date are mandatory.');
      return;
    }
    if (new Date(renewalIssueDate) > new Date(renewalExpiryDate)) {
      alert('Validation Error: Renewal Issue Date cannot be later than Renewal Expiry Date.');
      return;
    }

    const res = await submitDocumentRenewal({
      documentId: selectedDoc.id,
      newIssueDate: renewalIssueDate,
      newExpiryDate: renewalExpiryDate,
      fileName: renewalFileName,
      fileBlobUrl: renewalFileBlobUrl,
      file: renewalFile,
      notes: renewalNotes,
      actorName: user?.name,
      actorRole: role,
    });

    if (res.success) {
      setIsRenewalModalOpen(false);
      alert(`Renewal filing for ${selectedDoc.id} (${selectedDoc.category}) submitted as ${res.document.currentVersion}.`);
    } else {
      alert(res.error);
    }
  };

  // Open Details Modal
  const handleOpenDetailsModal = (doc) => {
    setSelectedDoc(doc);
    setDetailsTab('preview');
    setIsDetailsModalOpen(true);
  };

  // Open Reviewer Verification Modal
  const handleOpenVerifyModal = (doc) => {
    setSelectedDoc(doc);
    setVerifyStatus(doc.verificationStatus === 'Renewal submitted' ? 'Verified' : 'Verified');
    setVerifyRemarks('');
    setIsVerifyModalOpen(true);
  };

  // Confirm Verification Decision
  const handleConfirmVerification = async (e) => {
    e.preventDefault();

    let validity = 'Valid';
    if (verifyStatus === 'Expired / no verified renewal') validity = 'Overdue';
    else if (verifyStatus === 'Expiring soon') validity = 'Due soon';

    const res = await updateDocumentVerification({
      documentId: selectedDoc.id,
      verificationStatus: verifyStatus,
      validityStatus: validity,
      remarks: verifyRemarks,
      actorName: user?.name,
      actorRole: role,
    });

    if (res.success) {
      setIsVerifyModalOpen(false);
      alert(`Document ${selectedDoc.id} verification status recorded as "${verifyStatus}".`);
    } else {
      alert(res.error);
    }
  };

  return (
    <div>
      {/* Header Bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '16px', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <div style={{ fontSize: '10px', textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--color-teal)', fontWeight: '800' }}>
            STATUTORY COMPLIANCE REPOSITORY · APH NORMS
          </div>
          <h1 style={{ fontSize: '22px', fontWeight: '800', color: 'var(--color-navy)', margin: '4px 0 2px' }}>
            Document Registry &amp; Statutory Renewals
          </h1>
          <p style={{ color: 'var(--color-text-muted)', fontSize: 'var(--font-size-sm)' }}>
            Central repository of mandatory legal instruments, safety certificates, land deeds, and multi-version renewal lifecycles.
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <button onClick={handleOpenUploadModal} className="btn btn-primary" type="button">
            <Upload size={14} /> Upload Document &amp; Extract Text
          </button>
        </div>
      </div>

      {/* Temporary Memory Preview Advisory Notice */}
      <div
        style={{
          background: '#F0F7FD',
          border: '1px solid #C4DFF6',
          borderRadius: 'var(--radius-sm)',
          padding: '10px 14px',
          marginBottom: '16px',
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
          fontSize: 'var(--font-size-xs)',
          color: 'var(--color-navy)',
        }}
      >
        <ShieldCheck size={16} style={{ color: 'var(--color-teal)', flexShrink: 0 }} />
        <div>
          <strong>Statutory Depository Active:</strong> Documents are persisted to PostgreSQL with durable file storage and SHA-256 cryptographic proof hashes. Multi-version revisions and legal validity lifecycles are strictly tracked.
        </div>
      </div>

      {/* 5 KPI Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '14px', marginBottom: '18px' }}>
        <KPICard
          title="Total Documents"
          value={kpiTotal}
          subtitle="Monitored legal instruments"
          iconName="FolderArchive"
          tint="blue"
          onClick={() => setStatusFilter('ALL')}
        />
        <KPICard
          title="Verified Documents"
          value={kpiVerified}
          subtitle="Sealed & certified active"
          iconName="CheckCircle2"
          tint="green"
          onClick={() => setStatusFilter('VERIFIED')}
        />
        <KPICard
          title="Expiring Soon"
          value={kpiExpiringSoon}
          subtitle="Within 30-day renewal window"
          iconName="Clock"
          tint="amber"
          onClick={() => setStatusFilter('EXPIRING_SOON')}
        />
        <KPICard
          title="Expired Documents"
          value={kpiExpired}
          subtitle="Lapsed without certified renewal"
          iconName="AlertTriangle"
          tint="red"
          onClick={() => setStatusFilter('EXPIRED')}
        />
        <KPICard
          title="Pending Verification"
          value={kpiPendingVerification}
          subtitle="Awaiting desk review"
          iconName="FileCheck"
          tint="purple"
          onClick={() => setStatusFilter('PENDING')}
        />
      </div>

      {/* Toolbar: Search & Quick Status Filters */}
      <div className="panel" style={{ marginBottom: '16px', padding: '12px 16px', background: '#FCFDFE', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
        <div style={{ display: 'flex', gap: '12px', alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ position: 'relative', width: '300px' }}>
            <input
              type="text"
              className="form-input"
              placeholder="Search by ID, Document Type, College..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{ paddingLeft: '32px' }}
            />
            <Search size={15} style={{ position: 'absolute', left: '10px', top: '10px', color: '#899BAA' }} />
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: '600', color: 'var(--color-text-muted)' }}>
              Status Filter:
            </span>
            <select
              className="form-select"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              style={{ width: '180px', padding: '6px 10px', fontSize: 'var(--font-size-xs)' }}
            >
              <option value="ALL">All Documents</option>
              <option value="VERIFIED">Verified</option>
              <option value="PENDING">Pending Verification</option>
              <option value="EXPIRING_SOON">Expiring Soon</option>
              <option value="EXPIRED">Expired / Overdue</option>
              <option value="RENEWAL_SUBMITTED">Renewal Submitted</option>
            </select>
          </div>
        </div>

        <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
          Showing <strong>{filteredDocuments.length}</strong> of {baseDocuments.length} documents
        </div>
      </div>

      {/* Table Panel */}
      <div className="panel" style={{ padding: 0 }}>
        <div className="panel-body" style={{ padding: 0 }}>
          {filteredDocuments.length === 0 ? (
            <EmptyState message="No documents match your search and status filter." />
          ) : (
            <div className="table-container" style={{ border: 'none' }}>
              <table className="gov-table">
                <thead>
                  <tr>
                    <th>Document ID</th>
                    <th>Document Type</th>
                    <th>Institution</th>
                    <th>Issue Date</th>
                    <th>Expiry Date</th>
                    <th>Verification Status</th>
                    <th>Version</th>
                    <th>Renewal Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredDocuments.map((row) => (
                    <tr key={row.id}>
                      <td>
                        <strong>{row.id}</strong>
                      </td>
                      <td>
                        <strong>{row.documentType || row.category}</strong>
                        <div style={{ fontSize: '10px', color: 'var(--color-text-muted)' }}>
                          File: {row.fileName}
                        </div>
                      </td>
                      <td>
                        <strong>{row.institutionName}</strong>
                        <div style={{ fontSize: '10px', color: 'var(--color-text-muted)' }}>
                          Assigned: {row.reviewer}
                        </div>
                      </td>
                      <td style={{ fontSize: '11px' }}>
                        {row.issueDate || '—'}
                      </td>
                      <td>
                        <span
                          style={{
                            fontWeight: '700',
                            color:
                              row.validityStatus === 'Overdue' || row.verificationStatus === 'Expired / no verified renewal'
                                ? 'var(--color-critical)'
                                : row.validityStatus === 'Due soon'
                                ? 'var(--color-amber)'
                                : 'inherit',
                          }}
                        >
                          {row.expiryDate}
                        </span>
                      </td>
                      <td>
                        <StatusBadge status={row.verificationStatus} />
                      </td>
                      <td>
                        <span className="badge badge-gray" style={{ fontWeight: '700', fontFamily: 'monospace' }}>
                          {row.currentVersion || 'v1.0'}
                        </span>
                      </td>
                      <td>
                        <span
                          className={`badge ${
                            row.renewalStatus === 'Renewed & Verified'
                              ? 'badge-green'
                              : row.renewalStatus === 'Renewal submitted'
                              ? 'badge-teal'
                              : row.renewalStatus === 'Overdue for Renewal'
                              ? 'badge-red'
                              : row.renewalStatus === 'Renewal Due'
                              ? 'badge-amber'
                              : 'badge-gray'
                          }`}
                        >
                          {row.renewalStatus || 'Not Required'}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: '6px', alignItems: 'center' }}>
                          <button
                            onClick={() => handleOpenDetailsModal(row)}
                            className="btn btn-outline btn-sm"
                            title="View Document Details, Live Preview & Version History"
                          >
                            <Eye size={12} /> View
                          </button>

                          {/* Submit Renewal Button for Institution Rep or Officers */}
                          <button
                            onClick={() => handleOpenRenewalModal(row)}
                            className="btn btn-outline btn-sm"
                            title="Submit Renewed Document Version"
                            style={{ borderColor: 'var(--color-teal)', color: 'var(--color-teal)' }}
                          >
                            <RefreshCw size={12} /> Renew
                          </button>

                          {/* Reviewer Action: Verify Document */}
                          {isAuthorizedReviewer && (
                            <button
                              onClick={() => handleOpenVerifyModal(row)}
                              className="btn btn-primary btn-sm"
                              title="Evaluate and Update Verification Decision"
                            >
                              <ShieldCheck size={12} /> Verify
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

      {/* ========================================================================= */}
      {/* MODAL 1: UPLOAD STATUTORY DOCUMENT */}
      {/* ========================================================================= */}
      <Modal
        isOpen={isUploadModalOpen}
        onClose={() => setIsUploadModalOpen(false)}
        title="Upload Statutory Compliance Document"
      >
        <form onSubmit={handleConfirmUpload} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <div
            style={{
              background: '#FFF9E6',
              border: '1px solid #FFE699',
              borderRadius: 'var(--radius-sm)',
              padding: '10px 12px',
              fontSize: '11px',
              color: '#8A6D3B',
            }}
          >
            <strong>Statutory Rule:</strong> Mandatory documents must bear the registration stamp and seal of the competent government authority. Initial uploads enter <em>Verification pending</em> until scrutiny by AICTE.
          </div>

          <div>
            <label className="form-label">Institution</label>
            <input
              type="text"
              className="form-input"
              value={isInstitutionRep ? user?.institutionName : 'ABC Institute of Technology'}
              disabled
              style={{ background: '#F8F9FA' }}
            />
          </div>

          <div>
            <label className="form-label">Statutory Document Type *</label>
            <select
              className="form-select"
              value={uploadCategory}
              onChange={(e) => setUploadCategory(e.target.value)}
              required
            >
              <option value="Registered Land Lease Deed (30 Years)">Registered Land Lease Deed (30 Years)</option>
              <option value="State Fire Service Safety NOC">State Fire Service Safety NOC</option>
              <option value="Municipal Building Plan & Occupancy Certificate">Municipal Building Plan &amp; Occupancy Certificate</option>
              <option value="State Technical University Affiliation Order">State Technical University Affiliation Order</option>
              <option value="Joint FDR Security Deposit in favor of AICTE">Joint FDR Security Deposit in favor of AICTE</option>
              <option value="Non-Encumbrance Certificate from Sub-Registrar">Non-Encumbrance Certificate from Sub-Registrar</option>
              <option value="State Pollution Control Board Consent (CTO)">State Pollution Control Board Consent (CTO)</option>
              <option value="Structural Stability & Safety Certificate">Structural Stability &amp; Safety Certificate</option>
            </select>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
            <div>
              <label className="form-label">Issue Date *</label>
              <input
                type="date"
                className="form-input"
                value={uploadIssueDate}
                onChange={(e) => setUploadIssueDate(e.target.value)}
                required
              />
            </div>
            <div>
              <label className="form-label">Expiry Date *</label>
              <input
                type="date"
                className="form-input"
                value={uploadExpiryDate}
                onChange={(e) => setUploadExpiryDate(e.target.value)}
                required
              />
            </div>
          </div>

          <div>
            <label className="form-label">Select Document File (PDF, Image) *</label>
            <input
              type="file"
              className="form-input"
              accept=".pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg"
              onChange={(e) => handleFileUploadChange(e, false)}
              style={{ padding: '6px' }}
            />
            {uploadFileName ? (
              <div style={{ fontSize: '11px', color: 'var(--color-success)', marginTop: '4px', fontWeight: '600' }}>
                ✓ Selected: {uploadFileName} (Held in browser memory for preview)
              </div>
            ) : (
              <div style={{ fontSize: '10px', color: 'var(--color-text-muted)', marginTop: '4px' }}>
                PDF/PNG/JPEG only, up to 25 MB. SAGE stores the file and extracts PDF text or runs Tesseract OCR on scans.
              </div>
            )}
          </div>

          <div>
            <label className="form-label">Remarks / Document Notes</label>
            <textarea
              className="form-textarea"
              rows={2}
              placeholder="E.g., Renewed NOC certified by District Fire Officer, Reference no. FS/2026/89..."
              value={uploadNotes}
              onChange={(e) => setUploadNotes(e.target.value)}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '8px' }}>
            <button type="button" onClick={() => setIsUploadModalOpen(false)} className="btn btn-outline">
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={isUploading || !uploadFile}>
              <Upload size={14} /> {isUploading ? 'Uploading &amp; extracting text…' : 'Upload Document &amp; Run OCR'}
            </button>
          </div>
        </form>
      </Modal>

      {/* ========================================================================= */}
      {/* MODAL 2: SUBMIT DOCUMENT RENEWAL */}
      {/* ========================================================================= */}
      {selectedDoc && (
        <Modal
          isOpen={isRenewalModalOpen}
          onClose={() => setIsRenewalModalOpen(false)}
          title={`Submit Renewal for ${selectedDoc.id}`}
        >
          <form onSubmit={handleConfirmRenewal} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <div style={{ background: '#F4F7FA', padding: '12px', borderRadius: 'var(--radius-sm)', border: '1px solid #D5E1ED' }}>
              <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>Target Legal Instrument:</div>
              <div style={{ fontSize: '13px', fontWeight: '800', color: 'var(--color-navy)' }}>
                {selectedDoc.documentType || selectedDoc.category}
              </div>
              <div style={{ fontSize: '11px', color: 'var(--color-text-muted)', marginTop: '4px' }}>
                Current Version: <strong>{selectedDoc.currentVersion || 'v1.0'}</strong> · Current Expiry: <strong>{selectedDoc.expiryDate}</strong>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <div>
                <label className="form-label">Renewal Issue Date *</label>
                <input
                  type="date"
                  className="form-input"
                  value={renewalIssueDate}
                  onChange={(e) => setRenewalIssueDate(e.target.value)}
                  required
                />
              </div>
              <div>
                <label className="form-label">New Expiry Date *</label>
                <input
                  type="date"
                  className="form-input"
                  value={renewalExpiryDate}
                  onChange={(e) => setRenewalExpiryDate(e.target.value)}
                  required
                />
              </div>
            </div>

            <div>
              <label className="form-label">Upload Renewal Certificate / Endorsement (PDF, Image) *</label>
              <input
                type="file"
                className="form-input"
                accept=".pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg"
                required
                onChange={(e) => handleFileUploadChange(e, true)}
                style={{ padding: '6px' }}
              />
              {renewalFileName ? (
                <div style={{ fontSize: '11px', color: 'var(--color-success)', marginTop: '4px', fontWeight: '600' }}>
                  ✓ Attached Renewal: {renewalFileName}
                </div>
              ) : (
                <div style={{ fontSize: '10px', color: 'var(--color-text-muted)', marginTop: '4px' }}>
                  Submitting renewal creates a new immutable version in the document audit trail.
                </div>
              )}
            </div>

            <div>
              <label className="form-label">Renewal Endorsement Remarks *</label>
              <textarea
                className="form-textarea"
                rows={2}
                placeholder="E.g., Registered extension endorsement signed by Sub-Registrar on stamp paper..."
                value={renewalNotes}
                onChange={(e) => setRenewalNotes(e.target.value)}
                required
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '8px' }}>
              <button type="button" onClick={() => setIsRenewalModalOpen(false)} className="btn btn-outline">
                Cancel
              </button>
              <button type="submit" className="btn btn-primary">
                <RefreshCw size={14} /> Submit Renewal Version
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* ========================================================================= */}
      {/* MODAL 3: VIEW DOCUMENT DETAILS & LIVE PREVIEW & VERSION HISTORY */}
      {/* ========================================================================= */}
      {selectedDoc && (
        <Modal
          isOpen={isDetailsModalOpen}
          onClose={() => setIsDetailsModalOpen(false)}
          title={`Document Record: ${selectedDoc.id}`}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            {/* Modal Internal Sub-Tabs */}
            <div style={{ display: 'flex', borderBottom: '1px solid #D5E1ED', gap: '8px' }}>
              <button
                type="button"
                onClick={() => setDetailsTab('preview')}
                style={{
                  padding: '8px 14px',
                  background: 'none',
                  border: 'none',
                  borderBottom: detailsTab === 'preview' ? '2px solid var(--color-blue)' : '2px solid transparent',
                  fontWeight: detailsTab === 'preview' ? '700' : '500',
                  color: detailsTab === 'preview' ? 'var(--color-blue)' : 'var(--color-text-muted)',
                  cursor: 'pointer',
                  fontSize: 'var(--font-size-sm)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                }}
              >
                <Eye size={14} /> Document Overview &amp; Preview
              </button>
              <button
                type="button"
                onClick={() => setDetailsTab('versions')}
                style={{
                  padding: '8px 14px',
                  background: 'none',
                  border: 'none',
                  borderBottom: detailsTab === 'versions' ? '2px solid var(--color-blue)' : '2px solid transparent',
                  fontWeight: detailsTab === 'versions' ? '700' : '500',
                  color: detailsTab === 'versions' ? 'var(--color-blue)' : 'var(--color-text-muted)',
                  cursor: 'pointer',
                  fontSize: 'var(--font-size-sm)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '6px',
                }}
              >
                <History size={14} /> Version History ({selectedDoc.versions?.length || 1})
              </button>
            </div>

            {/* TAB 1: OVERVIEW & PREVIEW */}
            {detailsTab === 'preview' && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                {/* Metadata Grid */}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '10px', background: '#F8FAFC', padding: '12px', borderRadius: 'var(--radius-sm)', border: '1px solid #E2E8F0' }}>
                  <div>
                    <span style={{ fontSize: '10px', color: 'var(--color-text-muted)', textTransform: 'uppercase' }}>Document Type</span>
                    <div style={{ fontSize: '12px', fontWeight: '700', color: 'var(--color-navy)' }}>
                      {selectedDoc.documentType || selectedDoc.category}
                    </div>
                  </div>
                  <div>
                    <span style={{ fontSize: '10px', color: 'var(--color-text-muted)', textTransform: 'uppercase' }}>Institution</span>
                    <div style={{ fontSize: '12px', fontWeight: '700', color: 'var(--color-navy)' }}>
                      {selectedDoc.institutionName} ({selectedDoc.institutionId})
                    </div>
                  </div>
                  <div>
                    <span style={{ fontSize: '10px', color: 'var(--color-text-muted)', textTransform: 'uppercase' }}>Issue Date</span>
                    <div style={{ fontSize: '12px', fontWeight: '600' }}>{selectedDoc.issueDate || '1996-10-31'}</div>
                  </div>
                  <div>
                    <span style={{ fontSize: '10px', color: 'var(--color-text-muted)', textTransform: 'uppercase' }}>Expiry Date</span>
                    <div style={{ fontSize: '12px', fontWeight: '700', color: selectedDoc.validityStatus === 'Overdue' ? 'var(--color-critical)' : 'inherit' }}>
                      {selectedDoc.expiryDate}
                    </div>
                  </div>
                  <div>
                    <span style={{ fontSize: '10px', color: 'var(--color-text-muted)', textTransform: 'uppercase' }}>Verification Status</span>
                    <div style={{ marginTop: '2px' }}>
                      <StatusBadge status={selectedDoc.verificationStatus} />
                    </div>
                  </div>
                  <div>
                    <span style={{ fontSize: '10px', color: 'var(--color-text-muted)', textTransform: 'uppercase' }}>Current Version</span>
                    <div style={{ fontSize: '12px', fontWeight: '800', fontFamily: 'monospace' }}>
                      {selectedDoc.currentVersion || 'v1.0'}
                    </div>
                  </div>
                </div>

                {/* OCR result is persisted by the backend with the document and its version. */}
                <div style={{ border: '1px solid #CBD5E1', borderRadius: 'var(--radius-sm)', padding: '12px', background: '#F8FAFC' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                    <div style={{ fontSize: '12px', fontWeight: '800', color: 'var(--color-navy)' }}>
                      <FileText size={14} style={{ display: 'inline', verticalAlign: 'middle', marginRight: '6px' }} /> OCR / Extracted Document Text
                    </div>
                    <span className="badge badge-gray">{selectedDoc.ocrStatus || 'PENDING'}</span>
                  </div>
                  {selectedDoc.ocrMessage && <div style={{ fontSize: '11px', color: 'var(--color-text-muted)', marginTop: '6px' }}>{selectedDoc.ocrMessage}</div>}
                  {selectedDoc.extractedText ? (
                    <pre style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere', maxHeight: '220px', overflowY: 'auto', margin: '8px 0 0', padding: '10px', background: '#FFFFFF', border: '1px solid #E2E8F0', borderRadius: '4px', fontSize: '11px', lineHeight: 1.5, color: '#243447' }}>{selectedDoc.extractedText}</pre>
                  ) : (
                    <div style={{ marginTop: '8px', fontSize: '11px', color: 'var(--color-text-muted)' }}>No extracted text is available yet. Check the OCR status and backend setup.</div>
                  )}
                </div>

                {/* Live Preview Section */}
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px', flexWrap: 'wrap', gap: '8px' }}>
                    <span style={{ fontSize: '11px', fontWeight: '700', color: 'var(--color-navy)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                      Official Certificate File Preview: {selectedDoc.fileName}
                    </span>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <button
                        type="button"
                        onClick={() => downloadDocumentFile(selectedDoc.id, selectedDoc.fileName)}
                        className="btn btn-outline btn-sm"
                        style={{ fontSize: '11px', padding: '3px 8px' }}
                        title="Download durable document file with verified SHA-256 hash"
                      >
                        <Upload size={11} style={{ transform: 'rotate(180deg)' }} /> Download Official File
                      </button>
                      {selectedDoc.fileBlobUrl && (
                        <span style={{ fontSize: '10px', color: 'var(--color-success)', fontWeight: '700' }}>
                          ● Browser Memory Active
                        </span>
                      )}
                    </div>
                  </div>

                  {selectedDoc.fileBlobUrl ? (
                    <div style={{ border: '1px solid #CBD5E1', borderRadius: 'var(--radius-sm)', overflow: 'hidden', height: '240px', background: '#0F172A', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
                      <iframe
                        src={selectedDoc.fileBlobUrl}
                        title="Document Preview"
                        style={{ width: '100%', height: '100%', border: 'none' }}
                      />
                    </div>
                  ) : (
                    /* AICTE Official Statutory Document Certificate Visualizer */
                    <div
                      style={{
                        border: '2px dashed #B8D4EA',
                        borderRadius: 'var(--radius-sm)',
                        padding: '16px',
                        background: '#FAFDFE',
                        position: 'relative',
                      }}
                    >
                      <div style={{ textAlign: 'center', borderBottom: '1px solid #D5E4F0', paddingBottom: '10px', marginBottom: '12px' }}>
                        <div style={{ fontSize: '10px', fontWeight: '800', letterSpacing: '0.12em', color: '#1B527A', textTransform: 'uppercase' }}>
                          GOVERNMENT OF INDIA · ALL INDIA COUNCIL FOR TECHNICAL EDUCATION
                        </div>
                        <div style={{ fontSize: '13px', fontWeight: '800', color: 'var(--color-navy)', marginTop: '2px' }}>
                          CERTIFICATE OF STATUTORY RECORD VALIDATION
                        </div>
                      </div>

                      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '12px' }}>
                        <div style={{ fontSize: '11px', lineHeight: '1.6' }}>
                          <div><strong>Registered Instrument:</strong> {selectedDoc.category}</div>
                          <div><strong>Depository Reference:</strong> {selectedDoc.id} (Version {selectedDoc.currentVersion || 'v1.0'})</div>
                          <div><strong>Issuing Authority:</strong> Certified Government Department / Revenue Authority</div>
                          <div><strong>Validity Window:</strong> {selectedDoc.issueDate || 'Recorded'} to {selectedDoc.expiryDate}</div>
                          <div style={{ marginTop: '6px', color: 'var(--color-text-muted)' }}>
                            <strong>Official Note:</strong> {selectedDoc.notes || 'Verified against Approval Process Handbook statutory norms.'}
                          </div>
                        </div>

                        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', background: '#FFFFFF', border: '1px solid #E2E8F0', borderRadius: 'var(--radius-sm)', padding: '10px' }}>
                          <ShieldCheck size={36} style={{ color: 'var(--color-teal)', marginBottom: '4px' }} />
                          <div style={{ fontSize: '9px', fontWeight: '800', color: 'var(--color-navy)', textAlign: 'center' }}>
                            AICTE COMPLIANCE AUDIT SEAL
                          </div>
                          <div style={{ fontSize: '8px', color: 'var(--color-text-muted)', textAlign: 'center' }}>
                            QR CODE VERIFIED
                          </div>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* TAB 2: VERSION HISTORY */}
            {detailsTab === 'versions' && (
              <div>
                <div style={{ fontSize: '11px', color: 'var(--color-text-muted)', marginBottom: '10px' }}>
                  Immutable version audit trail. When institutions upload renewals, older certified certificates are permanently archived without overwriting historical records.
                </div>

                <div className="table-container" style={{ maxHeight: '280px', overflowY: 'auto' }}>
                  <table className="gov-table" style={{ fontSize: '11px' }}>
                    <thead>
                      <tr>
                        <th>Version</th>
                        <th>File Name</th>
                        <th>Validity Period</th>
                        <th>Submission Timestamp</th>
                        <th>Submitted By</th>
                        <th>Status</th>
                        <th>Remarks</th>
                      </tr>
                    </thead>
                    <tbody>
                      {(selectedDoc.versions || []).map((ver, idx) => (
                        <tr key={idx}>
                          <td>
                            <strong style={{ fontFamily: 'monospace' }}>{ver.version}</strong>
                          </td>
                          <td>
                            <span style={{ color: 'var(--color-blue)', textDecoration: 'underline' }}>
                              {ver.fileName}
                            </span>
                          </td>
                          <td>
                            {ver.issueDate} → {ver.expiryDate}
                          </td>
                          <td>
                            {ver.submittedDate}
                          </td>
                          <td>
                            {ver.submittedBy}
                          </td>
                          <td>
                            <StatusBadge status={ver.verificationStatus} />
                          </td>
                          <td style={{ maxWidth: '180px', whiteSpace: 'normal', fontSize: '10px' }}>
                            {ver.notes || '—'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '8px' }}>
              <button type="button" onClick={() => setIsDetailsModalOpen(false)} className="btn btn-outline">
                Close Viewer
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* ========================================================================= */}
      {/* MODAL 4: UPDATE VERIFICATION STATUS (AUTHORIZED REVIEWER) */}
      {/* ========================================================================= */}
      {selectedDoc && (
        <Modal
          isOpen={isVerifyModalOpen}
          onClose={() => setIsVerifyModalOpen(false)}
          title={`Verify Statutory Document: ${selectedDoc.id}`}
        >
          <form onSubmit={handleConfirmVerification} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <div style={{ background: '#F4F7FA', padding: '12px', borderRadius: 'var(--radius-sm)', border: '1px solid #D5E1ED' }}>
              <div style={{ fontSize: '13px', fontWeight: '800', color: 'var(--color-navy)' }}>
                {selectedDoc.documentType || selectedDoc.category}
              </div>
              <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
                Institution: <strong>{selectedDoc.institutionName}</strong> · Version: <strong>{selectedDoc.currentVersion || 'v1.0'}</strong>
              </div>
            </div>

            <div>
              <label className="form-label">Statutory Verification Decision *</label>
              <select
                className="form-select"
                value={verifyStatus}
                onChange={(e) => setVerifyStatus(e.target.value)}
                required
              >
                <option value="Verified">Verified (Conforms to APH handbook guidelines)</option>
                <option value="Verification pending">Verification pending (Under examination)</option>
                <option value="Expiring soon">Expiring soon (Valid but approaching expiry)</option>
                <option value="Renewal submitted">Renewal submitted (Awaiting revenue verification)</option>
                <option value="Expired / no verified renewal">Expired / Deficient (Non-compliant)</option>
              </select>
            </div>

            <div>
              <label className="form-label">Official Reviewer Remarks *</label>
              <textarea
                className="form-textarea"
                rows={3}
                placeholder="Enter statutory inspection notes, seal verification findings, or deficiency reasons..."
                value={verifyRemarks}
                onChange={(e) => setVerifyRemarks(e.target.value)}
                required
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '8px' }}>
              <button type="button" onClick={() => setIsVerifyModalOpen(false)} className="btn btn-outline">
                Cancel
              </button>
              <button type="submit" className="btn btn-primary">
                <ShieldCheck size={14} /> Record Verification
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};
