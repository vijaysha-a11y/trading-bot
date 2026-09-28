import { useMemo, useState } from 'react';
import PriceChart from '../components/Dashboard/PriceChart.jsx';
import PortfolioSummary from '../components/Dashboard/PortfolioSummary.jsx';
import SignalIndicator from '../components/Dashboard/SignalIndicator.jsx';
import { formatMoney } from '../services/format.js';

function Holdings({ holdings = [], loading, onOpenTrades }) {
  if (loading && !holdings.length) return <div className="empty-state">Loading holdings…</div>;
  if (!holdings.length) {
    return (
      <div className="empty-state">
        <span className="empty-icon" aria-hidden="true">◇</span>
        <strong>No open holdings yet</strong>
        <span>After your first trade, your assets and profit or loss will appear here.</span>
        <button type="button" className="text-button" onClick={onOpenTrades}>View trade history →</button>
      </div>
    );
  }

  return (
    <div className="table-wrap holdings-table">
      <table>
        <thead><tr><th>Asset</th><th>Quantity</th><th>Avg. buy</th><th>Market price</th><th>Unrealized P&amp;L</th></tr></thead>
        <tbody>{holdings.map((item) => (
          <tr key={item.symbol}>
            <td><span className="asset-name"><span className="coin-icon">{item.symbol?.slice(0, 1)}</span><strong>{item.symbol}</strong></span></td>
            <td>{Number(item.quantity || 0).toLocaleString('en-US', { maximumFractionDigits: 6 })}</td>
            <td>{formatMoney(item.avgBuyPrice)}</td>
            <td>{formatMoney(item.currentPrice)}</td>
            <td className={Number(item.profitLoss) >= 0 ? 'gain' : 'loss'}>{formatMoney(item.profitLoss)}</td>
          </tr>
        ))}</tbody>
      </table>
    </div>
  );
}

function SignalTimeline({ signals = [] }) {
  return (
    <section className="panel signal-timeline">
      <div className="panel-header"><div><span className="eyebrow">SIGNAL HISTORY</span><h2>Recent strategy decisions</h2></div></div>
      {!signals.length ? <div className="empty-state"><span>Signal evaluations will appear here as market data arrives.</span></div> : (
        <div className="signal-timeline-list">{signals.slice(0, 12).map((item, index) => (
          <div className="signal-timeline-row" key={`${item.strategyName}-${item.historyAt}-${index}`}>
            <span className={`signal-badge ${(item.action || 'hold').toLowerCase()}`}>{item.action}</span>
            <span><strong>{item.symbol} · {(item.strategyName || 'Strategy').replaceAll('_', ' ')}</strong><small>{item.reason || 'No explanation provided'}</small></span>
            <time>{item.generatedAt ? new Date(item.generatedAt).toLocaleString() : '—'}</time>
          </div>
        ))}</div>
      )}
    </section>
  );
}

function PortfolioPerformance({ portfolio, trades = [], transactions = [] }) {
  const analysis = useMemo(() => {
    const events = [
      ...trades.map((trade) => ({ kind: 'trade', time: new Date(trade.timestamp).getTime(), value: trade })),
      ...transactions.map((transaction) => ({ kind: 'fund', time: new Date(transaction.timestamp).getTime(), value: transaction })),
    ].filter((event) => Number.isFinite(event.time)).sort((a, b) => a.time - b.time);
    const cashFlows = events.reduce((sum, event) => {
      if (event.kind === 'trade') {
        const notional = Number(event.value.price) * Number(event.value.quantity);
        return sum + (event.value.type === 'BUY' ? notional : -notional);
      }
      const amount = Number(event.value.amount);
      return sum - (event.value.type === 'DEPOSIT' ? amount : -amount);
    }, 0);
    let cash = Number(portfolio?.availableBalance || 0) + cashFlows;
    const holdings = new Map();
    const prices = new Map();
    const points = [{ time: events[0]?.time || Date.now(), value: cash }];
    const lots = new Map();
    let wins = 0;
    let closes = 0;
    for (const event of events) {
      if (event.kind === 'fund') {
        const amount = Number(event.value.amount);
        cash += event.value.type === 'DEPOSIT' ? amount : -amount;
      } else {
        const trade = event.value;
        const quantity = Number(trade.quantity);
        const price = Number(trade.price);
        const previousQuantity = holdings.get(trade.symbol) || 0;
        cash += trade.type === 'BUY' ? -quantity * price : quantity * price;
        holdings.set(trade.symbol, previousQuantity + (trade.type === 'BUY' ? quantity : -quantity));
        prices.set(trade.symbol, price);
        const queue = lots.get(trade.symbol) || [];
        if (trade.type === 'BUY') queue.push({ quantity, price });
        else {
          let remaining = quantity;
          let realized = 0;
          while (remaining > 0 && queue.length) {
            const lot = queue[0];
            const matched = Math.min(remaining, lot.quantity);
            realized += (price - lot.price) * matched;
            remaining -= matched;
            lot.quantity -= matched;
            if (lot.quantity <= 0) queue.shift();
          }
          if (remaining === 0) {
            closes++;
            if (realized > 0) wins++;
          }
        }
        lots.set(trade.symbol, queue);
      }
      const value = cash + [...holdings].reduce((total, [asset, quantity]) => total + quantity * (prices.get(asset) || 0), 0);
      points.push({ time: event.time, value });
    }
    if (portfolio?.totalValue != null) points.push({ time: Date.now(), value: Number(portfolio.totalValue) });
    let peak = -Infinity;
    let maxDrawdown = 0;
    for (const point of points) {
      peak = Math.max(peak, point.value);
      if (peak > 0) maxDrawdown = Math.max(maxDrawdown, (peak - point.value) / peak * 100);
    }
    const returns = points.slice(1).map((point, index) => {
      const prior = points[index].value;
      return prior > 0 ? (point.value - prior) / prior : 0;
    });
    const mean = returns.length ? returns.reduce((sum, value) => sum + value, 0) / returns.length : 0;
    const deviation = returns.length > 1 ? Math.sqrt(returns.reduce((sum, value) => sum + (value - mean) ** 2, 0) / (returns.length - 1)) : 0;
    const sharpe = deviation ? mean / deviation * Math.sqrt(252) : 0;
    const values = points.map((point) => point.value);
    const low = Math.min(...values);
    const spread = Math.max(...values) - low || 1;
    const coords = points.map((point, index) => `${(index / Math.max(1, points.length - 1)) * 800},${205 - ((point.value - low) / spread) * 170}`);
    return { points, curve: coords.join(' '), winRate: closes ? wins / closes * 100 : 0, sharpe, maxDrawdown };
  }, [portfolio, trades, transactions]);

  return (
    <section className="panel performance-panel">
      <div className="panel-header"><div><span className="eyebrow">PERFORMANCE</span><h2>Estimated value by trade / fund event</h2></div></div>
      {analysis.points.length > 1 ? <div className="equity-curve"><svg viewBox="0 0 800 230" role="img" aria-label="Portfolio value history"><polyline points={analysis.curve} fill="none" stroke="#16a673" strokeWidth="3" vectorEffect="non-scaling-stroke" /></svg><div className="chart-labels"><span>Earlier activity</span><span>Portfolio value</span><span>Now</span></div></div> : <div className="chart-empty">Portfolio performance appears as trades and deposits are recorded.</div>}
      <div className="performance-metrics"><span>Closed-trade win rate <strong>{analysis.winRate.toFixed(1)}%</strong></span><span>Event-return Sharpe <strong>{analysis.sharpe.toFixed(2)}</strong></span><span>Max drawdown <strong>{analysis.maxDrawdown.toFixed(2)}%</strong></span></div>
    </section>
  );
}

