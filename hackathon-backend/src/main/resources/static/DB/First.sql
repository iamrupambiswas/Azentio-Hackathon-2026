

-- 1. Insert Customers from Dataset
INSERT INTO customers (id, full_name, email, national_id_masked, date_of_birth, risk_rating, created_at)
VALUES ('CUST_00001', 'Krishna Sharma', 'krishna.sharma31@gmail.com', 'XXXX-4321', '1985-05-26', 'MEDIUM', '2024-11-26 00:00:00');

INSERT INTO customers (id, full_name, email, national_id_masked, date_of_birth, risk_rating, created_at)
VALUES ('CUST_00002', 'Anika Fernandes', 'anika.fernandes907@outlook.com', 'XXXX-8765', '2008-08-31', 'LOW', '2020-06-15 00:00:00');


-- 2. Insert Accounts Linked to Customers
INSERT INTO accounts (id, customer_id, account_number, account_type, currency, balance, risk_rating, opening_date)
VALUES ('ACC_000001', 'CUST_00001', 'ACC_NUM_ACC_000001', 'NRE', 'INR', 33507.22, 'LOW', '2016-08-26 00:00:00');

INSERT INTO accounts (id, customer_id, account_number, account_type, currency, balance, risk_rating, opening_date)
VALUES ('ACC_000002', 'CUST_00001', 'ACC_NUM_ACC_000002', 'SAVINGS', 'INR', 25171.02, 'LOW', '2019-03-31 00:00:00');

INSERT INTO detection_rules (
    id,
    rule_code,
    enabled,
    threshold_amount,
    window_minutes,
    minimum_transaction_count
)
VALUES (
    gen_random_uuid(),
    'STRUCTURING',
    true,
    50000.00,
    60,
    3
);