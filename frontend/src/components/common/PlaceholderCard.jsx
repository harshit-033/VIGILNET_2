// SIH 26262 - Standard Placeholder for Future Modules

import React from 'react';
import * as Icons from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export const PlaceholderCard = ({ moduleName, route, iconName = 'FileText', description, upcomingFeatures = [] }) => {
  const { user, role } = useAuth();
  const IconComponent = Icons[iconName] || Icons.FileText;

  return (
    <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
      <div className="panel">
        <div className="panel-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div
              style={{
                width: '32px',
                height: '32px',
                background: 'var(--color-blue-soft)',
                color: 'var(--color-blue)',
                borderRadius: 'var(--radius-sm)',
                display: 'grid',
                placeItems: 'center',
              }}
            >
              <IconComponent size={18} />
            </div>
            <div>
              <h2 className="panel-title">{moduleName}</h2>
              <p className="panel-subtitle">Route: {route} · Accessible to: {role}</p>
            </div>
          </div>
          <span className="badge badge-blue">Phase 1 Placeholder</span>
        </div>
        <div className="panel-body">
          <div className="callout callout-blue">
            <strong>Module Activated:</strong> This module route is registered and protected in the Phase 1 routing architecture. Full operational controls and workflows will be connected in subsequent phases.
          </div>

          <p style={{ color: 'var(--color-text)', marginBottom: '14px', fontSize: 'var(--font-size-md)' }}>
            {description}
          </p>

          {upcomingFeatures.length > 0 && (
            <div style={{ marginTop: '16px' }}>
              <div style={{ fontSize: 'var(--font-size-xs)', fontWeight: '700', textTransform: 'uppercase', color: 'var(--color-text-muted)', marginBottom: '8px' }}>
                Planned Capabilities for this Module
              </div>
              <ul style={{ paddingLeft: '20px', color: 'var(--color-text)', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                {upcomingFeatures.map((feat, idx) => (
                  <li key={idx}>{feat}</li>
                ))}
              </ul>
            </div>
          )}

          <div style={{ marginTop: '20px', padding: '12px', background: '#F8FAFC', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-sm)', fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
            <strong>Active Session Scope:</strong> {user?.name} ({role}) · Region: {user?.region || 'National'} {user?.institutionId ? `· Institution: ${user.institutionName} (${user.institutionId})` : ''}
          </div>
        </div>
      </div>
    </div>
  );
};