export default function DashboardPage({
  portfolio, trades, transactions, signalHistory, loading, symbol, symbols, latestPrice, candles, signals, streamConnected,
  quantity, onQuantityChange, busy, canBuy, canSell, onTrade, onSymbolChange,
  onOpenTrades, onOpenFunds,
}) {
  const [decisionMode, setDecisionMode] = useState('signal');
  const [rsiBuyLevel, setRsiBuyLevel] = useState('30');
  const [rsiSellLevel, setRsiSellLevel] = useState('70');
  const activeSignal = useMemo(() => signals.find((item) => item.action !== 'HOLD') || signals[0], [signals]);
  const rsiValue = Number(activeSignal?.indicators?.RSI ?? signals[0]?.indicators?.RSI);
  const buyLevel = Number(rsiBuyLevel) || 30;
  const sellLevel = Number(rsiSellLevel) || 70;
  const paperSetup = decisionMode === 'rsi' && Number.isFinite(rsiValue)
    ? {
      ...(signals.find((item) => item.strategyName === 'RSI_MEAN_REVERSION') || activeSignal || {}),
      action: rsiValue < buyLevel ? 'BUY' : rsiValue > sellLevel ? 'SELL' : 'HOLD',
      strategyName: 'CUSTOM_RSI',
      reason: `RSI ${rsiValue.toFixed(1)} · ${rsiValue < buyLevel ? `below your ${buyLevel} buy level` : rsiValue > sellLevel ? `above your ${sellLevel} sell level` : `neutral between ${buyLevel} and ${sellLevel}`}.`,
    }
    : activeSignal;
  return (
    <>
      <PortfolioSummary portfolio={portfolio} loading={loading} />
      <PortfolioPerformance portfolio={portfolio} trades={trades} transactions={transactions} />
      <div className="overview-actions"><span>Live account overview</span><button type="button" className="text-button" onClick={onOpenFunds}>＋ Manage funds</button></div>
      <section className="dashboard-grid">
        <PriceChart symbol={symbol} symbols={symbols} latestPrice={latestPrice} candles={candles} onSymbolChange={onSymbolChange} streamConnected={streamConnected} signal={paperSetup} decisionMode={decisionMode} onDecisionModeChange={setDecisionMode} rsiBuyLevel={rsiBuyLevel} onRsiBuyLevelChange={setRsiBuyLevel} rsiSellLevel={rsiSellLevel} onRsiSellLevelChange={setRsiSellLevel} />
        <SignalIndicator signal={paperSetup} signals={signals} symbol={symbol} quantity={quantity} onQuantityChange={onQuantityChange} onTrade={onTrade} busy={busy} canBuy={canBuy} canSell={canSell} balance={Number(portfolio?.availableBalance || 0)} positionQuantity={Number(portfolio?.holdings?.find((holding) => holding.symbol === symbol)?.quantity || 0)} />
      </section>
      <section className="panel holdings-panel">
        <div className="panel-header"><div><span className="eyebrow">YOUR POSITIONS</span><h2>Holdings</h2></div><button type="button" className="text-button" onClick={onOpenTrades}>View trade history →</button></div>
        <Holdings holdings={portfolio?.holdings} loading={loading} onOpenTrades={onOpenTrades} />
      </section>
      <SignalTimeline signals={signalHistory} />
    </>
  );
}