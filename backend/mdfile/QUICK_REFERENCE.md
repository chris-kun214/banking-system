# 快速参考指南 - Quick Reference

本文档提供常用命令的快速参考。

---

## 🚀 快速启动

### 本地开发环境

```bash
# 1. 确保 PostgreSQL 运行中
brew services start postgresql  # macOS
sudo systemctl start postgresql  # Linux

# 2. 创建数据库（首次运行）
psql -U postgres -c "CREATE DATABASE banking_db;"

# 3. 启动应用（本地环境）
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 生产环境（EC2）

```bash
# 1. 设置环境变量
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL=jdbc:postgresql://your-rds-endpoint:5432/banking_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=your_password
export JWT_SECRET=your_secure_secret

# 2. 启动应用
java -jar banking-0.0.1-SNAPSHOT.jar
```

---

## 📦 构建和运行

### Gradle 命令

```bash
# 清理构建
./gradlew clean

# 编译代码
./gradlew build

# 构建 JAR（不运行测试）
./gradlew bootJar -x test

# 运行测试
./gradlew test

# 运行应用
./gradlew bootRun

# 查看依赖
./gradlew dependencies

# 查看所有任务
./gradlew tasks
```

### JAR 运行

```bash
# 构建 JAR
./gradlew bootJar

# 运行 JAR（本地环境）
java -jar build/libs/banking-0.0.1-SNAPSHOT.jar --spring.profiles.active=local

# 运行 JAR（生产环境）
java -jar build/libs/banking-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# 指定内存限制
java -Xmx512m -Xms256m -jar banking-0.0.1-SNAPSHOT.jar
```

---

## 🔄 环境切换

### 方法 1: 命令行参数

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
./gradlew bootRun --args='--spring.profiles.active=prod'
```

### 方法 2: 环境变量

```bash
# 设置环境变量
export SPRING_PROFILES_ACTIVE=local

# 运行应用
./gradlew bootRun
```

### 方法 3: IDE 配置（IntelliJ IDEA）

1. Run → Edit Configurations
2. VM options: `-Dspring.profiles.active=local`
3. Run

---

## 🗄️ 数据库操作

### PostgreSQL 本地操作

```bash
# 启动 PostgreSQL
brew services start postgresql  # macOS
sudo systemctl start postgresql  # Linux

# 停止 PostgreSQL
brew services stop postgresql  # macOS
sudo systemctl stop postgresql  # Linux

# 连接数据库
psql -U postgres -d banking_db

# 创建数据库
psql -U postgres -c "CREATE DATABASE banking_db;"

# 删除数据库（危险操作！）
psql -U postgres -c "DROP DATABASE banking_db;"

# 执行 SQL 文件
psql -U postgres -d banking_db -f database_init.sql

# 查看所有数据库
psql -U postgres -c "\l"

# 查看所有表
psql -U postgres -d banking_db -c "\dt"
```

### AWS RDS 操作

```bash
# 连接到 RDS
psql -h your-rds-endpoint.region.rds.amazonaws.com \
     -U postgres \
     -d banking_db

# 测试连接
telnet your-rds-endpoint.region.rds.amazonaws.com 5432

# 使用环境变量连接
psql -h $RDS_ENDPOINT -U $DB_USER -d $DB_NAME
```

---

## 🔐 JWT 密钥生成

```bash
# 生成 256-bit Base64 编码的密钥
openssl rand -base64 32

# 生成 512-bit Base64 编码的密钥（更安全）
openssl rand -base64 64
```

---

## 🧪 API 测试

### 使用 curl

```bash
# 健康检查
curl http://localhost:8080/api/accounts

# 创建账户
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountName": "张三",
    "accountNumber": "6222021234567890",
    "balance": 5000.00
  }'

# 查询所有账户
curl http://localhost:8080/api/accounts

# 根据 ID 查询账户
curl http://localhost:8080/api/accounts/1

# 更新账户
curl -X PUT http://localhost:8080/api/accounts/1 \
  -H "Content-Type: application/json" \
  -d '{
    "accountName": "张三-已更新",
    "accountNumber": "6222021234567890",
    "balance": 6000.00
  }'

# 删除账户
curl -X DELETE http://localhost:8080/api/accounts/1
```

### 使用 Postman

```bash
# 导入 Collection
1. 打开 Postman
2. Import → Banking_API_JWT.postman_collection.json
3. 设置环境变量 base_url = http://localhost:8080
4. 开始测试
```

---

## 🚢 部署到 EC2

### 本地准备

```bash
# 1. 构建 JAR
./gradlew clean bootJar

# 2. 上传到 EC2
scp -i your-key.pem \
    build/libs/banking-0.0.1-SNAPSHOT.jar \
    ec2-user@your-ec2-ip:/opt/banking/
```

