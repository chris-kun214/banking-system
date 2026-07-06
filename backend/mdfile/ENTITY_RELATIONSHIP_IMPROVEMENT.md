# 实体关系改进方案

## 📊 当前问题分析

### 1. User ↔ Account：缺少关联

**问题：** Account 表没有 userId，无法知道账户属于哪个用户

**影响：**

- ❌ 无法查询某个用户的所有账户
- ❌ 无法实现用户级别的权限控制
- ❌ 无法实现"只能操作自己的账户"

### 2. Account ↔ Transaction：弱引用

**问题：** Transaction 使用字符串 accountNumber 而非外键

**影响：**

- ❌ 没有数据库级别的引用完整性
- ❌ 可能出现孤儿交易记录
- ❌ 查询效率低（字符串比较 vs 索引查询）

---

## ✅ 推荐的实体关系

```
User (用户)
  ↓ 1:N
Account (账户)
  ↓ 1:N
Transaction (交易)
```

**关系说明：**

- 一个用户可以有多个账户 (User → Account: 1:N)
- 一个账户可以有多条交易记录 (Account → Transaction: 1:N)
- 一个用户可以查询其所有账户的交易记录

---

## 🔧 改进后的实体类

### 1. User.java (改进版)

**主要改动：**

- ✅ 添加 `@OneToMany` 关联到 Account
- ✅ 可以通过 user.getAccounts() 获取用户的所有账户

```java
package com.banking.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少6个字符")
    @Column(nullable = false)
    private String password;

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "邮箱不能为空")
    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===================================
    // 新增：关联到 Account (一对多)
    // ===================================
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Account> accounts = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // UserDetails 接口实现
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public enum Role {
        USER,
        ADMIN,
        MANAGER
    }

    // ===================================
    // 辅助方法：添加账户
    // ===================================
    public void addAccount(Account account) {
        accounts.add(account);
        account.setUser(this);
    }

    // ===================================
    // 辅助方法：移除账户
    // ===================================
    public void removeAccount(Account account) {
        accounts.remove(account);
        account.setUser(null);
    }
}
```

---

### 2. Account.java (改进版)

**主要改动：**

- ✅ 添加 `userId` 外键字段
- ✅ 添加 `@ManyToOne` 关联到 User
- ✅ 添加 `@OneToMany` 关联到 Transaction
- ✅ 添加时间戳字段（与数据库一致）

```java
package com.banking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "account")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "账户名不能为空")
    @Size(max = 20, message = "账户名不能超过20个字符")
    @Column(name = "account_name", unique = true, nullable = false, length = 20)
    private String accountName;

    @NotBlank(message = "账号不能为空")
    @Size(max = 30, message = "账号不能超过30个字符")
    @Column(name = "account_number", unique = true, nullable = false, length = 30)
    private String accountNumber;

    @Column(name = "balance", nullable = false, precision = 10, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    // ===================================
    // 新增：关联到 User (多对一)
    // ===================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_account_user"))
    @JsonIgnore  // 避免 JSON 序列化时的循环引用
    private User user;

    // ===================================
    // 新增：关联到 Transaction (一对多)
    // ===================================
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore  // 避免 JSON 序列化时的循环引用
    private List<Transaction> transactions = new ArrayList<>();

    // ===================================
    // 新增：时间戳字段
    // ===================================
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===================================
    // 辅助方法：添加交易记录
    // ===================================
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        transaction.setAccount(this);
    }

    // ===================================
    // 辅助方法：移除交易记录
    // ===================================
    public void removeTransaction(Transaction transaction) {
        transactions.remove(transaction);
        transaction.setAccount(null);
    }
}
```

---

### 3. Transaction.java (改进版)

**主要改动：**

- ✅ 添加 `accountId` 外键字段
- ✅ 添加 `@ManyToOne` 关联到 Account
- ✅ 保留 `accountNumber` 用于显示和查询（冗余字段）
- ✅ 添加时间戳字段

```java
package com.banking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transaction")
public class Transaction {
  
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false, length = 50)
    private String transactionId;

    // ===================================
    // 改进：添加外键关联到 Account
    // ===================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transaction_account"))
    @JsonIgnore  // 避免 JSON 序列化时的循环引用
    private Account account;

    // ===================================
    // 保留 accountNumber 用于显示（冗余字段）
    // 注意：这是冗余设计，便于查询，但需要保证数据一致性
    // ===================================
    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 18, scale = 2)
    private BigDecimal balanceAfter;

    // ===================================
    // 新增：时间戳字段
    // ===================================
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        // 自动从关联的 Account 获取 accountNumber
        if (account != null && accountNumber == null) {
            accountNumber = account.getAccountNumber();
        }
    }

    public enum TransactionType {
        DEPOSIT,        // 存款
        WITHDRAW,       // 取款
        TRANSFER_IN,    // 转入
        TRANSFER_OUT    // 转出
    }
}
```

