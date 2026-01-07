package com.ttt.safevault.crypto;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ttt.safevault.security.SecurityUtils;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 加密管理器
 * 使用AES-256-GCM进行加密解密
 * 主密钥派生自用户主密码 + 盐值
 */
public class CryptoManager {

    private static final String TAG = "CryptoManager";
    private static final String PREFS_NAME = "crypto_prefs";
    private static final String PREF_SALT = "master_salt";
    private static final String PREF_VERIFY_HASH = "verify_hash";
    private static final String PREF_INITIALIZED = "initialized";

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int IV_SIZE = 12; // GCM推荐IV大小
    private static final int TAG_SIZE = 128; // GCM认证标签大小
    private static final int PBKDF2_ITERATIONS = 100000;

    private final Context context;
    private final SharedPreferences prefs;
    private SecretKey masterKey;
    private boolean isUnlocked = false;

    public CryptoManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 检查是否已初始化（设置过主密码）
     */
    public boolean isInitialized() {
        return prefs.getBoolean(PREF_INITIALIZED, false);
    }

    /**
     * 初始化，设置主密码
     */
    public boolean initialize(@NonNull String masterPassword) {
        try {
            // 生成随机盐值
            byte[] salt = new byte[32];
            new SecureRandom().nextBytes(salt);

            // 派生主密钥
            SecretKey key = deriveKey(masterPassword, salt);

            // 生成验证哈希
            String verifyHash = generateVerifyHash(masterPassword, salt);

            // 保存盐值和验证哈希
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREF_SALT, Base64.encodeToString(salt, Base64.NO_WRAP));
            editor.putString(PREF_VERIFY_HASH, verifyHash);
            editor.putBoolean(PREF_INITIALIZED, true);
            editor.apply();

            // 设置为已解锁
            this.masterKey = key;
            this.isUnlocked = true;

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize", e);
            return false;
        }
    }

    /**
     * 使用主密码解锁
     */
    public boolean unlock(@NonNull String masterPassword) {
        try {
            String saltBase64 = prefs.getString(PREF_SALT, null);
            String storedHash = prefs.getString(PREF_VERIFY_HASH, null);

            if (saltBase64 == null || storedHash == null) {
                return false;
            }

            byte[] salt = Base64.decode(saltBase64, Base64.NO_WRAP);

            // 验证密码
            String verifyHash = generateVerifyHash(masterPassword, salt);
            if (!storedHash.equals(verifyHash)) {
                return false;
            }

            // 派生主密钥
            this.masterKey = deriveKey(masterPassword, salt);
            this.isUnlocked = true;

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to unlock", e);
            return false;
        }
    }

    /**
     * 锁定，清除内存中的密钥
     */
    public void lock() {
        this.masterKey = null;
        this.isUnlocked = false;
    }

    /**
     * 检查是否已解锁
     */
    public boolean isUnlocked() {
        return isUnlocked && masterKey != null;
    }

    /**
     * 加密字符串
     */
    @Nullable
    public EncryptedData encrypt(@Nullable String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }

        if (!isUnlocked()) {
            throw new IllegalStateException("CryptoManager is locked");
        }

        try {
            // 生成随机IV
            byte[] iv = new byte[IV_SIZE];
            new SecureRandom().nextBytes(iv);

            // 初始化加密器
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, spec);

            // 加密
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return new EncryptedData(
                    Base64.encodeToString(encrypted, Base64.NO_WRAP),
                    Base64.encodeToString(iv, Base64.NO_WRAP)
            );
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed", e);
            return null;
        }
    }

    /**
     * 解密字符串
     */
    @Nullable
    public String decrypt(@Nullable String encryptedBase64, @Nullable String ivBase64) {
        if (encryptedBase64 == null || ivBase64 == null) {
            return null;
        }

        if (!isUnlocked()) {
            throw new IllegalStateException("CryptoManager is locked");
        }

        try {
            byte[] encrypted = Base64.decode(encryptedBase64, Base64.NO_WRAP);
            byte[] iv = Base64.decode(ivBase64, Base64.NO_WRAP);

            // 初始化解密器
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, spec);

            // 解密
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Decryption failed", e);
            return null;
        }
    }

    /**
     * 更改主密码
     */
    public boolean changeMasterPassword(@NonNull String oldPassword, @NonNull String newPassword) {
        // 先验证旧密码
        String saltBase64 = prefs.getString(PREF_SALT, null);
        String storedHash = prefs.getString(PREF_VERIFY_HASH, null);

        if (saltBase64 == null || storedHash == null) {
            return false;
        }

        try {
            byte[] oldSalt = Base64.decode(saltBase64, Base64.NO_WRAP);
            String verifyHash = generateVerifyHash(oldPassword, oldSalt);

            if (!storedHash.equals(verifyHash)) {
                return false; // 旧密码错误
            }

            // 生成新盐值
            byte[] newSalt = new byte[32];
            new SecureRandom().nextBytes(newSalt);

            // 派生新主密钥
            SecretKey newKey = deriveKey(newPassword, newSalt);
            String newVerifyHash = generateVerifyHash(newPassword, newSalt);

            // 保存新的盐值和验证哈希
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREF_SALT, Base64.encodeToString(newSalt, Base64.NO_WRAP));
            editor.putString(PREF_VERIFY_HASH, newVerifyHash);
            editor.apply();

            // 更新内存中的密钥
            this.masterKey = newKey;

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to change master password", e);
            return false;
        }
    }

    /**
     * 使用PBKDF2从密码派生密钥
     */
    private SecretKey deriveKey(@NonNull String password, @NonNull byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                PBKDF2_ITERATIONS,
                KEY_SIZE
        );

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        spec.clearPassword();

        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 生成验证哈希
     */
    private String generateVerifyHash(@NonNull String password, @NonNull byte[] salt) {
        // 使用密码+盐值生成哈希，用于验证密码正确性
        String combined = password + Base64.encodeToString(salt, Base64.NO_WRAP);
        return SecurityUtils.sha256(combined);
    }

    /**
     * 加密数据封装类
     */
    public static class EncryptedData {
        public final String ciphertext;
        public final String iv;

        public EncryptedData(String ciphertext, String iv) {
            this.ciphertext = ciphertext;
            this.iv = iv;
        }
    }
}
