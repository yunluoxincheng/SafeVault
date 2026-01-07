# Android 密码管理应用 —— 后端开发文档（草稿）

## 概述
本文件为 Android 原生密码管理应用的 **后端开发文档草稿**。后端职责包括：加密核心、密钥管理、数据持久化、同步 API、TOTP 与审计、以及为前端提供明确的安全接口。该草稿包含函数级的加密模块拆解、数据库详细 schema、Repository 与 API 规范、以及按 Sprint 的开发任务分配。

---

# 1. 技术栈建议
- 语言：Java（同前端）或 Kotlin（可选）
- 本地库：SQLCipher（若后端实现于设备内）或 Room（仅数据结构，配合加密层）
- 加密：Java Cryptography Architecture (JCA)，BouncyCastle（如需）
- 网络（若实现云同步）：Spring Boot / Node.js + Express / Ktor
- 数据库（服务器端）：PostgreSQL / MySQL
- 测试：JUnit, Mockito

---

# 2. 后端职责边界
- **加密逻辑**：主密钥派生、Item Key 管理、字段级加密、TOTP 加密/解密、导入导出加密格式
- **持久化**：设计数据库 schema、实现 Repository CRUD
- **同步 API（可选）**：用户认证、vault 上传/下载、冲突解决
- **安全审计**：登录/导出/共享等事件的审计日志
- **密钥生命周期 & 内存管理**：避免长时间保存明文，及时清零

---

# 3. 数据库 Schema（详细）
见主文档中推荐的完整 schema。这里补充索引、约束与优化建议：
- 为 `vault_items.uuid` 和 `users.email` 添加唯一索引
- 为 `vault_items.updated_at` 添加索引以支持增量同步
- `item_key_enc` 与 `data_enc` 使用 BLOB 类型
- 对审计日志使用分区策略（按月）以避免日志膨胀

---

# 4. 加密模块 —— 函数级拆分

模块名：`CryptoService`（Java 类/组件）

职责：负责所有对称/非对称加解密、密钥派生、随机数生成、内存清零。

```java
public class CryptoService {

  // === 主密钥派生（PBKDF2 / Argon2） ===
  public SecretKey deriveMasterKey(char[] masterPassword, byte[] salt, int iterations, int keyLen);

  // === 随机数与 IV ===
  public byte[] generateRandomBytes(int len);
  public byte[] generateIv(); // 默认 12 bytes for GCM

  // === 数据加密/解密（AES-GCM） ===
  public CipherText encryptAesGcm(byte[] plaintext, SecretKey key);
  public byte[] decryptAesGcm(byte[] ciphertext, byte[] iv, SecretKey key);

  // === Item Key 管理 ===
  public byte[] wrapItemKey(SecretKey itemKey, SecretKey masterKey);
  public SecretKey unwrapItemKey(byte[] wrappedKey, SecretKey masterKey);

  // === HMAC / 完整性校验（如需要额外校验） ===
  public byte[] computeHmac(byte[] data, SecretKey macKey);
  public boolean verifyHmac(byte[] data, byte[] expectedMac, SecretKey macKey);

  // === 密钥生成/销毁 ===
  public SecretKey generateAesKey(int keySize);
  public void clearSensitive(byte[] data);
  public void clearSensitive(char[] data);
}
```

### 4.1 函数细节说明
- `deriveMasterKey`：实现 PBKDF2WithHmacSHA256，或在可能时提供 Argon2 的实现（需要原生库或第三方）。返回 256-bit AES key 或密钥材料。
- `generateRandomBytes`：使用 `SecureRandom`。
- `encryptAesGcm`：生成随机 12 字节 IV，使用 `GCMParameterSpec(128, iv)`，返回包含 iv + ciphertext + tag 的结构（或分别存储）。
- `wrapItemKey` / `unwrapItemKey`：使用 masterKey 加密 itemKey，可直接用 AES-GCM 对 itemKey 加密并返回密文。
- `computeHmac`：可用于对导出文件或网络传输进行额外完整性校验（HMAC-SHA256）。
- `clearSensitive`：显式覆盖数组内容以减少内存残留。

---

# 5. Repository 与接口设计（设备内实现）

如果后端逻辑部署在设备内（即在应用模块内实现“后端”），则 Repository 接口示例：

```java
public interface VaultRepository {
  void saveItem(VaultItemPlain plaintextItem, char[] masterPassword) throws CryptoException;
  VaultItemPlain getItem(UUID uuid, char[] masterPassword) throws CryptoException;
  List<VaultItemMeta> listItemsMeta();
  void deleteItem(UUID uuid);
  List<VaultItemPlain> search(String query, char[] masterPassword);
}
```

实现要点：
- `saveItem`：内部生成 itemKey -> 使用 itemKey 加密字段 -> 使用 masterKey wrap itemKey -> 存入数据库
- `getItem`：读取 itemKey_enc -> unwrap -> decrypt fields -> 返回明文模型（调用方应尽快清零）

---

# 6. API 规范（若实现云同步/服务器）

### 6.1 登录与认证
- `POST /api/v1/auth/login`：请求体 `email + clientDerivedHash`，返回 `sessionToken`。服务器只验证派生的 hash（非明文主密码）。

