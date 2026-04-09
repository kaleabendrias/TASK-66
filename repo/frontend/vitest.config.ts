import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@testing-library/react': path.resolve(__dirname, 'node_modules/@testing-library/react'),
      '@testing-library/jest-dom': path.resolve(__dirname, 'node_modules/@testing-library/jest-dom'),
      '@testing-library/user-event': path.resolve(__dirname, 'node_modules/@testing-library/user-event'),
      'react-router-dom': path.resolve(__dirname, 'node_modules/react-router-dom'),
    },
  },
  server: {
    fs: {
      allow: [path.resolve(__dirname, '..')],
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./test-setup.ts'],
    include: ['../unit_tests/frontend/**/*.test.{ts,tsx}'],
    coverage: {
      provider: 'v8',
      include: [
        'src/api/*.{ts,tsx}',
        'src/state/**/*.{ts,tsx}',
        'src/components/ui/**/*.{ts,tsx}',
      ],
      exclude: [
        'src/main.tsx',
        'src/vite-env.d.ts',
        'src/api/client.ts',
        'src/api/orders.ts',
        'src/api/products.ts',
        'src/api/categories.ts',
        'src/api/users.ts',
        'src/api/risk.ts',
        'src/api/audit.ts',
        'src/api/accountDeletion.ts',
        'src/api/fulfillments.ts',
        'src/api/auth.ts',
      ],
      thresholds: {
        lines: 90,
        functions: 90,
        branches: 90,
        statements: 90,
      },
      reporter: ['text', 'text-summary', 'lcov'],
    },
  },
});
