# Giải Thích Tất Cả Các Lỗi

## 📋 Tổng Quan

Từ đầu đến giờ gặp **3 lỗi chính** liên quan đến Contract table và database schema.

---

## 🔴 **LỖI 1: Missing Column `shipping_address`**

### **Error Message:**
```
ERROR: column "shipping_address" of relation "rental_order" does not exist
```

### **Nguyên Nhân:**
- Entity `RentalOrder` có field `shippingAddress`
- Nhưng database **chưa có** column `shipping_address`
- Hibernate cố INSERT nhưng thiếu column → FAIL

### **Root Cause:**
```java
// RentalOrder.java - Line 27
@Column(name = "shipping_address", length = 500)
private String shippingAddress;
```

Database thiếu column này.

### **Giải Pháp:**
```java
// ✅ Fixed: Đổi table name từ camelCase sang snake_case
@Table(name = "rental_order")  // Thay vì "RentalOrder"
```

**Hoặc chạy SQL:**
```sql
ALTER TABLE rental_order ADD COLUMN shipping_address VARCHAR(500);
```

---

## 🔴 **LỖI 2: Invalid Type "TEXT" và "NTEXT"**

### **Error Message:**
```
ERROR: type "NTEXT" does not exist
ERROR: type "TEXT" does not exist
```

### **Nguyên Nhân:**

#### **A. NTEXT (SQL Server type)**
```java
@Column(name = "contract_content", columnDefinition = "NTEXT")  // ❌ SQL Server
```
- `NTEXT` là kiểu dữ liệu của **SQL Server**
- PostgreSQL **KHÔNG có** kiểu này
- Entity ban đầu được copy từ SQL Server code

#### **B. TEXT (PostgreSQL type nhưng quoted)**
```java
@Column(name = "contract_content", columnDefinition = "TEXT")  // ❌ Quoted
```
- Hibernate generate SQL: `ALTER TABLE ... ADD COLUMN "TEXT"`
- PostgreSQL hiểu `"TEXT"` như một **user-defined type**, không phải built-in type
- → Không tìm thấy type `TEXT`

### **Root Cause:**
Khi dùng `columnDefinition`, Hibernate **quote** tên type:
```sql
-- ❌ Wrong
ALTER TABLE contract ADD COLUMN content "TEXT"

-- ✅ Correct
ALTER TABLE contract ADD COLUMN content TEXT
```

### **Giải Pháp:**
```java
// ✅ Fixed: Không dùng columnDefinition
@Column(name = "contract_content")  // Hibernate tự map String → TEXT
private String contractContent;

// ✅ Hoặc dùng @Lob (nhưng phải cẩn thận với PostgreSQL)
@Lob
@Column(name = "contract_content")
private String contractContent;
```

---

## 🔴 **LỖI 3: Transaction Silently Rolled Back**

### **Error Message:**
```
org.springframework.transaction.UnexpectedRollbackException: 
Transaction silently rolled back because it has been marked as rollback-only
```

### **Nguyên Nhân:**

#### **A. Exception xảy ra trong `@Transactional` method**
```java
@Override
@Transactional
public void run(ApplicationArguments args) {
    try {
        // Create contract
        contractRepository.save(contract);
    } catch (Exception e) {
        log.error("Error: {}", e.getMessage());  // ❌ Chỉ log, không throw
    }
    // Method hoàn thành thành công
}
```

#### **B. Flow gây lỗi:**
```
1. Start transaction (@Transactional)
2. Exception xảy ra (thiếu shipping_address)
3. Transaction được đánh dấu → ROLLBACK
4. catch block bắt exception → chỉ log
5. Method return SUCCESS
6. Spring cố commit transaction
7. Nhưng transaction đã rollback
8. → UnexpectedRollbackException ❌
```

### **Root Cause:**
- **ContractInitializer** tạo sample data khi app start
- Tạo `RentalOrder` không có `shippingAddress`
- Save fail → transaction rollback
- Nhưng exception bị catch → Spring vẫn cố commit
- → Rollback-only exception

### **Giải Pháp:**

#### **Option 1: Fix data (đã làm)**
```java
RentalOrder order = RentalOrder.builder()
    .shippingAddress("123 Đường ABC...")  // ✅ Thêm field
    .build();
```

