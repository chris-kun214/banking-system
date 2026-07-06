# 账号生成器使用指南

## 📋 概述

`AccountNumberGenerator` 是一个参照 **CBA（澳大利亚联邦银行）** 格式的自动账号生成工具。

### CBA 账号格式

```
完整格式: BSB-Account Number
示例: 062-001-123456789

组成部分:
- BSB (Bank-State-Branch): 6位数字
  - 前3位: 银行代码 (例如: 062)
  - 后3位: 分支代码 (例如: 001)
- Account Number: 9位数字账号
```

---

## 🚀 快速开始

### 1. 基本使用

```java
@Service
public class AccountService {
    
    @Autowired
    private AccountNumberGenerator accountNumberGenerator;
    
    @Autowired
    private AccountRepository accountRepository;
    
    public Account createAccount(AccountDTO dto) {
        Account account = new Account();
        
        // 自动生成账号
        String accountNumber = accountNumberGenerator.generateAccountNumber();
        
        account.setAccountNumber(accountNumber);
        account.setAccountName(dto.getAccountName());
        account.setBalance(dto.getBalance());
        
        return accountRepository.save(account);
    }
}
```

### 2. 生成的账号示例

```
062001-123456789
062015-987654321
062123-456789012
062-001-123456789 (格式化版本)
```

---

## 📚 功能说明

### 1. 生成基本账号

**方法：** `generateAccountNumber()`

**格式：** `XXXXXX-XXXXXXXXX` (6位BSB + 9位账号)

```java
String accountNumber = generator.generateAccountNumber();
// 输出: 062001-123456789
```

**特点：**
- ✅ 使用时间戳 + 随机数，保证唯一性
- ✅ 简洁格式，易于存储
- ✅ 符合 CBA 标准

---

### 2. 生成格式化账号

**方法：** `generateFormattedAccountNumber()`

**格式：** `XXX-XXX-XXXXXXXXX` (带横杠的 BSB)

```java
String accountNumber = generator.generateFormattedAccountNumber();
// 输出: 062-001-123456789
```

**特点：**
- ✅ 更易读的格式
- ✅ BSB 部分带横杠分隔
- ✅ 适合显示给用户

---

### 3. 基于用户ID生成

**方法：** `generateAccountNumberByUserId(Long userId)`

**用途：** 根据用户ID生成账号，便于追踪

```java
Long userId = 12345L;
String accountNumber = generator.generateAccountNumberByUserId(userId);
// 输出: 062001-012345000
```

**特点：**
- ✅ 包含用户ID信息
- ✅ 便于客服追踪
- ✅ 可预测性更高

**推荐场景：**
- 为新用户开户
- 需要关联用户信息

---

### 4. 基于日期生成

**方法：** `generateDateBasedAccountNumber()`

**格式：** 账号部分包含日期 `YYMMDDXXX`

```java
String accountNumber = generator.generateDateBasedAccountNumber();
// 输出: 062001-241223001
// 说明: 24年12月23日 + 随机数001
```

**特点：**
- ✅ 包含开户日期信息
- ✅ 便于按日期统计
- ✅ 便于审计追踪

**推荐场景：**
- 需要按开户日期统计
- 财务审计需求

---

### 5. 带校验位的账号

**方法：** `generateAccountNumberWithChecksum()`

**原理：** 使用 Luhn 算法生成校验位

```java
String accountNumber = generator.generateAccountNumberWithChecksum();
// 输出: 062001-123456784 (最后一位是校验位)

// 验证校验位
boolean isValid = generator.validateAccountNumberWithChecksum(accountNumber);
// 返回: true
```

**特点：**
- ✅ 防止输入错误
- ✅ 增加账号安全性
- ✅ 符合银行业标准

**推荐场景：**
- 高安全要求场景
- 用户手动输入账号的情况

---

### 6. 指定分支生成

**方法：** `generateAccountNumberForBranch(int branchCode)`

**用途：** 为特定分支生成账号

```java
int branchCode = 123; // 分支代码 001-999
String accountNumber = generator.generateAccountNumberForBranch(branchCode);
// 输出: 062123-987654321
```

**特点：**
- ✅ 指定分支代码
- ✅ 便于分支管理
- ✅ 支持多分支机构

**推荐场景：**
- 多分支银行系统
- 需要区分开户分支

---

### 7. 批量生成

**方法：** `generateBatchAccountNumbers(int count)`

**用途：** 一次性生成多个账号

```java
int count = 10;
String[] accountNumbers = generator.generateBatchAccountNumbers(count);

for (String accountNumber : accountNumbers) {
    System.out.println(accountNumber);
}
// 输出:
// 062001-123456789
// 062015-234567890
// 062023-345678901
// ...
```

