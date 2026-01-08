package com.ttt.safevault.utils;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ttt.safevault.model.PasswordItem;
import com.ttt.safevault.model.SharePermission;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 离线分享工具类
 * 实现密码的离线加密传输（通过二维码）
 * 
 * 数据格式：safevault://offline/{base64EncodedData}
 * base64EncodedData 结构：
 * - 版本号(1字节)
 * - 盐值(16字节)
 * - IV(12字节)
 * - 加密数据长度(4字节)
 * - 加密数据(变长)
 * - 过期时间(8字节)
 * - 权限标志(1字节)
 */
public class OfflineShareUtils {
    
    private static final String TAG = "OfflineShareUtils";
    
    // 协议版本
    private static final byte VERSION = 1;
    
    // 加密参数
    private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 10000;
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    
    // 协议前缀
    private static final String OFFLINE_PREFIX = "safevault://offline/";

    /**
     * 离线分享数据包
     */
    public static class OfflineSharePacket {
        public String qrContent;        // 二维码内容
        public String sharePassword;    // 分享密码（需要告知接收方）
        public long expireTime;         // 过期时间
        public SharePermission permission; // 权限
        
        public OfflineSharePacket(String qrContent, String sharePassword, 
                                 long expireTime, SharePermission permission) {
            this.qrContent = qrContent;
            this.sharePassword = sharePassword;
            this.expireTime = expireTime;
            this.permission = permission;
        }
    }

    /**
     * 创建离线分享数据包
     * 
     * @param passwordItem 要分享的密码
     * @param sharePassword 分享密码（用于加密）
     * @param expireInMinutes 过期时间（分钟），0表示永不过期
     * @param permission 分享权限
     * @return 离线分享数据包，失败返回null
     */
    @Nullable
    public static OfflineSharePacket createOfflineShare(@NonNull PasswordItem passwordItem,
                                                        @NonNull String sharePassword,
                                                        int expireInMinutes,
                                                        @NonNull SharePermission permission) {
        try {
            // 1. 将密码数据转为JSON
            JSONObject jsonData = new JSONObject();
            jsonData.put("title", passwordItem.getTitle());
            jsonData.put("username", passwordItem.getUsername());
            jsonData.put("password", passwordItem.getPassword());
            jsonData.put("url", passwordItem.getUrl() != null ? passwordItem.getUrl() : "");
            jsonData.put("notes", passwordItem.getNotes() != null ? passwordItem.getNotes() : "");
            
            String jsonString = jsonData.toString();
            Log.d(TAG, "JSON data size: " + jsonString.length() + " bytes");
            
            // 2. 压缩数据
            byte[] compressedData = compressData(jsonString.getBytes(StandardCharsets.UTF_8));
            Log.d(TAG, "Compressed data size: " + compressedData.length + " bytes");
            
            // 3. 生成随机盐值和IV
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(salt);
            random.nextBytes(iv);
            
            // 4. 从分享密码派生密钥
            SecretKey key = deriveKey(sharePassword, salt);
            
            // 5. 加密数据
            byte[] encryptedData = encryptData(compressedData, key, iv);
            if (encryptedData == null) {
                Log.e(TAG, "Failed to encrypt data");
                return null;
            }
            Log.d(TAG, "Encrypted data size: " + encryptedData.length + " bytes");
            
            // 6. 计算过期时间
            long expireTime = 0;
            if (expireInMinutes > 0) {
                expireTime = System.currentTimeMillis() + (expireInMinutes * 60 * 1000L);
            }
            
            // 7. 构建数据包
            byte[] packet = buildPacket(salt, iv, encryptedData, expireTime, permission);
            
            // 8. Base64编码
            String base64Data = Base64.encodeToString(packet, Base64.NO_WRAP | Base64.URL_SAFE);
            
            // 9. 构建二维码内容
            String qrContent = OFFLINE_PREFIX + base64Data;
            
            Log.d(TAG, "QR content size: " + qrContent.length() + " characters");
            
            // 检查大小
            if (!QRCodeUtils.isContentSizeValid(qrContent)) {
                Log.w(TAG, "QR content too large: " + qrContent.length());
                // 继续处理，让调用者决定是否使用
            }
            
            return new OfflineSharePacket(qrContent, sharePassword, expireTime, permission);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to create offline share", e);
            return null;
        }
    }