### 6.2 Vault 上传/下载
- `GET /api/v1/vault`：返回加密 vault（JSON）
- `POST /api/v1/vault`：上传加密变更（条目级）

### 6.3 冲突解决
- 每个 item 带 `version` 与 `updatedAt`
- 服务端以 `updatedAt` 为主，若冲突则返回冲突项并让客户端决定覆盖或合并

### 6.4 导出/导入
- 导出文件包含：`salt`, `iterations`, `version`, `payload`（加密），并附带 `HMAC` 签名
- 导入时校验 HMAC，再解密 payload

---

# 7. TOTP 模块

模块：`TOTPService`

函数签名：
```java
public class TOTPService {
  public String generateTOTP(byte[] secret, long time, int digits, int period);
  public boolean verifyTOTP(byte[] secret, String code, int window);
  public byte[] decodeBase32Secret(String base32);
}
```

实现说明：
- 使用 HMAC-SHA1（或 HMAC-SHA256）按 RFC6238 实现
- `window` 参数用于允许前后 N 步的偏移以兼容时间偏差
- secret 在数据库中应以加密形式保存，并在使用后立即清零

---

# 8. 密码强度与审计模块

### PasswordStrengthService
- `int scorePassword(String password)`：返回 0-4 或 0-100 分
- 可集成 zxcvbn（Java 移植版）或自实现规则

### AuditService
- 记录事件：login_attempt, export, import, autofill_used, share_invite, share_accept
- 审计记录包含：user_id, event_type, timestamp, metadata
- 提供 UI 供前端查询（仅显示非敏感信息）

---

# 9. 同步设计（E2EE）

原则：服务器不解密数据，服务器仅存储密文

流程简介：
1. 客户端派生 Master Key (MK)
2. 客户端对每个 item 使用 Item Key 加密并用 MK wrap Item Key
3. 上传的 payload 包含：{item_id, item_key_enc, data_enc, updatedAt, version}
4. 服务器存储该 payload
5. 其他设备拉取后用本地 MK 解开 item_key_enc 并解密 data_enc

冲突策略：基于 version 与 timestamp

---

# 10. 导出文件格式（示例 JSON）

```json
{
  "schema_version": 1,
  "salt": "BASE64",
  "kdf": "PBKDF2",
  "iterations": 100000,
  "payload": "BASE64(CIPHERTEXT)",
  "hmac": "BASE64(HMAC)"
}
```

---

# 11. Sprint 分工（后端部分）

### Sprint 0 — 准备（1 周）
- 初始化后端模块仓库（或后端子模块）
- 设定编码规范、测试流程
- 确定依赖（JCA, SQLCipher, zxcvbn）

### Sprint 1 — Crypto Core（2 周）
- 实现 CryptoService 的函数：deriveMasterKey, generateRandomBytes, encryptAesGcm, decryptAesGcm
- 单元测试覆盖（KAT tests）
- 内存清零工具实现

### Sprint 2 — Vault Repository（2 周）
- 实现 VaultRepository CRUD
- 完成数据库 schema 实现（Room + SQLCipher 或 SQLite + SQLCipher）
- 集成 Item Key wrap/unwrap 流程

### Sprint 3 — TOTP 与 Password Strength（1 周）
- 实现 TOTPService 并集成到 VaultRepository
- 实现 PasswordStrengthService（集成 zxcvbn-java）

### Sprint 4 — Autofill 后台与 API（1 周）
- 实现用于 Autofill 的查找接口（按 domain/package）
- 若支持云：实现 minimal Vault API（upload/download）

### Sprint 5 — 导出/导入 与 审计（1 周）
- 导出/导入实现与 HMAC 校验
- 审计日志实现

### Sprint 6 — 测试与发布（1 周）
- 整体集成测试
- 安全扫描（静态分析）
- 性能测试（数据库、加密热点）

---

# 12. 单元测试与安全验证
- 加密：使用 known-answer-tests (KAT) 验证加密/解密一致性
- 性能：测量 PBKDF2 时间并设定合理迭代次数
- 安全：使用静态分析工具（SpotBugs, MobSF）检测潜在漏洞

---

# 13. 接口契约（供前端使用的简明 API）

```
Unlock:
- Request: masterPassword (char[])
- Response: success / failure

GetItem:
- Request: itemId
- Response: {title, username, password, url, notes} (plaintext)

SaveItem:
- Request: {title, username, password, url, notes}
- Response: itemId

Search:
- Request: query
- Response: list of {itemId, title, snippet}

GeneratePassword:
- Request: length, useSymbols, useNumbers
- Response: password
```

注意：上述接口在实现时需确保传输层加密（如果为进程内调用则无需网络）。

---

# 14. 风险与缓解（补充）
- 高迭代次数导致性能瓶颈：提供可配置的 iterations 与异步派生（不阻塞 UI）
- 内存残留：严格使用 `char[]` 与 `byte[]` 并及时清零
- 数据库备份泄露：导出文件必须加密与 HMAC

---

如果你想，我可以：
- 把后端文档转换为 Markdown / PDF / Word 并提供下载
- 将两个草稿合并为最终的前后端分工版并生成 Sprint 任务 CSV
- 生成 CryptoService 的 Java 模板代码（含单元测试示例）

请选择下一步（或直接告诉我要生成的文件类型）。

