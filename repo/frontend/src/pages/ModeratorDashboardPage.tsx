import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Incident, Appeal } from '@/api/types';
import { getIncidents } from '@/api/incidents';
import { getAppeals } from '@/api/appeals';

const ModeratorDashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [appeals, setAppeals] = useState<Appeal[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([getIncidents(), getAppeals()])
      .then(([inc, app]) => { setIncidents(inc); setAppeals(app); })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const openIncidents = incidents.filter((i) => i.status === 'OPEN');
  const breachedIncidents = incidents.filter((i) => i.status === 'OPEN' && i.slaAckDeadline && new Date(i.slaAckDeadline) < new Date());
  const escalated = incidents.filter((i) => i.escalationLevel > 0 && i.status !== 'RESOLVED' && i.status !== 'CLOSED');
  const pendingAppeals = appeals.filter((a) => a.status === 'SUBMITTED');

  if (loading) return <div className="page"><p className="text-muted">Loading...</p></div>;

  return (
    <div className="page">
      <h1>Moderator Dashboard</h1>

      <div className="stats-grid" style={{ marginBottom: '2rem' }}>
        <div className="card stat-card">
          <div className="card-body">
            <div className="stat-number" style={{ color: 'var(--danger)' }}>{openIncidents.length}</div>
            <div className="stat-label">Open Incidents</div>
          </div>
        </div>
        <div className="card stat-card">
          <div className="card-body">
            <div className="stat-number" style={{ color: 'var(--danger)' }}>{breachedIncidents.length}</div>
            <div className="stat-label">SLA Breached</div>
          </div>
        </div>
        <div className="card stat-card">
          <div className="card-body">
            <div className="stat-number" style={{ color: 'var(--warning)' }}>{escalated.length}</div>
            <div className="stat-label">Escalated</div>
          </div>
        </div>
        <div className="card stat-card">
          <div className="card-body">
            <div className="stat-number" style={{ color: 'var(--primary)' }}>{pendingAppeals.length}</div>
            <div className="stat-label">Pending Appeals</div>
          </div>
        </div>
      </div>

      {breachedIncidents.length > 0 && (
        <div style={{ marginBottom: '1.5rem' }}>
          <h2>SLA Breaches - Immediate Attention Required</h2>
          {breachedIncidents.map((inc) => (
            <div key={inc.id} className="low-stock-alert" style={{ cursor: 'pointer' }} onClick={() => navigate(`/incidents/${inc.id}`)}>
              <strong>#{inc.id} {inc.title}</strong> — {inc.severity} — SLA deadline passed {Math.round((Date.now() - new Date(inc.slaAckDeadline!).getTime()) / 60000)} minutes ago
              {inc.escalationLevel > 0 && <span className="badge badge-danger ml-sm">Escalation L{inc.escalationLevel}</span>}
            </div>
          ))}
        </div>
      )}

      {escalated.length > 0 && (
        <div style={{ marginBottom: '1.5rem' }}>
          <h2>Escalated Incidents</h2>
          <div className="table-wrapper">
            <table className="table">
              <thead><tr><th>ID</th><th>Title</th><th>Severity</th><th>Level</th><th>Status</th></tr></thead>
              <tbody>
                {escalated.map((inc) => (
                  <tr key={inc.id} style={{ cursor: 'pointer' }} onClick={() => navigate(`/incidents/${inc.id}`)}>
                    <td>#{inc.id}</td>
                    <td>{inc.title}</td>
                    <td><span className="badge badge-danger">{inc.severity}</span></td>
                    <td><span className="badge badge-danger">L{inc.escalationLevel}</span></td>
                    <td><span className="badge badge-warning">{inc.status}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
        <div className="card">
          <div className="card-header">Recent Incidents</div>
          <div className="card-body">
            {incidents.slice(0, 10).map((inc) => (
              <div key={inc.id} style={{ padding: '0.5rem 0', borderBottom: '1px solid var(--neutral-200)', cursor: 'pointer' }} onClick={() => navigate(`/incidents/${inc.id}`)}>
                <div className="flex gap-sm">
                  <strong>#{inc.id}</strong>
                  <span>{inc.title}</span>
                  <span className={inc.status === 'OPEN' ? 'badge badge-danger' : inc.status === 'RESOLVED' ? 'badge badge-success' : 'badge badge-warning'}>{inc.status}</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="card">
          <div className="card-header">Pending Appeals</div>
          <div className="card-body">
            {pendingAppeals.length === 0 ? (
              <p className="text-muted">No pending appeals.</p>
            ) : (
              pendingAppeals.map((a) => (
                <div key={a.id} style={{ padding: '0.5rem 0', borderBottom: '1px solid var(--neutral-200)', cursor: 'pointer' }} onClick={() => navigate('/appeals')}>
                  <div className="flex gap-sm">
                    <strong>#{a.id}</strong>
                    <span>{a.relatedEntityType} #{a.relatedEntityId}</span>
                    <span className="badge badge-info">SUBMITTED</span>
                  </div>
                  <p style={{ fontSize: '0.8rem', marginTop: '0.25rem' }} className="text-muted">{a.reason.substring(0, 100)}...</p>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ModeratorDashboardPage;
