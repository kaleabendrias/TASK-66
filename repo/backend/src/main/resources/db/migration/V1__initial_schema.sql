-- Base schema: users, categories, products, orders
CREATE TABLE IF NOT EXISTS app_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    email           VARCHAR(120) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    role            VARCHAR(30)  NOT NULL DEFAULT 'GUEST',
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS category (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS product (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    price           NUMERIC(12,2) NOT NULL,
    stock_quantity  INTEGER       NOT NULL DEFAULT 0,
    category_id     BIGINT REFERENCES category(id),
    seller_id       BIGINT REFERENCES app_user(id),
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS product_order (
    id              BIGSERIAL PRIMARY KEY,
    buyer_id        BIGINT       NOT NULL REFERENCES app_user(id),
    product_id      BIGINT       NOT NULL REFERENCES product(id),
    quantity        INTEGER      NOT NULL,
    total_price     NUMERIC(12,2) NOT NULL,
    status          VARCHAR(30)  NOT NULL DEFAULT 'PLACED',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);
