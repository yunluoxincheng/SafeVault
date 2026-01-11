package com.ttt.safevault.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.security.SecurityManager;

/**
 * 基础Activity
 * 提供安全管理器和安全措施的便捷访问
 *
 * 注意：自动锁定功能由 MainActivity 和 SafeVaultApplication 统一处理
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected SecurityManager securityManager;
    protected BackendService backendService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 获取安全管理器单例
        securityManager = SecurityManager.getInstance(this);
        // TODO: 获取BackendService实例
        // backendService = Injector.get().getBackendService();

        // 应用安全措施
        applySecurityMeasures();
    }

    /**
     * 应用安全措施
     */
    protected void applySecurityMeasures() {
        // 防止截图
        securityManager.applySecurityMeasures(this);
    }

    /**
     * 导航到登录页面
     */
    protected void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * 手动锁定应用
     */
    protected void lockApp() {
        if (securityManager != null) {
            securityManager.lock();
        }
    }

    /**
     * 获取安全管理器
     */
    protected SecurityManager getSecurityManager() {
        return securityManager;
    }
}