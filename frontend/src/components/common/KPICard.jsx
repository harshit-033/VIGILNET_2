// SIH 26262 - Reusable Government KPI Card with Filter Navigation

import React from 'react';
import { useNavigate } from 'react-router-dom';
import * as Icons from 'lucide-react';

export const KPICard = ({
  title,
  value,
  subtitle,
  iconName = 'Activity',
  tint = 'blue',
  targetRoute,
  filterParams,
}) => {
  const navigate = useNavigate();
  const IconComponent = Icons[iconName] || Icons.Activity;

  const tintStyles = {
    blue: { bg: 'var(--color-blue-soft)', color: 'var(--color-blue)', border: '#C5DCF2' },
    amber: { bg: 'var(--color-warning-bg)', color: 'var(--color-warning)', border: '#F8DF9E' },
    red: { bg: 'var(--color-critical-bg)', color: 'var(--color-critical)', border: '#F5C6C2' },
    teal: { bg: 'var(--color-teal-soft)', color: 'var(--color-teal)', border: '#BFE7E5' },
    green: { bg: 'var(--color-success-bg)', color: 'var(--color-success)', border: '#C3E6D3' },
    purple: { bg: '#F2ECFB', color: '#6B47A5', border: '#D9C8F5' },
  };

  const currentTint = tintStyles[tint] || tintStyles.blue;

  const handleClick = () => {
    if (targetRoute) {
      navigate(targetRoute, { state: filterParams });
    }
  };

  return (
    <div
      onClick={handleClick}
      role={targetRoute ? 'button' : undefined}
      tabIndex={targetRoute ? 0 : undefined}
      style={{
        background: 'var(--color-surface)',
        border: '1px solid var(--color-border)',
        borderRadius: 'var(--radius-sm)',
        padding: '14px 16px',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        cursor: targetRoute ? 'pointer' : 'default',
        transition: 'border-color 0.15s, box-shadow 0.15s, background-color 0.15s',
      }}
      onMouseEnter={(e) => {
        if (targetRoute) {
          e.currentTarget.style.borderColor = 'var(--color-blue)';
          e.currentTarget.style.backgroundColor = '#FCFDFE';
        }
      }}
      onMouseLeave={(e) => {
        if (targetRoute) {
          e.currentTarget.style.borderColor = 'var(--color-border)';
          e.currentTarget.style.backgroundColor = 'var(--color-surface)';
        }
      }}
    >
      <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: '8px' }}>
        <span
          style={{
            fontSize: 'var(--font-size-xs)',
            fontWeight: '700',
            color: 'var(--color-text-muted)',
            textTransform: 'uppercase',
            letterSpacing: '0.04em',
          }}
        >
          {title}
        </span>
        <div
          style={{
            width: '28px',
            height: '28px',
            borderRadius: 'var(--radius-sm)',
            background: currentTint.bg,
            color: currentTint.color,
            border: `1px solid ${currentTint.border}`,
            display: 'grid',
            placeItems: 'center',
            flexShrink: 0,
          }}
        >
          <IconComponent size={16} />
        </div>
      </div>

      <div
        style={{
          fontSize: '24px',
          fontWeight: '800',
          color: 'var(--color-navy)',
          margin: '8px 0 2px',
          lineHeight: 1.1,
        }}
      >
        {value}
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span style={{ fontSize: '11px', color: 'var(--color-text-muted)' }}>
          {subtitle || 'Click to view records'}
        </span>
        {targetRoute && (
          <span style={{ fontSize: '11px', color: 'var(--color-blue)', fontWeight: '600' }}>
            ↗
          </span>
        )}
      </div>
    </div>
  );
};
