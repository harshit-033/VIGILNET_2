// SIH 26262 - DashboardSection Layout Panel

import React from 'react';

export const DashboardSection = ({ title, subtitle, action, children, noPadding = false, style = {} }) => {
  return (
    <section className="panel" style={style}>
      {(title || action) && (
        <div className="panel-header">
          <div>
            {title && <h2 className="panel-title">{title}</h2>}
            {subtitle && <p className="panel-subtitle">{subtitle}</p>}
          </div>
          {action && <div>{action}</div>}
        </div>
      )}
      <div className="panel-body" style={{ padding: noPadding ? 0 : '16px' }}>
        {children}
      </div>
    </section>
  );
};
