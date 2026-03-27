export default function Notice({ type = 'info', text }) {
  if (!text) return null;
  return <div className={`notice ${type}`}>{text}</div>;
}

