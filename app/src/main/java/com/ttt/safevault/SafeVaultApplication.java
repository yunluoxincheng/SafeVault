package com.ttt.safevault;

import android.app.Application;

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
    }
}
