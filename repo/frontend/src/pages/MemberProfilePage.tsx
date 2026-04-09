import React, { useState, useEffect, useCallback } from 'react';
import { getMyProfile, getTiers, getPackagesByTier, getItemsByPackage, redeemBenefit, getSpendHistory, updatePhone } from '@/api/members';
import { MemberProfile, MemberTier, BenefitPackage, BenefitItem, PointsLedgerEntry } from '@/api/types';

const TIER_CLASSES: Record<string, string> = {
  Bronze: 'tier-bronze',
  Silver: 'tier-silver',
  Gold: 'tier-gold',
  Platinum: 'tier-platinum',
};

const MemberProfilePage: React.FC = () => {
  const [tab, setTab] = useState<'benefits' | 'history' | 'phone'>('benefits');
  const [profile, setProfile] = useState<MemberProfile | null>(null);
  const [tiers, setTiers] = useState<MemberTier[]>([]);
  const [packages, setPackages] = useState<BenefitPackage[]>([]);
  const [items, setItems] = useState<Record<number, BenefitItem[]>>({});
  const [history, setHistory] = useState<PointsLedgerEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [phoneInput, setPhoneInput] = useState('');
  const [phoneLoading, setPhoneLoading] = useState(false);
  const [redeemLoading, setRedeemLoading] = useState<number | null>(null);

  const loadProfile = useCallback(async () => {
    try {
      setLoading(true);
      const [prof, allTiers] = await Promise.all([getMyProfile(), getTiers()]);
      setProfile(prof);
      setTiers(allTiers);
      // Load benefit packages for current tier
      const pkgs = await getPackagesByTier(prof.tierId);
      setPackages(pkgs);
      const itemMap: Record<number, BenefitItem[]> = {};
      await Promise.all(pkgs.map(async pkg => {
        itemMap[pkg.id] = await getItemsByPackage(pkg.id);
      }));
      setItems(itemMap);
    } catch { setError('Failed to load profile'); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { loadProfile(); }, [loadProfile]);

  useEffect(() => {
    if (tab === 'history') {
      getSpendHistory().then(setHistory).catch(() => {});
    }
  }, [tab]);

  const currentTier = tiers.find(t => t.id === profile?.tierId);
  const sortedTiers = [...tiers].sort((a, b) => a.rank - b.rank);
  const nextTier = currentTier ? sortedTiers.find(t => t.rank > currentTier.rank) : null;

  const progressPercent = (() => {
    if (!profile || !currentTier) return 0;
    if (!nextTier) return 100;
    const range = nextTier.minSpend - currentTier.minSpend;
    if (range <= 0) return 100;
    return Math.min(100, Math.round(((profile.totalSpend - currentTier.minSpend) / range) * 100));
  })();

  const handleRedeem = async (itemId: number) => {
    setRedeemLoading(itemId);
    try {
      await redeemBenefit(itemId, `redeem-${Date.now()}`);
      setError('');
      alert('Benefit redeemed successfully!');
    } catch { setError('Failed to redeem benefit'); }
    finally { setRedeemLoading(null); }
  };

  const handleUpdatePhone = async () => {
    if (!phoneInput.trim()) return;
    setPhoneLoading(true);
    try {
      const updated = await updatePhone(phoneInput.trim());
      setProfile(updated);
      setPhoneInput('');
    } catch { setError('Failed to update phone'); }
    finally { setPhoneLoading(false); }
  };

  if (loading) return <div className="page-loading">Loading...</div>;
  if (!profile) return <div className="alert alert-danger">Could not load profile</div>;

  const tierClass = TIER_CLASSES[profile.tierName] || 'tier-bronze';

  return (
    <div className="page">
      <div className="page-header"><h1>My Profile</h1></div>
      {error && <div className="alert alert-danger">{error}</div>}

      {/* Tier Card */}
      <div className={`tier-card ${tierClass}`} style={{ marginBottom: '1.5rem' }}>
        <h2>{profile.tierName} Member</h2>
        <div className="tier-points">${profile.totalSpend} spend</div>
        <div className="tier-progress">
          <div className="tier-progress-bar">
            <div className="tier-progress-fill" style={{ width: `${progressPercent}%` }} />
          </div>
          <div className="tier-progress-label">
            {nextTier ? `${progressPercent}% toward ${nextTier.name} ($${nextTier.minSpend} spend)` : 'Maximum tier reached'}
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="tabs">
        <div className={`tab ${tab === 'benefits' ? 'active' : ''}`} onClick={() => setTab('benefits')}>Benefits</div>
        <div className={`tab ${tab === 'history' ? 'active' : ''}`} onClick={() => setTab('history')}>Spend History</div>
        <div className={`tab ${tab === 'phone' ? 'active' : ''}`} onClick={() => setTab('phone')}>Phone</div>
      </div>

      {/* Benefits */}
      {tab === 'benefits' && (
        <div>
          <p className="text-muted" style={{ marginBottom: '1rem', fontSize: '0.85rem' }}>
            Benefits are non-stackable. Only the highest applicable benefit is applied per transaction.
          </p>
          {packages.length === 0 && <p className="text-muted">No benefit packages available for your tier.</p>}
          {packages.map(pkg => (
            <div key={pkg.id} style={{ marginBottom: '1rem' }}>
              <h3>{pkg.name}</h3>
              <p className="text-muted" style={{ fontSize: '0.85rem', marginBottom: '0.5rem' }}>{pkg.description}</p>
              {(items[pkg.id] || []).map(item => (
                <div key={item.id} className="benefit-card">
                  <div className="benefit-info">
                    <h4>{item.benefitType}</h4>
                    <p>{item.benefitValue}</p>
                  </div>
                  <button className="btn btn-primary btn-sm" disabled={redeemLoading === item.id} onClick={() => handleRedeem(item.id)}>
                    {redeemLoading === item.id ? 'Redeeming...' : 'Redeem'}
                  </button>
                </div>
              ))}
            </div>
          ))}
        </div>
      )}

      {/* Points History */}
      {tab === 'history' && (
        <div className="card">
          <div className="table-wrapper">
            <table className="table">
              <thead>
                <tr><th>Date</th><th>Amount</th><th>Balance After</th><th>Type</th><th>Reference</th></tr>
              </thead>
              <tbody>
                {history.length === 0 && <tr><td colSpan={5} style={{ textAlign: 'center' }}>No history</td></tr>}
                {history.map(entry => (
                  <tr key={entry.id}>
                    <td>{new Date(entry.createdAt).toLocaleDateString()}</td>
                    <td style={{ color: entry.amount >= 0 ? 'var(--success)' : 'var(--danger)', fontWeight: 600 }}>
                      {entry.amount >= 0 ? '+' : ''}{entry.amount}
                    </td>
                    <td>{entry.spendAfter}</td>
                    <td>{entry.entryType}</td>
                    <td>{entry.reference}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Phone */}
      {tab === 'phone' && (
        <div className="card">
          <div className="card-body">
            <p style={{ marginBottom: '1rem' }}>
              <strong>Current phone:</strong> {profile.phoneMasked || 'Not set'}
            </p>
            <div className="form-group">
              <label className="form-label">New Phone Number</label>
              <input type="tel" className="form-input" value={phoneInput} onChange={e => setPhoneInput(e.target.value)} placeholder="+1234567890" style={{ maxWidth: 300 }} />
            </div>
            <button className="btn btn-primary" disabled={phoneLoading || !phoneInput.trim()} onClick={handleUpdatePhone}>
              {phoneLoading ? 'Updating...' : 'Update Phone'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default MemberProfilePage;
