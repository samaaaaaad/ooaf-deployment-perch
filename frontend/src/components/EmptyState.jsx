export default function EmptyState({ title, description }) {
  return (
    <div className="empty-state">
      <span className="empty-state-icon" aria-hidden="true">○</span>
      <h3>{title}</h3>
      <p>{description}</p>
    </div>
  );
}
