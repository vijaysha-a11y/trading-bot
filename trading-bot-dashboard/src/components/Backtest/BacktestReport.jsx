const money = (value) => new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(Number(value || 0));

export default function BacktestReport({ report }) {
  if (!report) return <div className="empty-state"><strong>No backtest results yet</strong><span>Choose a market and date range to run a historical simulation.</span></div>;

  const metrics = [
    ['Starting balance', money(report.startingBalance)],
    ['Ending balance', money(report.endingBalance)],
    ['Net profit / loss', money(report.netProfitLoss)],
    ['Return', `${Number(report.netProfitLossPercent || 0).toFixed(2)}%`],
    ['Total trades', report.totalTrades ?? 0],
    ['Win rate', `${Number(report.winRatePercent || 0).toFixed(1)}%`],
    ['Max drawdown', Number.isFinite(Number(report.maxDrawdownPercent)) ? `${Number(report.maxDrawdownPercent).toFixed(2)}%` : money(report.maxDrawdown)],
    ['Sharpe ratio', Number.isFinite(Number(report.sharpeRatio)) ? Number(report.sharpeRatio).toFixed(2) : '—'],
  ];
  const values = (report.equityCurve || []).map((point) => Number(point.value)).filter(Number.isFinite);
  const min = Math.min(...values);
  const spread = Math.max(...values) - min || 1;
  const coordinates = values.map((value, index) => `${(index / Math.max(1, values.length - 1)) * 800},${205 - ((value - min) / spread) * 170}`);
  const curve = coordinates.join(' ');

  return (
    <section className="backtest-report">
      <div className="report-heading"><span className="eyebrow">SIMULATION RESULTS</span><h2>{report.symbol} · {report.strategyName}</h2><p className="muted">{report.startDate} to {report.endDate}</p></div>
      <div className="report-grid">{metrics.map(([label, value]) => <div className="report-metric" key={label}><span>{label}</span><strong>{value}</strong></div>)}</div>
      {coordinates.length > 1 && <div className="equity-curve"><h3>Simulated equity curve</h3><svg viewBox="0 0 800 230" role="img" aria-label={`${report.symbol} backtest equity curve`}><polyline points={curve} fill="none" stroke="#16a673" strokeWidth="3" vectorEffect="non-scaling-stroke" /></svg><div className="chart-labels"><span>{report.startDate}</span><span>Virtual balance · {money(report.startingBalance)}</span><span>{report.endDate}</span></div></div>}
    </section>
  );
}