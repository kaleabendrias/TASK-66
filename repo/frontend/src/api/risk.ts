import client from './client';
import { RiskScore, RiskEvent } from './types';

export async function getRiskScore(userId: number): Promise<RiskScore> {
  const { data } = await client.get<RiskScore>(`/risk/score/${userId}`);
  return data;
}

export async function computeRiskScore(userId: number): Promise<RiskScore> {
  const { data } = await client.post<RiskScore>(`/risk/compute/${userId}`);
  return data;
}

export async function getHighRiskUsers(threshold?: number): Promise<RiskScore[]> {
  const { data } = await client.get<RiskScore[]>('/risk/high-risk', { params: { threshold } });
  return data;
}

export async function getRiskEvents(userId: number): Promise<RiskEvent[]> {
  const { data } = await client.get<RiskEvent[]>(`/risk/events/${userId}`);
  return data;
}
