// SIH 26262 - Authentication Service connected to Spring Boot Backend
import { ROLES } from '../constants/roles';
import { apiClient } from './apiClient';

const AUTH_STORAGE_KEY = 'aicte_auth_user';
const TOKEN_STORAGE_KEY = 'aicte_auth_token';

export const authService = {
  getCurrentUser: () => {
    try {
      if (typeof window !== 'undefined' && window.location.search) {
        const params = new URLSearchParams(window.location.search);
        const devRole = params.get('devRole');
        if (devRole) {
          const user = {
            id: 'USR-RO-01',
            name: 'A. Sharma',
            role: devRole,
            email: 'ro.north@aicte-india.gov.in',
            region: 'Northern Region',
            designation: 'Regional Officer (North)'
          };
          localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(user));
          localStorage.setItem(TOKEN_STORAGE_KEY, 'dev-token-USR-RO-01');
          return user;
        }
      }
      const stored = localStorage.getItem(AUTH_STORAGE_KEY);
      if (stored) {
        return JSON.parse(stored);
      }
    } catch (e) {
      console.error('Failed to read auth from storage:', e);
    }
    return null;
  },

  loginByRole: async (role) => {
    try {
      const res = await apiClient('/auth/token', {
        method: 'POST',
        body: JSON.stringify({ role }),
      });
      if (res && res.access_token) {
        localStorage.setItem(TOKEN_STORAGE_KEY, res.access_token);
        localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(res.user));
        return res.user;
      }
      throw new Error('Authentication response did not return an access token.');
    } catch (e) {
      console.error('Backend loginByRole failed:', e);
      throw e;
    }
  },

  loginWithCredentials: async (email, role, password) => {
    try {
      const res = await apiClient('/auth/token', {
        method: 'POST',
        body: JSON.stringify({ username: email, role, password }),
      });
      if (res && res.access_token) {
        localStorage.setItem(TOKEN_STORAGE_KEY, res.access_token);
        localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(res.user));
        return res.user;
      }
      throw new Error('Authentication failed: Missing token in response.');
    } catch (e) {
      console.error('Backend loginWithCredentials failed:', e);
      throw e;
    }
  },

  logout: () => {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    localStorage.removeItem(TOKEN_STORAGE_KEY);
  },
};
