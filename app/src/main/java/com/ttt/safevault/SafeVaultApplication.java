package com.ttt.safevault;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.ttt.safevault.security.SecurityConfig;

/**
 * SafeVault应用程序类
 * 负责初始化全局服务
 */
public class SafeVaultApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化服务定位器
        ServiceLocator.init(this);

        // 加载并应用主题设置
        applyThemeSettings();
    }

    /**
     * 应用主题设置
     * 从SharedPreferences加载保存的主题模式并应用
     */
    private void applyThemeSettings() {
        SecurityConfig securityConfig = new SecurityConfig(this);
        SecurityConfig.ThemeMode themeMode = securityConfig.getThemeMode();

        int nightMode;
        switch (themeMode) {
            case LIGHT:
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case DARK:
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case SYSTEM:
            default:
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }

        AppCompatDelegate.setDefaultNightMode(nightMode);
    }
}