    /**
     * 解析离线分享数据包
     * 
     * @param qrContent 二维码内容
     * @param sharePassword 分享密码
     * @return 解密后的密码数据，失败返回null
     */
    @Nullable
    public static PasswordItem parseOfflineShare(@NonNull String qrContent, 
                                                @NonNull String sharePassword) {
        try {
            // 1. 验证前缀
            if (!qrContent.startsWith(OFFLINE_PREFIX)) {
                Log.e(TAG, "Invalid offline share format");
                return null;
            }
            
            // 2. 提取Base64数据
            String base64Data = qrContent.substring(OFFLINE_PREFIX.length());
            
            // 3. Base64解码
            byte[] packet = Base64.decode(base64Data, Base64.NO_WRAP | Base64.URL_SAFE);
            
            // 4. 解析数据包
            ByteBuffer buffer = ByteBuffer.wrap(packet);
            
            // 读取版本号
            byte version = buffer.get();
            if (version != VERSION) {
                Log.e(TAG, "Unsupported version: " + version);
                return null;
            }
            
            // 读取盐值
            byte[] salt = new byte[SALT_LENGTH];
            buffer.get(salt);
            
            // 读取IV
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            
            // 读取加密数据长度
            int encryptedDataLength = buffer.getInt();
            if (encryptedDataLength <= 0 || encryptedDataLength > buffer.remaining() - 9) {
                Log.e(TAG, "Invalid encrypted data length: " + encryptedDataLength);
                return null;
            }
            
            // 读取加密数据
            byte[] encryptedData = new byte[encryptedDataLength];
            buffer.get(encryptedData);
            
            // 读取过期时间
            long expireTime = buffer.getLong();
            
            // 检查是否过期
            if (expireTime > 0 && System.currentTimeMillis() > expireTime) {
                Log.e(TAG, "Share has expired");
                return null;
            }
            
            // 读取权限（如果有剩余数据）
            byte permissionFlags = 0;
            if (buffer.hasRemaining()) {
                permissionFlags = buffer.get();
            }
            
            // 5. 从分享密码派生密钥
            SecretKey key = deriveKey(sharePassword, salt);
            
            // 6. 解密数据
            byte[] compressedData = decryptData(encryptedData, key, iv);
            if (compressedData == null) {
                Log.e(TAG, "Failed to decrypt data - wrong password?");
                return null;
            }
            
            // 7. 解压数据
            byte[] jsonBytes = decompressData(compressedData);
            String jsonString = new String(jsonBytes, StandardCharsets.UTF_8);
            
            // 8. 解析JSON
            JSONObject jsonData = new JSONObject(jsonString);
            
            // 9. 构建PasswordItem
            PasswordItem item = new PasswordItem();
            item.setTitle(jsonData.optString("title", ""));
            item.setUsername(jsonData.optString("username", ""));
            item.setPassword(jsonData.optString("password", ""));
            item.setUrl(jsonData.optString("url", ""));
            item.setNotes(jsonData.optString("notes", ""));
            
            Log.d(TAG, "Successfully parsed offline share");
            return item;
            
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse JSON", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse offline share", e);
            return null;
        }
    }

    /**
     * 从分享密码派生加密密钥
     */
    private static SecretKey deriveKey(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            ITERATION_COUNT,
            KEY_LENGTH
        );
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 加密数据
     */
    private static byte[] encryptData(byte[] data, SecretKey key, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed", e);
            return null;
        }
    }

    /**
     * 解密数据
     */
    private static byte[] decryptData(byte[] encryptedData, SecretKey key, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            Log.e(TAG, "Decryption failed", e);
            return null;
        }
    }

    /**
     * 压缩数据
     */
    private static byte[] compressData(byte[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(data);
        }
        return baos.toByteArray();
    }

    /**
     * 解压数据
     */
    private static byte[] decompressData(byte[] compressedData) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPInputStream gzipIn = new GZIPInputStream(
                new ByteArrayInputStream(compressedData))) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIn.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
        }
        return baos.toByteArray();
    }

    /**
     * 构建数据包
     */
    private static byte[] buildPacket(byte[] salt, byte[] iv, byte[] encryptedData,
                                     long expireTime, SharePermission permission) {
        // 计算总大小
        int totalSize = 1 +  // version
                       SALT_LENGTH +  // salt
                       IV_LENGTH +  // iv
                       4 +  // encrypted data length
                       encryptedData.length +  // encrypted data
                       8 +  // expire time
                       1;  // permission flags
        
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        
        // 写入版本号
        buffer.put(VERSION);
        
        // 写入盐值
        buffer.put(salt);
        
        // 写入IV
        buffer.put(iv);
        
        // 写入加密数据长度
        buffer.putInt(encryptedData.length);
        
        // 写入加密数据
        buffer.put(encryptedData);
        
        // 写入过期时间
        buffer.putLong(expireTime);
        
        // 写入权限标志
        byte permissionFlags = 0;
        if (permission.isCanView()) {
            permissionFlags |= 0x01;
        }
        if (permission.isCanSave()) {
            permissionFlags |= 0x02;
        }
        if (permission.isRevocable()) {
            permissionFlags |= 0x04;
        }
        buffer.put(permissionFlags);
        
        return buffer.array();
    }

    /**
     * 生成随机分享密码
     * 
     * @param length 密码长度
     * @return 随机密码
     */
    @NonNull
    public static String generateRandomPassword(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 检查是否为离线分享二维码
     */
    public static boolean isOfflineShare(@Nullable String qrContent) {
        return qrContent != null && qrContent.startsWith(OFFLINE_PREFIX);
    }
}
