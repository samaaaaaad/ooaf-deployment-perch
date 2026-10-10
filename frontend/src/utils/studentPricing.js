// US 12: student helpers. The server is the source of truth (it decides eligibility and the
// discount at checkout); these only mirror it so the UI can show the right prices.

const STUDENT_DOMAINS = ['.edu.au', '.edu'];

/** True when an admin has verified the signed-in user as a student. */
export function isStudent(user) {
  return user?.studentEligible === true;
}

/** True when the user's email is on a university domain, which lets them apply for verification. */
export function hasUniversityEmail(user) {
  if (!user) return false;
  if (typeof user.universityEmail === 'boolean') return user.universityEmail;
  const domain = (user.email || '').toLowerCase().split('@')[1] || '';
  return STUDENT_DOMAINS.some((suffix) => domain.endsWith(suffix));
}

/** price reduced by percent%, rounded to cents (matches StudentEligibility.applyDiscount). */
export function discounted(price, percent) {
  const value = Number(price);
  if (!percent || percent <= 0 || Number.isNaN(value)) return value;
  return Math.round(value * (100 - percent)) / 100;
}

/** Formats a dollar amount without trailing ".00" noise for whole numbers. */
export function money(value) {
  const n = Number(value);
  return Number.isInteger(n) ? `$${n}` : `$${n.toFixed(2)}`;
}
