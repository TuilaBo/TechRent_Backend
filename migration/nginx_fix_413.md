# Fix 413 Request Entity Too Large trên Nginx

## Vấn đề
Nginx mặc định giới hạn request body là **1MB**, gây lỗi 413 khi upload file > 1MB.

## Giải pháp

### Bước 1: Tìm file cấu hình nginx

Thường nằm ở một trong các vị trí:
- `/etc/nginx/nginx.conf` (main config)
- `/etc/nginx/sites-available/default` (default site)
- `/etc/nginx/sites-available/your-site-name` (site cụ thể)

### Bước 2: Thêm `client_max_body_size`

**Cách 1: Thêm vào `http` block trong `/etc/nginx/nginx.conf`** (áp dụng cho tất cả sites):

```nginx
http {
    # ... existing config ...
    
    # Tăng giới hạn upload lên 30MB (đủ cho 3 file x 10MB)
    client_max_body_size 30M;
    
    # ... rest of config ...
}
```

**Cách 2: Thêm vào `server` block trong site config** (chỉ áp dụng cho site cụ thể):

**Trường hợp có HTTPS (HTTP redirect sang HTTPS):**

```nginx
# HTTP block (redirect)
server {
    listen 80;
    server_name techrent.website www.techrent.website;
    
    return 301 https://$host$request_uri;
}

# HTTPS block (quan trọng - thêm vào đây)
server {
    listen 443 ssl http2;
    server_name techrent.website www.techrent.website;
    
    # SSL certificates
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    
    # Tăng giới hạn upload lên 30MB ← THÊM DÒNG NÀY
    client_max_body_size 30M;
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

**Trường hợp chỉ HTTP:**

```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    # Tăng giới hạn upload lên 30MB
    client_max_body_size 30M;
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Bước 3: Kiểm tra cấu hình và reload nginx

```bash
# Kiểm tra syntax
sudo nginx -t

# Nếu OK, reload nginx
sudo systemctl reload nginx
# Hoặc
sudo service nginx reload
```

### Bước 4: Kiểm tra lại

Upload file > 1MB để xác nhận đã fix.

## Lưu ý

- `client_max_body_size` có thể đặt ở 3 level:
  - `http` block: áp dụng cho tất cả sites
  - `server` block: áp dụng cho site cụ thể
  - `location` block: áp dụng cho endpoint cụ thể

- Giá trị khuyến nghị:
  - **30M**: đủ cho batch KYC (3 file x 10MB)
  - **50M**: nếu cần buffer thêm
  - **100M**: nếu có upload file lớn hơn

- Nếu vẫn lỗi sau khi fix, kiểm tra:
  1. Đã reload nginx chưa?
  2. Có cache nginx không? (thử clear cache browser)
  3. Có reverse proxy khác (Cloudflare, etc.) không?