**特点：**
- ✅ 批量生成，效率高
- ✅ 保证唯一性
- ✅ 最多支持1000个

**推荐场景：**
- 预生成账号池
- 批量开户操作

---

## 🔧 辅助功能

### 1. 格式验证

```java
// 验证账号格式
boolean isValid = generator.validateAccountNumberFormat("062001-123456789");
// 返回: true

// 支持多种格式
generator.validateAccountNumberFormat("062001123456789");      // true
generator.validateAccountNumberFormat("062-001-123456789");    // true
generator.validateAccountNumberFormat("12345");                // false
```

---

### 2. 账号格式化

```java
String unformatted = "062001123456789";

// 简单格式: XXXXXX-XXXXXXXXX
String simple = generator.formatAccountNumber(unformatted, false);
// 输出: 062001-123456789

// 完整格式: XXX-XXX-XXXXXXXXX
String full = generator.formatAccountNumber(unformatted, true);
// 输出: 062-001-123456789
```

---

### 3. 提取 BSB

```java
String accountNumber = "062001-123456789";
String bsb = generator.extractBSB(accountNumber);
// 返回: 062001
```

---

### 4. 提取账号部分

```java
String accountNumber = "062001-123456789";
String accountPart = generator.extractAccountPart(accountNumber);
// 返回: 123456789
```

---

## 💡 在 Service 中使用

### 完整的 AccountService 示例

```java
package com.banking.service;

import com.banking.entity.Account;
import com.banking.entity.User;
import com.banking.repository.AccountRepository;
import com.banking.repository.UserRepository;
import com.banking.util.AccountNumberGenerator;
import com.banking.dto.AccountDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    @Autowired
    private AccountNumberGenerator accountNumberGenerator;
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * 为用户创建账户（自动生成账号）
     */
    @Transactional
    public Account createAccountForUser(Long userId, String accountName) {
        // 查找用户
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 生成唯一账号
        String accountNumber = generateUniqueAccountNumber(userId);
        
        // 创建账户
        Account account = new Account();
        account.setAccountName(accountName);
        account.setAccountNumber(accountNumber);
        account.setBalance(BigDecimal.ZERO);
        account.setUser(user);
        
        return accountRepository.save(account);
    }
    
    /**
     * 生成唯一账号（确保不重复）
     */
    private String generateUniqueAccountNumber(Long userId) {
        String accountNumber;
        int attempts = 0;
        int maxAttempts = 10;
        
        do {
            // 基于用户ID生成账号
            accountNumber = accountNumberGenerator.generateAccountNumberByUserId(userId);
            attempts++;
            
            if (attempts >= maxAttempts) {
                // 如果10次还重复，使用完全随机的方式
                accountNumber = accountNumberGenerator.generateAccountNumber();
            }
            
        } while (accountRepository.existsByAccountNumber(accountNumber));
        
        return accountNumber;
    }
    
    /**
     * 为指定分支创建账户
     */
    @Transactional
    public Account createAccountForBranch(Long userId, String accountName, int branchCode) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 为指定分支生成账号
        String accountNumber = generateUniqueAccountNumberForBranch(branchCode);
        
        Account account = new Account();
        account.setAccountName(accountName);
        account.setAccountNumber(accountNumber);
        account.setBalance(BigDecimal.ZERO);
        account.setUser(user);
        
        return accountRepository.save(account);
    }
    
    /**
     * 为分支生成唯一账号
     */
    private String generateUniqueAccountNumberForBranch(int branchCode) {
        String accountNumber;
        int attempts = 0;
        int maxAttempts = 10;
        
        do {
            accountNumber = accountNumberGenerator.generateAccountNumberForBranch(branchCode);
            attempts++;
            
            if (attempts >= maxAttempts) {
                throw new RuntimeException("无法生成唯一账号，请稍后重试");
            }
            
        } while (accountRepository.existsByAccountNumber(accountNumber));
        
        return accountNumber;
    }
    
    /**
     * 验证账号格式
     */
    public boolean isValidAccountNumber(String accountNumber) {
        return accountNumberGenerator.validateAccountNumberFormat(accountNumber);
    }
    
    /**
     * 格式化账号用于显示
     */
    public String formatAccountNumberForDisplay(String accountNumber) {
        return accountNumberGenerator.formatAccountNumber(accountNumber, true);
    }
}
```

---

## 🎯 推荐使用场景

### 场景 1: 普通用户开户

```java
// 使用基于用户ID的方式
String accountNumber = accountNumberGenerator.generateAccountNumberByUserId(userId);
```

