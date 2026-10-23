import { useEffect, useState } from 'react';
const INDEX = 'unichopeNavigationIndex';
const pathNow = () => window.location.pathname + window.location.search;
function indexNow() {
  const value: unknown = window.history.state?.[INDEX];
  return typeof value === 'number' && Number.isSafeInteger(value) ? value : null;
}
export function navigate(path: string, replace = false, bypassGuard = false) {
  if (!replace && !bypassGuard && !window.dispatchEvent(new CustomEvent('unichope:navigate', { cancelable: true, detail: { path } }))) return;
  const index = indexNow() ?? 0;
  window.history[replace ? 'replaceState' : 'pushState']({ [INDEX]: replace ? index : index + 1 }, '', path);
  // Programmatic navigation has already run its guard (auth replacements intentionally bypass it).
  window.dispatchEvent(new Event('unichope:location'));
}
export function useLocation() {
  const [location, setLocation] = useState(() => {
    if (indexNow() === null) window.history.replaceState({ ...window.history.state, [INDEX]: 0 }, '', window.location.href);
    return pathNow();
  });
  useEffect(() => {
    let index = indexNow() ?? 0, currentPath = pathNow();
    let restoring = false, resumeAfterRestore = false, allowed = false, requestedDelta = 0;
    const update = () => { index = indexNow() ?? index; currentPath = pathNow(); setLocation(currentPath); };
    const traversal = (event: PopStateEvent) => {
      if (restoring) {
        restoring = false;
        if (resumeAfterRestore) { resumeAfterRestore = false; allowed = true; window.history.go(requestedDelta); }
        return;
      }
      const targetIndex: unknown = event.state?.[INDEX];
      const delta = typeof targetIndex === 'number' ? targetIndex - index : 0;
      if (!allowed && delta !== 0 && pathNow() !== currentPath) {
        const proceed = () => {
          if (restoring) resumeAfterRestore = true;
          else { allowed = true; window.history.go(delta); }
        };
        if (!window.dispatchEvent(new CustomEvent('unichope:navigate', { cancelable: true, detail: { path: pathNow(), proceed } }))) {
          requestedDelta = delta; restoring = true; window.history.go(-delta);
          return;
        }
      }
      allowed = false; update();
    };
    window.addEventListener('popstate', traversal); window.addEventListener('unichope:location', update);
    return () => { window.removeEventListener('popstate', traversal); window.removeEventListener('unichope:location', update); };
  }, []);
  return location;
}
