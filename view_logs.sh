#!/bin/bash
# Script để xem log gần nhất của TechRent System

LOG_FILE="/var/log/techrentsystem/application.log"

echo "=== XEM LOG GẦN NHẤT (50 dòng cuối) ==="
tail -n 50 "$LOG_FILE"

echo ""
echo "=== XEM LOG THEO THỜI GIAN THỰC (Ctrl+C để dừng) ==="
echo "Chạy: tail -f $LOG_FILE"
echo ""
echo "=== XEM LOG CỦA HÔM NAY ==="
echo "Chạy: grep \"$(date +%Y-%m-%d)\" $LOG_FILE"
echo ""
echo "=== XEM LOG ERROR GẦN NHẤT ==="
echo "Chạy: grep -i error $LOG_FILE | tail -n 20"
echo ""
echo "=== XEM LOG WARN GẦN NHẤT ==="
echo "Chạy: grep -i warn $LOG_FILE | tail -n 20"
