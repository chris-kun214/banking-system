# JWT 认证使用指南

## 📮 Postman 测试步骤

### 步骤 1：启动应用
```bash
./gradlew bootRun
```
应用将在 `http://localhost:8080` 启动

### 步骤 2：注册新用户

1. 创建新请求
   - 方法：`POST`
   - URL：`http://localhost:8080/api/auth/register`

2. 设置请求头
   - 点击 **Headers** 标签
   - 添加：`Content-Type: application/json`

3. 设置请求体
   - 点击 **Body** 标签
   - 选择 **raw** 和 **JSON**
   - 输入以下内容：

```json
{
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com",
    "fullName": "测试用户",
    "phoneNumber": "13800138000"
}
```

4. 点击 **Send** 发送请求

5. 预期响应（状态码 201）：
```json
{
    "success": true,
    "message": "注册成功",
    "data": {
        "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTczMzg1MDAwMCwiZXhwIjoxNzMzOTM2NDAwfQ...",
        "type": "Bearer",
        "username": "testuser",
        "email": "test@example.com",
        "role": "USER"
    },
    "timestamp": "2025-12-10T10:00:00"
}
```

### 步骤 3：用户登录获取 Token

1. 创建新请求
   - 方法：`POST`
   - URL：`http://localhost:8080/api/auth/login`

2. 设置请求头
   - `Content-Type: application/json`

3. 设置请求体
```json
{
    "username": "testuser",
    "password": "password123"
}
```

4. 点击 **Send**，复制响应中的 `token` 值

**💡 重要：保存这个 Token，后续请求都需要用到！**

### 步骤 4：测试受保护的 API（需要认证）

#### 方法 A：手动添加 Authorization Header

1. 创建新请求
   - 方法：`GET`
   - URL：`http://localhost:8080/api/auth/me`

2. 设置请求头
   - 点击 **Headers** 标签
   - 添加：`Authorization: Bearer {your_token}`
   - 将 `{your_token}` 替换为实际的 Token

3. 点击 **Send**

#### 方法 B：使用 Postman 的 Authorization 功能（推荐）

1. 创建新请求
   - 方法：`GET`
   - URL：`http://localhost:8080/api/auth/me`

2. 设置认证
   - 点击 **Authorization** 标签
   - Type 选择：**Bearer Token**
   - Token 输入框：粘贴你的 JWT Token

3. 点击 **Send**

4. 预期响应（状态码 200）：
```json
{
    "success": true,
    "message": "获取用户信息成功",
    "data": {
        "id": 1,
        "username": "testuser",
        "email": "test@example.com",
        "fullName": "测试用户",
        "phoneNumber": "13800138000",
        "role": "USER",
        "enabled": true,
        "createdAt": "2025-12-10T10:00:00",
        "updatedAt": "2025-12-10T10:00:00"
    },
    "timestamp": "2025-12-10T10:00:00"
}
```

### 步骤 5：测试其他受保护的 API

#### 获取所有账户（需要认证）
```
GET http://localhost:8080/api/accounts
Authorization: Bearer {your_token}
```

#### 创建账户（需要认证）
```
POST http://localhost:8080/api/accounts
Authorization: Bearer {your_token}
Content-Type: application/json

{
    "accountName": "测试账户",
    "accountNumber": "1234567890",
    "balance": 1000.00
}
```

### 步骤 6：测试未授权访问

1. 不添加 Authorization 请求头
   - 方法：`GET`
   - URL：`http://localhost:8080/api/accounts`

2. 点击 **Send**

3. 预期响应（状态码 401）：
```json
{
    "status": 401,
    "error": "Unauthorized",
    "message": "Full authentication is required to access this resource",
    "path": "/api/accounts"
}
```

### 步骤 7：使用 Postman Collection（推荐）

项目已经包含了一个预配置的 Postman Collection！

#### 导入 Collection：

1. 打开 Postman
2. 点击左上角 **Import** 按钮
3. 选择 `Banking_API_JWT.postman_collection.json` 文件
4. 点击 **Import**

#### Collection 特性：

✅ **自动保存 Token**
   - 注册或登录成功后，Token 会自动保存到 Collection 变量中
   - 无需手动复制粘贴 Token

✅ **预配置认证**
   - 所有需要认证的请求已配置使用 `{{token}}` 变量
   - Collection 级别设置了 Bearer Token 认证

✅ **包含所有端点**
   - 认证：注册、登录、获取用户信息
   - 账户管理：CRUD 操作
   - 交易管理：CRUD 操作
   - 测试未授权访问

#### 使用步骤：

1. **先运行注册或登录请求**
   - 打开 "认证 Authentication" 文件夹
   - 运行 "用户登录 Login"
   - 查看 Console，确认 Token 已保存

2. **运行其他受保护的请求**
   - 所有请求会自动使用保存的 Token
   - 无需任何额外配置

