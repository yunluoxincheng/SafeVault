package com.ttt.safevault.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ttt.safevault.crypto.CryptoManager;
import com.ttt.safevault.data.AppDatabase;
import com.ttt.safevault.data.EncryptedPasswordEntity;
import com.ttt.safevault.data.PasswordDao;
import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.model.Friend;
import com.ttt.safevault.model.PasswordItem;
import com.ttt.safevault.model.PasswordShare;
import com.ttt.safevault.model.SharePermission;
import com.ttt.safevault.model.ShareStatus;
import com.ttt.safevault.model.UserProfile;
import com.ttt.safevault.security.BiometricKeyManager;
import com.ttt.safevault.security.SecurityConfig;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BackendService接口的具体实现
 * 提供所有后端功能：加密存储、密码生成等
 */
public class BackendServiceImpl implements BackendService {

    private static final String TAG = "BackendServiceImpl";
    private static final String PREFS_NAME = "backend_prefs";
    private static final String PREF_BACKGROUND_TIME = "background_time";
    private static final String PREF_LAST_BACKUP = "last_backup";
    private static final String PREF_BIOMETRIC_ENCRYPTED_PASSWORD = "biometric_encrypted_password";
    private static final String PREF_BIOMETRIC_IV = "biometric_iv";
    private static final String PREF_USER_ID = "user_id";
    private static final String PREF_USER_DISPLAY_NAME = "user_display_name";
    private static final String PREF_USER_PUBLIC_KEY = "user_public_key";
    private static final String PREF_USER_CREATED_AT = "user_created_at";

    // 密码生成字符集
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMBERS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()_+-=[]{}|;':\",./<>?";

    private final Context context;
    private final CryptoManager cryptoManager;
    private final PasswordDao passwordDao;
    private final SecurityConfig securityConfig;
    private final SharedPreferences prefs;
    private final SecureRandom secureRandom;
    private BiometricKeyManager biometricKeyManager;
    
    // 分享功能相关的内存存储（简化实现，生产环境应使用数据库）
    private final Map<String, Friend> friendsMap = new ConcurrentHashMap<>();
    private final Map<String, PasswordShare> sharesMap = new ConcurrentHashMap<>();
    private UserProfile currentUserProfile;

    public BackendServiceImpl(@NonNull Context context) {
        this.context = context.getApplicationContext();
        // 使用 ServiceLocator 的共享 CryptoManager，确保解锁状态同步
        this.cryptoManager = com.ttt.safevault.ServiceLocator.getInstance().getCryptoManager();
        this.passwordDao = AppDatabase.getInstance(context).passwordDao();
        this.securityConfig = new SecurityConfig(context);
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.secureRandom = new SecureRandom();
        
        // 初始化生物识别密钥管理器
        try {
            this.biometricKeyManager = BiometricKeyManager.getInstance();
            // 初始化生物识别密钥（如果不存在的话）
            this.biometricKeyManager.initializeKey();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize biometric key manager", e);
            this.biometricKeyManager = null;
        }
    }

    @Override
    public boolean unlock(String masterPassword) {
        boolean success = cryptoManager.unlock(masterPassword);
        
        // 解锁成功后保存密码
        if (success) {
            saveMasterPasswordForBiometric(masterPassword);
            // 保存一份用于自动填充服务
            savePasswordForAutofill(masterPassword);
        }
        
        return success;
    }
    
