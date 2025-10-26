# Create Contract Table on VPS

## 🚀 Quick Setup

```bash
# SSH to VPS
ssh root@your-vps-ip

# Run SQL script
psql -U postgres -d techrentdb -f create_contract_table.sql

# Or if using techrent user
psql -U techrent -d techrentdb -f create_contract_table.sql
```

## 📝 Manual SQL Commands

```bash
# Connect to database
psql -U postgres -d techrentdb
```

Then run:

```sql
-- Drop existing table
DROP TABLE IF EXISTS contract CASCADE;

-- Create table (copy from create_contract_table.sql)
CREATE TABLE contract (
    contract_id BIGSERIAL PRIMARY KEY,
    contract_number VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    contract_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    customer_id BIGINT NOT NULL,
    order_id BIGINT,
    contract_content TEXT,
    terms_and_conditions TEXT,
    rental_period_days INTEGER,
    total_amount DECIMAL(15,2),
    deposit_amount DECIMAL(15,2),
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    signed_at TIMESTAMP,
    staff_id BIGINT,
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

-- Create indexes
CREATE INDEX idx_contract_customer_id ON contract(customer_id);
CREATE INDEX idx_contract_order_id ON contract(order_id);
CREATE INDEX idx_contract_status ON contract(status);
CREATE INDEX idx_contract_number ON contract(contract_number);
CREATE INDEX idx_contract_kyc_status ON contract(kyc_status);
```

## ✅ Verify Table Creation

```bash
# Check if table exists
psql -U postgres -d techrentdb -c "\d contract"

# Or in psql console
\dt contract
\d contract
```

## 🔧 If Using Hibernate Auto-Update

**On application.properties:**
```properties
spring.jpa.hibernate.ddl-auto=update
```

Hibernate will automatically create the table on app startup.

## 📊 Table Columns Overview

| Column | Type | Description |
|--------|------|-------------|
| `contract_id` | BIGSERIAL | Primary key |
| `contract_number` | VARCHAR(50) | Unique contract number |
| `title` | VARCHAR(200) | Contract title |
| `status` | VARCHAR(50) | Contract status |
| `customer_id` | BIGINT | Customer reference |
| `order_id` | BIGINT | Order reference |
| `total_amount` | DECIMAL(15,2) | Total price |
| `deposit_amount` | DECIMAL(15,2) | Deposit amount |
| `kyc_status` | VARCHAR(50) | KYC verification status |
| ... | ... | ... |

## 🚨 Troubleshooting

### **Error: permission denied**

```bash
# Use postgres superuser
psql -U postgres -d techrentdb

# Or grant permissions
GRANT ALL PRIVILEGES ON TABLE contract TO techrent;
```

### **Error: table already exists**

```sql
-- Drop and recreate
DROP TABLE contract CASCADE;
-- Then run CREATE TABLE again
```

### **Error: database doesn't exist**

```bash
# Create database first
sudo -u postgres psql
CREATE DATABASE techrentdb;
\q
```

## 🔄 Alternative: Let Hibernate Create Table

**application.properties:**
```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.hbm2ddl.auto=update
```

Restart app → Hibernate will auto-create tables.

## 📝 One-Line Command

```bash
psql -U postgres -d techrentdb -c "CREATE TABLE IF NOT EXISTS contract (
    contract_id BIGSERIAL PRIMARY KEY,
    contract_number VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(200) NOT NULL,
    status VARCHAR(50) NOT NULL,
    customer_id BIGINT NOT NULL,
    contract_content TEXT,
    total_amount DECIMAL(15,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);"
```