---

## 📊 数据库表结构变化

### users 表（无变化）

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(100),
    phone_number VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
```

### account 表（新增 user_id 外键）

```sql
CREATE TABLE account (
    id BIGSERIAL PRIMARY KEY,
    account_name VARCHAR(20) UNIQUE NOT NULL,
    account_number VARCHAR(30) UNIQUE NOT NULL,
    balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    user_id BIGINT NOT NULL,  -- 新增
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_account_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE  -- 级联删除
);

CREATE INDEX idx_account_user_id ON account(user_id);
```

### transaction 表（新增 account_id 外键）

```sql
CREATE TABLE transaction (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(50) UNIQUE NOT NULL,
    account_id BIGINT NOT NULL,  -- 新增（外键）
    account_number VARCHAR(30) NOT NULL,  -- 保留（冗余）
    transaction_type VARCHAR(20) NOT NULL,
    amount DECIMAL(18, 2) NOT NULL,
    balance_before DECIMAL(18, 2) NOT NULL,
    balance_after DECIMAL(18, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_transaction_account FOREIGN KEY (account_id) 
        REFERENCES account(id) ON DELETE CASCADE  -- 级联删除
);

CREATE INDEX idx_transaction_account_id ON transaction(account_id);
CREATE INDEX idx_transaction_account_number ON transaction(account_number);
```

---

## 🎯 改进的优势

### 1. 数据完整性

- ✅ 数据库级别的外键约束
- ✅ 防止孤儿数据
- ✅ 级联删除保证数据一致性

### 2. 查询效率

- ✅ 使用外键索引，查询更快
- ✅ 支持 JPA 的关联查询
- ✅ 减少 JOIN 操作

### 3. 业务逻辑

- ✅ 支持"用户查询自己的账户"
- ✅ 支持"账户查询自己的交易记录"
- ✅ 支持权限控制（用户只能操作自己的账户）

### 4. 代码优雅

```java
// 查询用户的所有账户
List<Account> accounts = user.getAccounts();

// 查询账户的所有交易
List<Transaction> transactions = account.getTransactions();

// 查询账户所属的用户
User owner = account.getUser();
```

---

## 🔄 数据迁移方案

### 方案 1: 全新部署（推荐用于开发环境）

如果是新项目或可以清空数据：

```bash
# 1. 备份现有数据（如果需要）
pg_dump -U chrischen banking > backup.sql

# 2. 删除数据库
psql -U chrischen -c "DROP DATABASE banking;"

# 3. 重新创建数据库
psql -U chrischen -c "CREATE DATABASE banking;"

# 4. 应用新的实体类
# Hibernate 会自动创建新表结构（ddl-auto: update）
./start-local.sh
```

### 方案 2: 渐进式迁移（用于有数据的环境）

如果需要保留现有数据：

```sql
-- 1. 为 account 表添加 user_id 列（允许 NULL）
ALTER TABLE account ADD COLUMN user_id BIGINT;

-- 2. 为现有账户分配一个默认用户（需要先创建一个用户）
-- 示例：假设 user_id=1 是默认用户
UPDATE account SET user_id = 1 WHERE user_id IS NULL;

-- 3. 添加非空约束和外键
ALTER TABLE account ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE account ADD CONSTRAINT fk_account_user 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 4. 创建索引
CREATE INDEX idx_account_user_id ON account(user_id);

-- 5. 为 transaction 表添加 account_id 列
ALTER TABLE transaction ADD COLUMN account_id BIGINT;

-- 6. 根据 account_number 填充 account_id
UPDATE transaction t
SET account_id = a.id
FROM account a
WHERE t.account_number = a.account_number;

-- 7. 添加非空约束和外键
ALTER TABLE transaction ALTER COLUMN account_id SET NOT NULL;
ALTER TABLE transaction ADD CONSTRAINT fk_transaction_account 
    FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE CASCADE;

-- 8. 创建索引
CREATE INDEX idx_transaction_account_id ON transaction(account_id);
```

---

## 📝 Service 层的调整

### AccountService 需要的改动

```java
// 创建账户时需要关联用户
public Account createAccount(Long userId, AccountDTO accountDTO) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("用户不存在"));
  
    Account account = new Account();
    account.setAccountName(accountDTO.getAccountName());
    account.setAccountNumber(accountDTO.getAccountNumber());
    account.setBalance(accountDTO.getBalance());
    account.setUser(user);  // 关联用户
  
    return accountRepository.save(account);
}

// 查询用户的所有账户
public List<Account> getUserAccounts(Long userId) {
    return accountRepository.findByUserId(userId);
}

