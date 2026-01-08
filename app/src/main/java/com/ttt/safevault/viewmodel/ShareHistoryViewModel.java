package com.ttt.safevault.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.model.PasswordShare;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 分享历史页面的ViewModel
 * 负责管理分享历史列表和撤销操作
 */
public class ShareHistoryViewModel extends AndroidViewModel {

    private final BackendService backendService;
    private final ExecutorService executor;

    // LiveData用于UI状态管理
    private final MutableLiveData<List<PasswordShare>> _myShares = new MutableLiveData<>();
    private final MutableLiveData<List<PasswordShare>> _receivedShares = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _operationSuccess = new MutableLiveData<>(false);

    public LiveData<List<PasswordShare>> myShares = _myShares;
    public LiveData<List<PasswordShare>> receivedShares = _receivedShares;
    public LiveData<Boolean> isLoading = _isLoading;
    public LiveData<String> errorMessage = _errorMessage;
    public LiveData<Boolean> operationSuccess = _operationSuccess;
    public LiveData<Boolean> revokeSuccess = _operationSuccess; // 别名，为了兼容

    public ShareHistoryViewModel(@NonNull Application application, BackendService backendService) {
        super(application);
        this.backendService = backendService;
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * 加载我创建的分享列表
     */
    public void loadMyShares() {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        executor.execute(() -> {
            try {
                List<PasswordShare> shares = backendService.getMyShares();
                _myShares.postValue(shares);
            } catch (Exception e) {
                _errorMessage.postValue("加载分享列表失败: " + e.getMessage());
            } finally {
                _isLoading.postValue(false);
            }
        });
    }

    /**
     * 加载我接收的分享列表
     */
    public void loadReceivedShares() {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        executor.execute(() -> {
            try {
                List<PasswordShare> shares = backendService.getReceivedShares();
                _receivedShares.postValue(shares);
            } catch (Exception e) {
                _errorMessage.postValue("加载分享列表失败: " + e.getMessage());
            } finally {
                _isLoading.postValue(false);
            }
        });
    }

    /**
     * 撤销分享
     * @param shareId 分享ID
     */
    public void revokeShare(String shareId) {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);
        _operationSuccess.setValue(false);

        executor.execute(() -> {
            try {
                boolean success = backendService.revokePasswordShare(shareId);
                
                if (success) {
                    _operationSuccess.postValue(true);
                    // 重新加载列表
                    loadMyShares();
                } else {
                    _errorMessage.postValue("撤销分享失败");
                }
            } catch (Exception e) {
                _errorMessage.postValue("撤销分享失败: " + e.getMessage());
            } finally {
                _isLoading.postValue(false);
            }
        });
    }

    /**
     * 获取分享详情
     * @param shareId 分享ID
     */
    public void getShareDetails(String shareId, ShareDetailsCallback callback) {
        executor.execute(() -> {
            try {
                PasswordShare share = backendService.getShareDetails(shareId);
                if (callback != null) {
                    callback.onResult(share, null);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onResult(null, e);
                }
            }
        });
    }

    /**
     * 刷新所有数据
     */
    public void refreshAll() {
        loadMyShares();
        loadReceivedShares();
    }

    /**
     * 清除错误信息
     */
    public void clearError() {
        _errorMessage.setValue(null);
    }

    /**
     * 清除操作成功状态
     */
    public void clearOperationSuccess() {
        _operationSuccess.setValue(false);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }

    /**
     * 分享详情回调接口
     */
    public interface ShareDetailsCallback {
        void onResult(PasswordShare share, Exception error);
    }
}
