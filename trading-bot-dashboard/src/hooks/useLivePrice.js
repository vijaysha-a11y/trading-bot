import { useCallback, useEffect, useRef, useState } from 'react';
import { getLatestPrice, getRecentPrices } from '../services/api.js';
import { connectPriceStream } from '../services/websocket.js';

const REFRESH_INTERVAL = 30_000;

export default function useLivePrice(symbol) {
  const [latestPrice, setLatestPrice] = useState(null);
  const [candles, setCandles] = useState([]);
  const [errors, setErrors] = useState([]);
  const [streamConnected, setStreamConnected] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const inFlight = useRef(false);

  const refresh = useCallback(async () => {
    if (inFlight.current) return;
    inFlight.current = true;
    setRefreshing(true);
    const results = await Promise.allSettled([
      getLatestPrice(symbol),
      getRecentPrices(symbol, 50),
    ]);
    const nextErrors = [];

    if (results[0].status === 'fulfilled' && results[0].value) {
      setLatestPrice(results[0].value);
    } else {
      nextErrors.push(`latest market price: ${results[0].reason?.message || 'no price returned'}`);
    }

    if (results[1].status === 'fulfilled' && Array.isArray(results[1].value)) {
      setCandles(results[1].value);
    } else {
      nextErrors.push(`price history: ${results[1].reason?.message || 'invalid response from the backend'}`);
    }

    setErrors(nextErrors);
    setRefreshing(false);
    inFlight.current = false;
  }, [symbol]);

  useEffect(() => {
    refresh();
    const timer = window.setInterval(refresh, REFRESH_INTERVAL);
    const disconnect = connectPriceStream((price) => {
      if (price.symbol !== symbol || !Number.isFinite(Number(price.close))) return;
      setStreamConnected(true);
      setLatestPrice(price);
      setCandles((current) => {
        const timestamp = price.timestamp;
        const withoutDuplicate = current.filter((candle) => candle.timestamp !== timestamp);
        return [...withoutDuplicate, price].slice(-50);
      });
    }, (connected) => {
      if (!connected) setStreamConnected(false);
    });

    return () => {
      window.clearInterval(timer);
      disconnect();
    };
  }, [refresh, symbol]);

  return { latestPrice, candles, errors, streamConnected, refreshing, refresh };
}