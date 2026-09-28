import { useState } from 'react';

const money = (value) => {
  const amount = Number(value);
  return Number.isFinite(amount)
    ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount)
    : '—';
};

export default function SignalIndicator({ signal, signals = [], symbol, quantity, onQuantityChange, onTrade, busy, canBuy, canSell, balance, positionQuantity }) {
  const action = signal?.action || 'HOLD';
  const [strategy, setStrategy] = useState('');
  const [orderType, setOrderType] = useState('MARKET');
  const [limitPrice, setLimitPrice] = useState('');
  const [stopLoss, setStopLoss] = useState('');
  const [takeProfit, setTakeProfit] = useState('');
  const usableSignals = signals.filter((item) => item.strategyName);
  const selectedSignal = usableSignals.find((item) => item.strategyName === strategy);
  const activeSignal = strategy ? selectedSignal : signal;
  const currentPrice = Number(activeSignal?.price || 0);
  const onAction = (nextAction) => {
    onTrade(nextAction, {
      orderType,
      limitPrice,
      stopLoss: nextAction === 'BUY' ? stopLoss : '',
      takeProfit: nextAction === 'BUY' ? takeProfit : '',
      strategyName: activeSignal?.strategyName || 'MANUAL',
      reason: activeSignal?.reason || 'Manual paper order from dashboard.',
    });
  };

  return (
    <article className="panel signal-panel">
      <div className="panel-header"><div><span className="eyebrow">PAPER-TRADING SIGNAL</span><h2>Market signal</h2></div><span className={`signal-badge ${(activeSignal?.action || action).toLowerCase()}`}>{activeSignal?.action || action}</span></div>
      <div className="signal-score">
        <div className={`score-ring ${activeSignal ? '' : 'score-ring-empty'}`}><strong>{Math.round(Number(activeSignal?.confidence || 0) * 100)}<small>%</small></strong></div>
        <div><span className="muted">Confidence · {activeSignal?.strategyName || 'Strategy'}</span><p>{activeSignal?.reason || 'Waiting for the next strategy signal.'}</p></div>
      </div>
      <div className="signal-meta"><span>Compare strategy</span>
        <select aria-label="Compare strategy" value={strategy} onChange={(event) => setStrategy(event.target.value)}>
          <option value="">Best current signal</option>
          {[...new Set(usableSignals.map((item) => item.strategyName))].map((name) => <option key={name} value={name}>{name.replaceAll('_', ' ')}</option>)}
        </select>
      </div>
      <div className="signal-meta"><span>Indicators</span><strong>{activeSignal?.indicators ? Object.entries(activeSignal.indicators).map(([name, value]) => `${name} ${Number(value).toFixed(2)}`).join(' · ') : 'Waiting for indicator data'}</strong></div>
      <div className="signal-meta"><span>Signal price</span><strong>{activeSignal ? money(activeSignal.price) : '—'}</strong></div>
      <div className="trade-controls">
        <label htmlFor="order-quantity">Order quantity</label>
        <div className="quantity-input"><input id="order-quantity" type="number" min="0.000001" step="any" value={quantity} onChange={(event) => onQuantityChange(event.target.value)} /><span>{symbol.replace('USDT', '')}</span></div>
      </div>
      <div className="quick-size-actions">
        <button type="button" onClick={() => onQuantityChange(currentPrice > 0 ? String((balance * .01 / currentPrice).toFixed(6)) : '0')}>1% balance</button>
        <button type="button" onClick={() => onQuantityChange(currentPrice > 0 ? String((balance / currentPrice).toFixed(6)) : '0')}>Max qty</button>
        <button type="button" onClick={() => onQuantityChange(String(positionQuantity || 0))}>Max sell</button>
      </div>
      <div className="trade-controls">
        <label htmlFor="order-type">Order type</label>
        <select id="order-type" value={orderType} onChange={(event) => setOrderType(event.target.value)}><option value="MARKET">Market</option><option value="LIMIT">Limit</option></select>
      </div>
      {orderType === 'LIMIT' && <div className="trade-controls"><label htmlFor="limit-price">Limit price</label><input id="limit-price" type="number" min="0.01" step="any" value={limitPrice} onChange={(event) => setLimitPrice(event.target.value)} placeholder={money(currentPrice)} /></div>}
      <details className="protective-order">
        <summary>Stop-loss / take-profit (paper order)</summary>
        <div className="trade-controls"><label htmlFor="stop-loss">Stop-loss</label><input id="stop-loss" type="number" min="0.01" step="any" value={stopLoss} onChange={(event) => setStopLoss(event.target.value)} placeholder="Optional" /></div>
        <div className="trade-controls"><label htmlFor="take-profit">Take-profit</label><input id="take-profit" type="number" min="0.01" step="any" value={takeProfit} onChange={(event) => setTakeProfit(event.target.value)} placeholder="Optional" /></div>
      </details>
      <div className="trade-actions">
        <button type="button" disabled={busy || (!canBuy && orderType === 'MARKET')} title={!canBuy ? 'Check your balance and current price.' : ''} onClick={() => onAction('BUY')}>{busy ? 'Working…' : activeSignal?.action === 'BUY' ? 'Place BUY paper order' : 'Buy paper'}</button>
        <button type="button" className="secondary" disabled={busy || (!canSell && orderType === 'MARKET')} title={!canSell ? 'You need enough holdings to sell.' : ''} onClick={() => onAction('SELL')}>{busy ? 'Working…' : activeSignal?.action === 'SELL' ? 'Place SELL paper order' : 'Sell paper'}</button>
      </div>
      <p className="trade-hint">{activeSignal?.action === 'HOLD' ? 'No trade setup right now. You can still place a manual paper order; it will not execute automatically.' : 'Review the setup, then explicitly place a simulated paper order. No order runs automatically.'}</p>
    </article>
  );
}
