# Fix Transaction Error - Contract Table

## 🔴 Error

```
Transaction silently rolled back because it has been marked as rollback-only
```

## ⚠️ Nguyên Nhân

Contract table có vấn đề với schema hoặc data.

## ✅ Solution: Xóa và tạo lại table

### **Step 1: Xóa table cũ**

Chạy trong **DBeaver** hoặc psql:

```sql
DROP TABLE IF EXISTS contract CASCADE;
```

### **Step 2: Tạo table mới (chạy trong DBeaver)**

```sql
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
    kyc_front_ává_url VARCHAR(500),
    kyc_back_cccd_url VARCHAR(500),
    kyc_selfie_url VARCHAR(500),
    kyc_verified_at TIMESTAMP,
    kyc_verified_by BIGINT,
    kyc_rejection_reason VARCHAR(1000)
);

CREATE INDEX idx_contract_customer_id ON contract(customer_id);
CREATE INDEX idx_contract_order_id ON contract(order_id);
CREATE INDEX idx_contract_status ON contract(status);
CREATE INDEX idx_contract_number ON contract(contract_number);
```

### **Step 3: Rebuild và Deploy**

```bash
# Build
mvn clean package -DskipTests

# Deploy to VPS
scp target/techrental-0.0.1-SNAPSHOT.jar root@your-vps:/var/apps/techrental/

# Restart
ssh root@your-vps "systemctl restart techrental"
```

## 🔍 Alternative: Disable Hibernate Auto-Update

Nếu vẫn lỗi, disable auto-update:

**application-prod.properties:**
```properties
# Disable auto-update
spring.jpa.hibernate.ddl-auto=validate
```

Sau đó chạy SQL thủ công.

## 🎯 Quick Debug Query

```sql
-- Check table structure
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'contract';

-- Check constraints
SELECT conname, contype, conrelid::regclass
FROM pg_constraint
WHERE conrelid = 'contract'::regclass;
```

