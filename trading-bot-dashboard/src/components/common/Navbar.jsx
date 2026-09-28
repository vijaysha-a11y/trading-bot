const items = [
  ['overview', '⌂', 'Overview'],
  ['trades', '↔', 'Trade history'],
  ['backtest', '◷', 'Backtest'],
  ['funds', '＋', 'Funds'],
  ['users', '◎', 'Users'],
];

export default function Navbar({ activePage, onNavigate, connected, streamConnected, lastUpdated, portfolioId, user }) {
  const visibleItems = items.filter(([page]) => page !== 'users' || user?.role === 'ADMIN');
  return (
    <aside className="sidebar">
      <a className="brand" href="#overview" onClick={(event) => { event.preventDefault(); onNavigate('overview'); }}>
        <span className="brand-mark">↗</span><span>quant<span className="accent">flow</span></span>
      </a>
      <div className="profile">
        <div className="avatar">{user?.name?.slice(0, 1).toUpperCase() || '?'}</div>
        <div><strong>{user?.name}&apos;s portfolio</strong><span>{user?.role} · Portfolio #{portfolioId}</span></div>
        <span className={`online-dot ${connected ? '' : 'offline'}`} />
      </div>
      <nav aria-label="Main navigation">
        {visibleItems.map(([page, icon, label]) => (
          <button key={page} type="button" className={activePage === page ? 'nav-item active' : 'nav-item'} onClick={() => onNavigate(page)}>
            <span aria-hidden="true">{icon}</span><span>{label}</span>
          </button>
        ))}
      </nav>
      <div className="sidebar-footer">
        <span className={`status-dot ${connected ? '' : 'offline'}`} />
        {!connected ? 'Backend disconnected' : streamConnected ? 'Backend & market stream connected' : 'Backend connected · market stream reconnecting'}
        <div className="api-url">{lastUpdated ? `Updated ${new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: '2-digit', second: '2-digit', timeZoneName: 'short' }).format(lastUpdated)}` : 'Connecting…'}</div>
      </div>
    </aside>
  );
}