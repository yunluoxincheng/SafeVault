package com.ttt.safevault.security;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.ttt.safevault.utils.ClipboardManager;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 安全管理器
 * 负责管理应用的安全措施，包括防截图、自动锁定等
 */
public class SecurityManager implements LifecycleObserver {

    private static final String TAG = "SecurityManager";
    private static final int AUTO_LOCK_DELAY = 30000; // 30秒
    private static final int CLIPBOARD_CLEAR_DELAY = 30000; // 30秒

    private final Context context;
    private final ClipboardManager clipboardManager;
    private ScheduledExecutorService autoLockExecutor;
    private long lastInteractionTime;
    private boolean isLocked = false;

    public SecurityManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.clipboardManager = new ClipboardManager(this.context);
        this.lastInteractionTime = System.currentTimeMillis();
    }

    /**
     * 应用安全措施到Activity
     */
    public void applySecurityMeasures(@NonNull Activity activity) {
        // 防止截图
        preventScreenshots(activity);

        // 检查开发者选项
        if (isDeveloperOptionsEnabled()) {
            activity.finish();
            return;
        }

        // 检查设备是否rooted
        if (isDeviceRooted()) {
            // 可以选择警告用户或禁用某些功能
            showRootWarning(activity);
        }
    }

    /**
     * 应用安全措施到Fragment
     */
    public void applySecurityMeasures(@NonNull Fragment fragment) {
        Activity activity = fragment.getActivity();
        if (activity != null) {
            applySecurityMeasures(activity);
        }
    }

    /**
     * 应用安全措施到View
     */
    public void applySecurityMeasures(@NonNull View view) {
        // 防止View所在Activity被截图
        Context context = view.getContext();
        if (context instanceof Activity) {
            preventScreenshots((Activity) context);
        }
    }

    /**
     * 防止截图和录屏
     */
    public void preventScreenshots(@NonNull Activity activity) {
        activity.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SECURE |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        // 如果是敏感页面，可以添加更多保护
        if (isSensitiveActivity(activity)) {
            activity.getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            );
        }
    }

    
    /**
     * 开始自动锁定监控
     */
    public void startAutoLockMonitoring() {
        if (autoLockExecutor == null) {
            autoLockExecutor = Executors.newSingleThreadScheduledExecutor();
            autoLockExecutor.scheduleAtFixedRate(() -> {
                if (shouldAutoLock()) {
                    lock();
                }
            }, 5, 5, TimeUnit.SECONDS); // 每5秒检查一次
        }
    }

    /**
     * 停止自动锁定监控
     */
    public void stopAutoLockMonitoring() {
        if (autoLockExecutor != null) {
            autoLockExecutor.shutdownNow();
            autoLockExecutor = null;
        }
    }

    /**
     * 更新最后交互时间
     */
    public void updateLastInteraction() {
        lastInteractionTime = System.currentTimeMillis();
    }

    /**
     * 检查是否应该自动锁定
     */
    public boolean shouldAutoLock() {
        return !isLocked && (System.currentTimeMillis() - lastInteractionTime > AUTO_LOCK_DELAY);
    }

    /**
     * 锁定应用
     */
    public void lock() {
        isLocked = true;
        // 清理剪贴板
        clipboardManager.clearClipboard();
        // 触发锁定回调
        if (lockListener != null) {
            lockListener.onLocked();
        }
    }

    /**
     * 解锁应用
     */
    public void unlock() {
        isLocked = false;
        updateLastInteraction();
        // 触发解锁回调
        if (lockListener != null) {
            lockListener.onUnlocked();
        }
    }

    /**
     * 检查开发者选项是否开启
     */
    public boolean isDeveloperOptionsEnabled() {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;
    }

    /**
     * 检查设备是否已root
     */
    public boolean isDeviceRooted() {
        // 检查常见的root文件
        String[] rootFiles = {
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su"
        };

        for (String file : rootFiles) {
            if (new java.io.File(file).exists()) {
                return true;
            }
        }

        // 检查是否可以执行su命令
        try {
            Process process = Runtime.getRuntime().exec("su");
            process.destroy();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查是否为敏感Activity
     */
    private boolean isSensitiveActivity(@NonNull Activity activity) {
        String className = activity.getClass().getSimpleName();
        return className.contains("Password") ||
               className.contains("Login") ||
               className.contains("Detail");
    }

    /**
     * 显示root警告
     */
    private void showRootWarning(@NonNull Activity activity) {
        // 创建警告对话框
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(activity);
        builder.setTitle("安全警告");
        builder.setMessage("检测到设备可能已root，这可能会影响应用的安全性。建议在非root设备上使用。");
        builder.setPositiveButton("继续", null);
        builder.setCancelable(false);
        builder.show();
    }

    /**
     * 检查应用是否处于前台
     */
    public boolean isAppInForeground() {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) return false;

        List<ActivityManager.RunningAppProcessInfo> processes =
                activityManager.getRunningAppProcesses();
        if (processes == null) return false;

        String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo process : processes) {
            if (process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                process.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否有调试器附加
     */
    public boolean isDebuggerAttached() {
        return android.os.Debug.isDebuggerConnected() ||
               android.os.Debug.waitingForDebugger();
    }

    /**
     * 检查应用是否处于调试模式
     */
    public boolean isDebugMode() {
        return (context.getApplicationInfo().flags &
                ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    /**
     * 获取剪贴板管理器
     */
    public ClipboardManager getClipboardManager() {
        return clipboardManager;
    }

    // 生命周期事件处理
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onAppForegrounded() {
        updateLastInteraction();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onAppBackgrounded() {
        // 应用进入后台时可能需要额外的安全措施
    }

    // 锁定监听器接口
    public interface LockListener {
        void onLocked();
        void onUnlocked();
    }

    private LockListener lockListener;

    public void setLockListener(@Nullable LockListener listener) {
        this.lockListener = listener;
    }
    
    /**
     * 检查生物识别是否已启用且可用
     * @return true表示生物识别已启用且设备支持
     */
    public boolean isBiometricAuthenticationAvailable() {
        // 检查生物识别是否在设置中启用
        SecurityConfig config = new SecurityConfig(context);
        if (!config.isBiometricEnabled()) {
            return false;
        }
        
        // 检查设备是否支持生物识别
        return com.ttt.safevault.security.BiometricAuthHelper.isBiometricSupported(context);
    }
    
    /**
     * 检查生物识别是否已启用但设备不支持
     * @return true表示已启用但不可用
     */
    public boolean isBiometricEnabledButUnavailable() {
        SecurityConfig config = new SecurityConfig(context);
        return config.isBiometricEnabled() && 
               !com.ttt.safevault.security.BiometricAuthHelper.isBiometricSupported(context);
    }
}