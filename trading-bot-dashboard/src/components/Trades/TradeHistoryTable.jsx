import TradeRow from './TradeRow.jsx';

export default function TradeHistoryTable({ trades = [] }) {
  if (!trades.length) {
    return <div className="empty-state"><strong>No trades have been recorded yet</strong><span>Completed orders will show up here.</span></div>;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead><tr><th>Asset</th><th>Side</th><th>Quantity</th><th>Price</th><th>P&amp;L</th><th>Time</th></tr></thead>
        <tbody>{trades.map((trade) => <TradeRow key={trade.id} trade={trade} />)}</tbody>
      </table>
    </div>
  );
}