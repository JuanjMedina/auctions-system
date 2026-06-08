CREATE TABLE categories
(
    id        UUID         PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    slug      VARCHAR(100) NOT NULL UNIQUE,
    parent_id UUID         REFERENCES categories (id) ON DELETE SET NULL,
    is_active BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_categories_slug      ON categories (slug);
CREATE INDEX idx_categories_parent_id ON categories (parent_id);
CREATE INDEX idx_categories_active    ON categories (is_active) WHERE is_active = TRUE;
