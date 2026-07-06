# Banking System - 项目导航索引

欢迎使用Banking System！这是你的项目导航指南。

## 🚀 快速开始

**第一次使用？** 从这里开始：
1. 📖 阅读 [QUICK_START.md](QUICK_START.md) - **5分钟快速启动**
2. 🧪 使用 [Postman Collection](Banking_API.postman_collection.json) 测试API
3. 📚 查看 [POSTMAN_API_GUIDE.md](POSTMAN_API_GUIDE.md) 了解API详情

## 📚 文档导航

### 核心文档
| 文档 | 用途 | 适合人群 |
|------|------|----------|
| [QUICK_START.md](QUICK_START.md) | 5分钟快速启动指南 | 新手必读 |
| [README_CN.md](README_CN.md) | 完整项目说明文档 | 所有开发者 |
| [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) | 项目架构详解 | 架构师、高级开发者 |
| [POSTMAN_API_GUIDE.md](POSTMAN_API_GUIDE.md) | API测试完整指南 | 测试人员、前端开发者 |
| [FILES_CREATED.md](FILES_CREATED.md) | 文件清单和说明 | 项目管理者 |

### 工具文件
| 文件 | 用途 |
|------|------|
| [database_init.sql](database_init.sql) | PostgreSQL数据库初始化脚本 |
| [Banking_API.postman_collection.json](Banking_API.postman_collection.json) | Postman测试集合 |
| [.env.example](.env.example) | 环境变量配置示例 |

## 🏗️ 项目架构

```
┌─────────────────────────────────────┐
│         Controller Layer            │  ← RESTful API端点
│  AccountController, TransactionCtrl │
└─────────────┬───────────────────────┘
              ↓
┌─────────────────────────────────────┐
│          Service Layer              │  ← 业务逻辑处理
│   AccountService, TransactionSvc    │
└─────────────┬───────────────────────┘
              ↓
┌─────────────────────────────────────┐
│        Repository Layer             │  ← 数据访问接口
│  AccountRepo, TransactionRepo       │
└─────────────┬───────────────────────┘
              ↓
┌─────────────────────────────────────┐
│          Entity Layer               │  ← 数据模型
│      Account, Transaction           │
└─────────────┬───────────────────────┘
              ↓
┌─────────────────────────────────────┐
│    AWS RDS PostgreSQL Database      │  ← 数据存储
└─────────────────────────────────────┘
```

详细架构说明请查看 [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)

## 📂 代码结构

```
src/main/java/com/banking/
├── entity/              账户和交易实体
├── dto/                 API响应格式
├── repository/          数据访问层
├── service/             业务逻辑层
├── controller/          RESTful API
└── exception/           异常处理
```

## 🔧 配置文件

- `src/main/resources/application.properties` - Spring Boot配置
- `.env.example` - 环境变量模板
- `build.gradle.kts` - Gradle构建配置

## 🌐 API端点总览

### 账户管理 API (`/api/accounts`)
| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/accounts` | 创建账户 |
| GET | `/api/accounts` | 查询所有账户 |
| GET | `/api/accounts/{id}` | 根据ID查询 |
| GET | `/api/accounts/name/{accountName}` | 根据账户名查询 |
| GET | `/api/accounts/number/{accountNumber}` | 根据账号查询 |
| PUT | `/api/accounts/{id}` | 更新账户 |
| DELETE | `/api/accounts/{id}` | 删除账户 |

### 交易管理 API (`/api/transactions`)
| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/api/transactions` | 创建交易记录 |
| GET | `/api/transactions` | 查询所有交易 |
| GET | `/api/transactions/{id}` | 根据ID查询 |
| GET | `/api/transactions/transaction-id/{transactionId}` | 根据交易ID查询 |
| GET | `/api/transactions/account/{accountNumber}` | 查询账户交易 |
| GET | `/api/transactions/type/{transactionType}` | 根据类型查询 |
| PUT | `/api/transactions/{id}` | 更新交易 |
| DELETE | `/api/transactions/{id}` | 删除交易 |

