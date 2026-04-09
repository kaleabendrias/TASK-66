import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import Button from '@/components/ui/Button';

describe('Button', () => {
  it('renders children', () => {
    render(<Button>Click Me</Button>);
    expect(screen.getByText('Click Me')).toBeInTheDocument();
  });

  it('calls onClick handler', () => {
    const handler = vi.fn();
    render(<Button onClick={handler}>Click</Button>);
    fireEvent.click(screen.getByText('Click'));
    expect(handler).toHaveBeenCalledOnce();
  });

  it('is disabled when disabled prop is true', () => {
    render(<Button disabled>Nope</Button>);
    expect(screen.getByRole('button')).toBeDisabled();
  });

  it('shows loading text when loading', () => {
    render(<Button loading>Submit</Button>);
    expect(screen.getByText('Loading...')).toBeInTheDocument();
    expect(screen.getByRole('button')).toBeDisabled();
  });

  it('applies variant and size classes', () => {
    const { container } = render(
      <Button variant="danger" size="lg">
        Delete
      </Button>,
    );
    expect(container.firstChild).toHaveClass('btn-danger');
    expect(container.firstChild).toHaveClass('btn-lg');
  });

  it('defaults to primary variant and md size', () => {
    const { container } = render(<Button>Default</Button>);
    expect(container.firstChild).toHaveClass('btn-primary');
    expect(container.firstChild).toHaveClass('btn-md');
  });
});
