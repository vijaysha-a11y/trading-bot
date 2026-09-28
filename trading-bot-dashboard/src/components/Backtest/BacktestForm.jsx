import { useState } from 'react';
import { runBacktest } from '../../services/api.js';

const today = new Date().toISOString().slice(0, 10);
const monthAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);

export default function BacktestForm({ onComplete }) {
  const [symbol, setSymbol] = useState('BTCUSDT');
  const [strategyName, setStrategyName] = useState('RSI_MEAN_REVERSION');
  const [startingBalance, setStartingBalance] = useState('100000');
  const [startDate, setStartDate] = useState(monthAgo);
  const [endDate, setEndDate] = useState(today);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    setLoading(true);
    try {
      onComplete(await runBacktest({ symbol, strategyName, startDate, endDate, startingBalance }));
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form className="backtest-form" onSubmit={submit}>
      <label>Market<select value={symbol} onChange={(event) => setSymbol(event.target.value)}><option>BTCUSDT</option><option>ETHUSDT</option><option>SOLUSDT</option></select></label>
      <label>Strategy<select value={strategyName} onChange={(event) => setStrategyName(event.target.value)}><option>RSI_MEAN_REVERSION</option><option>SMA_EMA_CROSSOVER</option></select></label>
      <label>Starting virtual balance<input type="number" min="1" step="any" value={startingBalance} onChange={(event) => setStartingBalance(event.target.value)} required /></label>
      <label>Start date<input type="date" value={startDate} max={endDate} onChange={(event) => setStartDate(event.target.value)} required /></label>
      <label>End date<input type="date" value={endDate} min={startDate} max={today} onChange={(event) => setEndDate(event.target.value)} required /></label>
      <button type="submit" disabled={loading}>{loading ? 'Running backtest…' : 'Run backtest'}</button>
      {error && <p className="form-error" role="alert">{error}</p>}
    </form>
  );
}