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
}): Promise<Incident> {
  const { data } = await client.post<Incident>('/incidents', payload);
  return data;
}

export async function acknowledgeIncident(id: number): Promise<Incident> {
  const { data } = await client.post<Incident>(`/incidents/${id}/acknowledge`);
  return data;
}

export async function updateIncidentStatus(id: number, status: string): Promise<Incident> {
  const { data } = await client.patch<Incident>(`/incidents/${id}/status`, { status });
  return data;
}

export async function getIncidentComments(id: number): Promise<IncidentComment[]> {
  const { data } = await client.get<IncidentComment[]>(`/incidents/${id}/comments`);
  return data;
}

export async function addIncidentComment(id: number, content: string): Promise<IncidentComment> {
  const { data } = await client.post<IncidentComment>(`/incidents/${id}/comments`, { content });
  return data;
}
