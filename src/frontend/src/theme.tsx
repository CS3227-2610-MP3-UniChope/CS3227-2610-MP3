import { createContext, useContext, useLayoutEffect, useState, type ReactNode } from 'react';

type Theme = 'light' | 'dark';
const preferenceKey = 'unichope.theme';
const ThemeContext = createContext<{ theme: Theme; toggle: () => void } | null>(null);

// Keep this exact-value rule in sync with public/theme.js. Storage is optional.
function readTheme(): Theme {
  try { return window.localStorage.getItem(preferenceKey) === 'dark' ? 'dark' : 'light'; }
  catch { return 'light'; }
}

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<Theme>(readTheme);
  useLayoutEffect(() => { document.documentElement.setAttribute('data-bs-theme', theme); }, [theme]);
  function toggle() {
    const next = theme === 'dark' ? 'light' : 'dark';
    setTheme(next);
    // Only explicit activation persists; never put this side effect in an updater
    // (which React StrictMode may replay) or a session lifecycle effect.
    try { window.localStorage.setItem(preferenceKey, next); } catch { /* Use page-local preference. */ }
  }
  return <ThemeContext.Provider value={{ theme, toggle }}>{children}</ThemeContext.Provider>;
}

export function ThemeSwitch() {
  const value = useContext(ThemeContext);
  if (!value) throw new Error('ThemeSwitch requires ThemeProvider');
  const dark = value.theme === 'dark';
  return <button type="button" className="btn theme-switch" aria-label="Dark theme" aria-pressed={dark} onClick={value.toggle}>
    <svg className="line-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {dark ? <><circle cx="12" cy="12" r="4" /><path d="M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1.5 1.5m11 11L19 19M5 19l1.5-1.5m11-11L19 5" /></> : <path d="M20.5 14A9 9 0 0 1 10 3.5 9 9 0 1 0 20.5 14Z" />}
    </svg><span>Dark theme</span>
  </button>;
}
