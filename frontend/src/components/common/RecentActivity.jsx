// SIH 26262 - Reusable RecentActivity Component

import React from 'react';
import { Link } from 'react-router-dom';
import { Activity, Clock } from 'lucide-react';

export const RecentActivity = ({ activities = [] }) => {
  if (!activities || activities.length === 0) {
    return <div style={{ padding: '16px', color: 'var(--color-text-muted)', fontSize: 'var(--font-size-xs)' }}>No recent activity logged.</div>;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column' }}>
      {activities.map((item, idx) => (
        <div
          key={item.id || idx}
          style={{
            display: 'flex',
            alignItems: 'flex-start',
            gap: '12px',
            padding: '10px 14px',
            borderBottom: idx === activities.length - 1 ? 'none' : '1px solid var(--color-border-subtle)',
          }}
        >
          <div
            style={{
              width: '28px',
              height: '28px',
              borderRadius: '50%',
              background: 'var(--color-blue-soft)',
              color: 'var(--color-blue)',
              display: 'grid',
              placeItems: 'center',
              flexShrink: 0,
              marginTop: '2px',
            }}
          >
            <Activity size={14} />
          </div>

          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', gap: '8px' }}>
              <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: '700', color: 'var(--color-navy)' }}>
                {item.action}
              </span>
              <span style={{ fontSize: '10px', color: 'var(--color-text-light)', display: 'flex', alignItems: 'center', gap: '3px', flexShrink: 0 }}>
                <Clock size={10} /> {item.timestamp}
              </span>
            </div>

            <div style={{ fontSize: '11px', color: 'var(--color-text)', marginTop: '2px' }}>
              {item.details}
            </div>

            <div style={{ fontSize: '10px', color: 'var(--color-text-muted)', marginTop: '4px', display: 'flex', gap: '10px' }}>
              <span>Actor: <strong>{item.actor}</strong></span>
              {item.relatedRecordId && (
                <span>
                  Ref: <strong style={{ color: 'var(--color-blue)' }}>{item.relatedRecordId}</strong>
                </span>
              )}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
};
