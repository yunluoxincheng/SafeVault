package com.ttt.safevault.autofill;

import android.app.assist.AssistStructure;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.service.autofill.AutofillService;
import android.service.autofill.Dataset;
import android.service.autofill.FillCallback;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveRequest;
import android.service.autofill.SaveCallback;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.model.PasswordItem;
import com.ttt.safevault.ui.AutofillFilterActivity;
import com.ttt.safevault.utils.AutofillHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 自动填充服务实现（仅用于 API 26+）
 * 处理应用的自动填充请求
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class AutofillServiceV26 extends AutofillService {

    private BackendService backendService;
    private ExecutorService executor;
    private static final int MAX_DATASETS = 5;

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
        // TODO: 获取BackendService实例
        // backendService = Injector.get().getBackendService();
    }

    @Override
    public void onFillRequest(@NonNull FillRequest request,
                             @NonNull CancellationSignal cancellationSignal,
                             @NonNull FillCallback callback) {
        // 获取当前会话ID
        long sessionId = request.getClientState().getLong("sessionId", 0);

        // 解析应用结构
        AssistStructure structure = request.getFillContexts()
                .get(request.getFillContexts().size() - 1)
                .getStructure();

        // 查找可填充的字段
        AutofillHelper.FieldResult fields = AutofillHelper.findAutofillFields(structure);

        if (fields == null || !fields.hasFields()) {
            callback.onSuccess(null);
            return;
        }

        // 获取应用包名和域名
        String packageName = structure.getActivityComponent().getPackageName();
        String domain = fields.webDomain != null ? fields.webDomain : packageName;

        // 异步加载匹配的凭据
        executor.execute(() -> {
            try {
                List<PasswordItem> credentials = getMatchingCredentials(domain);
                FillResponse response = buildFillResponse(credentials, fields, packageName);

                // 在主线程回调
                runOnUiThread(() -> callback.onSuccess(response));
            } catch (Exception e) {
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
     * 构建填充响应
     */
    private FillResponse buildFillResponse(List<PasswordItem> credentials,
                                           AutofillHelper.FieldResult fields,
                                           String packageName) {
        FillResponse.Builder responseBuilder = new FillResponse.Builder();

        // 添加头部信息（仅API 28+支持setHeader）
        // 使用系统默认布局，避免自定义资源依赖
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            RemoteViews presentation = new RemoteViews(getPackageName(), android.R.layout.simple_list_item_1);
            String appName = getAppName(packageName);
            presentation.setTextViewText(android.R.id.text1, appName);
            presentation.setTextColor(android.R.id.text1, 0xFF212121); // Dark text color
            responseBuilder.setHeader(presentation);
        }

        // 添加数据集
        int count = Math.min(credentials.size(), MAX_DATASETS);
        for (int i = 0; i < count; i++) {
            PasswordItem item = credentials.get(i);
            Dataset dataset = createDataset(item, fields);
            if (dataset != null) {
                responseBuilder.addDataset(dataset);
            }
        }

        // 添加"其他"选项
        if (credentials.size() > MAX_DATASETS) {
            responseBuilder.addDataset(createMoreOptionsDataset());
        }

        // 添加保存选项
        if (credentials.isEmpty()) {
            responseBuilder.addDataset(createNewPasswordDataset());
        }

        // 设置客户端状态
        Bundle clientState = new Bundle();
        clientState.putLong("sessionId", System.currentTimeMillis());
        responseBuilder.setClientState(clientState);

        return responseBuilder.build();
    }

    /**
     * 创建数据集
     */
    private Dataset createDataset(PasswordItem item, AutofillHelper.FieldResult fields) {
        Dataset.Builder datasetBuilder = new Dataset.Builder();

        // 创建展示视图
        RemoteViews presentation = createDatasetPresentation(item);

        // 设置填充值
        if (fields.usernameId != null && item.getUsername() != null) {
            datasetBuilder.setValue(
                    fields.usernameId,
                    AutofillValue.forText(item.getUsername()),
                    presentation
            );
        }

        if (fields.passwordId != null && item.getPassword() != null) {
            datasetBuilder.setValue(
                    fields.passwordId,
                    AutofillValue.forText(item.getPassword())
            );
        }

        return datasetBuilder.build();
    }

    /**
     * 创建"更多选项"数据集
     */
    private Dataset createMoreOptionsDataset() {
        Intent intent = new Intent(this, AutofillFilterActivity.class);
        IntentSender intentSender = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE)
                .getIntentSender();

        // 使用系统默认布局，避免自定义资源依赖
        RemoteViews presentation = new RemoteViews(getPackageName(), android.R.layout.simple_list_item_1);
        presentation.setTextViewText(android.R.id.text1, "更多选项...");
        presentation.setTextColor(android.R.id.text1, 0xFF212121); // Dark text color

        return new Dataset.Builder(presentation)
                .setAuthentication(intentSender)
                .build();
    }

    /**
     * 创建新建密码数据集
     */
    private Dataset createNewPasswordDataset() {
        // 使用系统默认布局，避免自定义资源依赖
        RemoteViews presentation = new RemoteViews(getPackageName(), android.R.layout.simple_list_item_1);
        presentation.setTextViewText(android.R.id.text1, "新建密码");
        presentation.setTextColor(android.R.id.text1, 0xFF212121); // Dark text color

        return new Dataset.Builder(presentation).build();
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