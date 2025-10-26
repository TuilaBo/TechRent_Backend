# REST API Review - Các Vi Phạm Chuẩn REST

## 🚨 Các Vi Phạm:

### 1. Contract APIs - CẦN SỬA:

#### ❌ Vi Phạm: Query trong Path
```http
POST /api/contracts/{contractId}/send-pin/sms
POST /api/contracts/{contractId}/send-pin/email
```

**Vấn đề:** `/sms` và `/email` là query parameter, không nên trong path

**Nên là:**
```http
POST /api/contracts/{contractId}/pin
Body: { "method": "sms", "contact": "phone" }
Body: { "method": "email", "contact": "email" }
```

#### ❌ Vi Phạm: Verb trong URL
```http
POST /api/contracts/{contractId}/send-for-signature
```

**Vấn đề:** `send-for-signature` là action/verb

**Nên là:**
```http
PUT /api/contracts/{contractId}/status
Body: { "status": "PENDING_SIGNATURE" }
```

#### ❌ Vi Phạm: Động từ trong Path
```http
POST /api/contracts/{contractId}/sign
```

**Vấn đề:** `sign` là action

**Nên là:**
```http
POST /api/contracts/{contractId}/signatures
Body: { "pinCode": "...", "digitalSignature": "..." }
```

#### ❌ Vi Phạm: Nested Resource không rõ ràng
```http
GET /api/contracts/my-contracts
GET /api/contracts/pending-signature
GET /api/operator/kyc/customers/{id}/front-cccd
```

**Nên là:**
```http
GET /api/customers/me/contracts?status=PENDING_SIGNATURE
GET /api/contracts?status=PENDING_SIGNATURE
POST /api/kyc/customers/{id}/documents
Body: { "type": "front_cccd", "file": "..." }
```

### 2. KYC APIs - CẦN SỬA:

#### ❌ Vi Phạm: Quá nhiều action trong path
```http
POST /api/operator/kyc/customers/{customerId}/front-cccd
POST /api/operator/kyc/customers/{customerId}/back-cccd
POST /api/operator/kyc/customers/{customerId}/selfie
POST /api/operator/kyc/customers/{customerId}/verify
```

**Nên là:**
```http
# Upload documents
POST /api/kyc/customers/{customerId}/documents
Body: { "type": "front_cccd", "file": "..." }

POST /api/kyc/customers/{customerId}/documents
Body: { "type": "back_cccd", "file": "..." }

POST /api/kyc/customers/{customerId}/documents  
Body: { "type": "selfie", "file": "..." }

# Verify (update status)
PATCH /api/kyc/customers/{customerId}
Body: { "status": "VERIFIED" }

# Query
GET /api/kyc/customers/{customerId}
GET /api/kyc?status=PENDING_VERIFICATION
```

## ✅ Chuẩn REST API Nên Là:

### Resources & Actions:

| Resource | GET | POST | PUT | PATCH | DELETE |
|----------|-----|------|-----|-------|--------|
| `/api/contracts` | List all | Create new | - | - | - |
| `/api/contracts/{id}` | Get one | - | Update all | Update partial | Delete |
| `/api/contracts/{id}/status` | - | - | Update status | - | - |
| `/api/contracts/{id}/signatures` | Get signatures | Create signature | - | - | - |
| `/api/customers/me/contracts` | My contracts | - | - | - | - |
| `/api/kyc/customers/{id}` | Get KYC info | - | - | Update KYC | - |
| `/api/kyc/customers/{id}/documents` | Get docs | Upload doc | - | - | Delete doc |

### Query Parameters (không trong path):

```
❌ /api/contracts/pending-signature
✅ /api/contracts?status=PENDING_SIGNATURE

❌ /api/contracts/{id}/send-pin/sms
✅ /api/contracts/{id}/pin?method=sms

❌ /api/operator/kyc/customers/{id}/front-cccd
✅ /api/kyc/customers/{id}/documents?type=front_cccd
```

## 📊 So Sánh:

### ❌ HIỆN TẠI (Không chuẩn):
```
POST /api/contracts/{id}/send-pin/sms
POST /api/contracts/{id}/sign
POST /api/operator/kyc/customers/{id}/verify
POST /api/operator/kyc/customers/{id}/front-cccd
```

### ✅ NÊN LÀ (RESTful):
```
POST /api/contracts/{id}/pin
Body: { "method": "sms" }

POST /api/contracts/{id}/signatures
Body: { "pinCode": "...", ... }

PATCH /api/kyc/customers/{id}
Body: { "status": "VERIFIED" }

POST /api/kyc/customers/{id}/documents
Body: { "type": "front_cccd", "file": "..." }
```

## 🔧 Cần Sửa Gì:

1. **Loại bỏ verbs trong URL**: `send`, `verify`, `sign` → Dùng HTTP methods
2. **Loại bỏ type trong path**: `/sms`, `/email` → Query params hoặc body
3. **Dùng resources**: `/documents` thay vì `/front-cccd`, `/back-cccd`
4. **Dùng PATCH cho partial updates**: `/status`, `/verify`
5. **Query params cho filtering**: `?status=`, `?type=`

## Kết Luận:

**ĐÁNH GIÁ: 4/10** (Không chuẩn REST)

**Vấn đề:**
- ❌ Quá nhiều verbs trong URL
- ❌ Type/action trong path
- ❌ Nested resources không rõ ràng
- ❌ Thiếu query params

**Cần refactor để RESTful!**

