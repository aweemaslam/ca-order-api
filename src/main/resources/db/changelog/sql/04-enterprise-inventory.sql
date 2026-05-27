-- liquibase formatted sql

-- changeset aweem:ca-12 splitStatements:false runOnChange:false
-- comment: Add idempotency key to orders for duplicate create-order protection.
-- rollback ALTER TABLE orders DROP COLUMN idempotency_key;
ALTER TABLE orders
    ADD COLUMN idempotency_key VARCHAR(120);

CREATE UNIQUE INDEX uq_orders_idempotency_key ON orders (idempotency_key);

-- changeset aweem:ca-13 splitStatements:false runOnChange:false
-- comment: Create inventory reservations table for reserve-confirm-release stock workflow.
-- rollback DROP TABLE inventory_reservations;
CREATE TABLE inventory_reservations
(
    reservation_id   UUID         NOT NULL,
    order_id         UUID         NOT NULL,
    product_id       UUID         NOT NULL,
    quantity         INTEGER      NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    expires_at       TIMESTAMP    NOT NULL,
    idempotency_key  VARCHAR(120),
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_inventory_reservations PRIMARY KEY (reservation_id),
    CONSTRAINT fk_inventory_res_order FOREIGN KEY (order_id) REFERENCES orders (order_id),
    CONSTRAINT fk_inventory_res_product FOREIGN KEY (product_id) REFERENCES products (product_id)
);

CREATE INDEX idx_inventory_res_order_status ON inventory_reservations (order_id, status);
CREATE INDEX idx_inventory_res_expires_at ON inventory_reservations (expires_at);
CREATE INDEX idx_inventory_res_idempotency_key ON inventory_reservations (idempotency_key);

-- changeset aweem:ca-14 splitStatements:false runOnChange:false
-- comment: Add outbox retry metadata for resilient event publishing.
-- rollback ALTER TABLE outbox_events DROP COLUMN retry_count; ALTER TABLE outbox_events DROP COLUMN last_error;
ALTER TABLE outbox_events
    ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE outbox_events
    ADD COLUMN last_error VARCHAR(1024);

