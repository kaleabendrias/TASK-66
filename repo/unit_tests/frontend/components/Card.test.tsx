import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import Card from '@/components/ui/Card';

describe('Card', () => {
  it('renders children', () => {
    render(<Card>Card content</Card>);
    expect(screen.getByText('Card content')).toBeInTheDocument();
  });

  it('renders title when provided', () => {
    render(<Card title="My Card">Body</Card>);
    expect(screen.getByText('My Card')).toBeInTheDocument();
  });

  it('does not render header when no title', () => {
    const { container } = render(<Card>Body only</Card>);
    expect(container.querySelector('.card-header')).toBeNull();
  });

  it('applies custom className', () => {
    const { container } = render(<Card className="custom">Content</Card>);
    expect(container.firstChild).toHaveClass('custom');
  });

  it('wraps body in card-body div', () => {
    const { container } = render(<Card>Inner</Card>);
    expect(container.querySelector('.card-body')).not.toBeNull();
    expect(container.querySelector('.card-body')?.textContent).toBe('Inner');
  });
});
