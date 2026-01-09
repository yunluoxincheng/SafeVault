package com.ttt.safevault.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ttt.safevault.dto.response.AuthResponse;
import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.network.RetrofitClient;
import com.ttt.safevault.network.TokenManager;
import com.ttt.safevault.security.KeyManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 用户认证ViewModel
 */
public class AuthViewModel extends AndroidViewModel {
    private static final String TAG = "AuthViewModel";

    private final BackendService backendService;
    private final RetrofitClient retrofitClient;
    private final TokenManager tokenManager;
    private final KeyManager keyManager;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<AuthResponse> authResponseLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);

    public AuthViewModel(@NonNull Application application) {
        super(application);
        this.backendService = com.ttt.safevault.ServiceLocator.getInstance().getBackendService();
        this.retrofitClient = RetrofitClient.getInstance(application);
        this.tokenManager = retrofitClient.getTokenManager();
        this.keyManager = KeyManager.getInstance(application);
    }

    /**
     * 用户注册
     */
    public void register(String username, String displayName) {
        loadingLiveData.setValue(true);

        // 获取设备ID和公钥
        String deviceId = keyManager.getDeviceId();
        String publicKey = keyManager.getPublicKey();

        if (deviceId == null || publicKey == null) {
            loadingLiveData.setValue(false);
            errorLiveData.setValue("密钥初始化失败");
            return;
        }

        Disposable disposable = retrofitClient.getAuthServiceApi()
            .register(new com.ttt.safevault.dto.request.RegisterRequest(deviceId, username, displayName, publicKey))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                response -> {
                    loadingLiveData.setValue(false);
                    tokenManager.saveTokens(response);
                    authResponseLiveData.setValue(response);
                    Log.d(TAG, "Register success: " + response.getUserId());
                },
                error -> {
                    loadingLiveData.setValue(false);
                    String message = "注册失败: " + error.getMessage();
                    errorLiveData.setValue(message);
                    Log.e(TAG, "Register failed", error);
                }
            );

        disposables.add(disposable);
    }

    /**
     * 用户登录
     */
    public void login() {
        loadingLiveData.setValue(true);

        // 获取保存的userId
        String userId = tokenManager.getUserId();
        if (userId == null) {
            loadingLiveData.setValue(false);
            errorLiveData.setValue("请先注册账号");
            return;
        }

        // 获取设备ID
        String deviceId = keyManager.getDeviceId();
        if (deviceId == null) {
            loadingLiveData.setValue(false);
            errorLiveData.setValue("设备ID获取失败");
            return;
        }

        // 生成签名（简化版本：使用userId和deviceId的哈希）
        String signature = generateSignature(userId, deviceId);

        Disposable disposable = retrofitClient.getAuthServiceApi()
            .login(new com.ttt.safevault.dto.request.LoginRequest(userId, deviceId, signature))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                response -> {
                    loadingLiveData.setValue(false);
                    tokenManager.saveTokens(response);
                    authResponseLiveData.setValue(response);
                    Log.d(TAG, "Login success: " + response.getUserId());
                },
                error -> {
                    loadingLiveData.setValue(false);
                    String message = "登录失败: " + error.getMessage();
                    errorLiveData.setValue(message);
                    Log.e(TAG, "Login failed", error);
                }
            );

        disposables.add(disposable);
    }

    /**
     * 生成签名（简化版本）
     * 生产环境应该使用RSA私钥签名
     */
    private String generateSignature(String userId, String deviceId) {
        try {
            String data = userId + deviceId + System.currentTimeMillis();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate signature", e);
            return "";
        }
    }

    /**
     * 刷新Token
     */
    public void refreshToken() {
        String refreshToken = tokenManager.getRefreshToken();
        if (refreshToken == null) {
            errorLiveData.setValue("无刷新Token");
            return;
        }

        Disposable disposable = retrofitClient.getAuthServiceApi()
            .refreshToken("Bearer " + refreshToken)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                response -> {
                    tokenManager.saveTokens(response);
                    authResponseLiveData.setValue(response);
                    Log.d(TAG, "Token refreshed");
                },
                error -> {
                    tokenManager.clearTokens();
                    errorLiveData.setValue("Token刷新失败，请重新登录");
                    Log.e(TAG, "Token refresh failed", error);
                }
            );

        disposables.add(disposable);
    }

    /**
     * 检查登录状态
     */
    public boolean isLoggedIn() {
        return tokenManager.isLoggedIn();
    }

    /**
     * 登出
     */
    public void logout() {
        tokenManager.clearTokens();
        authResponseLiveData.setValue(null);
    }

    // Getters for LiveData
    public LiveData<AuthResponse> getAuthResponse() {
        return authResponseLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