详细API说明请查看 [POSTMAN_API_GUIDE.md](POSTMAN_API_GUIDE.md)

## 💡 使用场景指南

### 场景1: 我是新手开发者
1. ✅ 阅读 [QUICK_START.md](QUICK_START.md)
2. ✅ 按照步骤配置和启动项目
3. ✅ 使用Postman测试API
4. ✅ 查看代码了解实现细节

### 场景2: 我要进行API测试
1. ✅ 导入 [Banking_API.postman_collection.json](Banking_API.postman_collection.json)
2. ✅ 参考 [POSTMAN_API_GUIDE.md](POSTMAN_API_GUIDE.md)
3. ✅ 执行测试用例

### 场景3: 我要了解项目架构
1. ✅ 阅读 [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md)
2. ✅ 查看 [FILES_CREATED.md](FILES_CREATED.md)
3. ✅ 研究源代码

### 场景4: 我要部署到生产环境
1. ✅ 阅读 [README_CN.md](README_CN.md) 的"生产环境建议"部分
2. ✅ 配置 `.env` 文件（参考 `.env.example`）
3. ✅ 执行 [database_init.sql](database_init.sql)
4. ✅ 构建并部署应用

### 场景5: 我遇到问题了
1. ✅ 查看 [QUICK_START.md](QUICK_START.md) 的"常见问题排查"
2. ✅ 查看 [README_CN.md](README_CN.md) 的"常见问题"
3. ✅ 检查应用日志
4. ✅ 提交Issue

## 🎯 核心功能

- ✅ 账户管理（增删改查）
- ✅ 交易记录管理（增删改查）
- ✅ 多条件查询支持
- ✅ 数据验证
- ✅ 异常处理
- ✅ 统一响应格式
- ✅ RESTful API设计
- ✅ AWS RDS PostgreSQL集成

## 🛠️ 技术栈

- **后端框架**: Spring Boot 4.0.0
- **数据库**: AWS RDS PostgreSQL
- **ORM**: Spring Data JPA (Hibernate)
- **构建工具**: Gradle
- **Java版本**: 21
- **其他**: Lombok, Jakarta Validation, HikariCP

## 📊 项目统计

- **Java文件**: 10个
- **API端点**: 17个
- **文档页数**: 5个
- **代码行数**: ~4500行
- **测试用例**: 17个（Postman）

## 🔗 快速链接

### 文档
- [项目说明](README_CN.md)
- [快速开始](QUICK_START.md)
- [项目架构](PROJECT_STRUCTURE.md)
- [API指南](POSTMAN_API_GUIDE.md)
- [文件清单](FILES_CREATED.md)

### 配置
- [应用配置](src/main/resources/application.properties)
- [环境变量](env.example)
- [构建配置](build.gradle.kts)

### 工具
- [Postman集合](Banking_API.postman_collection.json)
- [数据库脚本](database_init.sql)

### 源代码
- [实体类](src/main/java/com/banking/entity/)
- [控制器](src/main/java/com/banking/controller/)
- [服务层](src/main/java/com/banking/service/)
- [数据访问](src/main/java/com/banking/repository/)

## 📞 获取帮助

遇到问题？按以下顺序寻找答案：

1. 📖 查看相关文档
2. 🔍 搜索常见问题部分
3. 📝 检查日志文件
4. 💬 提交Issue
5. 📧 联系开发团队

## 🎉 开始使用

**准备好了吗？** 执行以下命令开始：

```bash
# 1. 配置数据库连接
# 编辑 src/main/resources/application.properties

# 2. 启动应用
./gradlew bootRun

# 3. 测试API
curl http://localhost:8080/api/accounts
```

**祝你使用愉快！** 🚀

---

**最后更新**: 2024-11-24  
**项目版本**: 0.0.1-SNAPSHOT  
**文档版本**: 1.0

