// SIH 26262 - Application Router Configuration

import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from '../components/auth/ProtectedRoute';
import { DashboardLayout } from '../components/layout/DashboardLayout';

// Pages
import { LoginPage } from '../pages/Login/LoginPage';
import { DashboardPage } from '../pages/dashboards/DashboardPage';
import { ApplicationsPage } from '../pages/ApplicationsPage';
import { ApplicationDetailPage } from '../pages/ApplicationDetailPage';
import { EVCPage } from '../pages/EVCPage';
import { CompliancePage } from '../pages/CompliancePage';
import { DocumentsPage } from '../pages/DocumentsPage';
import { AlertsPage } from '../pages/AlertsPage';
import { ReportsPage } from '../pages/ReportsPage';

export const AppRouter = () => {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public Login Route */}
        <Route path="/login" element={<LoginPage />} />

        {/* Protected Dashboard & Operations Routes */}
        <Route
          element={
            <ProtectedRoute>
              <DashboardLayout />
            </ProtectedRoute>
          }
        >
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/applications" element={<ApplicationsPage />} />
          <Route path="/applications/:id" element={<ApplicationDetailPage />} />
          <Route path="/evc" element={<EVCPage />} />
          <Route path="/compliance" element={<CompliancePage />} />
          <Route path="/documents" element={<DocumentsPage />} />
          <Route path="/alerts" element={<AlertsPage />} />
          <Route path="/reports" element={<ReportsPage />} />
        </Route>

        {/* Default Catch-all */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
};