    /**
     * 保存密码供自动填充服务使用
     */
    private void savePasswordForAutofill(String password) {
        try {
            context.getSharedPreferences("autofill_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("master_password", password)
                .apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save password for autofill", e);
        }
    }

    @Override
    public void lock() {
        cryptoManager.lock();
    }

    @Override
    public PasswordItem decryptItem(int id) {
        EncryptedPasswordEntity entity = passwordDao.getById(id);
        if (entity == null) {
            return null;
        }
        return decryptEntity(entity);
    }

    @Override
    public List<PasswordItem> search(String query) {
        List<PasswordItem> results = new ArrayList<>();
        List<PasswordItem> allItems = getAllItems();

        if (query == null || query.trim().isEmpty()) {
            return allItems;
        }

        String lowerQuery = query.toLowerCase().trim();
        for (PasswordItem item : allItems) {
            if (matchesQuery(item, lowerQuery)) {
                results.add(item);
            }
        }

        return results;
    }

    private boolean matchesQuery(PasswordItem item, String query) {
        return (item.getTitle() != null && item.getTitle().toLowerCase().contains(query)) ||
               (item.getUsername() != null && item.getUsername().toLowerCase().contains(query)) ||
               (item.getUrl() != null && item.getUrl().toLowerCase().contains(query)) ||
               (item.getNotes() != null && item.getNotes().toLowerCase().contains(query));
    }

    @Override
    public int saveItem(PasswordItem item) {
        try {
            EncryptedPasswordEntity entity = encryptItem(item);

            if (item.getId() > 0) {
                // 更新现有记录
                entity.setId(item.getId());
                passwordDao.update(entity);
                return item.getId();
            } else {
                // 插入新记录
                long newId = passwordDao.insert(entity);
                return (int) newId;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to save item", e);
            return -1;
        }
    }

    @Override
    public boolean deleteItem(int id) {
        try {
            return passwordDao.deleteById(id) > 0;
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete item", e);
            return false;
        }
    }

    @Override
    public String generatePassword(int length, boolean symbols) {
        return generatePassword(length, true, true, true, symbols);
    }

    @Override
    public String generatePassword(int length, boolean useUppercase, boolean useLowercase,
                                   boolean useNumbers, boolean useSymbols) {
        StringBuilder charPool = new StringBuilder();

        if (useUppercase) charPool.append(UPPERCASE);
        if (useLowercase) charPool.append(LOWERCASE);
        if (useNumbers) charPool.append(NUMBERS);
        if (useSymbols) charPool.append(SYMBOLS);

        // 默认至少包含小写字母和数字
        if (charPool.length() == 0) {
            charPool.append(LOWERCASE).append(NUMBERS);
        }

        StringBuilder password = new StringBuilder(length);
        String pool = charPool.toString();

        // 确保密码包含所选的每种字符类型
        List<String> requiredChars = new ArrayList<>();
        if (useUppercase) requiredChars.add(UPPERCASE);
        if (useLowercase) requiredChars.add(LOWERCASE);
        if (useNumbers) requiredChars.add(NUMBERS);
        if (useSymbols) requiredChars.add(SYMBOLS);

        // 先添加每种类型至少一个字符
        for (String chars : requiredChars) {
            if (password.length() < length) {
                password.append(chars.charAt(secureRandom.nextInt(chars.length())));
            }
        }

        // 填充剩余长度
        while (password.length() < length) {
            password.append(pool.charAt(secureRandom.nextInt(pool.length())));
        }

        // 打乱顺序
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }

        return new String(passwordArray);
    }

    @Override
    public List<PasswordItem> getAllItems() {
        List<PasswordItem> items = new ArrayList<>();
        try {
            Log.d(TAG, "getAllItems: isUnlocked=" + cryptoManager.isUnlocked());
            List<EncryptedPasswordEntity> entities = passwordDao.getAll();
            Log.d(TAG, "getAllItems: found " + entities.size() + " entities in database");
            
            for (EncryptedPasswordEntity entity : entities) {
                PasswordItem item = decryptEntity(entity);
                if (item != null) {
                    items.add(item);
                } else {
                    Log.w(TAG, "getAllItems: failed to decrypt entity id=" + entity.getId());
                }
            }
            Log.d(TAG, "getAllItems: successfully decrypted " + items.size() + " items");
        } catch (Exception e) {
            Log.e(TAG, "Failed to get all items", e);
        }
        return items;
    }

    @Override
    public boolean isUnlocked() {
        return cryptoManager.isUnlocked();
    }

    @Override
    public boolean isInitialized() {
        return cryptoManager.isInitialized();
    }

    @Override
    public boolean initialize(String masterPassword) {
        boolean success = cryptoManager.initialize(masterPassword);
        
        // 初始化成功后保存主密码
        if (success) {
            saveMasterPasswordForBiometric(masterPassword);
            // 保存一份用于自动填充服务
            savePasswordForAutofill(masterPassword);
        }
        
        return success;
    }

    @Override
    public boolean changeMasterPassword(String oldPassword, String newPassword) {
        if (!cryptoManager.changeMasterPassword(oldPassword, newPassword)) {
            return false;
        }

        // 需要重新加密所有数据
        try {
            List<PasswordItem> items = getAllItems();
            for (PasswordItem item : items) {
                saveItem(item);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to re-encrypt data", e);
            return false;
        }
    }

    @Override
    public boolean exportData(String exportPath) {
        // TODO: 实现加密导出功能
        return false;
    }

    @Override
    public boolean importData(String importPath) {
        // TODO: 实现导入功能
        return false;
    }

    @Override
    public AppStats getStats() {
        try {
            List<PasswordItem> items = getAllItems();
            int totalItems = items.size();
            int weakPasswords = 0;
            int duplicatePasswords = 0;

            Set<String> passwordSet = new HashSet<>();
            for (PasswordItem item : items) {
                String pwd = item.getPassword();
                if (pwd != null) {
                    // 检查弱密码
                    if (isWeakPassword(pwd)) {
                        weakPasswords++;
                    }
                    // 检查重复密码
                    if (!passwordSet.add(pwd)) {
                        duplicatePasswords++;
                    }
                }
            }

            long lastBackup = prefs.getLong(PREF_LAST_BACKUP, 0);
            int daysSinceBackup = lastBackup > 0 ?
                    (int) ((System.currentTimeMillis() - lastBackup) / (1000 * 60 * 60 * 24)) : -1;

            return new AppStats(totalItems, weakPasswords, duplicatePasswords, daysSinceBackup);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get stats", e);
            return new AppStats(0, 0, 0, -1);
        }
    }

    private boolean isWeakPassword(String password) {
        if (password == null || password.length() < 8) {
            return true;
        }

        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasNumber = password.matches(".*\\d.*");
        boolean hasSymbol = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

        int types = (hasUpper ? 1 : 0) + (hasLower ? 1 : 0) + (hasNumber ? 1 : 0) + (hasSymbol ? 1 : 0);
        return types < 3;
    }

    @Override
    public void recordBackgroundTime() {
        prefs.edit().putLong(PREF_BACKGROUND_TIME, System.currentTimeMillis()).apply();
    }

    @Override
    public long getBackgroundTime() {
        return prefs.getLong(PREF_BACKGROUND_TIME, 0);
    }

    @Override
    public int getAutoLockTimeout() {
        return securityConfig.getAutoLockTimeout();
    }

    /**
     * 加密PasswordItem为EncryptedPasswordEntity
     * 每个字段使用独立的IV，IV与密文拼接存储
     */
    private EncryptedPasswordEntity encryptItem(PasswordItem item) {
        EncryptedPasswordEntity entity = new EncryptedPasswordEntity();

        // 每个字段独立加密，IV拼接到密文前
        entity.setEncryptedTitle(encryptField(item.getTitle()));
        entity.setEncryptedUsername(encryptField(item.getUsername()));
        entity.setEncryptedPassword(encryptField(item.getPassword()));
        entity.setEncryptedUrl(encryptField(item.getUrl()));
        entity.setEncryptedNotes(encryptField(item.getNotes()));

        entity.setUpdatedAt(item.getUpdatedAt() > 0 ? item.getUpdatedAt() : System.currentTimeMillis());

        return entity;
    }

    /**
     * 加密单个字段，返回格式: iv:ciphertext
     */
    @Nullable
    private String encryptField(@Nullable String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }
        CryptoManager.EncryptedData data = cryptoManager.encrypt(plaintext);
        if (data == null) {
            return null;
        }
        return data.iv + ":" + data.ciphertext;
    }

    /**
     * 解密EncryptedPasswordEntity为PasswordItem
     */
    @Nullable
    private PasswordItem decryptEntity(EncryptedPasswordEntity entity) {
        try {
            PasswordItem item = new PasswordItem();
            item.setId(entity.getId());
            item.setTitle(decryptField(entity.getEncryptedTitle()));
            item.setUsername(decryptField(entity.getEncryptedUsername()));
            item.setPassword(decryptField(entity.getEncryptedPassword()));
            item.setUrl(decryptField(entity.getEncryptedUrl()));
            item.setNotes(decryptField(entity.getEncryptedNotes()));
            item.setUpdatedAt(entity.getUpdatedAt());

            return item;
        } catch (Exception e) {
            Log.e(TAG, "Failed to decrypt entity", e);
            return null;
        }
    }

    /**
     * 解密单个字段，输入格式: iv:ciphertext
     */
    @Nullable
    private String decryptField(@Nullable String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) {
            return null;
        }
        String[] parts = encrypted.split(":", 2);
        if (parts.length != 2) {
            return null;
        }
        return cryptoManager.decrypt(parts[1], parts[0]);
    }

