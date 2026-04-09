import client from './client';
import { AuthResponse } from './types';

export async function login(username: string, password: string): Promise<AuthResponse> {
  const { data } = await client.post<AuthResponse>('/auth/login', { username, password });
  return data;
}

export async function register(
  username: string,
  email: string,
  password: string,
  displayName: string
): Promise<AuthResponse> {
  const { data } = await client.post<AuthResponse>('/auth/register', {
    username,
    email,
    password,
    displayName,
  });
  return data;
}
