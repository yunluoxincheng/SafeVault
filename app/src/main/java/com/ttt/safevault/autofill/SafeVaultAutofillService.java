package com.ttt.safevault.autofill;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Build;
import android.os.CancellationSignal;
import android.service.autofill.AutofillService;
import android.service.autofill.FillCallback;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveRequest;
import android.util.Log;
import android.view.autofill.AutofillId;

import com.ttt.safevault.ServiceLocator;
import com.ttt.safevault.autofill.builder.FillResponseBuilder;
import com.ttt.safevault.autofill.matcher.AutofillMatcher;
import com.ttt.safevault.autofill.model.AutofillField;
import com.ttt.safevault.autofill.model.AutofillParsedData;
import com.ttt.safevault.autofill.model.AutofillRequest;
import com.ttt.safevault.autofill.parser.AutofillParser;
import com.ttt.safevault.autofill.security.SecurityConfig;
import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.model.PasswordItem;
import com.ttt.safevault.ui.LoginActivity;
import com.ttt.safevault.ui.autofill.AutofillSaveActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SafeVault自动填充服务
 * 处理系统的自动填充请求和保存请求
 */
public class SafeVaultAutofillService extends AutofillService {
    private static final String TAG = "SafeVaultAutofillService";
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private BackendService backendService;
    private SecurityConfig securityConfig;

    @Override
    public void onCreate() {
        super.onCreate();
        logDebug("=== SafeVaultAutofillService onCreate ===");
        
        // 初始化BackendService
        backendService = ServiceLocator.getInstance().getBackendService();
        
        // 初始化安全配置
        securityConfig = new SecurityConfig();
    }

    @Override
    public void onConnected() {
        super.onConnected();
        logDebug("=== AutofillService 已连接 ===");
    }

    @Override
    public void onDisconnected() {
        super.onDisconnected();
        logDebug("=== AutofillService 已断开 ===");
    }

    @Override
    public void onFillRequest(FillRequest request, CancellationSignal cancellationSignal, 
                             FillCallback callback) {
        logDebug("=== 收到 FillRequest ===");

        // 异步处理请求
        executor.execute(() -> {
            try {
                // 检查是否被取消
                if (cancellationSignal.isCanceled()) {
                    logDebug("请求已取消");
                    return;
                }

                // 解析请求
                AutofillParsedData parsedData = AutofillParser.parseFillRequest(request);
                if (parsedData == null) {
                    logDebug("解析失败");
                    callback.onFailure("解析请求失败");
                    return;
                }

                // 安全检查：检查是否在阻止列表中
                String packageName = parsedData.getPackageName();
                if (securityConfig.isBlocked(packageName)) {
                    logDebug("应用在阻止列表中: " + packageName);
                    callback.onSuccess(null);
                    return;
                }

                // 构建AutofillRequest
                AutofillRequest autofillRequest = buildAutofillRequest(parsedData);
                if (autofillRequest == null) {
                    logDebug("构建AutofillRequest失败");
                    callback.onFailure("无法识别填充字段");
                    return;
                }

                // 检查应用是否已解锁
                if (backendService == null || !backendService.isUnlocked()) {
                    logDebug("应用未解锁，需要认证");
                    
                    // 创建认证Intent（打开LoginActivity）
                    Intent authIntent = new Intent(this, LoginActivity.class);
                    authIntent.putExtra("autofill_request", true);
                    authIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    
                    PendingIntent pendingIntent = PendingIntent.getActivity(
                            this, 
                            0, 
                            authIntent,
                            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                    );
                    
                    IntentSender intentSender = pendingIntent.getIntentSender();
                    
                    // 构建带认证的响应
                    FillResponseBuilder builder = new FillResponseBuilder(this);
                    FillResponse response = builder.buildResponse(autofillRequest, null, intentSender);
                    
                    callback.onSuccess(response);
                    return;
                }

                // 匹配凭据
                AutofillMatcher matcher = new AutofillMatcher(backendService);
                List<PasswordItem> credentials = matcher.matchCredentials(autofillRequest);

                // 构建响应
                FillResponseBuilder builder = new FillResponseBuilder(this);
                FillResponse response = builder.buildResponse(autofillRequest, credentials, null);

                if (response != null) {
                    logDebug("FillResponse构建成功");
                    callback.onSuccess(response);
                } else {
                    logDebug("没有可用的凭据");
                    callback.onSuccess(null);
                }

            } catch (Exception e) {
                logDebug("处理FillRequest异常: " + e.getMessage());
                e.printStackTrace();
                callback.onFailure("处理请求失败: " + e.getMessage());
            }
        });

        // 监听取消信号
        cancellationSignal.setOnCancelListener(() -> {
            logDebug("FillRequest被取消");
        });
    }

