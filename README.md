# SafeVault Android密码管理器 - 后端集成指南

## 项目概述

SafeVault是一个原生Android密码管理器应用，采用前后端分离架构。前端负责UI展示和用户交互，后端负责所有加密存储和数据持久化操作。

**关键信息**
- 包名：`com.ttt.safevault`
- 构建namespace：`com.ttt.safevault`
- 目标Android版本：Android 10+ (最小SDK 23，目标SDK 36)
- 架构：MVVM (Model-View-ViewModel)

## 前端架构

### 技术栈
- **语言**: Java 17
- **UI框架**: Material Components + ConstraintLayout
- **架构模式**: MVVM with Android Jetpack
- **导航**: Android Navigation Component
- **数据绑定**: ViewBinding

### 项目结构
```
com.ttt.safevault/
├── ui/                      # UI组件 (Activities/Fragments)
│   ├── LoginActivity.java   # 登录页面
│   ├── MainActivity.java    # 主容器
│   └── fragments/           # 各种Fragment
├── viewmodel/               # MVVM ViewModels
├── model/                   # 数据模型和BackendService接口
│   ├── PasswordItem.java    # 密码条目模型
│   └── BackendService.java  # 后端服务接口
├── autofill/                # Android自动填充服务
├── security/                # 安全工具类
├── utils/                   # 辅助类
└── adapter/                 # RecyclerView适配器
```

## BackendService接口

后端需要实现`BackendService`接口，该接口定义了前端与后端交互的所有方法。

### 核心方法

#### 1. 认证相关
```java
// 使用主密码解锁应用
boolean unlock(String masterPassword);

// 锁定应用，清除内存中的敏感数据
void lock();

// 检查应用是否已初始化
boolean isInitialized();

// 初始化应用，设置主密码
boolean initialize(String masterPassword);

// 更改主密码
boolean changeMasterPassword(String oldPassword, String newPassword);
```

#### 2. 密码条目管理
```java
// 解密并获取单个密码条目
PasswordItem decryptItem(int id);

// 搜索密码条目（支持标题、用户名、URL等字段）
List<PasswordItem> search(String query);

// 保存或更新密码条目
int saveItem(PasswordItem item);

// 删除密码条目
boolean deleteItem(int id);

// 获取所有密码条目
List<PasswordItem> getAllItems();
```

#### 3. 自动填充支持
```java
// 根据域名获取可用的登录凭据（用于自动填充）
List<PasswordItem> getCredentialsForDomain(String domain);
```

#### 4. 密码生成器
```java
// 简单密码生成
String generatePassword(int length, boolean symbols);

// 高级密码生成（支持多种选项）
String generatePassword(int length, boolean useUppercase,
                       boolean useLowercase, boolean useNumbers,
                       boolean useSymbols);
```

#### 5. 数据导入导出
```java
// 导出数据（加密导出）
boolean exportData(String exportPath);

// 导入数据
boolean importData(String importPath);
```

#### 6. 应用管理
```java
// 获取应用统计信息
AppStats getStats();

// 记录进入后台的时间
void recordBackgroundTime();

// 获取后台时间戳
long getBackgroundTime();

// 获取自动锁定超时时间
int getAutoLockTimeout();
```

## PasswordItem数据模型

```java
public class PasswordItem implements Parcelable {
    private int id;              // 条目ID
    private String title;        // 标题/网站名称
    private String username;     // 用户名/邮箱
    private String password;     // 密码（明文，由后端解密）
    private String url;          // 网站URL
    private String notes;        // 备注
    private long updatedAt;      // 最后更新时间戳

    // 构造函数、getter和setter方法
    // ...
}
```

**注意**：前端接收的`PasswordItem`对象中的密码是明文，后端负责加密存储和解密返回。

## 后端实现要求

### 1. 加密存储
- **必须**使用强加密算法（如AES-256）存储所有密码数据
- 主密码不应以明文存储，建议使用PBKDF2、scrypt或Argon2进行密钥派生
- 所有敏感数据在传输前必须加密

### 2. 数据库设计
建议的数据库表结构：
```sql
CREATE TABLE passwords (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    username TEXT NOT NULL,
    password_encrypted BLOB NOT NULL,  -- 加密后的密码
    url TEXT,
    notes TEXT,
    updated_at INTEGER NOT NULL
);

CREATE TABLE metadata (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);
```

### 3. 安全要求
- 实现自动锁定功能（应用进入后台后超时锁定）
- 支持生物识别认证集成
- 防止截屏和录屏（前端已实现FLAG_SECURE）
- 剪贴板敏感数据自动清除

### 4. 性能要求
- 搜索功能响应时间 < 500ms
- 解密单个条目时间 < 100ms
- 支持至少1000个密码条目的流畅操作

### 5. 错误处理
- 所有方法应处理异常情况
- 提供有意义的错误日志
- 确保敏感信息不会泄露到日志中

## 集成步骤

### 1. 创建BackendService实现类
```java
public class BackendServiceImpl implements BackendService {
    private static BackendService instance;

    public static synchronized BackendService getInstance() {
        if (instance == null) {
            instance = new BackendServiceImpl();
        }
        return instance;
    }

    // 实现所有接口方法...
}
```

### 2. 在前端注册服务
在应用的`onCreate`方法中：
```java
BackendService backendService = BackendServiceImpl.getInstance();
```

### 3. 初始化示例
```java
// 检查是否首次使用
if (!backendService.isInitialized()) {
    // 引导用户设置主密码
    backendService.initialize(masterPassword);
}

// 每次启动时解锁
if (backendService.unlock(masterPassword)) {
    // 解锁成功，加载密码列表
    List<PasswordItem> items = backendService.getAllItems();
}
```

## 自动填充服务集成

前端实现了Android AutofillService，后端需要：
1. 实现`getCredentialsForDomain`方法
2. 支持域名匹配逻辑（包括子域名）
3. 返回匹配的凭据列表

## 数据导入导出格式

建议使用JSON格式，包含版本信息：
```json
{
    "version": "1.0",
    "exported_at": 1640995200000,
    "items": [
        {
            "title": "示例网站",
            "username": "user@example.com",
            "password_encrypted": "base64编码的加密数据",
            "url": "https://example.com",
            "notes": "备注信息",
            "updated_at": 1640995200000
        }
    ]
}
```

## 测试建议

1. **单元测试**：测试所有BackendService方法
2. **加密测试**：验证加密解密的正确性
3. **性能测试**：测试大数据量下的性能表现
4. **安全测试**：确保敏感数据不会泄露

## 联系方式

如有关于后端集成的疑问，请参考：
- 源代码：查看`com.ttt.safevault.model.BackendService`接口
- 数据模型：查看`com.ttt.safevault.model.PasswordItem`类
- 前端使用示例：查看各个ViewModel类的实现

---

**重要提醒**：
- 前端已实现所有UI逻辑和用户交互
- 后端实现必须确保线程安全
- 所有加密操作应在后端完成，前端不处理任何加密逻辑
