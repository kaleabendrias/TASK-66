import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuthStore } from '@/state/authStore';
import { Incident, IncidentComment } from '@/api/types';
import { getIncident, getIncidentComments, addIncidentComment, acknowledgeIncident, updateIncidentStatus } from '@/api/incidents';

const IncidentDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const user = useAuthStore((s) => s.user);
  const isMod = user?.role === 'MODERATOR' || user?.role === 'ADMINISTRATOR';
  const [incident, setIncident] = useState<Incident | null>(null);
  const [comments, setComments] = useState<IncidentComment[]>([]);
  const [newComment, setNewComment] = useState('');
  const [loading, setLoading] = useState(true);

  const load = async () => {
    if (!id) return;
    setLoading(true);
    try {
      const [inc, cmts] = await Promise.all([getIncident(+id), getIncidentComments(+id)]);
      setIncident(inc);
      setComments(cmts);
    } catch {}
    setLoading(false);
  };

  useEffect(() => { load(); }, [id]);

  const handleAcknowledge = async () => {
    if (!id) return;
    await acknowledgeIncident(+id);
    load();
  };

  const handleStatusChange = async (status: string) => {
    if (!id) return;
    await updateIncidentStatus(+id, status);
    load();
  };

  const handleComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !newComment.trim()) return;
    await addIncidentComment(+id, newComment);
    setNewComment('');
    load();
  };

  if (loading) return <div className="page"><p className="text-muted">Loading...</p></div>;
  if (!incident) return <div className="page"><p className="text-muted">Incident not found.</p></div>;

  const slaBreached = incident.status === 'OPEN' && incident.slaAckDeadline && new Date(incident.slaAckDeadline) < new Date();

  return (
    <div className="page">
      <div className="page-header">
        <h1>Incident #{incident.id}: {incident.title}</h1>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1.5rem' }}>
        <div>
          <div className="card" style={{ marginBottom: '1.5rem' }}>
            <div className="card-header">Details</div>
            <div className="card-body">
              <p><strong>Type:</strong> {incident.incidentType.replace(/_/g, ' ')}</p>
              <p><strong>Severity:</strong> <span className={incident.severity === 'HIGH' || incident.severity === 'EMERGENCY' ? 'badge badge-danger' : 'badge badge-warning'}>{incident.severity}</span></p>
              <p><strong>Status:</strong> <span className={incident.status === 'RESOLVED' ? 'badge badge-success' : 'badge badge-info'}>{incident.status}</span></p>
              {incident.escalationLevel > 0 && <p><strong>Escalation Level:</strong> <span className="badge badge-danger">L{incident.escalationLevel}</span></p>}
              {slaBreached && <div className="alert alert-danger" style={{ marginTop: '0.5rem' }}>SLA BREACHED - Acknowledgment overdue</div>}
              <hr style={{ margin: '1rem 0', border: 'none', borderTop: '1px solid var(--neutral-200)' }} />
              <p style={{ whiteSpace: 'pre-wrap' }}>{incident.description}</p>
            </div>
          </div>

          <div className="card">
            <div className="card-header">Comments ({comments.length})</div>
            <div className="card-body">
              {comments.length === 0 && <p className="text-muted">No comments yet.</p>}
              {comments.map((c) => (
                <div key={c.id} style={{ padding: '0.75rem 0', borderBottom: '1px solid var(--neutral-200)' }}>
                  <div className="flex gap-sm">
                    <strong>User #{c.authorId}</strong>
                    <span className="text-muted" style={{ fontSize: '0.8rem' }}>{new Date(c.createdAt).toLocaleString()}</span>
                  </div>
                  <p style={{ marginTop: '0.25rem' }}>{c.content}</p>
                </div>
              ))}
              <form onSubmit={handleComment} style={{ marginTop: '1rem' }}>
                <textarea className="form-input" rows={3} placeholder="Add a comment..." value={newComment} onChange={(e) => setNewComment(e.target.value)} />
                <div className="form-actions"><button type="submit" className="btn btn-primary btn-sm">Post Comment</button></div>
              </form>
            </div>
          </div>
        </div>

        <div>
          <div className="card" style={{ marginBottom: '1rem' }}>
            <div className="card-header">Timeline</div>
            <div className="card-body" style={{ fontSize: '0.85rem' }}>
              <p><strong>Created:</strong> {new Date(incident.createdAt).toLocaleString()}</p>
              {incident.acknowledgedAt && <p><strong>Acknowledged:</strong> {new Date(incident.acknowledgedAt).toLocaleString()}</p>}
              {incident.resolvedAt && <p><strong>Resolved:</strong> {new Date(incident.resolvedAt).toLocaleString()}</p>}
              {incident.slaAckDeadline && <p><strong>Ack SLA:</strong> {new Date(incident.slaAckDeadline).toLocaleString()}</p>}
              {incident.slaResolveDeadline && <p><strong>Resolve SLA:</strong> {new Date(incident.slaResolveDeadline).toLocaleString()}</p>}
            </div>
          </div>

          {isMod && (
            <div className="card">
              <div className="card-header">Actions</div>
              <div className="card-body" style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                {incident.status === 'OPEN' && (
                  <button className="btn btn-primary btn-block" onClick={handleAcknowledge}>Acknowledge</button>
                )}
                {incident.status === 'ACKNOWLEDGED' && (
                  <button className="btn btn-primary btn-block" onClick={() => handleStatusChange('IN_PROGRESS')}>Start Work</button>
                )}
                {(incident.status === 'IN_PROGRESS' || incident.status === 'ACKNOWLEDGED') && (
                  <button className="btn btn-primary btn-block" style={{ background: 'var(--success)' }} onClick={() => handleStatusChange('RESOLVED')}>Resolve</button>
                )}
                {incident.status === 'RESOLVED' && (
                  <button className="btn btn-secondary btn-block" onClick={() => handleStatusChange('CLOSED')}>Close</button>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default IncidentDetailPage;
