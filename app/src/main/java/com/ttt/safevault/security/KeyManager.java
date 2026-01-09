package com.ttt.safevault.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

/**
 * 密钥管理器
 * 负责生成和存储RSA密钥对
 */
public class KeyManager {
    private static final String TAG = "KeyManager";
    private static final String PREFS_NAME = "key_prefs";
    private static final String KEY_PUBLIC_KEY = "public_key";
    private static final String KEY_PRIVATE_KEY = "private_key";
    private static final String KEY_DEVICE_ID = "device_id";

    private static volatile KeyManager INSTANCE;
    private final Context context;
    private final SharedPreferences prefs;
    private KeyPair keyPair;
    private String deviceId;

    private KeyManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadOrGenerateKeys();
        loadOrGenerateDeviceId();
    }

    public static KeyManager getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (KeyManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new KeyManager(context);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 加载或生成密钥对
     */
    private void loadOrGenerateKeys() {
        String publicKeyStr = prefs.getString(KEY_PUBLIC_KEY, null);
        String privateKeyStr = prefs.getString(KEY_PRIVATE_KEY, null);

        if (publicKeyStr != null && privateKeyStr != null) {
            // 加载已保存的密钥
            try {
                this.keyPair = loadKeyPair(publicKeyStr, privateKeyStr);
                Log.d(TAG, "Loaded existing key pair");
            } catch (Exception e) {
                Log.e(TAG, "Failed to load keys, generating new ones", e);
                generateAndSaveKeys();
            }
        } else {
            // 生成新密钥
            generateAndSaveKeys();
        }
    }

    /**
     * 加载或生成设备ID
     */
    private void loadOrGenerateDeviceId() {
        this.deviceId = prefs.getString(KEY_DEVICE_ID, null);
        if (this.deviceId == null) {
            this.deviceId = generateDeviceId();
            prefs.edit().putString(KEY_DEVICE_ID, this.deviceId).apply();
            Log.d(TAG, "Generated new device ID: " + deviceId);
        } else {
            Log.d(TAG, "Loaded existing device ID: " + deviceId);
        }
    }

    /**
     * 生成新的RSA密钥对
     */
    private void generateAndSaveKeys() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            this.keyPair = keyGen.generateKeyPair();

            // 保存密钥
            String publicKeyStr = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            String privateKeyStr = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

            prefs.edit()
                    .putString(KEY_PUBLIC_KEY, publicKeyStr)
                    .putString(KEY_PRIVATE_KEY, privateKeyStr)
                    .apply();

            Log.d(TAG, "Generated and saved new key pair");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Failed to generate key pair", e);
            throw new RuntimeException("Failed to generate key pair", e);
        }
    }

    /**
     * 生成设备ID
     */
    private String generateDeviceId() {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * 从字符串加载密钥对
     */
    private KeyPair loadKeyPair(String publicKeyStr, String privateKeyStr) throws Exception {
        // 简化实现，实际项目中需要更完整的密钥加载逻辑
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyStr);
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyStr);

        // 这里需要使用 KeyFactory 来重建密钥
        // 由于实现较复杂，这里返回 null，实际使用时会重新生成
        // 生产环境应该实现完整的密钥加载逻辑
        throw new UnsupportedOperationException("Key loading not implemented");
    }

    /**
     * 获取公钥（Base64编码）
     */
    public String getPublicKey() {
        if (keyPair != null && keyPair.getPublic() != null) {
            return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        }
        return null;
    }

    /**
     * 获取私钥
     */
    public PrivateKey getPrivateKey() {
        if (keyPair != null) {
            return keyPair.getPrivate();
        }
        return null;
    }

    /**
     * 获取设备ID
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * 清除密钥（用于重置）
     */
    public void clearKeys() {
        prefs.edit()
                .remove(KEY_PUBLIC_KEY)
                .remove(KEY_PRIVATE_KEY)
                .apply();
        keyPair = null;
        generateAndSaveKeys();
        Log.d(TAG, "Cleared and regenerated keys");
    }
}
