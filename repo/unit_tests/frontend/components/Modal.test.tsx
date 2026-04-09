import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import Modal from '@/components/ui/Modal';

describe('Modal', () => {
  it('renders children when open', () => {
    render(
      <Modal open={true} onClose={vi.fn()}>
        Modal content
      </Modal>,
    );
    expect(screen.getByText('Modal content')).toBeInTheDocument();
  });

  it('does not render when closed', () => {
    render(
      <Modal open={false} onClose={vi.fn()}>
        Hidden content
      </Modal>,
    );
    expect(screen.queryByText('Hidden content')).not.toBeInTheDocument();
  });

  it('renders title when provided', () => {
    render(
      <Modal open={true} onClose={vi.fn()} title="My Modal">
        Body
      </Modal>,
    );
    expect(screen.getByText('My Modal')).toBeInTheDocument();
  });

  it('calls onClose when overlay is clicked', () => {
    const onClose = vi.fn();
    render(
      <Modal open={true} onClose={onClose}>
        Content
      </Modal>,
    );
    fireEvent.click(screen.getByText('Content').closest('.modal-overlay')!);
    expect(onClose).toHaveBeenCalledOnce();
  });

  it('calls onClose when close button is clicked', () => {
    const onClose = vi.fn();
    render(
      <Modal open={true} onClose={onClose}>
        Content
      </Modal>,
    );
    fireEvent.click(screen.getByText('\u00d7'));
    expect(onClose).toHaveBeenCalledOnce();
  });

  it('does not close when modal content is clicked', () => {
    const onClose = vi.fn();
    render(
      <Modal open={true} onClose={onClose}>
        <button>Inner button</button>
      </Modal>,
    );
    fireEvent.click(screen.getByText('Inner button'));
    expect(onClose).not.toHaveBeenCalled();
  });
});
