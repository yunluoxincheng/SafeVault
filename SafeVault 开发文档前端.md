# Android 密码管理应用 —— 前端开发文档（草稿）

## 概述
本文件为 Android 原生密码管理应用的 **前端开发文档草稿**，基于 Java + Android 原生技术栈。文档内容覆盖：UI 设计、交互流程、模块结构、数据模型、与后端协作方式、自动填充服务设计等。

---

# 1. 技术栈
- Android SDK（Min SDK 建议 23）
- Java 8+
- Jetpack：
  - Room（仅访问 Repository，不操作加密层）
  - ViewModel
  - LiveData / StateFlow
- Material Components
- AutofillService API
- RecyclerView / ConstraintLayout
- Retrofit（如果启用云同步）

---

# 2. 前端整体架构
```
Presentation (Activity / Fragment)
        ↓
ViewModel (业务逻辑、状态)
        ↓
Repository Interface（由后端实现）
```
前端只处理：
- UI
- 状态管理
- 用户交互
- 发起请求到后端服务
- 渲染后端返回的数据

前端 **不处理加密、不处理数据库、不保存明文密码**。

---

# 3. UI 页面设计

### 3.1 登录 / 解锁页面
- 输入主密码
- 生物识别解锁（指纹）
- 调用前端接口：`backend.unlock(masterPassword)`
- 成功后跳转主界面

### 3.2 密码列表页
- RecyclerView 以卡片方式展示条目标题
- 搜索栏（本地搜索，由后端执行）
- 悬浮按钮：创建新条目

### 3.3 密码详情页
- 展示（经过后端解密后的）账号信息
- 复制按钮（受剪贴板保护）
- 显示/隐藏密码

### 3.4 新增 / 编辑密码页面
- 输入字段：标题/账号/密码/URL/备注
- 密码生成器弹窗（调用 backend.generatePassword()）
- 完成时调用：`backend.savePassword(item)`

### 3.5 设置页面
- 同步设置
- 导入导出
- 安全设置（生物识别/自动锁定）

---

# 4. 交互流程

### 4.1 解锁流程
```
用户输入主密码 → 前端 → backend.unlock()
    成功 → 渲染主界面
    失败 → 错误提示
```

### 4.2 搜索流程
```
前端监听搜索框 → 调 backend.search(query) → LiveData 更新
```

### 4.3 创建条目流程
```
用户输入 → 点击保存 → backend.encryptAndSave(item) → 返回 itemId
```

### 4.4 自动填充流程
前端需要实现：
- AutofillService
- Dataset 构建
- UI 提示用户选择账号

数据通过 backend 提供：
```
autofillBackend.getCredentialsForDomain(domain)
```

---

# 5. 前端模块结构

```
ui/
  LoginActivity
  MainActivity
  PasswordListFragment
  PasswordDetailFragment
  EditPasswordFragment

viewmodel/
  LoginViewModel
  PasswordListViewModel
  PasswordDetailViewModel
  EditPasswordViewModel

autofill/
  AutofillServiceImpl

adapter/
  PasswordListAdapter
```

---

# 6. 前端数据模型（明文模型，未加密）
```
class PasswordItem {
  int id;
  String title;
  String username;
  String password;
  String url;
  String notes;
  long updatedAt;
}
```

---

# 7. 与后端的接口（由后端提供）

```
interface BackendService {
  boolean unlock(String masterPassword);
  PasswordItem decryptItem(int id);
  List<PasswordItem> search(String query);
  int saveItem(PasswordItem item);
  boolean deleteItem(int id);
  String generatePassword(int length, boolean symbols);
  List<PasswordItem> getCredentialsForDomain(String domain);
}
```

前端不关心加密与数据库，只调用这些接口。

---

# 8. 前端的安全措施
- 使用 FLAG_SECURE 禁止截图
- 剪贴板自动清理
- Activity 切到后台自动锁定（调用 backend.lock()）
- TOTP 页面不截图、不缓存

---

# 9. Sprint 分工（前端部分）

### Sprint 1
- UI 框架搭建（MainActivity + Navigation）
- 登录页 UI + 交互
- 密码列表 UI

### Sprint 2
- 新增/编辑页面 UI
- 查看详情 UI
- 密码生成器 UI

### Sprint 3
- AutofillService MVP
- 搜索交互

### Sprint 4
- 设置页面
- 动效微调
- Bug 修复

---

（草稿完毕。后台文档将另建。）

