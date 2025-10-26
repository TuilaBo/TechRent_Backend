# Quick Fix: Create Contract Table on VPS

## 🚀 Cách nhanh nhất (1 lệnh)

```bash
ssh root@your-vps-ip
```

```bash
psql -U techrent -d techrentdb <<EOF
CREATE TABLE IF NOT EXISTS contract (
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
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    signed_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    kyc_status VARCHAR(50),
    kyc_front_cccd_url VARCHAR(500),
    kyc_back_cccd_url VARCHAR(500),
    kyc_selfie_url VARCHAR(500),
    kyc_verified_at TIMESTAMP,
    kyc_verified_by BIGINT,
    kyc_rejection_reason VARCHAR(1000)
);
EOF
```

## 📝 Step by Step

### Option 1: SSH và chạy SQL

```bash
# 1. SSH vào VPS
ssh root@your-vps-ip

# 2. Connect to database
psql -U techrent -d techrentdb

# 3. Copy và paste SQL
# (SQL từ create_contract_table.sql hoặc quick_fix_contract_table.sql)

# 4. Check
\dt contract
\d contract
```

### Option 2: Upload SQL file

```bash
# From your local machine
scp quick_fix_contract_table.sql root@your-vps-ip:/tmp/

# SSH to VPS
ssh root@your-vps-ip

# Run SQL
psql -U techrent -d techrentdb -f /tmp/quick_fix_contract_table.sql
```

### Option 3: Use postgres user

```bash
ssh root@your-vps-ip
psql -U postgres -d techrentdb

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
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    signed_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    kyc_status VARCHAR(50),
    kyc_front_cccd_url VARCHAR(500),
    kyc_back_cccd_url VARCHAR(500),
    kyc_selfie_url VARCHAR(500),
    kyc_verified_at TIMESTAMP,
    kyc_verified_by BIGINT,
    kyc_rejection_reason VARCHAR(1000)
);
```

## ✅ Verify

```bash
psql -U techrent -d techrentdb -c "\d contract"
```

## 🔧 Fix Hibernate Auto-Update Issue

Nếu vẫn muốn Hibernate tự động tạo, thêm vào `application-prod.properties`:

```properties
# Force table creation
spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop
```

Sau đó restart:

```bash
systemctl restart techrental
```

⚠️ **WARNING:** `create-drop` sẽ **XÓA tất cả data** khi app stop!

**Better option:**
```properties
spring.jpa.properties.hibernate.hbm2ddl.auto=validate
# Run SQL manually instead
```

## 🎯 Recommended: Run SQL manually

1. ✅ Không mất data
2. ✅ Kiểm soát schema
3. ✅ Nhanh và chắc chắn

```bash
psql -U postgres -d techrentdb -f /path/to/quick_fix_contract_table.sql
```

## 📊 Check logs nếu có lỗi

```bash
journalctl -u techrental -n 100 | grep -i "contract\|table\|hibernate"
```

