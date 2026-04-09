import client from './client';
import { AuditLogEntry } from './types';

export async function getAuditLog(entityType: string, entityId: number): Promise<AuditLogEntry[]> {
  const { data } = await client.get<AuditLogEntry[]>(`/audit/${entityType}/${entityId}`);
  return data;
}