    // ========== 新增：账户操作接口实现 ==========

    @Override
    public boolean setPinCode(String pinCode) {
        // TODO: 实现 PIN 码加密存储
        return false;
    }

    @Override
    public boolean verifyPinCode(String pinCode) {
        // TODO: 实现 PIN 码验证
        return false;
    }

    @Override
    public boolean clearPinCode() {
        // TODO: 实现 PIN 码清除
        return false;
    }

    @Override
    public boolean isPinCodeEnabled() {
        return securityConfig.isPinCodeEnabled();
    }

    @Override
    public void logout() {
        lock();
        // 清除内存中的敏感数据
    }

    @Override
    public boolean deleteAccount() {
        // TODO: 实现账户删除，包括本地和云端数据
        try {
            // 删除所有密码数据
            List<PasswordItem> items = getAllItems();
            for (PasswordItem item : items) {
                deleteItem(item.getId());
            }
            // 清除加密密钥
            cryptoManager.lock();
            // 清除所有设置
            securityConfig.clear();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete account", e);
            return false;
        }
    }

    @Override
    public boolean unlockWithBiometric() {
        // 检查生物识别是否启用
        if (!securityConfig.isBiometricEnabled()) {
            Log.e(TAG, "Biometric not enabled");
            return false;
        }
        
        // 检查是否已初始化
        if (!cryptoManager.isInitialized()) {
            Log.e(TAG, "Crypto manager not initialized");
            return false;
        }
        
        // 获取保存的加密主密码
        String masterPassword = getMasterPasswordForBiometric();
        if (masterPassword == null) {
            Log.e(TAG, "No master password stored for biometric unlock");
            return false;
        }
        
        // 使用主密码解锁
        return cryptoManager.unlock(masterPassword);
    }

