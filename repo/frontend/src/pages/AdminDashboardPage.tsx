import React, { useEffect, useState } from 'react';
import { getUsers } from '@/api/users';
import { getHighRiskUsers, computeRiskScore, getRiskEvents } from '@/api/risk';
import { getAuditLog } from '@/api/audit';
import { getDeletionStatus } from '@/api/accountDeletion';
import { User, RiskScore, RiskEvent, AuditLogEntry } from '@/api/types';

const AdminDashboardPage: React.FC = () => {
  const [tab, setTab] = useState<'overview' | 'risk' | 'audit'>('overview');
  const [users, setUsers] = useState<User[]>([]);
  const [highRisk, setHighRisk] = useState<RiskScore[]>([]);
  const [selectedUser, setSelectedUser] = useState<number | null>(null);
  const [riskEvents, setRiskEvents] = useState<RiskEvent[]>([]);
  const [auditEntity, setAuditEntity] = useState({ type: 'USER', id: '' });
  const [auditLog, setAuditLog] = useState<AuditLogEntry[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([getUsers(), getHighRiskUsers(30)])
      .then(([u, r]) => { setUsers(u); setHighRisk(r); })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const handleComputeRisk = async (userId: number) => {
    await computeRiskScore(userId);
    const updated = await getHighRiskUsers(30);
    setHighRisk(updated);
  };

  const handleViewRiskEvents = async (userId: number) => {
    setSelectedUser(userId);
    const events = await getRiskEvents(userId);
    setRiskEvents(events);
  };

  const handleAuditSearch = async () => {
    if (!auditEntity.id) return;
    try {
      const log = await getAuditLog(auditEntity.type, +auditEntity.id);
      setAuditLog(log);
    } catch { setAuditLog([]); }
  };

  const riskColor = (score: number) => {
    if (score >= 70) return 'var(--danger)';
    if (score >= 40) return 'var(--warning)';
    return 'var(--success)';
  };

  if (loading) return <div className="page"><p className="text-muted">Loading...</p></div>;

  return (
    <div className="page">
      <h1>Administrator Dashboard</h1>

      <div className="tabs">
        <div className={`tab ${tab === 'overview' ? 'active' : ''}`} onClick={() => setTab('overview')}>Overview</div>
        <div className={`tab ${tab === 'risk' ? 'active' : ''}`} onClick={() => setTab('risk')}>Risk Analytics</div>
        <div className={`tab ${tab === 'audit' ? 'active' : ''}`} onClick={() => setTab('audit')}>Audit Log</div>
      </div>

      {tab === 'overview' && (
        <>
          <div className="stats-grid">
            <div className="card stat-card"><div className="card-body"><div className="stat-number">{users.length}</div><div className="stat-label">Total Users</div></div></div>
            <div className="card stat-card"><div className="card-body"><div className="stat-number">{users.filter((u) => u.enabled).length}</div><div className="stat-label">Active</div></div></div>
            <div className="card stat-card"><div className="card-body"><div className="stat-number" style={{ color: 'var(--danger)' }}>{highRisk.length}</div><div className="stat-label">High Risk</div></div></div>
            <div className="card stat-card"><div className="card-body"><div className="stat-number">{users.filter((u) => !u.enabled).length}</div><div className="stat-label">Disabled</div></div></div>
          </div>
          <div className="card" style={{ marginTop: '1.5rem' }}>
            <div className="card-header">Users by Role</div>
            <div className="card-body">
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '0.75rem' }}>
                {['GUEST', 'MEMBER', 'SELLER', 'WAREHOUSE_STAFF', 'MODERATOR', 'ADMINISTRATOR'].map((role) => (
                  <div key={role} className="flex gap-sm">
                    <span className="badge badge-info">{role}</span>
                    <span>{users.filter((u) => u.role === role).length}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
          <div className="card" style={{ marginTop: '1rem' }}>
            <div className="card-header">Compliance Notes</div>
            <div className="card-body" style={{ fontSize: '0.875rem' }}>
              <p>Audit logs are retained for 2 years per policy. Expired entries are purged daily at 02:00 UTC.</p>
              <p style={{ marginTop: '0.5rem' }}>Account deletion requests have a 30-day cooling-off period before processing.</p>
              <p style={{ marginTop: '0.5rem' }}>Risk analytics are computed from local on-premise data only. No external APIs or cloud services are used.</p>
            </div>
          </div>
        </>
      )}

      {tab === 'risk' && (
        <>
          <div className="card" style={{ marginBottom: '1.5rem' }}>
            <div className="card-header">High Risk Users (score &gt; 30)</div>
            <div className="card-body">
              {highRisk.length === 0 ? (
                <p className="text-muted">No high-risk users detected.</p>
              ) : (
                <div className="table-wrapper">
                  <table className="table">
                    <thead><tr><th>User ID</th><th>Score</th><th>Computed</th><th>Actions</th></tr></thead>
                    <tbody>
                      {highRisk.map((rs) => (
                        <tr key={rs.id}>
                          <td>#{rs.userId}</td>
                          <td><span style={{ color: riskColor(rs.score), fontWeight: 700 }}>{rs.score.toFixed(1)}</span></td>
                          <td>{new Date(rs.computedAt).toLocaleString()}</td>
                          <td className="flex gap-sm">
                            <button className="btn btn-secondary btn-sm" onClick={() => handleComputeRisk(rs.userId)}>Recompute</button>
                            <button className="btn btn-primary btn-sm" onClick={() => handleViewRiskEvents(rs.userId)}>View Events</button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>

          <div className="card" style={{ marginBottom: '1.5rem' }}>
            <div className="card-header">Compute Risk Score</div>
            <div className="card-body">
              <div className="flex gap-sm">
                {users.slice(0, 10).map((u) => (
                  <button key={u.id} className="btn btn-secondary btn-sm" onClick={() => handleComputeRisk(u.id)}>
                    {u.username}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {selectedUser && (
            <div className="card">
              <div className="card-header">Risk Events for User #{selectedUser}</div>
              <div className="card-body">
                {riskEvents.length === 0 ? (
                  <p className="text-muted">No risk events recorded.</p>
                ) : (
                  <div className="table-wrapper">
                    <table className="table">
                      <thead><tr><th>Event</th><th>Severity</th><th>Date</th><th>Details</th></tr></thead>
                      <tbody>
                        {riskEvents.map((e) => (
                          <tr key={e.id}>
                            <td>{e.eventType}</td>
                            <td><span className={e.severity === 'HIGH' ? 'badge badge-danger' : 'badge badge-warning'}>{e.severity}</span></td>
                            <td>{new Date(e.createdAt).toLocaleString()}</td>
                            <td style={{ maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis' }}>{e.details}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>
          )}
        </>
      )}

      {tab === 'audit' && (
        <>
          <div className="card" style={{ marginBottom: '1.5rem' }}>
            <div className="card-header">Search Audit Log</div>
            <div className="card-body">
              <div className="flex gap-sm">
                <select className="form-input" style={{ width: '200px' }} value={auditEntity.type} onChange={(e) => setAuditEntity({ ...auditEntity, type: e.target.value })}>
                  <option value="USER">User</option>
                  <option value="PRODUCT">Product</option>
                  <option value="ORDER">Order</option>
                  <option value="INCIDENT">Incident</option>
                  <option value="APPEAL">Appeal</option>
                  <option value="INVENTORY">Inventory</option>
                </select>
                <input className="form-input" style={{ width: '120px' }} type="number" placeholder="Entity ID" value={auditEntity.id} onChange={(e) => setAuditEntity({ ...auditEntity, id: e.target.value })} />
                <button className="btn btn-primary" onClick={handleAuditSearch}>Search</button>
              </div>
            </div>
          </div>

          {auditLog.length > 0 && (
            <div className="card">
              <div className="card-header">Results ({auditLog.length} entries)</div>
              <div className="card-body">
                <div className="table-wrapper">
                  <table className="table">
                    <thead><tr><th>Date</th><th>Action</th><th>Actor</th><th>IP</th><th>Retention Until</th></tr></thead>
                    <tbody>
                      {auditLog.map((a) => (
                        <tr key={a.id}>
                          <td>{new Date(a.createdAt).toLocaleString()}</td>
                          <td><span className="badge badge-info">{a.action}</span></td>
                          <td>{a.actorId ? `#${a.actorId}` : '-'}</td>
                          <td>{a.ipAddress || '-'}</td>
                          <td>{new Date(a.retentionExpiresAt).toLocaleDateString()}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}
          {auditLog.length === 0 && auditEntity.id && (
            <p className="text-muted">No audit entries found for {auditEntity.type} #{auditEntity.id}.</p>
          )}
        </>
      )}
    </div>
  );
};

export default AdminDashboardPage;
