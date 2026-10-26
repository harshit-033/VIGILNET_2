// SIH 26262 - Reusable EmptyState Component

import React from 'react';
import { Inbox } from 'lucide-react';

export const EmptyState = ({ message = 'No records found for current criteria.', iconName, actionLabel, onAction }) => {
  return (
    <div style={{ textAlign: 'center', padding: '32px 16px', color: 'var(--color-text-muted)' }}>
      <div
        style={{
          width: '36px',
          height: '36px',
          borderRadius: '50%',
          background: '#F0F4F8',
          color: '#8295A7',
          display: 'grid',
          placeItems: 'center',
          margin: '0 auto 8px',
        }}
      >
        <Inbox size={20} />
      </div>
      <div style={{ fontSize: 'var(--font-size-sm)', fontWeight: '600', color: 'var(--color-text)' }}>
        {message}
      </div>
      {actionLabel && onAction && (
        <button onClick={onAction} className="btn btn-outline btn-sm" style={{ marginTop: '10px' }}>
          {actionLabel}
        </button>
      )}
    </div>
  );
};
