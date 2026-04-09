import client from './client';
import { MemberTier, BenefitPackage, BenefitItem, MemberProfile, PointsLedgerEntry } from './types';

export async function getTiers(): Promise<MemberTier[]> {
  const { data } = await client.get<MemberTier[]>('/tiers');
  return data;
}

export async function getMyProfile(): Promise<MemberProfile> {
  const { data } = await client.get<MemberProfile>('/members/me');
  return data;
}

export async function getProfileByUser(userId: number): Promise<MemberProfile> {
  const { data } = await client.get<MemberProfile>(`/members/${userId}`);
  return data;
}

export async function updatePhone(phone: string): Promise<MemberProfile> {
  const { data } = await client.put<MemberProfile>('/members/me/phone', { phone });
  return data;
}

export async function adjustSpend(amount: number, reference: string): Promise<MemberProfile> {
  const { data } = await client.post<MemberProfile>('/members/me/spend', { amount, reference });
  return data;
}

export async function getSpendHistory(): Promise<PointsLedgerEntry[]> {
  const { data } = await client.get<PointsLedgerEntry[]>('/members/me/spend/history');
  return data;
}

export async function getPackagesByTier(tierId: number): Promise<BenefitPackage[]> {
  const { data } = await client.get<BenefitPackage[]>(`/benefits/packages/tier/${tierId}`);
  return data;
}

export async function getItemsByPackage(packageId: number): Promise<BenefitItem[]> {
  const { data } = await client.get<BenefitItem[]>(`/benefits/items/package/${packageId}`);
  return data;
}

export async function redeemBenefit(benefitItemId: number, reference: string): Promise<void> {
  await client.post('/benefits/redeem', { benefitItemId, reference });
}
