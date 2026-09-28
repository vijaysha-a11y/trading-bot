import { useCallback, useEffect, useRef, useState } from 'react';
import { getPaperOrders, getPortfolioSummary, getSignalHistory, getSignals, getTrades, getTransactions } from '../services/api.js';

const REFRESH_INTERVAL = 30_000;

export default function useLivePortfolio(portfolioId, symbol) {
  const [portfolio, setPortfolio] = useState(null);
  const [signals, setSignals] = useState([]);
  const [signalHistory, setSignalHistory] = useState([]);
  const [trades, setTrades] = useState([]);
  const [orders, setOrders] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [errors, setErrors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [lastUpdated, setLastUpdated] = useState(null);
  const inFlight = useRef(false);

  const refresh = useCallback(async () => {
    if (inFlight.current) return;
    inFlight.current = true;
    setRefreshing(true);
    const results = await Promise.allSettled([
      getPortfolioSummary(portfolioId),
      getSignals(symbol),
      getTrades(portfolioId),
      getTransactions(portfolioId),
      getPaperOrders(portfolioId),
      getSignalHistory(symbol),
    ]);
    const labels = ['portfolio summary', 'strategy signals', 'trade history', 'fund transactions', 'paper orders', 'signal history'];
    const nextErrors = [];

    results.forEach((result, index) => {
      if (result.status === 'rejected') {
        nextErrors.push(`${labels[index]}: ${result.reason?.message || 'request failed'}`);
        return;
      }

      const value = result.value;
      if (index === 0 && value && typeof value === 'object') setPortfolio(value);
      else if (index === 1 && Array.isArray(value)) setSignals(value);
      else if (index === 2 && Array.isArray(value)) setTrades(value);
      else if (index === 3 && Array.isArray(value)) setTransactions(value);
      else if (index === 4 && Array.isArray(value)) setOrders(value);
      else if (index === 5 && Array.isArray(value)) setSignalHistory(value);
      else nextErrors.push(`${labels[index]}: invalid response from the backend`);
    });

    setErrors(nextErrors);
    setLastUpdated(new Date());
    setLoading(false);
    setRefreshing(false);
    inFlight.current = false;
  }, [portfolioId, symbol]);

  useEffect(() => {
    refresh();
    const timer = window.setInterval(refresh, REFRESH_INTERVAL);
    return () => window.clearInterval(timer);
  }, [refresh]);

  return { portfolio, signals, signalHistory, trades, orders, transactions, errors, loading, refreshing, lastUpdated, refresh };
}