import React, { useState, useEffect, useCallback } from 'react';
import { getWarehouses, getLowStockItems, getInventoryByProduct, adjustStock, getMyReservations, confirmReservation, cancelReservation } from '@/api/warehouses';
import { getProducts } from '@/api/products';
import { Warehouse, InventoryItem, Product, StockReservation } from '@/api/types';

const MOVEMENT_TYPES = ['RECEIPT', 'ADJUSTMENT', 'RETURN', 'TRANSFER'];

const InventoryPage: React.FC = () => {
  const [tab, setTab] = useState<'inventory' | 'reservations'>('inventory');
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [lowStock, setLowStock] = useState<InventoryItem[]>([]);
  const [inventory, setInventory] = useState<InventoryItem[]>([]);
  const [reservations, setReservations] = useState<StockReservation[]>([]);
  const [selectedWarehouse, setSelectedWarehouse] = useState<number | ''>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Adjust stock modal
  const [adjustItem, setAdjustItem] = useState<InventoryItem | null>(null);
  const [adjForm, setAdjForm] = useState({ quantityChange: 0, movementType: 'RECEIPT', referenceDocument: '', notes: '' });
  const [adjLoading, setAdjLoading] = useState(false);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [wh, prods, low] = await Promise.all([getWarehouses(), getProducts(), getLowStockItems()]);
      setWarehouses(wh);
      setProducts(prods);
      setLowStock(low);
    } catch { setError('Failed to load data'); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { loadData(); }, [loadData]);

  useEffect(() => {
    if (products.length === 0) return;
    const fetchInventory = async () => {
      try {
        const all = await Promise.all(products.map(p => getInventoryByProduct(p.id)));
        const flat = all.flat();
        setInventory(selectedWarehouse ? flat.filter(i => i.warehouseId === selectedWarehouse) : flat);
      } catch { /* ignore */ }
    };
    fetchInventory();
  }, [products, selectedWarehouse]);

  const loadReservations = useCallback(async () => {
    try { setReservations(await getMyReservations()); } catch { /* ignore */ }
  }, []);

  useEffect(() => { if (tab === 'reservations') loadReservations(); }, [tab, loadReservations]);

  const productName = (id: number) => products.find(p => p.id === id)?.name || `Product #${id}`;

  const handleAdjust = async () => {
    if (!adjustItem) return;
    setAdjLoading(true);
    try {
      await adjustStock({
        inventoryItemId: adjustItem.id,
        quantityChange: adjForm.quantityChange,
        movementType: adjForm.movementType,
        referenceDocument: adjForm.referenceDocument,
        notes: adjForm.notes,
      });
      setAdjustItem(null);
      setAdjForm({ quantityChange: 0, movementType: 'RECEIPT', referenceDocument: '', notes: '' });
      // Refresh inventory
      const all = await Promise.all(products.map(p => getInventoryByProduct(p.id)));
      const flat = all.flat();
      setInventory(selectedWarehouse ? flat.filter(i => i.warehouseId === Number(selectedWarehouse)) : flat);
      setLowStock(await getLowStockItems());
    } catch { setError('Failed to adjust stock'); }
    finally { setAdjLoading(false); }
  };

  const handleConfirm = async (id: number) => {
    try { await confirmReservation(id); loadReservations(); } catch { setError('Failed to confirm'); }
  };
  const handleCancel = async (id: number) => {
    try { await cancelReservation(id); loadReservations(); } catch { setError('Failed to cancel'); }
  };

  if (loading) return <div className="page-loading">Loading...</div>;

  return (
    <div className="page">
      <div className="page-header"><h1>Inventory Management</h1></div>
      {error && <div className="alert alert-danger">{error}</div>}

      {/* Low Stock Alerts */}
      {lowStock.length > 0 && (
        <div style={{ marginBottom: '1.5rem' }}>
          <h3>Low Stock Alerts</h3>
          {lowStock.map(item => (
            <div key={item.id} className="low-stock-alert">
              <strong>{productName(item.productId)}</strong> at {item.warehouseName} — Only {item.quantityAvailable} units left (threshold: {item.lowStockThreshold})
            </div>
          ))}
        </div>
      )}

      {/* Tabs */}
      <div className="tabs">
        <div className={`tab ${tab === 'inventory' ? 'active' : ''}`} onClick={() => setTab('inventory')}>Inventory</div>
        <div className={`tab ${tab === 'reservations' ? 'active' : ''}`} onClick={() => setTab('reservations')}>My Reservations</div>
      </div>

      {tab === 'inventory' && (
        <>
          {/* Warehouse selector */}
          <div className="form-group" style={{ maxWidth: 300, marginBottom: '1rem' }}>
            <label className="form-label">Warehouse</label>
            <select className="form-input" value={selectedWarehouse} onChange={e => setSelectedWarehouse(e.target.value ? Number(e.target.value) : '')}>
              <option value="">All Warehouses</option>
              {warehouses.map(w => <option key={w.id} value={w.id}>{w.name} ({w.code})</option>)}
            </select>
          </div>

          {/* Inventory table */}
          <div className="card">
            <div className="card-header">Product Inventory</div>
            <div className="table-wrapper">
              <table className="table">
                <thead>
                  <tr>
                    <th>Product</th><th>Warehouse</th><th>On Hand</th><th>Reserved</th><th>Available</th><th>Status</th><th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {inventory.length === 0 && <tr><td colSpan={7} style={{ textAlign: 'center' }}>No inventory items found</td></tr>}
                  {inventory.map(item => (
                    <tr key={item.id}>
                      <td>{productName(item.productId)}</td>
                      <td>{item.warehouseName}</td>
                      <td>{item.quantityOnHand}</td>
                      <td>{item.quantityReserved}</td>
                      <td>{item.quantityAvailable}</td>
                      <td>{item.lowStock && <span className="badge badge-danger">LOW STOCK</span>}</td>
                      <td><button className="btn btn-primary btn-sm" onClick={() => { setAdjustItem(item); setAdjForm({ quantityChange: 0, movementType: 'RECEIPT', referenceDocument: '', notes: '' }); }}>Adjust Stock</button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {tab === 'reservations' && (
        <div className="card">
          <div className="card-header">My Reservations</div>
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr><th>Inventory Item</th><th>Quantity</th><th>Status</th><th>Expires</th><th>Actions</th></tr>
              </thead>
              <tbody>
                {reservations.length === 0 && <tr><td colSpan={5} style={{ textAlign: 'center' }}>No reservations</td></tr>}
                {reservations.map(r => {
                  const expires = new Date(r.expiresAt);
                  const remaining = Math.max(0, Math.floor((expires.getTime() - Date.now()) / 60000));
                  return (
                    <tr key={r.id}>
                      <td>#{r.inventoryItemId}</td>
                      <td>{r.quantity}</td>
                      <td><span className={`badge ${r.status === 'HELD' ? 'badge-warning' : r.status === 'CONFIRMED' ? 'badge-success' : 'badge-neutral'}`}>{r.status}</span></td>
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
      )}

      {/* Adjust Stock Modal */}
      {adjustItem && (
        <div className="modal-overlay" onClick={() => setAdjustItem(null)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Adjust Stock — {productName(adjustItem.productId)}</h3>
              <button className="modal-close" onClick={() => setAdjustItem(null)}>&times;</button>
            </div>
            <div className="modal-body">
              <div className="form-group">
                <label className="form-label">Quantity Change</label>
                <input type="number" className="form-input" value={adjForm.quantityChange} onChange={e => setAdjForm({ ...adjForm, quantityChange: Number(e.target.value) })} />
                <small className="text-muted">Positive for receipt, negative for reduction</small>
              </div>
              <div className="form-group">
                <label className="form-label">Movement Type</label>
                <select className="form-input" value={adjForm.movementType} onChange={e => setAdjForm({ ...adjForm, movementType: e.target.value })}>
                  {MOVEMENT_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Reference Document</label>
                <input type="text" className="form-input" value={adjForm.referenceDocument} onChange={e => setAdjForm({ ...adjForm, referenceDocument: e.target.value })} />
              </div>
              <div className="form-group">
                <label className="form-label">Notes</label>
                <textarea className="form-input" rows={3} value={adjForm.notes} onChange={e => setAdjForm({ ...adjForm, notes: e.target.value })} />
              </div>
              <div className="form-actions">
                <button className="btn btn-secondary" onClick={() => setAdjustItem(null)}>Cancel</button>
                <button className="btn btn-primary" disabled={adjLoading} onClick={handleAdjust}>{adjLoading ? 'Saving...' : 'Submit Adjustment'}</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default InventoryPage;
