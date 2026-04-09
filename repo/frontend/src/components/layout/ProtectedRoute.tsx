import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuthStore } from '@/state/authStore';
import { Role } from '@/api/types';

interface ProtectedRouteProps {
  children: React.ReactNode;
  roles?: Role[];
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children, roles }) => {
  const { isAuthenticated, user, loading } = useAuthStore();

  if (loading) {
    return <div className="page-loading">Loading...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (roles && user && !roles.includes(user.role)) {
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
};

export default ProtectedRoute;
