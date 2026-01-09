package com.ttt.safevault.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.model.PasswordItem;
import com.ttt.safevault.model.SharePermission;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 密码分享页面的ViewModel
 * 负责管理分享配置和创建分享
 */
public class ShareViewModel extends AndroidViewModel {

    private final BackendService backendService;
    private final ExecutorService executor;

    // LiveData用于UI状态管理
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _shareResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _shareSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<PasswordItem> _passwordItem = new MutableLiveData<>();
    private final MutableLiveData<String> _sharePassword = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isOfflineShare = new MutableLiveData<>(false);

    public LiveData<Boolean> isLoading = _isLoading;
    public LiveData<String> errorMessage = _errorMessage;
    public LiveData<String> shareResult = _shareResult;
    public LiveData<Boolean> shareSuccess = _shareSuccess;
    public LiveData<PasswordItem> passwordItem = _passwordItem;
    public LiveData<String> sharePassword = _sharePassword;
    public LiveData<Boolean> isOfflineShare = _isOfflineShare;

    public ShareViewModel(@NonNull Application application, BackendService backendService) {
        super(application);
        this.backendService = backendService;
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * 加载要分享的密码条目
     */
    public void loadPasswordItem(int passwordId) {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        executor.execute(() -> {
            try {
                PasswordItem item = backendService.decryptItem(passwordId);
                _passwordItem.postValue(item);
            } catch (Exception e) {
                _errorMessage.postValue("加载密码失败: " + e.getMessage());
            } finally {
                _isLoading.postValue(false);
            }
        });
    }

    /**
     * 创建直接分享（无需好友）
     * @param passwordId 密码ID
     * @param expireInMinutes 过期时间（分钟）
     * @param permission 分享权限
     */
    public void createDirectShare(int passwordId, int expireInMinutes, 
                                 SharePermission permission) {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);
        _shareSuccess.setValue(false);

        executor.execute(() -> {
            try {
                String shareToken = backendService.createDirectPasswordShare(
                    passwordId, expireInMinutes, permission
                );
                
                if (shareToken != null && !shareToken.isEmpty()) {
                    _shareResult.postValue(shareToken);
                    _shareSuccess.postValue(true);
                } else {
                    _errorMessage.postValue("创建分享失败");
                }
            } catch (Exception e) {
                _errorMessage.postValue("创建分享失败: " + e.getMessage());
            } finally {
                _isLoading.postValue(false);
            }
        });
    }

    /**
     * 创建离线分享（二维码传输）
     * @param passwordId 密码ID
     * @param expireInMinutes 过期时间（分钟）
     * @param permission 分享权限
     */
    public void createOfflineShare(int passwordId, int expireInMinutes, 
                                  SharePermission permission) {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);
        _shareSuccess.setValue(false);
        _isOfflineShare.setValue(true);

        executor.execute(() -> {
            try {
                // 生成随机分享密码
                String password = backendService.generateSharePassword(8);
                _sharePassword.postValue(password);
                
                // 创建离线分享
                String qrContent = backendService.createOfflineShare(
                    passwordId, password, expireInMinutes, permission
                );
                
                if (qrContent != null && !qrContent.isEmpty()) {
                    _shareResult.postValue(qrContent);
                    _shareSuccess.postValue(true);
                } else {
                    _errorMessage.postValue("创建离线分享失败");
                }
            } catch (Exception e) {
                _errorMessage.postValue("创建离线分享失败: " + e.getMessage());
            } finally {
                _isLoading.postValue(false);
            }
        });
    }

    /**
     * 清除错误信息
     */
    public void clearError() {
        _errorMessage.setValue(null);
    }

    /**
     * 清除分享结果
     */
    public void clearShareResult() {
        _shareResult.setValue(null);
        _shareSuccess.setValue(false);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
