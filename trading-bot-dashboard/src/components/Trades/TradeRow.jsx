const money = (value) => new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(Number(value || 0));
const quantity = (value) => Number(value || 0).toLocaleString('en-US', { maximumFractionDigits: 6 });

export default function TradeRow({ trade }) {
  const type = String(trade.type || '').toLowerCase();
  return (
    <tr>
      <td><strong>{trade.symbol}</strong></td>
      <td><span className={`side ${type}`}>{trade.type || '—'}</span></td>
      <td>{quantity(trade.quantity)}</td>
      <td>{money(trade.price)}</td>
      <td className={Number(trade.pnl || 0) >= 0 ? 'gain' : 'loss'}>{money(trade.pnl)}</td>
      <td>{trade.timestamp ? new Date(trade.timestamp).toLocaleString() : '—'}</td>
    </tr>
  );
}