#!/bin/bash
# Script để check các log liên quan đến tạo task Device Replacement

LOG_FILE="/var/log/techrentsystem/application.log"

echo "=== KIỂM TRA LOG TẠO TASK DEVICE REPLACEMENT ==="
echo ""

echo "1. Log 'Bắt đầu tạo task Device Replacement':"
grep "Bắt đầu tạo task Device Replacement" "$LOG_FILE" | tail -n 5

echo ""
echo "2. Log 'Hoàn thành tạo task Device Replacement':"
grep "Hoàn thành tạo task Device Replacement" "$LOG_FILE" | tail -n 5

echo ""
echo "3. Log '✅ Đã tạo task Device Replacement (taskId:' (THÀNH CÔNG):"
grep "✅ Đã tạo task Device Replacement" "$LOG_FILE" | tail -n 5

echo ""
echo "4. Log '❌ Lỗi khi tạo task Device Replacement' (LỖI):"
grep "❌ Lỗi khi tạo task Device Replacement" "$LOG_FILE" | tail -n 5

echo ""
echo "5. Log 'Không tìm thấy TaskCategory' (Category chưa tồn tại):"
grep "Không tìm thấy TaskCategory 'Device Replacement'" "$LOG_FILE" | tail -n 5

echo ""
echo "6. Log 'Đã có task Device Replacement pending' (Task đã tồn tại):"
grep "Đã có task Device Replacement pending" "$LOG_FILE" | tail -n 5

echo ""
echo "7. Log 'Gọi taskService.createTask()' (Debug):"
grep "Gọi taskService.createTask() để tạo task Device Replacement" "$LOG_FILE" | tail -n 5

echo ""
echo "=== KIỂM TRA LOG CHO COMPLAINT #13 ==="
grep "complaint #13" "$LOG_FILE" | tail -n 10

