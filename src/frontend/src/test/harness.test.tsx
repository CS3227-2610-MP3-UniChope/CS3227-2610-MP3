import { useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect, test } from 'vitest';

// Test-only sentinel: verifies React DOM events and accessible queries without
// implementing any application feature before its implementer stage.
function HarnessSentinel() {
  const [message, setMessage] = useState('');
  return (
    <form onSubmit={(event) => { event.preventDefault(); setMessage('Harness ready'); }}>
      <label htmlFor="sentinel-name">Harness name</label>
      <input id="sentinel-name" required />
      <button type="submit">Check harness</button>
      <p role="status">{message}</p>
    </form>
  );
}

test('FE0-RUN: React renders labelled controls and responds to keyboard submission', async () => {
  const user = userEvent.setup();
  render(<HarnessSentinel />);
  await user.tab();
  expect(screen.getByRole('textbox', { name: 'Harness name' })).toHaveFocus();
  await user.keyboard('UniChope');
  expect(screen.getByRole('textbox')).toHaveValue('UniChope');
  await user.tab();
  expect(screen.getByRole('button', { name: 'Check harness' })).toHaveFocus();
  await user.keyboard('{Enter}');
  expect(screen.getByRole('status')).toHaveTextContent('Harness ready');
});
