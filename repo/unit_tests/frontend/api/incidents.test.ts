import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/api/client', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
  },
}));

import client from '@/api/client';
import {
  getIncidents,
  getMyIncidents,
  getIncident,
  createIncident,
  acknowledgeIncident,
  updateIncidentStatus,
  resolveIncident,
  getIncidentComments,
  addIncidentComment,
} from '@/api/incidents';

describe('incidents API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('getIncidents calls GET /incidents', async () => {
    (client.get as any).mockResolvedValue({ data: [] });
    const result = await getIncidents();
    expect(client.get).toHaveBeenCalledWith('/incidents');
    expect(result).toEqual([]);
  });

  it('getMyIncidents calls GET /incidents/my', async () => {
    (client.get as any).mockResolvedValue({ data: [{ id: 1 }] });
    await getMyIncidents();
    expect(client.get).toHaveBeenCalledWith('/incidents/my');
  });

  it('getIncident calls GET /incidents/:id', async () => {
    (client.get as any).mockResolvedValue({ data: { id: 5 } });
    const result = await getIncident(5);
    expect(client.get).toHaveBeenCalledWith('/incidents/5');
    expect(result.id).toBe(5);
  });

  it('createIncident calls POST /incidents', async () => {
    const payload = {
      incidentType: 'FRAUD',
      severity: 'HIGH',
      title: 'Test',
      description: 'Desc',
    };
    (client.post as any).mockResolvedValue({ data: { id: 1, ...payload } });
    const result = await createIncident(payload);
    expect(client.post).toHaveBeenCalledWith('/incidents', payload);
    expect(result.title).toBe('Test');
  });

  it('acknowledgeIncident calls POST /incidents/:id/acknowledge', async () => {
    (client.post as any).mockResolvedValue({ data: { id: 1, status: 'ACKNOWLEDGED' } });
    const result = await acknowledgeIncident(1);
    expect(client.post).toHaveBeenCalledWith('/incidents/1/acknowledge');
    expect(result.status).toBe('ACKNOWLEDGED');
  });

  it('updateIncidentStatus calls PATCH /incidents/:id/status without closureCode by default', async () => {
    (client.patch as any).mockResolvedValue({ data: { id: 1, status: 'ACKNOWLEDGED' } });
    const result = await updateIncidentStatus(1, 'ACKNOWLEDGED');
    expect(client.patch).toHaveBeenCalledWith('/incidents/1/status', { status: 'ACKNOWLEDGED' });
    expect(result.status).toBe('ACKNOWLEDGED');
  });

  it('updateIncidentStatus forwards closureCode when supplied', async () => {
    (client.patch as any).mockResolvedValue({ data: { id: 2, status: 'RESOLVED', closureCode: 'FIXED' } });
    await updateIncidentStatus(2, 'RESOLVED', 'FIXED');
    expect(client.patch).toHaveBeenCalledWith('/incidents/2/status', {
      status: 'RESOLVED',
      closureCode: 'FIXED',
    });
  });

  it('updateIncidentStatus omits empty closureCode strings', async () => {
    (client.patch as any).mockResolvedValue({ data: { id: 3, status: 'IN_PROGRESS' } });
    await updateIncidentStatus(3, 'IN_PROGRESS', '');
    expect(client.patch).toHaveBeenCalledWith('/incidents/3/status', { status: 'IN_PROGRESS' });
  });

  it('resolveIncident requires a closure code and forwards it to the PATCH endpoint', async () => {
    (client.patch as any).mockResolvedValue({ data: { id: 5, status: 'RESOLVED', closureCode: 'REFUNDED' } });
    const result = await resolveIncident(5, 'REFUNDED');
    expect(client.patch).toHaveBeenCalledWith('/incidents/5/status', {
      status: 'RESOLVED',
      closureCode: 'REFUNDED',
    });
    expect(result.status).toBe('RESOLVED');
  });

  it('resolveIncident throws synchronously when closureCode is blank', async () => {
    await expect(resolveIncident(6, '   ')).rejects.toThrow(/closure code is required/i);
    expect(client.patch).not.toHaveBeenCalled();
  });

  it('getIncidentComments calls GET /incidents/:id/comments', async () => {
    (client.get as any).mockResolvedValue({ data: [{ id: 1, content: 'Hi' }] });
    const result = await getIncidentComments(3);
    expect(client.get).toHaveBeenCalledWith('/incidents/3/comments');
    expect(result).toHaveLength(1);
  });

  it('addIncidentComment calls POST /incidents/:id/comments', async () => {
    (client.post as any).mockResolvedValue({ data: { id: 1, content: 'Note' } });
    const result = await addIncidentComment(3, 'Note');
    expect(client.post).toHaveBeenCalledWith('/incidents/3/comments', { content: 'Note' });
    expect(result.content).toBe('Note');
  });
});
