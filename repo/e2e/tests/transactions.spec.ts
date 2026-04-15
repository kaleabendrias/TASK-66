import { test, expect, Page } from '@playwright/test';

/**
 * Transactional workflow E2E tests — covers full business lifecycle flows:
 * - Order placement via API → UI verification → cancellation
 * - Fulfillment lifecycle: create + step advancement verifiable end-to-end
 * - Appeal submission by member via UI form
 * - Appeal review by moderator via UI
 *
 * Rate-limit note: all specs share the same backend 60 req/min per-user bucket.
 * By the time this file runs, the seeded 'member' account has usually exhausted
 * its quota. Each test therefore registers a fresh unique user (/api/auth/register
 * is exempt from rate limiting) so transactions run against a clean bucket.
 */

async function loginAs(page: Page, username: string): Promise<string> {
  await page.goto('/login');
  await page.evaluate(() => localStorage.clear());
  await page.goto('/login');
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Password').fill('password123');
  await page.getByRole('button', { name: 'Login' }).click();
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 15000 });
  return page.evaluate(() => localStorage.getItem('token') ?? '');
}

/**
 * Register a unique MEMBER user and complete a UI login, returning the JWT.
 * Uses a fresh bucket so earlier specs' quota consumption is irrelevant.
 */
async function freshMember(page: Page): Promise<string> {
  const username = `tx_mbr_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`;
  const regResp = await page.request.post('/api/auth/register', {
    headers: { 'Content-Type': 'application/json' },
    data: { username, password: 'password123', displayName: 'TX Member', email: `${username}@test.com` },
  });
  expect(regResp.status()).toBe(200);
  return loginAs(page, username);
}

async function placeOrderApi(page: Page, token: string, productId: number): Promise<number> {
  const resp = await page.request.post('/api/orders', {
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    data: { productId, quantity: 1, inventoryItemId: 1 },
  });
  expect(resp.status()).toBe(200);
  const body = await resp.json();
  return body.id as number;
}

// ── Order lifecycle ────────────────────────────────────────────────────────

test.describe('Order lifecycle', () => {
  test('placed order appears in member orders list with server-computed price', async ({ page }) => {
    const token = await freshMember(page);
    const orderId = await placeOrderApi(page, token, 1);

    await page.goto('/orders');
    await expect(page.getByRole('heading', { name: /orders/i })).toBeVisible({ timeout: 10000 });
    // OrdersPage renders the id in <td>{o.id}</td> without a # prefix
    await expect(page.locator('td').filter({ hasText: new RegExp(`^${orderId}$`) }).first()).toBeVisible({ timeout: 5000 });
    // Server-computed price is shown as $X.XX — must be greater than zero
    await expect(page.locator('td').filter({ hasText: /^\$\d+\.\d{2}$/ }).first()).toBeVisible({ timeout: 5000 });
  });

  test('cancelled order shows CANCELLED status in the orders list', async ({ page }) => {
    const token = await freshMember(page);
    const orderId = await placeOrderApi(page, token, 1);

    // Cancel via API (200 if succeeded, 409 if already in non-cancellable state)
    const resp = await page.request.patch(`/api/orders/${orderId}/status?status=CANCELLED`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect([200, 409]).toContain(resp.status());

    await page.goto('/orders');
    await expect(page.getByRole('heading', { name: /orders/i })).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('CANCELLED').first()).toBeVisible({ timeout: 5000 });
  });

  test('member cannot set order status to SHIPPED', async ({ page }) => {
    const token = await freshMember(page);
    const orderId = await placeOrderApi(page, token, 1);

    const resp = await page.request.patch(`/api/orders/${orderId}/status?status=SHIPPED`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(resp.status()).toBe(403);
  });
});

// ── Fulfillment pipeline ───────────────────────────────────────────────────

test.describe('Fulfillment pipeline', () => {
  /**
   * The FulfillmentPage makes O(n_orders) concurrent getFulfillmentByOrder requests on
   * load. With 70+ orders in the system the warehouse user's 60 req/min rate-limit
   * budget is exhausted and some requests return 429. We use page.route() to intercept
   * all fulfillment-status requests: our specific order's request passes through to the
   * real server; all others are answered immediately with 404 (no fulfillment). This
   * keeps the rate-limit budget intact and makes the page load deterministic.
   */
  test('warehouse creates and advances fulfillment — page reflects final step state', async ({ page }) => {
    const memberToken = await freshMember(page);
    const orderId = await placeOrderApi(page, memberToken, 1);

    const warehouseToken = await loginAs(page, 'warehouse');

    // Create fulfillment via API
    const fulResp = await page.request.post('/api/fulfillments', {
      headers: { Authorization: `Bearer ${warehouseToken}`, 'Content-Type': 'application/json' },
      data: { orderId, warehouseId: 1, idempotencyKey: `e2e-ful-${orderId}-${Date.now()}` },
    });
    expect(fulResp.status()).toBe(200);
    const ful = await fulResp.json();

    // Advance PICK via API so the detail panel shows "Advance to PACK" as the next action
    const advResp = await page.request.post(`/api/fulfillments/${ful.id}/advance`, {
      headers: { Authorization: `Bearer ${warehouseToken}`, 'Content-Type': 'application/json' },
      data: { stepName: 'PICK', notes: 'E2E — PICK completed' },
    });
    expect(advResp.status()).toBe(200);

    // Intercept getFulfillmentByOrder requests: only our order passes through;
    // all others return 404 immediately (saves rate-limit tokens).
    await page.route(/\/api\/fulfillments\/order\//, async (route) => {
      const match = route.request().url().match(/\/fulfillments\/order\/(\d+)/);
      if (match && parseInt(match[1]) === orderId) {
        await route.continue();
      } else {
        await route.fulfill({ status: 404, contentType: 'application/json', body: '{}' });
      }
    });

    await page.goto('/fulfillment');
    await expect(page.getByRole('heading', { name: /fulfillment management/i })).toBeVisible({ timeout: 10000 });

    // Click the specific order row to load the detail panel
    const orderRow = page.getByRole('row').filter({ hasText: `#${orderId}` });
    await expect(orderRow).toBeVisible({ timeout: 10000 });
    await orderRow.click();

    // The detail panel shows the post-PICK state: "Advance to PACK" is the next active step
    await expect(page.getByRole('button', { name: /advance to pack/i })).toBeVisible({ timeout: 10000 });
  });
});

