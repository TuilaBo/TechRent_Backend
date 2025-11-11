-- Add frontend URLs columns to invoice table for VNPAY payment redirect
ALTER TABLE invoice 
ADD COLUMN IF NOT EXISTS frontend_success_url VARCHAR(500),
ADD COLUMN IF NOT EXISTS frontend_failure_url VARCHAR(500);