    @Override
    public boolean canUseBiometricAuthentication() {
        return securityConfig.isBiometricEnabled() && hasMasterPasswordForBiometric();
    }

    /**
     * 保存主密码用于生物识别解锁
     */
    private void saveMasterPasswordForBiometric(String masterPassword) {
        if (biometricKeyManager == null) {
            Log.e(TAG, "BiometricKeyManager not initialized");
            return;
        }
        
        try {
            // 获取加密Cipher
            javax.crypto.Cipher cipher = biometricKeyManager.getEncryptCipher();
            
            // 加密主密码
            byte[] encrypted = cipher.doFinal(masterPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] iv = cipher.getIV();
            
            // 保存加密数据
            prefs.edit()
                .putString(PREF_BIOMETRIC_ENCRYPTED_PASSWORD, 
                    android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP))
                .putString(PREF_BIOMETRIC_IV, 
                    android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP))
                .apply();
            
            Log.d(TAG, "Master password saved for biometric unlock");
        } catch (Exception e) {
            Log.e(TAG, "Failed to save master password for biometric", e);
        }
    }

    /**
     * 获取用于生物识别解锁的主密码
     */
    private String getMasterPasswordForBiometric() {
        if (biometricKeyManager == null) {
            Log.e(TAG, "BiometricKeyManager not initialized");
            return null;
        }
        
        try {
            String encryptedPassword = prefs.getString(PREF_BIOMETRIC_ENCRYPTED_PASSWORD, null);
            String ivString = prefs.getString(PREF_BIOMETRIC_IV, null);
            
            if (encryptedPassword == null || ivString == null) {
                Log.e(TAG, "No encrypted password or IV found");
                return null;
            }
            
            byte[] encrypted = android.util.Base64.decode(encryptedPassword, android.util.Base64.NO_WRAP);
            byte[] iv = android.util.Base64.decode(ivString, android.util.Base64.NO_WRAP);
            
            // 获取解密Cipher
            javax.crypto.Cipher cipher = biometricKeyManager.getDecryptCipher(iv);
            
            // 解密主密码
            byte[] decrypted = cipher.doFinal(encrypted);
            
            return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Failed to decrypt master password for biometric", e);
            // 解密失败，可能是密钥已重建，清除旧数据
            clearBiometricData();
            return null;
        }
    }
    
    /**
     * 清除生物识别加密数据
     */
    private void clearBiometricData() {
        prefs.edit()
            .remove(PREF_BIOMETRIC_ENCRYPTED_PASSWORD)
            .remove(PREF_BIOMETRIC_IV)
            .apply();
        Log.d(TAG, "Biometric data cleared");
    }

    /**
     * 检查是否有保存的生物识别密码
     */
    private boolean hasMasterPasswordForBiometric() {
        return prefs.contains(PREF_BIOMETRIC_ENCRYPTED_PASSWORD) && 
               prefs.contains(PREF_BIOMETRIC_IV);
    }

    // ========== 新增：用户管理接口实现 ==========

    @Override
    public UserProfile getUserProfile() {
        if (currentUserProfile != null) {
            return currentUserProfile;
        }
        
        // 从 SharedPreferences 加载用户配置
        String userId = prefs.getString(PREF_USER_ID, null);
        if (userId == null) {
            // 创建新用户
            userId = "user_" + UUID.randomUUID().toString();
            String displayName = "默认用户";
            String publicKey = "public_key_" + UUID.randomUUID().toString(); // 简化实现
            long createdAt = System.currentTimeMillis();
            
            // 保存到 SharedPreferences
            prefs.edit()
                .putString(PREF_USER_ID, userId)
                .putString(PREF_USER_DISPLAY_NAME, displayName)
                .putString(PREF_USER_PUBLIC_KEY, publicKey)
                .putLong(PREF_USER_CREATED_AT, createdAt)
                .apply();
            
            currentUserProfile = new UserProfile(userId, displayName, publicKey);
            currentUserProfile.setCreatedAt(createdAt);
        } else {
            String displayName = prefs.getString(PREF_USER_DISPLAY_NAME, "默认用户");
            String publicKey = prefs.getString(PREF_USER_PUBLIC_KEY, "");
            long createdAt = prefs.getLong(PREF_USER_CREATED_AT, System.currentTimeMillis());
            
            currentUserProfile = new UserProfile(userId, displayName, publicKey);
            currentUserProfile.setCreatedAt(createdAt);
        }
        
        return currentUserProfile;
    }

    @Override
    public UserProfile getUserById(String userId) {
        // 简化实现：从好友列表中查找
        Friend friend = friendsMap.get(userId);
        if (friend != null) {
            UserProfile profile = new UserProfile();
            profile.setUserId(friend.getFriendId());
            profile.setDisplayName(friend.getDisplayName());
            profile.setPublicKey(friend.getPublicKey());
            profile.setCreatedAt(friend.getAddedAt());
            return profile;
        }
        
        // 检查是否是当前用户
        UserProfile currentUser = getUserProfile();
        if (currentUser.getUserId().equals(userId)) {
            return currentUser;
        }
        
        return null;
    }

    @Override
    public boolean addFriend(String userId) {
        try {
            // 检查是否已经是好友
            if (friendsMap.containsKey(userId)) {
                return false;
            }
            
            // 创建好友对象（简化实现）
            Friend friend = new Friend();
            friend.setFriendId(userId);
            friend.setDisplayName("好友_" + userId.substring(0, Math.min(8, userId.length())));
            friend.setPublicKey("public_key_" + userId);
            friend.setAddedAt(System.currentTimeMillis());
            friend.setBlocked(false);
            
            friendsMap.put(userId, friend);
            Log.d(TAG, "Friend added: " + userId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to add friend", e);
            return false;
        }
    }

    @Override
    public List<Friend> getFriendList() {
        return new ArrayList<>(friendsMap.values());
    }

    @Override
    public boolean removeFriend(String friendId) {
        try {
            Friend removed = friendsMap.remove(friendId);
            if (removed != null) {
                Log.d(TAG, "Friend removed: " + friendId);
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Failed to remove friend", e);
            return false;
        }
    }

    @Override
    public String generateUserQRCode() {
        try {
            UserProfile profile = getUserProfile();
            // 返回 JSON 格式的二维码内容
            return "{\"type\":\"user\",\"userId\":\"" + profile.getUserId() + 
                   "\",\"displayName\":\"" + profile.getDisplayName() + 
                   "\",\"publicKey\":\"" + profile.getPublicKey() + "\"}";
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate QR code", e);
            return null;
        }
    }

    // ========== 新增：分享管理接口实现 ==========

    @Override
    public String createPasswordShare(int passwordId, String toUserId,
                                     int expireInMinutes, SharePermission permission) {
        try {
            // 验证密码是否存在
            PasswordItem item = decryptItem(passwordId);
            if (item == null) {
                Log.e(TAG, "Password not found: " + passwordId);
                return null;
            }
            
            // 生成分享ID
            String shareId = "share_" + UUID.randomUUID().toString();
            
            // 创建分享对象
            PasswordShare share = new PasswordShare();
            share.setShareId(shareId);
            share.setPasswordId(passwordId);
            share.setFromUserId(getUserProfile().getUserId());
            share.setToUserId(toUserId);
            share.setCreatedAt(System.currentTimeMillis());
            
            // 计算过期时间
            if (expireInMinutes > 0) {
                long expireTime = System.currentTimeMillis() + (expireInMinutes * 60 * 1000L);
                share.setExpireTime(expireTime);
            } else {
                share.setExpireTime(0);
            }
            
            share.setPermission(permission);
            share.setStatus(ShareStatus.ACTIVE);
            
            // 加密密码数据（简化实现，直接存储JSON）
            String encryptedData = encryptPasswordForShare(item);
            share.setEncryptedData(encryptedData);
            
            // 保存分享
            sharesMap.put(shareId, share);
            Log.d(TAG, "Share created: " + shareId);
            
            return shareId;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create share", e);
            return null;
        }
    }

    @Override
    public String createDirectPasswordShare(int passwordId, int expireInMinutes,
                                           SharePermission permission) {
        // 直接分享与toUserId为null的普通分享相同
        return createPasswordShare(passwordId, null, expireInMinutes, permission);
    }

    @Override
    public PasswordItem receivePasswordShare(String shareId) {
        try {
            PasswordShare share = getShareDetails(shareId);
            if (share == null) {
                Log.e(TAG, "Share not found: " + shareId);
                return null;
            }
            
            // 验证分享状态
            if (!share.isAvailable()) {
                Log.e(TAG, "Share not available: " + shareId);
                return null;
            }
            
            // 解密密码数据
            PasswordItem item = decryptPasswordFromShare(share.getEncryptedData());
            
            // 更新分享状态
            share.setStatus(ShareStatus.ACCEPTED);
            
            Log.d(TAG, "Share received: " + shareId);
            return item;
        } catch (Exception e) {
            Log.e(TAG, "Failed to receive share", e);
            return null;
        }
    }

    @Override
    public boolean revokePasswordShare(String shareId) {
        try {
            PasswordShare share = sharesMap.get(shareId);
            if (share == null) {
                return false;
            }
            
            // 验证所有权
            if (!share.getFromUserId().equals(getUserProfile().getUserId())) {
                Log.e(TAG, "Not authorized to revoke share: " + shareId);
                return false;
            }
            
            // 验证是否可撤销
            if (!share.getPermission().isRevocable()) {
                Log.e(TAG, "Share is not revocable: " + shareId);
                return false;
            }
            
            // 更新状态
            share.setStatus(ShareStatus.REVOKED);
            Log.d(TAG, "Share revoked: " + shareId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to revoke share", e);
            return false;
        }
    }

    @Override
    public List<PasswordShare> getMyShares() {
        List<PasswordShare> myShares = new ArrayList<>();
        String currentUserId = getUserProfile().getUserId();
        
        for (PasswordShare share : sharesMap.values()) {
            if (currentUserId.equals(share.getFromUserId())) {
                myShares.add(share);
            }
        }
        
        return myShares;
    }

    @Override
    public List<PasswordShare> getReceivedShares() {
        List<PasswordShare> receivedShares = new ArrayList<>();
        String currentUserId = getUserProfile().getUserId();
        
        for (PasswordShare share : sharesMap.values()) {
            if (currentUserId.equals(share.getToUserId()) || share.getToUserId() == null) {
                receivedShares.add(share);
            }
        }
        
        return receivedShares;
    }

    @Override
    public int saveSharedPassword(String shareId) {
        try {
            PasswordItem item = receivePasswordShare(shareId);
            if (item == null) {
                return -1;
            }
            
            // 保存到密码库
            return saveItem(item);
        } catch (Exception e) {
            Log.e(TAG, "Failed to save shared password", e);
            return -1;
        }
    }

    @Override
    public PasswordShare getShareDetails(String shareId) {
        return sharesMap.get(shareId);
    }

    // ========== 新增：加密传输接口实现 ==========

    @Override
    public String generateShareData(PasswordItem passwordItem,
                                   String receiverPublicKey,
                                   SharePermission permission) {
        try {
            // 简化实现：直接序列化为JSON（生产环境应使用真正的加密）
            return encryptPasswordForShare(passwordItem);
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate share data", e);
            return null;
        }
    }

    @Override
    public PasswordItem parseShareData(String shareData) {
        try {
            // 简化实现：从JSON解析（生产环境应使用真正的解密）
            return decryptPasswordFromShare(shareData);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse share data", e);
            return null;
        }
    }

    /**
     * 加密密码用于分享（简化实现）
     */
    private String encryptPasswordForShare(PasswordItem item) {
        // 简化实现：返回JSON字符串
        // 生产环境应使用真正的端到端加密
        return "{\"title\":\"" + (item.getTitle() != null ? item.getTitle() : "") + 
               "\",\"username\":\"" + (item.getUsername() != null ? item.getUsername() : "") + 
               "\",\"password\":\"" + (item.getPassword() != null ? item.getPassword() : "") + 
               "\",\"url\":\"" + (item.getUrl() != null ? item.getUrl() : "") + 
               "\",\"notes\":\"" + (item.getNotes() != null ? item.getNotes() : "") + "\"}";
    }

    /**
     * 从分享数据解密密码（简化实现）
     */
    private PasswordItem decryptPasswordFromShare(String encryptedData) {
        // 简化实现：从JSON解析
        // 生产环境应使用真正的解密
        try {
            PasswordItem item = new PasswordItem();
            // 简单的JSON解析（生产环境应使用JSON库）
            if (encryptedData.contains("\"title\":\"")) {
                String title = extractJsonValue(encryptedData, "title");
                String username = extractJsonValue(encryptedData, "username");
                String password = extractJsonValue(encryptedData, "password");
                String url = extractJsonValue(encryptedData, "url");
                String notes = extractJsonValue(encryptedData, "notes");
                
                item.setTitle(title);
                item.setUsername(username);
                item.setPassword(password);
                item.setUrl(url);
                item.setNotes(notes);
            }
            return item;
        } catch (Exception e) {
            Log.e(TAG, "Failed to decrypt password from share", e);
            return null;
        }
    }

    /**
     * 从JSON字符串提取值（简化实现）
     */
    private String extractJsonValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":\"";
            int startIndex = json.indexOf(searchKey);
            if (startIndex == -1) {
                return "";
            }
            startIndex += searchKey.length();
            int endIndex = json.indexOf("\"", startIndex);
            if (endIndex == -1) {
                return "";
            }
            return json.substring(startIndex, endIndex);
        } catch (Exception e) {
            return "";
        }
    }

    // ========== 新增：离线分享接口实现 ==========

    @Override
    public String createOfflineShare(int passwordId, String sharePassword,
                                    int expireInMinutes, SharePermission permission) {
        try {
            // 获取密码数据
            PasswordItem item = decryptItem(passwordId);
            if (item == null) {
                Log.e(TAG, "Password not found: " + passwordId);
                return null;
            }

            // 使用OfflineShareUtils创建离线分享
            com.ttt.safevault.utils.OfflineShareUtils.OfflineSharePacket packet =
                com.ttt.safevault.utils.OfflineShareUtils.createOfflineShare(
                    item, sharePassword, expireInMinutes, permission
                );

            if (packet == null) {
                Log.e(TAG, "Failed to create offline share");
                return null;
            }

            Log.d(TAG, "Offline share created successfully");
            return packet.qrContent;

        } catch (Exception e) {
            Log.e(TAG, "Failed to create offline share", e);
            return null;
        }
    }

    @Override
    public PasswordItem receiveOfflineShare(String qrContent, String sharePassword) {
        try {
            // 使用OfflineShareUtils解析离线分享
            PasswordItem item = com.ttt.safevault.utils.OfflineShareUtils.parseOfflineShare(
                qrContent, sharePassword
            );

            if (item == null) {
                Log.e(TAG, "Failed to parse offline share");
                return null;
            }

            Log.d(TAG, "Offline share received successfully");
            return item;

        } catch (Exception e) {
            Log.e(TAG, "Failed to receive offline share", e);
            return null;
        }
    }

    @Override
    public String generateSharePassword(int length) {
        return com.ttt.safevault.utils.OfflineShareUtils.generateRandomPassword(length);
    }
}