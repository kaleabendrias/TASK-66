import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import Input from '@/components/ui/Input';

describe('Input', () => {
  it('renders with label', () => {
    render(<Input label="Email" name="email" />);
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
  });

  it('renders without label', () => {
    const { container } = render(<Input name="plain" />);
    expect(container.querySelector('label')).toBeNull();
  });

  it('shows error message', () => {
    render(<Input label="Name" name="name" error="Required field" />);
    expect(screen.getByText('Required field')).toBeInTheDocument();
  });

  it('applies input-error class when error exists', () => {
    const { container } = render(<Input name="x" error="bad" />);
    expect(container.querySelector('input')).toHaveClass('input-error');
  });

  it('does not apply input-error class when no error', () => {
    const { container } = render(<Input name="x" />);
    expect(container.querySelector('input')).not.toHaveClass('input-error');
  });

  it('forwards onChange events', () => {
    const handler = vi.fn();
    render(<Input name="test" onChange={handler} />);
    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'hello' } });
    expect(handler).toHaveBeenCalled();
  });

  it('uses name as id fallback', () => {
    render(<Input label="Foo" name="foo" />);
    const input = screen.getByLabelText('Foo');
    expect(input.id).toBe('foo');
  });
});
