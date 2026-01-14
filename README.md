# SafeVault - Android密码管理器

<div align="center">

![SafeVault](https://img.shields.io/badge/SafeVault-v1.1.6-brightgreen)
![Platform](https://img.shields.io/badge/platform-Android%2010%2B-blue)
![Language](https://img.shields.io/badge/language-Java%2017-orange)
![License](https://img.shields.io/badge/license-MIT-green)

**安全的本地密码管理器，支持云端同步和密码分享**

</div>

## 目录

- [项目简介](#项目简介)
- [主要功能](#主要功能)
- [技术栈](#技术栈)
- [系统要求](#系统要求)
- [快速开始](#快速开始)
- [功能详解](#功能详解)
- [项目结构](#项目结构)
- [开发指南](#开发指南)
- [版本历史](#版本历史)

---

## 项目简介

SafeVault是一款原生Android密码管理器应用，采用前后端分离架构设计。前端负责UI展示和用户交互，所有加密存储和数据持久化操作由后端服务处理。

### 设计理念

- **安全第一**：所有加密操作在后端完成，前端不处理任何加密逻辑
- **隐私保护**：支持本地存储和云端同步，用户完全掌控数据
- **便捷分享**：支持多种分享方式，包括二维码、蓝牙、NFC和云端分享
- **无缝体验**：集成Android自动填充服务，提供流畅的密码管理体验

---

## 主要功能

### 核心功能

| 功能 | 说明 |
|------|------|
| 密码管理 | 创建、编辑、删除、搜索密码条目 |
| 密码生成器 | 自定义长度和字符类型的强密码生成 |
| 生物识别 | 支持指纹和面部识别快速解锁 |
| 自动锁定 | 应用进入后台后自动锁定，保护数据安全 |
| 自动填充 | Android系统级自动填充服务集成 |

### 分享功能

| 方式 | 说明 |
|------|------|
| 二维码 | 生成包含加密密码的二维码，扫码即可接收 |
| 蓝牙 | 近距离蓝牙传输，无需网络 |
| NFC | 一碰即传，快捷便利 |
| 云端分享 | 生成分享链接，支持远程分享 |
| 附近用户 | 基于位置发现附近SafeVault用户并分享 |

### 云端功能

| 功能 | 说明 |
|------|------|
| 云端同步 | 多设备间密码数据同步 |
| 账号管理 | 用户注册、登录、邮箱验证 |
| 分享历史 | 查看所有分享记录和接收记录 |
| 设备管理 | 管理已登录设备，远程退出 |

---

## 技术栈

### 前端技术

- **语言**: Java 17
- **架构**: MVVM (Model-View-ViewModel)
- **UI框架**: Material Design 3 + ConstraintLayout
- **导航**: Android Navigation Component
- **数据绑定**: ViewBinding
- **异步处理**: RxJava 3
- **图片加载**: Glide
- **二维码**: ZXing

### 网络层

- **REST API**: Retrofit 2 + OkHttp 4
- **WebSocket**: 实时通知
- **数据解析**: Gson

### 系统集成

- **生物识别**: AndroidX Biometric
- **自动填充**: Android AutofillService
- **位置服务**: Google Play Services Location
- **通知**: Android Notification Channel

---

## 系统要求

| 项目 | 要求 |
|------|------|
| 最低版本 | Android 10 (API 29) |
| 目标版本 | Android 14 (API 36) |
| 存储空间 | 约 50MB |
| 权限 | 生物识别、相机、蓝牙、NFC、位置（可选） |

---

## 快速开始

### 前置要求

- Android Studio Hedgehog | 2023.1.1 或更高版本
- JDK 17
- Android SDK 36

### 克隆项目

```bash
git clone https://github.com/yunluoxincheng/SafeVault.git
cd SafeVault
```

### 构建项目

```bash
# 清理构建
./gradlew clean

# 构建Debug版本
./gradlew assembleDebug

# 构建Release版本
./gradlew assembleRelease

# 运行测试
./gradlew test
```

### 安装到设备

```bash
# 通过USB安装到连接的设备
./gradlew installDebug
```

### 首次运行

1. 启动应用后，首次使用需要设置主密码
2. 输入并确认主密码（请妥善保管，无法找回）
3. 开启生物识别解锁（可选）
4. 开始使用SafeVault管理密码

---

## 功能详解

### 密码管理

#### 添加密码

1. 点击主界面的「+」按钮
2. 填写网站名称、用户名、密码
3. 可选填写网站URL和备注
4. 点击保存

#### 搜索密码

- 在密码列表页面的搜索框中输入关键词
- 支持搜索标题、用户名、URL等字段
- 实时显示匹配结果

#### 编辑/删除密码

1. 长按密码条目或点击详情
2. 编辑或删除密码信息

### 密码生成器

#### 使用方法

1. 进入「生成器」标签页
2. 调整密码长度（8-32位）
3. 选择字符类型：大写、小写、数字、符号
4. 查看密码强度指示器
5. 点击生成并复制

#### 预设配置

- **PIN码**: 4位数字
- **强密码**: 16位混合字符
- **易记密码**: 12位可读字符

### 密码分享

#### 离线分享

**二维码分享**
1. 打开密码详情
2. 点击「分享」按钮
3. 选择「二维码」方式
4. 对方使用SafeVault扫码即可接收

**蓝牙分享**
1. 选择「蓝牙」分享方式
2. 确保对方蓝牙已开启
3. 配对后传输数据

**NFC分享**
1. 选择「NFC」分享方式
2. 两台设备NFC感应区靠近
3. 一碰即传

#### 云端分享

**分享链接**
1. 选择「云端链接」方式
2. 设置权限（可查看、可保存）
3. 设置过期时间
4. 生成分享链接或二维码

**用户间分享**
1. 选择「用户」分享方式
2. 选择目标用户
3. 发送分享请求
4. 对方接受后即可查看

### 自动填充服务

#### 启用自动填充

1. 进入手机「设置」→「系统」→「语言和输入法」→「高级」→「自动填充服务」
2. 选择「SafeVault」
3. 确认启用

#### 使用自动填充

1. 在其他应用中登录时，点击用户名或密码输入框
2. 系统弹出SafeVault自动填充界面
3. 选择对应的密码条目
4. 自动填充用户名和密码

#### 保存新密码

1. 登录新网站后，SafeVault会提示保存密码
2. 点击「保存」
3. 确认信息后添加到密码库

### 云端同步

#### 注册账号

1. 进入「设置」→「账号与安全」
2. 点击「注册云端账号」
3. 填写用户名、密码、邮箱
4. 完成邮箱验证

#### 登录同步

1. 点击「登录云端账号」
2. 输入用户名和密码
3. 首次登录可选择合并或覆盖本地数据

#### 设备管理

1. 进入「设备管理」
2. 查看所有已登录设备
3. 可远程退出其他设备

---

## 项目结构

```
com.ttt.safevault/
├── ui/                              # UI组件
│   ├── LoginActivity.java           # 登录页面
│   ├── MainActivity.java            # 主容器
│   ├── PasswordListFragment.java    # 密码列表
│   ├── PasswordDetailFragment.java  # 密码详情
│   ├── EditPasswordFragment.java    # 编辑密码
│   ├── GeneratorFragment.java       # 密码生成器
│   ├── SettingsFragment.java        # 设置页面
│   ├── share/                       # 分享相关UI
│   │   ├── ShareActivity.java       # 分享配置
│   │   ├── ReceiveShareActivity.java # 接收分享
│   │   ├── ShareHistoryFragment.java # 分享历史
│   │   └── NearbyUsersActivity.java  # 附近用户
│   └── autofill/                    # 自动填充UI
│       ├── AutofillSaveActivity.java        # 保存密码
│       └── AutofillCredentialSelectorActivity.java # 凭据选择
├── viewmodel/                       # MVVM ViewModels
│   ├── AuthViewModel.java           # 认证视图模型
│   ├── PasswordViewModel.java       # 密码视图模型
│   └── ShareViewModel.java          # 分享视图模型
├── model/                           # 数据模型
│   ├── BackendService.java          # 后端服务接口
│   └── PasswordItem.java            # 密码条目模型
├── service/                         # 后端服务实现
│   └── BackendServiceImpl.java      # 后端服务实现
├── crypto/                          # 加密管理
│   └── CryptoManager.java           # 加密管理器
├── security/                        # 安全工具
│   ├── SecurityManager.java         # 安全管理器
│   └── KeyManager.java              # 密钥管理器
├── network/                         # 网络层
│   ├── RetrofitClient.java          # Retrofit客户端
│   ├── TokenManager.java            # Token管理
│   └── api/                         # API接口
├── dto/                             # 数据传输对象
│   ├── request/                     # 请求对象
│   └── response/                    # 响应对象
├── autofill/                        # 自动填充服务
│   └── SafeVaultAutofillService.java # 自动填充服务
├── service/                         # 后台服务
│   └── ShareNotificationService.java # 分享通知服务
├── receiver/                        # 广播接收器
│   └── ShareNotificationReceiver.java # 分享通知接收器
├── utils/                           # 工具类
│   ├── PasswordStrengthCalculator.java # 密码强度计算
│   ├── ClipboardManager.java        # 剪贴板管理
│   └── BluetoothTransferManager.java # 蓝牙传输管理
└── adapter/                         # 适配器
    ├── PasswordListAdapter.java     # 密码列表适配器
    └── ShareHistoryAdapter.java     # 分享历史适配器
```

---

## 开发指南

### 架构设计

SafeVault采用MVVM架构模式：

```
┌─────────────────┐
│     UI Layer    │  Activities / Fragments
└────────┬────────┘
         │
┌────────▼────────┐
│  ViewModel      │  管理UI状态和业务逻辑
└────────┬────────┘
         │
┌────────▼────────┐
│  BackendService │  后端服务接口
└────────┬────────┘
         │
┌────────▼────────┐
│  BackendImpl    │  加密/存储实现
└─────────────────┘
```

### 核心接口

#### BackendService接口

```java
// 认证相关
boolean unlock(String masterPassword);
void lock();
boolean isInitialized();
boolean initialize(String masterPassword);
boolean changeMasterPassword(String oldPassword, String newPassword);

// 密码管理
PasswordItem decryptItem(int id);
List<PasswordItem> search(String query);
int saveItem(PasswordItem item);
boolean deleteItem(int id);
List<PasswordItem> getAllItems();

// 自动填充
List<PasswordItem> getCredentialsForDomain(String domain);

// 密码生成
String generatePassword(int length, boolean useUppercase,
                       boolean useLowercase, boolean useNumbers,
                       boolean useSymbols);

// 数据导入导出
boolean exportData(String exportPath);
boolean importData(String importPath);
```

### 安全特性

- **FLAG_SECURE**: 所有页面禁止截屏录屏
- **自动锁定**: 后台超时自动锁定
- **剪贴板保护**: 复制后30秒自动清除
- **生物识别**: 支持指纹/面部识别
- **加密存储**: AES-256加密算法

### 调试模式

启用调试日志：

```java
// 在Application中设置
SafeVaultApplication.setDebugMode(true);
```

查看自动填充日志：

```bash
adb shell
run-as com.ttt.safevault
cat files/autofill_debug.log
```

### 构建变体

```bash
# Debug构建
./gradlew assembleDebug

# Release构建
./gradlew assembleRelease

# 查看构建报告
./gradlew assembleDebug --info
```

---

## 版本历史

### v1.1.6 (最新)

- 修复自动锁定功能问题
- 优化后台锁定机制
- 改进应用生命周期管理

### v1.1.5

- 实现云端登录功能
- 完善密码分享功能
- 优化自动填充体验

### v1.1.4

- UI界面优化
- 交互体验提升
- 动画效果改进

### v1.1.3

- 应用图标升级
- UI细节优化

### v1.1.2

- 交互方式升级
- UI界面升级

### v1.1.1

- 删除好友功能
- 实现分享后端基础功能

### v1.1.0

- 实现自动填充功能
- 集成Android AutofillService

### v1.0.5

- 更新应用图标

### v1.0.4 - v1.0.2

- 实现生物识别功能
- 支持指纹解锁

### v1.0.1

- UI修改优化

### v1.0.0

- 基础功能实现
- 密码管理核心功能

---

## 贡献指南

欢迎贡献代码、报告问题或提出建议！

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

---

## 联系方式

- 作者: yunluoxincheng
- 邮箱: yunluoxincheng@outlook.com
- 项目链接: [https://github.com/yunluoxincheng/SafeVault](https://github.com/yunluoxincheng/SafeVault)

---

<div align="center">

**Made with ❤️ for Android Security**

</div>
