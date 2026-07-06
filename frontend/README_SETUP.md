# 银行系统前端 - 使用指南

## 📁 项目结构

```
src/
├── components/          # React 组件
│   ├── Login.tsx       # 登录组件
│   ├── Register.tsx    # 注册组件
│   ├── Dashboard.tsx   # 仪表板组件
│   ├── Auth.css        # 认证页面样式
│   └── Dashboard.css   # 仪表板样式
├── types.ts            # TypeScript 类型定义
├── api.ts              # API 服务
├── App.tsx             # 主应用组件
├── main.tsx            # 应用入口
└── index.css           # 全局样式
```

## 🚀 快速开始

### 1. 安装依赖

```bash
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

应用将在 `http://localhost:5173` 上运行

### 3. 确保后端服务运行

前端需要后端 API 服务运行在 `http://localhost:8080`

## 📋 功能说明

### 登录功能
- 用户名和密码登录
- 自动保存登录状态（使用 localStorage）
- 错误提示
- 表单验证

### 注册功能
- 必填字段：
  - 用户名（3-50个字符）
  - 密码（至少6个字符）
  - 邮箱（有效邮箱格式）
  - 全名
- 可选字段：
  - 电话号码
- 密码确认验证
- 前端表单验证

### 仪表板
- 显示用户信息
- 退出登录功能
- 功能卡片展示

## 🔧 API 配置

API 基础地址在 `src/api.ts` 中配置：

```typescript
const API_BASE_URL = 'http://localhost:8080/api';
```

如需修改后端地址，请更新此常量。

## 📡 API 端点

### 认证相关
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/register` - 用户注册
- `GET /api/auth/me` - 获取当前用户信息

### 请求格式

**登录请求：**
```json
{
  "username": "用户名",
  "password": "密码"
}
```

**注册请求：**
```json
{
  "username": "用户名",
  "password": "密码",
  "email": "邮箱",
  "fullName": "全名",
  "phoneNumber": "电话号码（可选）"
}
```

**响应格式：**
```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    "token": "JWT令牌",
    "type": "Bearer",
    "username": "用户名",
    "email": "邮箱",
    "role": "角色"
  }
}
```

## 🎨 样式特点

- 现代化渐变背景
- 卡片式设计
- 响应式布局（支持移动端）
- 平滑动画效果
- 悬停交互效果

## 🔐 安全特性

- JWT 令牌认证
- 自动在请求头添加 Authorization
- 安全的密码输入
- 客户端表单验证

## 📱 响应式设计

支持以下设备：
- 桌面（> 768px）
- 平板（768px - 480px）
- 手机（< 480px）

## 🛠️ 开发建议

### 调试 API 请求

打开浏览器开发者工具的 Network 标签查看 API 请求和响应。

### 清除本地存储

如需清除登录状态，在浏览器控制台执行：
```javascript
localStorage.clear();
```

### 常见问题

**1. 无法连接到后端**
- 检查后端服务是否在 `http://localhost:8080` 运行
- 检查 CORS 配置是否正确

**2. 登录后立即退出**
- 检查 JWT 令牌是否正确保存
- 检查浏览器是否启用了 localStorage

**3. 样式不显示**
- 确保 CSS 文件已正确导入
- 清除浏览器缓存

## 📦 生产构建

```bash
npm run build
```

构建产物将在 `dist` 目录中。

## 🔄 下一步开发建议

1. 添加账户管理功能
2. 添加转账功能
3. 添加交易历史查看
4. 添加用户个人资料编辑
5. 添加忘记密码功能
6. 添加 React Router 进行更复杂的路由管理
7. 添加状态管理（如 Redux 或 Zustand）
8. 添加单元测试和集成测试

## 📝 技术栈

- ⚛️ React 18
- 📘 TypeScript
- ⚡ Vite
- 🎨 CSS3
- 🔐 JWT 认证

## 👨‍💻 作者

银行系统前端应用 - 2024

