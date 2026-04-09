import React, { useEffect, useState } from 'react';
import { useAuth } from '@/features/auth/useAuth';
import { getOrders, getMyOrders, updateOrderStatus } from '@/api/orders';
import { Order, OrderStatus } from '@/api/types';
import Table from '@/components/ui/Table';
import Badge from '@/components/ui/Badge';
import Button from '@/components/ui/Button';

const statusVariant = (status: string) => {
  switch (status) {
    case 'DELIVERED': return 'success' as const;
    case 'SHIPPED': return 'info' as const;
    case 'CONFIRMED': return 'info' as const;
    case 'PLACED': return 'warning' as const;
    case 'CANCELLED': return 'danger' as const;
    default: return 'neutral' as const;
  }
};

const statusFlow: OrderStatus[] = ['PLACED', 'CONFIRMED', 'SHIPPED', 'DELIVERED'];

const OrdersPage: React.FC = () => {
  const { user, hasAnyRole } = useAuth();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const canManageOrders = hasAnyRole('WAREHOUSE_STAFF', 'ADMINISTRATOR');

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        if (user?.role === 'MEMBER') {
          setOrders(await getMyOrders(user.id));
        } else {
          setOrders(await getOrders());
        }
      } catch {
        setError('Failed to load orders');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [user]);

  const handleStatusUpdate = async (orderId: number, status: OrderStatus) => {
    try {
      const updated = await updateOrderStatus(orderId, status);
      setOrders((prev) => prev.map((o) => (o.id === orderId ? updated : o)));
    } catch {
      setError('Failed to update order status');
    }
  };

  const getNextStatus = (current: string): OrderStatus | null => {
    const idx = statusFlow.indexOf(current as OrderStatus);
    if (idx >= 0 && idx < statusFlow.length - 1) {
      return statusFlow[idx + 1];
    }
    return null;
  };

  return (
    <div className="page">
      <h1>Orders</h1>
      {error && <div className="alert alert-danger">{error}</div>}
      {loading ? (
        <p>Loading orders...</p>
      ) : orders.length === 0 ? (
        <p>No orders found.</p>
      ) : (
        <Table headers={['ID', 'Product ID', 'Quantity', 'Total', 'Status', ...(canManageOrders ? ['Actions'] : [])]}>
          {orders.map((o) => {
            const nextStatus = getNextStatus(o.status);
            return (
              <tr key={o.id}>
                <td>{o.id}</td>
                <td>{o.productId}</td>
                <td>{o.quantity}</td>
                <td>${Number(o.totalPrice).toFixed(2)}</td>
                <td><Badge variant={statusVariant(o.status)}>{o.status}</Badge></td>
                {canManageOrders && (
                  <td>
                    {nextStatus && (
                      <Button size="sm" onClick={() => handleStatusUpdate(o.id, nextStatus)}>
                        Mark {nextStatus}
                      </Button>
                    )}
                    {o.status !== 'CANCELLED' && o.status !== 'DELIVERED' && (
                      <Button
                        size="sm"
                        variant="danger"
                        onClick={() => handleStatusUpdate(o.id, 'CANCELLED')}
                        className="ml-sm"
                      >
                        Cancel
                      </Button>
                    )}
                  </td>
                )}
              </tr>
            );
          })}
        </Table>
      )}
    </div>
  );
};

export default OrdersPage;
