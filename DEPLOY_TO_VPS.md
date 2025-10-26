# Deploy Spring Boot to VPS

## 📦 Build Project

### **Step 1: Build JAR file**

```bash
# Clean và build (skip tests)
mvn clean package -DskipTests

# Hoặc build với tests
mvn clean package

# Output: target/techrental-0.0.1-SNAPSHOT.jar
```

### **Alternative: Build với profile**

```bash
# Build for production
mvn clean package -Pprod -DskipTests
```

## 🚀 Deploy to VPS

### **Step 1: Upload JAR to VPS**

```bash
# Sử dụng SCP
scp target/techrental-0.0.1-SNAPSHOT.jar username@your-vps-ip:/path/to/deploy/

# Ví dụ:
scp target/techrental-0.0.1-SNAPSHOT.jar root@160.191.245.242:/var/apps/techrental/
```

### **Step 2: SSH vào VPS**

```bash
ssh username@your-vps-ip
# Ví dụ:
ssh root@160.191.245.242
```

### **Step 3: Run Application**

#### **Option 1: Run trực tiếp (testing)**
```bash
# Chạy trực tiếp
java -jar techrental-0.0.1-SNAPSHOT.jar

# Chạy với production profile
java -jar -Dspring.profiles.active=prod techrental-0.0.1-SNAPSHOT.jar
```

#### **Option 2: Run background với nohup**
```bash
# Chạy background
nohup java -jar -Dspring.profiles.active=prod techrental-0.0.1-SNAPSHOT.jar &

# Xem log
tail -f nohup.out

# Kiểm tra process
ps aux | grep java
```

#### **Option 3: Sử dụng systemd (Recommended)**

**Tạo service file:**

```bash
sudo nano /etc/systemd/system/techrental.service
```

**Nội dung:**
```ini
[Unit]
Description=TechRental Spring Boot Application
After=syslog.target

[Service]
User=root
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod /var/apps/techrental/techrental-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

**Các lệnh systemd:**
```bash
# Reload systemd
sudo systemctl daemon-reload

# Start service
sudo systemctl start techrental

# Stop service
sudo systemctl stop techrental

# Restart service
sudo systemctl restart techrental

# Check status
sudo systemctl status techrental

# Enable auto-start on boot
sudo systemctl enable techrental

# View logs
sudo journalctl -u techrental -f
```

## 🔄 Quick Deploy Script

### **deploy.sh** (tạo file này)

```bash
#!/bin/bash

echo "Building project..."
mvn clean package -DskipTests

echo "Copying JAR to VPS..."
scp target/techrental-0.0.1-SNAPSHOT.jar root@160.191.245.242:/var/apps/techrental/

echo "Deploying on VPS..."
ssh root@160.191.245.242 << 'ENDSSH'
cd /var/apps/techrental/
systemctl stop techrental
mv techrental-0.0.1-SNAPSHOT.jar app.jar
systemctl start techrental
systemctl status techrental
ENDSSH

echo "Deploy complete!"
```

**Usage:**
```bash
chmod +x deploy.sh
./deploy.sh
```

## 📋 Complete Deployment Checklist

### **On Local Machine:**

```bash
# 1. Build
mvn clean package -DskipTests

# 2. Test locally
java -jar target/techrental-0.0.1-SNAPSHOT.jar

# 3. Upload to VPS
scp target/techrental-0.0.1-SNAPSHOT.jar root@VPS_IP:/var/apps/techrental/
```

### **On VPS:**

```bash
# 1. SSH to VPS
ssh root@VPS_IP

# 2. Go to app directory
cd /var/apps/techrental/

# 3. Stop old version
systemctl stop techrental

# 4. Backup old version (optional)
cp app.jar app.jar.backup

# 5. Copy new version
cp techrental-0.0.1-SNAPSHOT.jar app.jar

# 6. Start service
systemctl start techrental

# 7. Check status
systemctl status techrental

# 8. View logs
journalctl -u techrental -f

# 9. Check if running
curl http://localhost:8080/actuator/health
```

## 🗂️ Directory Structure on VPS

```
/var/apps/techrental/
├── app.jar                      # Main application
├── application.properties       # Config (optional)
├── logs/                        # Application logs
└── backups/                     # Backup JAR files
```

## 🌐 Nginx Configuration (if using reverse proxy)

**File:** `/etc/nginx/sites-available/techrental`

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

**Enable:**
```bash
sudo ln -s /etc/nginx/sites-available/techrental /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

## 💾 Database Setup on VPS

```bash
# Install PostgreSQL
sudo apt update
sudo apt install postgresql postgresql-contrib

# Create database
sudo -u postgres psql
```

```sql
CREATE DATABASE techrentdb;
CREATE USER techrent WITH PASSWORD 'sa';
GRANT ALL PRIVILEGES ON DATABASE techrentdb TO techrent;
\q
```

## 🔧 Environment Variables (Optional)

**File:** `/etc/systemd/system/techrental.service`

```ini
[Service]
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="DB_HOST=localhost"
Environment="DB_NAME=techrentdb"
Environment="DB_USER=techrent"
Environment="DB_PASSWORD=sa"
ExecStart=/usr/bin/java -jar /var/apps/techrental/app.jar
```

Sau đó:
```bash
sudo systemctl daemon-reload
sudo systemctl restart techrental
```

## 🔍 Monitoring & Logs

```bash
# View application logs
journalctl -u techrental -f

# View last 100 lines
journalctl -u techrental -n 100

# View logs from specific time
journalctl -u techrental --since "1 hour ago"

# Check if app is running
curl http://localhost:8080/actuator/health

# Check process
ps aux | grep java
```

## 🚨 Troubleshooting

### **Problem: Port 8080 already in use**

```bash
# Find process using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Or change port in application.properties
server.port=8081
```

### **Problem: Application won't start**

```bash
# Check logs
journalctl -u techrental -n 50

# Check if database is running
systemctl status postgresql

# Check disk space
df -h

# Check Java version
java -version
```

### **Problem: Database connection error**

```bash
# Check PostgreSQL status
systemctl status postgresql

# Check if database exists
sudo -u postgres psql -l | grep techrentdb

# Test connection
psql -h localhost -U techrent -d techrentdb
```

## ✅ Quick Commands Summary

```bash
# Build
mvn clean package -DskipTests

# Upload
scp target/app.jar user@vps:/path/

# Deploy
ssh user@vps "cd /path && systemctl restart techrental"

# Check status
ssh user@vps "systemctl status techrental"

# View logs
ssh user@vps "journalctl -u techrental -f"
```

## 🎯 One-Line Deploy

```bash
mvn clean package -DskipTests && \
scp target/techrental-0.0.1-SNAPSHOT.jar root@160.191.245.242:/var/apps/techrental/ && \
ssh root@160.191.245.242 "cd /var/apps/techrental && systemctl restart techrental"
```

