-- Fix contract table in DBeaver
-- Chạy SQL này để xóa bảng cũ và tạo lại

-- Step 1: Xóa bảng cũ
DROP TABLE IF EXISTS contract CASCADE;

-- Step 2: Tạo bảng mới
CREATE TABLE contract (
    contract_id BIGSERIAL PRIMARY KEY,
    contract_number VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    contract_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    customer_id BIGINT NOT NULL,
    order_id BIGINT,
    staff_id BIGINT,
    contract_content TEXT,
    terms_and_conditions TEXT,
    rental_period_days INTEGER,
    total_amount DECIMAL(15,2),
    deposit_amount DECIMAL(15,2),
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    expires_at TIMESTAMP,
    signed_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    kyc_status VARCHAR(50),
   充值_front_cccd_url VARCHAR(500),
    kyc_back_cccd_url VARCHAR(500),
    kyc_selfie_url VARCHAR(500),
    kyc_verified_at TIMESTAMP,
    kyc_verified_by BIGINT,
    kyc_rejection_reason VARCHAR(1000)
);

-- Step 3: Tạo indexes
CREATE INDEX idx_contract_customer_id ON contract(customer_id);
CREATE INDEX idx_contract_order_id ON contract(order_id);
CREATE INDEX idx_contract_status ON contract(status);
CREATE INDEX idx_contract_number ON contract(contract_number);

-- Step 4: Verify
SELECT * FROM information_schema.tables WHERE table_name = 'contract';

