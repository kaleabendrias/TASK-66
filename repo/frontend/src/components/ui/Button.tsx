import React from 'react';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  loading?: boolean;
}

const Button: React.FC<ButtonProps> = ({
  variant = 'primary',
  size = 'md',
  loading = false,
  children,
  className = '',
  disabled,
  ...props
}) => {
  const classes = `btn btn-${variant} btn-${size} ${className}`.trim();
  return (
    <button className={classes} disabled={disabled || loading} {...props}>
      {loading ? 'Loading...' : children}
    </button>
  );
};

export default Button;
