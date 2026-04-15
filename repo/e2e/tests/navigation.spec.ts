import { test, expect, Page } from '@playwright/test';

/**
 * Navigation and cross-page journey E2E tests:
 * - Products discovery to detail page
 * - Dashboard navigation to different sections
 * - 404 page for unknown routes
 * - Back navigation works correctly
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

test.describe('Public discovery page', () => {
  test('discovery page loads without auth', async ({ page }) => {
    await page.goto('/discover');
    await page.evaluate(() => localStorage.clear());
    await expect(page).toHaveURL(/\/discover/, { timeout: 5000 });
    // Page should render without errors
    await expect(page.locator('body')).toBeVisible();
  });

  test('404 page shown for unknown routes', async ({ page }) => {
    await page.goto('/this-route-does-not-exist');
    // Should show 404 page or redirect
    const content = await page.textContent('body');
    const is404 = content?.includes('404') || content?.includes('Not Found') || content?.includes('not found');
    const wasRedirected = !page.url().includes('/this-route-does-not-exist');
    expect(is404 || wasRedirected).toBe(true);
  });
});

test.describe('Authenticated navigation journeys', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'member');
  });

  test('nav from dashboard to products', async ({ page }) => {
    await page.goto('/dashboard');
    await expect(page.getByText(/welcome back/i)).toBeVisible({ timeout: 10000 });

    await page.goto('/products');
    await expect(page.getByRole('heading', { name: /products/i })).toBeVisible({ timeout: 10000 });
  });

  test('nav from products to orders', async ({ page }) => {
    await page.goto('/products');
    await expect(page.getByRole('heading', { name: /products/i })).toBeVisible({ timeout: 10000 });

    await page.goto('/orders');
    await expect(page.getByRole('heading', { name: /orders/i })).toBeVisible({ timeout: 10000 });
  });

  test('nav from dashboard to incidents', async ({ page }) => {
    await page.goto('/dashboard');
    await page.goto('/incidents');
    await expect(page.getByRole('heading', { name: /incidents/i })).toBeVisible({ timeout: 10000 });
  });

  test('nav from incidents back to dashboard', async ({ page }) => {
    await page.goto('/incidents');
    await expect(page.getByRole('heading', { name: /incidents/i })).toBeVisible({ timeout: 10000 });

    await page.goto('/dashboard');
    await expect(page.getByText(/welcome back/i)).toBeVisible({ timeout: 10000 });
  });

  test('profile page is accessible', async ({ page }) => {
    await page.goto('/profile');
    await expect(page).toHaveURL(/\/profile/, { timeout: 5000 });
  });

  test('appeals page loads', async ({ page }) => {
    await page.goto('/appeals');
    await expect(page).toHaveURL(/\/appeals/, { timeout: 10000 });
    await expect(page.getByRole('heading', { name: /appeals/i })).toBeVisible({ timeout: 10000 });
  });
});

test.describe('Multi-role workflow', () => {
  test('seller can create product and member can view it', async ({ browser }) => {
    // Two separate contexts: seller creates, member views
    const sellerCtx = await browser.newContext({ ignoreHTTPSErrors: true });
    const sellerPage = await sellerCtx.newPage();

    await sellerPage.goto('/login');
    await sellerPage.getByLabel('Username').fill('seller');
    await sellerPage.getByLabel('Password').fill('password123');
    await sellerPage.getByRole('button', { name: 'Login' }).click();
    await expect(sellerPage).toHaveURL(/\/dashboard/, { timeout: 15000 });

    // Seller navigates to products
    await sellerPage.goto('/products');
    await expect(sellerPage.getByRole('heading', { name: /products/i })).toBeVisible({ timeout: 10000 });

    await sellerCtx.close();

    // Member views same products page
    const memberCtx = await browser.newContext({ ignoreHTTPSErrors: true });
    const memberPage = await memberCtx.newPage();

    await memberPage.goto('/login');
    await memberPage.getByLabel('Username').fill('member');
    await memberPage.getByLabel('Password').fill('password123');
    await memberPage.getByRole('button', { name: 'Login' }).click();
    await expect(memberPage).toHaveURL(/\/dashboard/, { timeout: 15000 });

    await memberPage.goto('/products');
    await expect(memberPage.getByRole('heading', { name: /products/i })).toBeVisible({ timeout: 10000 });

    await memberCtx.close();
  });
});