### EC2 操作

```bash
# 1. SSH 连接
ssh -i your-key.pem ec2-user@your-ec2-ip

# 2. 设置环境变量（首次部署）
sudo nano /etc/profile.d/banking.sh
# 添加环境变量后保存

# 3. 加载环境变量
source /etc/profile.d/banking.sh

# 4. 启动应用（前台）
cd /opt/banking
java -jar banking-0.0.1-SNAPSHOT.jar

# 5. 启动应用（后台）
nohup java -jar banking-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

# 6. 查看日志
tail -f app.log
```

### 使用 systemd 服务

```bash
# 启动服务
sudo systemctl start banking

# 停止服务
sudo systemctl stop banking

# 重启服务
sudo systemctl restart banking

# 查看状态
sudo systemctl status banking

# 查看日志
sudo journalctl -u banking -f

# 查看最近 100 行日志
sudo journalctl -u banking -n 100

# 开机自启
sudo systemctl enable banking

# 禁用开机自启
sudo systemctl disable banking
```

---

## 📊 监控和日志

### 应用日志

```bash
# 实时查看日志
tail -f /var/log/banking/application.log

# 查看最近 100 行
tail -100 /var/log/banking/application.log

# 搜索错误
grep -i error /var/log/banking/application.log

# systemd 服务日志
sudo journalctl -u banking -f
```

### 系统监控

```bash
# CPU 和内存使用
top

# 磁盘使用
df -h

# 内存详情
free -h

# 网络连接
netstat -tuln | grep 8080

# 查看进程
ps aux | grep java

# 查看端口占用
lsof -i :8080
```

---

## 🛠️ 常用故障排查

### 检查应用状态

```bash
# 应用是否运行
curl http://localhost:8080/api/accounts

# 检查端口
lsof -i :8080
netstat -tuln | grep 8080

# 检查 Java 进程
ps aux | grep java
```

### 数据库连接测试

```bash
# 测试 PostgreSQL 连接
psql -h localhost -U postgres -d banking_db

# 测试 RDS 连接
psql -h your-rds-endpoint -U postgres -d banking_db

# 测试端口可达性
telnet localhost 5432
telnet your-rds-endpoint 5432

# 使用 nc 测试
nc -zv localhost 5432
nc -zv your-rds-endpoint 5432
```

### 清理和重启

```bash
# 停止所有 Java 进程（危险！）
pkill -9 java

# 杀死特定端口的进程
kill -9 $(lsof -ti:8080)

# 清理 Gradle 缓存
./gradlew clean

# 清理构建目录
rm -rf build/
```

---

## 🔒 安全检查清单

- [ ] 数据库密码已更换为强密码
- [ ] JWT 密钥已更换为安全密钥
- [ ] 生产环境不使用硬编码配置
- [ ] RDS 安全组限制了访问 IP
- [ ] EC2 安全组只开放必要端口
- [ ] 生产环境使用 `ddl-auto=validate`
- [ ] 日志级别设置为 INFO 或 WARN
- [ ] 定期备份数据库
- [ ] 配置了 HTTPS（推荐）

---

## 📚 配置文件位置

```
banking_backend/
├── src/main/resources/
│   ├── application.yml           # 通用配置
│   ├── application-local.yml     # 本地环境
│   └── application-prod.yml      # 生产环境
├── env.example                    # 环境变量模板
├── ENVIRONMENT_CONFIG_GUIDE.md   # 详细配置指南
└── QUICK_REFERENCE.md            # 本文档
```

---

## 🆘 获取帮助

### 查看文档

- **环境配置详细指南**: [ENVIRONMENT_CONFIG_GUIDE.md](ENVIRONMENT_CONFIG_GUIDE.md)
- **项目概述**: [README_CN.md](README_CN.md)
- **快速开始**: [QUICK_START.md](QUICK_START.md)
- **API 使用**: [POSTMAN_API_GUIDE.md](POSTMAN_API_GUIDE.md)
- **JWT 认证**: [JWT_AUTH_GUIDE.md](JWT_AUTH_GUIDE.md)

### 常见错误

| 错误信息 | 可能原因 | 解决方法 |
|---------|---------|---------|
| `Connection refused` | 数据库未启动 | 启动 PostgreSQL |
| `Port already in use` | 端口被占用 | 更换端口或杀死进程 |
| `Authentication failed` | 密码错误 | 检查数据库密码 |
| `Table doesn't exist` | 表未创建 | 使用 `ddl-auto=update` |
| `OutOfMemoryError` | 内存不足 | 增加 JVM 堆内存 |

---

**快速参考完毕！** ⚡

需要详细说明请查看 [ENVIRONMENT_CONFIG_GUIDE.md](ENVIRONMENT_CONFIG_GUIDE.md)

