# 项目结构说明

## 整体架构

本项目采用经典的三层架构模式：

```
Controller层 (控制器)
    ↓
Service层 (业务逻辑)
    ↓
Repository层 (数据访问)
    ↓
Database (AWS RDS PostgreSQL)
```

## 详细文件结构

```
banking/
├── src/
│   ├── main/
│   │   ├── java/com/banking/
│   │   │   ├── BankingApplication.java          # Spring Boot应用入口
│   │   │   ├── entity/                          # 实体层 (Entity)
│   │   │   │   ├── Account.java                # 账户实体
│   │   │   │   └── Transaction.java            # 交易实体
│   │   │   ├── dto/                            # 数据传输对象 (DTO)
│   │   │   │   └── ApiResponse.java            # 统一API响应格式
│   │   │   ├── repository/                     # 数据访问层 (Repository)
│   │   │   │   ├── AccountRepository.java      # 账户数据访问
│   │   │   │   └── TransactionRepository.java  # 交易数据访问
│   │   │   ├── service/                        # 业务逻辑层 (Service)
│   │   │   │   ├── AccountService.java         # 账户业务逻辑
│   │   │   │   └── TransactionService.java     # 交易业务逻辑
│   │   │   ├── controller/                     # 控制器层 (Controller)
│   │   │   │   ├── AccountController.java      # 账户API控制器
│   │   │   │   └── TransactionController.java  # 交易API控制器
│   │   │   └── exception/                      # 异常处理
│   │   │       └── GlobalExceptionHandler.java # 全局异常处理器
│   │   └── resources/
│   │       ├── application.properties          # 应用配置文件
│   │       ├── static/                         # 静态资源目录
│   │       └── templates/                      # 模板目录
│   └── test/                                   # 测试代码
│       └── java/com/kun/banking/
│           └── BankingApplicationTests.java
├── build.gradle.kts                            # Gradle构建配置
├── settings.gradle.kts                         # Gradle设置
├── gradlew                                     # Gradle Wrapper (Unix)
├── gradlew.bat                                 # Gradle Wrapper (Windows)
├── README_CN.md                                # 项目说明文档
├── POSTMAN_API_GUIDE.md                        # Postman测试指南
├── Banking_API.postman_collection.json         # Postman集合文件
├── database_init.sql                           # 数据库初始化脚本
└── .env.example                                # 环境变量示例
```

## 各层职责说明

### 1. Entity层 (实体层)

**位置**: `src/main/java/com/banking/entity/`

**职责**:
- 定义数据库表对应的Java对象
- 使用JPA注解映射数据库表结构
- 包含业务数据的基本属性和验证规则

**文件**:
- `Account.java` - 账户实体
  - 字段: id, accountName, accountNumber, balance
  - 注解: @Entity, @Table, @Id, @Column等
  
- `Transaction.java` - 交易实体
  - 字段: id, transactionId, accountNumber, transactionType, amount等
  - 包含交易类型枚举: DEPOSIT, WITHDRAW, TRANSFER_IN, TRANSFER_OUT

### 2. DTO层 (数据传输对象层)

**位置**: `src/main/java/com/banking/dto/`

**职责**:
- 定义API请求和响应的数据结构
- 统一API响应格式
- 与Entity分离，提供更灵活的数据传输

**文件**:
- `ApiResponse.java` - 统一API响应格式
  - 包含: success, message, data, timestamp
  - 提供静态工具方法: success(), error()

### 3. Repository层 (数据访问层)

**位置**: `src/main/java/com/banking/repository/`

**职责**:
- 定义数据库访问接口
- 继承JpaRepository，获得基础CRUD操作
- 定义自定义查询方法

**文件**:
- `AccountRepository.java`
  - 基础方法: save(), findById(), findAll(), deleteById()
  - 自定义方法: findByAccountName(), findByAccountNumber()等
  
- `TransactionRepository.java`
  - 基础方法: save(), findById(), findAll(), deleteById()
  - 自定义方法: findByAccountNumber(), findByTransactionType()等

### 4. Service层 (业务逻辑层)

**位置**: `src/main/java/com/banking/service/`

**职责**:
- 实现核心业务逻辑
- 调用Repository层进行数据操作
- 处理业务规则和验证
- 事务管理

**文件**:
- `AccountService.java`
  - 方法: createAccount(), getAccountById(), updateAccount()等
  - 业务逻辑: 检查账户唯一性、验证余额等
  
- `TransactionService.java`
  - 方法: createTransaction(), getTransactionById()等
  - 业务逻辑: 记录交易、验证交易类型等

### 5. Controller层 (控制器层)

**位置**: `src/main/java/com/banking/controller/`

**职责**:
- 接收HTTP请求
- 调用Service层处理业务
- 返回统一格式的响应
- 定义RESTful API端点

