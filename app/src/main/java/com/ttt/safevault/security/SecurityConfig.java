package com.ttt.safevault.security;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * 安全配置类
 * 管理安全相关的设置
 */
public class SecurityConfig {

    private static final String PREFS_NAME = "security_prefs";
    private static final String PREF_AUTO_LOCK_ENABLED = "auto_lock_enabled";
    private static final String PREF_AUTO_LOCK_TIMEOUT = "auto_lock_timeout";
    private static final String PREF_CLIPBOARD_CLEAR_ENABLED = "clipboard_clear_enabled";
    private static final String PREF_CLIPBOARD_CLEAR_TIMEOUT = "clipboard_clear_timeout";
    private static final String PREF_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String PREF_SCREENSHOT_PROTECTION = "screenshot_protection";

    // 默认值
    public static final boolean DEFAULT_AUTO_LOCK_ENABLED = true;
    public static final int DEFAULT_AUTO_LOCK_TIMEOUT = 30; // 秒
    public static final boolean DEFAULT_CLIPBOARD_CLEAR_ENABLED = true;
    public static final int DEFAULT_CLIPBOARD_CLEAR_TIMEOUT = 30; // 秒
    public static final boolean DEFAULT_BIOMETRIC_ENABLED = false;
    public static final boolean DEFAULT_SCREENSHOT_PROTECTION = true;

    private final SharedPreferences prefs;

    public SecurityConfig(@NonNull Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 自动锁定设置
     */
    public boolean isAutoLockEnabled() {
        return prefs.getBoolean(PREF_AUTO_LOCK_ENABLED, DEFAULT_AUTO_LOCK_ENABLED);
    }

    public void setAutoLockEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_AUTO_LOCK_ENABLED, enabled).apply();
    }

    public int getAutoLockTimeout() {
        return prefs.getInt(PREF_AUTO_LOCK_TIMEOUT, DEFAULT_AUTO_LOCK_TIMEOUT);
    }

    public void setAutoLockTimeout(int timeoutSeconds) {
        prefs.edit().putInt(PREF_AUTO_LOCK_TIMEOUT, timeoutSeconds).apply();
    }

    /**
     * 剪贴板清理设置
     */
    public boolean isClipboardClearEnabled() {
        return prefs.getBoolean(PREF_CLIPBOARD_CLEAR_ENABLED, DEFAULT_CLIPBOARD_CLEAR_ENABLED);
    }

    public void setClipboardClearEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_CLIPBOARD_CLEAR_ENABLED, enabled).apply();
    }

    public int getClipboardClearTimeout() {
        return prefs.getInt(PREF_CLIPBOARD_CLEAR_TIMEOUT, DEFAULT_CLIPBOARD_CLEAR_TIMEOUT);
    }

    public void setClipboardClearTimeout(int timeoutSeconds) {
        prefs.edit().putInt(PREF_CLIPBOARD_CLEAR_TIMEOUT, timeoutSeconds).apply();
    }

    /**
     * 生物识别设置
     */
    public boolean isBiometricEnabled() {
        return prefs.getBoolean(PREF_BIOMETRIC_ENABLED, DEFAULT_BIOMETRIC_ENABLED);
    }

    public void setBiometricEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_BIOMETRIC_ENABLED, enabled).apply();
    }

    /**
     * 截图保护设置
     */
    public boolean isScreenshotProtectionEnabled() {
        return prefs.getBoolean(PREF_SCREENSHOT_PROTECTION, DEFAULT_SCREENSHOT_PROTECTION);
    }

    public void setScreenshotProtectionEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_SCREENSHOT_PROTECTION, enabled).apply();
    }

    /**
     * 重置所有设置为默认值
     */
    public void resetToDefaults() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_AUTO_LOCK_ENABLED, DEFAULT_AUTO_LOCK_ENABLED);
        editor.putInt(PREF_AUTO_LOCK_TIMEOUT, DEFAULT_AUTO_LOCK_TIMEOUT);
        editor.putBoolean(PREF_CLIPBOARD_CLEAR_ENABLED, DEFAULT_CLIPBOARD_CLEAR_ENABLED);
        editor.putInt(PREF_CLIPBOARD_CLEAR_TIMEOUT, DEFAULT_CLIPBOARD_CLEAR_TIMEOUT);
        editor.putBoolean(PREF_BIOMETRIC_ENABLED, DEFAULT_BIOMETRIC_ENABLED);
        editor.putBoolean(PREF_SCREENSHOT_PROTECTION, DEFAULT_SCREENSHOT_PROTECTION);
        editor.apply();
    }

    /**
     * 清除所有设置
     */
    public void clear() {
        prefs.edit().clear().apply();
    }

    /**
     * 获取自动锁定的毫秒数
     */
    public long getAutoLockTimeoutMillis() {
        return getAutoLockTimeout() * 1000L;
    }

    /**
     * 获取剪贴板清理的毫秒数
     */
    public long getClipboardClearTimeoutMillis() {
        return getClipboardClearTimeout() * 1000L;
    }

    /**
     * 应用默认设置（首次安装时调用）
     */
    public void applyDefaults() {
        // 只有当设置不存在时才应用默认值
        if (!prefs.contains(PREF_AUTO_LOCK_ENABLED)) {
            resetToDefaults();
        }
    }
}