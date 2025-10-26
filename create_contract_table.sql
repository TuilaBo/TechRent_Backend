-- Create contract table for TechRental System

-- Drop table if exists (for fresh start)
DROP TABLE IF EXISTS contract CASCADE;

-- Create contract table
CREATE TABLE contract (
    contract_id BIGSERIAL PRIMARY KEY,
    contract_number VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    contract_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    
    -- Customer info
    customer_id BIGINT NOT NULL,
    order_id BIGINT,
    
    -- Contract content
    contract_content TEXT,
    terms_and_conditions TEXT,
    
    -- Rental details
    rental_period_days INTEGER,
    total_amount DECIMAL(15,2),
    deposit_amount DECIMAL(15,2),
    
    -- Dates
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    
    -- Signature info
    signed_at TIMESTAMP,
    
    -- Staff info
    staff_id BIGINT,
    
    -- Audit fields
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    
    -- KYC fields
    kyc_status VARCHAR(50),
    kyc_front_cccd_url VARCHAR(500),
    kyc_back_cccd_url VARCHAR(500),
    kyc_selfie_url VARCHAR(500),
    kyc_verified_at TIMESTAMP,
    kyc_verified_by BIGINT,
    kyc_rejection_reason VARCHAR(1000)
);

-- Create indexes
CREATE INDEX idx_contract_customer_id ON contract(customer_id);
CREATE INDEX idx_contract_order_id ON contract(order_id);
CREATE INDEX idx_contract_status ON contract(status);
CREATE INDEX idx_contract_number ON contract(contract_number);
CREATE INDEX idx_contract_kyc_status ON contract(kyc_status);

-- Add comments
COMMENT ON TABLE contract IS 'Hợp đồng thuê thiết bị';
COMMENT ON COLUMN contract.contract_id IS 'ID hợp đồng';
COMMENT ON COLUMN contract.contract_number IS 'Số hợp đồng (unique)';
COMMENT ON COLUMN contract.title IS 'Tiêu đề hợp đồng';
COMMENT ON COLUMN contract.description IS 'Mô tả hợp đồng';
COMMENT ON COLUMN contract.contract_type IS 'Loại hợp đồng: EQUIPMENT_RENTAL, etc';
COMMENT ON COLUMN contract.status IS 'Trạng thái: DRAFT, PENDING, SIGNED, EXPIRED, CANCELLED';
COMMENT ON COLUMN contract.customer_id IS 'ID khách hàng';
COMMENT ON COLUMN contract.order_id IS 'ID đơn thuê (nếu có)';
COMMENT ON COLUMN contract.contract_content IS 'Nội dung hợp đồng';
COMMENT ON COLUMN contract.terms_and_conditions IS 'Điều khoản và điều kiện';
COMMENT ON COLUMN contract.rental_period_days IS 'Số ngày thuê';
COMMENT ON COLUMN contract.total_amount IS 'Tổng tiền';
COMMENT ON COLUMN contract.deposit_amount IS 'Tiền đặt cọc';
COMMENT ON COLUMN contract.start_date IS 'Ngày bắt đầu';
COMMENT ON COLUMN contract.end_date IS 'Ngày kết thúc';
COMMENT ON COLUMN contract.expires_at IS 'Ngày hết hạn';
COMMENT ON COLUMN contract.signed_at IS 'Ngày ký';
COMMENT ON COLUMN contract.staff_id IS 'ID nhân viên xử lý';
COMMENT ON COLUMN contract.created_by IS 'ID người tạo';
COMMENT ON COLUMN contract.updated_by IS 'ID người cập nhật';
COMMENT ON COLUMN contract.kyc_status IS 'Trạng thái KYC';
COMMENT ON COLUMN contract.kyc_front_cccd_url IS 'Ảnh mặt trước CCCD';
COMMENT ON COLUMN contract.kyc_back_cccd_url IS 'Ảnh mặt sau CCCD';
COMMENT ON COLUMN contract.kyc_selfie_url IS 'Ảnh selfie';
COMMENT ON COLUMN contract.kyc_verified_at IS 'Ngày xác minh KYC';
COMMENT ON COLUMN contract.kyc_verified_by IS 'ID người xác minh KYC';

-- Grant permissions (adjust user as needed)
-- GRANT ALL PRIVILEGES ON TABLE contract TO techrent;
-- GRANT USAGE, SELECT ON SEQUENCE contract_contract_id_seq TO techrent;

