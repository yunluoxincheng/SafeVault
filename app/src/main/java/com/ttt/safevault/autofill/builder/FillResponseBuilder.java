package com.ttt.safevault.autofill.builder;

import android.content.Context;
import android.content.IntentSender;
import android.os.Build;
import android.service.autofill.Dataset;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveInfo;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;

import com.ttt.safevault.R;
import com.ttt.safevault.autofill.model.AutofillRequest;
import com.ttt.safevault.model.PasswordItem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * FillResponse构建器
 * 负责创建自动填充响应，包括Dataset和SaveInfo
 */
public class FillResponseBuilder {
    private static final String TAG = "FillResponseBuilder";
    
    private final Context context;

    public FillResponseBuilder(Context context) {
        this.context = context;
    }

    /**
     * 构建FillResponse
     *
     * @param request  AutofillRequest对象
     * @param credentials 匹配的凭据列表
     * @param authIntentSender 认证IntentSender（当应用未解锁时）
     * @return FillResponse对象
     */
    public FillResponse buildResponse(AutofillRequest request, 
                                      List<PasswordItem> credentials,
                                      IntentSender authIntentSender) {
        logDebug("=== 开始构建 FillResponse ===");
        logDebug("凭据数量: " + (credentials != null ? credentials.size() : 0));

        if (request == null) {
            logDebug("AutofillRequest为null");
            return null;
        }

        FillResponse.Builder responseBuilder = new FillResponse.Builder();

        // 如果需要认证（应用未解锁）
        if (authIntentSender != null) {
            logDebug("需要认证，设置认证IntentSender");
            
            // 创建认证提示的RemoteViews
            RemoteViews authPresentation = createAuthPresentation();
            
            // 创建一个Dataset作为认证入口
            Dataset.Builder authDatasetBuilder = new Dataset.Builder(authPresentation);
            
            // 为所有字段设置空值（仅用于触发认证）
            for (AutofillId usernameId : request.getUsernameIds()) {
                authDatasetBuilder.setValue(usernameId, null, authPresentation);
            }
            for (AutofillId passwordId : request.getPasswordIds()) {
                authDatasetBuilder.setValue(passwordId, null, authPresentation);
            }
            
            // 设置认证Intent
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                authDatasetBuilder.setAuthentication(authIntentSender);
            }
            
            responseBuilder.addDataset(authDatasetBuilder.build());
            
            // 添加SaveInfo
            SaveInfo saveInfo = createSaveInfo(request);
            if (saveInfo != null) {
                responseBuilder.setSaveInfo(saveInfo);
            }
            
            return responseBuilder.build();
        }

        // 如果没有匹配的凭据
        if (credentials == null || credentials.isEmpty()) {
            logDebug("没有匹配的凭据，仍然提供保存选项");
            
            // 即使没有匹配的凭据，也要添加SaveInfo以支持保存新凭据
            SaveInfo saveInfo = createSaveInfo(request);
            if (saveInfo != null) {
                responseBuilder.setSaveInfo(saveInfo);
                logDebug("已添加SaveInfo，用户可以保存新凭据");
                return responseBuilder.build();
            }
            
            // 如果无法创建SaveInfo（没有密码字段），返回null
            logDebug("无法创建SaveInfo，返回null");
            return null;
        }

        // 为每个凭据创建Dataset
        for (PasswordItem credential : credentials) {
            Dataset dataset = createDataset(request, credential);
            if (dataset != null) {
                responseBuilder.addDataset(dataset);
                logDebug("添加Dataset: " + credential.getTitle());
            }
        }

        // 添加SaveInfo以支持保存新凭据
        SaveInfo saveInfo = createSaveInfo(request);
        if (saveInfo != null) {
            responseBuilder.setSaveInfo(saveInfo);
        }

