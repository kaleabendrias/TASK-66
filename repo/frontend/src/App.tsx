import React, { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from '@/state/authStore';
import AppLayout from '@/components/layout/AppLayout';
import ProtectedRoute from '@/components/layout/ProtectedRoute';
import LoginPage from '@/pages/LoginPage';
import RegisterPage from '@/pages/RegisterPage';
import DashboardPage from '@/pages/DashboardPage';
import ProductsPage from '@/pages/ProductsPage';
import ProductDetailPage from '@/pages/ProductDetailPage';
import OrdersPage from '@/pages/OrdersPage';
import UsersPage from '@/pages/UsersPage';
import NotFoundPage from '@/pages/NotFoundPage';
import ListingDiscoveryPage from '@/pages/ListingDiscoveryPage';
import ListingDetailPage from '@/pages/ListingDetailPage';
import SellerListingsPage from '@/pages/SellerListingsPage';
import InventoryPage from '@/pages/InventoryPage';
import FulfillmentPage from '@/pages/FulfillmentPage';
import MemberProfilePage from '@/pages/MemberProfilePage';
import IncidentsPage from '@/pages/IncidentsPage';
import IncidentDetailPage from '@/pages/IncidentDetailPage';
import AppealsPage from '@/pages/AppealsPage';
import ModeratorDashboardPage from '@/pages/ModeratorDashboardPage';
import AdminDashboardPage from '@/pages/AdminDashboardPage';

const App: React.FC = () => {
  const loadFromStorage = useAuthStore((s) => s.loadFromStorage);
  const loading = useAuthStore((s) => s.loading);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  useEffect(() => {
    loadFromStorage();
  }, [loadFromStorage]);

  // Show nothing while checking auth state on initial load
  const token = localStorage.getItem('token');
  if (token && loading && !isAuthenticated) {
    return <div className="page-loading">Loading...</div>;
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/discover" element={<ListingDiscoveryPage />} />
        <Route path="/listings/:slug" element={<ListingDetailPage />} />

        <Route
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/discover" element={<ListingDiscoveryPage />} />
          <Route path="/listings/:slug" element={<ListingDetailPage />} />
          <Route path="/products" element={<ProductsPage />} />
          <Route path="/products/:id" element={<ProductDetailPage />} />
          <Route path="/orders" element={<OrdersPage />} />
          <Route
            path="/my-listings"
            element={
              <ProtectedRoute roles={['SELLER', 'ADMINISTRATOR']}>
                <SellerListingsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/inventory"
            element={
              <ProtectedRoute roles={['SELLER', 'WAREHOUSE_STAFF', 'ADMINISTRATOR']}>
                <InventoryPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/fulfillment"
            element={
              <ProtectedRoute roles={['WAREHOUSE_STAFF', 'ADMINISTRATOR']}>
                <FulfillmentPage />
              </ProtectedRoute>
            }
          />
          <Route path="/profile" element={<MemberProfilePage />} />
          <Route path="/incidents" element={<IncidentsPage />} />
          <Route path="/incidents/:id" element={<IncidentDetailPage />} />
          <Route path="/appeals" element={<AppealsPage />} />
          <Route
            path="/moderator"
            element={
              <ProtectedRoute roles={['MODERATOR', 'ADMINISTRATOR']}>
                <ModeratorDashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin"
            element={
              <ProtectedRoute roles={['ADMINISTRATOR']}>
                <AdminDashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/users"
            element={
              <ProtectedRoute roles={['ADMINISTRATOR']}>
                <UsersPage />
              </ProtectedRoute>
            }
          />
        </Route>

        <Route path="/" element={<Navigate to="/discover" replace />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  );
};

export default App;
