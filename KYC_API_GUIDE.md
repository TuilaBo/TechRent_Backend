# KYC (Know Your Customer) API Guide

## Overview
Hệ thống KYC để verify customer với 3 ảnh:
- Ảnh mặt trước CCCD
- Ảnh mặt sau CCCD  
- Ảnh selfie

## Customer Model Changes

### Added Fields:
```java
@Column(name = "kyc_status")
private KYCStatus kycStatus; // NOT_STARTED, PENDING_VERIFICATION, VERIFIED, REJECTED

@Column(name = "kyc_front_cccd_url")
private String kycFrontCCCDUrl;

@Column(name = "kyc_back_cccd_url")  
private String kycBackCCCDUrl;

@Column(name = "kyc_selfie_url")
private String kycSelfieUrl;

@Column(name = "kyc_verified_at")
private LocalDateTime kycVerifiedAt;

@Column(name = "kyc_verified_by")
private Long kycVerifiedBy;

@Column(name = "kyc_rejection_reason")
private String kycRejectionReason;
```

## API Endpoints (Operator Only)

### 1. Upload ảnh mặt trước CCCD
```http
POST /api/operator/kyc/customers/{customerId}/front-cccd
Content-Type: multipart/form-data
Authorization: Bearer {operator_token}

Body:
  file: <upload file>
```

### 2. Upload ảnh mặt sau CCCD
```http
POST /api/operator/kyc/customers/{customerId}/back-cccd
Content-Type: multipart/form-data
Authorization: Bearer {operator_token}

Body:
  file: <upload file>
```

### 3. Upload ảnh selfie
```http
POST /api/operator/kyc/customers/{customerId}/selfie
Content-Type: multipart/form-data
Authorization: Bearer {operator_token}

Body:
  file: <upload file>
```

### 4. Verify KYC
```http
POST /api/operator/kyc/customers/{customerId}/verify
Authorization: Bearer {operator_token}
Content-Type: application/json

Body:
{
  "status": "VERIFIED", // hoặc "REJECTED"
  "rejectionReason": "Ảnh không rõ" // nếu REJECTED
}
```

### 5. Xem danh sách customer chờ verify
```http
GET /api/operator/kyc/pending-verification
Authorization: Bearer {operator_token}
```

### 6. Xem thông tin KYC của customer
```http
GET /api/operator/kyc/customers/{customerId}
Authorization: Bearer {operator_token}
```

## KYC Status Flow

```
NOT_STARTED
    ↓ (Upload ảnh đầu tiên)
PENDING_VERIFICATION  
    ↓ (Upload đủ 3 ảnh)
DOCUMENTS_SUBMITTED
    ↓ (Operator verify)
VERIFIED hoặc REJECTED
```

## Usage Examples

### CURL Examples:

**1. Upload Front CCCD:**
```bash
curl -X POST 'http://localhost:8080/api/operator/kyc/customers/1/front-cccd' \
  -H 'Authorization: Bearer YOUR_TOKEN' \
  -F 'file=@/path/to/front_cccd.jpg'
```

**2. Upload Back CCCD:**
```bash
curl -X POST 'http://localhost:8080/api/operator/kyc/customers/1/back-cccd' \
  -H 'Authorization: Bearer YOUR_TOKEN' \
  -F 'file=@/path/to/back_cccd.jpg'
```

**3. Upload Selfie:**
```bash
curl -X POST 'http://localhost:8080/api/operator/kyc/customers/1/selfie' \
  -H 'Authorization: Bearer YOUR_TOKEN' \
  -F 'file=@/path/to/selfie.jpg'
```

**4. Verify KYC:**
```bash
curl -X POST 'http://localhost:8080/api/operator/kyc/customers/1/verify' \
  -H 'Authorization: Bearer YOUR_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "status": "VERIFIED"
  }'
```

**5. Xem danh sách pending:**
```bash
curl -X GET 'http://localhost:8080/api/operator/kyc/pending-verification' \
  -H 'Authorization: Bearer YOUR_TOKEN'
```

## KYC Status Values

- `NOT_STARTED` - Chưa bắt đầu
- `PENDING_VERIFICATION` - Đang chờ xác minh (đã upload 1-2 ảnh)
- `DOCUMENTS_SUBMITTED` - Đã nộp đủ giấy tờ (đủ 3 ảnh)
- `VERIFIED` - Đã xác minh
- `REJECTED` - Từ chối
- `EXPIRED` - Hết hạn

## Response Examples

**Upload Success:**
```json
{
  "message": "Upload ảnh thành công!",
  "details": "Ảnh front_cccd đã được lưu",
  "data": {
    "imageUrl": "/uploads/kyc/customer_1_front_cccd_1234567890.jpg",
    "imageType": "front_cccd",
    "kycStatus": "PENDING_VERIFICATION"
  }
}
```

**Verify Success:**
```json
{
  "message": "Xác minh KYC thành công!",
  "details": "Trạng thái KYC của customer đã được cập nhật: Đã xác minh",
  "data": {
    "customerId": 1,
    "kycStatus": "VERIFIED",
    "kycVerifiedAt": "2025-01-27T10:00:00",
    "kycVerifiedBy": 123
  }
}
```

## TODO: File Upload Implementation

Hiện tại đang return mock URL. Cần implement:

1. **Cloud Storage Integration**:
   - AWS S3
   - Azure Blob Storage
   - Google Cloud Storage
   - Hoặc local file system

2. **File Validation**:
   - Check file type (image only)
   - Check file size (max 5MB)
   - Check image dimensions
   - Virus scanning

3. **File Naming**:
   - Format: `customer_{id}_{type}_{timestamp}.jpg`
   - Ensure unique filenames

## Database Migration

Sau khi update Customer model, cần update database:

```sql
ALTER TABLE customer 
ADD COLUMN kyc_status VARCHAR(50) DEFAULT 'NOT_STARTED',
ADD COLUMN kyc_front_cccd_url VARCHAR(500),
ADD COLUMN kyc_back_cccd_url VARCHAR(500),
ADD COLUMN kyc_selfie_url VARCHAR(500),
ADD COLUMN kyc_verified_at TIMESTAMP,
ADD COLUMN kyc_verified_by BIGINT,
ADD COLUMN kyc_rejection_reason VARCHAR(1000);
```

