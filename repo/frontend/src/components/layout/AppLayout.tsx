import React from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/state/authStore';
import { Role } from '@/api/types';

interface NavItem {
  to: string;
  label: string;
  roles?: Role[];
}

const navItems: NavItem[] = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/discover', label: 'Discover' },
  { to: '/products', label: 'Products' },
  { to: '/orders', label: 'Orders' },
  { to: '/my-listings', label: 'My Listings', roles: ['SELLER', 'ADMINISTRATOR'] },
  { to: '/inventory', label: 'Inventory', roles: ['SELLER', 'WAREHOUSE_STAFF', 'ADMINISTRATOR'] },
  { to: '/reservations', label: 'My Reservations', roles: ['MEMBER', 'SELLER', 'WAREHOUSE_STAFF', 'MODERATOR', 'ADMINISTRATOR'] },
  { to: '/fulfillment', label: 'Fulfillment', roles: ['WAREHOUSE_STAFF', 'ADMINISTRATOR'] },
  { to: '/profile', label: 'My Profile' },
  { to: '/incidents', label: 'Incidents' },
  { to: '/appeals', label: 'Appeals' },
  { to: '/moderator', label: 'Moderation', roles: ['MODERATOR', 'ADMINISTRATOR'] },
  { to: '/admin', label: 'Admin', roles: ['ADMINISTRATOR'] },
  {
    to: '/users',
    label: 'Users',
    roles: ['ADMINISTRATOR'],
  },
];

const AppLayout: React.FC = () => {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const visibleNavItems = navItems.filter((item) => {
    if (!item.roles) return true;
    return user && item.roles.includes(user.role);
  });

  return (
    <div className="app-layout">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <h2>Demo App</h2>
        </div>
        <nav className="sidebar-nav">
          {visibleNavItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `nav-link ${isActive ? 'nav-link-active' : ''}`}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className="main-wrapper">
        <header className="topbar">
          <div className="topbar-left">
            <span className="topbar-welcome">
              {user?.displayName || user?.username}
            </span>
            <span className="badge badge-info">{user?.role}</span>
          </div>
          <button className="btn btn-secondary btn-sm" onClick={handleLogout}>
            Logout
          </button>
        </header>
        <main className="main-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default AppLayout;
