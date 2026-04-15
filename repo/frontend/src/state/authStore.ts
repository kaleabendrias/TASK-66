import { create } from 'zustand';
import { login as apiLogin, register as apiRegister } from '@/api/auth';
import { getMe } from '@/api/users';
import { User, Role } from '@/api/types';

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string, displayName: string) => Promise<void>;
  logout: () => void;
  loadFromStorage: () => Promise<void>;
}

// Read token and cached user synchronously at module init.
// Starting with loading=true (when a token exists) prevents ProtectedRoute
// from flash-redirecting to /login before loadFromStorage can run.
const _storedToken = (() => { try { return localStorage.getItem('token'); } catch { return null; } })();
const _storedUser = (() => {
  try {
    const raw = localStorage.getItem('user');
    return raw ? (JSON.parse(raw) as User) : null;
  } catch { return null; }
})();

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: _storedToken,
  isAuthenticated: false,
  loading: !!_storedToken,
  error: null,

  login: async (username: string, password: string) => {
    set({ loading: true, error: null });
    try {
      const res = await apiLogin(username, password);
      localStorage.setItem('token', res.token);
      localStorage.setItem('username', res.username);
      localStorage.setItem('role', res.role);
      // Fetch full user profile
      const user = await getMe();
      localStorage.setItem('user', JSON.stringify(user));
      set({ token: res.token, user, isAuthenticated: true, loading: false });
    } catch (err: any) {
      const message = err.response?.data?.message || err.response?.data || 'Login failed';
      set({ error: String(message), loading: false });
      throw err;
    }
  },

  register: async (username: string, email: string, password: string, displayName: string) => {
    set({ loading: true, error: null });
    try {
      const res = await apiRegister(username, email, password, displayName);
      localStorage.setItem('token', res.token);
      localStorage.setItem('username', res.username);
      localStorage.setItem('role', res.role);
      const user = await getMe();
      localStorage.setItem('user', JSON.stringify(user));
      set({ token: res.token, user, isAuthenticated: true, loading: false });
    } catch (err: any) {
      const message = err.response?.data?.message || err.response?.data || 'Registration failed';
      set({ error: String(message), loading: false });
      throw err;
    }
  },

  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    localStorage.removeItem('user');
    set({ user: null, token: null, isAuthenticated: false, error: null });
  },

  loadFromStorage: async () => {
    const token = localStorage.getItem('token');
    if (!token) {
      set({ loading: false });
      return;
    }
    // If we have a cached user, restore instantly without an API round-trip.
    // This keeps ProtectedRoute from blocking on a network call every page load.
    if (_storedUser) {
      set({ token, user: _storedUser, isAuthenticated: true, loading: false });
      return;
    }
    // No cached user — fall back to API (e.g. after logout/login cycle clears 'user').
    try {
      const user = await getMe();
      localStorage.setItem('user', JSON.stringify(user));
      set({ token, user, isAuthenticated: true, loading: false });
    } catch {
      localStorage.removeItem('token');
      localStorage.removeItem('username');
      localStorage.removeItem('role');
      localStorage.removeItem('user');
      set({ token: null, user: null, isAuthenticated: false, loading: false });
    }
  },
}));
