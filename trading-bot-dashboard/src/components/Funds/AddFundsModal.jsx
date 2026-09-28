import { useState } from 'react';

export default function AddFundsModal({ open, balance, busy, onClose, onSubmit }) {
  const [amount, setAmount] = useState('');
  const [error, setError] = useState('');

  if (!open) return null;

  const submit = async (action) => {
    const value = Number(amount);
    if (!Number.isFinite(value) || value <= 0) {
      setError('Enter an amount greater than zero.');
      return;
    }
    if (action === 'withdraw' && value > Number(balance || 0)) {
      setError(`Available balance is ${new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(Number(balance || 0))}.`);
      return;
    }
    setError('');
    const succeeded = await onSubmit(action, value);
    if (succeeded) setAmount('');
  };

  return (
    <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
      <section className="fund-modal" role="dialog" aria-modal="true" aria-labelledby="fund-modal-title">
        <div className="panel-header"><div><span className="eyebrow">CASH MANAGEMENT</span><h2 id="fund-modal-title">Manage your funds</h2></div><button type="button" className="modal-close" aria-label="Close funds dialog" onClick={onClose}>×</button></div>
        <p className="muted">Available balance <strong>{new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(Number(balance || 0))}</strong></p>
        <label className="fund-label" htmlFor="fund-amount">Amount in USD</label>
        <input className="modal-amount" id="fund-amount" type="number" min="0.01" step="0.01" placeholder="e.g. 100.00" value={amount} onChange={(event) => setAmount(event.target.value)} />
        {error && <p className="form-error" role="alert">{error}</p>}
        <div className="fund-form modal-actions">
          <button type="button" disabled={busy} onClick={() => submit('deposit')}>{busy ? 'Processing…' : 'Deposit funds'}</button>
          <button type="button" className="secondary" disabled={busy} onClick={() => submit('withdraw')}>Withdraw funds</button>
        </div>
        <p className="fund-note">Withdrawals cannot exceed your available balance.</p>
      </section>
    </div>
  );
}