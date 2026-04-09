import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import Table from '@/components/ui/Table';

describe('Table', () => {
  it('renders headers', () => {
    render(
      <Table headers={['Name', 'Age']}>
        <tr>
          <td>Alice</td>
          <td>30</td>
        </tr>
      </Table>,
    );
    expect(screen.getByText('Name')).toBeInTheDocument();
    expect(screen.getByText('Age')).toBeInTheDocument();
  });

  it('renders row data', () => {
    render(
      <Table headers={['Item']}>
        <tr>
          <td>Widget</td>
        </tr>
        <tr>
          <td>Gadget</td>
        </tr>
      </Table>,
    );
    expect(screen.getByText('Widget')).toBeInTheDocument();
    expect(screen.getByText('Gadget')).toBeInTheDocument();
  });

  it('applies custom className', () => {
    const { container } = render(
      <Table headers={['H']} className="striped">
        <tr>
          <td>D</td>
        </tr>
      </Table>,
    );
    expect(container.querySelector('table')).toHaveClass('striped');
  });

  it('wraps in table-wrapper div', () => {
    const { container } = render(
      <Table headers={['H']}>
        <tr>
          <td>D</td>
        </tr>
      </Table>,
    );
    expect(container.querySelector('.table-wrapper')).not.toBeNull();
  });
});
