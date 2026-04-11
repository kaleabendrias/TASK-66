import React, { useEffect, useState } from 'react';
import { useAuthStore } from '@/state/authStore';
import { Appeal } from '@/api/types';
import { getAppeals, getMyAppeals, createAppeal, reviewAppeal } from '@/api/appeals';
import client from '@/api/client';

const MAX_FILES = 5;
const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'application/pdf'];

const statusBadge = (s: string) => {
  if (s === 'APPROVED') return 'badge badge-success';
  if (s === 'REJECTED') return 'badge badge-danger';
  if (s === 'UNDER_REVIEW') return 'badge badge-warning';
  return 'badge badge-info';
};

const AppealsPage: React.FC = () => {
  const user = useAuthStore((s) => s.user);
  const isMod = user?.role === 'MODERATOR' || user?.role === 'ADMINISTRATOR';
  const [appeals, setAppeals] = useState<Appeal[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ relatedEntityType: 'PRODUCT', relatedEntityId: '', reason: '' });
  const [files, setFiles] = useState<File[]>([]);
  const [fileError, setFileError] = useState('');
  const [error, setError] = useState('');
  const [reviewingId, setReviewingId] = useState<number | null>(null);
  const [reviewForm, setReviewForm] = useState({ status: 'APPROVED', reviewNotes: '' });

  const load = async () => {
    setLoading(true);
    try {
      const data = isMod ? await getAppeals() : await getMyAppeals();
      setAppeals(data);
    } catch { setAppeals([]); }
    setLoading(false);
  };

  useEffect(() => { load(); }, []);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFileError('');
    const selected = Array.from(e.target.files || []);
    if (files.length + selected.length > MAX_FILES) {
      setFileError(`Maximum ${MAX_FILES} files allowed.`);
      return;
    }
    for (const f of selected) {
      if (!ALLOWED_TYPES.includes(f.type)) {
        setFileError(`Invalid file type: ${f.name}. Only photos and PDFs are allowed.`);
        return;
      }
      if (f.size > MAX_FILE_SIZE) {
        setFileError(`File too large: ${f.name}. Maximum 10 MB per file.`);
        return;
      }
    }
    setFiles([...files, ...selected]);
  };

  const removeFile = (idx: number) => setFiles(files.filter((_, i) => i !== idx));

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      const appeal = await createAppeal({
        relatedEntityType: form.relatedEntityType,
        relatedEntityId: +form.relatedEntityId,
        reason: form.reason,
      });
      // Upload attached files as evidence
      for (const file of files) {
        const formData = new FormData();
        formData.append('file', file);
        await client.post(`/appeals/${appeal.id}/evidence`, formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        });
      }
      setShowForm(false);
      setForm({ relatedEntityType: 'PRODUCT', relatedEntityId: '', reason: '' });
      setFiles([]);
      load();
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to create appeal');
    }
  };

  const handleReview = async (appealId: number) => {
    try {
      await reviewAppeal(appealId, reviewForm.status, reviewForm.reviewNotes);
      setReviewingId(null);
      setReviewForm({ status: 'APPROVED', reviewNotes: '' });
      load();
    } catch {}
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Appeals</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Cancel' : 'Submit Appeal'}
        </button>
      </div>

      {showForm && (
        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <div className="card-header">Submit New Appeal</div>
          <div className="card-body">
            {error && <div className="alert alert-danger">{error}</div>}
            <form onSubmit={handleCreate}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div className="form-group">
                  <label className="form-label">Related Entity Type</label>
                  <select
                    className="form-input"
                    aria-label="Related Entity Type"
                    value={form.relatedEntityType}
                    onChange={(e) => setForm({ ...form, relatedEntityType: e.target.value })}
                  >
                    <option value="PRODUCT">Product</option>
                    <option value="ORDER">Order</option>
                    <option value="INCIDENT">Incident</option>
                    <option value="LISTING">Listing</option>
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label">Related Entity ID</label>
                  <input className="form-input" type="number" value={form.relatedEntityId} onChange={(e) => setForm({ ...form, relatedEntityId: e.target.value })} required />
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">Reason for Appeal</label>
                <textarea className="form-input" rows={4} value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} required />
              </div>
              <div className="form-group">
                <label className="form-label">Attachments (photos/PDF, max 10 MB each, max 5 files)</label>
                <input type="file" accept=".jpg,.jpeg,.png,.gif,.webp,.pdf" multiple onChange={handleFileChange} style={{ display: 'block', marginTop: '0.25rem' }} />
                {fileError && <span className="form-error">{fileError}</span>}
                {files.length > 0 && (
                  <div style={{ marginTop: '0.5rem' }}>
                    {files.map((f, i) => (
                      <div key={i} className="flex gap-sm" style={{ fontSize: '0.85rem', marginBottom: '0.25rem' }}>
                        <span>{f.name} ({(f.size / 1024 / 1024).toFixed(1)} MB)</span>
                        <button type="button" className="btn btn-danger btn-sm" onClick={() => removeFile(i)} style={{ padding: '0.125rem 0.4rem', fontSize: '0.75rem' }}>Remove</button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
              <div className="form-actions">
                <button type="submit" className="btn btn-primary">Submit Appeal</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {loading ? (
        <p className="text-muted">Loading...</p>
      ) : appeals.length === 0 ? (
        <p className="text-muted">No appeals found.</p>
      ) : (
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Entity</th>
                <th>Reason</th>
                <th>Status</th>
                <th>Created</th>
                {/* The review/closure column is visible to every role —
                    the submitting user needs to see *why* their appeal
                    was approved or rejected just as much as the
                    moderator does. Previously this data was gated
                    behind `isMod`, so non-moderators had no visibility
                    into the decision loop. */}
                <th>Review Outcome</th>
                {isMod && <th>Actions</th>}
              </tr>
            </thead>
            <tbody>
              {appeals.map((a) => {
                const isClosed = a.status === 'APPROVED' || a.status === 'REJECTED';
                return (
                  <tr key={a.id}>
                    <td>#{a.id}</td>
                    <td>{a.relatedEntityType} #{a.relatedEntityId}</td>
                    <td style={{ maxWidth: '300px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{a.reason}</td>
                    <td><span className={statusBadge(a.status)}>{a.status}</span></td>
                    <td>{new Date(a.createdAt).toLocaleDateString()}</td>
                    <td style={{ maxWidth: '320px' }}>
                      {isClosed ? (
                        <div className="appeal-review" style={{ fontSize: '0.85rem' }}>
                          {a.reviewNotes
                            ? <div><strong>Notes:</strong> {a.reviewNotes}</div>
                            : <div className="text-muted">No reviewer notes provided.</div>}
                          {a.reviewedAt && (
                            <div className="text-muted" style={{ fontSize: '0.78rem', marginTop: '0.15rem' }}>
                              Closed {new Date(a.reviewedAt).toLocaleString()}
                              {a.resolvedAt && a.resolvedAt !== a.reviewedAt && (
                                <> · resolved {new Date(a.resolvedAt).toLocaleDateString()}</>
                              )}
                              {a.reviewerId != null && <> · reviewer #{a.reviewerId}</>}
                            </div>
                          )}
                        </div>
                      ) : (
                        <span className="text-muted" style={{ fontSize: '0.8rem' }}>
                          {a.status === 'UNDER_REVIEW' ? 'Under review…' : 'Awaiting review'}
                        </span>
                      )}
                    </td>
                    {isMod && (
                      <td>
                        {(a.status === 'SUBMITTED' || a.status === 'UNDER_REVIEW') && (
                          reviewingId === a.id ? (
                            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                              <select className="form-input" style={{ width: 'auto' }} value={reviewForm.status} onChange={(e) => setReviewForm({ ...reviewForm, status: e.target.value })}>
                                <option value="APPROVED">Approve</option>
                                <option value="REJECTED">Reject</option>
                              </select>
                              <input className="form-input" style={{ width: '150px' }} placeholder="Notes" value={reviewForm.reviewNotes} onChange={(e) => setReviewForm({ ...reviewForm, reviewNotes: e.target.value })} />
                              <button className="btn btn-primary btn-sm" onClick={() => handleReview(a.id)}>Submit</button>
                              <button className="btn btn-secondary btn-sm" onClick={() => setReviewingId(null)}>Cancel</button>
                            </div>
                          ) : (
                            <button className="btn btn-primary btn-sm" onClick={() => setReviewingId(a.id)}>Review</button>
                          )
                        )}
                      </td>
                    )}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default AppealsPage;
