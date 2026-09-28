import { useEffect, useMemo, useState } from 'react';
import AddFundsModal from './components/Funds/AddFundsModal.jsx';
import TransactionHistory from './components/Funds/TransactionHistory.jsx';
import AuthPage from './components/AuthPage.jsx';
import Loader from './components/common/Loader.jsx';
import Navbar from './components/common/Navbar.jsx';
import AdminUsersPage from './pages/AdminUsersPage.jsx';
import BacktestPage from './pages/BacktestPage.jsx';
import DashboardPage from './pages/DashboardPage.jsx';
import TradeHistoryPage from './pages/TradeHistoryPage.jsx';
import useLivePortfolio from './hooks/useLivePortfolio.js';
import useLivePrice from './hooks/useLivePrice.js';
import { cancelPaperOrder, executeTrade, getCurrentUser, logout, updateFunds } from './services/api.js';
import { formatMoney } from './services/format.js';
import './styles.css';

const SYMBOLS = ['BTCUSDT', 'ETHUSDT', 'SOLUSDT'];

export default function App() {
  const [user, setUser] = useState(null);
  const [checkingSession, setCheckingSession] = useState(true);

  useEffect(() => {
    let active = true;
    getCurrentUser()
      .then((account) => { if (active) setUser(account); })
      .catch(() => { if (active) setUser(null); })
      .finally(() => { if (active) setCheckingSession(false); });
    return () => { active = false; };
  }, []);

  if (checkingSession) return <Loader label="Securing your workspace…" />;
  if (!user) return <AuthPage onAuthenticated={setUser} />;
  return <DashboardApp user={user} onLogout={() => setUser(null)} />;
}

