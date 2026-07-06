# Banking API - Postman 测试指南

## API 基础信息
- **Base URL**: `http://localhost:8080`
- **Content-Type**: `application/json`

## 响应格式
所有API返回统一的响应格式：
```json
{
  "success": true,
  "message": "操作成功",
  "data": { ... },
  "timestamp": "2024-11-24T10:30:00"
}
```

---

## 账户管理 API (Account)

### 1. 创建账户
**POST** `/api/accounts`

**请求体示例：**
```json
{
  "accountName": "张三",
  "accountNumber": "6222021234567890",
  "balance": 1000.00
}
```

**响应示例：**
```json
{
  "success": true,
  "message": "账户创建成功",
  "data": {
    "id": 1,
    "accountName": "张三",
    "accountNumber": "6222021234567890",
    "balance": 1000.00
  },
  "timestamp": "2024-11-24T10:30:00"
}
```

### 2. 查询所有账户
**GET** `/api/accounts`

**响应示例：**
```json
{
  "success": true,
  "message": "查询成功",
  "data": [
    {
      "id": 1,
      "accountName": "张三",
      "accountNumber": "6222021234567890",
      "balance": 1000.00
    },
    {
      "id": 2,
      "accountName": "李四",
      "accountNumber": "6222021234567891",
      "balance": 2000.00
    }
  ],
  "timestamp": "2024-11-24T10:30:00"
}
```

### 3. 根据ID查询账户
**GET** `/api/accounts/{id}`

**示例：** `GET /api/accounts/1`

### 4. 根据账户名查询账户
**GET** `/api/accounts/name/{accountName}`

**示例：** `GET /api/accounts/name/张三`

### 5. 根据账号查询账户
**GET** `/api/accounts/number/{accountNumber}`

**示例：** `GET /api/accounts/number/6222021234567890`

### 6. 更新账户
**PUT** `/api/accounts/{id}`

**请求体示例：**
```json
{
  "accountName": "张三",
  "accountNumber": "6222021234567890",
  "balance": 1500.00
}
```

### 7. 删除账户
**DELETE** `/api/accounts/{id}`

**示例：** `DELETE /api/accounts/1`

### 8. 检查账户是否存在
**HEAD** `/api/accounts/{id}`

---

## 交易记录 API (Transaction)

### 1. 创建交易记录
**POST** `/api/transactions`

**请求体示例：**
```json
{
  "transactionId": "TXN20241124001",
  "accountNumber": "6222021234567890",
  "transactionType": "DEPOSIT",
  "amount": 500.00,
  "balanceBefore": 1000.00,
  "balanceAfter": 1500.00
}
```

**交易类型（transactionType）：**
- `DEPOSIT` - 存款
- `WITHDRAW` - 取款
- `TRANSFER_IN` - 转入
- `TRANSFER_OUT` - 转出

### 2. 查询所有交易记录
**GET** `/api/transactions`

### 3. 根据ID查询交易记录
**GET** `/api/transactions/{id}`

**示例：** `GET /api/transactions/1`

### 4. 根据交易ID查询交易记录
**GET** `/api/transactions/transaction-id/{transactionId}`

**示例：** `GET /api/transactions/transaction-id/TXN20241124001`

### 5. 根据账号查询所有交易记录
**GET** `/api/transactions/account/{accountNumber}`

**示例：** `GET /api/transactions/account/6222021234567890`

### 6. 根据交易类型查询交易记录
**GET** `/api/transactions/type/{transactionType}`

**示例：** `GET /api/transactions/type/DEPOSIT`

### 7. 根据账号和交易类型查询交易记录
**GET** `/api/transactions/account/{accountNumber}/type/{transactionType}`

**示例：** `GET /api/transactions/account/6222021234567890/type/DEPOSIT`

### 8. 更新交易记录
**PUT** `/api/transactions/{id}`

**请求体示例：**
```json
{
  "transactionId": "TXN20241124001",
  "accountNumber": "6222021234567890",
  "transactionType": "WITHDRAW",
  "amount": 300.00,
  "balanceBefore": 1500.00,
  "balanceAfter": 1200.00
}
```

### 9. 删除交易记录
**DELETE** `/api/transactions/{id}`

**示例：** `DELETE /api/transactions/1`

### 10. 检查交易记录是否存在
**HEAD** `/api/transactions/{id}`

---

## Postman 测试步骤

### 1. 设置环境变量
在Postman中创建环境变量：
- `base_url`: `http://localhost:8080`

### 2. 测试账户功能流程

#### Step 1: 创建账户
```
POST {{base_url}}/api/accounts
Body:
{
  "accountName": "测试账户",
  "accountNumber": "6222021234567890",
  "balance": 5000.00
}
```

#### Step 2: 查询所有账户
```
GET {{base_url}}/api/accounts
```

#### Step 3: 根据ID查询账户
```
GET {{base_url}}/api/accounts/1
```

#### Step 4: 更新账户
```
PUT {{base_url}}/api/accounts/1
Body:
{
  "accountName": "测试账户",
  "accountNumber": "6222021234567890",
  "balance": 6000.00
}
```

### 3. 测试交易记录功能流程

#### Step 1: 创建存款交易
```
POST {{base_url}}/api/transactions
Body:
{
  "transactionId": "TXN001",
  "accountNumber": "6222021234567890",
  "transactionType": "DEPOSIT",
  "amount": 1000.00,
  "balanceBefore": 5000.00,
  "balanceAfter": 6000.00
}
```

#### Step 2: 创建取款交易
```
POST {{base_url}}/api/transactions
Body:
{
  "transactionId": "TXN002",
  "accountNumber": "6222021234567890",
  "transactionType": "WITHDRAW",
  "amount": 500.00,
  "balanceBefore": 6000.00,
  "balanceAfter": 5500.00
}
```

#### Step 3: 查询账户所有交易记录
```
GET {{base_url}}/api/transactions/account/6222021234567890
```

#### Step 4: 查询特定类型的交易记录
```
GET {{base_url}}/api/transactions/type/DEPOSIT
```

---

## 错误响应示例

### 账户不存在
```json
{
  "success": false,
  "message": "账户不存在，ID: 999",
  "data": null,
  "timestamp": "2024-11-24T10:30:00"
}
```

### 账户名已存在
```json
{
  "success": false,
  "message": "账户名已存在: 张三",
  "data": null,
  "timestamp": "2024-11-24T10:30:00"
}
```

### 验证错误
```json
{
  "success": false,
  "message": "Username can not be blank",
  "data": null,
  "timestamp": "2024-11-24T10:30:00"
}
```

---

## 注意事项

1. 确保Spring Boot应用已启动并监听8080端口
2. 确保AWS RDS PostgreSQL数据库已配置并连接成功
3. 在application.properties中配置数据库连接信息
4. 所有金额字段使用BigDecimal类型，保持精度
5. 交易类型必须使用枚举值：DEPOSIT, WITHDRAW, TRANSFER_IN, TRANSFER_OUT
6. 账户名和账号都是唯一的，不能重复

