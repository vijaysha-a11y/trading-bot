import { useState } from 'react';
import BacktestForm from '../components/Backtest/BacktestForm.jsx';
import BacktestReport from '../components/Backtest/BacktestReport.jsx';

export default function BacktestPage() {
  const [report, setReport] = useState(null);
  return (
    <section className="panel backtest-panel">
      <div className="panel-header"><div><span className="eyebrow">STRATEGY LAB</span><h2>Historical backtest</h2></div></div>
      <p className="muted">Simulate a virtual investment over historical market data. Backtests and paper orders never place live orders or connect to a broker.</p>
      <BacktestForm onComplete={setReport} />
      <BacktestReport report={report} />
    </section>
  );
}