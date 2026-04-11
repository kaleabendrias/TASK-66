import React, { useState, useEffect, useCallback } from 'react';
import {
  getWarehouses,
  getLowStockItems,
  getInventoryByProduct,
  getMyReservations,
  confirmReservation,
  cancelReservation,
  recordInbound,
  recordOutbound,
  recordStocktake,
} from '@/api/warehouses';
import { getProducts } from '@/api/products';
import { Warehouse, InventoryItem, Product, StockReservation } from '@/api/types';

// These three operations map to dedicated backend endpoints
// (/inventory/inbound, /inventory/outbound, /inventory/stocktake), each of
// which writes a typed movement record and is audited independently. Do not
// fold them back into a generic "adjustment" — the endpoints diverge on
// validation and expected payload shape.
type MovementKind = 'INBOUND' | 'OUTBOUND' | 'STOCKTAKE';

const MOVEMENT_KINDS: { value: MovementKind; label: string; helper: string }[] = [
  { value: 'INBOUND', label: 'Inbound Receipt', helper: 'Stock received from a supplier or returned into inventory.' },
  { value: 'OUTBOUND', label: 'Outbound Movement', helper: 'Stock leaving the warehouse (shipment, write-off, transfer out).' },
  { value: 'STOCKTAKE', label: 'Stocktake Count', helper: 'Absolute physical count; the system reconciles any delta.' },
];

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

  // Movement modal state
  const [movementItem, setMovementItem] = useState<InventoryItem | null>(null);
  const [movementKind, setMovementKind] = useState<MovementKind>('INBOUND');
  const [quantity, setQuantity] = useState<number>(0);
  const [referenceDocument, setReferenceDocument] = useState('');
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

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

  const refreshInventory = useCallback(async (currentProducts: Product[]) => {
    const all = await Promise.all(currentProducts.map(p => getInventoryByProduct(p.id)));
    const flat = all.flat();
    setInventory(selectedWarehouse ? flat.filter(i => i.warehouseId === Number(selectedWarehouse)) : flat);
  }, [selectedWarehouse]);

  useEffect(() => {
    if (products.length === 0) return;
    refreshInventory(products).catch(() => { /* ignore */ });
  }, [products, refreshInventory]);

  const loadReservations = useCallback(async () => {
    try { setReservations(await getMyReservations()); } catch { /* ignore */ }
  }, []);

  useEffect(() => { if (tab === 'reservations') loadReservations(); }, [tab, loadReservations]);

  const productName = (id: number) => products.find(p => p.id === id)?.name || `Product #${id}`;

  const openMovement = (item: InventoryItem, kind: MovementKind = 'INBOUND') => {
    setMovementItem(item);
    setMovementKind(kind);
    setQuantity(0);
    setReferenceDocument('');
    setNotes('');
    setFormError(null);
  };

  const closeMovement = () => {
    setMovementItem(null);
    setFormError(null);
  };

  const submitMovement = async () => {
    if (!movementItem) return;
    setFormError(null);
    if (movementKind !== 'STOCKTAKE' && quantity <= 0) {
      setFormError('Quantity must be greater than zero for inbound/outbound movements.');
      return;
    }
    if (movementKind === 'STOCKTAKE' && quantity < 0) {
      setFormError('Counted quantity cannot be negative.');
      return;
    }
    setSubmitting(true);
    try {
      if (movementKind === 'INBOUND') {
        await recordInbound({
          inventoryItemId: movementItem.id,
          quantity,
          referenceDocument,
          notes,
        });
      } else if (movementKind === 'OUTBOUND') {
        await recordOutbound({
          inventoryItemId: movementItem.id,
          quantity,
          referenceDocument,
          notes,
        });
      } else {
        await recordStocktake({
          productId: movementItem.productId,
          warehouseId: movementItem.warehouseId,
          countedQuantity: quantity,
          referenceDocument,
        });
      }
      closeMovement();
      await refreshInventory(products);
      setLowStock(await getLowStockItems());
    } catch {
      setFormError('Failed to record movement. Check the values and try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleConfirm = async (id: number) => {
    try { await confirmReservation(id); loadReservations(); } catch { setError('Failed to confirm'); }
  };
  const handleCancel = async (id: number) => {
    try { await cancelReservation(id); loadReservations(); } catch { setError('Failed to cancel'); }
  };

  if (loading) return <div className="page-loading">Loading...</div>;

  const activeKind = MOVEMENT_KINDS.find(k => k.value === movementKind) ?? MOVEMENT_KINDS[0];
  const quantityLabel = movementKind === 'STOCKTAKE' ? 'Counted quantity' : 'Quantity';

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
                      <td>
                        <div className="flex gap-sm">
                          <button className="btn btn-primary btn-sm" onClick={() => openMovement(item, 'INBOUND')}>Inbound</button>
                          <button className="btn btn-secondary btn-sm" onClick={() => openMovement(item, 'OUTBOUND')}>Outbound</button>
                          <button className="btn btn-secondary btn-sm" onClick={() => openMovement(item, 'STOCKTAKE')}>Stocktake</button>
                        </div>
                      </td>
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

      {/* Movement Modal */}
      {movementItem && (
        <div className="modal-overlay" onClick={closeMovement}>
          <div className="modal-content" onClick={e => e.stopPropagation()} role="dialog" aria-label="Record inventory movement">
            <div className="modal-header">
              <h3>{activeKind.label} — {productName(movementItem.productId)}</h3>
              <button className="modal-close" onClick={closeMovement}>&times;</button>
            </div>
            <div className="modal-body">
              <p className="text-muted" style={{ marginBottom: '0.75rem' }}>{activeKind.helper}</p>
              <div className="form-group">
                <label className="form-label">Movement type</label>
                <select
                  className="form-input"
                  value={movementKind}
                  onChange={e => setMovementKind(e.target.value as MovementKind)}
                  aria-label="Movement type"
                >
                  {MOVEMENT_KINDS.map(k => <option key={k.value} value={k.value}>{k.label}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">{quantityLabel}</label>
                <input
                  type="number"
                  className="form-input"
                  value={quantity}
                  min={0}
                  onChange={e => setQuantity(Number(e.target.value))}
                  aria-label={quantityLabel}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Reference document</label>
                <input type="text" className="form-input" value={referenceDocument} onChange={e => setReferenceDocument(e.target.value)} />
              </div>
              {movementKind !== 'STOCKTAKE' && (
                <div className="form-group">
                  <label className="form-label">Notes</label>
                  <textarea className="form-input" rows={3} value={notes} onChange={e => setNotes(e.target.value)} />
                </div>
              )}
              {formError && <div className="alert alert-danger" role="alert">{formError}</div>}
              <div className="form-actions">
                <button className="btn btn-secondary" onClick={closeMovement} disabled={submitting}>Cancel</button>
                <button className="btn btn-primary" disabled={submitting} onClick={submitMovement}>
                  {submitting ? 'Saving...' : `Record ${activeKind.label}`}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default InventoryPage;