// ── Appeal workflow ────────────────────────────────────────────────────────

test.describe('Appeal workflow', () => {
  test('member submits appeal via UI form and it appears in the list', async ({ page }) => {
    await freshMember(page);
    await page.goto('/appeals');
    await expect(page.getByRole('heading', { name: /appeals/i })).toBeVisible({ timeout: 10000 });

    // Open the form (header button changes to "Cancel" once open)
    await page.getByRole('button', { name: 'Submit Appeal' }).click();
    await expect(page.locator('form')).toBeVisible({ timeout: 5000 });

    // Fill in entity ID (product #1 always exists) and reason
    await page.locator('input[type="number"]').fill('1');
    await page.locator('textarea').fill('E2E test: received wrong item and requesting review');

    // Submit (header now shows "Cancel"; only the form button says "Submit Appeal")
    await page.getByRole('button', { name: 'Submit Appeal' }).click();

    // Form closes and table shows the submitted appeal
    await expect(page.locator('form')).not.toBeVisible({ timeout: 5000 });
    await expect(page.locator('table')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('SUBMITTED').first()).toBeVisible({ timeout: 10000 });
  });

  test('moderator reviews a submitted appeal and approves it via the inline UI', async ({ page }) => {
    // Create appeal using a fresh member token (avoids rate-limit collision with
    // the shared 'member' seed account used by earlier specs).
    const memberToken = await freshMember(page);
    const appealResp = await page.request.post('/api/appeals', {
      headers: { Authorization: `Bearer ${memberToken}`, 'Content-Type': 'application/json' },
      data: {
        relatedEntityType: 'PRODUCT',
        relatedEntityId: 1,
        reason: `Moderator review E2E test – ${Date.now()}`,
      },
    });
    expect(appealResp.status()).toBe(200);
    const newAppeal = await appealResp.json();

    // Log in as moderator and navigate to appeals
    await loginAs(page, 'moderator');
    await page.goto('/appeals');
    await expect(page.getByRole('heading', { name: /appeals/i })).toBeVisible({ timeout: 10000 });

    // Locate the exact appeal row we just created (AppealsPage shows #{a.id} in the first cell)
    const appealRow = page.getByRole('row').filter({ has: page.getByText(`#${newAppeal.id}`) });
    await expect(appealRow).toBeVisible({ timeout: 10000 });
    await appealRow.getByRole('button', { name: 'Review' }).click();

    // Inline review form appears — fill notes and submit
    await expect(page.getByPlaceholder('Notes')).toBeVisible({ timeout: 5000 });
    await page.getByPlaceholder('Notes').fill('Approved via E2E automated test');

    // "Submit" (exact) avoids matching the "Submit Appeal" button in the page header.
    // Also wait for the review API response so we can confirm it returned 200.
    const [, reviewResp] = await Promise.all([
      page.getByRole('button', { name: 'Submit', exact: true }).click(),
      page.waitForResponse(
        (resp) => resp.url().includes(`/appeals/${newAppeal.id}/review`),
        { timeout: 10000 },
      ),
    ]);
    expect(reviewResp.status()).toBe(200);
    // Verify the API response body contains the APPROVED status
    const reviewedAppeal = await reviewResp.json();
    expect(reviewedAppeal.status).toBe('APPROVED');

    // After a successful review the inline form closes and the appeal is removed
    // from the moderator's pending queue (GET /api/appeals returns only pending appeals)
    await expect(page.getByPlaceholder('Notes')).not.toBeVisible({ timeout: 5000 });
    await expect(appealRow).not.toBeVisible({ timeout: 15000 });
  });
});
