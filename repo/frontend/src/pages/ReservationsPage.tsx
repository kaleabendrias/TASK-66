import React, { useEffect, useState, useCallback } from 'react';
import { getMyReservations, confirmReservation, cancelReservation } from '@/api/warehouses';
import { StockReservation } from '@/api/types';

/**
 * Member-facing reservations view. Deliberately isolated from InventoryPage —
 * the warehouse views call seller/staff-only endpoints (getProducts,
 * getInventoryByProduct, getLowStockItems, getWarehouses) which would 403
 * when a member visits /reservations. Keeping the two pages separate means
 * a member never triggers a forbidden prefetch.
 */
const ReservationsPage: React.FC = () => {
  const [reservations, setReservations] = useState<StockReservation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setError('');
    try {
      setLoading(true);
      const data = await getMyReservations();
      setReservations(data);
    } catch {
      setError('Failed to load reservations');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleConfirm = async (id: number) => {
    try {
      await confirmReservation(id);
      await load();
    } catch {
      setError('Failed to confirm reservation');
    }
  };

  const handleCancel = async (id: number) => {
    try {
      await cancelReservation(id);
      await load();
    } catch {
      setError('Failed to cancel reservation');
    }
  };

  if (loading) return <div className="page-loading">Loading...</div>;

  return (
    <div className="page">
      <div className="page-header"><h1>My Reservations</h1></div>
      {error && <div className="alert alert-danger" role="alert">{error}</div>}

      <div className="card">
        <div className="card-header">Held & confirmed reservations</div>
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>Inventory Item</th>
                <th>Quantity</th>
                <th>Status</th>
                <th>Expires</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {reservations.length === 0 && (
                <tr><td colSpan={5} style={{ textAlign: 'center' }} className="text-muted">No reservations.</td></tr>
              )}
              {reservations.map((r) => {
                const expires = new Date(r.expiresAt);
                const remaining = Math.max(0, Math.floor((expires.getTime() - Date.now()) / 60000));
                const statusClass = r.status === 'HELD'
                  ? 'badge-warning'
                  : r.status === 'CONFIRMED'
                    ? 'badge-success'
                    : 'badge-neutral';
                return (
                  <tr key={r.id}>
                    <td>#{r.inventoryItemId}</td>
                    <td>{r.quantity}</td>
                    <td><span className={`badge ${statusClass}`}>{r.status}</span></td>
                    <td>{remaining > 0 ? `${remaining} min remaining` : 'Expired'}</td>
                    <td>
                      {r.status === 'HELD' && (
                        <div className="flex gap-sm">
                          <button className="btn btn-primary btn-sm" onClick={() => handleConfirm(r.id)}>Confirm</button>
                          <button className="btn btn-danger btn-sm" onClick={() => handleCancel(r.id)}>Cancel</button>
                        </div>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default ReservationsPage;
