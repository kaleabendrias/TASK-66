import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { vi } from 'vitest';

vi.mock('react-router-dom', () => ({
  Link: ({ children, to }: any) => <a href={to}>{children}</a>,
}));

import NotFoundPage from '@/pages/NotFoundPage';

describe('NotFoundPage', () => {
  it('renders 404 message', () => {
    render(<NotFoundPage />);
    expect(screen.getByText('404 - Not Found')).toBeInTheDocument();
  });

  it('shows descriptive text', () => {
    render(<NotFoundPage />);
    expect(
      screen.getByText('The page you are looking for does not exist.'),
    ).toBeInTheDocument();
  });

  it('has a link to dashboard', () => {
    render(<NotFoundPage />);
    const link = screen.getByText('Go to Dashboard');
    expect(link).toBeInTheDocument();
    expect(link.closest('a')).toHaveAttribute('href', '/dashboard');
  });
});
