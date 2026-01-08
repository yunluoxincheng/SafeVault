package com.ttt.safevault.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.model.Friend;
import com.ttt.safevault.model.UserProfile;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 好友管理页面的ViewModel
 * 负责管理好友列表、添加和删除好友
 */
public class FriendViewModel extends AndroidViewModel {

    private final BackendService backendService;
    private final ExecutorService executor;

    // LiveData用于UI状态管理
    private final MutableLiveData<List<Friend>> _friendList = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> _userProfile = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _operationSuccess = new MutableLiveData<>(false);
    private final MutableLiveData<String> _userQRCode = new MutableLiveData<>();

    public LiveData<List<Friend>> friendList = _friendList;
    public LiveData<UserProfile> userProfile = _userProfile;
    public LiveData<Boolean> isLoading = _isLoading;
    public LiveData<String> errorMessage = _errorMessage;
    public LiveData<Boolean> operationSuccess = _operationSuccess;
    public LiveData<String> userQRCode = _userQRCode;

    public FriendViewModel(@NonNull Application application, BackendService backendService) {
        super(application);
        this.backendService = backendService;
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * 加载好友列表
     */
    public void loadFriendList() {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        executor.execute(() -> {
            try {
                List<Friend> friends = backendService.getFriendList();
                _friendList.postValue(friends);
            } catch (Exception e) {
                _errorMessage.postValue("加载好友列表失败: " + e.getMessage());
            } finally {
                _isLoading.postValue(false);
            }
        });
    }

    /**
     * 加载用户配置文件
     */
    public void loadUserProfile() {
        executor.execute(() -> {
            try {
                UserProfile profile = backendService.getUserProfile();
                _userProfile.postValue(profile);
            } catch (Exception e) {
                _errorMessage.postValue("加载用户信息失败: " + e.getMessage());
            }
        });
    }

    /**
     * 生成用户二维码
     */
    public void generateUserQRCode() {
        executor.execute(() -> {
            try {
                String qrCode = backendService.generateUserQRCode();
                _userQRCode.postValue(qrCode);
            } catch (Exception e) {
                _errorMessage.postValue("生成二维码失败: " + e.getMessage());
            }
        });
    }

    /**
     * 添加好友
     * @param userId 好友用户ID
     */
    public void addFriend(String userId) {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);
        _operationSuccess.setValue(false);

        executor.execute(() -> {
            try {
                boolean success = backendService.addFriend(userId);
                
                if (success) {
                    _operationSuccess.postValue(true);
                    loadFriendList(); // 重新加载好友列表
                } else {
                    _errorMessage.postValue("添加好友失败");
                }
            } catch (Exception e) {
                _errorMessage.postValue("添加好友失败: " + e.getMessage());
            } finally {
                _isLoading.postValue(false);
            }
        });
    }

    /**
     * 删除好友
     * @param friendId 好友ID
     */
    public void removeFriend(String friendId) {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);
        _operationSuccess.setValue(false);

        executor.execute(() -> {
            try {
                boolean success = backendService.removeFriend(friendId);
                
                if (success) {
                    _operationSuccess.postValue(true);
                    // 从列表中移除
                    List<Friend> currentList = _friendList.getValue();
                    if (currentList != null) {
                        currentList.removeIf(friend -> friend.getFriendId().equals(friendId));
                        _friendList.postValue(currentList);
                    }
                } else {
                    _errorMessage.postValue("删除好友失败");
                }
            } catch (Exception e) {
                _errorMessage.postValue("删除好友失败: " + e.getMessage());
            } finally {
                _isLoading.postValue(false);
            }
        });
    }

    /**
     * 获取用户信息（用于添加好友确认）
     * @param userId 用户ID
     */
    public void getUserInfo(String userId, UserInfoCallback callback) {
        executor.execute(() -> {
            try {
                UserProfile user = backendService.getUserById(userId);
                if (callback != null) {
                    callback.onResult(user, null);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onResult(null, e);
                }
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
     * 用户信息回调接口
     */
    public interface UserInfoCallback {
        void onResult(UserProfile user, Exception error);
    }
}
