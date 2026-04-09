import React from 'react';

interface BadgeProps {
  children: React.ReactNode;
  variant?: 'info' | 'success' | 'warning' | 'danger' | 'neutral';
  className?: string;
}

const Badge: React.FC<BadgeProps> = ({ children, variant = 'neutral', className = '' }) => {
  return <span className={`badge badge-${variant} ${className}`}>{children}</span>;
};

export default Badge;
