# Fix Transaction Rollback Error

## 🔴 Error

```
org.springframework.transaction.UnexpectedRollbackException: 
Transaction silently rolled back because it has been marked as rollback-only
```

## 📋 Nguyên Nhân

1. **Exception trong transaction** không được handle
2. Transaction bị đánh dấu `rollback-only`
3. Code cố commit sau khi đã rollback

## 🔧 Giải Pháp

### **Solution 1: Catch và handle exception**

```java
@Transactional
public void createContract(...) {
    try {
        // Business logic
        contractRepository.save(contract);
    } catch (Exception e) {
        // Log error
        throw new RuntimeException("Error: " + e.getMessage(), e);
    }
}
```

### **Solution 2: Sử dụng @Transactional(noRollbackFor)**

```java
@Transactional(noRollbackFor = {SpecificException.class})
public void method() {
    // Won't rollback for SpecificException
}
```

### **Solution 3: Kiểm tra constraint violations**

```java
// Check if contract_number already exists
if (contractRepository.existsByContractNumber(number)) {
    throw new RuntimeException("Contract number already exists");
}
```

## 🎯 Quick Fix

### **Step 1: Xem log đầy đủ**

Kiểm tra stack trace để tìm vị trí chính xác:

```bash
# Check application logs
journalctl -u techrental -n 100

# Or view log file
tail -f /var/log/techrentsystem/application.log
```

### **Step 2: Common fixes**

#### **A. Foreign Key Constraint**

```sql
-- Check if customer_id exists
SELECT * FROM customer WHERE customer_id = ?;

-- Check if order_id exists
SELECT * FROM rental_order WHERE order_id = ?;
```

#### **B. Unique Constraint**

```sql
-- Check duplicate contract_number
SELECT * FROM contract WHERE contract_number = ?;
```

#### **C. Not Null Constraint**

Kiểm tra các field required không được null.

## 🔍 Debug Steps

### **1. Enable Transaction Logs**

Add to `application.properties`:

```properties
# Transaction logging
logging.level.org.springframework.transaction=DEBUG
logging.level.org.springframework.orm.jpa=DEBUG
```

### **2. Check Database Constraints**

```sql
-- View table constraints
SELECT conname, contype 
FROM pg_constraint 
WHERE conrelid = 'contract'::regclass;
```

### **3. Test in isolation**

```java
@Test
void testCreateContract() {
    // Test logic without transaction
    Contract contract = ...;
    contractRepository.save(contract);
}
```

## ⚠️ Common Issues

### **Issue 1: Duplicate contract_number**

Fix:
```java
if (contractRepository.existsByContractNumber(request.getContractNumber())) {
    throw new RuntimeException("Contract number already exists");
}
```

### **Issue 2: Invalid customer_id**

Fix:
```java
Customer customer = customerRepository.findById(customerId)
    .orElseThrow(() -> new RuntimeException("Customer not found"));
```

### **Issue 3: Data type mismatch**

Fix: Check entity field types match database columns.

## 🔄 Temporary Workaround

### **Disable transaction temporarily**

```java
// Remove @Transactional to see actual error
// @Transactional  // Comment out
public void method() {
    // Business logic
}
```

## 📊 Transaction Flow

```
Start Transaction (@Transactional)
    ↓
Execute Business Logic
    ↓
Exception Occurs ❌
    ↓
Mark Transaction as Rollback-Only
    ↓
Try to commit
    ↓
UnexpectedRollbackException ❌
```

## ✅ Proper Error Handling

```java
@Service
public class ContractService {
    
    @Autowired
    private ContractRepository contractRepository;
    
    @Transactional
    public Contract createContract(ContractCreateRequestDto request) {
        try {
            // Validate
            validateRequest(request);
            
            // Check constraints
            if (contractRepository.existsByContractNumber(request.getContractNumber())) {
                throw new IllegalArgumentException("Contract number exists");
            }
            
            // Create entity
            Contract contract = mapToEntity(request);
            
            // Save
            return contractRepository.save(contract);
            
        } catch (Exception e) {
            // Log
            log.error("Error creating contract", e);
            // Re-throw
            throw new RuntimeException("Failed to create contract: " + e.getMessage(), e);
        }
    }
}
```

## 🎯 Next Steps

1. ✅ Check application logs for full stack trace
2. ✅ Identify the actual exception
3. ✅ Fix the root cause
4. ✅ Add proper error handling
5. ✅ Test in isolation

## 💡 Best Practice

Always wrap database operations with proper exception handling:

```java
@Transactional
public void method() {
    try {
        // Database operations
    } catch (DataIntegrityViolationException e) {
        throw new RuntimeException("Data constraint violation", e);
    } catch (Exception e) {
        throw new RuntimeException("Unexpected error", e);
    }
}
```

