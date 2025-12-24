# Pre-Rental QC Request Body Guide

## API Endpoint

### Tạo mới QC Report
```
POST /api/technician/qc-reports/pre-rental
Content-Type: multipart/form-data
Authorization: Bearer {technician_token}
```

### Cập nhật QC Report
```
PUT /api/technician/qc-reports/pre-rental/{reportId}
Content-Type: multipart/form-data
Authorization: Bearer {technician_token}
```

---

## Request Body Structure

**Lưu ý:** API sử dụng `multipart/form-data`, không phải `application/json`.

### Form Data Fields:

1. **`request`** (required): JSON string chứa thông tin QC report
2. **`accessorySnapshot`** (optional): File ảnh (MultipartFile)

---

## Request JSON Schema

```json
{
  "taskId": 100,                    // Long - REQUIRED: ID của task QC
  "result": "READY_FOR_SHIPPING",   // QCResult - REQUIRED
  "findings": "Device hoạt động tốt", // String - OPTIONAL: Ghi chú
  "orderDetailSerialNumbers": {      // Map<Long, List<String>> - REQUIRED
    "50": ["DEVICE-SERIAL-001"],    // Key: orderDetailId, Value: List serial numbers
    "51": ["DEVICE-SERIAL-002", "DEVICE-SERIAL-003"]
  },
  "deviceConditions": [             // List<QCDeviceConditionRequestDto> - OPTIONAL
    {
      "deviceId": 999,              // Long - REQUIRED
      "conditionDefinitionId": 1,   // Long - REQUIRED
      "severity": "GOOD",            // String - OPTIONAL
      "images": []                   // List<String> - OPTIONAL: URLs hoặc base64
    }
  ]
}
```

---

## QCResult Enum Values

- `READY_FOR_SHIPPING` - Device sẵn sàng để giao hàng
- `READY_FOR_RE_STOCK` - Device cần trả về kho
- `PRE_RENTAL_FAILED` - QC thất bại (pre-rental)
- `POST_RENTAL_FAILED` - QC thất bại (post-rental)

**Lưu ý:** Đối với complaint flow, phải dùng `READY_FOR_SHIPPING` để trigger tự động tạo task "Device Replacement".

---

## Ví dụ Request Body

### Ví dụ 1: Tạo QC Report đơn giản (PASS)
```json
{
  "taskId": 100,
  "result": "READY_FOR_SHIPPING",
  "findings": "Device hoạt động tốt, không có lỗi",
  "orderDetailSerialNumbers": {
    "50": ["LAPTOP-001"]
  },
  "deviceConditions": []
}
```

### Ví dụ 2: Tạo QC Report với device conditions
```json
{
  "taskId": 100,
  "result": "READY_FOR_SHIPPING",
  "findings": "Device hoạt động tốt. Màn hình có vết xước nhỏ nhưng không ảnh hưởng sử dụng.",
  "orderDetailSerialNumbers": {
    "50": ["LAPTOP-001"]
  },
  "deviceConditions": [
    {
      "deviceId": 999,
      "conditionDefinitionId": 1,
      "severity": "GOOD",
      "images": []
    },
    {
      "deviceId": 999,
      "conditionDefinitionId": 5,
      "severity": "MINOR",
      "images": [
        "https://cloudinary.com/image1.jpg",
        "https://cloudinary.com/image2.jpg"
      ]
    }
  ]
}
```

### Ví dụ 3: QC Report cho nhiều devices trong cùng order
```json
{
  "taskId": 100,
  "result": "READY_FOR_SHIPPING",
  "findings": "Tất cả devices đều hoạt động tốt",
  "orderDetailSerialNumbers": {
    "50": ["LAPTOP-001"],
    "51": ["MOUSE-001", "MOUSE-002"],
    "52": ["KEYBOARD-001"]
  },
  "deviceConditions": [
    {
      "deviceId": 999,
      "conditionDefinitionId": 1,
      "severity": "GOOD",
      "images": []
    },
    {
      "deviceId": 1000,
      "conditionDefinitionId": 1,
      "severity": "GOOD",
      "images": []
    }
  ]
}
```

