-- =============================================================================
-- SEED DE DESARROLLO — solo para entornos locales/test
-- Contraseña de todos los usuarios: Password123!
-- Hash BCrypt(10): $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- =============================================================================

-- -----------------------------------------------------------------------------
-- USERS
-- IDs predecibles para facilitar las pruebas
-- -----------------------------------------------------------------------------
INSERT INTO users (id, email, username, password_hash, full_name, phone, role, is_active)
VALUES
    ('a0000000-0000-0000-0000-000000000001', 'admin@subastapp.com',   'admin',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Administrador',    NULL,          'ADMIN'::user_role,  TRUE),
    ('a0000000-0000-0000-0000-000000000002', 'seller1@subastapp.com', 'seller1',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Carlos Vendedor',  '+573001112233', 'SELLER'::user_role, TRUE),
    ('a0000000-0000-0000-0000-000000000003', 'seller2@subastapp.com', 'seller2',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Ana Vendedora',    '+573009998877', 'SELLER'::user_role, TRUE),
    ('a0000000-0000-0000-0000-000000000004', 'buyer1@subastapp.com',  'buyer1',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Pedro Comprador',  '+573115556677', 'BUYER'::user_role,  TRUE),
    ('a0000000-0000-0000-0000-000000000005', 'buyer2@subastapp.com',  'buyer2',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'María Compradora', '+573124445566', 'BUYER'::user_role,  TRUE),
    ('a0000000-0000-0000-0000-000000000006', 'buyer3@subastapp.com',  'buyer3',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Luis Pujador',     NULL,           'BUYER'::user_role,  TRUE);

-- -----------------------------------------------------------------------------
-- CATEGORIES
-- Electrónica (padre) → Smartphones, Laptops (hijos)
-- Arte y Moda (padres sin hijos)
-- -----------------------------------------------------------------------------
INSERT INTO categories (id, name, slug, parent_id, is_active)
VALUES
    ('c0000000-0000-0000-0000-000000000001', 'Electrónica', 'electronica', NULL, TRUE),
    ('c0000000-0000-0000-0000-000000000002', 'Smartphones', 'smartphones', 'c0000000-0000-0000-0000-000000000001', TRUE),
    ('c0000000-0000-0000-0000-000000000003', 'Laptops',     'laptops',     'c0000000-0000-0000-0000-000000000001', TRUE),
    ('c0000000-0000-0000-0000-000000000004', 'Arte',        'arte',        NULL, TRUE),
    ('c0000000-0000-0000-0000-000000000005', 'Moda',        'moda',        NULL, TRUE);

-- -----------------------------------------------------------------------------
-- WALLETS
-- seller1 recibió payout de subasta ganada: 500 + 150 = 650
-- buyer1  pagó subasta ganada: 1000 - 150 = 850
-- buyer2  tiene reserva activa de 200 (puja ganadora en subasta activa)
-- -----------------------------------------------------------------------------
INSERT INTO wallets (id, user_id, balance, reserved_balance, currency, version)
VALUES
    ('w0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001',    0.0000,   0.0000, 'USD', 0),
    ('w0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002',  650.0000,   0.0000, 'USD', 2),
    ('w0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000003',  500.0000,   0.0000, 'USD', 0),
    ('w0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000004',  850.0000,   0.0000, 'USD', 1),
    ('w0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000005', 1000.0000, 200.0000, 'USD', 1),
    ('w0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000006', 1000.0000,   0.0000, 'USD', 0);

-- Historial de transacciones representativo
INSERT INTO wallet_transactions (id, wallet_id, reference_id, type, amount, balance_after, description, created_at)
VALUES
    -- seller1 recibió payout de la subasta ganada
    ('t0000000-0000-0000-0000-000000000001', 'w0000000-0000-0000-0000-000000000002',
     'e0000000-0000-0000-0000-000000000005', 'AUCTION_PAYOUT', 150.0000, 650.0000,
     'Auction payout received', NOW() - INTERVAL '2 hours'),

    -- buyer1 depósito inicial
    ('t0000000-0000-0000-0000-000000000002', 'w0000000-0000-0000-0000-000000000004',
     NULL, 'DEPOSIT', 1000.0000, 1000.0000,
     'Depósito inicial', NOW() - INTERVAL '3 days'),

    -- buyer1 fue cobrado al ganar la subasta
    ('t0000000-0000-0000-0000-000000000003', 'w0000000-0000-0000-0000-000000000004',
     'e0000000-0000-0000-0000-000000000005', 'AUCTION_CHARGE', 150.0000, 850.0000,
     'Auction winning charge', NOW() - INTERVAL '2 hours'),

    -- buyer2 depósito inicial
    ('t0000000-0000-0000-0000-000000000004', 'w0000000-0000-0000-0000-000000000005',
     NULL, 'DEPOSIT', 1000.0000, 1000.0000,
     'Depósito inicial', NOW() - INTERVAL '3 days'),

    -- buyer2 reserva activa por puja ganadora en subasta activa
    ('t0000000-0000-0000-0000-000000000005', 'w0000000-0000-0000-0000-000000000005',
     'b0000000-0000-0000-0000-000000000002', 'BID_RESERVE', 200.0000, 1000.0000,
     'Funds reserved for bid', NOW() - INTERVAL '30 minutes');

-- -----------------------------------------------------------------------------
-- AUCTIONS — 7 subastas en distintos estados
-- -----------------------------------------------------------------------------
INSERT INTO auctions (id, seller_id, category_id, title, description, starting_price,
                      reserve_price, current_price, current_winner_id, status,
                      auto_extend, extend_minutes, starts_at, ends_at, closed_at, version)
VALUES
    -- 1. DRAFT — sin publicar
    ('e0000000-0000-0000-0000-000000000001',
     'a0000000-0000-0000-0000-000000000002',
     'c0000000-0000-0000-0000-000000000002',
     'iPhone 15 Pro - Sin Publicar', 'iPhone 15 Pro 256GB Titanio Negro, caja sellada.',
     800.0000, 1000.0000, 800.0000, NULL, 'DRAFT',
     FALSE, 5, NOW() + INTERVAL '2 days', NOW() + INTERVAL '9 days', NULL, 0),

    -- 2. SCHEDULED — publicada, empieza mañana
    ('e0000000-0000-0000-0000-000000000002',
     'a0000000-0000-0000-0000-000000000002',
     'c0000000-0000-0000-0000-000000000003',
     'MacBook Pro M3 — Programada', 'MacBook Pro 14" M3 Pro, 18GB RAM, 512GB SSD.',
     1500.0000, 2000.0000, 1500.0000, NULL, 'SCHEDULED',
     TRUE, 10, NOW() + INTERVAL '1 day', NOW() + INTERVAL '8 days', NULL, 0),

    -- 3. ACTIVE — activa, sin pujas aún
    ('e0000000-0000-0000-0000-000000000003',
     'a0000000-0000-0000-0000-000000000003',
     'c0000000-0000-0000-0000-000000000004',
     'Cuadro Original — Óleo sobre Lienzo', 'Pintura original al óleo 60x80cm firmada por el artista.',
     200.0000, NULL, 200.0000, NULL, 'ACTIVE',
     FALSE, 5, NOW() - INTERVAL '1 hour', NOW() + INTERVAL '6 days', NULL, 0),

    -- 4. ACTIVE — activa con pujas (buyer2 ganando con $200)
    ('e0000000-0000-0000-0000-000000000004',
     'a0000000-0000-0000-0000-000000000002',
     'c0000000-0000-0000-0000-000000000002',
     'Samsung Galaxy S24 Ultra', 'Samsung Galaxy S24 Ultra 512GB Phantom Black, libre de fábrica.',
     100.0000, 180.0000, 200.0000, 'a0000000-0000-0000-0000-000000000005', 'ACTIVE',
     TRUE, 5, NOW() - INTERVAL '2 hours', NOW() + INTERVAL '5 days', NULL, 2),

    -- 5. AWARDED — cerrada con ganador (buyer1 ganó a $150)
    ('e0000000-0000-0000-0000-000000000005',
     'a0000000-0000-0000-0000-000000000002',
     'c0000000-0000-0000-0000-000000000005',
     'Chaqueta Vintage Levi''s 90s', 'Chaqueta de jean Levi''s original de los 90s, talla M, excelente estado.',
     80.0000, 120.0000, 150.0000, 'a0000000-0000-0000-0000-000000000004', 'AWARDED',
     FALSE, 5, NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours', 3),

    -- 6. FAILED — cerrada sin ganador (precio de reserva no alcanzado)
    ('e0000000-0000-0000-0000-000000000006',
     'a0000000-0000-0000-0000-000000000003',
     'c0000000-0000-0000-0000-000000000001',
     'Consola PS5 Digital Edition', 'PlayStation 5 Digital Edition, con 2 mandos, en caja.',
     300.0000, 400.0000, 300.0000, NULL, 'FAILED',
     FALSE, 5, NOW() - INTERVAL '7 days', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', 1),

    -- 7. CANCELLED — cancelada por el vendedor
    ('e0000000-0000-0000-0000-000000000007',
     'a0000000-0000-0000-0000-000000000003',
     'c0000000-0000-0000-0000-000000000001',
     'Tablet Lenovo Tab P12', 'Lenovo Tab P12 12.7" 256GB, con teclado y stylus.',
     150.0000, NULL, 150.0000, NULL, 'CANCELLED',
     FALSE, 5, NOW() + INTERVAL '1 day', NOW() + INTERVAL '8 days', NULL, 0);

-- -----------------------------------------------------------------------------
-- BIDS — pujas sobre las subastas activa y awarded
-- -----------------------------------------------------------------------------
INSERT INTO bids (id, auction_id, bidder_id, amount, is_auto_bid, max_auto_amount, status, created_at)
VALUES
    -- Subasta activa (e...004): buyer1 pujó primero a $150 → fue superado (OUTBID)
    ('b0000000-0000-0000-0000-000000000001',
     'e0000000-0000-0000-0000-000000000004',
     'a0000000-0000-0000-0000-000000000004',
     150.0000, FALSE, NULL, 'OUTBID', NOW() - INTERVAL '90 minutes'),

    -- Subasta activa (e...004): buyer2 pujó $200 → ganando actualmente (ACTIVE)
    ('b0000000-0000-0000-0000-000000000002',
     'e0000000-0000-0000-0000-000000000004',
     'a0000000-0000-0000-0000-000000000005',
     200.0000, FALSE, NULL, 'ACTIVE', NOW() - INTERVAL '30 minutes'),

    -- Subasta awarded (e...005): buyer1 ganó a $150 (WINNING)
    ('b0000000-0000-0000-0000-000000000003',
     'e0000000-0000-0000-0000-000000000005',
     'a0000000-0000-0000-0000-000000000004',
     150.0000, FALSE, NULL, 'WINNING', NOW() - INTERVAL '3 days');
