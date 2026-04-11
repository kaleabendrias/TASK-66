import client from './client';
import { Incident, IncidentComment } from './types';

export async function getIncidents(): Promise<Incident[]> {
  const { data } = await client.get<Incident[]>('/incidents');
  return data;
}

export async function getMyIncidents(): Promise<Incident[]> {
  const { data } = await client.get<Incident[]>('/incidents/my');
  return data;
}

export async function getIncident(id: number): Promise<Incident> {
  const { data } = await client.get<Incident>(`/incidents/${id}`);
  return data;
}

export async function createIncident(payload: {
  incidentType: string;
  severity: string;
  title: string;
  description: string;
  address?: string;
  crossStreet?: string;
  sellerId?: number | null;
}): Promise<Incident> {
  const { data } = await client.post<Incident>('/incidents', payload);
  return data;
}

export async function acknowledgeIncident(id: number): Promise<Incident> {
  const { data } = await client.post<Incident>(`/incidents/${id}/acknowledge`);
  return data;
}

export async function updateIncidentStatus(
  id: number,
  status: string,
  closureCode?: string,
): Promise<Incident> {
  const payload: { status: string; closureCode?: string } = { status };
  // Backend rejects RESOLVED without a closureCode; forward whatever the
  // caller supplied so the contract isn't implicitly lossy.
  if (closureCode !== undefined && closureCode !== null && closureCode !== '') {
    payload.closureCode = closureCode;
  }
  const { data } = await client.patch<Incident>(`/incidents/${id}/status`, payload);
  return data;
}

export async function resolveIncident(id: number, closureCode: string): Promise<Incident> {
  if (!closureCode || !closureCode.trim()) {
    throw new Error('Closure code is required to resolve an incident');
  }
  return updateIncidentStatus(id, 'RESOLVED', closureCode.trim());
}

export async function getIncidentComments(id: number): Promise<IncidentComment[]> {
  const { data } = await client.get<IncidentComment[]>(`/incidents/${id}/comments`);
  return data;
}

export async function addIncidentComment(id: number, content: string): Promise<IncidentComment> {
  const { data } = await client.post<IncidentComment>(`/incidents/${id}/comments`, { content });
  return data;
}
