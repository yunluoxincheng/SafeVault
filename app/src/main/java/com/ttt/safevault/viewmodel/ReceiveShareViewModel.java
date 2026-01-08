package com.ttt.safevault.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.model.PasswordItem;
import com.ttt.safevault.model.PasswordShare;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 接收分享页面的ViewModel
 * 负责管理分享接收和保存
 */
public class ReceiveShareViewModel extends AndroidViewModel {

    private final BackendService backendService;
    private final ExecutorService executor;

    // LiveData用于UI状态管理
    private final MutableLiveData<PasswordItem> _sharedPassword = new MutableLiveData<>();
    private final MutableLiveData<PasswordShare> _shareDetails = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _saveSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> _savedPasswordId = new MutableLiveData<>();

    public LiveData<PasswordItem> sharedPassword = _sharedPassword;
    public LiveData<PasswordShare> shareDetails = _shareDetails;
    public LiveData<Boolean> isLoading = _isLoading;
    public LiveData<String> errorMessage = _errorMessage;
    public LiveData<Boolean> saveSuccess = _saveSuccess;
    public LiveData<Integer> savedPasswordId = _savedPasswordId;

    public ReceiveShareViewModel(@NonNull Application application, BackendService backendService) {
        super(application);
        this.backendService = backendService;
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * 接收密码分享
     * @param shareId 分享ID或分享Token
     */
    public void receiveShare(String shareId) {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        executor.execute(() -> {
            try {
                // 获取分享详情
                PasswordShare shareDetails = backendService.getShareDetails(shareId);
                _shareDetails.postValue(shareDetails);

                // 解密并获取密码数据
                PasswordItem passwordItem = backendService.receivePasswordShare(shareId);
                
                if (passwordItem != null) {
                    _sharedPassword.postValue(passwordItem);
                } else {
                    _errorMessage.postValue("无法接收分享：数据无效");
                }
            } catch (Exception e) {
                _errorMessage.postValue("接收分享失败: " + e.getMessage());
            } finally {
                _isLoading.postValue(false);
            }
        });
    }

    /**
     * 接收离线分享（二维码）
     * @param qrContent 二维码内容
     * @param sharePassword 分享密码
     */
    public void receiveOfflineShare(String qrContent, String sharePassword) {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        executor.execute(() -> {
            try {
                // 解析离线分享
                PasswordItem passwordItem = backendService.receiveOfflineShare(qrContent, sharePassword);
                
                if (passwordItem != null) {
                    _sharedPassword.postValue(passwordItem);
                    // 离线分享没有分享详情，创建一个默认的
                    PasswordShare fakeShare = new PasswordShare();
                    fakeShare.setShareId("offline");
                    fakeShare.setFromUserId("离线分享");
                    com.ttt.safevault.model.SharePermission permission = 
                        new com.ttt.safevault.model.SharePermission();
                    permission.setCanView(true);
                    permission.setCanSave(true);
                    permission.setRevocable(false);
                    fakeShare.setPermission(permission);
                    fakeShare.setExpireTime(0);
                    _shareDetails.postValue(fakeShare);
                } else {
                    _errorMessage.postValue("无法接收离线分享：密码错误或数据无效");
                }
            } catch (Exception e) {
                _errorMessage.postValue("接收离线分享失败: " + e.getMessage());
            } finally {
                _isLoading.postValue(false);
            }
        });
    }

    /**
     * 保存分享的密码到本地
     * @param shareId 分享ID
     */
    public void saveSharedPassword(String shareId) {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);
        _saveSuccess.setValue(false);

        executor.execute(() -> {
            try {
                int passwordId;
                
                // 如果是离线分享，直接保存已获取的PasswordItem
                if ("offline".equals(shareId)) {
                    PasswordItem item = _sharedPassword.getValue();
                    if (item != null) {
                        passwordId = backendService.saveItem(item);
                    } else {
                        _errorMessage.postValue("无法保存：数据丢失");
                        _isLoading.postValue(false);
                        return;
                    }
                } else {
                    // 在线分享
                    passwordId = backendService.saveSharedPassword(shareId);
                }
                
                if (passwordId > 0) {
                    _savedPasswordId.postValue(passwordId);
                    _saveSuccess.postValue(true);
                } else {
                    _errorMessage.postValue("保存失败");
                }
            } catch (Exception e) {
                _errorMessage.postValue("保存失败: " + e.getMessage());
            } finally {
                _isLoading.postValue(false);
            }
        });
    }

    /**
     * 检查分享是否有效
     */
    public boolean isShareValid() {
        PasswordShare details = _shareDetails.getValue();
        if (details == null) {
            return false;
        }
        return details.isAvailable();
    }

    /**
     * 清除错误信息
     */
    public void clearError() {
        _errorMessage.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
