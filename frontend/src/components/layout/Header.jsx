// SIH 26262 - Shared Portal Header Component

import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useData } from '../../context/DataContext';
import { NAVIGATION_ITEMS } from '../../utils/permissions';
import { LogOut, User, Shield, ChevronRight, RefreshCw } from 'lucide-react';

export const Header = () => {
  const { user, role, switchRole, logout, availableRoles } = useAuth();
  const { fetchApplicationsFromBackend, fetchInstitutionsFromBackend } = useData();
  const [isRefreshing, setIsRefreshing] = React.useState(false);
  const location = useLocation();
  const navigate = useNavigate();

  // Find current page label
  const currentNav = NAVIGATION_ITEMS.find((item) => {
    if (location.pathname === item.path) return true;
    if (location.pathname.startsWith('/applications/') && item.path === '/applications') return true;
    return false;
  });

  const pageTitle = currentNav?.label || 'Platform Workspace';

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const handleRoleChange = (e) => {
    const selectedRole = e.target.value;
    switchRole(selectedRole);
  };

  return (
    <header
      style={{
        height: 'var(--header-height)',
        background: '#FFFFFF',
        borderBottom: '1px solid var(--color-border)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '0 20px',
        position: 'sticky',
        top: 0,
        zIndex: 20,
      }}
    >
      {/* Left: Breadcrumbs & Page Title */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        <span style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)', fontWeight: '600' }}>
          AICTE Platform
        </span>
        <ChevronRight size={14} color="var(--color-text-light)" />
        <span style={{ fontSize: 'var(--font-size-md)', fontWeight: '700', color: 'var(--color-navy)' }}>
          {pageTitle}
        </span>
      </div>

      {/* Right: Quick Role Switcher, Current User, Role Badge, Logout */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
        {/* Role Switcher for instant evaluator testing */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <label
            htmlFor="headerRoleSelect"
            style={{
              fontSize: 'var(--font-size-xs)',
              fontWeight: '700',
              color: 'var(--color-text-muted)',
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
            }}
          >
            <Shield size={13} color="var(--color-blue)" /> Active Role:
          </label>
          <select
            id="headerRoleSelect"
            value={role || ''}
            onChange={handleRoleChange}
            className="form-select"
            style={{
              fontSize: 'var(--font-size-xs)',
              padding: '4px 8px',
              maxWidth: '190px',
              borderColor: 'var(--color-border)',
              backgroundColor: '#F8FAFC',
              fontWeight: '600',
              color: 'var(--color-navy)',
            }}
          >
            {availableRoles.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>
        </div>

        {/* User Info Capsule */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            padding: '4px 10px',
            background: '#F4F7FA',
            borderRadius: 'var(--radius-sm)',
            border: '1px solid var(--color-border-subtle)',
          }}
        >
          <div
            style={{
              width: '26px',
              height: '26px',
              borderRadius: '50%',
              background: 'var(--color-blue-soft)',
              color: 'var(--color-blue)',
              display: 'grid',
              placeItems: 'center',
              fontWeight: '700',
              fontSize: '11px',
            }}
          >
            <User size={14} />
          </div>
          <div style={{ lineHeight: 1.2 }}>
            <div style={{ fontSize: 'var(--font-size-xs)', fontWeight: '700', color: 'var(--color-navy)' }}>
              {user?.name || 'Authorized Official'}
            </div>
            <div style={{ fontSize: '10px', color: 'var(--color-text-muted)' }}>
              {user?.institutionName ? `${user.institutionName} (${user.institutionId})` : user?.region || 'All India'}
            </div>
          </div>
        </div>

        {/* Refresh Live Data Button */}
        <button
          onClick={async () => {
            setIsRefreshing(true);
            try {
              await Promise.all([fetchApplicationsFromBackend(), fetchInstitutionsFromBackend()]);
            } finally {
              setIsRefreshing(false);
            }
          }}
          disabled={isRefreshing}
          className="btn btn-outline btn-sm"
          title="Reload latest applications and institutions from backend API"
          style={{ display: 'inline-flex', alignItems: 'center', gap: '5px', fontSize: '11px', color: 'var(--color-navy)' }}
        >
          <RefreshCw size={13} className={isRefreshing ? 'animate-spin' : ''} /> {isRefreshing ? 'Refreshing...' : 'Refresh Live Data'}
        </button>

        {/* Logout Button */}
        <button
          onClick={handleLogout}
          className="btn btn-outline btn-sm"
          title="Sign out of platform"
          style={{ display: 'inline-flex', alignItems: 'center', gap: '5px', color: 'var(--color-critical)' }}
        >
          <LogOut size={14} /> Logout
        </button>
      </div>
    </header>
  );
};