    @Override
    public void onSaveRequest(SaveRequest request, SaveCallback callback) {
        logDebug("=== 收到 SaveRequest ===");

        try {
            // 解析SaveRequest
            AutofillParsedData parsedData = AutofillParser.parseSaveRequest(request);
            if (parsedData == null) {
                logDebug("解析SaveRequest失败");
                callback.onFailure("解析保存请求失败");
                return;
            }

            // 提取用户名和密码值
            String username = extractFieldValue(parsedData, AutofillField.FieldType.USERNAME);
            String password = extractFieldValue(parsedData, AutofillField.FieldType.PASSWORD);

            if (password == null || password.isEmpty()) {
                logDebug("密码为空，无法保存");
                callback.onFailure("密码不能为空");
                return;
            }

            // 创建Intent启动保存Activity
            Intent saveIntent = new Intent(this, AutofillSaveActivity.class);
            saveIntent.putExtra("username", username);
            saveIntent.putExtra("password", password);
            saveIntent.putExtra("domain", parsedData.getDomain());
            saveIntent.putExtra("packageName", parsedData.getPackageName());
            saveIntent.putExtra("isWeb", parsedData.isWeb());
            saveIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // 创建PendingIntent
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    saveIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            IntentSender intentSender = pendingIntent.getIntentSender();
            
            // 返回IntentSender给系统
            callback.onSuccess(intentSender);
            
            logDebug("SaveRequest处理成功，已启动保存界面");

        } catch (Exception e) {
            logDebug("处理SaveRequest异常: " + e.getMessage());
            e.printStackTrace();
            callback.onFailure("处理保存请求失败");
        }
    }

    /**
     * 构建AutofillRequest
     */
    private AutofillRequest buildAutofillRequest(AutofillParsedData parsedData) {
        AutofillRequest.Builder builder = new AutofillRequest.Builder();

        // 添加用户名字段
        for (AutofillField field : parsedData.getUsernameFields()) {
            builder.addUsernameId(field.getAutofillId());
        }

        // 添加密码字段
        for (AutofillField field : parsedData.getPasswordFields()) {
            builder.addPasswordId(field.getAutofillId());
        }

        // 设置元数据
        builder.setDomain(parsedData.getDomain())
               .setPackageName(parsedData.getPackageName())
               .setApplicationName(parsedData.getApplicationName())
               .setIsWeb(parsedData.isWeb());

        AutofillRequest request = builder.build();
        
        // 检查是否有有效字段（用户名或密码字段任一即可）
        if (request.getUsernameIds().isEmpty() && request.getPasswordIds().isEmpty()) {
            logDebug("没有找到用户名或密码字段");
            return null;
        }
        
        logDebug("找到字段: 用户名=" + request.getUsernameIds().size() + 
                ", 密码=" + request.getPasswordIds().size());

        return request;
    }

    /**
     * 从解析数据中提取字段值
     */
    private String extractFieldValue(AutofillParsedData parsedData, 
                                     AutofillField.FieldType fieldType) {
        for (AutofillField field : parsedData.getFields()) {
            if (field.getFieldType() == fieldType) {
                // 注意：这里只能获取hint，实际值需要从SaveRequest中获取
                // 实际实现中需要使用AssistStructure.ViewNode.getText()
                return field.getHint();
            }
        }
        return null;
    }

    /**
     * 调试日志输出到文件
     */
    private void logDebug(String message) {
        Log.d(TAG, message);
        
        // 同时输出到文件（用于手机端调试）
        try {
            String logDir = "/storage/emulated/0/Android/data/com.ttt.safevault/files/autofill_logs/";
            File dir = new File(logDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                    .format(new Date());
            String logMessage = timestamp + " [" + TAG + "] " + message + "\n";
            
            File logFile = new File(dir, "autofill_service.log");
            FileWriter writer = new FileWriter(logFile, true);
            writer.write(logMessage);
            writer.close();
        } catch (IOException e) {
            // 忽略日志写入错误
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        logDebug("=== SafeVaultAutofillService onDestroy ===");
    }
}
