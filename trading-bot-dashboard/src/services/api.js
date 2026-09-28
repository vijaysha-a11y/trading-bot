const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');
let csrfToken;

export async function apiRequest(path, options = {}) {
  const method = (options.method || 'GET').toUpperCase();
  const headers = {
    Accept: 'application/json',
    ...(options.body ? { 'Content-Type': 'application/json' } : {}),
    ...options.headers,
  };
  if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
    if (!csrfToken) {
      let tokenResponse;
      try {
        tokenResponse = await fetch(`${API_BASE_URL}/api/auth/csrf`, { credentials: 'include' });
      } catch {
        throw new Error('Cannot reach the authentication service. Start the backend on port 8080 and try again.');
      }
      if (!tokenResponse.ok) {
        throw new Error(`Secure sign-in setup failed (${tokenResponse.status}). Restart the backend with the latest code and try again.`);
      }
      const tokenPayload = await tokenResponse.json();
      if (typeof tokenPayload.token !== 'string' || !tokenPayload.token) {
        throw new Error('The authentication service returned an invalid security token. Restart the backend and try again.');
      }
      csrfToken = tokenPayload.token;
    }
    headers['X-XSRF-TOKEN'] = csrfToken;
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    method,
    credentials: 'include',
    headers,
  });

  if (!response.ok) {
    const body = await response.text();
    let message = body;

    if (body) {
      try {
        const details = JSON.parse(body);
        message = details.message || details.detail || details.error || body;
      } catch {
        message = body;
      }
    }

    throw new Error(message || `Request failed (${response.status} ${response.statusText})`);
  }

  if (response.status === 204) return null;

  const body = await response.text();
  return body ? JSON.parse(body) : null;
}

export const getPortfolioSummary = (portfolioId) =>
  apiRequest(`/api/portfolio/${portfolioId}/summary`);

export const getLatestPrice = (symbol) =>
  apiRequest(`/api/market-data/${encodeURIComponent(symbol)}/latest`);

export const getRecentPrices = (symbol, count = 50) =>
  apiRequest(`/api/market-data/${encodeURIComponent(symbol)}/recent?count=${count}`);

export const getSignals = (symbol) =>
  apiRequest(`/api/strategy/${encodeURIComponent(symbol)}/signals`);

export const getSignalHistory = (symbol) =>
  apiRequest(`/api/strategy/${encodeURIComponent(symbol)}/history`);

export const getTrades = (portfolioId) =>
  apiRequest(`/api/trades/portfolio/${portfolioId}`);

export const getPaperOrders = (portfolioId) =>
  apiRequest(`/api/trades/portfolio/${portfolioId}/orders`);

export const cancelPaperOrder = (orderId) =>
  apiRequest(`/api/trades/orders/${orderId}`, { method: 'DELETE' });

export const executeTrade = (trade) =>
  apiRequest('/api/trades/execute', {
    method: 'POST',
    body: JSON.stringify(trade),
  });

export const updateFunds = (portfolioId, action, amount) =>
  apiRequest(`/api/funds/${portfolioId}/${action}`, {
    method: 'POST',
    body: JSON.stringify({ amount }),
  });

export const getTransactions = (portfolioId) =>
  apiRequest(`/api/funds/${portfolioId}/transactions`);

export const runBacktest = ({ symbol, strategyName, startDate, endDate, startingBalance }) => {
  const query = new URLSearchParams({ symbol, strategyName, startDate, endDate, startingBalance: String(startingBalance) });
  return apiRequest(`/api/strategy/backtest?${query}`, { method: 'POST' });
};

export const register = ({ name, email, password }) =>
  apiRequest('/api/auth/register', { method: 'POST', body: JSON.stringify({ name, email, password }) });

export const login = ({ email, password }) =>
  apiRequest('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) });

export const getCurrentUser = () => apiRequest('/api/auth/me');

export const logout = () => apiRequest('/api/auth/logout', { method: 'POST' });

export const getAdminUsers = () => apiRequest('/api/admin/users');