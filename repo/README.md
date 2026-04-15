# Heritage Marketplace Operations Management System

A full-stack marketplace platform featuring multi-role e-commerce workflows, incident management, risk analytics, and warehouse inventory operations. Built with a Spring Boot REST API backend and a React TypeScript SPA frontend, all containerized and secured end-to-end with TLS.

## Architecture & Tech Stack

* **Frontend:** React 18, TypeScript, Zustand (state), React Router v6, Axios, Vite
* **Backend:** Java 21, Spring Boot 3.3.5, Spring Security, JPA/Hibernate, Flyway
* **Database:** PostgreSQL 16
* **Proxy:** Nginx (TLS termination, security headers, rate limiting)
* **Containerization:** Docker & Docker Compose (Required)

## Project Structure

```text
.
├── backend/                # Spring Boot source code and Dockerfile
├── frontend/               # React + TypeScript SPA and Dockerfile
├── proxy/                  # Nginx TLS reverse proxy and Dockerfile
├── API_tests/              # JUnit 5 + RestAssured integration tests (86 endpoints)
├── unit_tests/
│   ├── backend/            # Spring Boot service and controller unit tests
│   └── frontend/           # Vitest component, page, and store tests
├── e2e/                    # Playwright browser-level E2E tests
├── docker-compose.yml      # Multi-container orchestration - MANDATORY
├── run_tests.sh            # Standardized test execution script - MANDATORY
└── README.md               # Project documentation - MANDATORY
```

## Prerequisites

To ensure a consistent environment, this project is designed to run entirely within containers. You must have the following installed:

* [Docker](https://docs.docker.com/get-docker/)
* [Docker Compose](https://docs.docker.com/compose/install/)

## Running the Application

1. **Build and Start Containers:**
   Use Docker Compose to build the images and spin up the entire stack in detached mode.
   ```bash
   docker-compose up --build -d
   ```

2. **Access the App:**
   * Frontend + API Proxy: `https://localhost:8443`
   * Backend API: `https://localhost:8443/api`

   > The proxy uses a self-signed TLS certificate. Accept the browser security warning or pass `--insecure` to curl.

3. **Stop the Application:**
   ```bash
   docker-compose down -v
   ```

## Manual Verification

After starting the stack (`docker-compose up --build -d`), confirm the system is working correctly using the following checklist. All requests use the self-signed TLS certificate — pass `-k` to curl to skip certificate validation.

### 1. Authentication — obtain a token

```bash
curl -k -s -X POST https://localhost:8443/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"member","password":"password123"}' | python3 -m json.tool
```

**Expected:** HTTP 200 with a JSON body containing `"token": "<jwt>"` and `"role": "MEMBER"`. Copy the token value for subsequent steps.

```bash
# Store token (replace <jwt> with the value from above)
TOKEN=<jwt>
```

### 2. Public product catalogue (no auth required)

```bash
curl -k -s https://localhost:8443/api/products | python3 -m json.tool | head -30
```

**Expected:** HTTP 200, JSON array with at least one object containing `id`, `name`, and `price` fields.

### 3. Place an order (member role)

```bash
curl -k -s -X POST https://localhost:8443/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":1,"inventoryItemId":1}' | python3 -m json.tool
```

**Expected:** HTTP 200 with a JSON body containing `"id"`, `"status": "PLACED"`, and a server-computed `"totalPrice"` greater than 0.

### 4. RBAC enforcement — member blocked from admin endpoint

```bash
curl -k -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $TOKEN" \
  https://localhost:8443/api/users
```

**Expected:** `403` — member role is not permitted to list all users.

### 5. Admin access — list all users

```bash
ADMIN_TOKEN=$(curl -k -s -X POST https://localhost:8443/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

curl -k -s -H "Authorization: Bearer $ADMIN_TOKEN" \
  https://localhost:8443/api/users | python3 -m json.tool | head -40
```

**Expected:** HTTP 200, JSON array with objects each containing `id`, `username`, and `role` fields.

### 6. UI smoke check

Open `https://localhost:8443` in a browser (accept the self-signed certificate warning).

| Step | Action | Expected outcome |
| :--- | :--- | :--- |
| Login | Enter `member` / `password123` and click **Login** | Redirected to `/dashboard` |
| Orders | Click **Orders** in the nav | Table shows at least the order placed in step 3 with status `PLACED` |
| Incidents | Click **Incidents** in the nav | Page loads with a **Report Incident** button visible |
| Logout | Click **Logout** | Redirected to `/login` |

## Testing

All unit, integration, and E2E tests are executed via a single, standardized shell script. This script automatically handles container orchestration for the test environment.

Make sure the script is executable, then run it:

```bash
chmod +x run_tests.sh
./run_tests.sh
```

The script runs four test suites in order:

| Suite | Tool | Coverage Target |
| :--- | :--- | :--- |
| Backend unit tests | Maven + JaCoCo | ≥ 70% line coverage |
| Frontend unit tests | Vitest + v8 | ≥ 74% lines / 55% branches |
| API integration tests | JUnit 5 + RestAssured | ≥ 90% of 86 endpoints |
| E2E browser tests | Playwright (Chromium) | Core user journeys |

*Note: The `run_tests.sh` script outputs exit code `0` for success and non-zero for failure, integrating smoothly with CI/CD validators.*

## Seeded Credentials

The database is pre-seeded with the following test users on startup. All users share the password `password123`.

| Role | Username | Password | Notes |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `password123` | Full access to all system modules, user management, admin dashboard. |
| **Moderator** | `moderator` | `password123` | Reviews incidents and appeals, accesses moderator dashboard. |
| **Seller** | `seller` | `password123` | Creates products and listings, manages inventory. |
| **Warehouse Staff** | `warehouse` | `password123` | Manages fulfillments, reservations, and inventory adjustments. |
| **Member** | `member` | `password123` | Places orders, reports incidents, submits appeals. |
| **Guest** | `guest` | `password123` | Limited access, used for lockout testing. |
