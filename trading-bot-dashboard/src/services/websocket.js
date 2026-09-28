export function connectPriceStream(onPrice, onConnectionChange = () => {}) {
  let socket;
  let reconnectTimer;
  let connectTimer;
  let closedByUser = false;

  const connect = () => {
    if (closedByUser) return;
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    socket = new WebSocket(`${protocol}//${window.location.host}/ws/prices`);

    socket.onopen = () => {
      if (closedByUser) {
        socket.close();
        return;
      }
      onConnectionChange(true);
    };
    socket.onmessage = (event) => {
      try {
        const price = JSON.parse(event.data);
        if (price && typeof price.symbol === 'string') onPrice(price);
      } catch (error) {
        console.error('Unable to read a live price update:', error);
      }
    };
    socket.onerror = () => {
      if (!closedByUser) onConnectionChange(false);
    };
    socket.onclose = () => {
      if (closedByUser) return;
      onConnectionChange(false);
      if (!closedByUser) reconnectTimer = window.setTimeout(connect, 5000);
    };
  };

  connectTimer = window.setTimeout(connect, 0);

  return () => {
    closedByUser = true;
    window.clearTimeout(connectTimer);
    window.clearTimeout(reconnectTimer);
    if (socket?.readyState === WebSocket.OPEN) socket.close();
  };
}