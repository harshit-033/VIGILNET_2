// SIH 26262 - Standardized KPI Metric Card

import React from 'react';
import * as Icons from 'lucide-react';

export const MetricCard = ({ title, value, subtitle, iconName, tint = 'blue' }) => {
  const IconComponent = Icons[iconName] || Icons.Activity;

  const tintStyles = {
    blue: { bg: 'var(--color-blue-soft)', color: 'var(--color-blue)' },
    amber: { bg: 'var(--color-warning-bg)', color: 'var(--color-warning)' },
    red: { bg: 'var(--color-critical-bg)', color: 'var(--color-critical)' },
    teal: { bg: 'var(--color-teal-soft)', color: 'var(--color-teal)' },
    green: { bg: 'var(--color-success-bg)', color: 'var(--color-success)' },
  };

  const currentTint = tintStyles[tint] || tintStyles.blue;

  return (
    <div
      style={{
        background: 'var(--color-surface)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-sm)',
        padding: '16px',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        minHeight: '100px',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '8px' }}>
        <span style={{ fontSize: 'var(--font-size-xs)', fontWeight: '700', color: 'var(--color-text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
          {title}
        </span>
        <div
          style={{
            width: '32px',
            height: '32px',
            borderRadius: 'var(--radius-sm)',
            background: currentTint.bg,
            color: currentTint.color,
            display: 'grid',
            placeItems: 'center',
            flexShrink: 0,
          }}
        >
          <IconComponent size={18} />
        </div>
      </div>
      <div style={{ fontSize: 'var(--font-size-2xl)', fontWeight: '800', color: 'var(--color-navy)', marginTop: '8px', lineHeight: 1.1 }}>
        {value}
      </div>
      {subtitle && (
        <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)', marginTop: '6px' }}>
          {subtitle}
        </div>
      )}
    </div>
  );
};
