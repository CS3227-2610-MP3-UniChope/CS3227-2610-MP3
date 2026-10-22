import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import 'bootstrap/dist/css/bootstrap.min.css';
import { BookingsPage } from './pages/BookingsPage';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BookingsPage />
  </StrictMode>,
);
