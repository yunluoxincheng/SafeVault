package com.ttt.safevault.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ttt.safevault.model.BackendService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 登录/解锁页面的ViewModel
 * 负责处理用户认证相关的业务逻辑
 */
public class LoginViewModel extends AndroidViewModel {

    private final BackendService backendService;
    private final ExecutorService executor;

    // LiveData用于UI状态管理
    private final MutableLiveData<Boolean> _isAuthenticated = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _isInitialized = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _canUseBiometric = new MutableLiveData<>(false);

    public LiveData<Boolean> isAuthenticated = _isAuthenticated;
    public LiveData<String> errorMessage = _errorMessage;
    public LiveData<Boolean> isLoading = _isLoading;
    public LiveData<Boolean> isInitialized = _isInitialized;
    public LiveData<Boolean> canUseBiometric = _canUseBiometric;

    public LoginViewModel(@NonNull Application application, BackendService backendService) {
        super(application);
        this.backendService = backendService;
        this.executor = Executors.newSingleThreadExecutor();
        checkInitializationStatus();
    }

    /**
     * 检查应用是否已初始化
     */
    private void checkInitializationStatus() {
        _isLoading.setValue(true);
        executor.execute(() -> {
            try {
                boolean initialized = backendService.isInitialized();
                _isInitialized.postValue(initialized);

                // TODO: 检查是否支持生物识别
                boolean biometricSupported = checkBiometricSupport();
                _canUseBiometric.postValue(biometricSupported && initialized);
            } catch (Exception e) {
                _errorMessage.postValue("检查初始化状态失败: " + e.getMessage());
            } finally {
                _isLoading.postValue(false);
            }
        });
    }

    /**
     * 使用主密码登录/解锁
     */
    public void loginWithPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            _errorMessage.setValue("请输入密码");
            return;
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        executor.execute(() -> {
            try {
                boolean success = backendService.unlock(password);
                if (success) {
                    _isAuthenticated.postValue(true);
                } else {
                    _errorMessage.postValue("密码错误，请重试");
                }
            } catch (Exception e) {
                _errorMessage.postValue("登录失败: " + e.getMessage());
            } finally {
                _isLoading.postValue(false);
            }
        });
    }

    /**
     * 初始化应用，设置主密码
     */
    public void initializeWithPassword(String password, String confirmPassword) {
        // 验证密码输入
        String validationError = validatePasswordInput(password, confirmPassword);
        if (validationError != null) {
            _errorMessage.setValue(validationError);
            return;
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        executor.execute(() -> {
            try {
                boolean success = backendService.initialize(password);
                if (success) {
                    _isInitialized.postValue(true);
                    _isAuthenticated.postValue(true);
                } else {
                    _errorMessage.postValue("初始化失败，请重试");
                }
            } catch (Exception e) {
                _errorMessage.postValue("初始化失败: " + e.getMessage());
            } finally {
                _isLoading.postValue(false);
            }
        });
    }

    /**
     * 使用生物识别解锁
     */
    public void loginWithBiometric() {
        // TODO: 实现生物识别逻辑
        // 这里需要集成BiometricPrompt
        _isLoading.setValue(true);
        _errorMessage.setValue("生物识别功能待实现");
    }

    /**
     * 清除错误信息
     */
    public void clearError() {
        _errorMessage.setValue(null);
    }

    /**
     * 验证密码输入
     */
    private String validatePasswordInput(String password, String confirmPassword) {
        if (password == null || password.trim().isEmpty()) {
            return "请输入密码";
        }

        if (password.length() < 8) {
            return "密码长度至少8位";
        }

        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            return "请确认密码";
        }

        if (!password.equals(confirmPassword)) {
            return "两次输入的密码不一致";
        }

        // 检查密码强度
        if (!isStrongPassword(password)) {
            return "密码必须包含大小写字母和数字";
        }

        return null;
    }

    /**
     * 检查密码强度
     */
    private boolean isStrongPassword(String password) {
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }

        return hasUpper && hasLower && hasDigit;
    }

    /**
     * 检查生物识别支持
     */
    private boolean checkBiometricSupport() {
        // TODO: 实际检查生物识别硬件支持
        // 可以使用 BiometricManager.from(context).canAuthenticate()
        return false;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}