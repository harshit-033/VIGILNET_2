// SIH 26262 - Standardized Government Status Badge

import React from 'react';

export const StatusBadge = ({ status, variant }) => {
  const getBadgeClass = (s, v) => {
    if (v) return `badge-${v}`;
    const str = String(s || '').toLowerCase();
    if (/overdue|expired|failed|rejected|escalated/.test(str)) return 'badge-red';
    if (/due soon|pending|verification|returned|awaiting|warning|in progress/.test(str)) return 'badge-amber';
    if (/verified|valid|completed|delivered|active|complete|closed/.test(str)) return 'badge-green';
    if (/scheduled|scrutiny|unread|queue|review/.test(str)) return 'badge-blue';
    return 'badge-gray';
  };

  return (
    <span className={`badge ${getBadgeClass(status, variant)}`}>
      {status}
    </span>
  );
};
