-- Rollback: Revert Task.order_id to NOT NULL
-- WARNING: Only run if you need to rollback
-- This will fail if there are any tasks with NULL orderId

-- First, delete or update any tasks with NULL orderId
-- DELETE FROM "Task" WHERE order_id IS NULL;
-- OR update them to have an orderId:
-- UPDATE "Task" SET order_id = <some_order_id> WHERE order_id IS NULL;

-- Then make it NOT NULL again
ALTER TABLE "Task" ALTER COLUMN order_id SET NOT NULL;


