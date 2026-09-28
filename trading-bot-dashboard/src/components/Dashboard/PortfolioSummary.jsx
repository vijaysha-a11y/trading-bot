const money = (value) => {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return '—';
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
};

export default function PortfolioSummary({ portfolio, loading }) {
  const holdings = portfolio?.holdings || [];
  const profit = Number(portfolio?.totalProfitLoss || 0);
  const percent = Number(portfolio?.profitLossPercent || 0);
  const cards = [
    ['Total portfolio value', loading && !portfolio ? 'Loading…' : money(portfolio?.totalValue), portfolio ? `${percent >= 0 ? '+' : ''}${percent.toFixed(2)}% all time` : 'Awaiting portfolio data', portfolio ? (percent >= 0 ? 'gain' : 'loss') : 'muted'],
    ['Available balance', loading && !portfolio ? 'Loading…' : money(portfolio?.availableBalance), 'Ready to invest', 'muted'],
    ['Invested amount', loading && !portfolio ? 'Loading…' : money(portfolio?.investedAmount), `${holdings.length} active ${holdings.length === 1 ? 'position' : 'positions'}`, 'muted'],
    ['Total profit / loss', loading && !portfolio ? 'Loading…' : money(portfolio?.totalProfitLoss), 'Since inception', portfolio ? (profit >= 0 ? 'gain' : 'loss') : 'muted'],
  ];

  return (
    <section className="stats-grid" aria-label="Portfolio summary">
      {cards.map(([label, value, detail, tone]) => (
        <article className="stat-card" key={label}>
          <span className="eyebrow">{label}</span><strong>{value}</strong><span className={tone}>{detail}</span>
        </article>
      ))}
    </section>
  );
}