import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';

const RouterContext = createContext(null);

function currentPath() {
  return window.location.pathname || '/';
}

export function RouterProvider({ children }) {
  const [path, setPath] = useState(currentPath);

  useEffect(() => {
    const handlePopState = () => setPath(currentPath());
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const navigate = useCallback((to, { replace = false } = {}) => {
    if (to === currentPath()) return;
    window.history[replace ? 'replaceState' : 'pushState']({}, '', to);
    setPath(currentPath());
  }, []);

  const value = useMemo(() => ({ path, navigate }), [path, navigate]);
  return <RouterContext.Provider value={value}>{children}</RouterContext.Provider>;
}

export function useRouter() {
  const context = useContext(RouterContext);
  if (!context) throw new Error('useRouter must be used inside RouterProvider');
  return context;
}

export function Redirect({ to }) {
  const { navigate } = useRouter();

  useEffect(() => {
    navigate(to, { replace: true });
  }, [navigate, to]);

  return null;
}

export function Link({ to, onClick, children, ...props }) {
  const { navigate } = useRouter();

  function handleClick(event) {
    onClick?.(event);
    if (
      event.defaultPrevented
      || event.button !== 0
      || event.metaKey
      || event.ctrlKey
      || event.shiftKey
      || event.altKey
    ) return;

    event.preventDefault();
    navigate(to);
  }

  return <a href={to} onClick={handleClick} {...props}>{children}</a>;
}

export function NavLink({ to, className = '', children, ...props }) {
  const { path } = useRouter();
  const classes = `${className} ${path === to ? 'active' : ''}`.trim();
  return <Link to={to} className={classes} {...props}>{children}</Link>;
}
