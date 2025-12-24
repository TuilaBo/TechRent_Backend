# 📋 HƯỚNG DẪN API - LUỒNG COMPLAINT → QC → DEVICE REPLACEMENT

## 🔄 Flow tổng quan

```
1. Customer tạo complaint
2. Staff process complaint → Tự động tạo task "Pre rental QC Replace"
3. Operator assign task cho technician
4. Technician tạo/cập nhật QC report → Tự động tạo task "Device Replacement"
5. Operator assign task "Device Replacement" cho staff
6. Staff ký biên bản replacement report
7. Customer ký biên bản replacement report → Tự động complete task
8. Staff resolve complaint (optional)
```

---

## 📝 DANH SÁCH API THEO THỨ TỰ

### **BƯỚC 1: Customer tạo Complaint**

#### 1.1. Tạo complaint
```
POST /api/customer/complaints
Content-Type: multipart/form-data
Authorization: Bearer {customer_token}

Body:
- request: {
    "orderId": 123,
    "deviceId": 456,
    "allocationId": 789,
    "description": "Thiết bị bị hỏng màn hình"
  }
- evidenceImage (optional): File ảnh bằng chứng

Response:
{
  "complaintId": 1,
  "status": "PENDING",
  ...
}
```

**Kết quả:**
- Complaint status = `PENDING`
- Device status = `COMPLAINED`

---

### **BƯỚC 2: Staff Process Complaint**

#### 2.1. Process complaint (Tự động tạo task QC)
```
PATCH /api/staff/complaints/{complaintId}/process
Content-Type: application/json
Authorization: Bearer {staff_token}

Body (optional):
{
  "faultSource": "CUSTOMER" | "SYSTEM" | "UNKNOWN",
  "conditionDefinitionIds": [1, 2, 3],
  "damageNote": "Màn hình vỡ",
  "staffNote": "Ghi chú của staff"
}

Response:
{
  "complaintId": 1,
  "status": "PROCESSING",
  "replacementTaskId": 100,  // ← Task "Pre rental QC Replace" được tạo tự động
  "replacementDeviceId": 999,
  ...
}
```

**Kết quả:**
- ✅ Tự động tạo task "Pre rental QC Replace" (category: "Pre rental QC Replace")
- ✅ Tự động tìm device thay thế
- ✅ Device cũ → `DAMAGED`
- ✅ Device mới (suggested) → `PRE_RENTAL_QC`
- ✅ Complaint status = `PROCESSING`
- ✅ Link complaint với task: `complaint.replacementTask = task`

**Lưu ý:**
- TaskCategory "Pre rental QC Replace" sẽ được tạo tự động nếu chưa có
- Task được tạo với status = `PENDING`, chưa assign staff

---

### **BƯỚC 3: Operator Assign Task QC cho Technician**

#### 3.1. Assign task "Pre rental QC Replace"
```
PATCH /api/staff/tasks/{taskId}/assign
Content-Type: application/json
Authorization: Bearer {operator_token}

Body:
{
  "staffIds": [technicianStaffId1, technicianStaffId2]
}

Response:
{
  "taskId": 100,
  "assignedStaff": [...],
  "status": "PENDING",
  ...
}
```

**Kết quả:**
- Task được assign cho technician(s)
- Technician có thể xem task trong danh sách task của mình

---

### **BƯỚC 4: Technician Tạo/Cập Nhật QC Report**

#### 4.1. Tạo QC report (Lần đầu)
```
POST /api/technician/qc-reports/pre-rental
Content-Type: multipart/form-data
Authorization: Bearer {technician_token}

Body:
- request: {
    "taskId": 100,  // Task "Pre rental QC Replace"
    "result": "READY_FOR_SHIPPING",  // ← QUAN TRỌNG: Phải là READY_FOR_SHIPPING
    "findings": "Device hoạt động tốt",
    "orderDetailSerialNumbers": {
      "orderDetailId": 50,
      "serials": ["NEW-DEVICE-SERIAL-123"]
    },
    "deviceConditions": [
      {
        "deviceId": 999,
        "conditionDefinitionId": 1,
        "severity": "GOOD",
        "images": []
      }
    ]
  }
- accessorySnapshot (optional): File ảnh

Response:
{
  "qcReportId": 200,
  "taskId": 100,
  "result": "READY_FOR_SHIPPING",
  ...
}
```

**HOẶC**

#### 4.2. Cập nhật QC report (Nếu đã có)
```
PUT /api/technician/qc-reports/pre-rental/{reportId}
Content-Type: multipart/form-data
Authorization: Bearer {technician_token}

Body: (tương tự như POST)
- request: {
    "result": "READY_FOR_SHIPPING",  // ← QUAN TRỌNG
    "findings": "...",
    "orderDetailSerialNumbers": {...},
    "deviceConditions": [...]
  }
- accessorySnapshot (optional): File ảnh
```

