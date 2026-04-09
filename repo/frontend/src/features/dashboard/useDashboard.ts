import { useState, useCallback } from 'react';
import { useAuthStore } from '@/state/authStore';
import * as productsApi from '@/api/products';
import * as ordersApi from '@/api/orders';
import * as usersApi from '@/api/users';

interface DashboardStats {
  totalProducts?: number;
  totalOrders?: number;
  totalUsers?: number;
  myOrders?: number;
  pendingProducts?: number;
}

export function useDashboard() {
  const [stats, setStats] = useState<DashboardStats>({});
  const [loading, setLoading] = useState(false);
  const user = useAuthStore((s) => s.user);

  const fetchStats = useCallback(async () => {
    if (!user) return;
    setLoading(true);
    try {
      const results: DashboardStats = {};

      const products = await productsApi.getProducts();
      results.totalProducts = products.length;
      results.pendingProducts = products.filter((p) => p.status === 'PENDING').length;

      if (['ADMINISTRATOR', 'WAREHOUSE_STAFF', 'MODERATOR'].includes(user.role)) {
        const orders = await ordersApi.getOrders();
        results.totalOrders = orders.length;
      }

      if (user.role === 'MEMBER') {
        try {
          const myOrders = await ordersApi.getMyOrders(user.id);
          results.myOrders = myOrders.length;
        } catch {
          results.myOrders = 0;
        }
      }

      if (user.role === 'ADMINISTRATOR') {
        const users = await usersApi.getUsers();
        results.totalUsers = users.length;
      }

      setStats(results);
    } catch {
      // silently fail stats
    } finally {
      setLoading(false);
    }
  }, [user]);

  return { stats, loading, fetchStats };
}
