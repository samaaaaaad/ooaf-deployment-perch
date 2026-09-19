import { useRef, useState } from 'react';

const ACCEPTED_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
const MAX_SIZE_MB = 5;

export default function PublishEventModal({ event, onClose, onPublish }) {
  const [imageDataUrl, setImageDataUrl] = useState(null);
  const [dragOver, setDragOver] = useState(false);
  const [fileError, setFileError] = useState('');
  const [publishing, setPublishing] = useState(false);
  const [publishError, setPublishError] = useState('');
  const inputRef = useRef(null);

  function readFile(file) {
    setFileError('');
    if (!ACCEPTED_TYPES.includes(file.type)) {
      setFileError('Only JPG, JPEG, PNG, or WEBP images are accepted.');
      return;
    }
    if (file.size > MAX_SIZE_MB * 1024 * 1024) {
      setFileError(`Image must be smaller than ${MAX_SIZE_MB} MB.`);
      return;
    }
    const reader = new FileReader();
    reader.onload = (e) => setImageDataUrl(e.target.result);
    reader.readAsDataURL(file);
  }

  function handleFileChange(e) {
    const file = e.target.files?.[0];
    if (file) readFile(file);
  }

  function handleDrop(e) {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files?.[0];
    if (file) readFile(file);
  }

  async function handleConfirm() {
    if (!imageDataUrl) {
      setFileError('Please select a banner image before publishing.');
      return;
    }
    setPublishing(true);
    setPublishError('');
    try {
      await onPublish(imageDataUrl);
    } catch (err) {
      setPublishError(err.message || 'Unable to publish event');
      setPublishing(false);
    }
  }

  return (
    <div style={{
      position: 'fixed', inset: 0, backgroundColor: 'rgba(10, 35, 40, 0.75)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100, padding: '1rem',
      backdropFilter: 'blur(4px)'
    }}>
      <div style={{
        background: '#ffffff', borderRadius: '16px', maxWidth: '520px', width: '100%',
        padding: '2rem', boxShadow: '0 20px 40px rgba(0,0,0,0.2)', maxHeight: '90vh', overflowY: 'auto'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1.25rem' }}>
          <div>
            <p className="eyebrow" style={{ margin: 0 }}>Publish Event</p>
            <h2 style={{ margin: '0.25rem 0 0', fontSize: '1.4rem' }}>{event.title}</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={publishing}
            style={{ background: 'transparent', border: 0, fontSize: '1.5rem', cursor: 'pointer', color: '#667681' }}
            aria-label="Close modal"
          >
            ×
          </button>
        </div>

        <p style={{ color: '#667681', fontSize: '0.9rem', marginBottom: '1.25rem' }}>
          A banner image is required to publish. It will be shown to customers browsing events.
        </p>

        {/* Drop zone */}
        <div
          role="button"
          tabIndex={0}
          onClick={() => inputRef.current?.click()}
          onKeyDown={(e) => e.key === 'Enter' && inputRef.current?.click()}
          onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
          onDragLeave={() => setDragOver(false)}
          onDrop={handleDrop}
          style={{
            border: `2px dashed ${dragOver ? '#176b63' : '#cbd5d9'}`,
            borderRadius: '12px',
            padding: '1.5rem',
            textAlign: 'center',
            cursor: 'pointer',
            background: dragOver ? '#f0faf9' : '#fafafa',
            transition: 'all 0.15s',
          }}
        >
          {imageDataUrl ? (
            <img
              src={imageDataUrl}
              alt="Banner preview"
              style={{ maxHeight: '200px', maxWidth: '100%', borderRadius: '8px', objectFit: 'cover' }}
            />
          ) : (
            <div style={{ color: '#667681' }}>
              <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>🖼</div>
              <div style={{ fontWeight: 600 }}>Click or drag & drop an image here</div>
              <small>JPG, PNG, WEBP · max {MAX_SIZE_MB} MB</small>
            </div>
          )}
        </div>

        <input
          ref={inputRef}
          type="file"
          accept="image/jpeg,image/jpg,image/png,image/webp"
          style={{ display: 'none' }}
          onChange={handleFileChange}
        />

        {imageDataUrl && (
          <button
            type="button"
            onClick={() => { setImageDataUrl(null); setFileError(''); }}
            style={{ marginTop: '0.5rem', background: 'none', border: 'none', color: '#667681', cursor: 'pointer', fontSize: '0.85rem', padding: 0 }}
          >
            Remove image
          </button>
        )}

        {fileError && <div className="notice notice-error" role="alert" style={{ marginTop: '0.75rem' }}>{fileError}</div>}
        {publishError && <div className="notice notice-error" role="alert" style={{ marginTop: '0.75rem' }}>{publishError}</div>}

        <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', marginTop: '1.5rem' }}>
          <button type="button" className="button button-secondary" onClick={onClose} disabled={publishing}>
            Cancel
          </button>
          <button type="button" className="button button-primary" onClick={handleConfirm} disabled={publishing || !imageDataUrl}>
            {publishing ? 'Publishing…' : 'Publish Event'}
          </button>
        </div>
      </div>
    </div>
  );
}
