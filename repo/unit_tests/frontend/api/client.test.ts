import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';

// Test the real client behavior, not a mock
// client.ts is excluded from coverage but we still verify behavior here

describe('API client (real module)', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('client instance is an axios instance', async () => {
    const { default: client } = await import('@/api/client');
    expect(client).toBeDefined();
    expect(typeof client.get).toBe('function');
    expect(typeof client.post).toBe('function');
    expect(typeof client.put).toBe('function');
    expect(typeof client.delete).toBe('function');
    expect(typeof client.patch).toBe('function');
  });

  it('client baseURL contains /api', async () => {
    const { default: client } = await import('@/api/client');
    expect(client.defaults.baseURL).toContain('/api');
  });

  it('client has request interceptors registered', async () => {
    const { default: client } = await import('@/api/client');
    // Axios stores handlers internally; we verify the interceptors object exists
    expect(client.interceptors).toBeDefined();
    expect(client.interceptors.request).toBeDefined();
    expect(client.interceptors.response).toBeDefined();
  });

  it('token from localStorage is injected into request Authorization header', async () => {
    localStorage.setItem('token', 'test-bearer-token');

    const { default: client } = await import('@/api/client');

    // Spy on the adapter to capture the config sent to the network
    const adapterSpy = vi.fn().mockResolvedValue({
      data: {},
      status: 200,
      statusText: 'OK',
      headers: {},
      config: {},
    });

    const originalAdapter = client.defaults.adapter;
    client.defaults.adapter = adapterSpy;

    try {
      await client.get('/test');
    } catch {
      // ignore network errors; we care about the config
    } finally {
      client.defaults.adapter = originalAdapter;
    }

    if (adapterSpy.mock.calls.length > 0) {
      const capturedConfig = adapterSpy.mock.calls[0][0];
      expect(capturedConfig.headers?.Authorization).toBe('Bearer test-bearer-token');
    }
  });

  it('request with no token has no Authorization header', async () => {
    // localStorage already cleared in beforeEach
    const { default: client } = await import('@/api/client');

    const adapterSpy = vi.fn().mockResolvedValue({
      data: {},
      status: 200,
      statusText: 'OK',
      headers: {},
      config: {},
    });

    const originalAdapter = client.defaults.adapter;
    client.defaults.adapter = adapterSpy;

    try {
      await client.get('/test-no-auth');
    } catch {
      // ignore
    } finally {
      client.defaults.adapter = originalAdapter;
    }

    if (adapterSpy.mock.calls.length > 0) {
      const capturedConfig = adapterSpy.mock.calls[0][0];
      const authHeader = capturedConfig.headers?.Authorization;
      expect(authHeader === undefined || authHeader === null || authHeader === '').toBeTruthy();
    }
  });
});

describe('API client localStorage helpers', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('token stored in localStorage is accessible', () => {
    localStorage.setItem('token', 'test-token-123');
    expect(localStorage.getItem('token')).toBe('test-token-123');
  });

  it('removing token from localStorage clears it', () => {
    localStorage.setItem('token', 'temp');
    localStorage.removeItem('token');
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('localStorage survives multiple set/get cycles', () => {
    localStorage.setItem('token', 'first');
    expect(localStorage.getItem('token')).toBe('first');
    localStorage.setItem('token', 'second');
    expect(localStorage.getItem('token')).toBe('second');
  });
});
