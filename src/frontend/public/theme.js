// Runs synchronously before the application. Match src/theme.tsx's exact-value
// validation and silent light fallback; never write a preference during loading.
(function () {
  var theme = 'light';
  try { if (window.localStorage.getItem('unichope.theme') === 'dark') theme = 'dark'; }
  catch (_) { /* Storage may be unavailable. */ }
  document.documentElement.setAttribute('data-bs-theme', theme);
}());
