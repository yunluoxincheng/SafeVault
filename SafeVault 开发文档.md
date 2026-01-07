Android 原生密码管理器开发文档

一、项目概述

本项目旨在开发一款 Android 原生（Java）密码管理应用，支持密码生成、加密存储、自动填充、多设备同步、安全审计、双因素认证及密码共享等高级功能。该应用将采用零知识安全架构，确保用户敏感数据在任何时刻均以加密形式存储和传输。

二、产品功能需求

1. 核心功能（MVP 必备）

主密码与生物识别登录

密码生成器（可选长度/字符集）

加密存储密码与账户信息

密码分类和标签管理

本地数据加密数据库（SQLCipher）

添加/删除/编辑密码条目

自动锁屏/会话过期

2. 进阶功能（后续版本）

自动填充（Android Autofill Service）

密码强度分析与安全审计

双因素认证（TOTP）

多设备端到端加密同步

密码共享（家庭/团队）

导入/导出（加密文件）

弱密码提醒/重复密码检查

三、系统架构设计

采用 MVVM + Repository + Crypto 分层架构。

┌──────────────────────┐
│ Presentation Layer    │
│ Activities / Fragments│
│ ViewModels            │
└──────────┬───────────┘
           │
┌──────────▼───────────┐
│ Domain Layer           │
│ UseCases               │
└──────────┬────────────┘
           │
┌──────────▼───────────┐
│ Data Layer             │
│ Room + SQLCipher       │
│ Repository              │
└──────────┬────────────┘
           │
┌──────────▼───────────┐
│ Crypto Layer           │
│ CryptoManager          │
│ KeyStoreHelper         │
└──────────┬────────────┘
           │
┌──────────▼───────────┐
│ Autofill Service       │
└───────────────────────┘

四、安全设计

1. 安全架构原则

零知识（Zero-Knowledge）：所有敏感信息只在本地设备解密。

最小权限：只请求必要权限。

数据独立性：服务器无法读取任何明文信息。

2. 密钥与加密机制

项目

技术

描述

主密码

PBKDF2/Argon2

派生 KEK（不存明文）

数据加密

AES-GCM 256

加密密码/用户名/TOTP

密钥存储

Android Keystore

保存 DEK 或密钥包装密钥

数据库

SQLCipher

全库加密

3. 安全策略

禁止截屏（FLAG_SECURE）

自动锁定（后台超过 X 分钟）

生物识别解锁

错误登录次数限制

五、主要模块设计

1. CryptoManager

负责 AES-GCM 加解密

负责 PBKDF2 密钥派生

负责 keystore 密钥管理

2. Repository（数据仓库）

统一处理数据库操作

屏蔽加密解密细节

3. PasswordGenerator

基于 SecureRandom

支持可配置参数

4. Autofill Service

提供密码选择面板

根据 URL/App Package 匹配

自动填充前验证

5. BackupManager

提供加密 JSON 文件导出

导入时解密验证兼容性

6. SyncManager（未来迭代）

云端仅存密文

E2EE 同步协议

六、数据模型设计

Credential 表（简化示例）

字段

类型

描述

id

long

主键

title

String

服务名称

usernameEnc

Blob

加密用户名

passwordEnc

Blob

加密密码

url

String

登录地址

tags

String

标签

ivUsername

Blob

AES-GCM IV

ivPassword

Blob

AES-GCM IV

updatedAt

long

用于同步

TOTP 表

| 字段 | 类型 | 描述 | | secretEnc | Blob | 加密 TOTP 秘钥 | | issuer | String | 服务提供商 | | account | String | 登录账户 | | period | int | 默认30秒周期 |

七、UI 设计（核心页面）

登录/解锁界面：主密码 + 生物识别

首页密码列表：支持搜索、分组、标签

密码详情页：复制、显示/隐藏密码、编辑

新增密码页：密码生成器、表单输入

设置页：安全设置、导入导出、同步、主题

自动填充选择界面

八、开发流程（里程碑）

📌 版本 0.1 — MVP（本地安全存储）

主密码设置流程

数据库 + SQLCipher

CryptoManager 开发

添加/编辑/删除密码

密码生成器

生物识别解锁

自动锁屏机制

📌 版本 0.2 — 自动填充

AutofillService 实现

自动填充 UI

自动匹配账号

📌 版本 0.3 — 安全审计模块

