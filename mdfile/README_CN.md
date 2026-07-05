# Banking System - 银行管理系统

这是一个基于Spring Boot的银行管理系统后端API，使用AWS RDS PostgreSQL作为数据库。

## 技术栈

- **Java**: 21
- **Spring Boot**: 4.0.0
- **数据库**: AWS RDS PostgreSQL
- **ORM**: Spring Data JPA (Hibernate)
- **构建工具**: Gradle
- **其他依赖**: 
  - Lombok (简化代码)
  - Spring Validation (数据验证)
  - HikariCP (连接池)

## 项目结构

```
src/main/java/com/banking/
├── entity/              # 实体类
│   ├── Account.java     # 账户实体
│   └── Transaction.java # 交易实体
├── repository/          # 数据访问层
│   ├── AccountRepository.java
│   └── TransactionRepository.java
├── service/             # 业务逻辑层
│   ├── AccountService.java
│   └── TransactionService.java
├── controller/          # 控制器层 (RESTful API)
│   ├── AccountController.java
│   └── TransactionController.java
├── dto/                 # 数据传输对象
│   └── ApiResponse.java # 统一响应格式
└── BankingApplication.java # 应用入口
```

## 配置步骤

### 1. 配置AWS RDS PostgreSQL数据库

#### 在AWS RDS创建PostgreSQL实例

1. 登录AWS控制台，进入RDS服务
2. 创建新的PostgreSQL数据库实例
3. 记录以下信息：
   - Endpoint (终端节点)
   - Port (端口，默认5432)
   - Master username (主用户名)
   - Master password (主密码)

#### 创建数据库

连接到PostgreSQL实例，创建数据库：

```sql
CREATE DATABASE banking_db;
```

### 2. 配置application.properties

修改 `src/main/resources/application.properties` 文件：

```properties
# 替换为你的AWS RDS信息
spring.datasource.url=jdbc:postgresql://your-rds-endpoint.region.rds.amazonaws.com:5432/banking_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

**示例：**
```properties
spring.datasource.url=jdbc:postgresql://banking-db.c9abc1defghi.us-east-1.rds.amazonaws.com:5432/banking_db
spring.datasource.username=postgres
spring.datasource.password=YourSecurePassword123
```

### 3. 确保安全组配置

在AWS RDS安全组中，确保允许来自你的IP地址的入站流量：

- **类型**: PostgreSQL
- **端口**: 5432
- **来源**: 你的IP地址或0.0.0.0/0（不推荐用于生产环境）

## 运行项目

### 使用Gradle运行

```bash
# 在项目根目录执行
./gradlew bootRun
```

### 使用IDE运行

在IDE中直接运行 `BankingApplication.java` 的main方法。

### 验证启动成功

应用启动后，你应该看到类似以下日志：

```
Started BankingApplication in X.XXX seconds
```

应用将在 `http://localhost:8080` 上运行。

## API测试

### 使用Postman测试

1. **导入Postman Collection**
   - 打开Postman
   - 点击 Import
   - 选择 `Banking_API.postman_collection.json` 文件
   - 导入成功后，你会看到所有的API请求

2. **查看测试文档**
   - 详细的API文档请参考 `POSTMAN_API_GUIDE.md`

### API端点概览

#### 账户管理 API

- `POST /api/accounts` - 创建账户
- `GET /api/accounts` - 查询所有账户
- `GET /api/accounts/{id}` - 根据ID查询账户
- `GET /api/accounts/name/{accountName}` - 根据账户名查询
- `GET /api/accounts/number/{accountNumber}` - 根据账号查询
- `PUT /api/accounts/{id}` - 更新账户
- `DELETE /api/accounts/{id}` - 删除账户

#### 交易管理 API

- `POST /api/transactions` - 创建交易记录
- `GET /api/transactions` - 查询所有交易记录
- `GET /api/transactions/{id}` - 根据ID查询交易
- `GET /api/transactions/transaction-id/{transactionId}` - 根据交易ID查询
- `GET /api/transactions/account/{accountNumber}` - 查询账户的所有交易
- `GET /api/transactions/type/{transactionType}` - 根据类型查询交易
- `PUT /api/transactions/{id}` - 更新交易记录
- `DELETE /api/transactions/{id}` - 删除交易记录

## 快速测试示例

### 1. 创建账户

```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountName": "张三",
    "accountNumber": "6222021234567890",
    "balance": 5000.00
  }'
```

**响应：**
```json
{
  "success": true,
  "message": "账户创建成功",
  "data": {
    "id": 1,
    "accountName": "张三",
    "accountNumber": "6222021234567890",
    "balance": 5000.00
  },
  "timestamp": "2024-11-24T10:30:00"
}
```

### 2. 创建交易记录

```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TXN20241124001",
    "accountNumber": "6222021234567890",
    "transactionType": "DEPOSIT",
    "amount": 1000.00,
    "balanceBefore": 5000.00,
    "balanceAfter": 6000.00
  }'
```

### 3. 查询所有账户

```bash
curl http://localhost:8080/api/accounts
```

## 数据库表结构

应用启动后，Hibernate会自动创建以下表结构：

### Account表

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键，自增 |
| account_name | VARCHAR(20) | 账户名，唯一 |
| account_number | VARCHAR(30) | 账号，唯一 |
| balance | DECIMAL(10,2) | 余额 |

### Transaction表

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | BIGINT | 主键，自增 |
| transaction_id | VARCHAR(50) | 交易ID，唯一 |
| account_number | VARCHAR(30) | 账号 |
| transaction_type | VARCHAR(20) | 交易类型 |
| amount | DECIMAL(18,2) | 交易金额 |
| balance_before | DECIMAL(18,2) | 交易前余额 |
| balance_after | DECIMAL(18,2) | 交易后余额 |

### 交易类型枚举

- `DEPOSIT` - 存款
- `WITHDRAW` - 取款
- `TRANSFER_IN` - 转入
- `TRANSFER_OUT` - 转出

## 常见问题

### 1. 无法连接到数据库

**问题：** `Connection refused` 或 `Connection timeout`

**解决方法：**
- 检查RDS实例是否正在运行
- 确认安全组配置允许你的IP访问
- 验证endpoint、端口、用户名和密码是否正确
- 检查VPC和子网配置

### 2. 表未创建

**问题：** 应用启动但表没有创建

**解决方法：**
- 确认 `spring.jpa.hibernate.ddl-auto=update` 配置
- 检查数据库用户是否有CREATE TABLE权限
- 查看应用启动日志中的错误信息

### 3. 端口冲突

**问题：** `Port 8080 is already in use`

**解决方法：**
- 在application.properties中修改端口：
  ```properties
  server.port=8081
  ```

## 生产环境建议

1. **安全性**
   - 不要在代码中硬编码数据库密码
   - 使用环境变量或AWS Secrets Manager
   - 限制RDS安全组的访问范围
   - 使用SSL连接数据库

2. **性能优化**
   - 调整Hikari连接池参数
   - 添加合适的数据库索引
   - 启用查询缓存

3. **配置优化**
   - 将 `spring.jpa.hibernate.ddl-auto` 改为 `validate` 或 `none`
   - 关闭SQL日志输出
   - 配置合适的日志级别

4. **监控**
   - 使用Spring Boot Actuator监控应用健康状态
   - 配置CloudWatch监控RDS性能

## 许可证

MIT License

## 联系方式

如有问题，请提交Issue或联系开发团队。

