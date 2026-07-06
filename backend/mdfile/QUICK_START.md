# 快速开始指南

本指南帮助你快速启动Banking System项目。

## 前置要求

确保你的系统已安装：
- ✅ Java 21 或更高版本
- ✅ Gradle 8.x (或使用项目自带的Gradle Wrapper)
- ✅ PostgreSQL 客户端工具 (用于连接AWS RDS)
- ✅ Postman (用于API测试)
- ✅ AWS账户 (用于创建RDS实例)

## 5分钟快速启动

### Step 1: 配置AWS RDS PostgreSQL数据库 (2分钟)

1. 登录AWS控制台，创建RDS PostgreSQL实例
2. 配置安全组，允许你的IP访问
3. 记录以下信息：
   - Endpoint: `xxx.rds.amazonaws.com`
   - 端口: `5432`
   - 用户名: `postgres`
   - 密码: `your_password`
   - 数据库名: `banking_db`

### Step 2: 配置application.properties (1分钟)

编辑 `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://your-endpoint.rds.amazonaws.com:5432/banking_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### Step 3: 启动应用 (1分钟)

```bash
# 在项目根目录执行
./gradlew bootRun
```

看到以下日志说明启动成功：
```
Started BankingApplication in X.XXX seconds
```

### Step 4: 测试API (1分钟)

**方法1: 使用Postman**
1. 打开Postman
2. 导入 `Banking_API.postman_collection.json`
3. 执行 "创建账户" 请求

**方法2: 使用curl**
```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountName": "测试账户",
    "accountNumber": "6222021234567890",
    "balance": 5000.00
  }'
```

成功响应：
```json
{
  "success": true,
  "message": "账户创建成功",
  "data": {
    "id": 1,
    "accountName": "测试账户",
    "accountNumber": "6222021234567890",
    "balance": 5000.00
  },
  "timestamp": "2024-11-24T10:30:00"
}
```

## 详细配置步骤

### 1. 创建AWS RDS PostgreSQL实例

#### 使用AWS控制台

1. 打开 [AWS RDS Console](https://console.aws.amazon.com/rds/)
2. 点击 "Create database"
3. 选择配置：
   - Engine: PostgreSQL
   - Version: 15.x 或更高
   - Template: Free tier (学习测试用) 或 Production
   - DB instance identifier: `banking-db`
   - Master username: `postgres`
   - Master password: 设置强密码
   - DB instance class: db.t3.micro (免费套餐)
   - Storage: 20 GB
   - VPC: 默认VPC
   - Public access: Yes (用于外部访问)
   - VPC security group: 创建新的或使用现有的

4. 点击 "Create database"
5. 等待实例创建完成（约5-10分钟）

#### 配置安全组

1. 进入RDS实例详情页
2. 点击 VPC security groups
3. 编辑入站规则，添加：
   - Type: PostgreSQL
   - Port: 5432
   - Source: 
     - 开发环境: My IP (推荐)
     - 或: 0.0.0.0/0 (允许所有IP，不推荐生产环境)

#### 创建数据库

使用PostgreSQL客户端连接并创建数据库：

```bash
# 使用psql连接
psql -h your-endpoint.rds.amazonaws.com -U postgres -d postgres

# 创建数据库
CREATE DATABASE banking_db;

# 退出
\q
```

### 2. 初始化数据库（可选）

如果想要手动创建表和插入测试数据：

```bash
# 执行初始化脚本
psql -h your-endpoint.rds.amazonaws.com -U postgres -d banking_db -f database_init.sql
```

**注意**: Spring Boot会自动创建表结构，所以这一步是可选的。

### 3. 配置应用

#### 方式1: 直接修改application.properties

```properties
spring.datasource.url=jdbc:postgresql://banking-db.c9abc1defghi.us-east-1.rds.amazonaws.com:5432/banking_db
spring.datasource.username=postgres
spring.datasource.password=YourSecurePassword123
```

#### 方式2: 使用环境变量（推荐用于生产环境）

1. 创建 `.env` 文件：
```bash
cp .env.example .env
```

2. 编辑 `.env` 文件：
```
DB_HOST=banking-db.c9abc1defghi.us-east-1.rds.amazonaws.com
DB_PORT=5432
DB_NAME=banking_db
DB_USERNAME=postgres
DB_PASSWORD=YourSecurePassword123
```

3. 修改 `application.properties`：
```properties
spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

### 4. 运行应用

#### 使用Gradle

```bash
# Unix/Mac
./gradlew bootRun

# Windows
gradlew.bat bootRun
```

#### 使用IDE

