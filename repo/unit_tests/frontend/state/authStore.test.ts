import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  register: vi.fn(),
}));
vi.mock('@/api/users', () => ({
  getMe: vi.fn(),
}));

import { useAuthStore } from '@/state/authStore';
import { login as apiLogin } from '@/api/auth';
import { getMe } from '@/api/users';

describe('authStore', () => {
  beforeEach(() => {
    useAuthStore.setState({
      user: null,
      token: null,
      isAuthenticated: false,
      loading: false,
      error: null,
    });
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('starts with no user', () => {
    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.isAuthenticated).toBe(false);
    expect(state.token).toBeNull();
    expect(state.loading).toBe(false);
    expect(state.error).toBeNull();
  });

  it('login sets user and token', async () => {
    (apiLogin as any).mockResolvedValue({
      token: 'tok123',
      username: 'admin',
      role: 'ADMINISTRATOR',
    });
    (getMe as any).mockResolvedValue({
      id: 1,
      username: 'admin',
      role: 'ADMINISTRATOR',
      displayName: 'Admin',
      email: 'admin@test.com',
      enabled: true,
    });

    await useAuthStore.getState().login('admin', 'password123');

    const state = useAuthStore.getState();
    expect(state.isAuthenticated).toBe(true);
    expect(state.user?.username).toBe('admin');
    expect(state.token).toBe('tok123');
    expect(localStorage.getItem('token')).toBe('tok123');
  });

  it('login persists token to localStorage', async () => {
    (apiLogin as any).mockResolvedValue({ token: 'persist-tok', username: 'u', role: 'MEMBER' });
    (getMe as any).mockResolvedValue({ id: 1, username: 'u', role: 'MEMBER', displayName: 'U', email: 'u@t.c', enabled: true });

    await useAuthStore.getState().login('u', 'pass');
    expect(localStorage.getItem('token')).toBe('persist-tok');
  });

  it('login clears previous error before attempting', async () => {
    useAuthStore.setState({ error: 'Old error' });
    (apiLogin as any).mockResolvedValue({ token: 'tok', username: 'u', role: 'MEMBER' });
    (getMe as any).mockResolvedValue({ id: 1, username: 'u', role: 'MEMBER', displayName: 'U', email: 'u@t.c', enabled: true });

    await useAuthStore.getState().login('u', 'pass');
    expect(useAuthStore.getState().error).toBeNull();
  });

  it('logout clears state', () => {
    useAuthStore.setState({
      user: {
        id: 1,
        username: 'admin',
        email: 'a@b.c',
        displayName: 'Admin',
        role: 'ADMINISTRATOR',
        enabled: true,
      },
      token: 'tok',
      isAuthenticated: true,
    });
    localStorage.setItem('token', 'tok');

    useAuthStore.getState().logout();

    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(useAuthStore.getState().user).toBeNull();
    expect(useAuthStore.getState().token).toBeNull();
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('logout removes token from localStorage', () => {
    localStorage.setItem('token', 'should-be-gone');
    useAuthStore.getState().logout();
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('login failure sets error and clears loading', async () => {
    (apiLogin as any).mockRejectedValue({
      response: { data: { message: 'Bad credentials' } },
    });

    await expect(
      useAuthStore.getState().login('x', 'y'),
    ).rejects.toBeDefined();

    expect(useAuthStore.getState().error).toBeTruthy();
    expect(useAuthStore.getState().loading).toBe(false);
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
  });

  it('login failure does not set token', async () => {
    (apiLogin as any).mockRejectedValue({
      response: { data: { message: 'Bad credentials' } },
    });

    try { await useAuthStore.getState().login('x', 'y'); } catch {}
    expect(useAuthStore.getState().token).toBeNull();
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('register sets user and token', async () => {
    (vi.mocked(await import('@/api/auth')).register as any).mockResolvedValue({
      token: 'reg-tok',
      username: 'newuser',
      role: 'MEMBER',
    });
    (getMe as any).mockResolvedValue({
      id: 2,
      username: 'newuser',
      role: 'MEMBER',
      displayName: 'New User',
      email: 'new@test.com',
      enabled: true,
    });

    await useAuthStore.getState().register('newuser', 'new@test.com', 'pass', 'New User');

    const state = useAuthStore.getState();
    expect(state.isAuthenticated).toBe(true);
    expect(state.user?.username).toBe('newuser');
  });

  it('loadFromStorage restores session', async () => {
    localStorage.setItem('token', 'stored-tok');
    (getMe as any).mockResolvedValue({
      id: 1,
      username: 'admin',
      role: 'ADMINISTRATOR',
      displayName: 'Admin',
      email: 'a@b.c',
      enabled: true,
    });

    await useAuthStore.getState().loadFromStorage();

    expect(useAuthStore.getState().isAuthenticated).toBe(true);
    expect(useAuthStore.getState().user?.username).toBe('admin');
    expect(useAuthStore.getState().token).toBe('stored-tok');
  });

  it('loadFromStorage does nothing when no token in localStorage', async () => {
    // No token set
    await useAuthStore.getState().loadFromStorage();

    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(useAuthStore.getState().user).toBeNull();
    expect(getMe).not.toHaveBeenCalled();
  });

  it('loadFromStorage handles getMe failure gracefully', async () => {
    localStorage.setItem('token', 'bad-token');
    (getMe as any).mockRejectedValue(new Error('401 Unauthorized'));

    await useAuthStore.getState().loadFromStorage();

    // Should not be authenticated after getMe fails
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
  });
});