3. **查看 Collection 变量**
   - 点击 Collection → **Variables** 标签
   - 可以看到 `baseUrl` 和 `token` 变量

#### 自定义配置：

如果需要修改基础 URL：
- 点击 Collection → **Variables**
- 修改 `baseUrl` 的值（例如：`http://your-server:8080`）

## 认证端点

### 1. 用户注册

```http
POST /api/auth/register
Content-Type: application/json

{
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com",
    "fullName": "测试用户",
    "phoneNumber": "13800138000"
}
```

### 2. 用户登录

```http
POST /api/auth/login
Content-Type: application/json

{
    "username": "testuser",
    "password": "password123"
}
```

**响应示例：**

```json
{
    "success": true,
    "message": "登录成功",
    "data": {
        "token": "eyJhbGciOiJIUzI1NiJ9...",
        "type": "Bearer",
        "username": "testuser",
        "email": "test@example.com",
        "role": "USER"
    },
    "timestamp": "2025-12-10T10:00:00"
}
```

### 3. 获取当前用户信息

```http
GET /api/auth/me
Authorization: Bearer {token}
```

### 4. 健康检查

```http
GET /api/auth/health
```

## 使用认证

在所有需要认证的请求中，添加以下请求头：

```http
Authorization: Bearer {your_jwt_token}
```

## 用户角色

- **USER**: 普通用户
- **MANAGER**: 管理员
- **ADMIN**: 超级管理员

## 安全配置

### 公开端点（无需认证）：

- `/api/auth/**` - 所有认证相关端点
- `/api/public/**` - 公开 API
- `/error` - 错误页面

### 需要认证的端点：

- `/api/accounts/**` - 账户管理
- `/api/transactions/**` - 交易管理

### 管理员端点：

- `/api/admin/**` - 仅管理员可访问

## 配置说明

JWT 配置在 `application.yml` 中：

```yaml
jwt:
  secret: ***REMOVED_JWT_SECRET***  # Base64编码的密钥
  expiration: 86400000  # Token过期时间（毫秒），默认24小时
```

## 数据库表

系统会自动创建 `users` 表，包含以下字段：

- id
- username
- password (BCrypt 加密)
- email
- full_name
- phone_number
- role (USER/MANAGER/ADMIN)
- enabled
- created_at
- updated_at

---

## ❓ 常见问题

### Q1: Token 过期了怎么办？
**A:** Token 默认有效期为 24 小时。过期后需要重新登录获取新 Token。

### Q2: 如何测试不同角色的用户？
**A:** 
- 默认注册的用户角色为 `USER`
- 如需测试管理员功能，需要直接在数据库中修改用户的 `role` 字段为 `ADMIN` 或 `MANAGER`

### Q3: 为什么返回 401 Unauthorized？
**A:** 检查以下几点：
- Token 是否已过期
- Authorization 头格式是否正确：`Bearer {token}`
- Token 前面是否有多余的空格
- 是否使用了正确的 Token

### Q4: 为什么返回 403 Forbidden？
**A:** 你的 Token 有效，但没有权限访问该资源。检查用户角色是否符合要求。

### Q5: 如何在 Postman 中查看 Token？
**A:** 
1. 点击 Collection 右侧的 **三个点**
2. 选择 **Edit**
3. 点击 **Variables** 标签
4. 查看 `token` 变量的 Current Value

### Q6: 注册时提示用户名或邮箱已存在？
**A:** 
- 用户名和邮箱必须唯一
- 可以使用不同的用户名或邮箱重新注册
- 或者直接使用现有账户登录

---

## 🔧 故障排除

### 问题：无法连接到数据库
**解决方案：**
1. 检查 AWS RDS 是否正常运行
2. 确认安全组规则允许你的 IP 访问
3. 验证 `application.yml` 中的数据库配置

### 问题：应用启动失败
**解决方案：**
```bash
# 清理并重新构建
./gradlew clean build -x test

# 查看详细日志
./gradlew bootRun --info
```

### 问题：Token 验证失败
**解决方案：**
1. 确认 `jwt.secret` 配置正确
2. 检查系统时间是否正确（Token 包含时间戳）
3. 重新登录获取新 Token

---

## 📚 相关文档

- `QUICK_START.md` - 快速开始指南
- `POSTMAN_API_GUIDE.md` - Postman API 测试指南
- `PROJECT_STRUCTURE.md` - 项目结构说明
- `database_init.sql` - 数据库初始化脚本

---

## 🎯 测试清单

使用以下清单确保 JWT 认证功能正常工作：

- [ ] 用户注册成功，返回 Token
- [ ] 用户登录成功，返回 Token
- [ ] 使用 Token 访问 `/api/auth/me` 成功
- [ ] 使用 Token 访问 `/api/accounts` 成功
- [ ] 不带 Token 访问受保护端点返回 401
- [ ] 使用错误的 Token 返回 401
- [ ] Token 在日志中正确记录用户操作
