// SIH 26262 - Login Screen connected to AICTE Backend Authentication
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { ROLES } from '../../constants/roles';
import { Shield, CheckCircle2, ArrowRight, AlertCircle, Loader2 } from 'lucide-react';
import './LoginPage.css';

const ROLE_DEFAULT_EMAILS = {
  [ROLES.INSTITUTION_REP]: 'principal@abctech.edu.in',
  [ROLES.REGIONAL_OFFICER]: 'ro.north@aicte-india.gov.in',
  [ROLES.SCRUTINY_OFFICER]: 'scrutiny.north@aicte-india.gov.in',
  [ROLES.EVC_COORDINATOR]: 'evc.coord@aicte-india.gov.in',
  [ROLES.COMPLIANCE_REVIEWER]: 'compliance.rev@aicte-india.gov.in',
  [ROLES.SENIOR_MANAGEMENT]: 'advisor.approval@aicte-india.gov.in',
  [ROLES.ADMINISTRATOR]: 'admin@aicte-india.gov.in',
};

export const LoginPage = () => {
  const { login, isAuthenticated, availableRoles, loading } = useAuth();
  const navigate = useNavigate();

  const [selectedRole, setSelectedRole] = useState(ROLES.REGIONAL_OFFICER);
  const [email, setEmail] = useState(ROLE_DEFAULT_EMAILS[ROLES.REGIONAL_OFFICER]);
  const [password, setPassword] = useState('••••••••••••');
  const [errorMessage, setErrorMessage] = useState(null);

  // If already logged in, redirect to dashboard
  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  // When role changes, update suggested email
  const handleRoleChange = (role) => {
    setSelectedRole(role);
    setErrorMessage(null);
    if (ROLE_DEFAULT_EMAILS[role]) {
      setEmail(ROLE_DEFAULT_EMAILS[role]);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage(null);
    try {
      await login(email, selectedRole, password);
      navigate('/dashboard');
    } catch (err) {
      setErrorMessage(err.message || 'Authentication failed. Please verify server connection.');
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        {/* Left: Branding & Regulatory Context */}
        <div className="login-branding">
          <div>
            <div className="login-brand-top">
              <div className="login-brand-logo" aria-label="SAGE Logo">S</div>
              <div>
                <div style={{ fontSize: '11px', fontWeight: '800', letterSpacing: '0.08em', color: '#9BB4CD' }}>
                  SIH PROBLEM STATEMENT 26262
                </div>
                <div style={{ fontSize: '14px', fontWeight: '800', color: '#FFFFFF' }}>
                  SAGE PLATFORM
                </div>
              </div>
            </div>

            <h1>System for Approval Governance and Evaluation</h1>
            <p>
              SAGE · AICTE Approval and Compliance Platform | SIH Problem Statement 26262 · An orchestrated, accountable, and auditable governance platform for technical education approvals and compliance.
            </p>
          </div>

          <div className="login-features">
            <div className="login-feature-item">
              <CheckCircle2 size={15} color="#65D6BD" />
              <span>Multi-stage Approval Process Handbook (APH) orchestration</span>
            </div>
            <div className="login-feature-item">
              <CheckCircle2 size={15} color="#65D6BD" />
              <span>Dedicated EVC scheduling &amp; inspection report tracking</span>
            </div>
            <div className="login-feature-item">
              <CheckCircle2 size={15} color="#65D6BD" />
              <span>Continuous post-approval compliance &amp; statutory document validity</span>
            </div>
            <div className="login-feature-item">
              <CheckCircle2 size={15} color="#65D6BD" />
              <span>Role-governed workspaces with immutable audit trails</span>
            </div>
          </div>
        </div>

        {/* Right: Sign-in Form with 7 Roles */}
        <div className="login-form-side">
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
            <Shield size={18} color="var(--color-blue)" />
            <span style={{ fontSize: '10px', fontWeight: '800', textTransform: 'uppercase', letterSpacing: '0.08em', color: 'var(--color-blue)' }}>
              OFFICIAL ACCESS GATEWAY
            </span>
          </div>

          <h2 className="login-form-title">Authorized Sign In</h2>
          <p className="login-form-subtitle">
            Select your assigned role and official identity to connect to backend APIs.
          </p>

          {errorMessage && (
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                padding: '10px 12px',
                background: '#FDF2F2',
                border: '1px solid #F8B4B4',
                borderRadius: 'var(--radius-sm)',
                marginBottom: '14px',
                color: '#9B1C1C',
                fontSize: '12px',
              }}
            >
              <AlertCircle size={16} style={{ flexShrink: 0 }} />
              <span>{errorMessage}</span>
            </div>
          )}

          <form onSubmit={handleSubmit}>
            {/* Role Selector (All 7 Roles) */}
            <div className="form-group">
              <label className="form-label" htmlFor="roleSelect">
                Select Your Operational Role
              </label>
              <select
                id="roleSelect"
                value={selectedRole}
                onChange={(e) => handleRoleChange(e.target.value)}
                className="form-select"
                required
              >
                {availableRoles.map((role) => (
                  <option key={role} value={role}>
                    {role}
                  </option>
                ))}
              </select>
            </div>

            {/* Email / Username */}
            <div className="form-group">
              <label className="form-label" htmlFor="emailInput">
                Official Email / Username
              </label>
              <input
                id="emailInput"
                type="text"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="form-input"
                placeholder="name@aicte-india.gov.in"
                required
              />
            </div>

            {/* Password */}
            <div className="form-group">
              <label className="form-label" htmlFor="passwordInput">
                Password / Secure Passcode
              </label>
              <input
                id="passwordInput"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="form-input"
                required
              />
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading}
              style={{ width: '100%', padding: '10px', marginTop: '6px', fontSize: '13px', display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '6px' }}
            >
              {loading ? (
                <>
                  <Loader2 size={16} className="animate-spin" /> Authenticating...
                </>
              ) : (
                <>
                  Sign In to Workspace <ArrowRight size={16} />
                </>
              )}
            </button>
          </form>

          {/* Quick Role Chips */}
          <div className="quick-role-chips">
            <div className="quick-role-title">
              Quick Role Select:
            </div>
            <div className="chips-container">
              {availableRoles.map((r) => (
                <button
                  key={r}
                  type="button"
                  onClick={() => handleRoleChange(r)}
                  className={`role-chip ${selectedRole === r ? 'active' : ''}`}
                >
                  {r}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
