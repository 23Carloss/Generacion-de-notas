export default function EmptyState({ title, subtitle, actionLabel, onAction }) {
  return (
    <div className="empty-state">
      <p className="eyebrow">Nada por aquí</p>
      <h2>{title}</h2>
      <p className="hint">{subtitle}</p>
      {actionLabel && onAction && (
        <button className="btn btn-primary" style={{ marginTop: "1rem" }} onClick={onAction}>
          {actionLabel}
        </button>
      )}
    </div>
  );
}