**优点：**
- 账号包含用户ID信息
- 便于客服查询和追踪
- 可预测性适中

---

### 场景 2: 企业批量开户

```java
// 使用批量生成
String[] accountNumbers = accountNumberGenerator.generateBatchAccountNumbers(100);
```

**优点：**
- 效率高
- 保证唯一性
- 适合大量开户

---

### 场景 3: 多分支银行

```java
// 指定分支代码
String accountNumber = accountNumberGenerator.generateAccountNumberForBranch(branchCode);
```

**优点：**
- 账号包含分支信息
- 便于分支管理
- 支持分支级统计

---

### 场景 4: 高安全要求

```java
// 使用带校验位的账号
String accountNumber = accountNumberGenerator.generateAccountNumberWithChecksum();

// 验证时
if (!accountNumberGenerator.validateAccountNumberWithChecksum(accountNumber)) {
    throw new RuntimeException("账号格式错误");
}
```

**优点：**
- 防止输入错误
- 增加安全性
- 符合行业标准

---

## 📊 Repository 添加方法

在 `AccountRepository` 中添加以下方法：

```java
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    // 现有方法
    Optional<Account> findByAccountName(String accountName);
    Optional<Account> findByAccountNumber(String accountNumber);
    
    // 新增：检查账号是否存在
    boolean existsByAccountNumber(String accountNumber);
    
    // 新增：统计账号数量
    long countByAccountNumber(String accountNumber);
}
```

---

## 🔍 测试

运行测试类查看效果：

```bash
./gradlew test --tests AccountNumberGeneratorTest
```

测试输出示例：

```
生成的账号: 062001-123456789
生成的格式化账号: 062-001-234567890
生成的 BSB: 062015
用户 12345 的账号: 062023-012345456
基于日期的账号: 062001-241223789

批量生成的账号:
  - 062001-123456789
  - 062015-234567890
  - 062023-345678901
  - 062031-456789012
  - 062045-567890123
```

---

## ⚙️ 配置选项

### 修改银行代码

在 `AccountNumberGenerator.java` 中修改：

```java
// 默认是 062 (CBA 的代码)
private static final String BANK_CODE = "062";

// 可以改为其他银行代码
private static final String BANK_CODE = "123";  // 你的银行代码
```

### 修改账号长度

```java
// 默认是9位
private static final int ACCOUNT_NUMBER_LENGTH = 9;

// 可以改为其他长度（如8位）
private static final int ACCOUNT_NUMBER_LENGTH = 8;
```

---

## 🚨 注意事项

### 1. 唯一性保证

生成账号后必须检查数据库中是否已存在：

```java
String accountNumber;
do {
    accountNumber = generator.generateAccountNumber();
} while (accountRepository.existsByAccountNumber(accountNumber));
```

### 2. 并发处理

在高并发场景下，建议在数据库层面添加唯一约束：

```sql
ALTER TABLE account ADD CONSTRAINT uk_account_number 
    UNIQUE (account_number);
```

### 3. 性能考虑

批量生成时会有短暂延迟（1ms），确保时间戳不同：

```java
// 如果需要更快速度，可以移除延迟，但可能产生相同账号
// 需要在应用层做唯一性检查
```

### 4. 格式兼容性

工具支持多种格式的验证和解析：

```java
// 以下格式都有效
"062001-123456789"      // 标准格式
"062001123456789"       // 无横杠
"062-001-123456789"     // 完整格式
"062 001 123456789"     // 带空格（会自动清理）
```

---

## 📖 相关文档

- [Entity Relationship Improvement](ENTITY_RELATIONSHIP_IMPROVEMENT.md) - 实体关系改进
- [Environment Config Guide](ENVIRONMENT_CONFIG_GUIDE.md) - 环境配置
- [Quick Reference](QUICK_REFERENCE.md) - 快速参考

---

## 🎉 总结

### 核心优势

1. ✅ **符合国际标准** - 参照 CBA 格式
2. ✅ **多种生成方式** - 支持不同业务场景
3. ✅ **唯一性保证** - 使用时间戳 + 随机数
4. ✅ **格式验证** - 完整的验证工具
5. ✅ **易于使用** - Spring Bean 注入即用
6. ✅ **可扩展** - 支持自定义配置

### 快速开始

```java
@Autowired
private AccountNumberGenerator generator;

// 生成账号
String accountNumber = generator.generateAccountNumber();

// 验证格式
boolean isValid = generator.validateAccountNumberFormat(accountNumber);

// 格式化显示
String formatted = generator.formatAccountNumber(accountNumber, true);
```

---

**祝你使用愉快！** 🚀