密码强度评分

弱密码提醒

重复密码检测

📌 版本 0.4 — 多设备同步

E2EE 协议定义

云端接口实现

同步冲突处理

📌 版本 0.5 — 双因素认证

TOTP 扫码导入

生成一次性密码

自动填充支持

📌 版本 1.0 — 密码共享

加密共享通道

团队管理（基础版本）

九、测试与质量保证

单元测试（Crypto/Repo/UseCase）

UI 自动化测试（Espresso）

安全测试（MobSF）

依赖漏洞扫描

十、发布与合规

Play Store 隐私政策

不上传敏感数据的声明

匿名分析（可选且需用户授权）

应用签名与安全配置

十一、未来扩展

跨平台客户端（iOS/PC）

浏览器扩展（Chrome/Edge/Firefox）

企业团队版

远程擦除

密码泄露监测 API（haveibeenpwned）

十二、总结

该文档涵盖了本项目从需求、架构、安全、数据库、模块、UI 到开发阶段规划的完整设计，可直接用作项目立项文档、开发蓝图或团队协作基准文件。

附录 A — Bitwarden 加密流程图（文本版）

客户端加密与 Vault 流程（步骤）

用户输入邮箱 + 主密码（password）

客户端生成随机 salt（若首次）并使用 PBKDF2/Argon2 对主密码派生 Master Key（MK）

使用 MK 生成或解密 Vault Key（VK）与条目密钥（Item Keys）

每个条目（Cipher）使用单独的 Item Key 对条目内敏感字段（username/password/totp/notes）进行 AES 加密

Item Key 被 MK 加密后存储于 Vault 元数据中

客户端将加密后的 Vault（包含被 MK 加密的 Item Keys 与各条目的密文）上传到服务器或持久化到本地

任何时候解密：客户端使用 MK 解密对应 Item Key，再用 Item Key 解密单条目数据

备注：该流程保证了服务器无法读取任何明文数据。条目级别密钥使得单条共享或旋转密钥更容易实现。

附录 B — 建议的完整数据库模式（SQL）

表：users

id (INTEGER PRIMARY KEY AUTOINCREMENT)

email (TEXT UNIQUE)

master_salt (BLOB) -- PBKDF2/Argon2 salt

master_hash (BLOB) -- 用于登录验证（派生后 hash）

created_at (INTEGER)

updated_at (INTEGER)

表：vault_items

id (INTEGER PRIMARY KEY AUTOINCREMENT)

uuid (TEXT UNIQUE)

user_id (INTEGER) REFERENCES users(id)

type (INTEGER) -- 1:login,2:card,3:identity,4:note

name (TEXT) -- 可明文

folder_id (INTEGER NULL)

favorite (INTEGER) -- 0/1

organization_id (INTEGER NULL)

item_key_enc (BLOB) -- Item Key 被 Master Key 加密后的密文

data_enc (BLOB) -- 整个条目的密文（可选：或把敏感字段拆成多列）

updated_at (INTEGER)

created_at (INTEGER)

version (INTEGER) -- 用于同步/冲突解决

表：vault_fields

id (INTEGER PRIMARY KEY AUTOINCREMENT)

item_id (INTEGER) REFERENCES vault_items(id)

field_type (TEXT) -- username/password/totp/custom

name (TEXT)

value_enc (BLOB)

iv (BLOB)

表：folders

id (INTEGER PRIMARY KEY AUTOINCREMENT)

uuid (TEXT UNIQUE)

user_id (INTEGER)

name (TEXT)

表：totp

id (INTEGER PRIMARY KEY AUTOINCREMENT)

item_id (INTEGER) REFERENCES vault_items(id)

secret_enc (BLOB)

issuer (TEXT)

account_name (TEXT)

period (INTEGER DEFAULT 30)

digits (INTEGER DEFAULT 6)

表：shared_items

id

item_id

owner_user_id

recipient_user_id

encrypted_key_for_recipient (BLOB)

status (INTEGER) -- pending/accepted/revoked

表：audit_logs

id

user_id

event_type (TEXT)

event_time (INTEGER)

metadata (TEXT)

附录 C — UML 类图（文本化）

以下使用简洁的类说明（便于直接转为 PlantUML）

