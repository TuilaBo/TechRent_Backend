# Hướng Dẫn Chạy SQL Trên DBeaver

## 📌 Bước 1: Kết Nối Database

### Tạo Connection Mới:
1. Click **"New Database Connection"** (icon plug)
2. Chọn **PostgreSQL**
3. Điền thông tin:
   ```
   Host: localhost (hoặc VPS IP)
   Port: 5432
   Database: techrentdb
   Username: postgres (hoặc techrent)
   Password: sa
   ```
4. Click **"Test Connection"** → **"Finish"**

## 📝 Bước 2: Mở SQL Editor

### Cách 1: SQL Editor
- Click menu: **Scripts → SQL Editor → New SQL Script**
- Hoặc nhấn: `Ctrl + Alt + ]`

### Cách 2: Mở File SQL
- Click menu: **File → Open SQL Script**
- Chọn file: `dbeaver_contract_table.sql`

## ⚡ Bước 3: Chạy SQL

### Cách 1: Chạy Toàn Bộ Script
1. Paste toàn bộ SQL vào editor
2. Nhấn **Ctrl + Enter** (hoặc `Alt + X`)
3. Hoặc click button **Execute SQL Script** (▶️)

### Cách 2: Chạy Từng Dòng
1. Click vào dòng SQL cần chạy
2. Nhấn **Ctrl + Enter**
3. Kết quả hiển thị ở tab phía dưới

### Cách 3: Chọn Vùng Code
1. Highlight đoạn code cần chạy
2. Nhấn **Ctrl + Enter**

## 🎯 Demo: Tạo Table Contract

### Copy SQL này vào DBeaver:

```sql
-- Drop table if exists
DROP TABLE IF EXISTS contract CASCADE;

-- Create table
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
    rental_period崔 INTEGER,
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
```

Nhấn **Ctrl + Enter** để chạy!

## ✅ Bước 4: Kiểm Tra Kết Quả

### Cách 1: Query
```sql
SELECT * FROM information_schema.tables 
WHERE table_name = 'contract';
```

### Cách 2: View Table
1. Expand database **techrentdb**
2. Expand **Schemas → public → Tables**
3. Tìm table **contract**
4. Right-click → **View Data** hoặc **Edit Data**

## 🎨 Tips & Shortcuts

| Action | Shortcut |
|--------|----------|
| Execute SQL | `Ctrl + Enter` |
| Execute Script | `Alt + X` |
| New SQL Editor | `Ctrl + Alt + ]` |
| Format SQL | `Ctrl + Shift + F` |
| Auto-complete | `Ctrl + Space` |
| Comment line | `Ctrl + /` |

## 🔍 View Table Structure

### Trong DBeaver:
1. Right-click table **contract**
2. Click **"Generate SQL → INSERT"** để xem structure
3. Hoặc **Properties** để xem schema

### Query Structure:
```sql
SELECT 
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'contract';
```

## 📊 Common Queries

### View all columns:
```sql
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'contract';
```

### Count rows:
```sql
SELECT COUNT(*) FROM contract;
```

### View data:
```sql
SELECT * FROM contract LIMIT 10;
```

### View indexes:
```sql
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename = 'contract';
```

## 🚨 Troubleshooting

### Error: Permission denied
→ Switch to `postgres` user hoặc grant quyền

### Error: Table already exists
→ Chạy `DROP TABLE contract CASCADE;` trước

### Error: Cannot connect to database
→ Check:
- PostgreSQL đang chạy
- Credentials đúng
- Firewall/port mở

## 📁 File SQL Reference

Sử dụng file: `dbeaver_contract_table.sql`

