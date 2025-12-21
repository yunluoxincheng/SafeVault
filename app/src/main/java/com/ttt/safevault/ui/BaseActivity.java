package com.ttt.safevault.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.security.SecurityManager;

/**
 * 基础Activity
 * 所有Activity都应该继承此类以自动应用安全措施
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected SecurityManager securityManager;
    protected BackendService backendService;
    private boolean shouldAutoLock = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化安全管理器
        securityManager = new SecurityManager(this);
        // TODO: 获取BackendService实例
        // backendService = Injector.get().getBackendService();

        // 应用安全措施
        applySecurityMeasures();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 开始自动锁定监控
        if (shouldAutoLock) {
            securityManager.startAutoLockMonitoring();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 更新最后交互时间
        if (shouldAutoLock) {
            securityManager.updateLastInteraction();
        }

        // 检查是否需要锁定
        if (securityManager.shouldAutoLock()) {
            navigateToLogin();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 记录进入后台的时间
        if (backendService != null) {
            backendService.recordBackgroundTime();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 停止自动锁定监控
        securityManager.stopAutoLockMonitoring();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理资源
        if (securityManager != null) {
            securityManager.stopAutoLockMonitoring();
        }
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
        // 用户交互时更新时间
        if (shouldAutoLock) {
            securityManager.updateLastInteraction();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        // 用户交互时更新时间
        if (shouldAutoLock) {
            securityManager.updateLastInteraction();
        }
    }

    /**
     * 应用安全措施
     */
    protected void applySecurityMeasures() {
        // 防止截图
        securityManager.applySecurityMeasures(this);

        // 设置锁定监听器
        securityManager.setLockListener(new SecurityManager.LockListener() {
            @Override
            public void onLocked() {
                runOnUiThread(() -> navigateToLogin());
            }

            @Override
            public void onUnlocked() {
                // 解锁后的处理
            }
        });
    }

    /**
     * 禁用自动锁定
     */
    protected void disableAutoLock() {
        shouldAutoLock = false;
        if (securityManager != null) {
            securityManager.stopAutoLockMonitoring();
        }
    }

    /**
     * 启用自动锁定
     */
    protected void enableAutoLock() {
        shouldAutoLock = true;
        if (securityManager != null) {
            securityManager.startAutoLockMonitoring();
        }
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