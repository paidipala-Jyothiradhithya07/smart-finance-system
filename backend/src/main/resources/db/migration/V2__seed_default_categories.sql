-- =========================================================
-- V2: System default categories (user_id NULL = available to everyone)
-- =========================================================

INSERT INTO categories (user_id, name, type) VALUES
    (NULL, 'Salary',         'INCOME'),
    (NULL, 'Freelancing',    'INCOME'),
    (NULL, 'Business',       'INCOME'),
    (NULL, 'Interest',       'INCOME'),
    (NULL, 'Bonus',          'INCOME'),
    (NULL, 'Food',           'EXPENSE'),
    (NULL, 'Shopping',       'EXPENSE'),
    (NULL, 'Transportation', 'EXPENSE'),
    (NULL, 'Rent',           'EXPENSE'),
    (NULL, 'Bills',          'EXPENSE'),
    (NULL, 'Entertainment',  'EXPENSE'),
    (NULL, 'Healthcare',     'EXPENSE'),
    (NULL, 'Education',      'EXPENSE'),
    (NULL, 'Travel',         'EXPENSE'),
    (NULL, 'EMI',            'EXPENSE'),
    (NULL, 'Other',          'BOTH');