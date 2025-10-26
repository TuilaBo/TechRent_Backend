-- SQL script to add KYC fields to Customer table

ALTER TABLE customer 
ADD COLUMN IF NOT EXISTS kyc_status VARCHAR(50) DEFAULT 'NOT_STARTED',
ADD COLUMN IF NOT EXISTS kyc_front_cccd_url VARCHAR(500),
ADD COLUMN IF NOT EXISTS kyc_back_cccd_url VARCHAR(500),
ADD COLUMN IF NOT EXISTS kyc_selfie_url VARCHAR(500),
ADD COLUMN IF NOT EXISTS kyc_verified_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS kyc_verified_by BIGINT,
ADD COLUMN IF NOT EXISTS kyc_rejection_reason VARCHAR(1000);

-- Add comments
COMMENT ON COLUMN customer.kyc_status IS 'Trạng thái KYC: NOT_STARTED, PENDING_VERIFICATION, DOCUMENTS_SUBMITTED, VERIFIED, REJECTED, EXPIRED';
COMMENT ON COLUMN customer.kyc_front_cccd_url IS 'URL ảnh mặt trước CCCD';
COMMENT ON COLUMN customer.kyc_back_cccd_url IS 'URL ảnh mặt sau CCCD';
COMMENT ON COLUMN customer.kyc_selfie_url IS 'URL ảnh selfie';
COMMENT ON COLUMN customer.kyc_verified_at IS 'Ngày xác minh KYC';
COMMENT ON COLUMN customer.kyc_verified_by IS 'ID operator xác minh';
COMMENT ON COLUMN customer.kyc_rejection_reason IS 'Lý do từ chối KYC';

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_customer_kyc_status ON customer(kyc_status);
CREATE INDEX IF NOT EXISTS idx_customer_kyc_verified_by ON customer(kyc_verified_by);

