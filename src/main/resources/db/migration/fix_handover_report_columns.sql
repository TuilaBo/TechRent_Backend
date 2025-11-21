-- Migration script to fix handover_report table columns
-- Run this script BEFORE starting the application

-- Fix status column
UPDATE handover_report 
SET status = 'PENDING_STAFF_SIGNATURE' 
WHERE status IS NULL;

-- Fix staff_signed column
UPDATE handover_report 
SET staff_signed = false 
WHERE staff_signed IS NULL;

-- Fix customer_signed column
UPDATE handover_report 
SET customer_signed = false 
WHERE customer_signed IS NULL;

-- Fix device usage_count column
UPDATE "Device" 
SET usage_count = 0 
WHERE usage_count IS NULL;

