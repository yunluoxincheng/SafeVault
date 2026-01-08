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
import com.ttt.safevault.model.PasswordItem;
import com.ttt.safevault.security.BiometricKeyManager;
import com.ttt.safevault.security.SecurityConfig;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public BackendServiceImpl(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.cryptoManager = new CryptoManager(context);
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
        
        // 解锁成功后总是保存加密的主密码，以便生物识别启用时可以立即使用
        if (success) {
            saveMasterPasswordForBiometric(masterPassword);
        }
        
        return success;
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
    public List<PasswordItem> getCredentialsForDomain(String domain) {
        List<PasswordItem> results = new ArrayList<>();
        List<PasswordItem> allItems = getAllItems();

        if (domain == null || domain.trim().isEmpty()) {
            return results;
        }

        String normalizedDomain = normalizeDomain(domain);
        for (PasswordItem item : allItems) {
            if (item.getUrl() != null) {
                String itemDomain = normalizeDomain(item.getUrl());
                if (itemDomain.contains(normalizedDomain) || normalizedDomain.contains(itemDomain)) {
                    results.add(item);
                }
            }
        }

        return results;
    }

    private String normalizeDomain(String url) {
        return url.toLowerCase()
                .replace("https://", "")
                .replace("http://", "")
                .replace("www.", "")
                .split("/")[0];
    }

    @Override
    public List<PasswordItem> getAllItems() {
        List<PasswordItem> items = new ArrayList<>();
        try {
            List<EncryptedPasswordEntity> entities = passwordDao.getAll();
            for (EncryptedPasswordEntity entity : entities) {
                PasswordItem item = decryptEntity(entity);
                if (item != null) {
                    items.add(item);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get all items", e);
        }
        return items;
    }

    @Override
    public boolean isInitialized() {
        return cryptoManager.isInitialized();
    }

    @Override
    public boolean initialize(String masterPassword) {
        boolean success = cryptoManager.initialize(masterPassword);
        
        // 初始化成功后保存主密码，以便以后启用生物识别时可以立即使用
        if (success) {
            saveMasterPasswordForBiometric(masterPassword);
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
}