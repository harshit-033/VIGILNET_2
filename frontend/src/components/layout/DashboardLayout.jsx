// SIH 26262 - Dashboard Layout Wrapper

import React from 'react';
import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Header } from './Header';

export const DashboardLayout = () => {
  return (
    <div style={{ display: 'flex', minHeight: '100vh', backgroundColor: 'var(--color-bg)' }}>
      {/* Role-Aware Navy Sidebar */}
      <Sidebar />

      {/* Main Workspace Frame */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
        <Header />

        <main style={{ flex: 1, padding: '24px 28px 40px', overflowY: 'auto' }}>
          <Outlet />
        </main>
      </div>
    </div>
  );
};