1. 在IDE中打开项目（IntelliJ IDEA推荐）
2. 找到 `BankingApplication.java`
3. 右键 → Run 'BankingApplication'

#### 构建JAR并运行

```bash
# 构建
./gradlew build

# 运行
java -jar build/libs/banking-0.0.1-SNAPSHOT.jar
```

### 5. 验证应用运行

#### 检查日志

看到以下内容表示成功：
```
2024-11-24 10:30:00.000  INFO --- [main] com.banking.BankingApplication : Started BankingApplication in 5.123 seconds
```

#### 测试健康检查

```bash
# 检查应用是否运行
curl http://localhost:8080/api/accounts
```

预期响应：
```json
{
  "success": true,
  "message": "查询成功",
  "data": [],
  "timestamp": "2024-11-24T10:30:00"
}
```

## 使用Postman测试

### 导入Collection

1. 打开Postman
2. 点击 **Import** 按钮
3. 选择 `Banking_API.postman_collection.json` 文件
4. 导入成功后，左侧会出现 "Banking API" 集合

### 设置环境变量

1. 点击右上角的 "Environment" 设置
2. 创建新环境或使用默认环境
3. 添加变量：
   - Variable: `base_url`
   - Initial Value: `http://localhost:8080`
   - Current Value: `http://localhost:8080`

### 执行测试

#### 测试流程1: 账户管理

1. **创建账户** - POST /api/accounts
2. **查询所有账户** - GET /api/accounts
3. **根据ID查询账户** - GET /api/accounts/1
4. **更新账户** - PUT /api/accounts/1
5. **删除账户** - DELETE /api/accounts/1

#### 测试流程2: 交易管理

1. **创建账户**（如果还没有）
2. **创建存款交易** - POST /api/transactions
3. **创建取款交易** - POST /api/transactions
4. **查询账户的所有交易** - GET /api/transactions/account/{accountNumber}
5. **根据类型查询交易** - GET /api/transactions/type/DEPOSIT

## 常见问题排查

### 问题1: 无法连接数据库

**错误信息**: `Connection refused` 或 `Timeout`

**解决方法**:
1. 检查RDS实例是否正在运行
2. 验证安全组配置是否正确
3. 确认endpoint、端口、用户名、密码是否正确
4. 使用psql测试连接：
   ```bash
   psql -h your-endpoint.rds.amazonaws.com -U postgres -d banking_db
   ```

### 问题2: 应用启动失败

**错误信息**: `Failed to configure a DataSource`

**解决方法**:
1. 检查 `application.properties` 配置是否正确
2. 确认PostgreSQL驱动是否已添加到依赖中
3. 验证数据库是否已创建

### 问题3: 端口冲突

**错误信息**: `Port 8080 was already in use`

**解决方法**:
1. 修改 `application.properties`：
   ```properties
   server.port=8081
   ```
2. 或者停止占用8080端口的进程：
   ```bash
   # Mac/Linux
   lsof -ti:8080 | xargs kill -9
   
   # Windows
   netstat -ano | findstr :8080
   taskkill /PID <PID> /F
   ```

### 问题4: 权限不足

**错误信息**: `Permission denied`

**解决方法**:
```bash
# 给gradlew添加执行权限
chmod +x gradlew
```

### 问题5: Gradle下载缓慢

**解决方法**:
使用国内镜像，编辑 `build.gradle.kts`:
```kotlin
repositories {
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    maven { url = uri("https://maven.aliyun.com/repository/spring") }
    mavenCentral()
}
```

## 下一步

✅ 应用已成功运行

现在你可以：

1. 📖 阅读 [README_CN.md](README_CN.md) 了解项目详情
2. 🏗️ 阅读 [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) 了解项目架构
3. 🧪 阅读 [POSTMAN_API_GUIDE.md](POSTMAN_API_GUIDE.md) 学习API使用
4. 💻 开始开发你的业务功能
5. 🚀 部署到生产环境

## 有用的命令

```bash
# 清理构建
./gradlew clean

# 构建项目
./gradlew build

# 运行测试
./gradlew test

# 运行应用
./gradlew bootRun

# 构建JAR包
./gradlew bootJar

# 查看依赖
./gradlew dependencies

# 查看任务列表
./gradlew tasks
```

## 获取帮助

- 查看项目文档：README_CN.md
- 查看API文档：POSTMAN_API_GUIDE.md
- 查看架构文档：PROJECT_STRUCTURE.md
- 查看数据库脚本：database_init.sql

## 联系支持

如有问题，请：
1. 检查日志文件
2. 查看常见问题部分
3. 提交Issue
4. 联系开发团队

---

**祝你开发顺利！** 🎉