function DashboardApp({ user, onLogout }) {
  const [clock, setClock] = useState(() => new Date());
  const [activePage, setActivePage] = useState('overview');
  const [symbol, setSymbol] = useState(SYMBOLS[0]);
  const [quantity, setQuantity] = useState('0.001');
  const [busy, setBusy] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const [fundModalOpen, setFundModalOpen] = useState(false);
  const [actionMessage, setActionMessage] = useState(null);
  const [logoutError, setLogoutError] = useState('');
  const portfolioData = useLivePortfolio(user.portfolioId, symbol);
  const priceData = useLivePrice(symbol);

  useEffect(() => {
    const timer = window.setInterval(() => setClock(new Date()), 1000);
    return () => window.clearInterval(timer);
  }, []);

  const errors = [...portfolioData.errors, ...priceData.errors];
  const currentPrice = Number(priceData.latestPrice?.close || 0);
  const amount = Number(quantity);
  const availableBalance = Number(portfolioData.portfolio?.availableBalance || 0);
  const position = portfolioData.portfolio?.holdings?.find((holding) => holding.symbol === symbol);
  const canBuy = Boolean(portfolioData.portfolio && currentPrice > 0 && amount > 0 && availableBalance >= currentPrice * amount);
  const canSell = Boolean(position && Number(position.quantity) >= amount && amount > 0 && currentPrice > 0);
  const lastUpdated = portfolioData.lastUpdated;
  const loading = portfolioData.loading || (!priceData.latestPrice && priceData.refreshing);

  const header = useMemo(() => ({
    overview: `${clock.getHours() < 12 ? 'Good morning' : clock.getHours() < 18 ? 'Good afternoon' : 'Good evening'}, ${user.name.split(' ')[0]}`,
    trades: 'Trade history',
    backtest: 'Strategy backtest',
    funds: 'Manage funds',
    users: 'Workspace access',
  }[activePage] || 'Your workspace'), [activePage, user.name, clock]);

  const refresh = async () => {
    await Promise.all([portfolioData.refresh(), priceData.refresh()]);
  };

  const submitTrade = async (action, options = {}) => {
    if (!currentPrice || !Number.isFinite(amount) || amount <= 0) {
      setActionMessage({ type: 'error', text: 'Enter a valid quantity and wait for a current market price.' });
      return;
    }
    if (action === 'BUY' && options.orderType !== 'LIMIT' && !canBuy) {
      setActionMessage({ type: 'error', text: 'There is not enough available balance for this order. Add funds or reduce the quantity.' });
      return;
    }
    if (action === 'SELL' && options.orderType !== 'LIMIT' && !canSell) {
      setActionMessage({ type: 'error', text: 'You do not have enough of this asset to sell that quantity.' });
      return;
    }

    const executionPrice = options.orderType === 'LIMIT' ? Number(options.limitPrice) : currentPrice;
    if (options.orderType === 'LIMIT' && (!Number.isFinite(executionPrice) || executionPrice <= 0)) {
      setActionMessage({ type: 'error', text: 'Enter a valid limit price.' });
      return;
    }
    const stopLoss = options.stopLoss ? Number(options.stopLoss) : null;
    const takeProfit = options.takeProfit ? Number(options.takeProfit) : null;
    if (action === 'BUY' && ((stopLoss !== null && (!Number.isFinite(stopLoss) || stopLoss >= executionPrice || stopLoss <= 0))
      || (takeProfit !== null && (!Number.isFinite(takeProfit) || takeProfit <= executionPrice)))) {
      setActionMessage({ type: 'error', text: 'For a buy, stop-loss must be below and take-profit above the expected fill price.' });
      return;
    }
    const notional = formatMoney(executionPrice * amount);
    if (!window.confirm(`Confirm simulated ${options.orderType === 'LIMIT' ? 'limit ' : ''}${action.toLowerCase()} ${amount} ${symbol} at about ${formatMoney(executionPrice)} each (${notional} total)?`)) return;

    setBusy(true);
    setActionMessage(null);
    try {
      await executeTrade({
        symbol,
        action,
        strategyName: options.strategyName || 'MANUAL',
        orderType: options.orderType || 'MARKET',
        price: currentPrice,
        limitPrice: options.orderType === 'LIMIT' ? executionPrice : null,
        stopLoss,
        takeProfit,
        quantity: amount,
        confidence: 1,
        reason: options.reason || 'Manual order from dashboard',
        generatedAt: new Date().toISOString(),
      });
      setActionMessage({ type: 'success', text: `${action} paper order submitted. Limit orders wait for their trigger price; protective exits monitor live paper-market prices.` });
      await refresh();
    } catch (error) {
      setActionMessage({ type: 'error', text: error.message });
    } finally {
      setBusy(false);
    }
  };

  const cancelOrder = async (orderId) => {
    setBusy(true);
    setActionMessage(null);
    try {
      await cancelPaperOrder(orderId);
      setActionMessage({ type: 'success', text: 'Pending paper order cancelled.' });
      await refresh();
    } catch (error) {
      setActionMessage({ type: 'error', text: error.message });
    } finally {
      setBusy(false);
    }
  };

  const submitFunds = async (action, value) => {
    setBusy(true);
    setActionMessage(null);
    try {
      await updateFunds(user.portfolioId, action, value);
      setActionMessage({ type: 'success', text: `Funds ${action === 'deposit' ? 'added' : 'withdrawn'} successfully.` });
      setFundModalOpen(false);
      await refresh();
      return true;
    } catch (error) {
      setActionMessage({ type: 'error', text: error.message });
      return false;
    } finally {
      setBusy(false);
    }
  };

  const signOut = async () => {
    setLoggingOut(true);
    setLogoutError('');
    try {
      await logout();
      onLogout();
    } catch (error) {
      setLogoutError(error.message);
      setLoggingOut(false);
    }
  };

  const localDateTime = new Intl.DateTimeFormat(undefined, {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
    second: '2-digit',
    timeZoneName: 'short',
  }).format(clock).toUpperCase();
  const dateLabel = localDateTime;

  return (
    <div className="app-shell">
      <Navbar activePage={activePage} onNavigate={setActivePage} connected={!errors.length} streamConnected={priceData.streamConnected} lastUpdated={lastUpdated} portfolioId={user.portfolioId} user={user} />
      <main className="content">
        <header className="topbar">
          <div><span className="eyebrow">{dateLabel}</span><h1>{header}</h1></div>
          <div className="topbar-actions">
            <div className="account-chip"><span className="account-chip-avatar">{user.name.slice(0, 1).toUpperCase()}</span><span><strong>{user.name}</strong><small>{user.role}</small></span></div>
            <button type="button" className="signout-button" onClick={signOut} disabled={loggingOut}>{loggingOut ? 'Signing out…' : 'Sign out'}</button>
            {activePage !== 'users' && <button type="button" className="refresh" onClick={refresh} disabled={portfolioData.refreshing || priceData.refreshing}>
              <span aria-hidden="true">↻</span> {portfolioData.refreshing || priceData.refreshing ? 'Refreshing…' : 'Refresh'}
            </button>}
          </div>
        </header>

        {logoutError && <div className="alert alert-error" role="alert">{logoutError}</div>}
        {errors.length > 0 && (
          <div className="alert alert-error" role="alert">
            <strong>Some live data could not be loaded.</strong>
            <span>{errors.join(' · ')}</span>
            {!portfolioData.portfolio && <span>Check that the backend is running and portfolio #{user.portfolioId} is available.</span>}
          </div>
        )}
        {actionMessage && (
          <div className={`alert ${actionMessage.type === 'error' ? 'alert-error' : 'alert-success'}`} role="status">
            <span>{actionMessage.text}</span>
            <button type="button" className="alert-dismiss" aria-label="Dismiss message" onClick={() => setActionMessage(null)}>×</button>
          </div>
        )}

        {activePage === 'overview' && (
          <DashboardPage
            portfolio={portfolioData.portfolio}
            trades={portfolioData.trades}
            transactions={portfolioData.transactions}
            signalHistory={portfolioData.signalHistory}
            loading={loading}
            symbol={symbol}
            symbols={SYMBOLS}
            latestPrice={priceData.latestPrice}
            candles={priceData.candles}
            signals={portfolioData.signals}
            streamConnected={priceData.streamConnected}
            quantity={quantity}
            onQuantityChange={setQuantity}
            busy={busy}
            canBuy={canBuy}
            canSell={canSell}
            onTrade={submitTrade}
            onSymbolChange={setSymbol}
            onOpenTrades={() => setActivePage('trades')}
            onOpenFunds={() => setFundModalOpen(true)}
          />
        )}

        {activePage === 'trades' && <TradeHistoryPage trades={portfolioData.trades} orders={portfolioData.orders} transactions={portfolioData.transactions} onCancelOrder={cancelOrder} />}
        {activePage === 'backtest' && <BacktestPage />}
        {activePage === 'users' && user.role === 'ADMIN' && <AdminUsersPage />}
        {activePage === 'funds' && (
          <section className="panel funds-panel">
            <span className="eyebrow">CASH MANAGEMENT</span>
            <h2>Manage your funds</h2>
            <p className="muted">Available balance <strong>{portfolioData.portfolio ? formatMoney(portfolioData.portfolio.availableBalance) : '—'}</strong></p>
            <button type="button" className="fund-open-button" disabled={!portfolioData.portfolio} onClick={() => setFundModalOpen(true)}>Deposit or withdraw funds</button>
            <p className="fund-note">Transactions update your available balance immediately.</p>
            <div className="panel-header transactions-heading"><div><span className="eyebrow">ACCOUNT ACTIVITY</span><h2>Fund transactions</h2></div></div>
            <TransactionHistory transactions={portfolioData.transactions} />
          </section>
        )}

        {portfolioData.loading && activePage !== 'overview' && activePage !== 'users' && <Loader compact label="Loading your account data…" />}
        <AddFundsModal open={fundModalOpen} balance={portfolioData.portfolio?.availableBalance} busy={busy} onClose={() => setFundModalOpen(false)} onSubmit={submitFunds} />
      </main>
    </div>
  );
}
