const money = (value) => new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(Number(value || 0));

export default function TransactionHistory({ transactions = [] }) {
  if (!transactions.length) {
    return <div className="empty-state"><strong>No fund transactions yet</strong><span>Deposits and withdrawals will appear here.</span></div>;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead><tr><th>Type</th><th>Amount</th><th>Balance after</th><th>Date</th></tr></thead>
        <tbody>
          {transactions.map((transaction) => (
            <tr key={transaction.id}>
              <td><span className={`side ${String(transaction.type).toLowerCase()}`}>{transaction.type}</span></td>
              <td>{money(transaction.amount)}</td>
              <td>{money(transaction.balanceAfter)}</td>
              <td>{transaction.timestamp ? new Date(transaction.timestamp).toLocaleString() : '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}