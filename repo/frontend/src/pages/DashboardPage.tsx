import React, { useEffect } from 'react';
import { useAuth } from '@/features/auth/useAuth';
import { useDashboard } from '@/features/dashboard/useDashboard';
import Card from '@/components/ui/Card';
import Badge from '@/components/ui/Badge';

const DashboardPage: React.FC = () => {
  const { user } = useAuth();
  const { stats, loading, fetchStats } = useDashboard();

  useEffect(() => {
    fetchStats();
  }, [fetchStats]);

  if (!user) return null;

  return (
    <div className="page">
      <h1>Dashboard</h1>
      <p className="text-muted">
        Welcome back, <strong>{user.displayName || user.username}</strong>!
        Your role is <Badge variant="info">{user.role}</Badge>
      </p>

      {loading ? (
        <p>Loading stats...</p>
      ) : (
        <div className="stats-grid">
          {stats.totalProducts !== undefined && (
            <Card title="Products" className="stat-card">
              <div className="stat-number">{stats.totalProducts}</div>
              <div className="stat-label">Total Products</div>
            </Card>
          )}
          {stats.pendingProducts !== undefined && stats.pendingProducts > 0 && (
            <Card title="Pending Review" className="stat-card">
              <div className="stat-number">{stats.pendingProducts}</div>
              <div className="stat-label">Products Pending</div>
            </Card>
          )}
          {stats.myOrders !== undefined && (
            <Card title="My Orders" className="stat-card">
              <div className="stat-number">{stats.myOrders}</div>
              <div className="stat-label">Orders Placed</div>
            </Card>
          )}
          {stats.totalOrders !== undefined && (
            <Card title="All Orders" className="stat-card">
              <div className="stat-number">{stats.totalOrders}</div>
              <div className="stat-label">Total Orders</div>
            </Card>
          )}
          {stats.totalUsers !== undefined && (
            <Card title="Users" className="stat-card">
              <div className="stat-number">{stats.totalUsers}</div>
              <div className="stat-label">Total Users</div>
            </Card>
          )}
        </div>
      )}
    </div>
  );
};

export default DashboardPage;
