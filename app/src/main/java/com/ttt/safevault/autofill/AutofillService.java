package com.ttt.safevault.autofill;

import android.app.assist.AssistStructure;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.service.autofill.Dataset;
import android.service.autofill.FillCallback;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveRequest;
import android.service.autofill.SaveCallback;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import com.ttt.safevault.ServiceLocator;
import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.model.PasswordItem;
import com.ttt.safevault.ui.AutofillFilterActivity;
import com.ttt.safevault.utils.AutofillHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 自动填充服务实现
 * 处理应用的自动填充请求
 */
public class AutofillService extends android.service.autofill.AutofillService {

    private static final String TAG = "AutofillService";
    private BackendService backendService;
    private ExecutorService executor;
    private static final int MAX_DATASETS = 5;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "AutofillService onCreate");
        executor = Executors.newSingleThreadExecutor();
        // 获取BackendService实例
        backendService = ServiceLocator.getInstance().getBackendService();
        Log.d(TAG, "backendService: " + (backendService != null));
        
        // 尝试从保存的主密码自动解锁
        if (backendService != null && !backendService.isUnlocked()) {
            tryAutoUnlock();
        }
        Log.d(TAG, "isUnlocked after tryAutoUnlock: " + (backendService != null && backendService.isUnlocked()));
    }
    
    /**
     * 尝试自动解锁
     */
    private void tryAutoUnlock() {
        try {
            SharedPreferences prefs = getSharedPreferences("autofill_prefs", Context.MODE_PRIVATE);
            String savedPassword = prefs.getString("master_password", null);
            if (savedPassword != null) {
                boolean unlocked = backendService.unlock(savedPassword);
                Log.d(TAG, "Auto unlock result: " + unlocked);
            } else {
                Log.d(TAG, "No saved password for autofill");
            }
        } catch (Exception e) {
            Log.e(TAG, "Auto unlock failed", e);
        }
    }

    @Override
    public void onFillRequest(@NonNull FillRequest request,
                             @NonNull CancellationSignal cancellationSignal,
                             @NonNull FillCallback callback) {
        Log.d(TAG, "onFillRequest called");
        
        // 获取当前会话ID（可能为null）
        Bundle clientState = request.getClientState();
        long sessionId = clientState != null ? clientState.getLong("sessionId", 0) : 0;

        // 解析应用结构
        AssistStructure structure = request.getFillContexts()
                .get(request.getFillContexts().size() - 1)
                .getStructure();

        // 查找可填充的字段
        AutofillHelper.FieldResult fields = AutofillHelper.findAutofillFields(structure);
        
        Log.d(TAG, "Fields found - username: " + (fields != null && fields.usernameId != null) + 
                   ", password: " + (fields != null && fields.passwordId != null));

        if (fields == null || !fields.hasFields()) {
            Log.d(TAG, "No autofill fields found");
            callback.onSuccess(null);
            return;
        }

        // 获取应用包名和域名
        String packageName = structure.getActivityComponent().getPackageName();
        String domain = fields.webDomain != null ? fields.webDomain : packageName;
        Log.d(TAG, "Package: " + packageName + ", Domain: " + domain);

        // 异步加载匹配的凭据
        executor.execute(() -> {
            try {
                List<PasswordItem> credentials = getMatchingCredentials(domain);
                Log.d(TAG, "Found " + credentials.size() + " matching credentials");
                
                FillResponse response = buildFillResponse(credentials, fields, packageName);
                Log.d(TAG, "FillResponse built: " + (response != null));

                // 在主线程回调
                runOnUiThread(() -> callback.onSuccess(response));
            } catch (Exception e) {
                Log.e(TAG, "Error in onFillRequest", e);
                // 发生错误，返回null
                runOnUiThread(() -> callback.onSuccess(null));
            }
        });
    }

    @Override
    public void onSaveRequest(@NonNull SaveRequest request,
                             @NonNull SaveCallback callback) {
        // 获取保存的数据
        Bundle clientState = request.getClientState();

        // 解析字段
        AssistStructure structure = request.getFillContexts()
                .get(request.getFillContexts().size() - 1)
                .getStructure();

        AutofillHelper.FieldResult fields = AutofillHelper.findAutofillFields(structure);

        if (fields == null || !fields.hasRequiredFields()) {
            callback.onSuccess();
            return;
        }

        // 获取填充数据集（从客户端状态中）
        Bundle data = clientState != null ? clientState.getBundle("autofillData") : null;
        String username = null;
        String password = null;
        String url = null;

        if (data != null) {
            username = data.getString("username");
            password = data.getString("password");
            url = data.getString("url");
        }

        // 验证必要数据
        if (username == null || password == null) {
            callback.onSuccess();
            return;
        }

        // 生成标题
        String packageName = structure.getActivityComponent().getPackageName();
        final String finalTitle = generateTitle(packageName, url);
        final String finalUsername = username;
        final String finalPassword = password;
        final String finalUrl = url;

        // 异步保存密码
        executor.execute(() -> {
            try {
                saveCredentials(finalTitle, finalUsername, finalPassword, finalUrl);
                runOnUiThread(callback::onSuccess);
            } catch (Exception e) {
                runOnUiThread(() -> callback.onFailure("Failed to save credentials"));
            }
        });
    }

    /**
     * 获取匹配的凭据
     */
    private List<PasswordItem> getMatchingCredentials(String domain) {
        if (backendService == null) {
            Log.e(TAG, "backendService is null");
            return new ArrayList<>();
        }

        try {
            // 检查解锁状态，如果未解锁则尝试自动解锁
            if (!backendService.isUnlocked()) {
                Log.d(TAG, "Backend service is locked, trying auto unlock");
                tryAutoUnlock();
            }
            
            // 再次检查解锁状态
            if (!backendService.isUnlocked()) {
                Log.w(TAG, "Cannot access credentials: backend service is still locked");
                return new ArrayList<>();
            }
            
            List<PasswordItem> credentials = backendService.getAllItems();
            Log.d(TAG, "getAllItems returned: " + credentials.size() + " items");
            
            if (credentials.isEmpty()) {
                Log.w(TAG, "No credentials found in database");
            }
            
            return credentials;
        } catch (Exception e) {
            Log.e(TAG, "Error getting credentials", e);
            return new ArrayList<>();
        }
    }

    /**
     * 构建填充响应
     */
    private FillResponse buildFillResponse(List<PasswordItem> credentials,
                                           AutofillHelper.FieldResult fields,
                                           String packageName) {
        FillResponse.Builder responseBuilder = new FillResponse.Builder();
        
        boolean hasDataset = false;

        // 直接添加数据集，不使用认证
        int count = Math.min(credentials.size(), MAX_DATASETS);
        for (int i = 0; i < count; i++) {
            PasswordItem item = credentials.get(i);
            Dataset dataset = createDataset(item, fields);
            if (dataset != null) {
                responseBuilder.addDataset(dataset);
                hasDataset = true;
            }
        }

        // 如果没有任何数据集，返回null
        if (!hasDataset) {
            return null;
        }

        return responseBuilder.build();
    }

    /**
     * 创建数据集
     */
    private Dataset createDataset(PasswordItem item, AutofillHelper.FieldResult fields) {
        // 创建展示视图
        RemoteViews presentation = createDatasetPresentation(item);
        
        Dataset.Builder datasetBuilder = new Dataset.Builder(presentation);
        
        boolean hasValue = false;

        // 设置填充值
        if (fields.usernameId != null && item.getUsername() != null) {
            datasetBuilder.setValue(
                    fields.usernameId,
                    AutofillValue.forText(item.getUsername())
            );
            hasValue = true;
        }

        if (fields.passwordId != null && item.getPassword() != null) {
            datasetBuilder.setValue(
                    fields.passwordId,
                    AutofillValue.forText(item.getPassword())
            );
            hasValue = true;
        }

        // 必须至少有一个填充值
        if (!hasValue) {
            return null;
        }

        return datasetBuilder.build();
    }

    /**
     * 创建"打开SafeVault"数据集 - 使用 FillResponse 认证而非 Dataset 认证
     */
    private Dataset createOpenAppDataset(AutofillHelper.FieldResult fields) {
        // 此方法不再使用，由 buildAuthenticationResponse 替代
        return null;
    }
    
    /**
     * 构建需要认证的 FillResponse
     */
    private FillResponse buildAuthenticationResponse(AutofillHelper.FieldResult fields, String packageName) {
        try {
            // 创建跳转到应用的Intent
            Intent intent = new Intent(this, AutofillFilterActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            // 传递字段ID
            if (fields.usernameId != null) {
                intent.putExtra("usernameId", fields.usernameId);
                Log.d(TAG, "Passing usernameId to auth: " + fields.usernameId);
            }
            if (fields.passwordId != null) {
                intent.putExtra("passwordId", fields.passwordId);
                Log.d(TAG, "Passing passwordId to auth: " + fields.passwordId);
            }
            
            IntentSender intentSender = PendingIntent.getActivity(
                    this, 
                    (int) System.currentTimeMillis(),
                    intent, 
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_MUTABLE)
                    .getIntentSender();
            
            // 创建展示视图
            RemoteViews presentation = new RemoteViews(getPackageName(), android.R.layout.simple_list_item_1);
            presentation.setTextViewText(android.R.id.text1, "🔒 点击选择账号填充");
            presentation.setTextColor(android.R.id.text1, 0xFF1976D2);
            
            // 使用 FillResponse 认证
            FillResponse.Builder responseBuilder = new FillResponse.Builder();
            responseBuilder.setAuthentication(
                new AutofillId[]{fields.usernameId != null ? fields.usernameId : fields.passwordId},
                intentSender,
                presentation
            );
            
            return responseBuilder.build();
        } catch (Exception e) {
            Log.e(TAG, "buildAuthenticationResponse failed", e);
            return null;
        }
    }

    /**
     * 创建展示视图（不再使用）
     */
    private RemoteViews createPresentation(String packageName) {
        // 使用系统默认布局，避免自定义资源依赖
        RemoteViews presentation = new RemoteViews(getPackageName(), android.R.layout.simple_list_item_1);
        String appName = getAppName(packageName);
        presentation.setTextViewText(android.R.id.text1, appName);
        presentation.setTextColor(android.R.id.text1, 0xFF212121); // Dark text color
        return presentation;
    }

    /**
     * 创建数据集展示视图
     */
    private RemoteViews createDatasetPresentation(PasswordItem item) {
        // 使用系统默认布局，避免自定义资源依赖
        RemoteViews presentation = new RemoteViews(getPackageName(), android.R.layout.simple_list_item_2);
        presentation.setTextViewText(android.R.id.text1, item.getDisplayName());
        presentation.setTextColor(android.R.id.text1, 0xFF212121); // Dark text color

        if (item.getUsername() != null) {
            presentation.setTextViewText(android.R.id.text2, item.getUsername());
            presentation.setTextColor(android.R.id.text2, 0xFF757575); // Gray text color for subtitle
        }

        return presentation;
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
        PackageManager pm = getPackageManager();
        try {
            return pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            if (url != null) {
                return extractDomain(url);
            }
            return packageName;
        }
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

    /**
     * 获取应用名称
     */
    private String getAppName(String packageName) {
        PackageManager pm = getPackageManager();
        try {
            return pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    /**
     * 在主线程执行
     */
    private void runOnUiThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}