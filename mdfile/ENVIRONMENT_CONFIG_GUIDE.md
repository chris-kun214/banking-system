# 环境配置指南 - Environment Configuration Guide

本指南详细说明如何配置Banking System项目，以支持本地开发和生产环境部署。

## 📋 目录

- [配置文件结构](#配置文件结构)
- [本地开发环境配置](#本地开发环境配置)
- [生产环境配置 (EC2)](#生产环境配置-ec2)
- [环境切换方法](#环境切换方法)
- [常见问题](#常见问题)

---

## 配置文件结构

项目使用 Spring Profile 机制管理多环境配置：

```
src/main/resources/
├── application.yml           # 通用配置（所有环境共享）
├── application-local.yml     # 本地开发环境配置
└── application-prod.yml      # 生产环境配置 (EC2)
```

### 配置加载优先级

1. `application.yml` - 基础配置
2. `application-{profile}.yml` - 特定环境配置（会覆盖基础配置）

---

## 本地开发环境配置

### 前置条件

1. **安装 PostgreSQL**

   **macOS:**
   ```bash
   brew install postgresql@15
   brew services start postgresql@15
   ```

   **Ubuntu/Debian:**
   ```bash
   sudo apt-get update
   sudo apt-get install postgresql postgresql-contrib
   sudo systemctl start postgresql
   ```

   **Windows:**
   - 下载安装 [PostgreSQL](https://www.postgresql.org/download/windows/)

2. **创建本地数据库**

   ```bash
   # 连接到 PostgreSQL
   psql -U postgres
   
   # 创建数据库
   CREATE DATABASE banking_db;
   
   # 设置密码（如果需要）
   ALTER USER postgres PASSWORD 'password';
   
   # 退出
   \q
   ```

3. **验证连接**

   ```bash
   psql -U postgres -d banking_db -h localhost -p 5432
   ```

### 配置说明

`application-local.yml` 配置：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/banking_db
    username: postgres
    password: password
  jpa:
    hibernate:
      ddl-auto: update  # 自动创建/更新表结构
    show-sql: true      # 显示SQL语句
```

### 启动本地环境

#### 方法 1: 使用 Gradle

```bash
# 在项目根目录执行
./gradlew bootRun --args='--spring.profiles.active=local'
```

#### 方法 2: 使用 IDE (IntelliJ IDEA)

1. 打开 `Run/Debug Configurations`
2. 找到 `BankingApplication`
3. 在 `VM options` 中添加：
   ```
   -Dspring.profiles.active=local
   ```
4. 点击 `Run`

#### 方法 3: 使用环境变量

```bash
# 设置环境变量
export SPRING_PROFILES_ACTIVE=local

# 运行应用
./gradlew bootRun
```

#### 方法 4: 构建 JAR 后运行

```bash
# 构建 JAR
./gradlew clean bootJar

# 运行
java -jar build/libs/banking-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### 验证本地环境

```bash
# 检查应用是否启动成功
curl http://localhost:8080/api/accounts

# 预期响应
{
  "success": true,
  "message": "查询成功",
  "data": [],
  "timestamp": "2024-12-23T10:30:00"
}
```

---

## 生产环境配置 (EC2)

### 架构说明

```
┌─────────────────┐
│   Internet      │
└────────┬────────┘
         │
┌────────▼────────┐
│  EC2 Instance   │
│  (Spring Boot)  │
│  Port: 8080     │
└────────┬────────┘
         │
┌────────▼────────┐
│   AWS RDS       │
│  PostgreSQL     │
│  Port: 5432     │
└─────────────────┘
```

### Step 1: 创建 AWS RDS PostgreSQL 实例

1. **登录 AWS 控制台**
   - 进入 [RDS Console](https://console.aws.amazon.com/rds/)

2. **创建数据库**
   ```
   Engine: PostgreSQL 15.x
   Template: Free tier / Production
   DB instance identifier: banking-rds
   Master username: postgres
   Master password: [设置强密码]
   DB instance class: db.t3.micro (Free tier)
   Storage: 20 GB
   VPC: 默认VPC
   Public access: Yes（如果EC2需要访问）
   Initial database name: banking_db
   ```

3. **配置安全组**
   - 进入 RDS 实例的安全组
   - 添加入站规则：
     ```
     Type: PostgreSQL
     Port: 5432
     Source: EC2实例的安全组ID 或 0.0.0.0/0（测试用）
     ```

4. **记录连接信息**
   ```
   Endpoint: banking-rds.xxxxxx.region.rds.amazonaws.com
   Port: 5432
   Username: postgres
   Password: [你设置的密码]
   Database: banking_db
   ```

### Step 2: 准备 EC2 实例

1. **启动 EC2 实例**
   ```
   AMI: Amazon Linux 2 或 Ubuntu 22.04
   Instance type: t2.micro (Free tier)
   Security group: 允许 SSH (22) 和 HTTP (8080)
   ```

2. **连接到 EC2**
   ```bash
   ssh -i your-key.pem ec2-user@your-ec2-ip
   ```

3. **安装 Java 21**

   **Amazon Linux 2:**
   ```bash
   sudo yum update -y
   sudo yum install java-21-amazon-corretto -y
   java -version
   ```

   **Ubuntu:**
   ```bash
   sudo apt update
   sudo apt install openjdk-21-jdk -y
   java -version
   ```

4. **创建应用目录**
   ```bash
   sudo mkdir -p /opt/banking
   sudo mkdir -p /var/log/banking
   sudo chown $USER:$USER /opt/banking
   sudo chown $USER:$USER /var/log/banking
   ```

### Step 3: 配置环境变量

在 EC2 上创建环境配置文件：

```bash
# 创建环境配置文件
sudo nano /etc/profile.d/banking.sh
```

添加以下内容（**替换为你的实际值**）：

```bash
# Banking System 生产环境变量
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL=jdbc:postgresql://banking-rds.xxxxxx.region.rds.amazonaws.com:5432/banking_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=your_secure_password
export JWT_SECRET=your_very_long_and_secure_jwt_secret_key_here
export SERVER_PORT=8080
```

使配置生效：

```bash
source /etc/profile.d/banking.sh

# 验证环境变量
echo $SPRING_PROFILES_ACTIVE
echo $SPRING_DATASOURCE_URL
```

### Step 4: 初始化数据库

首次部署需要初始化数据库表结构：

**方法 1: 使用 psql**

```bash
# 安装 PostgreSQL 客户端
sudo yum install postgresql -y  # Amazon Linux
sudo apt install postgresql-client -y  # Ubuntu

# 连接到 RDS
psql -h banking-rds.xxxxxx.region.rds.amazonaws.com \
     -U postgres \
     -d banking_db

# 在 psql 中执行
# 应用会自动创建表，所以这一步可选
```

**方法 2: 临时使用 ddl-auto=update**

首次部署时，可以临时在生产环境使用 `update` 模式：

```bash
# 临时覆盖配置
export HIBERNATE_DDL_AUTO=update

# 启动应用（会自动创建表）
java -jar banking-0.0.1-SNAPSHOT.jar
```

启动成功并创建表后，停止应用，然后：

```bash
# 改回 validate 模式
export HIBERNATE_DDL_AUTO=validate
```

### Step 5: 部署应用

1. **在本地构建 JAR 包**

   ```bash
   # 在你的开发机器上
   cd /Users/chrischen/Desktop/bank_project/banking_backend
   ./gradlew clean bootJar
   ```

2. **上传到 EC2**

   ```bash
   # 上传 JAR 文件
   scp -i your-key.pem \
       build/libs/banking-0.0.1-SNAPSHOT.jar \
       ec2-user@your-ec2-ip:/opt/banking/
   ```

3. **在 EC2 上启动应用**

   ```bash
   # 连接到 EC2
   ssh -i your-key.pem ec2-user@your-ec2-ip
   
   # 进入应用目录
   cd /opt/banking
   
   # 启动应用（前台运行）
   java -jar banking-0.0.1-SNAPSHOT.jar
   ```

4. **配置为后台服务 (推荐)**

   创建 systemd 服务：

   ```bash
   sudo nano /etc/systemd/system/banking.service
   ```

   添加以下内容：

   ```ini
   [Unit]
   Description=Banking System Service
   After=network.target

   [Service]
   Type=simple
   User=ec2-user
   WorkingDirectory=/opt/banking
   EnvironmentFile=/etc/profile.d/banking.sh
   ExecStart=/usr/bin/java -jar /opt/banking/banking-0.0.1-SNAPSHOT.jar
   Restart=on-failure
   RestartSec=10
   StandardOutput=journal
   StandardError=journal
   SyslogIdentifier=banking

   [Install]
   WantedBy=multi-user.target
   ```

   启动服务：

   ```bash
   # 重载 systemd 配置
   sudo systemctl daemon-reload
   
   # 启动服务
   sudo systemctl start banking
   
   # 设置开机自启
   sudo systemctl enable banking
   
   # 查看状态
   sudo systemctl status banking
   
   # 查看日志
   sudo journalctl -u banking -f
   ```

### Step 6: 验证部署

```bash
# 在 EC2 上测试
curl http://localhost:8080/api/accounts

# 从外部测试（需要配置安全组允许8080端口）
curl http://your-ec2-public-ip:8080/api/accounts
```

### Step 7: 配置 Nginx 反向代理 (可选但推荐)

1. **安装 Nginx**

   ```bash
   sudo yum install nginx -y  # Amazon Linux
   sudo apt install nginx -y  # Ubuntu
   ```

2. **配置 Nginx**

   ```bash
   sudo nano /etc/nginx/conf.d/banking.conf
   ```

   添加：

   ```nginx
   server {
       listen 80;
       server_name your-domain.com;  # 或使用 EC2 公网IP
   
       location / {
           proxy_pass http://localhost:8080;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
           proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
           proxy_set_header X-Forwarded-Proto $scheme;
       }
   }
   ```

3. **启动 Nginx**

   ```bash
   sudo systemctl start nginx
   sudo systemctl enable nginx
   
   # 验证配置
   sudo nginx -t
   ```

4. **更新 EC2 安全组**
   - 允许 HTTP (80) 入站
   - 可以移除 8080 端口的公网访问

---

## 环境切换方法

### 方法对比

| 方法 | 优点 | 缺点 | 推荐场景 |
|------|------|------|----------|
| 命令行参数 | 灵活，一次性使用 | 每次都要输入 | 临时测试 |
| 环境变量 | 持久化，适合CI/CD | 需要配置环境 | 生产部署 |
| IDE配置 | 方便开发 | 仅限IDE使用 | 本地开发 |
| 配置文件 | 代码化管理 | 敏感信息风险 | 不推荐 |

### 快速切换

```bash
# 本地开发
./gradlew bootRun --args='--spring.profiles.active=local'

# 生产环境
java -jar banking.jar --spring.profiles.active=prod

# 使用环境变量（推荐）
export SPRING_PROFILES_ACTIVE=local  # 或 prod
./gradlew bootRun
```

---

## 常见问题

### 1. 本地无法连接数据库

**错误:** `Connection refused` 或 `FATAL: password authentication failed`

**解决方法:**

```bash
# 检查 PostgreSQL 是否运行
sudo systemctl status postgresql  # Linux
brew services list  # macOS

# 检查端口是否监听
sudo lsof -i :5432

# 重置密码
psql -U postgres
ALTER USER postgres PASSWORD 'password';

# 检查 pg_hba.conf 配置
# 确保有类似这样的行：
# local   all   postgres   md5
```

### 2. EC2 无法连接 RDS

**错误:** `Connection timeout` 或 `Connection refused`

**解决方法:**

1. **检查 RDS 安全组**
   - 确保允许 EC2 的安全组或 IP 访问
   - 端口 5432 是否开放

2. **检查 RDS 可访问性**
   ```bash
   # 在 EC2 上测试连接
   telnet banking-rds.xxxxxx.region.rds.amazonaws.com 5432
   
   # 或使用 psql
   psql -h banking-rds.xxxxxx.region.rds.amazonaws.com \
        -U postgres \
        -d banking_db
   ```

3. **检查 VPC 配置**
   - EC2 和 RDS 是否在同一 VPC
   - 子网路由表配置是否正确

### 3. 环境变量未生效

**问题:** 应用使用了错误的配置

**解决方法:**

```bash
# 验证环境变量
env | grep SPRING

# 确保环境变量已加载
source /etc/profile.d/banking.sh

# 查看应用启动时使用的配置
java -jar banking.jar --spring.profiles.active=prod --debug
```

### 4. JWT 认证失败

**错误:** `Invalid JWT token` 或 `JWT expired`

**解决方法:**

1. **确保 JWT_SECRET 在所有环境中一致**
   ```bash
   echo $JWT_SECRET
   ```

2. **生成新的安全密钥**
   ```bash
   # 生成 256-bit base64 编码的密钥
   openssl rand -base64 32
   ```

3. **更新环境变量**
   ```bash
   export JWT_SECRET=your_new_secret_key
   ```

### 5. 生产环境表不存在

**错误:** `Table "account" doesn't exist`

**解决方法:**

```bash
# 方法1: 临时使用 update 模式创建表
export HIBERNATE_DDL_AUTO=update
java -jar banking.jar
# 启动后停止，然后改回 validate

# 方法2: 手动执行 SQL
psql -h your-rds-endpoint \
     -U postgres \
     -d banking_db \
     -f database_init.sql
```

### 6. 端口被占用

**错误:** `Port 8080 is already in use`

**解决方法:**

```bash
# 查找占用端口的进程
lsof -ti:8080

# 杀死进程
kill -9 $(lsof -ti:8080)

# 或更换端口
export SERVER_PORT=8081
```

### 7. 内存不足

**错误:** `OutOfMemoryError`

**解决方法:**

```bash
# 限制 JVM 堆内存
java -Xmx512m -Xms256m -jar banking.jar

# EC2 t2.micro 推荐配置
java -Xmx400m -Xms256m -jar banking.jar
```

---

## 安全最佳实践

### 1. 不要在代码中硬编码敏感信息

❌ **错误做法:**
```yaml
spring:
  datasource:
    password: mypassword123
```

✅ **正确做法:**
```yaml
spring:
  datasource:
    password: ${SPRING_DATASOURCE_PASSWORD}
```

### 2. 使用强密码和密钥

```bash
# 生成强密码
openssl rand -base64 32

# JWT 密钥建议至少 256-bit
openssl rand -base64 32
```

### 3. 限制数据库访问

- RDS 安全组只允许特定 EC2 实例访问
- 不要设置 `0.0.0.0/0` 除非必要

### 4. 使用 HTTPS

- 配置 SSL/TLS 证书
- 使用 Let's Encrypt 免费证书
- Nginx 配置 HTTPS

### 5. 定期备份

```bash
# RDS 自动备份配置
# 在 AWS 控制台设置：
# - Backup retention period: 7 days
# - Backup window: 凌晨时间段
```

---

## 监控和日志

### 查看应用日志

```bash
# systemd 服务日志
sudo journalctl -u banking -f

# 文件日志
tail -f /var/log/banking/application.log

# 查看最近 100 行
tail -100 /var/log/banking/application.log
```

### 监控 RDS

- AWS CloudWatch 监控 CPU、内存、连接数
- 设置告警阈值

### 监控 EC2

```bash
# 查看 CPU 和内存
top

# 查看磁盘使用
df -h

# 查看网络连接
netstat -tuln | grep 8080
```

---

## 更新部署流程

### 滚动更新（零停机）

1. **构建新版本**
   ```bash
   ./gradlew clean bootJar
   ```

2. **上传到 EC2**
   ```bash
   scp -i your-key.pem \
       build/libs/banking-0.0.1-SNAPSHOT.jar \
       ec2-user@your-ec2-ip:/opt/banking/banking-new.jar
   ```

3. **平滑重启**
   ```bash
   ssh ec2-user@your-ec2-ip
   cd /opt/banking
   
   # 备份旧版本
   mv banking-0.0.1-SNAPSHOT.jar banking-backup.jar
   mv banking-new.jar banking-0.0.1-SNAPSHOT.jar
   
   # 重启服务
   sudo systemctl restart banking
   
   # 验证
   sudo systemctl status banking
   curl http://localhost:8080/api/accounts
   ```

---

## 总结

### 本地开发工作流

```bash
# 1. 启动本地 PostgreSQL
brew services start postgresql

# 2. 运行应用
./gradlew bootRun --args='--spring.profiles.active=local'

# 3. 测试 API
curl http://localhost:8080/api/accounts
```

### 生产部署工作流

```bash
# 1. 本地构建
./gradlew clean bootJar

# 2. 上传到 EC2
scp build/libs/banking-0.0.1-SNAPSHOT.jar ec2-user@ec2-ip:/opt/banking/

# 3. SSH 到 EC2
ssh ec2-user@ec2-ip

# 4. 启动服务
sudo systemctl restart banking

# 5. 验证
curl http://localhost:8080/api/accounts
```

---

## 相关文档

- [README_CN.md](README_CN.md) - 项目概述
- [QUICK_START.md](QUICK_START.md) - 快速开始
- [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) - 项目结构
- [POSTMAN_API_GUIDE.md](POSTMAN_API_GUIDE.md) - API 使用指南
- [JWT_AUTH_GUIDE.md](JWT_AUTH_GUIDE.md) - JWT 认证指南

---

**祝部署顺利！** 🚀

如有问题，请查看 [常见问题](#常见问题) 或提交 Issue。

