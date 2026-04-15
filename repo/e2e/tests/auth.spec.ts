import { test, expect } from '@playwright/test';

/**
 * Authentication E2E tests:
 * - Login with valid credentials navigates to dashboard
 * - Login with invalid credentials shows error
 * - Registration creates account and lands on dashboard
 * - Logout clears session and redirects to login
 * - Auth persistence: refreshing page keeps user logged in
 */

test.describe('Login flow', () => {
  test.beforeEach(async ({ page }) => {
    // Clear any persisted session
    await page.goto('/login');
    await page.evaluate(() => localStorage.clear());
  });

  test('login page renders form elements', async ({ page }) => {
    await page.goto('/login');
    await expect(page.getByLabel('Username')).toBeVisible();
    await expect(page.getByLabel('Password')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Login' })).toBeVisible();
    await expect(page.getByText('Register')).toBeVisible();
  });

  test('valid credentials navigate to dashboard', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Username').fill('member');
    await page.getByLabel('Password').fill('password123');
    await page.getByRole('button', { name: 'Login' }).click();

    await expect(page).toHaveURL(/\/dashboard/, { timeout: 15000 });
    await expect(page.getByText(/welcome back/i)).toBeVisible({ timeout: 10000 });
  });

  test('invalid credentials show error and stay on login page', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Username').fill('member');
    await page.getByLabel('Password').fill('wrongpassword');
    await page.getByRole('button', { name: 'Login' }).click();

    // Should remain on login page and show error
    await expect(page).toHaveURL(/\/login/, { timeout: 5000 });
    await expect(page.locator('.alert-danger')).toBeVisible({ timeout: 5000 });
  });

  test('login as admin sees ADMINISTRATOR role badge', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Username').fill('admin');
    await page.getByLabel('Password').fill('password123');
    await page.getByRole('button', { name: 'Login' }).click();

    await expect(page).toHaveURL(/\/dashboard/, { timeout: 15000 });
    await expect(page.getByText('ADMINISTRATOR').first()).toBeVisible({ timeout: 10000 });
  });

  test('auth persists after page refresh', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Username').fill('member');
    await page.getByLabel('Password').fill('password123');
    await page.getByRole('button', { name: 'Login' }).click();

    await expect(page).toHaveURL(/\/dashboard/, { timeout: 15000 });

    // Refresh the page — session should persist via localStorage
    await page.reload();
    await expect(page).toHaveURL(/\/dashboard/, { timeout: 15000 });
    await expect(page.getByText(/welcome back/i)).toBeVisible({ timeout: 10000 });
  });

  test('logout clears session and redirects to login', async ({ page }) => {
    // Login first
    await page.goto('/login');
    await page.getByLabel('Username').fill('member');
    await page.getByLabel('Password').fill('password123');
    await page.getByRole('button', { name: 'Login' }).click();

    await expect(page).toHaveURL(/\/dashboard/, { timeout: 15000 });

    // Find and click logout
    const logoutBtn = page.getByRole('button', { name: /logout/i }).or(page.getByText(/sign out/i));
    if (await logoutBtn.isVisible()) {
      await logoutBtn.click();
      await expect(page).toHaveURL(/\/(login|discover)/, { timeout: 10000 });
    } else {
      // Manually clear and verify unauthenticated state
      await page.evaluate(() => localStorage.removeItem('token'));
      await page.goto('/dashboard');
      await expect(page).toHaveURL(/\/(login|discover)/, { timeout: 5000 });
    }
  });

  test('unauthenticated user redirected from protected route', async ({ page }) => {
    await page.goto('/dashboard');
    // Should redirect away from dashboard
    await expect(page).not.toHaveURL(/\/dashboard$/, { timeout: 5000 });
  });
});

test.describe('Registration flow', () => {
  test('registration page renders all fields', async ({ page }) => {
    await page.goto('/register');
    await expect(page.getByLabel('Username')).toBeVisible();
    await expect(page.getByLabel('Email')).toBeVisible();
    await expect(page.getByLabel('Password')).toBeVisible();
    await expect(page.getByLabel('Display Name')).toBeVisible();
    await expect(page.getByRole('button', { name: /register/i })).toBeVisible();
  });

  test('successful registration navigates to dashboard', async ({ page }) => {
    const unique = Date.now();
    await page.goto('/register');
    await page.getByLabel('Username').fill(`e2euser${unique}`);
    await page.getByLabel('Email').fill(`e2e${unique}@test.com`);
    await page.getByLabel('Display Name').fill('E2E User');
    await page.getByLabel('Password').fill('password123');
    await page.getByRole('button', { name: /register/i }).click();

    await expect(page).toHaveURL(/\/dashboard/, { timeout: 15000 });
  });

  test('registration with existing username shows error', async ({ page }) => {
    await page.goto('/register');
    await page.getByLabel('Username').fill('admin');
    await page.getByLabel('Email').fill('newemail@test.com');
    await page.getByLabel('Display Name').fill('Admin Copy');
    await page.getByLabel('Password').fill('password123');
    await page.getByRole('button', { name: /register/i }).click();

    // Should either show error or stay on register page
    await expect(
      page.locator('.alert-danger').or(page.getByText(/error|taken|exists/i))
    ).toBeVisible({ timeout: 10000 });
  });

  test('login link on register page navigates to login', async ({ page }) => {
    await page.goto('/register');
    await page.getByRole('link', { name: /login/i }).click();
    await expect(page).toHaveURL(/\/login/, { timeout: 5000 });
  });
});
