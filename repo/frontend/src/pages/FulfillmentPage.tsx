import React, { useState, useEffect, useCallback } from 'react';
import { getOrders } from '@/api/orders';
import { getWarehouses } from '@/api/warehouses';
import { getFulfillmentByOrder, createFulfillment, advanceFulfillment, cancelFulfillment, getFulfillmentSteps } from '@/api/fulfillments';
import { Order, Warehouse, Fulfillment, FulfillmentStep } from '@/api/types';

const STEPS = ['PICK', 'PACK', 'SHIP', 'DELIVER'];

const FulfillmentPage: React.FC = () => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [fulfillments, setFulfillments] = useState<Record<number, Fulfillment | null>>({});
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [steps, setSteps] = useState<FulfillmentStep[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Create modal
  const [showCreate, setShowCreate] = useState<Order | null>(null);
  const [createWarehouse, setCreateWarehouse] = useState<number | ''>('');

  // Advance form
  const [advanceNotes, setAdvanceNotes] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  const loadOrders = useCallback(async () => {
    try {
      setLoading(true);
      const [ords, whs] = await Promise.all([getOrders(), getWarehouses()]);
      setOrders(ords);
      setWarehouses(whs);
      // Load fulfillments for all orders
      const map: Record<number, Fulfillment | null> = {};
      await Promise.all(ords.map(async o => {
        try { map[o.id] = await getFulfillmentByOrder(o.id); }
        catch { map[o.id] = null; }
      }));
      setFulfillments(map);
    } catch { setError('Failed to load orders'); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { loadOrders(); }, [loadOrders]);

  const loadSteps = useCallback(async (ful: Fulfillment) => {
    try { setSteps(await getFulfillmentSteps(ful.id)); } catch { setSteps([]); }
  }, []);

  useEffect(() => {
    if (selectedOrder && fulfillments[selectedOrder.id]) {
      loadSteps(fulfillments[selectedOrder.id]!);
    } else {
      setSteps([]);
    }
  }, [selectedOrder, fulfillments, loadSteps]);

  const handleCreate = async () => {
    if (!showCreate || !createWarehouse) return;
    setActionLoading(true);
    try {
      const key = `ful-${showCreate.id}-${Date.now()}`;
      const ful = await createFulfillment(showCreate.id, Number(createWarehouse), key);
      setFulfillments(prev => ({ ...prev, [showCreate.id]: ful }));
      setShowCreate(null);
      setCreateWarehouse('');
    } catch { setError('Failed to create fulfillment'); }
    finally { setActionLoading(false); }
  };

  const getNextStep = (ful: Fulfillment): string | null => {
    const completedSteps = steps.filter(s => s.completedAt).map(s => s.stepName);
    return STEPS.find(s => !completedSteps.includes(s)) || null;
  };

  const handleAdvance = async () => {
    if (!selectedOrder) return;
    const ful = fulfillments[selectedOrder.id];
    if (!ful) return;
    const next = getNextStep(ful);
    if (!next) return;
    setActionLoading(true);
    try {
      const updated = await advanceFulfillment(ful.id, next, advanceNotes);
      setFulfillments(prev => ({ ...prev, [selectedOrder.id]: updated }));
      setAdvanceNotes('');
      loadSteps(updated);
    } catch { setError('Failed to advance'); }
    finally { setActionLoading(false); }
  };

  const handleCancelFulfillment = async () => {
    if (!selectedOrder) return;
    const ful = fulfillments[selectedOrder.id];
    if (!ful) return;
    setActionLoading(true);
    try {
      const updated = await cancelFulfillment(ful.id);
      setFulfillments(prev => ({ ...prev, [selectedOrder.id]: updated }));
    } catch { setError('Failed to cancel fulfillment'); }
    finally { setActionLoading(false); }
  };

  const getStepStatus = (stepName: string): 'completed' | 'current' | 'pending' => {
    const completed = steps.filter(s => s.completedAt).map(s => s.stepName);
    if (completed.includes(stepName)) return 'completed';
    const nextStep = STEPS.find(s => !completed.includes(s));
    if (nextStep === stepName) return 'current';
    return 'pending';
  };

  if (loading) return <div className="page-loading">Loading...</div>;

  return (
    <div className="page">
      <div className="page-header"><h1>Fulfillment Management</h1></div>
      {error && <div className="alert alert-danger">{error}</div>}

      <div className="fulfillment-grid">
        {/* Orders list */}
        <div className="card">
          <div className="card-header">Orders</div>
          <div className="table-wrapper">
            <table className="table">
              <thead><tr><th>Order</th><th>Status</th><th>Qty</th><th>Fulfillment</th></tr></thead>
              <tbody>
                {orders.map(o => {
                  const ful = fulfillments[o.id];
                  return (
                    <tr key={o.id} onClick={() => setSelectedOrder(o)} style={{ cursor: 'pointer', background: selectedOrder?.id === o.id ? 'var(--primary-light)' : undefined }}>
                      <td>#{o.id}</td>
                      <td><span className={`badge ${o.status === 'DELIVERED' ? 'badge-success' : o.status === 'CANCELLED' ? 'badge-danger' : 'badge-info'}`}>{o.status}</span></td>
                      <td>{o.quantity}</td>
                      <td>
                        {ful ? (
                          <span className={`badge ${ful.status === 'COMPLETED' ? 'badge-success' : ful.status === 'CANCELLED' ? 'badge-danger' : 'badge-warning'}`}>{ful.status}</span>
                        ) : (
                          <button className="btn btn-primary btn-sm" onClick={e => { e.stopPropagation(); setShowCreate(o); }}>Create</button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>

        {/* Detail panel */}
        <div className="card">
          <div className="card-header">Fulfillment Detail</div>
          <div className="card-body">
            {!selectedOrder ? (
              <p className="text-muted">Select an order to view fulfillment details</p>
            ) : !fulfillments[selectedOrder.id] ? (
              <p className="text-muted">No fulfillment created for order #{selectedOrder.id}</p>
            ) : (() => {
              const ful = fulfillments[selectedOrder.id]!;
              const nextStep = getNextStep(ful);
              const canCancel = ['PENDING', 'PICKING'].includes(ful.status);
              return (
                <>
                  <p><strong>Order:</strong> #{ful.orderId}</p>
                  <p><strong>Status:</strong> <span className="badge badge-info">{ful.status}</span></p>
                  <p><strong>Warehouse:</strong> {warehouses.find(w => w.id === ful.warehouseId)?.name || ful.warehouseId}</p>
                  {ful.trackingInfo && <p><strong>Tracking:</strong> {ful.trackingInfo}</p>}

                  {/* Step Pipeline */}
                  <div className="step-pipeline">
                    {STEPS.map((step, i) => {
                      const status = getStepStatus(step);
                      return (
                        <React.Fragment key={step}>
                          {i > 0 && <div className={`step-connector ${status === 'completed' || (status === 'current' && i > 0 && getStepStatus(STEPS[i - 1]) === 'completed') ? '' : ''} ${getStepStatus(STEPS[i - 1]) === 'completed' ? 'completed' : ''}`} />}
                          <div className="step-node">
                            <div className={`step-circle ${status}`}>
                              {status === 'completed' ? '\u2713' : i + 1}
                            </div>
                            <span className="step-label">{step}</span>
                          </div>
                        </React.Fragment>
                      );
                    })}
                  </div>

                  {/* Advance step */}
                  {nextStep && ful.status !== 'CANCELLED' && ful.status !== 'COMPLETED' && (
                    <div style={{ marginTop: '1rem' }}>
                      <div className="form-group">
                        <label className="form-label">Notes for {nextStep}</label>
                        <input type="text" className="form-input" value={advanceNotes} onChange={e => setAdvanceNotes(e.target.value)} placeholder="Optional notes..." />
                      </div>
                      <button className="btn btn-primary" disabled={actionLoading} onClick={handleAdvance}>
                        {actionLoading ? 'Processing...' : `Advance to ${nextStep}`}
                      </button>
                    </div>
                  )}

                  {canCancel && (
                    <button className="btn btn-danger mt-md" disabled={actionLoading} onClick={handleCancelFulfillment}>Cancel Fulfillment</button>
                  )}

                  {/* Step history */}
                  {steps.length > 0 && (
                    <div style={{ marginTop: '1.5rem' }}>
                      <h3>Step History</h3>
                      <table className="table" style={{ marginTop: '0.5rem' }}>
                        <thead><tr><th>Step</th><th>Status</th><th>Notes</th><th>Completed</th></tr></thead>
                        <tbody>
                          {steps.map(s => (
                            <tr key={s.id}>
                              <td>{s.stepName}</td>
                              <td><span className={`badge ${s.completedAt ? 'badge-success' : 'badge-neutral'}`}>{s.status}</span></td>
                              <td>{s.notes || '—'}</td>
                              <td>{s.completedAt ? new Date(s.completedAt).toLocaleString() : '—'}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </>
              );
            })()}
          </div>
        </div>
      </div>

      {/* Create Fulfillment Modal */}
      {showCreate && (
        <div className="modal-overlay" onClick={() => setShowCreate(null)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Create Fulfillment for Order #{showCreate.id}</h3>
              <button className="modal-close" onClick={() => setShowCreate(null)}>&times;</button>
            </div>
            <div className="modal-body">
              <div className="form-group">
                <label className="form-label">Warehouse</label>
                <select className="form-input" value={createWarehouse} onChange={e => setCreateWarehouse(Number(e.target.value))}>
                  <option value="">Select warehouse...</option>
                  {warehouses.map(w => <option key={w.id} value={w.id}>{w.name} ({w.code})</option>)}
                </select>
              </div>
              <div className="form-actions">
                <button className="btn btn-secondary" onClick={() => setShowCreate(null)}>Cancel</button>
                <button className="btn btn-primary" disabled={!createWarehouse || actionLoading} onClick={handleCreate}>
                  {actionLoading ? 'Creating...' : 'Create Fulfillment'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default FulfillmentPage;
