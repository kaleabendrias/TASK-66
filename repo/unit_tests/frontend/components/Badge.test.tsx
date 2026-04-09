import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import Badge from '@/components/ui/Badge';

describe('Badge', () => {
  it('renders children text', () => {
    render(<Badge>Test Label</Badge>);
    expect(screen.getByText('Test Label')).toBeInTheDocument();
  });

  it('applies variant class', () => {
    const { container } = render(<Badge variant="success">OK</Badge>);
    expect(container.firstChild).toHaveClass('badge-success');
  });

  it('defaults to neutral variant', () => {
    const { container } = render(<Badge>Default</Badge>);
    expect(container.firstChild).toHaveClass('badge-neutral');
  });

  it('applies custom className', () => {
    const { container } = render(<Badge className="extra">X</Badge>);
    expect(container.firstChild).toHaveClass('extra');
  });

  it('renders as a span', () => {
    const { container } = render(<Badge variant="info">Info</Badge>);
    expect(container.firstChild?.nodeName).toBe('SPAN');
  });
});