        return responseBuilder.build();
    }

    /**
     * 创建Dataset
     */
    private Dataset createDataset(AutofillRequest request, PasswordItem credential) {
        // 创建presentation（显示给用户的视图）
        RemoteViews presentation = createPresentation(credential);
        
        Dataset.Builder datasetBuilder = new Dataset.Builder(presentation);
        
        boolean hasAnyField = false;

        // 设置用户名字段的值
        for (AutofillId usernameId : request.getUsernameIds()) {
            AutofillValue usernameValue = AutofillValue.forText(credential.getUsername());
            datasetBuilder.setValue(usernameId, usernameValue, presentation);
            hasAnyField = true;
            logDebug("设置用户名字段: " + usernameId);
        }

        // 设置密码字段的值
        for (AutofillId passwordId : request.getPasswordIds()) {
            AutofillValue passwordValue = AutofillValue.forText(credential.getPassword());
            datasetBuilder.setValue(passwordId, passwordValue, presentation);
            hasAnyField = true;
            logDebug("设置密码字段: " + passwordId);
        }
        
        // 确保至少有一个字段被设置
        if (!hasAnyField) {
            logDebug("没有字段可设置");
            return null;
        }

        return datasetBuilder.build();
    }

    /**
     * 创建Presentation视图（显示凭据信息）
     */
    private RemoteViews createPresentation(PasswordItem credential) {
        RemoteViews presentation = new RemoteViews(
                context.getPackageName(), 
                R.layout.autofill_dataset_item
        );

        // 设置标题
        String title = credential.getTitle();
        if (title == null || title.isEmpty()) {
            title = credential.getUsername();
        }
        presentation.setTextViewText(R.id.autofill_dataset_title, title);

        // 设置用户名（部分隐藏）
        String username = credential.getUsername();
        String maskedUsername = maskString(username);
        presentation.setTextViewText(R.id.autofill_dataset_username, maskedUsername);

        return presentation;
    }

    /**
     * 创建认证提示的RemoteViews
     */
    private RemoteViews createAuthPresentation() {
        RemoteViews presentation = new RemoteViews(
                context.getPackageName(),
                R.layout.autofill_auth_item
        );
        
        presentation.setTextViewText(
                R.id.autofill_auth_text,
                context.getString(R.string.autofill_unlock_safevault)
        );
        
        return presentation;
    }

    /**
     * 创建SaveInfo
     */
    private SaveInfo createSaveInfo(AutofillRequest request) {
        List<AutofillId> usernameIds = request.getUsernameIds();
        List<AutofillId> passwordIds = request.getPasswordIds();

        // 至少需要一个密码字段
        if (passwordIds.isEmpty()) {
            logDebug("没有密码字段，无法创建SaveInfo");
            return null;
        }

        SaveInfo.Builder saveInfoBuilder = new SaveInfo.Builder(
                SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                passwordIds.toArray(new AutofillId[0])
        );

        // 添加可选的用户名字段
        if (!usernameIds.isEmpty()) {
            saveInfoBuilder.setOptionalIds(usernameIds.toArray(new AutofillId[0]));
        }

        return saveInfoBuilder.build();
    }

    /**
     * 部分隐藏字符串（用于显示）
     */
    private String maskString(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }

        if (str.length() <= 4) {
            return str;
        }

        // 显示前2个和后2个字符，中间用*替代
        int visibleChars = 2;
        String start = str.substring(0, visibleChars);
        String end = str.substring(str.length() - visibleChars);
        int maskedLength = str.length() - (visibleChars * 2);
        
        StringBuilder masked = new StringBuilder(start);
        for (int i = 0; i < maskedLength; i++) {
            masked.append("*");
        }
        masked.append(end);

        return masked.toString();
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
            
            File logFile = new File(dir, "autofill_builder.log");
            FileWriter writer = new FileWriter(logFile, true);
            writer.write(logMessage);
            writer.close();
        } catch (IOException e) {
            // 忽略日志写入错误
        }
    }
}
