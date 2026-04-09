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

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: null,
  isAuthenticated: false,
  loading: false,
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
    set({ user: null, token: null, isAuthenticated: false, error: null });
  },

  loadFromStorage: async () => {
    const token = localStorage.getItem('token');
    if (!token) return;
    set({ loading: true });
    try {
      const user = await getMe();
      set({ token, user, isAuthenticated: true, loading: false });
    } catch {
      localStorage.removeItem('token');
      localStorage.removeItem('username');
      localStorage.removeItem('role');
      set({ token: null, user: null, isAuthenticated: false, loading: false });
    }
  },
}));
