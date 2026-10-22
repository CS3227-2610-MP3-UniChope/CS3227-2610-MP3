import { useEffect, useState } from 'react';
export function navigate(path: string, replace = false) {
  window.history[replace ? 'replaceState' : 'pushState']({}, '', path);
  window.dispatchEvent(new PopStateEvent('popstate'));
}
export function useLocation() {
  const [location, setLocation] = useState(() => window.location.pathname + window.location.search);
  useEffect(() => { const update = () => setLocation(window.location.pathname + window.location.search); window.addEventListener('popstate', update); return () => window.removeEventListener('popstate', update); }, []);
  return location;
}