**Kết quả khi `result = READY_FOR_SHIPPING`:**
- ✅ Tự động tạo allocation cho device thay thế
- ✅ Tự động tạo task "Device Replacement" (nếu chưa có)
- ✅ Tự động tạo DeviceReplacementReport
- ✅ Update complaint với `replacementAllocation`
- ✅ Task "Pre rental QC Replace" → `COMPLETED`

**Lưu ý:**
- Nếu `result = PRE_RENTAL_FAILED` → Reset device về `AVAILABLE`, không tạo task
- Technician có thể chọn device khác (không nhất thiết là device suggested)

---

### **BƯỚC 5: Operator Assign Task "Device Replacement" cho Staff**

#### 5.1. Assign task "Device Replacement"
```
PATCH /api/staff/tasks/{taskId}/assign
Content-Type: application/json
Authorization: Bearer {operator_token}

Body:
{
  "staffIds": [deliveryStaffId1, deliveryStaffId2]
}

Response:
{
  "taskId": 101,  // Task "Device Replacement"
  "assignedStaff": [...],
  "status": "PENDING",
  ...
}
```

**Kết quả:**
- Task "Device Replacement" được assign cho staff đi giao
- Staff có thể xem task trong danh sách task của mình

---

### **BƯỚC 6: Staff Ký Biên Bản Replacement Report**

#### 6.1. Gửi PIN cho staff (nếu cần)
```
POST /api/staff/device-replacement-reports/{replacementReportId}/pin
Content-Type: application/json
Authorization: Bearer {staff_token}

Response:
{
  "pinCode": "123456",
  "smsSent": true,
  "emailSent": false,
  ...
}
```

#### 6.2. Staff ký biên bản
```
PATCH /api/staff/device-replacement-reports/{replacementReportId}/signature
Content-Type: application/json
Authorization: Bearer {staff_token}

Body:
{
  "pin": "123456",
  "signature": "base64_image_string"
}

Response:
{
  "replacementReportId": 300,
  "staffSigned": true,
  "customerSigned": false,
  "status": "STAFF_SIGNED",
  ...
}
```

**Kết quả:**
- Biên bản được ký bởi staff
- Status = `STAFF_SIGNED`
- Chờ customer ký

---

### **BƯỚC 7: Customer Ký Biên Bản Replacement Report**

#### 7.1. Gửi PIN cho customer
```
POST /api/customers/device-replacement-reports/{replacementReportId}/pin
Content-Type: application/json
Authorization: Bearer {customer_token}

Body:
{
  "email": "customer@example.com"  // Optional, dùng email từ account
}

Response:
{
  "pinCode": "654321",
  "smsSent": true,
  "emailSent": true,
  ...
}
```

#### 7.2. Customer ký biên bản
```
PATCH /api/customers/device-replacement-reports/{replacementReportId}/signature
Content-Type: application/json
Authorization: Bearer {customer_token}

Body:
{
  "pin": "654321",
  "signature": "base64_image_string"
}

Response:
{
  "replacementReportId": 300,
  "staffSigned": true,
  "customerSigned": true,
  "status": "BOTH_SIGNED",
  ...
}
```

**Kết quả:**
- ✅ Biên bản được ký bởi cả staff và customer
- ✅ Status = `BOTH_SIGNED`
- ✅ **Tự động đánh dấu task "Device Replacement" = `COMPLETED`**

---

### **BƯỚC 8: Staff Resolve Complaint (Optional, Idempotent)**

#### 8.1. Resolve complaint
```
PATCH /api/staff/complaints/{complaintId}/resolve
Content-Type: multipart/form-data
Authorization: Bearer {staff_token}

Body:
- staffNote (optional): "Đã giao xong"
- evidenceFiles (optional): [File1, File2]

Response:
{
  "complaintId": 1,
  "status": "RESOLVED",
  ...
}
```

**Kết quả:**
- ✅ Complaint status = `RESOLVED`
- ✅ Task "Pre rental QC Replace" = `COMPLETED` (nếu chưa)
- ✅ Task "Device Replacement" = `COMPLETED` (nếu chưa)
- ✅ Upload ảnh bằng chứng (nếu có)

**Lưu ý:**
- API này là idempotent, có thể gọi nhiều lần
- Nếu task đã complete rồi thì không ảnh hưởng

---

## 🔍 API HỖ TRỢ (Optional)

### **Xem danh sách complaints**
```
GET /api/staff/complaints?status=PROCESSING
Authorization: Bearer {staff_token}
```