#### **Option 2: Fix exception handling**
```java
@Override
@Transactional
public void run(ApplicationArguments args) {
    try {
        createContracts();
    } catch (Exception e) {
        log.error("Error", e);
        throw new RuntimeException(e);  // ✅ Re-throw để rollback
    }
}
```

#### **Option 3: Disable initializer (đã làm)**
```java
// @Component  // ✅ Disabled
public class ContractInitializer { ... }
```

---

## 📊 **Timeline Các Lỗi**

```
1. Deploy lên VPS
   ↓
2. MISSING shipping_address column
   → Fix: Đổi table name + update entity
   ↓
3. NTEXT type không tồn tại
   → Fix: Bỏ columnDefinition, dùng @Lob hoặc không dùng gì
   ↓
4. TEXT type bị quote sai
   → Fix: Bỏ @Lob, để Hibernate tự map
   ↓
5. Transaction rollback (local)
   → Fix: Disable ContractInitializer
   ↓
✅ Hoạt động OK
```

---

## 🎯 **Lessons Learned**

### **1. Database Schema Consistency**
- ✅ Luôn dùng **snake_case** cho PostgreSQL
- ✅ Không dùng **camelCase** cho table/column names
- ✅ Kiểm tra entity mapping với database

### **2. Data Type Mapping**
- ✅ Không dùng SQL Server types (NTEXT, NVARCHAR) trong PostgreSQL
- ✅ Tránh dùng `columnDefinition` nếu không cần thiết
- ✅ Để Hibernate tự map: String → TEXT, Long → BIGINT

### **3. Transaction Handling**
- ✅ Không **swallow exceptions** trong transaction
- ✅ Nếu catch exception, phải **re-throw** hoặc rollback
- ✅ Initializer nên có proper error handling

### **4. Development vs Production**
- ✅ Test local trước khi deploy production
- ✅ Schema migration cần được test kỹ
- ✅ Initializer có thể gây lỗi nếu data không hợp lệ

---

## 🔧 **Quick Fix Summary**

| Lỗi | Fix | File |
|-----|-----|------|
| Missing column | Add `shippingAddress` field | `RentalOrder.java` |
| Wrong table name | `@Table(name = "rental_order")` | `RentalOrder.java` |
| NTEXT type | Remove `columnDefinition` | `Contract.java` |
| TEXT quoted | Remove `@Lob`, use default | `Contract.java` |
| Transaction error | Disable `@Component` | `ContractInitializer.java` |

---

## ✅ **Final Working State**

```java
// ✅ RentalOrder.java
@Entity
@Table(name = "rental_order")  // snake_case
public class RentalOrder {
    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;  // Required field
}

// ✅ Contract.java
@Entity
@Table(name = "contract")  // snake_case
public class Contract {
    @Column(name = "contract_content")  // No columnDefinition
    private String contractContent;  // Maps to TEXT
    
    @Column(name = "terms_and_conditions")  // No columnDefinition
    private String termsAndConditions;  // Maps to TEXT
}

// ✅ ContractInitializer.java
// @Component  // Disabled
public class ContractInitializer { ... }
```

---

## 🚀 **Next Steps**

1. ✅ Local app running OK
2. ✅ VPS app running OK
3. ⚠️ Test API endpoints
4. ⚠️ Re-enable ContractInitializer nếu cần
5. ⚠️ Verify database schema consistency

---

## 💡 **Best Practices**

### **Database:**
```sql
-- ✅ Always use snake_case
CREATE TABLE rental_order (...)
CREATE TABLE contract (...)

-- ✅ Use PostgreSQL native types
column_name TEXT  -- Not "NTEXT" or "TEXT" with quotes
```

### **Java Entity:**
```java
// ✅ Good
@Table(name = "rental_order")
@Column(name = "shipping_address")
private String shippingAddress;

// ❌ Bad
@Table(name = "RentalOrder")  // camelCase
@Column(name = "contract_content", columnDefinition = "NTEXT")  // SQL Server type
```

### **Transaction:**
```java
// ✅ Good
@Transactional
public void method() {
    try {
        // business logic
    } catch (Exception e) {
        log.error("Error", e);
        throw new RuntimeException(e);  // Re-throw
    }
}

// ❌ Bad
@Transactional
public void method() {
    try {
        // business logic
    } catch (Exception e) {
        log.error("Error");  // Swallow exception
    }
    // Method succeeds but transaction rollback
}
```

