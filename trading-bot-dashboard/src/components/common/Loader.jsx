export default function Loader({ label = 'Loading…', compact = false }) {
  return (
    <div className={`loader ${compact ? 'loader-compact' : ''}`} role="status">
      <span className="loader-spinner" aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}