class CryptoManager {
  +deriveMasterKey(password: char[], salt: byte[]): SecretKey
  +encrypt(plaintext: byte[], key: SecretKey): CipherText
  +decrypt(ciphertext: byte[], iv: byte[], key: SecretKey): byte[]
  +generateRandomBytes(len: int): byte[]
}

class KeyStoreHelper {
  +generateKey(alias: String)
  +getKey(alias: String): SecretKey
  +wrapKey(key: SecretKey, wrappingKeyAlias: String): byte[]
  +unwrapKey(wrapped: byte[], wrappingKeyAlias: String): SecretKey
}

class VaultRepository {
  +getItem(uuid: String): VaultItem
  +saveItem(item: VaultItem)
  +deleteItem(uuid: String)
}

class AutofillService {
  +onFillRequest(request: FillRequest): FillResponse
  +onSaveRequest(request: SaveRequest)
}

class AuthManager {
  +login(email: String, password: char[]): boolean
  +logout()
  +enableBiometric()
}

class TOTPManager {
  +generateCode(secret: byte[], time: long): String
  +verifyCode(secret: byte[], code: String): boolean
}

VaultRepository --> CryptoManager
AuthManager --> CryptoManager
AutofillService --> VaultRepository
TOTPManager --> CryptoManager
KeyStoreHelper --> CryptoManager

附录 D — 架构图（模块交互说明）

启动/登录流程

App 启动 -> AuthManager 检查本地会话 -> 若无会话跳转登录页

登录时：AuthManager 调用 CryptoManager.deriveMasterKey -> 生成 MK -> 验证 master_hash（本地或服务器）-> 成功后加载 Vault（Repository）

读取凭据流程

UI 请求 VaultRepository.getItem

Repository 读取 item_key_enc 与 data_enc

Repository 用 CryptoManager 解密 item_key，再解密字段

返回明文给 ViewModel -> UI（显示后尽快清零）

自动填充流程

系统触发 AutofillService.onFillRequest

AutofillService 查询 VaultRepository（可能先触发生物认证）

返回 FillResponse 给系统

备份/导出流程

BackupManager 请求 Master Key 派生 -> 使用 MK 对 vault 导出数据逐条加密 -> 生成加密文件并签名（HMAC）

附录 E — Sprint / 迭代级任务分解（可直接导入 Jira）

Sprint 0 — 准备（1 周）

项目初始化（Android Studio 项目、CI 模板）

设定 code style、PR 流程

确定最低 SDK、依赖库清单

设计初始数据库 schema

Jira 任务示例:

PROJ-0 Init project scaffold

PROJ-1 Setup CI (GitHub Actions)

PROJ-2 Define dependencies & lint rules

Sprint 1 — 核心加密与存储（2 周）

实现 CryptoManager（PBKDF2、AES-GCM）

集成 Android Keystore（KeyStoreHelper）

Room + SQLCipher 基础集成

VaultRepository 基本 CRUD

单元测试 CryptoManager

验收标准:

Master Key 派生测试通过

数据库中可保存加密条目并能解密回文

Sprint 2 — 基本 UI 与 Auth（2 周）

登录/首次启动流程

主界面列表（mock 数据）

添加/编辑凭据页面（含密码生成器）

BiometricPrompt 集成（解锁）

Sprint 3 — Autofill 与权限（2 周）

实现 AutofillService 基础

AutoFill 的安全性策略（生物/主密码验证）

测试不同浏览器/APP 的字段匹配

Sprint 4 — 备份 / 导出 / 导入（1 周）

实现加密导出与导入

文件格式与版本兼容（包含 salt/iterations/version）

Sprint 5 — 安全审计模块（1 周）

密码强度检测

重复密码扫描

UI 报表

Sprint 6 — TOTP 与测试（2 周）

实现 TOTPManager（RFC6238）

在凭据页面集成 TOTP 显示

集成单元 + UI 测试（Espresso）

Sprint 7 — Refinement & Release Prep（2 周）

全面测试（功能/安全/兼容）

修复关键 bug

Play Store 发布准备（隐私政策、截图、描述）

附录 F — 每个任务的验收准则（示例）

功能点必须有单元测试覆盖（关键加密路径）

UI 关键流程有 Espresso 或手工测试记录

安全路径通过静态扫描（MobSF）

Play Store 准备项全部填写并获得内部测试通过

附录 G — CI / Release 流程建议

