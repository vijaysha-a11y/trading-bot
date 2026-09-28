import TransactionHistory from '../components/Funds/TransactionHistory.jsx';
import TradeHistoryTable from '../components/Trades/TradeHistoryTable.jsx';
import { useMemo, useState } from 'react';

export default function TradeHistoryPage({ trades = [], orders = [], transactions, transactionsOnly = false, onCancelOrder }) {
  const [symbol, setSymbol] = useState('ALL');
  const [side, setSide] = useState('ALL');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const symbols = useMemo(() => [...new Set(trades.map((trade) => trade.symbol).filter(Boolean))].sort(), [trades]);
  const filteredTrades = useMemo(() => trades.filter((trade) => {
    const date = new Date(trade.timestamp);
    return (symbol === 'ALL' || trade.symbol === symbol)
      && (side === 'ALL' || trade.type === side)
      && (!from || date >= new Date(`${from}T00:00:00`))
      && (!to || date <= new Date(`${to}T23:59:59`));
  }), [trades, symbol, side, from, to]);

  return (
    <div className="history-grid">
      {!transactionsOnly && (
        <section className="panel page-panel">
          <div className="panel-header"><div><span className="eyebrow">EXECUTION LOG</span><h2>Recent trades</h2></div></div>
          <div className="history-filters">
            <label>From<input type="date" value={from} max={to || undefined} onChange={(event) => setFrom(event.target.value)} /></label>
            <label>To<input type="date" value={to} min={from || undefined} onChange={(event) => setTo(event.target.value)} /></label>
            <label>Market<select value={symbol} onChange={(event) => setSymbol(event.target.value)}><option value="ALL">All markets</option>{symbols.map((item) => <option key={item}>{item}</option>)}</select></label>
            <label>Side<select value={side} onChange={(event) => setSide(event.target.value)}><option value="ALL">Buy &amp; sell</option><option value="BUY">Buy</option><option value="SELL">Sell</option></select></label>
            <button type="button" className="text-button" onClick={() => { setSymbol('ALL'); setSide('ALL'); setFrom(''); setTo(''); }}>Clear filters</button>
          </div>
          <TradeHistoryTable trades={filteredTrades} />
          {orders.some((order) => order.status === 'PENDING') && (
            <div className="paper-orders">
              <div className="panel-header"><div><span className="eyebrow">SIMULATED ORDERS</span><h2>Pending limit orders</h2></div></div>
              {orders.filter((order) => order.status === 'PENDING').map((order) => (
                <div className="paper-order-row" key={order.id}>
                  <span><strong>{order.action} {order.symbol}</strong><small>{order.quantity} @ {order.limitPrice} · {order.status}</small></span>
                  <button type="button" className="text-button" onClick={() => onCancelOrder(order.id)}>Cancel</button>
                </div>
              ))}
            </div>
          )}
          {orders.some((order) => order.status === 'PROTECTIVE') && (
            <div className="paper-orders">
              <div className="panel-header"><div><span className="eyebrow">AUTOMATIC PAPER EXITS</span><h2>Active stop-loss / take-profit</h2></div></div>
              {orders.filter((order) => order.status === 'PROTECTIVE').map((order) => (
                <div className="paper-order-row" key={order.id}>
                  <span><strong>{order.symbol} · {order.quantity}</strong><small>{order.stopLoss ? `Stop ${order.stopLoss}` : 'No stop'} · {order.takeProfit ? `Target ${order.takeProfit}` : 'No target'}</small></span>
                  <span className="signal-badge hold">MONITORING</span>
                </div>
              ))}
            </div>
          )}
        </section>
      )}
      <section className="panel page-panel">
        <div className="panel-header"><div><span className="eyebrow">CASH ACTIVITY</span><h2>Fund transactions</h2></div></div>
        <TransactionHistory transactions={transactions} />
      </section>
    </div>
  );
}