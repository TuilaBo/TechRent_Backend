-- Fix invoice_payment_method_check constraint to include VNPAY
-- Run this script in your PostgreSQL database

-- Step 1: Drop the existing constraint
ALTER TABLE invoice DROP CONSTRAINT IF EXISTS invoice_payment_method_check;

-- Step 2: Add new constraint that includes VNPAY
ALTER TABLE invoice ADD CONSTRAINT invoice_payment_method_check 
    CHECK (payment_method IN ('PAYOS', 'VNPAY', 'MOMO', 'BANK_ACCOUNT'));

