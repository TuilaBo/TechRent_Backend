# Test VNPAY Hash

## Cách test hash secret

### 1. Test với online tool
1. Vào: https://www.freeformatter.com/hmac-generator.html
2. Chọn algorithm: HMAC SHA512
3. Secret key: `7P3XI2WYW9U345M89WWNV2EWHCED4WXJ`
4. Input string (query string từ logs):
   ```
   vnp_Amount=1520000000&vnp_Command=pay&vnp_CreateDate=20251111235647&vnp_CurrCode=VND&vnp_IpAddr=127.0.0.1&vnp_Locale=vn&vnp_OrderInfo=Thanhtoandonhang102&vnp_OrderType=other&vnp_ReturnUrl=https://haunched-karina-nondiscriminatively.ngrok-free.dev/api/v1/vnpay/ipn&vnp_TmnCode=MCOOOKCG&vnp_TxnRef=1762880206997&vnp_Version=2.1.0
   ```
5. So sánh hash với hash trong logs

### 2. Kiểm tra hash secret trong VNPAY portal
- Đăng nhập: https://sandbox.vnpayment.vn/
- Vào phần cấu hình
- Xác nhận lại hash secret có đúng `7P3XI2WYW9U345M89WWNV2EWHCED4WXJ` không

### 3. Test với code
Chạy `VnpayHashTest.main()` để test hash generation