### Ví dụ 4: QC Report FAILED
```json
{
  "taskId": 100,
  "result": "PRE_RENTAL_FAILED",
  "findings": "Device không khởi động được. Cần thay thế device khác.",
  "orderDetailSerialNumbers": {
    "50": ["LAPTOP-001"]
  },
  "deviceConditions": [
    {
      "deviceId": 999,
      "conditionDefinitionId": 10,
      "severity": "CRITICAL",
      "images": [
        "https://cloudinary.com/error1.jpg"
      ]
    }
  ]
}
```

---

## Cách gọi API (cURL)

### Tạo mới QC Report
```bash
curl -X POST "https://www.techrent.website/api/technician/qc-reports/pre-rental" \
  -H "Authorization: Bearer {token}" \
  -F "request={\"taskId\":100,\"result\":\"READY_FOR_SHIPPING\",\"findings\":\"Device tốt\",\"orderDetailSerialNumbers\":{\"50\":[\"SERIAL-001\"]},\"deviceConditions\":[]}" \
  -F "accessorySnapshot=@/path/to/image.jpg"
```

### Cập nhật QC Report
```bash
curl -X PUT "https://www.techrent.website/api/technician/qc-reports/pre-rental/200" \
  -H "Authorization: Bearer {token}" \
  -F "request={\"result\":\"READY_FOR_SHIPPING\",\"findings\":\"Updated findings\",\"orderDetailSerialNumbers\":{\"50\":[\"SERIAL-001\"]},\"deviceConditions\":[]}" \
  -F "accessorySnapshot=@/path/to/image.jpg"
```

---

## Cách gọi API (Postman)

1. **Method:** `POST` hoặc `PUT`
2. **URL:** `https://www.techrent.website/api/technician/qc-reports/pre-rental` (hoặc `/pre-rental/{reportId}` cho PUT)
3. **Headers:**
   - `Authorization: Bearer {token}`
4. **Body:** Chọn `form-data`
   - Key: `request` (Type: Text), Value: JSON string
   - Key: `accessorySnapshot` (Type: File), Value: Chọn file ảnh (optional)

---

## Cách gọi API (JavaScript/Fetch)

```javascript
const formData = new FormData();

// Request JSON
const requestBody = {
  taskId: 100,
  result: "READY_FOR_SHIPPING",
  findings: "Device hoạt động tốt",
  orderDetailSerialNumbers: {
    "50": ["DEVICE-SERIAL-001"]
  },
  deviceConditions: []
};

formData.append('request', JSON.stringify(requestBody));

// Optional: File ảnh
const fileInput = document.querySelector('input[type="file"]');
if (fileInput.files[0]) {
  formData.append('accessorySnapshot', fileInput.files[0]);
}

fetch('https://www.techrent.website/api/technician/qc-reports/pre-rental', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + token
  },
  body: formData
})
.then(response => response.json())
.then(data => console.log(data));
```

---

## Lưu ý quan trọng

1. **Multipart/Form-Data:** API không nhận `application/json`, phải dùng `multipart/form-data`
2. **Request field:** Phải là JSON string, không phải object
3. **orderDetailSerialNumbers:** Key phải là `orderDetailId` (Long), Value là array of serial numbers
4. **QCResult:** Đối với complaint flow, **phải** dùng `READY_FOR_SHIPPING` để trigger tự động tạo task "Device Replacement"
5. **TaskId:** Phải là task có category "Pre rental QC" hoặc "Pre rental QC Replace"
6. **Validation:** Nếu order đã checkout (status `IN_USE`, `RENTED`, `ACTIVE`), không thể cập nhật QC report đã pass

---

## Response Example

```json
{
  "success": true,
  "message": "Tạo báo cáo QC PRE_RENTAL thành công!",
  "data": {
    "qcReportId": 200,
    "taskId": 100,
    "phase": "PRE_RENTAL",
    "result": "READY_FOR_SHIPPING",
    "findings": "Device hoạt động tốt",
    "createdAt": "2025-12-19T10:30:00",
    "createdBy": "technician_username",
    "orderId": 500,
    "deviceConditions": [...],
    "accessorySnapshotUrl": "https://cloudinary.com/..."
  }
}
```