### **Xem chi tiết complaint**
```
GET /api/staff/complaints/{complaintId}
Authorization: Bearer {staff_token}
```

### **Xem danh sách tasks**
```
GET /api/staff/tasks?orderId={orderId}&status=PENDING
Authorization: Bearer {staff_token}
```

### **Xem chi tiết task**
```
GET /api/staff/tasks/{taskId}
Authorization: Bearer {staff_token}
```

### **Xem QC report**
```
GET /api/technician/qc-reports/{reportId}
Authorization: Bearer {technician_token}
```

### **Xem replacement report**
```
GET /api/staff/device-replacement-reports/{replacementReportId}
Authorization: Bearer {staff_token}
```

### **Cập nhật fault source và conditions (Sau khi kiểm tra tại chỗ)**
```
PATCH /api/staff/complaints/{complaintId}/fault
Content-Type: application/json
Authorization: Bearer {staff_token}

Body:
{
  "faultSource": "CUSTOMER",
  "conditionDefinitionIds": [1, 2, 3],
  "damageNote": "Màn hình vỡ do va đập",
  "staffNote": "Khách hàng làm rơi"
}

Response:
{
  "complaintId": 1,
  "faultSource": "CUSTOMER",
  ...
}
```

**Kết quả:**
- ✅ Update fault source và conditions
- ✅ Nếu `faultSource = CUSTOMER` → Tự động tạo DiscrepancyReport để tính phí thiệt hại

---

## ⚠️ LƯU Ý QUAN TRỌNG

### 1. TaskCategory phải tồn tại
- ✅ "Pre rental QC Replace" → Tự động tạo nếu chưa có
- ❌ "Device Replacement" → Phải tạo thủ công hoặc chạy SQL

### 2. QC Result phải đúng
- ✅ `READY_FOR_SHIPPING` → Tạo task "Device Replacement"
- ❌ `PRE_RENTAL_FAILED` → Không tạo task, reset device

### 3. Thứ tự ký biên bản
- ✅ Staff ký trước → Customer ký sau
- ❌ Customer không thể ký nếu staff chưa ký

### 4. Task tự động complete
- ✅ Task "Device Replacement" tự động complete khi customer ký
- ✅ Task "Pre rental QC Replace" tự động complete khi QC pass

---

## 📊 TÓM TẮT API THEO VAI TRÒ

### **Customer:**
1. `POST /api/customer/complaints` - Tạo complaint
2. `POST /api/customers/device-replacement-reports/{id}/pin` - Yêu cầu PIN
3. `PATCH /api/customers/device-replacement-reports/{id}/signature` - Ký biên bản

### **Staff/Operator:**
1. `PATCH /api/staff/complaints/{id}/process` - Process complaint
2. `PATCH /api/staff/tasks/{id}/assign` - Assign task
3. `PATCH /api/staff/device-replacement-reports/{id}/signature` - Ký biên bản
4. `PATCH /api/staff/complaints/{id}/resolve` - Resolve complaint

### **Technician:**
1. `POST /api/technician/qc-reports/pre-rental` - Tạo QC report
2. `PUT /api/technician/qc-reports/pre-rental/{id}` - Cập nhật QC report

---

## 🎯 FLOW TỐI THIỂU (Bắt buộc)

```
1. POST /api/customer/complaints
2. PATCH /api/staff/complaints/{id}/process
3. PATCH /api/staff/tasks/{qcTaskId}/assign
4. POST /api/technician/qc-reports/pre-rental (result = READY_FOR_SHIPPING)
5. PATCH /api/staff/tasks/{deliveryTaskId}/assign
6. PATCH /api/staff/device-replacement-reports/{id}/signature
7. PATCH /api/customers/device-replacement-reports/{id}/signature
```

**Tổng: 7 API calls tối thiểu**

---

## 📌 CHECKLIST TRƯỚC KHI TEST

- [ ] TaskCategory "Pre rental QC Replace" đã có (hoặc sẽ tự tạo)
- [ ] TaskCategory "Device Replacement" đã có (chạy SQL nếu chưa)
- [ ] Có ít nhất 1 device AVAILABLE cùng model với device bị hỏng
- [ ] Order đang ở status phù hợp (PROCESSING, IN_USE, RENTED, ACTIVE)
- [ ] Có staff với role TECHNICIAN
- [ ] Có staff với role để đi giao hàng
- [ ] Customer có email/phone để nhận PIN

---

## 🔗 Xem thêm

- File SQL kiểm tra TaskCategory: `check_task_categories.sql`
- File SQL kiểm tra Device Replacement: `check_device_replacement.sql`
