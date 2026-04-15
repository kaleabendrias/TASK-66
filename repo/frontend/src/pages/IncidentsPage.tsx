import React, { useEffect, useState } from 'react';
import { useAuthStore } from '@/state/authStore';
import { Incident } from '@/api/types';
import { getIncidents, getMyIncidents, createIncident } from '@/api/incidents';

const TYPES = ['ORDER_ISSUE', 'PRODUCT_DEFECT', 'ACCOUNT_ISSUE', 'POLICY_VIOLATION', 'EMERGENCY', 'OTHER'];
const SEVERITIES = ['LOW', 'NORMAL', 'HIGH', 'EMERGENCY'];

const severityBadge = (s: string) => {
  if (s === 'EMERGENCY' || s === 'HIGH') return 'badge badge-danger';
  if (s === 'NORMAL') return 'badge badge-warning';
  return 'badge badge-neutral';
};

const statusBadge = (s: string) => {
  if (s === 'RESOLVED' || s === 'CLOSED') return 'badge badge-success';
  if (s === 'OPEN') return 'badge badge-danger';
  return 'badge badge-warning';
};

const IncidentsPage: React.FC = () => {
  const user = useAuthStore((s) => s.user);
  const isMod = user?.role === 'MODERATOR' || user?.role === 'ADMINISTRATOR';
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ incidentType: 'ORDER_ISSUE', severity: 'NORMAL', title: '', description: '', address: '', crossStreet: '', sellerId: '' });
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    try {
      const data = isMod ? await getIncidents() : await getMyIncidents();
      setIncidents(data);
    } catch { setIncidents([]); }
    setLoading(false);
  };

  useEffect(() => { load(); }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    // sellerId is required for the risk analytics engine to attribute the
    // incident to the correct seller's 30-day repeat-incident window. Without
    // it, escalations against a seller would not roll up into their score.
    if (!form.sellerId.trim()) {
      setError('Seller ID is required so risk analytics can attribute the incident.');
      return;
    }
    const sellerIdNum = Number(form.sellerId);
    if (!Number.isFinite(sellerIdNum) || sellerIdNum <= 0) {
      setError('Seller ID must be a positive number.');
      return;
    }
    try {
      await createIncident({
        incidentType: form.incidentType,
        severity: form.severity,
        title: form.title,
        description: form.description,
        address: form.address || undefined,
        crossStreet: form.crossStreet || undefined,
        sellerId: sellerIdNum,
      });
      setShowForm(false);
      setForm({ incidentType: 'ORDER_ISSUE', severity: 'NORMAL', title: '', description: '', address: '', crossStreet: '', sellerId: '' });
      load();
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to create incident');
    }
  };

  const slaStatus = (incident: Incident) => {
    if (!incident.slaAckDeadline) return null;
    const deadline = new Date(incident.slaAckDeadline);
    const now = new Date();
    if (incident.status === 'OPEN' && deadline < now) return <span className="badge badge-danger">SLA BREACHED</span>;
    if (incident.status === 'OPEN') {
      const mins = Math.round((deadline.getTime() - now.getTime()) / 60000);
      return <span className="badge badge-warning">{mins}m to ack</span>;
    }
    return null;
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Incidents</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Cancel' : 'Report Incident'}
        </button>
      </div>

      {showForm && (
        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <div className="card-header">Report New Incident</div>
          <div className="card-body">
            {error && <div className="alert alert-danger">{error}</div>}
            <form onSubmit={handleCreate}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div className="form-group">
                  <label className="form-label">Type</label>
                  <select className="form-input" value={form.incidentType} onChange={(e) => setForm({ ...form, incidentType: e.target.value })}>
                    {TYPES.map((t) => <option key={t} value={t}>{t.replace(/_/g, ' ')}</option>)}
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Severity</label>
                  <select className="form-input" value={form.severity} onChange={(e) => setForm({ ...form, severity: e.target.value })}>
                    {SEVERITIES.map((s) => <option key={s} value={s}>{s}</option>)}
                  </select>
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">Title</label>
                <input className="form-input" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
              </div>
              <div className="form-group">
                <label className="form-label">Description</label>
                <textarea className="form-input" rows={4} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} required />
              </div>
              <div className="form-group">
                <label className="form-label">Seller ID (required)</label>
                <input
                  className="form-input"
                  type="number"
                  min="1"
                  aria-label="Seller ID"
                  placeholder="User ID of the seller this incident is filed against"
                  value={form.sellerId}
                  onChange={(e) => setForm({ ...form, sellerId: e.target.value })}
                />
                <small className="text-muted">
                  Drives seller-scoped risk analytics. Must reference an existing user.
                </small>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div className="form-group">
                  <label className="form-label">Address (optional)</label>
                  <input className="form-input" placeholder="e.g. 123 Main St" value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
                </div>
                <div className="form-group">
                  <label className="form-label">Cross Street (optional)</label>
                  <input className="form-input" placeholder="e.g. 5th Ave" value={form.crossStreet} onChange={(e) => setForm({ ...form, crossStreet: e.target.value })} />
                </div>
              </div>
              <div className="form-actions">
                <button type="submit" className="btn btn-primary">Submit Incident</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {loading ? (
        <p className="text-muted">Loading...</p>
      ) : incidents.length === 0 ? (
        <p className="text-muted">No incidents found.</p>
      ) : (
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>ID</th><th>Title</th><th>Type</th><th>Severity</th><th>Status</th><th>SLA</th><th>Escalation</th><th>Created</th>
              </tr>
            </thead>
            <tbody>
              {incidents.map((inc) => (
                <tr key={inc.id} style={{ cursor: 'pointer' }} onClick={() => window.location.href = `/incidents/${inc.id}`}>
                  <td>#{inc.id}</td>
                  <td>{inc.title}</td>
                  <td>{inc.incidentType.replace(/_/g, ' ')}</td>
                  <td><span className={severityBadge(inc.severity)}>{inc.severity}</span></td>
                  <td><span className={statusBadge(inc.status)}>{inc.status}</span></td>
                  <td>{slaStatus(inc)}</td>
                  <td>{inc.escalationLevel > 0 ? <span className="badge badge-danger">L{inc.escalationLevel}</span> : '-'}</td>
                  <td>{new Date(inc.createdAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default IncidentsPage;
