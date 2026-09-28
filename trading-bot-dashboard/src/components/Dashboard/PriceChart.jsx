import { useMemo, useState } from 'react';

const money = (value) => {
  const amount = Number(value);
  return Number.isFinite(amount)
    ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount)
    : '—';
};

const movingAverage = (values, period) => values.map((_, index) => {
  if (index + 1 < period) return null;
  const window = values.slice(index - period + 1, index + 1);
  return window.reduce((sum, value) => sum + value, 0) / period;
});

const relativeStrength = (values, period = 14) => values.map((_, index) => {
  if (index < period) return null;
  let gains = 0;
  let losses = 0;
  for (let cursor = index - period + 1; cursor <= index; cursor++) {
    const delta = values[cursor] - values[cursor - 1];
    if (delta > 0) gains += delta;
    else losses -= delta;
  }
  if (!losses) return 100;
  return 100 - 100 / (1 + gains / losses);
});

export default function PriceChart({
  symbol, symbols, latestPrice, candles = [], onSymbolChange, streamConnected, signal,
  decisionMode = 'signal', onDecisionModeChange = () => {},
  rsiBuyLevel = '30', onRsiBuyLevelChange = () => {},
  rsiSellLevel = '70', onRsiSellLevelChange = () => {},
}) {
  const [chartType, setChartType] = useState('area');
  const [range, setRange] = useState('1H');
  const [indicators, setIndicators] = useState({ sma: true, ema: true, bollinger: false, rsi: false, volume: false });
  const { data, closes, change, lastPrice, low, high, coords, sma, ema, bands, rsi, volumeMax } = useMemo(() => {
    const now = Date.now();
    const intervalMs = { '1H': 60 * 60_000, '24H': 24 * 60 * 60_000, '7D': 7 * 24 * 60 * 60_000 };
    const normalized = candles
      .filter((candle) => Number(candle.close) > 0)
      .map((candle) => ({ ...candle, time: new Date(candle.timestamp).getTime(), open: Number(candle.open ?? candle.close), high: Number(candle.high ?? candle.close), low: Number(candle.low ?? candle.close), close: Number(candle.close), volume: Number(candle.volume || 0) }))
      .filter((candle) => Number.isFinite(candle.time) && Number.isFinite(candle.close))
      .sort((a, b) => a.time - b.time);
    const cutoff = range === 'ALL' ? 0 : now - intervalMs[range];
    const data = normalized.filter((candle) => candle.time >= cutoff);
    const closes = data.map((candle) => candle.close);
    const lastPrice = closes.at(-1);
    const change = closes.length > 1 && closes[0] ? (lastPrice - closes[0]) / closes[0] * 100 : null;
    const averagesSma = movingAverage(closes, 20);
    const averagesEma = closes.map((_, index) => {
      if (index < 19) return null;
      const window = closes.slice(index - 19, index + 1);
      const multiplier = 2 / 21;
      return window.slice(1).reduce((value, close) => value + (close - value) * multiplier, window[0]);
    });
    const bollinger = averagesSma.map((mean, index) => {
      if (mean === null) return null;
      const slice = closes.slice(index - 19, index + 1);
      const deviation = Math.sqrt(slice.reduce((sum, value) => sum + (value - mean) ** 2, 0) / slice.length);
      return { upper: mean + deviation * 2, lower: mean - deviation * 2 };
    });
    const rsi = relativeStrength(closes);
    const prices = data.flatMap((candle) => [candle.low, candle.high]);
    const low = prices.length ? Math.min(...prices) : 0;
    const high = prices.length ? Math.max(...prices) : 0;
    const volumeMax = Math.max(1, ...data.map((candle) => candle.volume));
    const coords = (value) => 200 - ((value - low) / (high - low || high * .001 || 1)) * 165;
    return { data, closes, change, lastPrice, low, high, coords, sma: averagesSma, ema: averagesEma, bands: bollinger, rsi, volumeMax };
  }, [candles, range]);

  const x = (index) => data.length <= 1 ? 400 : index / (data.length - 1) * 800;
  const line = data.map((_, index) => `${x(index)},${coords(closes[index])}`).join(' ');
  const overlay = (series) => series.map((value, index) => value === null ? null : `${x(index)},${coords(value)}`).filter(Boolean).join(' ');
  const currentRsi = [...rsi].reverse().find((value) => value !== null);
  const isBull = data.length > 1 && lastPrice >= closes[0];
  const buyLevel = Math.max(1, Math.min(49, Number(rsiBuyLevel) || 30));
  const sellLevel = Math.max(buyLevel + 1, Math.min(99, Number(rsiSellLevel) || 70));
  const decisionLabel = decisionMode === 'signal' && signal?.action
    ? signal.action
    : currentRsi < buyLevel ? 'BUY' : currentRsi > sellLevel ? 'SELL' : 'HOLD';
  const decisionReason = decisionMode === 'signal' && signal?.reason
    ? signal.reason
    : currentRsi == null ? 'Collecting enough prices for an RSI reading.'
      : `RSI ${currentRsi.toFixed(1)} · ${currentRsi < buyLevel ? `below your ${buyLevel} entry level` : currentRsi > sellLevel ? `above your ${sellLevel} exit level` : `neutral between ${buyLevel} and ${sellLevel}`}.`;

  const toggle = (key) => setIndicators((current) => ({ ...current, [key]: !current[key] }));

  return (
    <article className="panel chart-panel">
      <div className="panel-header">
        <div><span className="eyebrow">MARKET WATCH</span><h2>{symbol} <span className="live-pill"><span className="live-dot" />{streamConnected ? 'Live stream' : 'Reconnecting'}</span></h2></div>
        <label className="select-wrap"><span className="sr-only">Select market</span><select value={symbol} onChange={(event) => onSymbolChange(event.target.value)}>{symbols.map((item) => <option key={item}>{item}</option>)}</select></label>
      </div>
      <div className="price-line">
        <strong>{latestPrice ? money(latestPrice.close) : '—'}</strong>
        {change !== null && <span className={change >= 0 ? 'gain' : 'loss'}>{change >= 0 ? '+' : ''}{change.toFixed(2)}% selected range</span>}
        {latestPrice?.timestamp && <span className="price-time">Updated {new Date(latestPrice.timestamp).toLocaleTimeString()}</span>}
      </div>
      <div className="chart-toolbar">
        <div className="chart-choice" aria-label="Chart style">{[['area', 'Area'], ['line', 'Line'], ['candle', 'Candles']].map(([value, label]) => <button key={value} type="button" className={chartType === value ? 'selected' : ''} aria-pressed={chartType === value} onClick={() => setChartType(value)}>{label}</button>)}</div>
        <div className="chart-choice chart-ranges" aria-label="Chart time range">{['1H', '24H', '7D', 'ALL'].map((value) => <button key={value} type="button" className={range === value ? 'selected' : ''} aria-pressed={range === value} onClick={() => setRange(value)}>{value}</button>)}</div>
      </div>
      <div className="chart-indicator-toggles">
        {[['sma', 'SMA 20'], ['ema', 'EMA 20'], ['bollinger', 'Bollinger'], ['rsi', 'RSI'], ['volume', 'Volume']].map(([key, label]) => <label key={key}><input type="checkbox" checked={indicators[key]} onChange={() => toggle(key)} />{label}</label>)}
      </div>
      {data.length < 2 ? (
        <div className="chart-empty">{data.length ? 'Collecting price history…' : 'No candles in this time range yet. Try ALL.'}</div>
      ) : (
        <div className={`chart chart-${chartType}`}>
          <svg viewBox="0 0 800 230" preserveAspectRatio="none" role="img" aria-label={`${symbol} ${chartType} price chart`}>
            <g className="chart-grid-lines"><path d="M0 35H800 M0 90H800 M0 145H800 M0 200H800" /></g>
            {chartType === 'area' && <path d={`M ${line.replaceAll(' ', ' L ')} L 800 230 L 0 230 Z`} fill="url(#chart-fill)" />}
            {(chartType === 'area' || chartType === 'line') && <polyline points={line} fill="none" stroke="#16a673" strokeWidth="3" vectorEffect="non-scaling-stroke" />}
            {chartType === 'candle' && data.map((candle, index) => {
              const candleWidth = Math.max(2, Math.min(12, 640 / data.length));
              const yOpen = coords(candle.open);
              const yClose = coords(candle.close);
              const rising = candle.close >= candle.open;
              return <g key={`${candle.time}-${index}`} className={rising ? 'candle-up' : 'candle-down'}><title>{new Date(candle.time).toLocaleString()} · O {money(candle.open)} · H {money(candle.high)} · L {money(candle.low)} · C {money(candle.close)}</title><line x1={x(index)} x2={x(index)} y1={coords(candle.high)} y2={coords(candle.low)} /><rect x={x(index) - candleWidth / 2} y={Math.min(yOpen, yClose)} width={candleWidth} height={Math.max(1.5, Math.abs(yClose - yOpen))} /></g>;
            })}
            {indicators.bollinger && <><polyline points={overlay(bands.map((value) => value?.upper ?? null))} fill="none" stroke="#b08bdb" strokeDasharray="4 4" strokeWidth="1.5" /><polyline points={overlay(bands.map((value) => value?.lower ?? null))} fill="none" stroke="#b08bdb" strokeDasharray="4 4" strokeWidth="1.5" /></>}
            {indicators.sma && <polyline points={overlay(sma)} fill="none" stroke="#e5a83d" strokeWidth="2" />}
            {indicators.ema && <polyline points={overlay(ema)} fill="none" stroke="#4d88dc" strokeWidth="2" />}
            {indicators.volume && data.map((candle, index) => <rect key={`volume-${candle.time}`} x={x(index) - 2} y={225 - candle.volume / volumeMax * 34} width="4" height={candle.volume / volumeMax * 34} fill="#96b9a6" opacity=".48" />)}
          </svg>
        </div>
      )}
      {indicators.rsi && currentRsi !== null && currentRsi !== undefined && <div className="rsi-strip"><span>RSI 14</span><div className="rsi-track"><i style={{ left: `${currentRsi}%` }} /><span className="rsi-low" /><span className="rsi-high" /></div><strong className={currentRsi < buyLevel ? 'gain' : currentRsi > sellLevel ? 'loss' : ''}>{currentRsi.toFixed(1)}</strong></div>}
      <div className="chart-legend">
        {indicators.sma && <span><i className="legend-sma" />SMA 20</span>}{indicators.ema && <span><i className="legend-ema" />EMA 20</span>}{indicators.bollinger && <span><i className="legend-bands" />Bollinger bands</span>}{indicators.volume && <span><i className="legend-volume" />Volume</span>}
      </div>
      <div className="chart-labels"><span>{data[0] ? new Date(data[0].time).toLocaleString() : '—'}</span><span>{data.length} candles</span><span>{data.length ? new Date(data.at(-1).time).toLocaleTimeString() : '—'}</span></div>
      <div className="chart-decision">
        <span className={`signal-badge ${decisionLabel.toLowerCase()}`}>{decisionLabel}</span>
        <div><strong>{signal?.strategyName?.replaceAll('_', ' ') || 'RSI 14 setup'}</strong><span>{decisionReason}</span></div>
        <span className={isBull ? 'gain' : 'loss'}>{isBull ? '↑' : '↓'} range</span>
      </div>
      <div className="chart-decision-controls">
        <label>Decision source<select value={decisionMode} onChange={(event) => onDecisionModeChange(event.target.value)}><option value="signal">AI strategy signal</option><option value="rsi">My RSI levels</option></select></label>
        <label>RSI buy below<input aria-label="RSI buy threshold" type="number" min="1" max="49" value={rsiBuyLevel} onChange={(event) => onRsiBuyLevelChange(event.target.value)} /></label>
        <label>RSI sell above<input aria-label="RSI sell threshold" type="number" min="51" max="99" value={rsiSellLevel} onChange={(event) => onRsiSellLevelChange(event.target.value)} /></label>
      </div>
    </article>
  );
}