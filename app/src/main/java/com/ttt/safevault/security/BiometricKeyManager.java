package com.ttt.safevault.security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 * 生物识别密钥管理器
 * 用于安全地管理生物识别解锁所需的加密密钥
 */
public class BiometricKeyManager {
    private static final String TAG = "BiometricKeyManager";
    private static final String KEY_ALIAS = "safevault_biometric_key";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";

    private static BiometricKeyManager instance;
    private KeyStore keyStore;

    private BiometricKeyManager() throws Exception {
        keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
    }

    public static synchronized BiometricKeyManager getInstance() throws Exception {
        if (instance == null) {
            instance = new BiometricKeyManager();
        }
        return instance;
    }

    /**
     * 初始化生物识别密钥
     */
    public void initializeKey() throws Exception {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);

            KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(true)  // 要求用户认证
                .setInvalidatedByBiometricEnrollment(true)  // 生物识别信息更新时使密钥失效
                .build();

            keyGenerator.init(keyGenParameterSpec);
            keyGenerator.generateKey();
        }
    }

    /**
     * 获取加密Cipher
     */
    public Cipher getEncryptCipher() throws Exception {
        SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        Cipher cipher = Cipher.getInstance(
            KeyProperties.KEY_ALGORITHM_AES + "/" +
            KeyProperties.BLOCK_MODE_CBC + "/" +
            KeyProperties.ENCRYPTION_PADDING_PKCS7);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher;
    }

    /**
     * 获取解密Cipher
     */
    public Cipher getDecryptCipher(byte[] iv) throws Exception {
        SecretKey secretKey = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        Cipher cipher = Cipher.getInstance(
            KeyProperties.KEY_ALGORITHM_AES + "/" +
            KeyProperties.BLOCK_MODE_CBC + "/" +
            KeyProperties.ENCRYPTION_PADDING_PKCS7);

        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
        return cipher;
    }

    /**
     * 检查密钥是否存在
     */
    public boolean hasKey() throws Exception {
        return keyStore.containsAlias(KEY_ALIAS);
    }

    /**
     * 删除密钥
     */
    public void deleteKey() throws Exception {
        keyStore.deleteEntry(KEY_ALIAS);
    }
}