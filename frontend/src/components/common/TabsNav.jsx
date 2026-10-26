// SIH 26262 - Reusable Government TabsNav Component

import React from 'react';

export const TabsNav = ({ tabs = [], activeTab, onTabChange }) => {
  return (
    <div
      style={{
        display: 'flex',
        borderBottom: '1px solid var(--color-border)',
        backgroundColor: '#FFFFFF',
        padding: '0 16px',
        overflowX: 'auto',
        gap: '4px',
      }}
    >
      {tabs.map((tab) => {
        const tabKey = typeof tab === 'string' ? tab : tab.key;
        const tabLabel = typeof tab === 'string' ? tab : tab.label;
        const count = typeof tab === 'object' ? tab.count : null;
        const isActive = activeTab === tabKey;

        return (
          <button
            key={tabKey}
            onClick={() => onTabChange(tabKey)}
            style={{
              padding: '12px 14px',
              fontSize: '12px',
              fontWeight: isActive ? '700' : '500',
              color: isActive ? 'var(--color-navy)' : 'var(--color-text-muted)',
              borderBottom: isActive ? '3px solid var(--color-blue)' : '3px solid transparent',
              background: 'transparent',
              whiteSpace: 'nowrap',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              transition: 'color 0.15s, border-bottom 0.15s',
            }}
          >
            <span>{tabLabel}</span>
            {count !== null && count !== undefined && (
              <span
                style={{
                  fontSize: '10px',
                  fontWeight: '700',
                  padding: '1px 6px',
                  borderRadius: '10px',
                  backgroundColor: isActive ? 'var(--color-blue-soft)' : '#F0F4F8',
                  color: isActive ? 'var(--color-blue)' : 'var(--color-text-muted)',
                }}
              >
                {count}
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
};