// 权限检查：确保用户只能操作自己的账户
public Account getAccountWithPermissionCheck(Long accountId, Long userId) {
    Account account = accountRepository.findById(accountId)
        .orElseThrow(() -> new RuntimeException("账户不存在"));
  
    if (!account.getUser().getId().equals(userId)) {
        throw new RuntimeException("无权访问此账户");
    }
  
    return account;
}
```

### TransactionService 需要的改动

```java
// 创建交易时关联 Account 实体
public Transaction createTransaction(TransactionDTO dto) {
    Account account = accountRepository.findByAccountNumber(dto.getAccountNumber())
        .orElseThrow(() -> new RuntimeException("账户不存在"));
  
    Transaction transaction = new Transaction();
    transaction.setTransactionId(dto.getTransactionId());
    transaction.setAccount(account);  // 关联账户（外键）
    transaction.setAccountNumber(account.getAccountNumber());  // 冗余字段
    transaction.setTransactionType(dto.getTransactionType());
    transaction.setAmount(dto.getAmount());
    // ... 其他字段
  
    return transactionRepository.save(transaction);
}
```

---

## 🔍 Repository 层的新方法

### AccountRepository

```java
public interface AccountRepository extends JpaRepository<Account, Long> {
    // 现有方法
    Optional<Account> findByAccountName(String accountName);
    Optional<Account> findByAccountNumber(String accountNumber);
  
    // 新增方法：根据用户ID查询账户
    List<Account> findByUserId(Long userId);
  
    // 新增方法：查询用户的账户数量
    long countByUserId(Long userId);
  
    // 新增方法：检查账户是否属于指定用户
    boolean existsByIdAndUserId(Long accountId, Long userId);
}
```

### TransactionRepository

```java
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // 现有方法
    Optional<Transaction> findByTransactionId(String transactionId);
    List<Transaction> findByAccountNumber(String accountNumber);
    List<Transaction> findByTransactionType(TransactionType type);
  
    // 新增方法：根据账户ID查询交易
    List<Transaction> findByAccountId(Long accountId);
  
    // 新增方法：查询用户的所有交易（通过账户关联）
    @Query("SELECT t FROM Transaction t WHERE t.account.user.id = :userId")
    List<Transaction> findByUserId(@Param("userId") Long userId);
  
    // 新增方法：统计账户的交易数量
    long countByAccountId(Long accountId);
}
```

---

## ⚠️ 注意事项

### 1. 循环引用问题

使用 `@JsonIgnore` 避免 JSON 序列化时的循环引用：

```java
// 在 Account 中
@ManyToOne
@JsonIgnore
private User user;

@OneToMany
@JsonIgnore
private List<Transaction> transactions;
```

### 2. 懒加载问题

关联实体使用 `FetchType.LAZY` 避免 N+1 查询问题：

```java
@ManyToOne(fetch = FetchType.LAZY)
private User user;
```

如需加载关联数据，使用 JPQL 的 JOIN FETCH：

```java
@Query("SELECT a FROM Account a JOIN FETCH a.user WHERE a.id = :id")
Optional<Account> findByIdWithUser(@Param("id") Long id);
```

### 3. 级联操作

谨慎使用级联删除：

```java
// 删除用户时会删除其所有账户
@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
private List<Account> accounts;
```

### 4. 双向关联的维护

使用辅助方法保证双向关联的一致性：

```java
public void addAccount(Account account) {
    accounts.add(account);
    account.setUser(this);  // 维护双向关联
}
```

---

## 🚀 实施步骤

### 第一步：备份数据

```bash
pg_dump -U chrischen banking > backup_$(date +%Y%m%d_%H%M%S).sql
```

### 第二步：更新实体类

替换 User.java、Account.java、Transaction.java

### 第三步：更新 Repository

添加新的查询方法

### 第四步：更新 Service 层

调整业务逻辑，支持用户关联

### 第五步：更新 Controller

调整 API，添加用户权限检查

### 第六步：数据迁移

选择迁移方案并执行

### 第七步：测试

- 单元测试
- 集成测试
- API 测试

---

## ✅ 总结

### 改进前：

```
User (独立)
Account (独立)
Transaction → Account (字符串引用)
```

### 改进后：

```
User → Account (外键 user_id)
Account → Transaction (外键 account_id)
```

### 核心优势：

1. ✅ 数据完整性保证
2. ✅ 查询性能提升
3. ✅ 支持用户级权限控制
4. ✅ 防止数据不一致
5. ✅ 代码更清晰优雅

---

## 📞 需要帮助？

如果需要我帮你实施这些改进，请告诉我：

1. 是否有现有数据需要保留？
2. 优先实施哪些改进？
3. 是否需要我更新相关的 Service 和 Controller？
