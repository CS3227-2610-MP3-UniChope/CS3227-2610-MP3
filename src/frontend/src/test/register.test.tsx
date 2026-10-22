import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { expect, test, vi } from 'vitest';
import Register from '../Register';
import { ApiError } from '../api';
import { ThemeProvider } from '../theme';

function setup(register = vi.fn<(...args: string[]) => Promise<void>>().mockResolvedValue(undefined)) {
  const onSuccess = vi.fn(); const onSignIn = vi.fn();
  const view = render(<ThemeProvider><Register register={register} onSuccess={onSuccess} onSignIn={onSignIn} /></ThemeProvider>);
  return { ...view, register, onSuccess, onSignIn, user: userEvent.setup() };
}
async function fill(user: ReturnType<typeof userEvent.setup>, password = ' Exact password! ', confirmation = password) {
  await user.type(screen.getByLabelText('Name'), 'Student Name');
  await user.type(screen.getByLabelText('Email'), 'student@example.edu');
  await user.type(screen.getByLabelText('Password'), password);
  await user.type(screen.getByLabelText('Confirm password'), confirmation);
}

test('registration submits exact passwords without role selection and returns to sign in', async () => {
  const { user, register, onSuccess } = setup();
  await fill(user);
  expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
  expect(screen.getByLabelText('Password')).toHaveAttribute('autocomplete', 'new-password');
  await user.click(screen.getByRole('button', { name: 'Create account' }));
  expect(register).toHaveBeenCalledWith('Student Name', 'student@example.edu', ' Exact password! ', ' Exact password! ');
  expect(onSuccess).toHaveBeenCalledOnce();
  expect(screen.getByLabelText('Password')).toHaveValue('');
  expect(screen.getByLabelText('Confirm password')).toHaveValue('');
});

test('mismatched and short passwords never submit a registration', async () => {
  const { user, register } = setup();
  await fill(user, 'long password', 'different password');
  await user.click(screen.getByRole('button', { name: 'Create account' }));
  expect(screen.getByRole('alert')).toHaveTextContent('The passwords do not match.');
  await user.clear(screen.getByLabelText('Password'));
  await user.type(screen.getByLabelText('Password'), 'short');
  await user.click(screen.getByRole('button', { name: 'Create account' }));
  expect(screen.getByRole('alert')).toHaveTextContent('between 8 and 128');
  expect(register).not.toHaveBeenCalled();
});

test('duplicate email shows safe guidance, clears both secrets and stays on registration', async () => {
  const { user, onSuccess } = setup(vi.fn().mockRejectedValue(new ApiError(409, 'ACCOUNT_UNAVAILABLE')));
  await fill(user);
  await user.click(screen.getByRole('button', { name: 'Create account' }));
  expect(screen.getByRole('alert')).toHaveTextContent('Try signing in or use another email');
  expect(screen.getByLabelText('Password')).toHaveValue('');
  expect(screen.getByLabelText('Confirm password')).toHaveValue('');
  expect(screen.getByLabelText('Email')).toHaveValue('student@example.edu');
  expect(onSuccess).not.toHaveBeenCalled();
});

test('uncertain registration does not retry automatically and offers sign in', async () => {
  const { user, register, onSignIn } = setup(vi.fn().mockRejectedValue(new ApiError()));
  await fill(user);
  await user.click(screen.getByRole('button', { name: 'Create account' }));
  expect(screen.getByRole('alert')).toHaveTextContent('Try signing in before submitting again');
  expect(register).toHaveBeenCalledOnce();
  await user.click(screen.getByRole('button', { name: 'Sign in' }));
  expect(onSignIn).toHaveBeenCalledOnce();
});

test('pending request blocks duplicate submits and ignores completion after leaving', async () => {
  let complete!: () => void;
  const register = vi.fn(() => new Promise<void>(resolve => { complete = resolve; }));
  const { user, onSuccess, unmount } = setup(register);
  await fill(user);
  await user.click(screen.getByRole('button', { name: 'Create account' }));
  expect(screen.getByRole('button', { name: 'Creating account…' })).toBeDisabled();
  expect(screen.getByRole('button', { name: 'Sign in' })).toBeDisabled();
  unmount();
  await act(async () => { complete(); });
  expect(onSuccess).not.toHaveBeenCalled();
  expect(register).toHaveBeenCalledOnce();
});