**文件**:
- `AccountController.java`
  - 端点: POST /api/accounts, GET /api/accounts/{id}等
  - 返回ApiResponse格式的响应
  
- `TransactionController.java`
  - 端点: POST /api/transactions, GET /api/transactions/{id}等
  - 返回ApiResponse格式的响应

### 6. Exception层 (异常处理层)

**位置**: `src/main/java/com/banking/exception/`

**职责**:
- 全局异常捕获和处理
- 统一错误响应格式
- 记录异常日志

**文件**:
- `GlobalExceptionHandler.java`
  - 处理验证异常 (MethodArgumentNotValidException)
  - 处理运行时异常 (RuntimeException)
  - 处理通用异常 (Exception)

## 数据流转示例

### 创建账户的完整流程：

1. **客户端请求**
   ```
   POST /api/accounts
   Body: {"accountName": "张三", "accountNumber": "123456", "balance": 1000}
   ```

2. **Controller层** (`AccountController.java`)
   - 接收HTTP POST请求
   - 验证请求参数 (@Valid)
   - 调用Service层

3. **Service层** (`AccountService.java`)
   - 检查账户名是否已存在
   - 检查账号是否已存在
   - 调用Repository层保存数据

4. **Repository层** (`AccountRepository.java`)
   - 执行数据库INSERT操作
   - 返回保存后的实体对象

5. **返回响应**
   ```json
   {
     "success": true,
     "message": "账户创建成功",
     "data": {
       "id": 1,
       "accountName": "张三",
       "accountNumber": "123456",
       "balance": 1000
     },
     "timestamp": "2024-11-24T10:30:00"
   }
   ```

## 技术栈详解

### 核心框架
- **Spring Boot 4.0.0** - 应用框架
- **Spring Data JPA** - ORM框架
- **Hibernate** - JPA实现
- **Spring Web MVC** - Web框架

### 数据库
- **PostgreSQL** - 关系型数据库
- **AWS RDS** - 云数据库服务
- **HikariCP** - 连接池

### 工具库
- **Lombok** - 简化Java代码
  - @Data: 自动生成getter/setter
  - @RequiredArgsConstructor: 自动生成构造函数
  - @NoArgsConstructor: 无参构造函数
  - @AllArgsConstructor: 全参构造函数

- **Jakarta Validation** - 数据验证
  - @NotBlank: 非空验证
  - @Size: 长度验证
  - @Valid: 启用验证

### 构建工具
- **Gradle 8.x** - 构建工具
- **Java 21** - 编程语言

## 配置文件说明

### application.properties
应用的核心配置文件，包含：
- 数据库连接配置
- JPA/Hibernate配置
- 连接池配置
- 服务器端口配置
- 日志配置

### build.gradle.kts
Gradle构建配置，定义：
- 项目依赖
- 插件配置
- Java版本
- 编译选项

## 测试文档

### POSTMAN_API_GUIDE.md
详细的API测试指南，包含：
- 所有API端点说明
- 请求参数示例
- 响应格式说明
- 测试步骤

### Banking_API.postman_collection.json
Postman集合文件，可直接导入Postman使用，包含：
- 所有API的预配置请求
- 环境变量配置
- 测试用例

### database_init.sql
数据库初始化脚本，包含：
- 表结构创建
- 索引创建
- 测试数据插入
- 查询验证语句

## 最佳实践

### 1. 分层架构优势
- **解耦**: 各层职责明确，修改一层不影响其他层
- **可测试**: 每层可独立进行单元测试
- **可维护**: 代码结构清晰，易于维护和扩展
- **可复用**: Service层可被多个Controller复用

### 2. RESTful API设计
- 使用HTTP方法表达操作: GET(查询), POST(创建), PUT(更新), DELETE(删除)
- 资源命名清晰: /api/accounts, /api/transactions
- 统一响应格式: 所有API返回相同结构的响应

### 3. 异常处理
- 全局异常处理器统一处理异常
- 参数验证自动处理
- 友好的错误信息返回

### 4. 数据验证
- 实体层使用注解验证
- Service层进行业务逻辑验证
- Controller层启用自动验证

## 扩展建议

### 短期扩展
1. 添加分页查询支持
2. 添加排序和过滤功能
3. 添加更多自定义查询方法
4. 添加单元测试和集成测试

### 中期扩展
1. 添加用户认证和授权 (Spring Security)
2. 添加API文档 (Swagger/OpenAPI)
3. 添加缓存机制 (Redis)
4. 添加消息队列 (RabbitMQ/Kafka)

### 长期扩展
1. 微服务架构改造
2. 添加监控和日志系统
3. 实现分布式事务
4. 添加容器化部署 (Docker/Kubernetes)

## 总结

本项目采用清晰的分层架构，遵循Spring Boot最佳实践，提供完整的RESTful API，易于理解、维护和扩展。通过合理的代码组织和充分的文档说明，使项目具有良好的可读性和可维护性。

