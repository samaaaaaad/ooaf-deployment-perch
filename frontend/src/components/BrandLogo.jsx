import './BrandLogo.css';

/** Use the yellow wordmark on dark surfaces and the primary wordmark on light ones. */
export default function BrandLogo({ onDark = false }) {
  return (
    <img
      className="perch-logo"
      src={`/branding/perch-logo-${onDark ? 'dark-backgrounds' : 'primary'}.png`}
      alt="Perch"
      width="2340"
      height="624"
    />
  );
}
