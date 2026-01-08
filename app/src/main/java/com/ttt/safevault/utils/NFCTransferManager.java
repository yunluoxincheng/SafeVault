package com.ttt.safevault.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * NFC传输管理器
 * 负责通过NFC发送和接收密码分享数据
 * 注意：Android 10+ 已移除Android Beam功能，主要用于读取NFC标签
 */
public class NFCTransferManager {
    
    private static final String TAG = "NFCTransfer";
    
    // MIME类型 - 用于SafeVault密码分享
    private static final String MIME_TYPE = "application/vnd.safevault.share";
    
    // 最大NFC数据大小（约32KB，根据实际设备可能有所不同）
    private static final int MAX_NFC_SIZE = 32 * 1024;
    
    private final Context context;
    private NfcAdapter nfcAdapter;
    private String pendingShareData;
    private TransferCallback callback;

    /**
     * 传输回调接口
     */
    public interface TransferCallback {
        void onTransferStarted();
        void onTransferSuccess();
        void onTransferFailed(String error);
        void onDataReceived(String data);
    }

    public NFCTransferManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(context);
    }

    /**
     * 检查设备是否支持NFC
     */
    public boolean isNfcAvailable() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC);
    }

    /**
     * 检查NFC是否已启用
     */
    public boolean isNfcEnabled() {
        return nfcAdapter != null && nfcAdapter.isEnabled();
    }

    /**
     * 检查设备是否支持Android Beam（已废弃）
     * Android 10+ 已移除此功能
     */
    @Deprecated
    public boolean isBeamAvailable() {
        // Android 10+ 已移除Android Beam
        return false;
    }

    /**
     * 设置传输回调
     */
    public void setTransferCallback(@Nullable TransferCallback callback) {
        this.callback = callback;
    }

    /**
     * 准备发送数据
     * 注意：Android 10+ 已移除Android Beam，此方法仅用于兼容旧版本
     * 建议使用写入NFC标签的方式
     * 
     * @param activity Activity实例（未使用，保留用于API兼容）
     * @param data     要发送的数据
     * @return 总是返回false，因为Android Beam已废弃
     */
    @Deprecated
    public boolean prepareSendData(@NonNull Activity activity, @NonNull String data) {
        Log.w(TAG, "Android Beam has been removed in Android 10+. Use writeToTag() instead.");
        notifyError("当前Android版本不支持NFC Beam传输，请使用写入NFC标签的方式");
        return false;
    }

    /**
     * 停止发送准备（已废弃）
     */
    @Deprecated
    public void stopSending(@NonNull Activity activity) {
        pendingShareData = null;
    }

    /**
     * 创建分享数据的NDEF消息
     * 用于写入NFC标签
     * 
     * @param data 要分享的数据
     * @return NDEF消息
     */
    @Nullable
    public NdefMessage createShareMessage(@NonNull String data) {
        try {
            // 创建NDEF记录
            NdefRecord mimeRecord = createMimeRecord(MIME_TYPE, data);
            NdefRecord appRecord = NdefRecord.createApplicationRecord(context.getPackageName());
            
            // 创建NDEF消息
            NdefMessage message = new NdefMessage(new NdefRecord[]{mimeRecord, appRecord});
            
            Log.d(TAG, "NDEF message created: " + data.length() + " bytes");
            return message;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to create NDEF message", e);
            notifyError("创建NFC消息失败");
            return null;
        }
    }

    /**
     * 从NFC标签读取数据
     * 
     * @param tag NFC标签
     * @return 读取的数据，失败返回null
     */
    @Nullable
    public String readFromTag(@NonNull Tag tag) {
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                Log.w(TAG, "NDEF not supported");
                return null;
            }

            NdefMessage message = ndef.getCachedNdefMessage();
            if (message == null) {
                Log.w(TAG, "No NDEF message found");
                return null;
            }

            NdefRecord[] records = message.getRecords();
            for (NdefRecord record : records) {
                String mimeType = new String(record.getType(), StandardCharsets.US_ASCII);
                
                if (MIME_TYPE.equals(mimeType)) {
                    byte[] payload = record.getPayload();
                    String data = new String(payload, StandardCharsets.UTF_8);
                    
                    Log.d(TAG, "Data read from NFC: " + data.length() + " bytes");
                    notifyDataReceived(data);
                    
                    return data;
                }
            }

            Log.w(TAG, "No matching MIME type found");
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to read from NFC tag", e);
            notifyError("读取NFC数据失败：" + e.getMessage());
            return null;
        }
    }

    /**
     * 将数据写入NFC标签（需要可写标签）
     * 
     * @param tag  NFC标签
     * @param data 要写入的数据
     * @return true表示写入成功
     */
    public boolean writeToTag(@NonNull Tag tag, @NonNull String data) {
        if (data.length() > MAX_NFC_SIZE) {
            notifyError("数据过大，无法写入NFC标签");
            return false;
        }

        Ndef ndef = null;
        try {
            ndef = Ndef.get(tag);
            if (ndef == null) {
                Log.w(TAG, "NDEF not supported");
                notifyError("该NFC标签不支持NDEF格式");
                return false;
            }

            ndef.connect();

            // 检查标签是否可写
            if (!ndef.isWritable()) {
                Log.w(TAG, "Tag is not writable");
                notifyError("该NFC标签不可写入");
                return false;
            }

            // 检查标签容量
            int size = data.getBytes(StandardCharsets.UTF_8).length;
            if (ndef.getMaxSize() < size) {
                Log.w(TAG, "Tag size insufficient");
                notifyError("NFC标签容量不足");
                return false;
            }

            notifyStarted();

            // 创建NDEF消息
            NdefRecord mimeRecord = createMimeRecord(MIME_TYPE, data);
            NdefRecord appRecord = NdefRecord.createApplicationRecord(context.getPackageName());
            NdefMessage message = new NdefMessage(new NdefRecord[]{mimeRecord, appRecord});

            // 写入标签
            ndef.writeNdefMessage(message);
            
            Log.d(TAG, "Data written to NFC tag: " + size + " bytes");
            notifySuccess();
            
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to NFC tag", e);
            notifyError("写入NFC标签失败：" + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error writing to NFC tag", e);
            notifyError("写入失败：" + e.getMessage());
            return false;
        } finally {
            if (ndef != null) {
                try {
                    ndef.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close NDEF", e);
                }
            }
        }
    }

    /**
     * 从NDEF消息中提取数据
     * 
     * @param message NDEF消息
     * @return 提取的数据，失败返回null
     */
    @Nullable
    public String extractDataFromMessage(@NonNull NdefMessage message) {
        try {
            NdefRecord[] records = message.getRecords();
            
            for (NdefRecord record : records) {
                String mimeType = new String(record.getType(), StandardCharsets.US_ASCII);
                
                if (MIME_TYPE.equals(mimeType)) {
                    byte[] payload = record.getPayload();
                    String data = new String(payload, StandardCharsets.UTF_8);
                    
                    Log.d(TAG, "Data extracted: " + data.length() + " bytes");
                    notifyDataReceived(data);
                    
                    return data;
                }
            }

            Log.w(TAG, "No matching MIME type in message");
            return null;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to extract data from message", e);
            notifyError("解析NFC消息失败");
            return null;
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 创建MIME类型的NDEF记录
     */
    private NdefRecord createMimeRecord(String mimeType, String data) {
        byte[] mimeBytes = mimeType.getBytes(StandardCharsets.US_ASCII);
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        
        return new NdefRecord(
            NdefRecord.TNF_MIME_MEDIA,
            mimeBytes,
            new byte[0],
            dataBytes
        );
    }

    private void notifyStarted() {
        if (callback != null) {
            callback.onTransferStarted();
        }
    }

    private void notifySuccess() {
        if (callback != null) {
            callback.onTransferSuccess();
        }
    }

    private void notifyError(String error) {
        if (callback != null) {
            callback.onTransferFailed(error);
        }
    }

    private void notifyDataReceived(String data) {
        if (callback != null) {
            callback.onDataReceived(data);
        }
    }

    /**
     * 获取NFC适配器（用于Activity中设置前台分发）
     */
    @Nullable
    public NfcAdapter getNfcAdapter() {
        return nfcAdapter;
    }
}
