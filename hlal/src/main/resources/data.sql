-- Kosongkan isi tabel dulu
TRUNCATE TABLE transaction_type RESTART IDENTITY CASCADE;
TRUNCATE TABLE top_up_method RESTART IDENTITY CASCADE;

--Set semua balance di tabel wallets jadi 0
UPDATE wallets SET balance = 0;

-- Isi data untuk transaction_type
INSERT INTO transaction_type (id, name) VALUES
(1, 'Top Up'),
(2, 'Transfer');

-- Isi data untuk top_up_method
INSERT INTO top_up_method (id, name) VALUES
(1, 'Virtual Account'),
(2, 'Credit Card'),
(3, 'Debit Card'),
(4, 'Retail / Outlet'),
(5, 'Mobile Banking'),
(6, 'Internet Banking');
