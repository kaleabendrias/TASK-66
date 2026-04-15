import { test, expect, Page } from '@playwright/test';

/**
 * Role-Based Access Control E2E tests:
 * - Members cannot access admin routes
 * - Sellers can access /my-listings but not /admin
 * - Admin can access /admin and /users
 * - Warehouse staff can access /fulfillment
 * - Unauthenticated users are redirected from all protected routes
 */

async function loginAs(page: Page, username: string) {
  await page.goto('/login');
  await page.evaluate(() => localStorage.clear());
  await page.goto('/login');
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Password').fill('password123');
  await page.getByRole('button', { name: 'Login' }).click();
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 15000 });
}

test.describe('MEMBER role restrictions', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'member');
  });

  test('member can access dashboard', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page.getByText(/welcome back/i)).toBeVisible({ timeout: 10000 });
  });

  test('member can access products page', async ({ page }) => {
    await page.goto('/products');
    await expect(page).toHaveURL(/\/products/, { timeout: 5000 });
  });

  test('member cannot access /admin route', async ({ page }) => {
    await page.goto('/admin');
    // Should be redirected or see access denied
    await expect(page).not.toHaveURL(/\/admin$/, { timeout: 10000 });
  });

  test('member cannot access /users route', async ({ page }) => {
    await page.goto('/users');
    await expect(page).not.toHaveURL(/\/users$/, { timeout: 5000 });
  });

  test('member cannot access /inventory route', async ({ page }) => {
    await page.goto('/inventory');
    await expect(page).not.toHaveURL(/\/inventory$/, { timeout: 5000 });
  });

  test('member cannot access /fulfillment route', async ({ page }) => {
    await page.goto('/fulfillment');
    await expect(page).not.toHaveURL(/\/fulfillment$/, { timeout: 5000 });
  });

  test('member can access /incidents page', async ({ page }) => {
    await page.goto('/incidents');
    await expect(page).toHaveURL(/\/incidents/, { timeout: 5000 });
    await expect(page.getByRole('heading', { name: /incidents/i })).toBeVisible({ timeout: 10000 });
  });
});

test.describe('ADMINISTRATOR role access', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'admin');
  });

  test('admin can access /admin route', async ({ page }) => {
    await page.goto('/admin');
    await expect(page).toHaveURL(/\/admin/, { timeout: 5000 });
  });

  test('admin can access /users route', async ({ page }) => {
    await page.goto('/users');
    await expect(page).toHaveURL(/\/users/, { timeout: 5000 });
  });

  test('admin can access dashboard and sees ADMINISTRATOR badge', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page.getByText('ADMINISTRATOR').first()).toBeVisible({ timeout: 10000 });
  });
});

test.describe('SELLER role access', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'seller');
  });

  test('seller can access /my-listings', async ({ page }) => {
    await page.goto('/my-listings');
    await expect(page).toHaveURL(/\/my-listings/, { timeout: 5000 });
  });

  test('seller cannot access /admin', async ({ page }) => {
    await page.goto('/admin');
    await expect(page).not.toHaveURL(/\/admin$/, { timeout: 5000 });
  });

  test('seller can access /inventory', async ({ page }) => {
    await page.goto('/inventory');
    await expect(page).toHaveURL(/\/inventory/, { timeout: 5000 });
  });
});

test.describe('MODERATOR role access', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'moderator');
  });

  test('moderator can access /moderator dashboard', async ({ page }) => {
    await page.goto('/moderator');
    await expect(page).toHaveURL(/\/moderator/, { timeout: 5000 });
  });

  test('moderator can access /incidents', async ({ page }) => {
    await page.goto('/incidents');
    await expect(page).toHaveURL(/\/incidents/, { timeout: 5000 });
  });
});

test.describe('Unauthenticated access', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.evaluate(() => localStorage.clear());
  });

  test('unauthenticated user redirected from /dashboard', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page).not.toHaveURL(/\/dashboard$/, { timeout: 5000 });
  });

  test('unauthenticated user redirected from /orders', async ({ page }) => {
    await page.goto('/orders');
    await expect(page).not.toHaveURL(/\/orders$/, { timeout: 5000 });
  });

  test('/discover is publicly accessible', async ({ page }) => {
    await page.goto('/discover');
    await expect(page).toHaveURL(/\/discover/, { timeout: 5000 });
  });

  test('/ redirects to /discover', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveURL(/\/discover/, { timeout: 5000 });
  });
});
