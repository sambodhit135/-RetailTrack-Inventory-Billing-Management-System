-- ============================================================
--  RetailTrack — Inventory & Billing Management System
--  Database Schema
--  Generated: 2026-05-18
-- ============================================================

-- Drop tables in reverse dependency order for clean re-runs
DROP TABLE IF EXISTS reorder_requests;
DROP TABLE IF EXISTS stock_audit;
DROP TABLE IF EXISTS invoices;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS coupons;
DROP TABLE IF EXISTS product_suppliers;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS suppliers;
DROP TABLE IF EXISTS categories;

-- ============================================================
-- Table: categories
-- ============================================================
CREATE TABLE categories (
    id          INT             NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)    NOT NULL,
    gst_slab    DECIMAL(5, 2)   NOT NULL COMMENT 'GST percentage e.g. 5.00, 12.00, 18.00',
    description TEXT,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Table: suppliers
-- ============================================================
CREATE TABLE suppliers (
    id              INT             NOT NULL AUTO_INCREMENT,
    name            VARCHAR(150)    NOT NULL,
    email           VARCHAR(150)    NOT NULL,
    phone           VARCHAR(20),
    address         TEXT,
    lead_time_days  INT             NOT NULL DEFAULT 1 COMMENT 'Number of days supplier takes to deliver',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Table: products
-- ============================================================
CREATE TABLE products (
    id                  INT             NOT NULL AUTO_INCREMENT,
    name                VARCHAR(200)    NOT NULL,
    description         TEXT,
    price               DECIMAL(10, 2)  NOT NULL,
    stock_quantity      INT             NOT NULL DEFAULT 0,
    reorder_threshold   INT             NOT NULL DEFAULT 10 COMMENT 'Minimum stock before reorder alert is triggered',
    avg_daily_sales     DECIMAL(10, 2)  NOT NULL DEFAULT 0.00 COMMENT 'Estimated units sold per day',
    category_id         INT             NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_category
        FOREIGN KEY (category_id) REFERENCES categories (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Table: product_suppliers  (many-to-many join table)
-- ============================================================
CREATE TABLE product_suppliers (
    product_id  INT         NOT NULL,
    supplier_id INT         NOT NULL,
    is_primary  BOOLEAN     NOT NULL DEFAULT FALSE,
    PRIMARY KEY (product_id, supplier_id),
    CONSTRAINT fk_ps_product
        FOREIGN KEY (product_id)  REFERENCES products  (id) ON DELETE CASCADE,
    CONSTRAINT fk_ps_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Table: coupons
-- ============================================================
CREATE TABLE coupons (
    id              INT             NOT NULL AUTO_INCREMENT,
    code            VARCHAR(50)     NOT NULL,
    discount_type   ENUM('PERCENTAGE', 'FLAT') NOT NULL,
    discount_value  DECIMAL(10, 2)  NOT NULL,
    min_cart_value  DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    max_uses        INT             NOT NULL DEFAULT 1,
    used_count      INT             NOT NULL DEFAULT 0,
    expiry_date     DATE,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_coupon_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Table: orders
-- ============================================================
CREATE TABLE orders (
    id              INT             NOT NULL AUTO_INCREMENT,
    total_amount    DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    gst_amount      DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    grand_total     DECIMAL(12, 2)  NOT NULL DEFAULT 0.00,
    status          ENUM('PENDING', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Table: order_items
-- ============================================================
CREATE TABLE order_items (
    id          INT             NOT NULL AUTO_INCREMENT,
    order_id    INT             NOT NULL,
    product_id  INT             NOT NULL,
    quantity    INT             NOT NULL,
    unit_price  DECIMAL(10, 2)  NOT NULL,
    gst_amount  DECIMAL(10, 2)  NOT NULL DEFAULT 0.00,
    subtotal    DECIMAL(12, 2)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_oi_order
        FOREIGN KEY (order_id)   REFERENCES orders   (id) ON DELETE CASCADE,
    CONSTRAINT fk_oi_product
        FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Table: invoices
-- ============================================================
CREATE TABLE invoices (
    id              INT             NOT NULL AUTO_INCREMENT,
    order_id        INT             NOT NULL,
    invoice_number  VARCHAR(50)     NOT NULL,
    generated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_invoice_order  UNIQUE (order_id),
    CONSTRAINT uq_invoice_number UNIQUE (invoice_number),
    CONSTRAINT fk_invoice_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Table: stock_audit
-- ============================================================
CREATE TABLE stock_audit (
    id              INT             NOT NULL AUTO_INCREMENT,
    product_id      INT             NOT NULL,
    event_type      ENUM('SALE', 'RESTOCK', 'ADJUSTMENT') NOT NULL,
    quantity_change INT             NOT NULL COMMENT 'Negative for SALE, positive for RESTOCK/ADJUSTMENT',
    stock_before    INT             NOT NULL,
    stock_after     INT             NOT NULL,
    remarks         VARCHAR(255),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_audit_product
        FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Table: reorder_requests
-- ============================================================
CREATE TABLE reorder_requests (
    id                  INT     NOT NULL AUTO_INCREMENT,
    product_id          INT     NOT NULL,
    supplier_id         INT     NOT NULL,
    requested_quantity  INT     NOT NULL,
    status              ENUM('PENDING', 'SENT', 'FULFILLED') NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_rr_product
        FOREIGN KEY (product_id)  REFERENCES products  (id) ON DELETE CASCADE,
    CONSTRAINT fk_rr_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================
--  Sample Data — categories (6 rows)
-- ============================================================
INSERT INTO categories (name, gst_slab, description) VALUES
    ('Beverages',        18.00, 'Soft drinks, juices, energy drinks and bottled water'),
    ('Snacks & Bakery',   5.00, 'Chips, biscuits, cookies and packaged bakery items'),
    ('Dairy & Eggs',      5.00, 'Milk, butter, cheese, paneer and fresh eggs'),
    ('Personal Care',    18.00, 'Soaps, shampoos, toothpaste and hygiene products'),
    ('Household Goods',  12.00, 'Cleaning supplies, detergents and kitchen essentials'),
    ('Staples & Grains',  0.00, 'Rice, wheat flour, pulses and other essential grains');

-- ============================================================
--  Sample Data — suppliers (5 rows)
-- ============================================================
INSERT INTO suppliers (name, email, phone, address, lead_time_days) VALUES
    ('Reliance Retail Distributors',  'supply@reliancedist.in',   '+91-9800001001', '14, MIDC Industrial Area, Mumbai, Maharashtra 400093',  2),
    ('Agro Fresh Pvt. Ltd.',          'orders@agrofresh.co.in',   '+91-9800002002', '88, Pune-Nashik Highway, Pune, Maharashtra 411019',      3),
    ('HUL Trade Partners',            'trade@hul-partner.in',     '+91-9800003003', '5th Floor, Nirlon Complex, Goregaon, Mumbai 400063',     1),
    ('Metro Cash & Carry India',      'b2b@metro-cc.in',          '+91-9800004004', 'Plot 12, Sector 35, Gurugram, Haryana 122001',           4),
    ('Sri Venkatesh Wholesale',       'info@srivenkatesh.com',    '+91-9800005005', '23, Gandhi Nagar Market, Bangalore, Karnataka 560009',   5);
