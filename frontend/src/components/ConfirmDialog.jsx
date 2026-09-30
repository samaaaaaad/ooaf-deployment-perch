import { useState } from 'react';

export default function ConfirmDialog({
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  danger = false,
  onConfirm,
  onClose,
}) {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  async function handleConfirm() {
    setSubmitting(true);
    setError('');
    try {
      await onConfirm();
    } catch (err) {
      setError(err.message || 'Something went wrong');
      setSubmitting(false);
    }
  }

  return (
    <div style={{
      position: 'fixed', inset: 0, backgroundColor: 'rgba(10, 35, 40, 0.75)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100, padding: '1rem',
      backdropFilter: 'blur(4px)',
    }}>
      <div style={{
        background: '#ffffff', borderRadius: '16px', maxWidth: '420px', width: '100%',
        padding: '1.75rem', boxShadow: '0 20px 40px rgba(0,0,0,0.2)',
      }}>
        <h2 style={{ margin: '0 0 0.6rem', fontSize: '1.2rem' }}>{title}</h2>
        <p style={{ color: '#667681', fontSize: '0.92rem', margin: 0, lineHeight: 1.5 }}>{message}</p>

        {error && <div className="notice notice-error" role="alert" style={{ marginTop: '1rem' }}>{error}</div>}

        <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', marginTop: '1.5rem' }}>
          <button type="button" className="button button-secondary" onClick={onClose} disabled={submitting}>
            {cancelLabel}
          </button>
          <button
            type="button"
            className={danger ? 'button button-danger' : 'button button-primary'}
            onClick={handleConfirm}
            disabled={submitting}
          >
            {submitting ? 'Working…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