PR -> 自动化构建（lint、unit tests、security scan）

main 分支 -> nightly build（自动化测试）

release 分支 -> 打包、签名、内部测试发布

使用 Play Console internal testing track 进行首轮验证

附录 H — 风险与缓解措施

密钥泄露风险

缓解：使用 Android Keystore，不导出主密钥；强制复杂主密码。

Autofill 泄露风险

缓解：在 Autofill 回调中要求生物验证；只返回最小必要字段。

同步冲突

缓解：Version 与 timestamp 基于 item 级别解决冲突；优先本地最新或用户选择覆盖。

如果你需要，我可以把以上 UML 模式直接转为 PlantUML 代码、把数据库模式导出为可执行的 SQL 文件，或者把 Sprint 任务导出为 CSV/JSON 便于导入 Jira。

前后端任务分离（两人团队开发规划）

以下将整个密码管理应用的开发任务明确拆分为 前端工程师（Android Java） 与 后端工程师（加密/数据/同步/服务逻辑） 的职责范围。

🟦 前端工程师（Android 原生 Java）职责

1. 应用界面 & 用户交互（UI/UX）

实现所有 Activity / Fragment：

登录/解锁界面（主密码输入、生物识别）

密码列表页面

密码详情页面

添加/编辑密码界面

设置界面（自动填充、同步、主题）

搜索界面

动效、组件设计、Material Design 实现。

2. 本地功能交互（调用后端逻辑）

输入主密码 → 调用后端密钥推导流程。

创建/编辑条目 → 调用后端数据库写入接口。

搜索密码 → 调用后端查询接口。

密码强度展示 → 调用后端密码评分 API。

3. Android 自动填充（AutofillService）

实现 AutofillService：

监听浏览器/APP 的登录表单

调用后端查询匹配的条目

自动填充用户名/密码

4. UI 层的安全措施

防截图（FLAG_SECURE）

剪贴板保护

静态资源安全（不在 XML 中写死敏感内容）

5. 网络层（如果支持同步）

Retrofit/OkHttp 进行 API 调用。

Token 管理（调用后端生成）。

6. 本地缓存展示

列表缓存、RecyclerView adapter

离线提示与数据更新提示

🟥 后端工程师（安全、加密、数据库、同步）职责

1. 主密码 & 密钥管理

PBKDF2/HKDF 密钥推导实现（主密钥、加密密钥、MAC 密钥）。

不保存主密码，仅保存计算参数（盐、迭代次数）。

内存中敏感数据擦除。

2. 加密模块

AES-256-GCM 封装：加密/解密统一接口。

条目字段加密（标题、账号、密码、自定义字段）。

数据完整性校验（HMAC 或 GCM tag）。

生成随机密码 API。

3. 本地数据库（Room/SQLite）

数据库 schema 设计（已在文档中）。

提供 Repository API：

addPassword()

updatePassword()

deletePassword()

searchPasswords()

数据加密/解密在 DAO 之外进行。

4. 密码强度审计

实现密码评分算法（zxcvbn 或自实现）。

提供前端可调用的 API。

5. 自动填充后台逻辑

通过域名/APP 包名匹配条目。

提供 AutofillService 调用的接口。

6. 同步（可选模块）

若未来加入云同步：

API 设计（登录、上传、下载、冲突解决）。

Token 生成、加密传输（TLS + 端对端加密）。

本地与云数据合并。

7. TOTP（可选）

TOTP 密钥加密存储。

生成动态六位验证码。

与条目绑定。

🟩 前后端协作方式（建议）

1. 确定统一的数据模型

例如：PasswordItem

{
  id: int,
  title: String,
  username: String,
  password: String,
  url: String,
  notes: String,
  updatedAt: long
}

前端：传输明文模型

后端：负责所有加密逻辑

2. 明确接口边界

前端不会直接访问数据库，也不会直接做加密。所有敏感行为都经由后端模块实现。

3. 严格控制明文出现位置

明文仅存在短暂的内存中，从 UI 输入 → 传给后端 → 马上擦除 UI 输入缓存。

4. 单元测试分工

后端：加密、数据库、同步逻辑的 Unit Test。

前端：UI Test（Espresso）、交互流程测试。

如果你希望，我还可以继续将整个文档按“前端部分/后端部分”重新重排、拆分成两个文档，或生成可导出的 PDF/Markdown。