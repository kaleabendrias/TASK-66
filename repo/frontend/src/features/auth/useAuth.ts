import { useAuthStore } from '@/state/authStore';

export function useAuth() {
  const store = useAuthStore();
  return {
    user: store.user,
    token: store.token,
    isAuthenticated: store.isAuthenticated,
    loading: store.loading,
    error: store.error,
    login: store.login,
    register: store.register,
    logout: store.logout,
    loadFromStorage: store.loadFromStorage,
    isRole: (role: string) => store.user?.role === role,
    hasAnyRole: (...roles: string[]) => store.user ? roles.includes(store.user.role) : false,
  };
}
