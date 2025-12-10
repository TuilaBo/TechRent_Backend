-- Migration: Make Task.order_id nullable
-- Date: 2025-01-XX
-- Description: Allow creating tasks without orderId (standalone tasks)
-- 
-- IMPORTANT: 
-- 1. Backup database before running
-- 2. Test on staging first
-- 3. Run during maintenance window
-- 4. All existing tasks have orderId, so this is safe

-- Check current constraint (run this first to verify)
-- SELECT column_name, is_nullable, data_type 
-- FROM information_schema.columns 
-- WHERE table_name = 'Task' AND column_name = 'order_id';

-- Make order_id nullable
ALTER TABLE "Task" ALTER COLUMN order_id DROP NOT NULL;

-- Verify (run after migration)
-- SELECT column_name, is_nullable 
-- FROM information_schema.columns 
-- WHERE table_name = 'Task' AND column_name = 'order_id';
-- Expected: is_nullable = 'YES'


