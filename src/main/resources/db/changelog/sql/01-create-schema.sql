-- liquibase formatted sql

-- changeset aweem:ca-01 splitStatements:false runOnChange:false
-- comment: Create order_statuses lookup table for managing order status codes and metadata
-- rollback DROP TABLE order_statuses;
CREATE TABLE order_statuses
(
    status_code VARCHAR(50)  NOT NULL,
    description VARCHAR(255) NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_order_statuses PRIMARY KEY (status_code)
);

-- changeset aweem:ca-02 splitStatements:false runOnChange:false
-- comment: Create order_item_statuses lookup table for managing order item status codes
-- rollback DROP TABLE order_item_statuses;
CREATE TABLE order_item_statuses
(
    status_code VARCHAR(50)  NOT NULL,
    description VARCHAR(255) NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_order_item_statuses PRIMARY KEY (status_code)
);

-- changeset aweem:ca-03 splitStatements:false runOnChange:false
-- comment: Create products table for managing retail catalog and inventory
-- rollback DROP TABLE products;
CREATE TABLE products
(
    product_id    UUID         NOT NULL,
    sku           VARCHAR(255) NOT NULL UNIQUE,
    name          VARCHAR(255) NOT NULL,
    description   VARCHAR(255) NOT NULL,
    price         NUMERIC(12, 2) NOT NULL,
    stock_quantity INTEGER      NOT NULL,
    version       INTEGER      DEFAULT 0,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_products PRIMARY KEY (product_id)
);

CREATE INDEX idx_products_sku ON products (sku);

-- changeset aweem:ca-04 splitStatements:false runOnChange:false
-- comment: Create orders table aggregate root for customer orders
-- rollback DROP TABLE orders;
CREATE TABLE orders
(
    order_id       UUID           NOT NULL,
    customer_email VARCHAR(255)   NOT NULL,
    status_code    VARCHAR(50)    NOT NULL,
    currency       VARCHAR(3)     NOT NULL,
    total_amount   NUMERIC(12, 2) NOT NULL,
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_orders PRIMARY KEY (order_id),
    CONSTRAINT fk_order_statuses_code FOREIGN KEY (status_code) REFERENCES order_statuses (status_code)
);

CREATE INDEX idx_orders_customer_email ON orders (customer_email);
CREATE INDEX idx_orders_status_code ON orders (status_code);

-- changeset aweem:ca-05 splitStatements:false runOnChange:false
-- comment: Create order_items table for items within each order
-- rollback DROP TABLE order_items;
CREATE TABLE order_items
(
    order_item_id  UUID           NOT NULL,
    order_id       UUID           NOT NULL,
    product_id     UUID           NOT NULL,
    quantity       INTEGER        NOT NULL,
    price          NUMERIC(12, 2) NOT NULL,
    status_code    VARCHAR(50)    NOT NULL,
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_order_items PRIMARY KEY (order_item_id),
    CONSTRAINT fk_order_items_parent FOREIGN KEY (order_id) REFERENCES orders (order_id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (product_id),
    CONSTRAINT fk_order_items_status FOREIGN KEY (status_code) REFERENCES order_item_statuses (status_code)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);
CREATE INDEX idx_order_items_status_code ON order_items (status_code);

-- changeset aweem:ca-06 splitStatements:false runOnChange:false
-- comment: Create update_status_restriction table for managing permissible workflow state transitions
-- rollback DROP TABLE update_status_restriction;
CREATE TABLE update_status_restriction
(
    update_status_restriction_id INTEGER    NOT NULL GENERATED ALWAYS AS IDENTITY,
    current_status               VARCHAR(50) NOT NULL,
    allowed_next_status          VARCHAR(50) NOT NULL,
    is_active                    BOOLEAN    NOT NULL DEFAULT TRUE,
    created_at                   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_update_status_restriction PRIMARY KEY (update_status_restriction_id),
    CONSTRAINT uq_transition_rule UNIQUE (current_status, allowed_next_status)
);

-- changeset aweem:ca-07 splitStatements:false runOnChange:false
-- comment: Create outbox_events table for event sourcing and outbox pattern implementation
-- rollback DROP TABLE outbox_events;
CREATE TABLE outbox_events
(
    outbox_event_id UUID         NOT NULL,
    aggregate_id    VARCHAR(255) NOT NULL,
    aggregate_type  VARCHAR(255) NOT NULL,
    event_type      VARCHAR(255) NOT NULL,
    payload         JSONB        NOT NULL,
    processed       BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_outbox_events PRIMARY KEY (outbox_event_id)
);

CREATE INDEX idx_outbox_events_aggregate_id ON outbox_events (aggregate_id);
CREATE INDEX idx_outbox_events_aggregate_type ON outbox_events (aggregate_type);
CREATE INDEX idx_outbox_events_processed ON outbox_events (processed);
CREATE INDEX idx_outbox_events_created_at ON outbox_events (created_at);

-- changeset aweem:ca-12 splitStatements:false runOnChange:false
-- comment: Add idempotency key to orders for duplicate create-order protection.
-- rollback ALTER TABLE orders DROP COLUMN idempotency_key;
ALTER TABLE orders
    ADD COLUMN idempotency_key VARCHAR(120);

CREATE UNIQUE INDEX uq_orders_idempotency_key ON orders (idempotency_key);

-- changeset aweem:ca-14 splitStatements:false runOnChange:false
-- comment: Add outbox retry metadata for resilient event publishing.
-- rollback ALTER TABLE outbox_events DROP COLUMN retry_count; ALTER TABLE outbox_events DROP COLUMN last_error;
ALTER TABLE outbox_events
    ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE outbox_events
    ADD COLUMN last_error VARCHAR(1024);

