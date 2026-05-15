INSERT INTO plans (code, name, description, amount_cents, currency, billing_interval, active, created_at, updated_at) VALUES
('starter_monthly',    'Starter',    'For individuals getting started',         999,   'USD', 'MONTH', 1, NOW(6), NOW(6)),
('pro_monthly',        'Pro',        'For growing teams',                       2999,  'USD', 'MONTH', 1, NOW(6), NOW(6)),
('enterprise_monthly', 'Enterprise', 'For organizations with advanced needs',   9999,  'USD', 'MONTH', 1, NOW(6), NOW(6)),
('starter_yearly',     'Starter (Annual)',    'Starter billed yearly, two months free',    9990,  'USD', 'YEAR', 1, NOW(6), NOW(6)),
('pro_yearly',         'Pro (Annual)',        'Pro billed yearly, two months free',        29990, 'USD', 'YEAR', 1, NOW(6), NOW(6)),
('enterprise_yearly',  'Enterprise (Annual)', 'Enterprise billed yearly, two months free', 99990, 'USD', 'YEAR', 1, NOW(6), NOW(6));

-- Demo accounts (password: Password123!). Documented in the README so the app is explorable on first boot.
INSERT INTO users (email, password_hash, name, provider, provider_id, enabled, created_at, updated_at) VALUES
('demo@demo.io',  '$2y$10$yzuDh73P51mS9uTg1HUrJOkPkDTrrZUtYrRF9D30XokbpvnZTEE.W', 'Demo User',  'LOCAL', NULL, 1, NOW(6), NOW(6)),
('admin@demo.io', '$2y$10$iugUYYC6HAWSOCsU8oAClOVY36Ji4gdDIJbAOipGB3uBltgRw1Ate', 'Admin User', 'LOCAL', NULL, 1, NOW(6), NOW(6));

INSERT INTO user_roles (user_id, role) VALUES
((SELECT id FROM users WHERE email = 'demo@demo.io'),  'USER'),
((SELECT id FROM users WHERE email = 'admin@demo.io'), 'USER'),
((SELECT id FROM users WHERE email = 'admin@demo.io'), 'ADMIN');
