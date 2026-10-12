import { useState } from 'react';

import { submitStudentVerification } from '../api/studentVerificationApi';
import { hasUniversityEmail } from '../utils/studentPricing';

const ACCEPTED_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
const MAX_SIZE_MB = 3;

// US 12: a customer applies to be recognised as a university student. An admin reviews the
// details and card photo; nothing changes for the customer until the request is approved.
export default function StudentVerificationModal({ user, onClose, onSubmitted }) {
  const [university, setUniversity] = useState('');
  const [studentNumber, setStudentNumber] = useState('');
  const [cardImage, setCardImage] = useState(null);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const canApply = hasUniversityEmail(user);

  function handleFile(e) {
    const file = e.target.files?.[0];
    setError('');
    if (!file) return;
    if (!ACCEPTED_TYPES.includes(file.type)) {
      setError('Only JPG, PNG or WEBP images are accepted.');
      return;
    }
    if (file.size > MAX_SIZE_MB * 1024 * 1024) {
      setError(`Image must be smaller than ${MAX_SIZE_MB} MB.`);
      return;
    }
    const reader = new FileReader();
    reader.onload = (event) => setCardImage(event.target.result);
    reader.readAsDataURL(file);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!university.trim()) { setError('Please enter your university.'); return; }
    if (!/^[A-Za-z0-9-]{4,30}$/.test(studentNumber.trim())) {
      setError('Student number must be 4-30 letters, digits or dashes.');
      return;
    }
    if (!cardImage) { setError('Please upload a photo of your student card.'); return; }

    setSubmitting(true);
    setError('');
    try {
      const created = await submitStudentVerification(user.userId, {
        university: university.trim(),
        studentNumber: studentNumber.trim(),
        cardImage,
      });
      onSubmitted(created);
    } catch (err) {
      setError(err.message || 'Unable to send your verification request');
      setSubmitting(false);
    }
  }

  return (
    <div style={{
      position: 'fixed', inset: 0, backgroundColor: 'rgba(10, 35, 40, 0.75)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100, padding: '1rem',
      backdropFilter: 'blur(4px)',
    }}>
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="student-verify-title"
        style={{
          background: '#ffffff', borderRadius: '16px', maxWidth: '480px', width: '100%',
          padding: '1.75rem', boxShadow: '0 20px 40px rgba(0,0,0,0.2)', maxHeight: '90vh', overflowY: 'auto',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
          <div>
            <p className="eyebrow" style={{ margin: 0 }}>Student pricing</p>
            <h2 id="student-verify-title" style={{ margin: '0.25rem 0 0', fontSize: '1.3rem' }}>Verify student status</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={submitting}
            style={{ background: 'transparent', border: 0, fontSize: '1.5rem', cursor: 'pointer', color: '#667681' }}
            aria-label="Close"
          >
            ×
          </button>
        </div>

        {!canApply ? (
          <>
            <p style={{ color: '#667681', fontSize: '0.92rem', lineHeight: 1.5 }}>
              Student verification is only available to accounts registered with a university email
              (ending in .edu.au or .edu). Create an account with your university email to apply.
            </p>
            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '1.25rem' }}>
              <button type="button" className="button button-secondary" onClick={onClose}>Close</button>
            </div>
          </>
        ) : (
          <form onSubmit={handleSubmit} noValidate>
            <p style={{ color: '#667681', fontSize: '0.88rem', lineHeight: 1.5, marginTop: 0 }}>
              An admin will check your details against your student card. Once approved you&apos;ll get student
              prices and access to students-only events. Your card photo is deleted after review.
            </p>

            <div className="field">
              <label htmlFor="sv-university">University</label>
              <input
                id="sv-university"
                type="text"
                value={university}
                onChange={(e) => setUniversity(e.target.value)}
                placeholder="e.g. University of Technology Sydney"
                maxLength={150}
                disabled={submitting}
              />
            </div>

            <div className="field">
              <label htmlFor="sv-number">Student number</label>
              <input
                id="sv-number"
                type="text"
                value={studentNumber}
                onChange={(e) => setStudentNumber(e.target.value)}
                placeholder="e.g. 12345678"
                maxLength={30}
                disabled={submitting}
              />
            </div>

            <div className="field">
              <label htmlFor="sv-card">Student card photo</label>
              <input
                id="sv-card"
                type="file"
                accept="image/jpeg,image/png,image/webp"
                onChange={handleFile}
                disabled={submitting}
              />
              <small>JPG, PNG or WEBP, under {MAX_SIZE_MB} MB. Make sure your name and student number are readable.</small>
              {cardImage && (
                <img
                  src={cardImage}
                  alt="Student card preview"
                  style={{ marginTop: '0.6rem', maxWidth: '100%', maxHeight: '180px', borderRadius: '8px', border: '1px solid #cbd5d9' }}
                />
              )}
            </div>

            {error && <div className="notice notice-error" role="alert">{error}</div>}

            <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', marginTop: '1.25rem' }}>
              <button type="button" className="button button-secondary" onClick={onClose} disabled={submitting}>
                Cancel
              </button>
              <button type="submit" className="button button-primary" disabled={submitting}>
                {submitting ? 'Sending…' : 'Send for review'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
