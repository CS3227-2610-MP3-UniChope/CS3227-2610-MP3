import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach, beforeEach, vi } from 'vitest';

afterEach(cleanup);

// Theme is the sole permitted non-private persisted preference. Isolate cases.
beforeEach(() => {
  vi.restoreAllMocks();
  localStorage.clear(); sessionStorage.clear();
  document.documentElement.removeAttribute('data-bs-theme');
  document.documentElement.style.removeProperty('color-scheme');
});
