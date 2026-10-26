// SIH 26262 - Alerts & Escalations Module (/alerts)

import React, { useState, useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useData } from '../context/DataContext';
import { ROLES } from '../constants/roles';
import { KPICard } from '../components/common/KPICard';
import { TabsNav } from '../components/common/TabsNav';
import { StatusBadge } from '../components/common/StatusBadge';
import { EmptyState } from '../components/common/EmptyState';
import { Modal } from '../components/common/Modal';
import {
  AlertTriangle,
  Bell,
  Clock,
  CheckCircle2,
  AlertOctagon,
  ArrowUpRight,
  RefreshCw,
  Search,
  ExternalLink,
  Check,
  Zap,
  Eye,
  Send,
  XCircle,
  FileText,
} from 'lucide-react';

export const AlertsPage = () => {
  const { user, role } = useAuth();
  const navigate = useNavigate();
  const {
    alerts,
    markAlertRead,
    acknowledgeAlert,
    simulateNotificationFailure,
    retryNotification,
    simulateEscalation,
    generateSystemAlerts,
  } = useData();

  // Active Tab & Search
  const [activeTab, setActiveTab] = useState('All Alerts');
  const [searchQuery, setSearchQuery] = useState('');

  // Escalation Modal State
  const [isEscalateModalOpen, setIsEscalateModalOpen] = useState(false);
  const [selectedAlert, setSelectedAlert] = useState(null);
  const [escalateReason, setEscalateReason] = useState('Handbook turnaround SLA breached beyond allowable buffer.');

  // Helper to resolve link for Related Record
  const getRecordLink = (recId) => {
    if (!recId) return '/dashboard';
    if (recId.startsWith('APP-')) return `/applications/${recId}`;
    if (recId.startsWith('EVC-')) return '/evc';
    if (recId.startsWith('DOC-')) return '/documents';
    if (recId.startsWith('OBL-')) return '/compliance';
    return '/dashboard';
  };

  // Search Filter
  const searchedAlerts = useMemo(() => {
    if (!searchQuery.trim()) return alerts;
    const q = searchQuery.toLowerCase();
    return alerts.filter(
      (a) =>
        a.id.toLowerCase().includes(q) ||
        a.type.toLowerCase().includes(q) ||
        (a.relatedRecordId && a.relatedRecordId.toLowerCase().includes(q)) ||
        (a.institutionName && a.institutionName.toLowerCase().includes(q)) ||
        (a.recipient && a.recipient.toLowerCase().includes(q)) ||
        (a.message && a.message.toLowerCase().includes(q)) ||
        (a.requiredAction && a.requiredAction.toLowerCase().includes(q))
    );
  }, [alerts, searchQuery]);

  // Tab Filter
  const tabFilteredAlerts = useMemo(() => {
    switch (activeTab) {
      case 'Upcoming Deadlines':
        return searchedAlerts.filter(
          (a) => a.category === 'Upcoming Deadlines' || a.urgency === 'Due soon'
        );
      case 'Overdue Tasks':
        return searchedAlerts.filter(
          (a) => a.category === 'Overdue Tasks' || a.urgency === 'Overdue'
        );
      case 'EVC Reminders':
        return searchedAlerts.filter(
          (a) => a.category === 'EVC Reminders' || a.type.includes('EVC')
        );
      case 'Compliance Alerts':
        return searchedAlerts.filter(
          (a) =>
            a.category === 'Compliance Alerts' ||
            a.type.includes('Compliance') ||
            a.type.includes('Obligation')
        );
      case 'Document Expiry':
        return searchedAlerts.filter(
          (a) =>
            a.category === 'Document Expiry' ||
            a.type.includes('Document') ||
            a.type.includes('Expiry')
        );
      case 'Escalations':
        return searchedAlerts.filter(
          (a) =>
            a.category === 'Escalations' ||
            a.deliveryStatus === 'Escalated' ||
            a.urgency === 'Escalated'
        );
      case 'All Alerts':
      default:
        return searchedAlerts;
    }
  }, [searchedAlerts, activeTab]);

  // KPI calculations
  const kpiTotal = alerts.length;
  const kpiUnread = alerts.filter((a) => !a.read).length;
  const kpiOverdue = alerts.filter((a) => a.urgency === 'Overdue').length;
  const kpiEscalated = alerts.filter(
    (a) => a.deliveryStatus === 'Escalated' || a.urgency === 'Escalated'
  ).length;
  const kpiFailed = alerts.filter((a) => a.deliveryStatus === 'Delivery failed').length;

  // 7 Tabs
  const tabs = [
    { key: 'All Alerts', label: 'All Alerts', count: kpiTotal },
    {
      key: 'Upcoming Deadlines',
      label: 'Upcoming Deadlines',
      count: alerts.filter((a) => a.category === 'Upcoming Deadlines' || a.urgency === 'Due soon').length,
    },
    {
      key: 'Overdue Tasks',
      label: 'Overdue Tasks',
      count: alerts.filter((a) => a.category === 'Overdue Tasks' || a.urgency === 'Overdue').length,
    },
    {
      key: 'EVC Reminders',
      label: 'EVC Reminders',
      count: alerts.filter((a) => a.category === 'EVC Reminders' || a.type.includes('EVC')).length,
    },
    {
      key: 'Compliance Alerts',
      label: 'Compliance Alerts',
      count: alerts.filter((a) => a.category === 'Compliance Alerts' || a.type.includes('Compliance') || a.type.includes('Obligation')).length,
    },
    {
      key: 'Document Expiry',
      label: 'Document Expiry',
      count: alerts.filter((a) => a.category === 'Document Expiry' || a.type.includes('Document') || a.type.includes('Expiry')).length,
    },
    { key: 'Escalations', label: 'Escalations', count: kpiEscalated },
  ];

  // Action: Mark Read
  const handleMarkRead = (alertId) => {
    markAlertRead({ alertId });
  };

  // Action: Acknowledge
  const handleAcknowledge = async (alertId) => {
    const res = await acknowledgeAlert({
      alertId,
      actorName: user?.name,
      actorRole: role,
    });
    if (res.success) {
      alert(`Alert ${alertId} formally acknowledged. Officer receipt recorded in statutory audit trail.`);
    } else {
      alert(res.error || 'Failed to acknowledge alert.');
    }
  };

  // Action: Simulate Failure
  const handleSimulateFailure = (alertId) => {
    if (simulateNotificationFailure) simulateNotificationFailure({ alertId });
    alert(`Simulated delivery failure for alert ${alertId}. Status is now "Delivery failed".`);
  };

  // Action: Retry Delivery
  const handleRetryNotification = (alertId) => {
    retryNotification({ alertId });
    alert(`Notification gateway re-dispatched for alert ${alertId}. Status restored to "Delivery confirmed".`);
  };

  // Action: Open Escalate Modal
  const handleOpenEscalateModal = (alt) => {
    setSelectedAlert(alt);
    setEscalateReason('Handbook turnaround SLA breached beyond allowable buffer.');
    setIsEscalateModalOpen(true);
  };

  // Action: Confirm Escalation
  const handleConfirmEscalation = async (e) => {
    e.preventDefault();
    const res = await simulateEscalation({
      alertId: selectedAlert.id,
      reason: escalateReason,
      actorName: user?.name,
      actorRole: role,
    });

    if (res.success) {
      setIsEscalateModalOpen(false);
      alert(
        `Case ${selectedAlert.relatedRecordId || selectedAlert.id} formally escalated to Senior Management / Competent Authority.\nReason: ${escalateReason}`
      );
    } else {
      alert(res.error || 'Failed to escalate alert.');
    }
  };

  // Action: Generate System Alerts from Expiries & Deadlines
  const handleGenerateSystemAlerts = async () => {
    const res = await generateSystemAlerts();
    if (res && res.count > 0) {
      alert(`Synchronized: ${res.count} alerts active in the Early-Warning Engine.`);
    } else {
      alert('All active document expiries and compliance obligations are synchronized in the Alerts Registry.');
    }
  };

  return (
    <div>
      {/* Header Bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '16px', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <div style={{ fontSize: '10px', textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--color-teal)', fontWeight: '800' }}>
            EARLY-WARNING SLA ENGINE · STATUTORY NOTIFICATIONS
          </div>
          <h1 style={{ fontSize: '22px', fontWeight: '800', color: 'var(--color-navy)', margin: '4px 0 2px' }}>
            Alerts &amp; Escalations Center
          </h1>
          <p style={{ color: 'var(--color-text-muted)', fontSize: 'var(--font-size-sm)' }}>
            Proactive monitoring of handbook turnaround times, committee deadlines, delivery dispatches, officer acknowledgements, and policy escalations.
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <button
            onClick={handleGenerateSystemAlerts}
            className="btn btn-outline"
            title="Scan database and generate alerts for approaching expiries and overdue items"
          >
            <Zap size={14} style={{ color: 'var(--color-amber)' }} /> Sync System Alerts
          </button>
        </div>
      </div>

      {/* 5 KPI Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))', gap: '14px', marginBottom: '18px' }}>
        <KPICard
          title="Total Alerts"
          value={kpiTotal}
          subtitle="All dispatch notifications"
          iconName="Bell"
          tint="blue"
          onClick={() => setActiveTab('All Alerts')}
        />
        <KPICard
          title="Unread Alerts"
          value={kpiUnread}
          subtitle="Awaiting officer view"
          iconName="Eye"
          tint="amber"
          onClick={() => setActiveTab('All Alerts')}
        />
        <KPICard
          title="Overdue Alerts"
          value={kpiOverdue}
          subtitle="SLA window breached"
          iconName="Clock"
          tint="red"
          onClick={() => setActiveTab('Overdue Tasks')}
        />
        <KPICard
          title="Escalated Cases"
          value={kpiEscalated}
          subtitle="Escalated to Senior Mgmt"
          iconName="ArrowUpRight"
          tint="purple"
          onClick={() => setActiveTab('Escalations')}
        />
        <KPICard
          title="Failed Notifications"
          value={kpiFailed}
          subtitle="Gateway delivery fault"
          iconName="XCircle"
          tint="red"
          onClick={() => setActiveTab('All Alerts')}
        />
      </div>

      {/* Toolbar: Search */}
      <div className="panel" style={{ marginBottom: '16px', padding: '12px 16px', background: '#FCFDFE', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
        <div style={{ position: 'relative', width: '340px' }}>
          <input
            type="text"
            className="form-input"
            placeholder="Search by Alert ID, Type, College, Record..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ paddingLeft: '32px' }}
          />
          <Search size={15} style={{ position: 'absolute', left: '10px', top: '10px', color: '#899BAA' }} />
        </div>

        <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
          Showing <strong>{tabFilteredAlerts.length}</strong> alerts in <em>{activeTab}</em>
        </div>
      </div>

      {/* Table Panel with 7 Tabs */}
      <div className="panel" style={{ padding: 0 }}>
        <TabsNav tabs={tabs} activeTab={activeTab} onTabChange={setActiveTab} />

        <div className="panel-body" style={{ padding: 0 }}>
          {tabFilteredAlerts.length === 0 ? (
            <EmptyState message="No alerts match the active tab filter." />
          ) : (
            <div className="table-container" style={{ border: 'none' }}>
              <table className="gov-table">
                <thead>
                  <tr>
                    <th>Alert ID</th>
                    <th>Alert Type</th>
                    <th>Related Record</th>
                    <th>Recipient</th>
                    <th>Trigger Date</th>
                    <th>Delivery Status</th>
                    <th>Acknowledgement</th>
                    <th>Required Action</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {tabFilteredAlerts.map((row) => (
                    <tr
                      key={row.id}
                      style={{
                        background: row.read ? 'transparent' : 'rgba(27, 82, 122, 0.03)',
                      }}
                    >
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                          {!row.read && (
                            <span
                              style={{
                                width: '6px',
                                height: '6px',
                                borderRadius: '50%',
                                background: 'var(--color-teal)',
                                display: 'inline-block',
                              }}
                              title="Unread alert"
                            />
                          )}
                          <strong>{row.id}</strong>
                        </div>
                      </td>
                      <td>
                        <strong>{row.type}</strong>
                        <div style={{ fontSize: '10px', color: 'var(--color-text-muted)', maxWidth: '220px', whiteSpace: 'normal' }}>
                          {row.message}
                        </div>
                      </td>
                      <td>
                        <Link
                          to={getRecordLink(row.relatedRecordId)}
                          style={{
                            color: 'var(--color-blue)',
                            fontWeight: '700',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '4px',
                          }}
                          title={`Navigate to ${row.relatedRecordId}`}
                        >
                          {row.relatedRecordId} <ExternalLink size={11} />
                        </Link>
                        {row.institutionName && (
                          <div style={{ fontSize: '10px', color: 'var(--color-text-muted)' }}>
                            {row.institutionName}
                          </div>
                        )}
                      </td>
                      <td>
                        <span style={{ fontSize: '11px', fontWeight: '600' }}>
                          {row.recipient}
                        </span>
                        <div style={{ fontSize: '10px', color: 'var(--color-text-muted)' }}>
                          {row.recipientRole || 'Assigned Officer'}
                        </div>
                      </td>
                      <td style={{ fontSize: '11px' }}>
                        {row.createdAt}
                      </td>
                      <td>
                        <span
                          className={`badge ${
                            row.deliveryStatus === 'Delivery confirmed' || row.deliveryStatus === 'Delivered'
                              ? 'badge-green'
                              : row.deliveryStatus === 'Acknowledged'
                              ? 'badge-blue'
                              : row.deliveryStatus === 'Delivery failed'
                              ? 'badge-red'
                              : row.deliveryStatus === 'Escalated'
                              ? 'badge-purple'
                              : 'badge-gray'
                          }`}
                        >
                          {row.deliveryStatus}
                        </span>
                        {row.deliveryStatus === 'Delivery failed' && (
                          <div style={{ fontSize: '9px', color: 'var(--color-critical)', marginTop: '2px' }}>
                            Gateway timeout
                          </div>
                        )}
                      </td>
                      <td>
                        {row.acknowledged ? (
                          <span style={{ color: 'var(--color-success)', fontWeight: '700', fontSize: '11px', display: 'inline-flex', alignItems: 'center', gap: '3px' }}>
                            <Check size={13} /> Acknowledged
                          </span>
                        ) : (
                          <button
                            onClick={() => handleAcknowledge(row.id)}
                            className="btn btn-outline btn-sm"
                            title="Formally acknowledge alert receipt"
                            style={{ borderColor: 'var(--color-blue)', color: 'var(--color-blue)' }}
                          >
                            <Check size={11} /> Acknowledge
                          </button>
                        )}
                      </td>
                      <td style={{ maxWidth: '180px', whiteSpace: 'normal', fontSize: '11px', lineHeight: 1.4 }}>
                        <span style={{ color: row.urgency === 'Overdue' ? 'var(--color-critical)' : 'inherit', fontWeight: '500' }}>
                          {row.requiredAction || row.message}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: 'flex', gap: '4px', alignItems: 'center' }}>
                          {/* Mark Read */}
                          {!row.read && (
                            <button
                              onClick={() => handleMarkRead(row.id)}
                              className="btn btn-outline btn-sm"
                              title="Mark as Read"
                            >
                              <Eye size={12} />
                            </button>
                          )}

                          {/* Open Related Record */}
                          <Link
                            to={getRecordLink(row.relatedRecordId)}
                            className="btn btn-outline btn-sm"
                            title="Open Related Dossier"
                          >
                            Open
                          </Link>

                          {/* Retry if failed */}
                          {row.deliveryStatus === 'Delivery failed' ? (
                            <button
                              onClick={() => handleRetryNotification(row.id)}
                              className="btn btn-primary btn-sm"
                              title="Retry Failed Gateway Dispatch"
                            >
                              <RefreshCw size={11} /> Retry
                            </button>
                          ) : (
                            /* Simulate Failure button for demo testing */
                            <button
                              onClick={() => handleSimulateFailure(row.id)}
                              className="btn btn-outline btn-sm"
                              title="Simulate SMS/Email Delivery Failure"
                              style={{ color: 'var(--color-text-muted)' }}
                            >
                              Fail
                            </button>
                          )}

                          {/* Simulate Escalation */}
                          {row.deliveryStatus !== 'Escalated' && (
                            <button
                              onClick={() => handleOpenEscalateModal(row)}
                              className="btn btn-outline btn-sm"
                              title="Escalate to Senior Management"
                              style={{ color: 'var(--color-critical)', borderColor: '#F5C6CB' }}
                            >
                              <ArrowUpRight size={11} />
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
      {/* MODAL: SIMULATE STATUTORY ESCALATION */}
      {/* ========================================================================= */}
      {selectedAlert && (
        <Modal
          isOpen={isEscalateModalOpen}
          onClose={() => setIsEscalateModalOpen(false)}
          title={`Escalate Statutory Alert: ${selectedAlert.id}`}
        >
          <form onSubmit={handleConfirmEscalation} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <div
              style={{
                background: '#FFF5F5',
                border: '1px solid #FED7D7',
                borderRadius: 'var(--radius-sm)',
                padding: '12px',
                fontSize: '11px',
                color: '#C53030',
              }}
            >
              <strong>Policy Escalation Notice:</strong> Escalating triggers an urgent high-priority statutory notification dispatched directly to Senior Management (Member Secretary / Regional Director).
            </div>

            <div style={{ background: '#F8FAFC', padding: '10px 12px', borderRadius: 'var(--radius-sm)', border: '1px solid #E2E8F0' }}>
              <div style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>Target Case / Record:</div>
              <div style={{ fontSize: '13px', fontWeight: '800', color: 'var(--color-navy)' }}>
                {selectedAlert.relatedRecordId} ({selectedAlert.institutionName})
              </div>
              <div style={{ fontSize: '11px', color: 'var(--color-text-muted)', marginTop: '2px' }}>
                Alert Type: <strong>{selectedAlert.type}</strong> · Triggered: <strong>{selectedAlert.createdAt}</strong>
              </div>
            </div>

            <div>
              <label className="form-label">Escalation Destination</label>
              <input
                type="text"
                className="form-input"
                value="Prof. H. R. Adhikari (Senior Management / Competent Authority)"
                disabled
                style={{ background: '#F8F9FA' }}
              />
            </div>

            <div>
              <label className="form-label">Statutory Escalation Justification *</label>
              <textarea
                className="form-textarea"
                rows={3}
                value={escalateReason}
                onChange={(e) => setEscalateReason(e.target.value)}
                placeholder="Specify regulatory non-compliance reason, handbook turnaround breach..."
                required
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '8px' }}>
              <button type="button" onClick={() => setIsEscalateModalOpen(false)} className="btn btn-outline">
                Cancel
              </button>
              <button type="submit" className="btn btn-danger">
                <ArrowUpRight size={14} /> Confirm Escalation
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};
