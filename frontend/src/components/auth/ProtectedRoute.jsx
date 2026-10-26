// SIH 26262 - ProtectedRoute Component with Role Authorization

import React from 'react';
import { Navigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { hasRoutePermission } from '../../utils/permissions';
import { ShieldAlert, ArrowLeft } from 'lucide-react';

export const ProtectedRoute = ({ children }) => {
  const { isAuthenticated, user, role } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Check if role has access to current path
  const isAuthorized = hasRoutePermission(role, location.pathname);

  if (!isAuthorized) {
    return (
      <div style={{ padding: '40px 20px', maxWidth: '680px', margin: '40px auto' }}>
        <div className="panel" style={{ borderTop: '4px solid var(--color-critical)' }}>
          <div className="panel-body" style={{ textAlign: 'center', padding: '32px 24px' }}>
            <div
              style={{
                width: '48px',
                height: '48px',
                borderRadius: '50%',
                background: 'var(--color-critical-bg)',
                color: 'var(--color-critical)',
                display: 'grid',
                placeItems: 'center',
                margin: '0 auto 16px',
              }}
            >
              <ShieldAlert size={28} />
            </div>
            <h2 style={{ fontSize: 'var(--font-size-xl)', color: 'var(--color-navy)', marginBottom: '8px' }}>
              Access Restricted · Role Scope Limitation
            </h2>
            <p style={{ color: 'var(--color-text-muted)', fontSize: 'var(--font-size-md)', marginBottom: '16px' }}>
              Your current logged-in role (<strong>{role}</strong>) does not have authorization to access route{' '}
              <code>{location.pathname}</code> under the AICTE Approval Process governance matrix.
            </p>
            <div style={{ background: '#F8FAFC', padding: '12px', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-sm)', marginBottom: '20px', fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
              Logged in as: <strong>{user?.name}</strong> ({user?.email}) · ID: {user?.id}
            </div>
            <Link to="/dashboard" className="btn btn-primary" style={{ display: 'inline-flex' }}>
              <ArrowLeft size={16} /> Return to Dashboard
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return children;
};
