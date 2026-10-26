// SIH 26262 - Shared Navy Sidebar Component with Role-Based Navigation

import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { getNavItemsForRole } from '../../utils/permissions';
import * as Icons from 'lucide-react';

export const Sidebar = () => {
  const { user, role } = useAuth();
  const navItems = getNavItemsForRole(role);

  return (
    <aside
      style={{
        width: 'var(--sidebar-width)',
        backgroundColor: 'var(--color-navy)',
        color: '#EAF1F7',
        display: 'flex',
        flexDirection: 'column',
        flexShrink: 0,
        height: '100vh',
        position: 'sticky',
        top: 0,
        borderRight: '1px solid rgba(255, 255, 255, 0.1)',
        zIndex: 30,
      }}
    >
      {/* Platform Branding */}
      <div
        style={{
          padding: '18px 16px',
          borderBottom: '1px solid rgba(255, 255, 255, 0.12)',
          display: 'flex',
          alignItems: 'center',
          gap: '12px',
        }}
      >
        <div
          style={{
            width: '36px',
            height: '36px',
            borderRadius: 'var(--radius-sm)',
            backgroundColor: '#FFFFFF',
            color: 'var(--color-navy)',
            display: 'grid',
            placeItems: 'center',
            fontSize: '18px',
            fontWeight: '900',
            flexShrink: 0,
          }}
        >
          A
        </div>
        <div>
          <div style={{ fontSize: '13px', fontWeight: '800', lineHeight: 1.25, color: '#FFFFFF', letterSpacing: '0.01em' }}>
            AICTE Approval &amp; Compliance
          </div>
          <div style={{ fontSize: '9px', fontWeight: '700', letterSpacing: '0.08em', color: '#9BB4CD', marginTop: '3px' }}>
            ORCHESTRATION PLATFORM
          </div>
        </div>
      </div>

      {/* Role & Scope Indicator */}
      <div
        style={{
          margin: '12px 14px 6px',
          padding: '8px 10px',
          backgroundColor: 'rgba(255, 255, 255, 0.08)',
          borderRadius: 'var(--radius-sm)',
          border: '1px solid rgba(255, 255, 255, 0.1)',
        }}
      >
        <div style={{ fontSize: '9px', textTransform: 'uppercase', letterSpacing: '0.06em', color: '#89A9C9', fontWeight: '700' }}>
          Current Access Scope
        </div>
        <div style={{ fontSize: '11px', fontWeight: '700', color: '#FFFFFF', marginTop: '2px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
          {role}
        </div>
        <div style={{ fontSize: '10px', color: '#B3C8DD', marginTop: '1px' }}>
          {user?.region || 'National'}
        </div>
      </div>

      {/* Navigation Section */}
      <div style={{ padding: '8px 14px 4px', fontSize: '10px', fontWeight: '700', color: '#7E9BB8', textTransform: 'uppercase', letterSpacing: '0.08em' }}>
        Core Modules ({navItems.length})
      </div>

      <nav style={{ padding: '4px 10px 16px', flex: 1, overflowY: 'auto' }}>
        {navItems.map((item) => {
          const IconComponent = Icons[item.icon] || Icons.Circle;
          return (
            <NavLink
              key={item.path}
              to={item.path}
              style={({ isActive }) => ({
                display: 'flex',
                alignItems: 'center',
                gap: '10px',
                padding: '9px 12px',
                margin: '2px 0',
                borderRadius: 'var(--radius-sm)',
                fontSize: '12px',
                fontWeight: isActive ? '700' : '500',
                color: isActive ? '#FFFFFF' : '#D1DFED',
                backgroundColor: isActive ? 'var(--color-navy-light)' : 'transparent',
                borderLeft: isActive ? '3px solid #65C5CB' : '3px solid transparent',
                transition: 'background-color 0.15s',
              })}
            >
              <IconComponent size={16} style={{ flexShrink: 0, opacity: 0.9 }} />
              <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {item.label}
              </span>
            </NavLink>
          );
        })}
      </nav>

      {/* Sidebar Footer */}
      <div
        style={{
          padding: '12px 14px',
          borderTop: '1px solid rgba(255, 255, 255, 0.1)',
          fontSize: '10px',
          color: '#89A4BD',
          lineHeight: 1.35,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '4px' }}>
          <span style={{ width: '6px', height: '6px', borderRadius: '50%', backgroundColor: '#65D6BD', display: 'inline-block' }}></span>
          <span style={{ color: '#DCE7F0', fontWeight: '700' }}>SIH 26262 Platform</span>
        </div>
        <div>Core Backend API (Port 8080)</div>
      </div>
    </aside>
  );
};
