package com.ttt.safevault.autofill;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.model.PasswordItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 自动填充服务实现
 * 支持API 23+，但自动填充功能仅在API 26+可用
 *
 * 注意：实际的自动填充功能由AutofillServiceV26处理
 * 这个服务作为兼容层，确保在所有设备上都能正常安装
 */
public class AutofillServiceImpl extends Service {

    private BackendService backendService;
    private ExecutorService executor;

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
        // TODO: 获取BackendService实例
        // backendService = Injector.get().getBackendService();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // 返回null，因为我们不需要绑定
        return null;
    }

    // ========== 以下方法仅用于辅助 AutofillServiceV26 ==========
    // 这些方法可以在API 26+的设备上被调用

    /**
     * 获取匹配的凭据（兼容所有API级别）
     */
    private List<PasswordItem> getMatchingCredentials(String domain) {
        if (backendService == null) {
            return new ArrayList<>();
        }

        try {
            // 先尝试精确匹配域名
            List<PasswordItem> credentials = backendService.getCredentialsForDomain(domain);

            // 如果没有结果，尝试部分匹配
            if (credentials.isEmpty()) {
                // 尝试提取域名部分
                String domainPart = extractDomain(domain);
                if (!domainPart.equals(domain)) {
                    credentials = backendService.getCredentialsForDomain(domainPart);
                }
            }

            return credentials;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 保存凭据
     */
    private void saveCredentials(String title, String username, String password, String url) {
        if (backendService == null) return;

        PasswordItem item = new PasswordItem();
        item.setTitle(title);
        item.setUsername(username);
        item.setPassword(password);
        item.setUrl(url);
        item.updateTimestamp();

        backendService.saveItem(item);
    }

    /**
     * 生成标题
     */
    private String generateTitle(String packageName, String url) {
        if (url != null) {
            return extractDomain(url);
        }
        return packageName;
    }

    /**
     * 提取域名
     */
    private String extractDomain(String url) {
        if (url == null) return "";

        // 移除协议
        String domain = url.replace("https://", "")
                         .replace("http://", "")
                         .replace("www.", "");

        // 移除路径
        int slashIndex = domain.indexOf('/');
        if (slashIndex > 0) {
            domain = domain.substring(0, slashIndex);
        }

        return domain;
    }
}