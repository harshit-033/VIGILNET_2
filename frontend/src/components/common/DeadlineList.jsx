// SIH 26262 - Reusable DeadlineList Component

import React from 'react';
import { Link } from 'react-router-dom';
import { StatusBadge } from './StatusBadge';
import { Calendar, ArrowUpRight } from 'lucide-react';

export const DeadlineList = ({ deadlines = [] }) => {
  if (!deadlines || deadlines.length === 0) {
    return <div style={{ padding: '16px', color: 'var(--color-text-muted)', fontSize: 'var(--font-size-xs)' }}>No pending deadlines for this period.</div>;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column' }}>
      {deadlines.map((item, idx) => (
        <div
          key={item.id || idx}
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '10px 14px',
            borderBottom: idx === deadlines.length - 1 ? 'none' : '1px solid var(--color-border-subtle)',
            gap: '12px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', minWidth: 0 }}>
            <div
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                width: '42px',
                height: '40px',
                background: '#F0F4F8',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-sm)',
                flexShrink: 0,
              }}
            >
              <span style={{ fontSize: '9px', fontWeight: '800', color: 'var(--color-text-muted)', textTransform: 'uppercase' }}>
                {item.month || 'OCT'}
              </span>
              <span style={{ fontSize: '13px', fontWeight: '800', color: 'var(--color-navy)', lineHeight: 1 }}>
                {item.day || item.dueDate?.slice(-2) || '24'}
              </span>
            </div>

            <div style={{ minWidth: 0 }}>
              <div style={{ fontSize: 'var(--font-size-xs)', fontWeight: '700', color: 'var(--color-navy)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {item.title}
              </div>
              <div style={{ fontSize: '11px', color: 'var(--color-text-muted)', marginTop: '2px' }}>
                {item.institutionName ? `${item.institutionName} · ` : ''}Ref: <strong>{item.referenceId || item.id}</strong>
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexShrink: 0 }}>
            <StatusBadge status={item.status || item.urgency || 'Due soon'} />
            {item.linkTo && (
              <Link to={item.linkTo} className="btn btn-sm btn-outline" style={{ padding: '3px 6px' }}>
                <ArrowUpRight size={12} />
              </Link>
            )}
          </div>
        </div>
      ))}
    </div>
  );
};
